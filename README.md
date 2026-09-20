# CPEN321_26W1_ProjectName

_Keep this README up to date with the steps required to build and run the frontend and backend (including any scripts, config files, and environment variables). TAs ill follow these instructions._

## Requirements

Install the following before the frontend or backend setup steps:

- [git](https://git-scm.com/install/)


--- 

## Frontend Setup

### Requirements

- [Android Studio](https://developer.android.com/studio) (latest version)
- [Java 17](https://adoptium.net/temurin/releases/?version=17)
- [Android SDK](https://developer.android.com/studio#command-tools) with API level 36+ (Android 16)

### Setup

1. **Open project**: Open the `frontend/` directory in Android Studio
2. **Sync Gradle**: Android Studio will automatically prompt you to sync the project. Click "Sync Now". You can also manually run `cd frontend && ./gradlew build` to trigger the sync and download the necessary dependencies.
3. **Configure Android SDK**: Ensure you have Android SDK 36 installed.
4. **Set up emulator/device**:
   - Create a new AVD (Android Virtual Device) by selecting Pixel 9 as the device and Android Baklava (API level 36) as the system image.
   - Alternatively, connect a physical Android device running Android 16 (API level 36).
5. **Setup app config**: Copy the example file, then fill in local values:
   ```bash
   cp frontend/local.properties.example frontend/local.properties
   ```
   Set at least:
   - `sdk.dir`: path to your Android SDK. Android Studio usually writes this the first time you open `frontend/`. On Mac it is often `sdk.dir=/Users/<username>/Library/Android/sdk`.
   - `API_BASE_URL`: backend URL baked into the APK. Use `http://10.0.2.2:3000` for the emulator (`10.0.2.2` is the host machine). For a physical device on the same Wi-Fi, use `http://<your-lan-ip>:3000`.


### Build and Run

- **Debug build**: Click the green play button in the toolbar, to compile the code, package a debug APK, and install it on the connected device or running emulator. Alternatively, from the project root, run `./scripts/run-frontend.sh`.
- **Release build**: Go to Build -> Generate Signed App Bundle or APK -> APK. Follow the on-screen instructions to create a key, and select the "release" build variant. You will then have to manually install the generated APK on your device or the running emulator.


### Backend Configuration

Ensure the backend server is running and update the base URL in the app configuration if needed.

---
## Backend Setup

You can run the backend in one of two ways:
* Locally via Node.js 
* Via Docker Compose

Both ways use the same `backend/.env` file (see below).

### Environment configuration

From the project root:

```bash
cp backend/.env.example backend/.env
```

The server refuses to start if a required variable is missing, and tells you which one.

Required:
- `JWT_SECRET`: a long random string used to sign session tokens. Generate one with
  `node -e "console.log(require('crypto').randomBytes(48).toString('hex'))"`.
- `GOOGLE_CLIENT_ID`: the **Web application** OAuth client ID (see [Google OAuth setup](#google-oauth-setup)). Must be the same value as the frontend's `GOOGLE_CLIENT_ID` in `local.properties`.

Optional:
- `DEVELOPER_FIRST_NAME` / `DEVELOPER_LAST_NAME`: your own name, which `GET /api/info/developer` reports and the app displays. Defaults to `First` / `Last`.
- `SERVER_PUBLIC_IP`: skips the outbound public-IP lookup. Leave empty to have the server ask an external echo service (`api.ipify.org`) what address its traffic comes from.
- `PORT`: defaults to `3000` if unset.
- `MONGODB_URI`: not read by any code yet — login is stateless, and the identity travels inside the signed token. Kept for upcoming features. Ignored when running via Docker Compose.

### API endpoints

| Method | Path | Auth | Returns |
| ------ | ---- | ---- | ------- |
| `GET` | `/health` | — | `{ status }` |
| `POST` | `/api/auth/google` | — | `{ token, user }` — verifies a Google ID token and issues a session token |
| `GET` | `/api/info/server-ip` | Bearer | `{ serverIp, clientIp }` |
| `GET` | `/api/info/server-time` | Bearer | `{ serverTime }` as `hh:mm:ss GMT±hh:mm` |
| `GET` | `/api/info/developer` | Bearer | `{ firstName, lastName }` |
| `WS` | `/ws/pixels` | — | Relays the course pixel stream verbatim (Button 2) |
| `GET` | `/api/surprise/fact` | — | `{ text, source, sourceUrl }` — random fact for Button 3 |

`/ws/pixels` and `/api/surprise/fact` are deliberately unauthenticated: the three buttons must work
independently, so Button 2 cannot depend on Button 1 having been used.

Authenticated routes expect the session token from `/api/auth/google` in an `Authorization: Bearer <token>` header.

### Tests

```bash
cd backend
npm test          # jest, with coverage
npm run typecheck
```


### Option 1: Run locally

**Requirements:** 
- [Node.js](https://nodejs.org/en/download/) 22+
- [npm](https://docs.npmjs.com/downloading-and-installing-node-js-and-npm) 10+

**Setup:** 
1. Install dependencies:

   ```bash
   cd backend
   npm install
   ```

2. **Development** (TypeScript with auto-reload):

   ```bash
   npm run dev
   ```

3. **Production build** (optional):

   ```bash
   npm run build
   npm start
   ```

### Option 2: Run with Docker Compose

**Requirements:** 
- [Docker](https://docs.docker.com/desktop/setup/install) and [Docker Compose](https://docs.docker.com/desktop/setup/install) v2.24+
- [curl](https://curl.se/download.html)

**Setup**
1. **Start** (from the project root):

   ```bash
   ./scripts/run-backend.sh
   ```

   Or run Compose directly:

   ```bash
   docker compose up --build -d
   ```

2. **Stop**:

   ```bash
   docker compose down
   ```

## Cloud deployment

The backend runs on a Google Cloud VM behind Caddy, which terminates HTTPS with
an auto-renewing Let's Encrypt certificate:

**`https://34-29-207-92.sslip.io`**

Port 3000 is not publicly reachable — the firewall admits only 80/443 and the
Node process binds loopback. Full runbook, including firewall and certificate
setup: [deploy/README.md](deploy/README.md).

---

## Additional Setup

<a name="google-oauth-setup"></a>
### Google OAuth setup

Sign-in needs two OAuth clients in the same Google Cloud project. This is console work that cannot be scripted.

1. In the [Google Cloud Console](https://console.cloud.google.com/), create (or pick) a project, then open **APIs & Services → OAuth consent screen**. Choose **External**, fill in the app name and support email, and add your own Google account under **Test users** — without this, sign-in fails for accounts outside the project.
2. Open **APIs & Services → Credentials → Create Credentials → OAuth client ID** and create a **Web application** client. Copy its client ID into:
   - `backend/.env` as `GOOGLE_CLIENT_ID`
   - `frontend/local.properties` as `GOOGLE_CLIENT_ID`

   Both sides use the *Web* client ID: the Android app requests an ID token with it as the audience, and the backend verifies that audience matches.
3. Create a **second** OAuth client, type **Android**, with package name `com.example.cpen321application` and the SHA-1 of the keystore that signs the APK. This client's ID is never pasted anywhere — its existence is what authorizes the app to request tokens.

   Debug keystore SHA-1:
   ```bash
   keytool -list -v -alias androiddebugkey \
     -keystore ~/.android/debug.keystore \
     -storepass android -keypass android | grep SHA1
   ```
   For a release APK, run the same command against your release keystore and add that SHA-1 as well — a debug-only fingerprint means sign-in breaks in the submitted APK.

Neither `backend/.env` nor `frontend/local.properties` is committed, so every team member does steps 2 and 3's copying locally.