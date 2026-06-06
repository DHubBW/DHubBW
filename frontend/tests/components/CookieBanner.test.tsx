import { Platform } from 'react-native';
import { render, screen, fireEvent } from '@testing-library/react-native';
import React from 'react';

// Mutate the already-initialised Platform object rather than replacing the
// module path — react-native-css-interop reads Platform.OS from the react-native
// export at render time, so replacing the module-path mock leaves it undefined.
const originalOS = Platform.OS;
beforeAll(() => {
  (Platform as { OS: string }).OS = 'web';
});
afterAll(() => {
  (Platform as { OS: string }).OS = originalOS;
});

jest.mock('expo-router', () => ({
  Link: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

const mockAccept = jest.fn();
const mockReject = jest.fn();
let mockConsent = 'unknown';

jest.mock('@/lib/consent/useCookieConsent', () => ({
  useCookieConsent: () => ({
    get consent() {
      return mockConsent;
    },
    accept: mockAccept,
    reject: mockReject,
    resetConsent: jest.fn(),
  }),
}));

import { CookieBanner } from '@/components/CookieBanner';

describe('CookieBanner', () => {
  beforeEach(() => {
    mockConsent = 'unknown';
    mockAccept.mockClear();
    mockReject.mockClear();
  });

  it('renders when consent is unknown on web', () => {
    render(<CookieBanner />);
    expect(screen.getByText('Cookie-Hinweis')).toBeTruthy();
  });

  it('does not render when consent is accepted', () => {
    mockConsent = 'accepted';
    const { toJSON } = render(<CookieBanner />);
    expect(toJSON()).toBeNull();
  });

  it('does not render when consent is rejected', () => {
    mockConsent = 'rejected';
    const { toJSON } = render(<CookieBanner />);
    expect(toJSON()).toBeNull();
  });

  it('calls accept when Akzeptieren is pressed', () => {
    render(<CookieBanner />);
    fireEvent.press(screen.getByText('Akzeptieren'));
    expect(mockAccept).toHaveBeenCalledTimes(1);
  });

  it('calls reject when Ablehnen is pressed (no override)', () => {
    render(<CookieBanner />);
    fireEvent.press(screen.getByText('Ablehnen'));
    expect(mockReject).toHaveBeenCalledTimes(1);
  });

  it('calls onRejectOverride instead of reject when provided', () => {
    const override = jest.fn();
    render(<CookieBanner onRejectOverride={override} />);
    fireEvent.press(screen.getByText('Ablehnen'));
    expect(override).toHaveBeenCalledTimes(1);
    expect(mockReject).not.toHaveBeenCalled();
  });
});
