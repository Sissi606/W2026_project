import cors from 'cors';
import express, { type Express } from 'express';

import { errorHandler, notFoundHandler } from './middleware/error';
import { authRouter } from './routes/auth.routes';
import { infoRouter } from './routes/info.routes';
import { surpriseRouter } from './routes/surprise.routes';
import './types/auth';

export function createApp(): Express {
  const app = express();

  // In Docker and behind a reverse proxy the socket's peer is the proxy, not
  // the phone, so req.ip must come from X-Forwarded-For. Only hops on private
  // networks are trusted; a header forged by an outside client is ignored.
  app.set('trust proxy', ['loopback', 'linklocal', 'uniquelocal']);

  app.use(cors());
  app.use(express.json());

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok' });
  });

  app.use('/api/auth', authRouter);
  app.use('/api/info', infoRouter);
  app.use('/api/surprise', surpriseRouter);

  app.use(notFoundHandler);
  app.use(errorHandler);

  return app;
}
