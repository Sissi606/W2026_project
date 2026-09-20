import {
  clearPublicIpCache,
  getServerPublicIp,
  normalizeIp,
} from '../../src/services/ip.service';

// Unit under test: normalizeIp
describe('Unmocked: normalizeIp', () => {
  // Input: an IPv4-mapped IPv6 address, as a dual-stack socket reports it
  // Expected behavior: the mapping prefix is unwrapped for readability
  // Expected output: "203.0.113.7"
  test('IPv4-mapped address', () => {
    expect(normalizeIp('::ffff:203.0.113.7')).toBe('203.0.113.7');
  });

  // Input: a genuine IPv6 address
  // Expected behavior: left untouched
  // Expected output: the same address
  test('Native IPv6 address', () => {
    expect(normalizeIp('2001:db8::1')).toBe('2001:db8::1');
  });

  // Input: a plain IPv4 address
  // Expected behavior: left untouched
  // Expected output: the same address
  test('Plain IPv4 address', () => {
    expect(normalizeIp('192.168.1.10')).toBe('192.168.1.10');
  });

  // Input: a string that starts with the mapping prefix but does not wrap an
  //   IPv4 address
  // Expected behavior: not unwrapped, since the result would not be an address
  // Expected output: the input unchanged
  test('Prefix without a valid IPv4 payload', () => {
    expect(normalizeIp('::ffff:not-an-ip')).toBe('::ffff:not-an-ip');
  });
});

// Unit under test: getServerPublicIp, with the outbound lookup mocked
describe('Mocked: getServerPublicIp', () => {
  // Mocks the global fetch, so no network call leaves the test machine.
  const fetchMock = jest.fn();

  beforeEach(() => {
    clearPublicIpCache();
    fetchMock.mockReset();
    global.fetch = fetchMock as unknown as typeof fetch;
  });

  function okResponse(body: string) {
    return { ok: true, status: 200, text: () => Promise.resolve(body) };
  }

  // Input: the echo service returns a valid address
  // Expected behavior: the address is returned and reused from cache, so a
  //   second call makes no further request
  // Expected output: "198.51.100.42", fetch called exactly once
  test('Successful lookup is cached', async () => {
    fetchMock.mockResolvedValue(okResponse('198.51.100.42\n'));

    expect(await getServerPublicIp()).toBe('198.51.100.42');
    expect(await getServerPublicIp()).toBe('198.51.100.42');
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  // Input: the echo service responds with an HTTP error
  // Expected behavior: the failure propagates rather than caching a bad value
  // Expected output: a rejected promise
  test('Echo service returns an error status', async () => {
    fetchMock.mockResolvedValue({ ok: false, status: 503 });

    await expect(getServerPublicIp()).rejects.toThrow('HTTP 503');
  });

  // Input: the echo service responds 200 with something that is not an address
  // Expected behavior: the body is validated, not trusted
  // Expected output: a rejected promise
  test('Echo service returns a non-address body', async () => {
    fetchMock.mockResolvedValue(okResponse('<html>error</html>'));

    await expect(getServerPublicIp()).rejects.toThrow('non-address');
  });
});
