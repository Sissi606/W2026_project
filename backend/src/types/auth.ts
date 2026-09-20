/** The authenticated user, as carried in our JWT and attached to requests. */
export interface AuthenticatedUser {
  /** Google's stable per-account subject identifier. */
  googleId: string;
  email: string;
  firstName: string;
  lastName: string;
}

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Express {
    interface Request {
      user?: AuthenticatedUser;
    }
  }
}
