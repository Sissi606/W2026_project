import jwt from 'jsonwebtoken';

import {
  signSessionToken,
  verifySessionToken,
} from '../../src/services/token.service';
import { HttpError } from '../../src/types/errors';

const user = {
  googleId: '1234567890',
  email: 'ada@example.com',
  firstName: 'Ada',
  lastName: 'Lovelace',
};

// Unit under test: signSessionToken / verifySessionToken
describe('Unmocked: session tokens', () => {
  // Input: a user signed into a token, then verified back out
  // Expected behavior: every identity field survives the round trip
  // Expected output: the original user object
  test('Round trip', () => {
    expect(verifySessionToken(signSessionToken(user))).toEqual(user);
  });

  // Input: a token signed with a different secret
  // Expected behavior: rejected; a valid-looking token from elsewhere is not
  //   accepted just because it parses
  // Expected output: HttpError with status 401
  test('Wrong signing secret', () => {
    const forged = jwt.sign({ sub: user.googleId }, 'some-other-secret');

    expect(() => verifySessionToken(forged)).toThrow(HttpError);
    expect(() => verifySessionToken(forged)).toThrow('Invalid or expired token');
  });

  // Input: a token that expired an hour ago
  // Expected behavior: rejected on expiry alone
  // Expected output: HttpError with status 401
  test('Expired token', () => {
    const secret = process.env.JWT_SECRET ?? '';
    const expired = jwt.sign({ sub: user.googleId }, secret, {
      expiresIn: '-1h',
    });

    expect(() => verifySessionToken(expired)).toThrow(HttpError);
  });

  // Input: a string that is not a JWT at all
  // Expected behavior: rejected without the library's parse error escaping
  // Expected output: HttpError with status 401
  test('Malformed token', () => {
    try {
      verifySessionToken('not-a-token');
      throw new Error('expected verifySessionToken to throw');
    } catch (error) {
      expect(error).toBeInstanceOf(HttpError);
      expect((error as HttpError).status).toBe(401);
    }
  });
});
