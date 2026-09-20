import { Router } from 'express';

import { getSurpriseFact } from '../controllers/surprise.controller';

export const surpriseRouter = Router();

// Unauthenticated, like the pixel stream: the three buttons must work
// independently, so Button 3 cannot depend on Button 1 having been used.
surpriseRouter.get('/fact', getSurpriseFact);
