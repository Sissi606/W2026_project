#!/usr/bin/env bash
#
# Installs (or reinstalls) the backend systemd unit on the VM.
#
# Exists because the path to node is not predictable: a NodeSource install puts
# it in /usr/bin, nvm puts it under ~/.nvm/versions/node/<version>/bin, and
# systemd does not read your shell profile -- so a unit that hard-codes
# /usr/bin/npm fails with status=203/EXEC on an nvm machine.
#
# Run from the repo root:  sudo -E ./deploy/install-service.sh

set -euo pipefail

die() { printf '\033[31mERROR:\033[0m %s\n' "$*" >&2; exit 1; }
info() { printf '\033[34m==>\033[0m %s\n' "$*"; }

# Resolve as the invoking user, not root: with nvm, root has a different PATH.
RUN_AS="${SUDO_USER:-$USER}"
USER_HOME="$(getent passwd "$RUN_AS" | cut -d: -f6)"

# Tried in order. A login shell picks up nvm's PATH, which a plain `sudo -u`
# does not; the glob covers the case where nvm is installed but its init script
# is not sourced non-interactively; the fixed paths cover system installs.
find_node() {
  # 0. explicit override: NODE_BIN=/path/to/node sudo -E ./deploy/install-service.sh
  if [[ -n "${NODE_BIN:-}" && -x "${NODE_BIN}" ]]; then
    echo "$NODE_BIN"; return
  fi

  # 1. login shell as the invoking user
  local candidate
  candidate="$(sudo -u "$RUN_AS" bash -lc 'command -v node' 2>/dev/null || true)"
  if [[ -n "$candidate" && -x "$candidate" ]]; then
    echo "$candidate"; return
  fi

  # 2. newest nvm-installed node
  if [[ -d "$USER_HOME/.nvm/versions/node" ]]; then
    candidate="$(find "$USER_HOME/.nvm/versions/node" -maxdepth 2 -type f -name node -perm -u+x 2>/dev/null | sort -V | tail -1)"
    if [[ -n "$candidate" ]]; then
      echo "$candidate"; return
    fi
  fi

  # 3. system locations
  for candidate in /usr/local/bin/node /usr/bin/node /opt/node/bin/node; do
    if [[ -x "$candidate" ]]; then
      echo "$candidate"; return
    fi
  done
}

NODE_BIN="$(find_node)"
[[ -n "$NODE_BIN" ]] || die "Could not find node for user $RUN_AS.
Run 'which node' as that user, then re-run with:
  NODE_BIN=<that path> sudo -E ./deploy/install-service.sh"

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../backend" && pwd)"
[[ -f "$APP_DIR/dist/index.js" ]] || die "$APP_DIR/dist/index.js is missing. Run: npm ci && npm run build"

info "node:  $NODE_BIN"
info "user:  $RUN_AS"
info "app:   $APP_DIR"

# node is invoked directly rather than through npm: one less executable to
# locate, and no npm process sitting between systemd and the server.
cat > /etc/systemd/system/cpen321-backend.service <<UNIT
[Unit]
Description=CPEN 321 backend
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=$RUN_AS
WorkingDirectory=$APP_DIR

# Bind loopback only: Caddy reverse-proxies to this, and nothing else can
# reach it even if a firewall rule is later opened by mistake.
Environment=HOST=127.0.0.1
Environment=NODE_ENV=production

ExecStart=$NODE_BIN $APP_DIR/dist/index.js
Restart=on-failure
RestartSec=5

NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=full

[Install]
WantedBy=multi-user.target
UNIT

info "Reloading systemd"
systemctl daemon-reload
systemctl enable --now cpen321-backend
systemctl restart cpen321-backend

sleep 2
systemctl status cpen321-backend --no-pager || true
