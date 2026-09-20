import type { Request, Response } from 'express';

import { verifyGoogleIdToken } from '../services/google.service';
import { signSessionToken } from '../services/token.service';
import { HttpError } from '../types/errors';

interface GoogleLoginBody {
  idToken?: unknown;
}

/**
 * POST /api/auth/google
 *
 * Exchanges a Google ID token for one of ours. Nothing is persisted: the
 * user's identity travels inside the signed token, so the server stays
 * stateless and no database is involved in the login path.
 */
export async function googleLogin(req: Request, res: Response): Promise<void> {
  const { idToken } = req.body as GoogleLoginBody;

  if (typeof idToken !== 'string' || idToken.trim() === '') {
    throw new HttpError(400, 'Request body must include an "idToken" string');
  }

  const user = await verifyGoogleIdToken(idToken.trim());

  res.json({
    token: signSessionToken(user),
    user: {
      email: user.email,
      firstName: user.firstName,
      lastName: user.lastName,
    },
  });
}
