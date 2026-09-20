import 'dotenv/config';

/**
 * Environment variables are read by literal name rather than by dynamic lookup:
 * `process.env[name]` trips the security/detect-object-injection lint rule, and
 * literal access also lets TypeScript see exactly which keys we depend on.
 */

function required(name: string, value: string | undefined): string {
  const trimmed = value?.trim() ?? '';
  if (trimmed === '') {
    throw new Error(
      `Missing required environment variable: ${name}. Copy .env.example to .env and fill it in.`
    );
  }
  return trimmed;
}

function optional(value: string | undefined, fallback: string): string {
  const trimmed = value?.trim() ?? '';
  return trimmed === '' ? fallback : trimmed;
}

function parsePort(value: string | undefined): number {
  const trimmed = value?.trim() ?? '';
  if (trimmed === '') {
    return 3000;
  }
  const parsed = Number.parseInt(trimmed, 10);
  if (Number.isNaN(parsed) || parsed < 1 || parsed > 65535) {
    throw new Error(`Invalid PORT: ${trimmed}`);
  }
  return parsed;
}

export const env = {
  port: parsePort(process.env.PORT),

  // Which interface to bind. Behind a reverse proxy set HOST=127.0.0.1 so the
  // Node process is unreachable from outside the machine and the proxy is the
  // only way in. Docker needs 0.0.0.0, since the port is published from the
  // container, so that stays the default.
  host: optional(process.env.HOST, '0.0.0.0'),

  nodeEnv: optional(process.env.NODE_ENV, 'development'),

  // Signs this app's own session tokens. Unrelated to Google's signing keys.
  jwtSecret: required('JWT_SECRET', process.env.JWT_SECRET),
  jwtExpiresInSeconds: 7 * 24 * 60 * 60,

  // The *Web* OAuth client ID. The Android app asks Google for an ID token with
  // this as the audience, and this server checks that the audience matches.
  googleClientId: required('GOOGLE_CLIENT_ID', process.env.GOOGLE_CLIENT_ID),

  // The developer's own name, reported by /api/info/developer. This is not the
  // name of whoever is logged in -- the app displays both, side by side.
  developerFirstName: optional(process.env.DEVELOPER_FIRST_NAME, 'First'),
  developerLastName: optional(process.env.DEVELOPER_LAST_NAME, 'Last'),

  // Set this when the host cannot discover its own public address, or to skip
  // the outbound lookup entirely. Empty means "ask an external echo service".
  serverPublicIp: optional(process.env.SERVER_PUBLIC_IP, ''),

  // The course-provided pixel stream that Button 2 relays. Configurable
  // because the address is outside our control and may change.
  courseWsUrl: optional(process.env.COURSE_WS_URL, 'wss://8.229.22.124'),
} as const;
