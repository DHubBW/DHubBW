import { render } from '@testing-library/react-native';
import React from 'react';

const mockPathname = jest.fn();
const mockParams = jest.fn();
const mockRedirect = jest.fn<null, [{ href: string }]>();

jest.mock('expo-router', () => ({
  Redirect: (props: { href: string }) => {
    mockRedirect(props);
    return null;
  },
  Stack: ({ children }: { children?: React.ReactNode }) => <>{children}</>,
  usePathname: () => mockPathname(),
  useGlobalSearchParams: () => mockParams(),
}));

const mockUseAuth = jest.fn();
jest.mock('@/lib/auth/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}));

import AuthGroupLayout from '@/app/(auth)/_layout';

describe('AuthGroupLayout', () => {
  beforeEach(() => {
    mockPathname.mockReset();
    mockParams.mockReset();
    mockRedirect.mockReset();
    mockUseAuth.mockReset();
    mockParams.mockReturnValue({});
  });

  it('renders the auth stack when no user is authenticated', () => {
    mockUseAuth.mockReturnValue({ isLoading: false, user: null });
    mockPathname.mockReturnValue('/login');
    render(<AuthGroupLayout />);
    expect(mockRedirect).not.toHaveBeenCalled();
  });

  it('renders a splash placeholder while auth is still loading', () => {
    mockUseAuth.mockReturnValue({ isLoading: true, user: null });
    mockPathname.mockReturnValue('/login');
    const { getByText } = render(<AuthGroupLayout />);
    expect(getByText('Lade…')).toBeTruthy();
    expect(mockRedirect).not.toHaveBeenCalled();
  });

  it('redirects an authenticated user from /login to the dashboard fallback', () => {
    mockUseAuth.mockReturnValue({
      isLoading: false,
      user: { dhbwMail: 'x@student.dhbw-karlsruhe.de' },
    });
    mockPathname.mockReturnValue('/login');
    render(<AuthGroupLayout />);
    expect(mockRedirect).toHaveBeenCalledWith({ href: '/(app)/dashboard' });
  });

  it('honours a whitelisted ?returnTo on /login', () => {
    mockUseAuth.mockReturnValue({
      isLoading: false,
      user: { dhbwMail: 'x@student.dhbw-karlsruhe.de' },
    });
    mockPathname.mockReturnValue('/login');
    mockParams.mockReturnValue({ returnTo: '/(app)/mensa' });
    render(<AuthGroupLayout />);
    expect(mockRedirect).toHaveBeenCalledWith({ href: '/(app)/mensa' });
  });

  it('falls back to dashboard when ?returnTo fails the open-redirect whitelist', () => {
    mockUseAuth.mockReturnValue({
      isLoading: false,
      user: { dhbwMail: 'x@student.dhbw-karlsruhe.de' },
    });
    mockPathname.mockReturnValue('/login');
    mockParams.mockReturnValue({ returnTo: '//evil.com' });
    render(<AuthGroupLayout />);
    expect(mockRedirect).toHaveBeenCalledWith({ href: '/(app)/dashboard' });
  });

  it('does not redirect an authenticated user away from /register (Step-2 exemption)', () => {
    mockUseAuth.mockReturnValue({
      isLoading: false,
      user: { dhbwMail: 'x@student.dhbw-karlsruhe.de' },
    });
    mockPathname.mockReturnValue('/register');
    render(<AuthGroupLayout />);
    expect(mockRedirect).not.toHaveBeenCalled();
  });
});
