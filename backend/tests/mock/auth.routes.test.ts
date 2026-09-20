import request from 'supertest';

import { createApp } from '../../src/app';
import { verifyGoogleIdToken } from '../../src/services/google.service';
import { verifySessionToken } from '../../src/services/token.service';
import { HttpError } from '../../src/types/errors';

// Google's verifier is mocked: the real one calls out to Google for signing
// keys and needs a token only Google can mint, neither of which belongs in a
// test suite. Everything downstream of it is exercised for real.
jest.mock('../../src/services/google.service');

const verifyGoogleIdTokenMock = jest.mocked(verifyGoogleIdToken);

// Interface POST /api/auth/google
describe('Mocked: POST /api/auth/google', () => {
  beforeEach(() => {
    verifyGoogleIdTokenMock.mockReset();
  });

  // Input: a body carrying an ID token that Google accepts
  // Mocked behavior: verifyGoogleIdToken resolves with the account's profile
  // Expected status code: 200
  // Expected behavior: a session token is issued that decodes back to that
  //   same profile; nothing is written to any store
  // Expected output: { token, user: { email, firstName, lastName } }
  test('Valid Google ID token', async () => {
    verifyGoogleIdTokenMock.mockResolvedValue({
      googleId: '1234567890',
      email: 'ada@example.com',
      firstName: 'Ada',
      lastName: 'Lovelace',
    });

    const response = await request(createApp())
      .post('/api/auth/google')
      .send({ idToken: 'a-google-id-token' });

    expect(response.status).toBe(200);
    expect(response.body.user).toEqual({
      email: 'ada@example.com',
      firstName: 'Ada',
      lastName: 'Lovelace',
    });
    expect(verifySessionToken(response.body.token)).toEqual({
      googleId: '1234567890',
      email: 'ada@example.com',
      firstName: 'Ada',
      lastName: 'Lovelace',
    });
  });

  // Input: a body carrying a token Google rejects
  // Mocked behavior: verifyGoogleIdToken throws HttpError(401)
  // Expected status code: 401
  // Expected behavior: no session token is issued
  // Expected output: { error: "Google ID token verification failed" }
  test('Rejected Google ID token', async () => {
    verifyGoogleIdTokenMock.mockRejectedValue(
      new HttpError(401, 'Google ID token verification failed')
    );

    const response = await request(createApp())
      .post('/api/auth/google')
      .send({ idToken: 'a-forged-token' });

    expect(response.status).toBe(401);
    expect(response.body).toEqual({
      error: 'Google ID token verification failed',
    });
  });

  // Input: a body with no idToken field
  // Mocked behavior: none; the request is rejected before verification
  // Expected status code: 400
  // Expected behavior: Google is never contacted
  // Expected output: { error: 'Request body must include an "idToken" string' }
  test('Missing idToken', async () => {
    const response = await request(createApp())
      .post('/api/auth/google')
      .send({});

    expect(response.status).toBe(400);
    expect(verifyGoogleIdTokenMock).not.toHaveBeenCalled();
  });

  // Input: a body whose idToken is a number rather than a string
  // Mocked behavior: none; the request is rejected before verification
  // Expected status code: 400
  // Expected behavior: a wrongly typed field is refused, not coerced
  // Expected output: { error: 'Request body must include an "idToken" string' }
  test('Wrongly typed idToken', async () => {
    const response = await request(createApp())
      .post('/api/auth/google')
      .send({ idToken: 12345 });

    expect(response.status).toBe(400);
    expect(verifyGoogleIdTokenMock).not.toHaveBeenCalled();
  });
});
