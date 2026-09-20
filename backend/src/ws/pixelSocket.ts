import type { Server } from 'node:http';

import { WebSocketServer, WebSocket } from 'ws';

import type { PixelRelay } from '../services/pixelRelay.service';

export const PIXEL_WS_PATH = '/ws/pixels';

/**
 * Serves the downstream pixel stream to connected phones.
 *
 * Deliberately unauthenticated. The three buttons must work independently of
 * one another, so Button 2 cannot require the session token that Button 1
 * produces. The stream carries no user data -- it is the same public pixel
 * feed for everyone.
 *
 * Shares the existing HTTP server rather than opening a second port, so the
 * single Caddy reverse proxy covers it and no extra firewall rule is needed.
 */
export function attachPixelSocket(server: Server, relay: PixelRelay): WebSocketServer {
  const wss = new WebSocketServer({ server, path: PIXEL_WS_PATH });

  const onFrame = (data: Buffer | ArrayBuffer | Buffer[], isBinary: boolean) => {
    for (const client of wss.clients) {
      if (client.readyState === WebSocket.OPEN) {
        // Forwarded verbatim: no batching, no delay, no reformatting.
        client.send(data, { binary: isBinary });
      }
    }
  };

  relay.on('frame', onFrame);

  wss.on('connection', (socket) => {
    console.log(`[ws] client connected (${wss.clients.size} total)`);

    socket.on('close', () => {
      console.log(`[ws] client disconnected (${wss.clients.size} remaining)`);
    });

    socket.on('error', (error: Error) => {
      console.error(`[ws] client error: ${error.message}`);
    });
  });

  wss.on('close', () => {
    relay.off('frame', onFrame);
  });

  return wss;
}
