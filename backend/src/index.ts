import { createApp } from './app';
import { env } from './config/env';

const app = createApp();

const server = app.listen(env.port, env.host, () => {
  console.log(`Server listening on ${env.host}:${env.port}`);
});

for (const signal of ['SIGINT', 'SIGTERM'] as const) {
  process.on(signal, () => {
    server.close(() => {
      process.exit(0);
    });
  });
}
