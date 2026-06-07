import { resolveReturnTo } from '@/lib/auth/resolveReturnTo';

describe('resolveReturnTo', () => {
  const FALLBACK = '/(app)/dashboard';

  it('returns the fallback when params.returnTo is undefined', () => {
    expect(resolveReturnTo({})).toBe(FALLBACK);
    expect(resolveReturnTo({ returnTo: undefined })).toBe(FALLBACK);
  });

  it('returns the fallback for the empty string', () => {
    expect(resolveReturnTo({ returnTo: '' })).toBe(FALLBACK);
  });

  it('decodes percent-encoded in-app paths and accepts them', () => {
    // Encoded `/(app)/mensa` — Expo Router routes use `(group)`-segments
    // whose parens are URL-encoded as `%28` / `%29`.
    expect(resolveReturnTo({ returnTo: '%2F%28app%29%2Fmensa' })).toBe(
      '/(app)/mensa',
    );
  });

  it('accepts a plain in-app path without percent-encoding', () => {
    expect(resolveReturnTo({ returnTo: '/dashboard' })).toBe('/dashboard');
  });

  it('takes the first value when returnTo is an array', () => {
    expect(resolveReturnTo({ returnTo: ['/profile', '/evil'] })).toBe('/profile');
  });

  it('rejects protocol-relative URLs (//evil.com)', () => {
    expect(resolveReturnTo({ returnTo: '//evil.com' })).toBe(FALLBACK);
    expect(resolveReturnTo({ returnTo: '//evil.com/path' })).toBe(FALLBACK);
  });

  it('rejects absolute URLs (https://evil.com)', () => {
    expect(resolveReturnTo({ returnTo: 'https://evil.com' })).toBe(FALLBACK);
    expect(resolveReturnTo({ returnTo: 'http://evil.com/path' })).toBe(FALLBACK);
  });

  it('rejects encoded protocol-relative URLs', () => {
    // %2F%2Fevil.com decodes to `//evil.com` — must still be rejected.
    expect(resolveReturnTo({ returnTo: '%2F%2Fevil.com' })).toBe(FALLBACK);
  });

  it('rejects values that do not start with /', () => {
    expect(resolveReturnTo({ returnTo: 'dashboard' })).toBe(FALLBACK);
    expect(resolveReturnTo({ returnTo: 'javascript:alert(1)' })).toBe(FALLBACK);
  });

  it('falls back when decodeURIComponent throws on a malformed value', () => {
    // Lone `%` with no two hex digits triggers URIError.
    expect(resolveReturnTo({ returnTo: '%E0%A4%A' })).toBe(FALLBACK);
  });

  it('rejects paths containing a backslash', () => {
    // Some URL parsers treat `\` as `/`, so `/\evil.com` could leak as
    // `//evil.com`. Defense-in-depth: refuse outright.
    expect(resolveReturnTo({ returnTo: '/\\evil.com' })).toBe(FALLBACK);
    expect(resolveReturnTo({ returnTo: '%2F%5Cevil.com' })).toBe(FALLBACK);
  });

  it('rejects paths containing a NUL byte', () => {
    expect(resolveReturnTo({ returnTo: '/dashboard\0evil' })).toBe(FALLBACK);
    expect(resolveReturnTo({ returnTo: '%2Fdashboard%00evil' })).toBe(FALLBACK);
  });
});
