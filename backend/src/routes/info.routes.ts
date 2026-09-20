import { Router } from 'express';

import {
  getDeveloper,
  getServerIp,
  getServerTime,
} from '../controllers/info.controller';
import { requireAuth } from '../middleware/auth';

export const infoRouter = Router();

// Everything below requires a valid session token. That is what makes the
// app's "connects to the backend once authenticated" step observable.
infoRouter.use(requireAuth);

infoRouter.get('/server-ip', getServerIp);
infoRouter.get('/server-time', getServerTime);
infoRouter.get('/developer', getDeveloper);
