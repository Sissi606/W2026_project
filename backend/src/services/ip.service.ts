import { isIP } from 'node:net';

import { env } from '../config/env';

const LOOKUP_TIMEOUT_MS = 3000;
const CACHE_TTL_MS = 10 * 60 * 1000;

// Public IPs change rarely, so one lookup is reused for ten minutes rather than
// hitting an external service on every request.
let cachedIp: string | null = null;
let cachedAt = 0;

/**
 * IPv4 addresses arrive from Node's TCP stack as IPv4-mapped IPv6 (for example
 * `::ffff:127.0.0.1`) when the socket is dual-stack. The mapped form is correct
 * but unreadable on a phone screen, so it is unwrapped.
 */
export function normalizeIp(ip: string): string {
  const prefix = '::ffff:';
  if (ip.startsWith(prefix)) {
    const unwrapped = ip.slice(prefix.length);
    if (isIP(unwrapped) === 4) {
      return unwrapped;
    }
  }
  return ip;
}

/**
 * The server's own public address. There is no reliable way for a host behind
 * NAT to know this locally, so unless SERVER_PUBLIC_IP is configured we ask an
 * external echo service what address our traffic appears to come from.
 */
export async function getServerPublicIp(): Promise<string> {
  if (env.serverPublicIp !== '') {
    return env.serverPublicIp;
  }

  const now = Date.now();
  if (cachedIp !== null && now - cachedAt < CACHE_TTL_MS) {
    return cachedIp;
  }

  const response = await fetch('https://api.ipify.org', {
    signal: AbortSignal.timeout(LOOKUP_TIMEOUT_MS),
  });
  if (!response.ok) {
    throw new Error(`Public IP lookup failed with HTTP ${response.status}`);
  }

  const body = (await response.text()).trim();
  if (isIP(body) === 0) {
    throw new Error(`Public IP lookup returned a non-address: ${body}`);
  }

  cachedIp = body;
  cachedAt = now;
  return body;
}

/** Drops the cached lookup. Used by tests; also handy after a network change. */
export function clearPublicIpCache(): void {
  cachedIp = null;
  cachedAt = 0;
}
