import { Platform } from 'react-native';
import { render, screen, fireEvent } from '@testing-library/react-native';
import React from 'react';

// Mutate the already-initialised Platform object — see CookieBanner.test.tsx
// for rationale.
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

const mockResetConsent = jest.fn();

jest.mock('@/lib/consent/useCookieConsent', () => ({
  useCookieConsent: () => ({
    consent: 'accepted',
    accept: jest.fn(),
    reject: jest.fn(),
    resetConsent: mockResetConsent,
  }),
}));

import { Footer } from '@/components/layout/Footer';

describe('Footer', () => {
  beforeEach(() => {
    mockResetConsent.mockClear();
  });

  it('renders Impressum and Datenschutz links', () => {
    render(<Footer />);
    expect(screen.getByText('Impressum')).toBeTruthy();
    expect(screen.getByText('Datenschutz')).toBeTruthy();
  });

  it('renders Cookie-Einstellungen on web', () => {
    render(<Footer />);
    expect(screen.getByText('Cookie-Einstellungen')).toBeTruthy();
  });

  it('calls resetConsent when Cookie-Einstellungen is pressed', () => {
    render(<Footer />);
    fireEvent.press(screen.getByText('Cookie-Einstellungen'));
    expect(mockResetConsent).toHaveBeenCalledTimes(1);
  });
});
