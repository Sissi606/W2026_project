# Cloud deployment (GCP VM + Caddy + HTTPS)

Deploys the backend to the Google Cloud VM behind Caddy, which terminates TLS
with an automatically renewed Let's Encrypt certificate.

| | |
| --- | --- |
| Project | `sturdy-ranger-435518-a3` |
| Zone / region | `us-central1-a` / `us-central1` |
| Instance | `cpen321-m1` |
| External IP | `34.29.207.92` (reserved static — see step 1) |
| Public URL | `https://34-29-207-92.sslip.io` |

## Why this shape

Port 3000 is never reachable from the internet. Two independent things enforce
that: the GCP firewall only admits `tcp:80` and `tcp:443`, and the Node process
binds `127.0.0.1` (`HOST=127.0.0.1`), so even an accidental firewall rule would
not expose it. Caddy on the same host is the only route in.

The hostname is a [sslip.io](https://sslip.io) address: `34-29-207-92.sslip.io`
resolves to `34.29.207.92` without creating a single DNS record. Let's Encrypt
will issue for it, but not for a bare IP — which is what makes this worth doing.
Because the certificate comes from a real CA, Android trusts it with no
`network_security_config.xml` and no certificate bundled into the APK.

## Step 1 — Reserve the external IP

The VM currently holds an *ephemeral* IP, which is released when the VM stops.
If it changed, the sslip.io hostname, the certificate and the URL baked into the
APK would all break at once. Promoting it keeps the same address:

```bash
gcloud config set project sturdy-ranger-435518-a3

gcloud compute addresses create cpen321-ip \
  --addresses=34.29.207.92 \
  --region=us-central1

gcloud compute addresses describe cpen321-ip \
  --region=us-central1 --format='value(address,status)'
```

Expect `34.29.207.92  IN_USE`.

## Step 2 — Open 80 and 443 (and only those)

Scoped to a network tag, so the rule applies to this VM rather than every
instance in the project.

```bash
gcloud compute firewall-rules create allow-http-https \
  --allow=tcp:80,tcp:443 \
  --target-tags=cpen321-web \
  --description="HTTP/HTTPS to the CPEN 321 backend via Caddy"

gcloud compute instances add-tags cpen321-m1 \
  --zone=us-central1-a --tags=cpen321-web
```

Port 80 is required: Let's Encrypt's HTTP-01 challenge is served over it, and
Caddy redirects it to HTTPS afterwards.

Confirm nothing exposes 3000:

```bash
gcloud compute firewall-rules list \
  --format='table(name,allowed[].map().firewall_rule().list(),sourceRanges.list(),targetTags.list())'
```

## Step 3 — Build and run the backend as a service

On the VM:

```bash
cd ~/W2026_project
git pull
cd backend
npm ci
npm run build

sudo cp ~/W2026_project/deploy/cpen321-backend.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now cpen321-backend
systemctl status cpen321-backend --no-pager
```

This replaces `npm run dev`. A process launched from an SSH session dies when
that session ends, so the demo would stop the moment the browser tab closed;
systemd also restarts it after a crash or a VM reboot.

The unit sets `HOST=127.0.0.1` and `NODE_ENV=production`. `dotenv` does not
overwrite variables that are already set, so these win over `backend/.env`.

Confirm the bind address is loopback, not `0.0.0.0`:

```bash
sudo ss -lntp | grep 3000
```

## Step 4 — Install and configure Caddy

Debian / Ubuntu:

```bash
sudo apt update
sudo apt install -y debian-keyring debian-archive-keyring apt-transport-https curl

curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' \
  | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' \
  | sudo tee /etc/apt/sources.list.d/caddy-stable.list

sudo apt update
sudo apt install -y caddy

sudo mkdir -p /var/log/caddy && sudo chown caddy:caddy /var/log/caddy
sudo cp ~/W2026_project/deploy/Caddyfile /etc/caddy/Caddyfile
sudo systemctl restart caddy
```

Watch it obtain the certificate — this takes a few seconds:

```bash
sudo journalctl -u caddy -f
```

Look for `certificate obtained successfully`. If it reports a challenge failure,
step 2 has not taken effect: Let's Encrypt must reach port 80 from outside.

## Step 5 — Verify

From your own machine, not the VM:

```bash
BASE=https://34-29-207-92.sslip.io

curl -s $BASE/health                       # {"status":"ok"}
curl -s -o /dev/null -w '%{http_code}\n' $BASE/api/info/server-time   # 401
curl -sI http://34-29-207-92.sslip.io/health | head -1                # 308 redirect to HTTPS
curl -s -m 8 -o /dev/null -w '%{http_code}\n' http://34.29.207.92:3000/health  # must fail
```

The certificate chain should validate with no `-k` flag. To inspect it:

```bash
echo | openssl s_client -connect 34-29-207-92.sslip.io:443 \
  -servername 34-29-207-92.sslip.io 2>/dev/null \
  | openssl x509 -noout -subject -issuer -dates
```

Authenticated endpoints, using a token minted on the VM with the real secret:

```bash
# on the VM
cd ~/W2026_project/backend
TOKEN=$(node -e "require('dotenv/config');const jwt=require('jsonwebtoken');console.log(jwt.sign({email:'test@example.com',firstName:'Linjia',lastName:'Qi'},process.env.JWT_SECRET,{subject:'test-123',expiresIn:600}))")
echo $TOKEN
```

```bash
# from your machine, with that token
curl -s https://34-29-207-92.sslip.io/api/info/server-time -H "Authorization: Bearer $TOKEN"
curl -s https://34-29-207-92.sslip.io/api/info/developer   -H "Authorization: Bearer $TOKEN"
curl -s https://34-29-207-92.sslip.io/api/info/server-ip   -H "Authorization: Bearer $TOKEN"
```

`clientIp` should now be your home IP rather than `::1`, which is what proves
the request genuinely crossed the internet.

## Step 6 — Point the app at it

In `frontend/local.properties`:

```properties
API_BASE_URL=https://34-29-207-92.sslip.io
```

Once this is in place, `android:usesCleartextTraffic="true"` can come out of
`AndroidManifest.xml` — nothing needs plaintext any more.

## Redeploying after a code change

```bash
cd ~/W2026_project && git pull
cd backend && npm ci && npm run build
sudo systemctl restart cpen321-backend
```

Caddy is untouched by application changes.
