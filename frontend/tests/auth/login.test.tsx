import { render, screen, fireEvent, waitFor } from '@testing-library/react-native';
import React from 'react';
import { Platform } from 'react-native';

// Web-mode is the realistic test target: that's where the Cookie-Banner +
// `setOnCookieDisabled`-toast wiring lives. Native mode short-circuits both.
const originalOS = Platform.OS;
beforeAll(() => {
  (Platform as { OS: string }).OS = 'web';
});
afterAll(() => {
  (Platform as { OS: string }).OS = originalOS;
});

// ---- Mocks ----------------------------------------------------------------

const mockReplace = jest.fn();
const mockPush = jest.fn();
let mockParams: Record<string, string | string[] | undefined> = {};

jest.mock('expo-router', () => ({
  Link: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  router: {
    replace: (...args: unknown[]) => mockReplace(...args),
    push: (...args: unknown[]) => mockPush(...args),
  },
  useRouter: () => ({ replace: mockReplace, push: mockPush, back: jest.fn() }),
  useLocalSearchParams: () => mockParams,
}));

const mockLogin = jest.fn<Promise<void>, [string, string]>();
const mockSetOnCookieDisabled = jest.fn();

jest.mock('@/lib/auth/AuthContext', () => ({
  useAuth: () => ({
    user: null,
    isLoading: false,
    login: mockLogin,
    register: jest.fn(),
    logout: jest.fn(),
    refresh: jest.fn(),
    setOnCookieDisabled: mockSetOnCookieDisabled,
  }),
}));

let mockConsent: 'unknown' | 'accepted' | 'rejected' = 'accepted';
jest.mock('@/lib/consent/useCookieConsent', () => ({
  useCookieConsent: () => ({
    get consent() {
      return mockConsent;
    },
    accept: jest.fn(),
    reject: jest.fn(),
    resetConsent: jest.fn(),
  }),
}));

const mockToastError = jest.fn();
jest.mock('sonner-native', () => ({
  toast: {
    error: (...args: unknown[]) => mockToastError(...args),
    success: jest.fn(),
  },
  Toaster: () => null,
}));

// useExpiredReason — keep the real hook, but neutralise its only external
// dependency (`useLocalSearchParams`) via the expo-router mock above.
// This way we exercise the full screen including the hook wiring.

import LoginScreen from '@/app/(auth)/login';
import { ApiError } from '@/lib/api/errors';

describe('LoginScreen', () => {
  beforeEach(() => {
    mockReplace.mockClear();
    mockPush.mockClear();
    mockLogin.mockReset();
    mockSetOnCookieDisabled.mockClear();
    mockToastError.mockClear();
    mockConsent = 'accepted';
    mockParams = {};
  });

  it('renders the form', () => {
    render(<LoginScreen />);
    expect(screen.getByLabelText('DHBW-Mail')).toBeTruthy();
    expect(screen.getByLabelText('Passwort')).toBeTruthy();
    expect(screen.getByLabelText('Anmelden')).toBeTruthy();
  });

  it('keeps the submit button disabled until mail+password are valid', () => {
    render(<LoginScreen />);
    const submit = screen.getByLabelText('Anmelden');
    expect(submit.props.accessibilityState?.disabled).toBe(true);

    fireEvent.changeText(
      screen.getByLabelText('DHBW-Mail'),
      'max.muster.tinf24b2@student.dhbw-karlsruhe.de',
    );
    fireEvent.changeText(screen.getByLabelText('Passwort'), 'pw1');
    expect(submit.props.accessibilityState?.disabled).toBe(false);
  });

  it('disables submit when consent is rejected, even with valid inputs', () => {
    mockConsent = 'rejected';
    render(<LoginScreen />);

    fireEvent.changeText(
      screen.getByLabelText('DHBW-Mail'),
      'max.muster.tinf24b2@student.dhbw-karlsruhe.de',
    );
    fireEvent.changeText(screen.getByLabelText('Passwort'), 'pw1');
    expect(
      screen.getByLabelText('Anmelden').props.accessibilityState?.disabled,
    ).toBe(true);
  });

  it('calls login with trimmed mail + password on submit', async () => {
    mockLogin.mockResolvedValueOnce(undefined);
    render(<LoginScreen />);

    fireEvent.changeText(
      screen.getByLabelText('DHBW-Mail'),
      '  max.muster.tinf24b2@student.dhbw-karlsruhe.de  ',
    );
    fireEvent.changeText(screen.getByLabelText('Passwort'), 'pw1');
    fireEvent.press(screen.getByLabelText('Anmelden'));

    await waitFor(() => expect(mockLogin).toHaveBeenCalledTimes(1));
    expect(mockLogin).toHaveBeenCalledWith(
      'max.muster.tinf24b2@student.dhbw-karlsruhe.de',
      'pw1',
    );
  });

  it('redirects to the dashboard fallback when no returnTo is set', async () => {
    mockLogin.mockResolvedValueOnce(undefined);
    render(<LoginScreen />);

    fireEvent.changeText(
      screen.getByLabelText('DHBW-Mail'),
      'max.muster.tinf24b2@student.dhbw-karlsruhe.de',
    );
    fireEvent.changeText(screen.getByLabelText('Passwort'), 'pw1');
    fireEvent.press(screen.getByLabelText('Anmelden'));

    await waitFor(() => expect(mockReplace).toHaveBeenCalledTimes(1));
    expect(mockReplace).toHaveBeenCalledWith('/(app)/dashboard');
    expect(mockPush).not.toHaveBeenCalled();
  });

  it('honours a whitelisted ?returnTo on successful login', async () => {
    mockParams = { returnTo: '/(app)/mensa' };
    mockLogin.mockResolvedValueOnce(undefined);
    render(<LoginScreen />);

    fireEvent.changeText(
      screen.getByLabelText('DHBW-Mail'),
      'max.muster.tinf24b2@student.dhbw-karlsruhe.de',
    );
    fireEvent.changeText(screen.getByLabelText('Passwort'), 'pw1');
    fireEvent.press(screen.getByLabelText('Anmelden'));

    await waitFor(() => expect(mockReplace).toHaveBeenCalledTimes(1));
    expect(mockReplace).toHaveBeenCalledWith('/(app)/mensa');
  });

  it('falls back to the dashboard when ?returnTo fails the open-redirect whitelist', async () => {
    mockParams = { returnTo: '//evil.com' };
    mockLogin.mockResolvedValueOnce(undefined);
    render(<LoginScreen />);

    fireEvent.changeText(
      screen.getByLabelText('DHBW-Mail'),
      'max.muster.tinf24b2@student.dhbw-karlsruhe.de',
    );
    fireEvent.changeText(screen.getByLabelText('Passwort'), 'pw1');
    fireEvent.press(screen.getByLabelText('Anmelden'));

    await waitFor(() => expect(mockReplace).toHaveBeenCalledTimes(1));
    expect(mockReplace).toHaveBeenCalledWith('/(app)/dashboard');
  });

  it('caps the password input at 256 characters', () => {
    render(<LoginScreen />);
    expect(screen.getByLabelText('Passwort').props.maxLength).toBe(256);
  });

  it('fires only one login call on rapid double-submit', async () => {
    let resolveLogin: (() => void) | undefined;
    mockLogin.mockImplementationOnce(
      () =>
        new Promise<void>((resolve) => {
          resolveLogin = resolve;
        }),
    );
    render(<LoginScreen />);

    fireEvent.changeText(
      screen.getByLabelText('DHBW-Mail'),
      'max.muster.tinf24b2@student.dhbw-karlsruhe.de',
    );
    fireEvent.changeText(screen.getByLabelText('Passwort'), 'pw1');

    const submit = screen.getByLabelText('Anmelden');
    fireEvent.press(submit);
    fireEvent.press(submit); // second tap before the first promise resolves

    expect(mockLogin).toHaveBeenCalledTimes(1);

    resolveLogin?.();
    await waitFor(() => expect(mockLogin).toHaveBeenCalledTimes(1));
  });

  it('shows a "wrong credentials" toast on 401', async () => {
    mockLogin.mockRejectedValueOnce(new ApiError(401, 'Unauthorized'));
    render(<LoginScreen />);

    fireEvent.changeText(
      screen.getByLabelText('DHBW-Mail'),
      'max.muster.tinf24b2@student.dhbw-karlsruhe.de',
    );
    fireEvent.changeText(screen.getByLabelText('Passwort'), 'wrong');
    fireEvent.press(screen.getByLabelText('Anmelden'));

    await waitFor(() => expect(mockToastError).toHaveBeenCalledWith('Falsche Zugangsdaten.'));
  });

  it('shows the backend message on 422', async () => {
    mockLogin.mockRejectedValueOnce(
      new ApiError(422, 'Invalid mail format'),
    );
    render(<LoginScreen />);

    fireEvent.changeText(
      screen.getByLabelText('DHBW-Mail'),
      'max.muster.tinf24b2@student.dhbw-karlsruhe.de',
    );
    fireEvent.changeText(screen.getByLabelText('Passwort'), 'pw1');
    fireEvent.press(screen.getByLabelText('Anmelden'));

    await waitFor(() => expect(mockToastError).toHaveBeenCalledWith('Invalid mail format'));
  });

  it('shows a network-error toast on status 0', async () => {
    mockLogin.mockRejectedValueOnce(new ApiError(0, 'Network error'));
    render(<LoginScreen />);

    fireEvent.changeText(
      screen.getByLabelText('DHBW-Mail'),
      'max.muster.tinf24b2@student.dhbw-karlsruhe.de',
    );
    fireEvent.changeText(screen.getByLabelText('Passwort'), 'pw1');
    fireEvent.press(screen.getByLabelText('Anmelden'));

    await waitFor(() =>
      expect(mockToastError).toHaveBeenCalledWith(
        'Netzwerkfehler. Bitte erneut versuchen.',
      ),
    );
  });

  it('registers and unregisters the cookie-disabled callback', () => {
    const { unmount } = render(<LoginScreen />);
    // Mount → register a function
    expect(mockSetOnCookieDisabled).toHaveBeenCalledTimes(1);
    expect(typeof mockSetOnCookieDisabled.mock.calls[0][0]).toBe('function');

    unmount();
    // Unmount → cleanup with null
    expect(mockSetOnCookieDisabled).toHaveBeenLastCalledWith(null);
  });

  it('fires the expired-toast when ?reason=expired is in the URL', async () => {
    mockParams = { reason: 'expired' };
    render(<LoginScreen />);
    await waitFor(() =>
      expect(mockToastError).toHaveBeenCalledWith(
        'Deine Sitzung ist abgelaufen. Bitte erneut anmelden.',
      ),
    );
  });

  it('toggles password visibility via the eye button', () => {
    render(<LoginScreen />);
    const pw = screen.getByLabelText('Passwort');
    expect(pw.props.secureTextEntry).toBe(true);

    fireEvent.press(screen.getByLabelText('Passwort anzeigen'));
    expect(pw.props.secureTextEntry).toBe(false);
    expect(screen.getByLabelText('Passwort verbergen')).toBeTruthy();
  });
});
