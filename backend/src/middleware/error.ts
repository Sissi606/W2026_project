import type { NextFunction, Request, Response } from 'express';

import { env } from '../config/env';
import { HttpError } from '../types/errors';

export function notFoundHandler(_req: Request, res: Response): void {
  res.status(404).json({ error: 'Not Found' });
}

/**
 * Single place where thrown errors become responses. Express 5 forwards
 * rejected promises from async handlers here automatically, so handlers can
 * simply throw.
 */
export function errorHandler(
  error: unknown,
  _req: Request,
  res: Response,
  // Express identifies an error handler by its arity, so this stays in the
  // signature even though it is unused.
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  _next: NextFunction
): void {
  if (error instanceof HttpError) {
    res.status(error.status).json({ error: error.message });
    return;
  }

  // Unexpected failures are logged in full but reported vaguely: internal
  // messages can leak configuration details.
  console.error('Unhandled error:', error);
  res.status(500).json({
    error:
      env.nodeEnv === 'development' && error instanceof Error
        ? error.message
        : 'Internal Server Error',
  });
}
