import jwt from 'jsonwebtoken';

import { env } from '../config/env';
import { HttpError } from '../types/errors';
import type { AuthenticatedUser } from '../types/auth';

/**
 * Issues the app's own session token. We deliberately do not hand Google's ID
 * token back to the client for reuse: it is scoped to Google, expires on
 * Google's schedule, and re-verifying it on every request would mean an
 * outbound network call per API call.
 */
export function signSessionToken(user: AuthenticatedUser): string {
  return jwt.sign(
    {
      email: user.email,
      firstName: user.firstName,
      lastName: user.lastName,
    },
    env.jwtSecret,
    {
      subject: user.googleId,
      expiresIn: env.jwtExpiresInSeconds,
    }
  );
}

export function verifySessionToken(token: string): AuthenticatedUser {
  let payload: jwt.JwtPayload | string;
  try {
    payload = jwt.verify(token, env.jwtSecret);
  } catch {
    // Covers expiry, a bad signature, and malformed input alike. We do not
    // echo the library's message back: it tells an attacker which part failed.
    throw new HttpError(401, 'Invalid or expired token');
  }

  if (typeof payload === 'string' || typeof payload.sub !== 'string') {
    throw new HttpError(401, 'Invalid or expired token');
  }

  return {
    googleId: payload.sub,
    email: typeof payload.email === 'string' ? payload.email : '',
    firstName: typeof payload.firstName === 'string' ? payload.firstName : '',
    lastName: typeof payload.lastName === 'string' ? payload.lastName : '',
  };
}
