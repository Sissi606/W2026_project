import { Router } from 'express';

import { googleLogin } from '../controllers/auth.controller';

export const authRouter = Router();

// Unauthenticated by definition: this is how a client gets a token.
authRouter.post('/google', googleLogin);
