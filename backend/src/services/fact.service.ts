import { HttpError } from '../types/errors';

const FACT_URL = 'https://uselessfacts.jsph.pl/api/v2/facts/random?language=en';
const TIMEOUT_MS = 5000;

export interface RandomFact {
  text: string;
  source: string;
  sourceUrl: string;
}

/** The subset of the upstream payload we rely on. */
interface UpstreamFact {
  text?: unknown;
  source?: unknown;
  source_url?: unknown;
}

/**
 * Fetches a random fact for Button 3's surprise.
 *
 * Proxied through our backend rather than called from the phone directly, so
 * the app talks to exactly one host: if the upstream ever moves or changes
 * shape, only this file changes and no APK needs rebuilding.
 */
export async function getRandomFact(): Promise<RandomFact> {
  let response: Response;
  try {
    response = await fetch(FACT_URL, {
      signal: AbortSignal.timeout(TIMEOUT_MS),
      headers: { Accept: 'application/json' },
    });
  } catch {
    // Timeout or DNS/connection failure. 502 rather than 500: the fault is
    // upstream, not in this server.
    throw new HttpError(502, 'Could not reach the facts service');
  }

  if (!response.ok) {
    throw new HttpError(502, `Facts service returned HTTP ${response.status}`);
  }

  let payload: UpstreamFact;
  try {
    payload = (await response.json()) as UpstreamFact;
  } catch {
    throw new HttpError(502, 'Facts service returned malformed JSON');
  }

  if (typeof payload.text !== 'string' || payload.text.trim() === '') {
    throw new HttpError(502, 'Facts service returned no fact text');
  }

  // Renamed from the upstream's snake_case so our own API stays camelCase
  // throughout. Attribution fields are optional upstream.
  return {
    text: payload.text.trim(),
    source: typeof payload.source === 'string' ? payload.source : '',
    sourceUrl: typeof payload.source_url === 'string' ? payload.source_url : '',
  };
}
