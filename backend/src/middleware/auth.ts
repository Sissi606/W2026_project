import type { NextFunction, Request, Response } from 'express';

import { verifySessionToken } from '../services/token.service';
import { HttpError } from '../types/errors';

const BEARER_PREFIX = 'Bearer ';

/**
 * Gate for every endpoint that is only meaningful once the user has signed in.
 * On success the decoded user is attached to `req.user`.
 */
export function requireAuth(
  req: Request,
  _res: Response,
  next: NextFunction
): void {
  const header = req.headers.authorization;

  if (header === undefined || !header.startsWith(BEARER_PREFIX)) {
    next(new HttpError(401, 'Missing Bearer token'));
    return;
  }

  try {
    req.user = verifySessionToken(header.slice(BEARER_PREFIX.length).trim());
    next();
  } catch (error) {
    next(error);
  }
}
