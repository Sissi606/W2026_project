import { formatLocalTimeWithOffset } from '../../src/services/time.service';

// Unit under test: formatLocalTimeWithOffset
describe('Unmocked: formatLocalTimeWithOffset', () => {
  // The server's real timezone varies by machine, so the offset is stubbed and
  // the clock fields are read from a fixed local-time Date.
  function dateWithOffset(
    local: string,
    offsetMinutesBehindUtc: number
  ): Date {
    const date = new Date(local);
    jest
      .spyOn(Date.prototype, 'getTimezoneOffset')
      .mockReturnValue(offsetMinutesBehindUtc);
    return date;
  }

  afterEach(() => {
    jest.restoreAllMocks();
  });

  // Input: a local time of 09:05:03 in a zone 7 hours behind UTC
  // Expected behavior: 24-hour clock, every field zero-padded to two digits
  // Expected output: "09:05:03 GMT-07:00"
  test('Zone behind UTC', () => {
    expect(formatLocalTimeWithOffset(dateWithOffset('2026-09-19T09:05:03', 420)))
      .toBe('09:05:03 GMT-07:00');
  });

  // Input: a local time of 23:59:59 in a zone 2 hours ahead of UTC
  // Expected behavior: afternoon times stay in 24-hour form, offset sign flips
  // Expected output: "23:59:59 GMT+02:00"
  test('Zone ahead of UTC', () => {
    expect(formatLocalTimeWithOffset(dateWithOffset('2026-09-19T23:59:59', -120)))
      .toBe('23:59:59 GMT+02:00');
  });

  // Input: a local time in a zone offset by a non-whole number of hours
  // Expected behavior: the leftover minutes appear rather than being truncated
  // Expected output: "12:00:00 GMT+05:45"
  test('Fractional-hour offset', () => {
    expect(formatLocalTimeWithOffset(dateWithOffset('2026-09-19T12:00:00', -345)))
      .toBe('12:00:00 GMT+05:45');
  });

  // Input: a local time at UTC itself
  // Expected behavior: a zero offset is rendered as "+00:00", never "-00:00"
  // Expected output: "00:00:00 GMT+00:00"
  test('Zero offset', () => {
    expect(formatLocalTimeWithOffset(dateWithOffset('2026-09-19T00:00:00', 0)))
      .toBe('00:00:00 GMT+00:00');
  });
});
