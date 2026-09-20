import type { Request, Response } from 'express';

import { env } from '../config/env';
import { getServerPublicIp, normalizeIp } from '../services/ip.service';
import { formatLocalTimeWithOffset } from '../services/time.service';

/**
 * GET /api/info/server-ip
 *
 * Reports both ends of the connection. The client's address is included here
 * because a phone behind NAT cannot discover its own public address on its
 * own -- only the server it dialled can see it.
 */
export async function getServerIp(req: Request, res: Response): Promise<void> {
  res.json({
    serverIp: await getServerPublicIp(),
    clientIp: normalizeIp(req.ip ?? ''),
  });
}

/** GET /api/info/server-time -- server local time as `hh:mm:ss GMT+hh:mm`. */
export function getServerTime(_req: Request, res: Response): void {
  res.json({ serverTime: formatLocalTimeWithOffset(new Date()) });
}

/**
 * GET /api/info/developer
 *
 * The developer's name, which the app shows alongside the name of whoever is
 * currently signed in. These are two different people by design.
 */
export function getDeveloper(_req: Request, res: Response): void {
  res.json({
    firstName: env.developerFirstName,
    lastName: env.developerLastName,
  });
}
