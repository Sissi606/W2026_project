import request from 'supertest';

import { createApp } from '../../src/app';

// The upstream facts service is mocked at the fetch boundary: it is a third
// party, it returns a different fact every call, and the suite must not depend
// on it being up.
const fetchMock = jest.fn();

// Interface GET /api/surprise/fact
describe('Mocked: GET /api/surprise/fact', () => {
  beforeEach(() => {
    fetchMock.mockReset();
    global.fetch = fetchMock as unknown as typeof fetch;
  });

  function jsonResponse(body: unknown, status = 200) {
    return { ok: status >= 200 && status < 300, status, json: () => Promise.resolve(body) };
  }

  // Input: a request while the upstream is healthy
  // Mocked behavior: upstream returns a complete fact payload
  // Expected status code: 200
  // Expected behavior: snake_case source_url is renamed to camelCase so our
  //   own API stays internally consistent
  // Expected output: { text, source, sourceUrl }
  test('Returns a fact', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({
        id: 'abc',
        text: '  Honey never spoils.  ',
        source: 'djtech.net',
        source_url: 'https://example.com/fact',
        language: 'en',
      })
    );

    const response = await request(createApp()).get('/api/surprise/fact');

    expect(response.status).toBe(200);
    expect(response.body).toEqual({
      text: 'Honey never spoils.',
      source: 'djtech.net',
      sourceUrl: 'https://example.com/fact',
    });
  });

  // Input: a request while the upstream omits attribution
  // Mocked behavior: upstream returns text only
  // Expected status code: 200
  // Expected behavior: optional fields become empty strings rather than
  //   undefined, so the app always has something to bind to
  // Expected output: empty source and sourceUrl
  test('Tolerates a payload without attribution', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ text: 'Bananas are berries.' }));

    const response = await request(createApp()).get('/api/surprise/fact');

    expect(response.status).toBe(200);
    expect(response.body).toEqual({
      text: 'Bananas are berries.',
      source: '',
      sourceUrl: '',
    });
  });

  // Input: a request while the upstream is erroring
  // Mocked behavior: upstream responds 500
  // Expected status code: 502
  // Expected behavior: reported as a bad gateway, since the fault is upstream
  //   rather than in this server
  // Expected output: { error: <message naming the upstream status> }
  test('Upstream error becomes 502', async () => {
    fetchMock.mockResolvedValue(jsonResponse({}, 500));

    const response = await request(createApp()).get('/api/surprise/fact');

    expect(response.status).toBe(502);
    expect(response.body.error).toContain('500');
  });

  // Input: a request while the upstream is unreachable
  // Mocked behavior: fetch rejects, as on a timeout or DNS failure
  // Expected status code: 502
  // Expected behavior: the failure is caught rather than crashing the request
  // Expected output: { error: "Could not reach the facts service" }
  test('Unreachable upstream becomes 502', async () => {
    fetchMock.mockRejectedValue(new Error('ETIMEDOUT'));

    const response = await request(createApp()).get('/api/surprise/fact');

    expect(response.status).toBe(502);
    expect(response.body).toEqual({ error: 'Could not reach the facts service' });
  });

  // Input: a request while the upstream returns a body with no fact
  // Mocked behavior: upstream responds 200 with an unexpected shape
  // Expected status code: 502
  // Expected behavior: the payload is validated, not trusted, so the app never
  //   renders an empty surprise
  // Expected output: { error: "Facts service returned no fact text" }
  test('Missing fact text becomes 502', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ id: 'abc', language: 'en' }));

    const response = await request(createApp()).get('/api/surprise/fact');

    expect(response.status).toBe(502);
    expect(response.body).toEqual({ error: 'Facts service returned no fact text' });
  });

  // Input: a request with no Authorization header
  // Mocked behavior: upstream healthy
  // Expected status code: 200
  // Expected behavior: the endpoint is deliberately unauthenticated, because
  //   Button 3 must work without Button 1 having been used
  // Expected output: a fact
  test('Works without authentication', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ text: 'Octopuses have three hearts.' }));

    const response = await request(createApp()).get('/api/surprise/fact');

    expect(response.status).toBe(200);
  });
});
