/**
 * An error with an HTTP status attached, so services can signal "this is the
 * client's fault" without knowing anything about Express. Anything thrown that
 * is *not* an HttpError becomes a 500 in the error handler.
 */
export class HttpError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'HttpError';
    this.status = status;
  }
}
