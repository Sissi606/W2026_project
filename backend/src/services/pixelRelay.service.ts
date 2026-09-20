import { EventEmitter } from 'node:events';

import WebSocket from 'ws';

import { env } from '../config/env';

const INITIAL_BACKOFF_MS = 1000;
const MAX_BACKOFF_MS = 30_000;

/**
 * Maintains the outbound connection to the course-provided pixel stream and
 * re-emits every frame it receives, untouched.
 *
 * One upstream connection is shared by every connected phone. Opening one per
 * client would multiply load on a server we do not own, and each client would
 * join a different image mid-build.
 *
 * Frames are re-emitted exactly as they arrive. The requirement is to relay
 * without batching, delaying or reformatting, so this class deliberately does
 * not parse the JSON -- parsing then re-serialising would be reformatting, and
 * would drop any field we failed to anticipate.
 */
export class PixelRelay extends EventEmitter {
  private socket: WebSocket | null = null;
  private backoffMs = INITIAL_BACKOFF_MS;
  private reconnectTimer: NodeJS.Timeout | null = null;
  private stopped = false;

  constructor(private readonly url: string = env.courseWsUrl) {
    super();
  }

  /** True while the upstream connection is established. */
  get isConnected(): boolean {
    return this.socket?.readyState === WebSocket.OPEN;
  }

  start(): void {
    this.stopped = false;
    this.connect();
  }

  stop(): void {
    this.stopped = true;
    if (this.reconnectTimer !== null) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    this.socket?.close();
    this.socket = null;
  }

  private connect(): void {
    console.log(`[relay] connecting to ${this.url}`);
    const socket = new WebSocket(this.url);
    this.socket = socket;

    socket.on('open', () => {
      console.log('[relay] upstream connected');
      // Only reset after a successful open, so a server that accepts and then
      // immediately drops the connection still gets backed off.
      this.backoffMs = INITIAL_BACKOFF_MS;
      this.emit('status', { connected: true });
    });

    socket.on('message', (data: WebSocket.RawData, isBinary: boolean) => {
      this.emit('frame', data, isBinary);
    });

    socket.on('close', (code: number) => {
      console.log(`[relay] upstream closed (code ${code})`);
      this.emit('status', { connected: false });
      this.scheduleReconnect();
    });

    socket.on('error', (error: Error) => {
      // 'error' is always followed by 'close', which handles the reconnect.
      // Without a listener here, ws would throw the error unhandled and take
      // the whole process down.
      console.error(`[relay] upstream error: ${error.message}`);
    });
  }

  private scheduleReconnect(): void {
    if (this.stopped || this.reconnectTimer !== null) {
      return;
    }

    const delay = this.backoffMs;
    console.log(`[relay] reconnecting in ${delay}ms`);

    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.connect();
    }, delay);

    // Exponential backoff, capped: the upstream is not ours to hammer.
    this.backoffMs = Math.min(this.backoffMs * 2, MAX_BACKOFF_MS);
  }
}
