import type { Request, Response } from 'express';

import { getRandomFact } from '../services/fact.service';

/**
 * GET /api/surprise/fact
 *
 * Powers the reveal when Button 3's timer finishes.
 */
export async function getSurpriseFact(_req: Request, res: Response): Promise<void> {
  res.json(await getRandomFact());
}
