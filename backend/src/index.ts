import { createApp } from './app';
import { env } from './config/env';
import { PixelRelay } from './services/pixelRelay.service';
import { attachPixelSocket } from './ws/pixelSocket';

const app = createApp();

const server = app.listen(env.port, env.host, () => {
  console.log(`Server listening on ${env.host}:${env.port}`);
});

// Button 2: relay the course pixel stream to connected phones. Started
// alongside the HTTP server so the upstream is already streaming by the time
// the first client connects.
const relay = new PixelRelay();
const pixelSocket = attachPixelSocket(server, relay);
relay.start();

for (const signal of ['SIGINT', 'SIGTERM'] as const) {
  process.on(signal, () => {
    relay.stop();
    pixelSocket.close();
    server.close(() => {
      process.exit(0);
    });
  });
}
