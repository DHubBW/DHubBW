import { render, screen, fireEvent, waitFor } from '@testing-library/react-native';
import React from 'react';

const mockReplace = jest.fn();
const mockPush = jest.fn();

jest.mock('expo-router', () => ({
  Link: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  useRouter: () => ({ replace: mockReplace, push: mockPush, back: jest.fn() }),
  useLocalSearchParams: () => ({}),
}));

const mockRegister = jest.fn<Promise<void>, [string, string, string]>();

jest.mock('@/lib/auth/AuthContext', () => ({
  useAuth: () => ({
    user: null,
    isLoading: false,
    login: jest.fn(),
    register: mockRegister,
    logout: jest.fn(),
    refresh: jest.fn(),
    setOnCookieDisabled: jest.fn(),
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

// Step 2 lives in `<RegisterStep2 />` and pulls in TanStack Query +
// `fetchClient`. Step-2 behaviour is covered by `register.step2.test.tsx`;
// here we replace the component with a marker so the Step-1 → Step-2
// transition can be asserted via local state without a QueryClientProvider.
jest.mock('@/components/auth/RegisterStep2', () => {
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const { Pressable, Text } = require('react-native');
  return {
    RegisterStep2: ({ onBack }: { onBack: () => void }) => (
      <>
        <Pressable accessibilityLabel="Überspringen">
          <Text>Step 2 Mock</Text>
        </Pressable>
        <Pressable accessibilityLabel="Zurück" onPress={onBack}>
          <Text>Zurück</Text>
        </Pressable>
      </>
    ),
  };
});

import RegisterScreen from '@/app/(auth)/register';
import { ApiError } from '@/lib/api/errors';

const VALID_NAME = 'Max Mustermann';
const VALID_MAIL = 'max.muster.tinf24b2@student.dhbw-karlsruhe.de';

describe('RegisterScreen', () => {
  beforeEach(() => {
    mockReplace.mockClear();
    mockPush.mockClear();
    mockRegister.mockReset();
    mockToastError.mockClear();
    mockConsent = 'accepted';
  });

  it('renders the Step 1 form on mount', () => {
    render(<RegisterScreen />);
    expect(screen.getByLabelText('Vollständiger Name')).toBeTruthy();
    expect(screen.getByLabelText('DHBW-Mail')).toBeTruthy();
    expect(screen.getByLabelText('Passwort')).toBeTruthy();
    expect(screen.getByLabelText('Weiter')).toBeTruthy();
  });

  it('keeps Weiter disabled until all three fields are valid', () => {
    render(<RegisterScreen />);
    const submit = screen.getByLabelText('Weiter');
    expect(submit.props.accessibilityState?.disabled).toBe(true);

    fireEvent.changeText(screen.getByLabelText('Vollständiger Name'), VALID_NAME);
    expect(submit.props.accessibilityState?.disabled).toBe(true);

    fireEvent.changeText(screen.getByLabelText('DHBW-Mail'), VALID_MAIL);
    expect(submit.props.accessibilityState?.disabled).toBe(true);

    fireEvent.changeText(screen.getByLabelText('Passwort'), 'pw1');
    expect(submit.props.accessibilityState?.disabled).toBe(false);
  });

  it('calls register with (mail, name, password) on submit and advances to Step 2', async () => {
    mockRegister.mockResolvedValueOnce(undefined);
    render(<RegisterScreen />);

    fireEvent.changeText(screen.getByLabelText('Vollständiger Name'), `  ${VALID_NAME}  `);
    fireEvent.changeText(screen.getByLabelText('DHBW-Mail'), `  ${VALID_MAIL}  `);
    fireEvent.changeText(screen.getByLabelText('Passwort'), 'pw1');
    fireEvent.press(screen.getByLabelText('Weiter'));

    await waitFor(() => expect(mockRegister).toHaveBeenCalledTimes(1));
    expect(mockRegister).toHaveBeenCalledWith(VALID_MAIL, VALID_NAME, 'pw1');

    // After resolution we are on Step 2 — Skip + Zurück appear, Step 1 inputs gone.
    await waitFor(() => expect(screen.getByLabelText('Überspringen')).toBeTruthy());
    expect(screen.getByLabelText('Zurück')).toBeTruthy();
    expect(screen.queryByLabelText('Vollständiger Name')).toBeNull();

    // Crucially: NO router navigation on Step1→Step2.
    expect(mockReplace).not.toHaveBeenCalled();
    expect(mockPush).not.toHaveBeenCalled();
  });

  it('Step 2 Zurück returns to Step 1 (local state, no navigation)', async () => {
    mockRegister.mockResolvedValueOnce(undefined);
    render(<RegisterScreen />);

    fireEvent.changeText(screen.getByLabelText('Vollständiger Name'), VALID_NAME);
    fireEvent.changeText(screen.getByLabelText('DHBW-Mail'), VALID_MAIL);
    fireEvent.changeText(screen.getByLabelText('Passwort'), 'pw1');
    fireEvent.press(screen.getByLabelText('Weiter'));
    await waitFor(() => screen.getByLabelText('Zurück'));

    fireEvent.press(screen.getByLabelText('Zurück'));
    expect(screen.getByLabelText('Vollständiger Name')).toBeTruthy();
    expect(mockReplace).not.toHaveBeenCalled();
  });

  it('shows a 409-toast and stays on Step 1 when mail already exists', async () => {
    mockRegister.mockRejectedValueOnce(new ApiError(409, 'Already taken'));
    render(<RegisterScreen />);

    fireEvent.changeText(screen.getByLabelText('Vollständiger Name'), VALID_NAME);
    fireEvent.changeText(screen.getByLabelText('DHBW-Mail'), VALID_MAIL);
    fireEvent.changeText(screen.getByLabelText('Passwort'), 'pw1');
    fireEvent.press(screen.getByLabelText('Weiter'));

    await waitFor(() =>
      expect(mockToastError).toHaveBeenCalledWith('E-Mail ist bereits registriert.'),
    );
    expect(screen.getByLabelText('Vollständiger Name')).toBeTruthy();
    expect(screen.queryByLabelText('Überspringen')).toBeNull();
  });

  it('shows the backend message on 422', async () => {
    mockRegister.mockRejectedValueOnce(new ApiError(422, 'Invalid name'));
    render(<RegisterScreen />);

    fireEvent.changeText(screen.getByLabelText('Vollständiger Name'), VALID_NAME);
    fireEvent.changeText(screen.getByLabelText('DHBW-Mail'), VALID_MAIL);
    fireEvent.changeText(screen.getByLabelText('Passwort'), 'pw1');
    fireEvent.press(screen.getByLabelText('Weiter'));

    await waitFor(() => expect(mockToastError).toHaveBeenCalledWith('Invalid name'));
  });

  it('shows a network-error toast on status 0', async () => {
    mockRegister.mockRejectedValueOnce(new ApiError(0, 'Network error'));
    render(<RegisterScreen />);

    fireEvent.changeText(screen.getByLabelText('Vollständiger Name'), VALID_NAME);
    fireEvent.changeText(screen.getByLabelText('DHBW-Mail'), VALID_MAIL);
    fireEvent.changeText(screen.getByLabelText('Passwort'), 'pw1');
    fireEvent.press(screen.getByLabelText('Weiter'));

    await waitFor(() =>
      expect(mockToastError).toHaveBeenCalledWith(
        'Netzwerkfehler. Bitte erneut versuchen.',
      ),
    );
  });

  it('toggles password visibility via the eye button', () => {
    render(<RegisterScreen />);
    const pw = screen.getByLabelText('Passwort');
    expect(pw.props.secureTextEntry).toBe(true);

    fireEvent.press(screen.getByLabelText('Passwort anzeigen'));
    expect(pw.props.secureTextEntry).toBe(false);
  });

  it('disables Weiter when consent is rejected, even with valid inputs', () => {
    mockConsent = 'rejected';
    render(<RegisterScreen />);

    fireEvent.changeText(screen.getByLabelText('Vollständiger Name'), VALID_NAME);
    fireEvent.changeText(screen.getByLabelText('DHBW-Mail'), VALID_MAIL);
    fireEvent.changeText(screen.getByLabelText('Passwort'), 'pw1');

    expect(
      screen.getByLabelText('Weiter').props.accessibilityState?.disabled,
    ).toBe(true);
  });

  it('caps the password input at 256 characters', () => {
    render(<RegisterScreen />);
    expect(screen.getByLabelText('Passwort').props.maxLength).toBe(256);
  });

  it('fires only one register call on rapid double-submit', async () => {
    let resolveRegister: (() => void) | undefined;
    mockRegister.mockImplementationOnce(
      () =>
        new Promise<void>((resolve) => {
          resolveRegister = resolve;
        }),
    );
    render(<RegisterScreen />);

    fireEvent.changeText(screen.getByLabelText('Vollständiger Name'), VALID_NAME);
    fireEvent.changeText(screen.getByLabelText('DHBW-Mail'), VALID_MAIL);
    fireEvent.changeText(screen.getByLabelText('Passwort'), 'pw1');

    const submit = screen.getByLabelText('Weiter');
    fireEvent.press(submit);
    fireEvent.press(submit);

    expect(mockRegister).toHaveBeenCalledTimes(1);

    resolveRegister?.();
    await waitFor(() => screen.getByLabelText('Zurück'));
    expect(mockRegister).toHaveBeenCalledTimes(1);
  });
});
