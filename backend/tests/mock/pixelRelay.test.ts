import { createServer, type Server } from 'node:http';
import type { AddressInfo } from 'node:net';

import WebSocket, { WebSocketServer } from 'ws';

import { PixelRelay } from '../../src/services/pixelRelay.service';
import { attachPixelSocket, PIXEL_WS_PATH } from '../../src/ws/pixelSocket';

/**
 * The course-provided server is mocked by a local WebSocket server. The real
 * one is outside our control and streams continuously, which would make
 * assertions non-deterministic; a local stand-in lets us send an exact frame
 * and check exactly what comes out the other end.
 */
describe('Mocked: pixel relay', () => {
  let upstream: WebSocketServer;
  let upstreamUrl: string;
  let relay: PixelRelay | null = null;
  let downstream: Server | null = null;

  beforeEach(async () => {
    upstream = new WebSocketServer({ port: 0 });
    await new Promise<void>((resolve) => upstream.on('listening', resolve));
    const { port } = upstream.address() as AddressInfo;
    upstreamUrl = `ws://127.0.0.1:${port}`;
  });

  afterEach(async () => {
    relay?.stop();
    relay = null;
    await new Promise<void>((resolve) => upstream.close(() => resolve()));
    if (downstream !== null) {
      await new Promise<void>((resolve) => downstream?.close(() => resolve()));
      downstream = null;
    }
  });

  /** Resolves once the upstream has a client, i.e. the relay has connected. */
  function waitForUpstreamClient(): Promise<WebSocket> {
    return new Promise((resolve) => upstream.on('connection', resolve));
  }

  // Input: a pixel update sent by the upstream server
  // Mocked behavior: local WebSocket server stands in for the course server
  // Expected behavior: the relay re-emits the frame with its bytes unchanged,
  //   since reformatting the payload is explicitly disallowed
  // Expected output: the identical JSON string
  test('Relays a frame verbatim', async () => {
    const payload = '{"x":3,"y":11,"color":"#ff8800"}';

    relay = new PixelRelay(upstreamUrl);
    const frameReceived = new Promise<string>((resolve) => {
      relay?.on('frame', (data: Buffer) => resolve(data.toString()));
    });

    relay.start();
    const client = await waitForUpstreamClient();
    client.send(payload);

    expect(await frameReceived).toBe(payload);
  });

  // Input: several updates sent back to back
  // Mocked behavior: upstream sends three frames with no pause
  // Expected behavior: all arrive, in order, individually -- not coalesced
  // Expected output: the three payloads in the order sent
  test('Preserves frame order and does not batch', async () => {
    const payloads = [
      '{"x":0,"y":0,"color":"#000000"}',
      '{"x":1,"y":0,"color":"#111111"}',
      '{"x":2,"y":0,"color":"#222222"}',
    ];

    relay = new PixelRelay(upstreamUrl);
    const received: string[] = [];
    const done = new Promise<void>((resolve) => {
      relay?.on('frame', (data: Buffer) => {
        received.push(data.toString());
        if (received.length === payloads.length) resolve();
      });
    });

    relay.start();
    const client = await waitForUpstreamClient();
    payloads.forEach((p) => client.send(p));
    await done;

    expect(received).toEqual(payloads);
  });

  // Input: the upstream drops the connection
  // Mocked behavior: the server closes the socket after the relay connects
  // Expected behavior: the relay reconnects on its own rather than going
  //   permanently dead, so a restart of the course server is survivable
  // Expected output: a second upstream connection is observed
  test('Reconnects after the upstream drops', async () => {
    relay = new PixelRelay(upstreamUrl);

    // Keyed off the relay's own status events rather than the server's
    // 'connection' event: the server accepts the socket slightly before the
    // client side reaches OPEN, so asserting on the latter would race.
    let opens = 0;
    const reconnected = new Promise<void>((resolve) => {
      relay?.on('status', (status: { connected: boolean }) => {
        if (status.connected) {
          opens += 1;
          if (opens === 2) resolve();
        }
      });
    });

    const firstClient = await new Promise<WebSocket>((resolve) => {
      upstream.on('connection', resolve);
      relay?.start();
    });

    firstClient.close();
    await reconnected;

    expect(opens).toBe(2);
    expect(relay.isConnected).toBe(true);
  }, 10_000);

  // Input: a pixel update, with a phone connected to our own WebSocket
  // Mocked behavior: local upstream; a real ws client stands in for the app
  // Expected behavior: the full path -- course server to our relay to our
  //   WebSocket to the client -- delivers the payload untouched
  // Expected output: the identical JSON string at the client
  test('Delivers a frame end to end to a downstream client', async () => {
    downstream = createServer();
    relay = new PixelRelay(upstreamUrl);
    attachPixelSocket(downstream, relay);

    await new Promise<void>((resolve) => downstream?.listen(0, resolve));
    const { port } = downstream.address() as AddressInfo;

    relay.start();
    const upstreamClient = await waitForUpstreamClient();

    const phone = new WebSocket(`ws://127.0.0.1:${port}${PIXEL_WS_PATH}`);
    await new Promise<void>((resolve) => phone.on('open', () => resolve()));

    const payload = '{"x":15,"y":15,"color":"#00ff00"}';
    const delivered = new Promise<string>((resolve) => {
      phone.on('message', (data: Buffer) => resolve(data.toString()));
    });

    upstreamClient.send(payload);
    expect(await delivered).toBe(payload);

    phone.close();
  });
});
