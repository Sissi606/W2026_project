import { verifyGoogleIdToken } from '../../src/services/google.service';
import { HttpError } from '../../src/types/errors';

// google-auth-library itself is mocked here, which is what lets the service's
// own logic -- audience wiring, the empty-payload guard, the fallbacks for a
// profile with no name -- be exercised without a token only Google can mint.
//
// jest.mock is hoisted above everything else in the file, and the service
// constructs its OAuth2Client at import time, so the factory cannot close over
// a const declared below. It delegates through a `var` (hoisted, so no
// temporal dead zone) that is only dereferenced once a test actually calls it.
// The `mock` name prefix is what lets jest.mock reference an outer variable.
var mockVerifyIdToken: jest.Mock;

jest.mock('google-auth-library', () => ({
  OAuth2Client: jest.fn().mockImplementation(() => ({
    verifyIdToken: (...args: unknown[]) => mockVerifyIdToken(...args),
  })),
}));

mockVerifyIdToken = jest.fn();

function ticketWith(payload: unknown) {
  return { getPayload: () => payload };
}

// Unit under test: verifyGoogleIdToken
describe('Mocked: verifyGoogleIdToken', () => {
  beforeEach(() => {
    mockVerifyIdToken.mockReset();
  });

  // Input: a token Google accepts, for a fully populated profile
  // Mocked behavior: verifyIdToken resolves with a complete payload
  // Expected behavior: the payload is mapped onto our user shape, and the
  //   token is checked against our own client ID as the audience
  // Expected output: the mapped user
  test('Complete profile', async () => {
    mockVerifyIdToken.mockResolvedValue(
      ticketWith({
        sub: '1234567890',
        email: 'ada@example.com',
        given_name: 'Ada',
        family_name: 'Lovelace',
      })
    );

    await expect(verifyGoogleIdToken('a-token')).resolves.toEqual({
      googleId: '1234567890',
      email: 'ada@example.com',
      firstName: 'Ada',
      lastName: 'Lovelace',
    });
    expect(mockVerifyIdToken).toHaveBeenCalledWith({
      idToken: 'a-token',
      audience: process.env.GOOGLE_CLIENT_ID,
    });
  });

  // Input: a token for an account whose Google profile carries no name
  // Mocked behavior: verifyIdToken resolves with sub only
  // Expected behavior: missing name fields become empty strings rather than
  //   undefined, so the app always has something to render
  // Expected output: a user with empty email and name fields
  test('Profile without a name', async () => {
    mockVerifyIdToken.mockResolvedValue(ticketWith({ sub: '1234567890' }));

    await expect(verifyGoogleIdToken('a-token')).resolves.toEqual({
      googleId: '1234567890',
      email: '',
      firstName: '',
      lastName: '',
    });
  });

  // Input: a token the library refuses (bad signature, wrong audience, expiry)
  // Mocked behavior: verifyIdToken rejects
  // Expected behavior: converted to a 401 without the library's message, which
  //   would tell an attacker which check failed
  // Expected output: HttpError with status 401
  test('Library rejects the token', async () => {
    mockVerifyIdToken.mockRejectedValue(new Error('Wrong recipient'));

    await expect(verifyGoogleIdToken('a-token')).rejects.toThrow(HttpError);
    await expect(verifyGoogleIdToken('a-token')).rejects.toThrow(
      'Google ID token verification failed'
    );
  });

  // Input: a token the library accepts but whose ticket carries no payload
  // Mocked behavior: getPayload returns undefined
  // Expected behavior: treated as a failure rather than dereferenced
  // Expected output: HttpError with status 401
  test('Ticket without a payload', async () => {
    mockVerifyIdToken.mockResolvedValue(ticketWith(undefined));

    await expect(verifyGoogleIdToken('a-token')).rejects.toThrow(HttpError);
  });
});
