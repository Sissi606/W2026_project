import { OAuth2Client } from 'google-auth-library';

import { env } from '../config/env';
import { HttpError } from '../types/errors';
import type { AuthenticatedUser } from '../types/auth';

// The client caches Google's public signing keys across calls, so it is created
// once rather than per request.
const oauthClient = new OAuth2Client(env.googleClientId);

/**
 * Verifies an ID token minted by Google for our Web client. verifyIdToken
 * checks the signature against Google's published keys, the issuer, the
 * expiry, and -- because we pass `audience` -- that the token was issued for
 * *our* client rather than some other app's.
 */
export async function verifyGoogleIdToken(
  idToken: string
): Promise<AuthenticatedUser> {
  let payload;
  try {
    const ticket = await oauthClient.verifyIdToken({
      idToken,
      audience: env.googleClientId,
    });
    payload = ticket.getPayload();
  } catch {
    throw new HttpError(401, 'Google ID token verification failed');
  }

  if (payload === undefined) {
    throw new HttpError(401, 'Google ID token verification failed');
  }

  // given_name/family_name are absent when the user's Google profile has no
  // name set, or when the token was issued without the `profile` scope.
  return {
    googleId: payload.sub,
    email: payload.email ?? '',
    firstName: payload.given_name ?? '',
    lastName: payload.family_name ?? '',
  };
}
