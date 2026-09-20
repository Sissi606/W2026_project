import request from 'supertest';

import { createApp } from '../../src/app';
import { getServerPublicIp } from '../../src/services/ip.service';
import { signSessionToken } from '../../src/services/token.service';

// Only the outbound public-IP lookup is mocked; the auth gate, the token
// service and the time formatter all run for real.
jest.mock('../../src/services/ip.service', () => ({
  ...jest.requireActual('../../src/services/ip.service'),
  getServerPublicIp: jest.fn(),
}));

const getServerPublicIpMock = jest.mocked(getServerPublicIp);

const authHeader = `Bearer ${signSessionToken({
  googleId: '1234567890',
  email: 'ada@example.com',
  firstName: 'Ada',
  lastName: 'Lovelace',
})}`;

const TIME_PATTERN = /^\d{2}:\d{2}:\d{2} GMT[+-]\d{2}:\d{2}$/;

// Interface GET /api/info/server-ip
describe('Mocked: GET /api/info/server-ip', () => {
  beforeEach(() => {
    getServerPublicIpMock.mockReset();
  });

  // Input: an authenticated request
  // Mocked behavior: the public-IP lookup resolves with a fixed address
  // Expected status code: 200
  // Expected behavior: both ends of the connection are reported; the client
  //   address comes from the socket, which supertest dials over loopback
  // Expected output: { serverIp: "198.51.100.42", clientIp: <loopback> }
  test('Authenticated request', async () => {
    getServerPublicIpMock.mockResolvedValue('198.51.100.42');

    const response = await request(createApp())
      .get('/api/info/server-ip')
      .set('Authorization', authHeader);

    expect(response.status).toBe(200);
    expect(response.body.serverIp).toBe('198.51.100.42');
    expect(response.body.clientIp).toBe('127.0.0.1');
  });

  // Input: an authenticated request while the echo service is unreachable
  // Mocked behavior: the public-IP lookup rejects
  // Expected status code: 500
  // Expected behavior: the failure is reported as a server error rather than
  //   crashing the process or hanging the request
  // Expected output: { error: <message> }
  test('Public IP lookup fails', async () => {
    getServerPublicIpMock.mockRejectedValue(new Error('lookup exploded'));
    const consoleError = jest.spyOn(console, 'error').mockImplementation(() => undefined);

    const response = await request(createApp())
      .get('/api/info/server-ip')
      .set('Authorization', authHeader);

    expect(response.status).toBe(500);
    consoleError.mockRestore();
  });

  // Input: a request with no Authorization header
  // Mocked behavior: none; the request is rejected at the auth gate
  // Expected status code: 401
  // Expected behavior: the handler never runs, so no lookup is attempted
  // Expected output: { error: "Missing Bearer token" }
  test('No Authorization header', async () => {
    const response = await request(createApp()).get('/api/info/server-ip');

    expect(response.status).toBe(401);
    expect(response.body).toEqual({ error: 'Missing Bearer token' });
    expect(getServerPublicIpMock).not.toHaveBeenCalled();
  });

  // Input: a request whose Bearer token was signed with another secret
  // Mocked behavior: none; the request is rejected at the auth gate
  // Expected status code: 401
  // Expected behavior: a forged token is refused
  // Expected output: { error: "Invalid or expired token" }
  test('Forged Bearer token', async () => {
    const response = await request(createApp())
      .get('/api/info/server-ip')
      .set('Authorization', 'Bearer not-a-real-token');

    expect(response.status).toBe(401);
    expect(response.body).toEqual({ error: 'Invalid or expired token' });
  });
});

// Interface GET /api/info/server-time
describe('Mocked: GET /api/info/server-time', () => {
  // Input: an authenticated request
  // Mocked behavior: none needed; the formatter runs against the real clock
  // Expected status code: 200
  // Expected behavior: the time is returned in the exact display format, so
  //   the app can show it without reformatting
  // Expected output: { serverTime: "hh:mm:ss GMT±hh:mm" }
  test('Authenticated request', async () => {
    const response = await request(createApp())
      .get('/api/info/server-time')
      .set('Authorization', authHeader);

    expect(response.status).toBe(200);
    expect(response.body.serverTime).toMatch(TIME_PATTERN);
  });

  // Input: a request with no Authorization header
  // Mocked behavior: none; the request is rejected at the auth gate
  // Expected status code: 401
  // Expected behavior: the time is not disclosed to an unauthenticated caller
  // Expected output: { error: "Missing Bearer token" }
  test('No Authorization header', async () => {
    const response = await request(createApp()).get('/api/info/server-time');

    expect(response.status).toBe(401);
  });
});

// Interface GET /api/info/developer
describe('Mocked: GET /api/info/developer', () => {
  // Input: an authenticated request
  // Mocked behavior: none; the name comes from the test environment config
  // Expected status code: 200
  // Expected behavior: reports the developer's name, which is independent of
  //   whichever account is signed in
  // Expected output: { firstName: "Ada", lastName: "Lovelace" }
  test('Authenticated request', async () => {
    const response = await request(createApp())
      .get('/api/info/developer')
      .set('Authorization', authHeader);

    expect(response.status).toBe(200);
    expect(response.body).toEqual({ firstName: 'Ada', lastName: 'Lovelace' });
  });

  // Input: a request with no Authorization header
  // Mocked behavior: none; the request is rejected at the auth gate
  // Expected status code: 401
  // Expected behavior: the handler never runs
  // Expected output: { error: "Missing Bearer token" }
  test('No Authorization header', async () => {
    const response = await request(createApp()).get('/api/info/developer');

    expect(response.status).toBe(401);
  });
});
