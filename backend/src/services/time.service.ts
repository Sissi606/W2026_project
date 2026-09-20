const MINUTES_PER_HOUR = 60;

function pad2(value: number): string {
  return value.toString().padStart(2, '0');
}

/**
 * Formats a moment as `hh:mm:ss GMT+hh:mm` in the *server's* local timezone,
 * which is the format the app displays verbatim. Formatting here rather than on
 * the client keeps the two clocks on screen in the same shape.
 *
 * Date#getTimezoneOffset reports minutes *behind* UTC, so a zone ahead of GMT
 * yields a negative number -- hence the flipped sign below.
 */
export function formatLocalTimeWithOffset(date: Date): string {
  const time = `${pad2(date.getHours())}:${pad2(date.getMinutes())}:${pad2(date.getSeconds())}`;

  const offsetMinutes = -date.getTimezoneOffset();
  const sign = offsetMinutes < 0 ? '-' : '+';
  const absolute = Math.abs(offsetMinutes);
  const offsetHours = Math.floor(absolute / MINUTES_PER_HOUR);
  const offsetRemainder = absolute % MINUTES_PER_HOUR;

  return `${time} GMT${sign}${pad2(offsetHours)}:${pad2(offsetRemainder)}`;
}
