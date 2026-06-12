import {
  QueryClient,
  QueryClientProvider,
  useQuery,
} from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import React from 'react';

const mockReplace = jest.fn();
const mockPush = jest.fn();

jest.mock('expo-router', () => ({
  Link: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  useRouter: () => ({ replace: mockReplace, push: mockPush, back: jest.fn() }),
  useLocalSearchParams: () => ({}),
}));

const mockToastError = jest.fn();
const mockToastSuccess = jest.fn();
jest.mock('sonner-native', () => ({
  toast: {
    error: (...args: unknown[]) => mockToastError(...args),
    success: (...args: unknown[]) => mockToastSuccess(...args),
  },
  Toaster: () => null,
}));

const mockFetchClient = jest.fn();
jest.mock('@/lib/api/fetchClient', () => ({
  fetchClient: (...args: unknown[]) => mockFetchClient(...args),
  setUnauthorizedHandler: jest.fn(() => () => {}),
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

// `@rn-primitives/dialog` reaches into `react-native-reanimated`'s FlatList
// path, which the global jest.setup mock does not fully cover (missing
// `createSerializable` shim). The test only cares about visibility and
// child layout, so a flat passthrough is enough.
jest.mock('@/components/ui/dialog', () => {
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const { View } = require('react-native');
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const Pass = ({ children }: any) => <View>{children}</View>;
  return {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    Dialog: ({ open, children }: any) =>
      open === false ? null : <View>{children}</View>,
    DialogContent: Pass,
    DialogHeader: Pass,
    DialogTitle: Pass,
    DialogDescription: Pass,
    DialogFooter: Pass,
    DialogClose: Pass,
    DialogOverlay: Pass,
    DialogPortal: Pass,
    DialogTrigger: Pass,
  };
});

// `useQuery` is wrapped so individual tests can stub the catalogue load
// without spinning up a real network mock — fetchClient is the boundary,
// and useQuery's only job here is to expose the data to the component.
jest.mock('@tanstack/react-query', () => {
  const actual = jest.requireActual('@tanstack/react-query');
  return {
    ...actual,
    useQuery: jest.fn(),
  };
});

import { RegisterStep2 } from '@/components/auth/RegisterStep2';
import { ApiError } from '@/lib/api/errors';

const mockUseQuery = useQuery as jest.MockedFunction<typeof useQuery>;

const SAMPLE_KURSE = [
  { kursID: 'k-1', kursName: 'TINF24B2', jahrgang: '2024' },
  { kursID: 'k-2', kursName: 'TINF24A1', jahrgang: '2024' },
  { kursID: 'k-3', kursName: 'TINF23B1', jahrgang: '2023' },
];

function renderStep2(onBack: () => void = jest.fn()) {
  // A throwaway QueryClient keeps the Provider chain happy even though
  // `useQuery` itself is mocked — TanStack pulls a context value during
  // render-time fast paths.
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <RegisterStep2 onBack={onBack} />
    </QueryClientProvider>,
  );
}

function stubQuery<T>(overrides: Partial<{
  data: T;
  isPending: boolean;
  isError: boolean;
  error: unknown;
  refetch: () => Promise<unknown>;
}> = {}) {
  const refetch = overrides.refetch ?? jest.fn(() => Promise.resolve({}));
  mockUseQuery.mockReturnValue({
    data: overrides.data,
    isPending: overrides.isPending ?? false,
    isError: overrides.isError ?? false,
    error: overrides.error ?? null,
    refetch,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } as any);
}

describe('RegisterStep2', () => {
  beforeEach(() => {
    mockReplace.mockClear();
    mockPush.mockClear();
    mockToastError.mockClear();
    mockToastSuccess.mockClear();
    mockFetchClient.mockReset();
    mockUseQuery.mockReset();
    mockConsent = 'accepted';
  });

  it('renders a loading spinner while the catalogue is pending', () => {
    stubQuery({ isPending: true });
    renderStep2();
    expect(screen.getByLabelText('Kurse werden geladen')).toBeTruthy();
  });

  it('Skip works while the catalogue is still loading', () => {
    stubQuery({ isPending: true });
    renderStep2();
    fireEvent.press(screen.getByLabelText('Überspringen'));
    expect(mockReplace).toHaveBeenCalledWith('/(app)/dashboard');
  });

  it('Zurück triggers onBack and does not navigate', () => {
    const onBack = jest.fn();
    stubQuery({ data: SAMPLE_KURSE });
    renderStep2(onBack);
    fireEvent.press(screen.getByLabelText('Zurück'));
    expect(onBack).toHaveBeenCalledTimes(1);
    expect(mockReplace).not.toHaveBeenCalled();
  });

  it('Skip → router.replace(/(app)/dashboard) once data is loaded', () => {
    stubQuery({ data: SAMPLE_KURSE });
    renderStep2();
    fireEvent.press(screen.getByLabelText('Überspringen'));
    expect(mockReplace).toHaveBeenCalledWith('/(app)/dashboard');
  });

  it('renders the catalogue and disables join until a course is selected', () => {
    stubQuery({ data: SAMPLE_KURSE });
    renderStep2();

    expect(screen.getByLabelText('TINF24B2, Jahrgang 2024')).toBeTruthy();
    expect(screen.getByLabelText('TINF24A1, Jahrgang 2024')).toBeTruthy();
    expect(screen.getByLabelText('TINF23B1, Jahrgang 2023')).toBeTruthy();

    const join = screen.getByLabelText('Beitrittsanfrage senden');
    expect(join.props.accessibilityState?.disabled).toBe(true);
  });

  it('filters the catalogue client-side via the search input', () => {
    stubQuery({ data: SAMPLE_KURSE });
    renderStep2();

    fireEvent.changeText(screen.getByLabelText('Kurs suchen'), '23');
    expect(screen.queryByLabelText('TINF24B2, Jahrgang 2024')).toBeNull();
    expect(screen.queryByLabelText('TINF24A1, Jahrgang 2024')).toBeNull();
    expect(screen.getByLabelText('TINF23B1, Jahrgang 2023')).toBeTruthy();
  });

  it('selects a course, posts to /student/kurse/beitreten/{kursId} and replaces to dashboard', async () => {
    mockFetchClient.mockResolvedValueOnce({});
    stubQuery({ data: SAMPLE_KURSE });
    renderStep2();

    fireEvent.press(screen.getByLabelText('TINF24B2, Jahrgang 2024'));
    fireEvent.press(screen.getByLabelText('Beitrittsanfrage senden'));

    await waitFor(() => expect(mockFetchClient).toHaveBeenCalledTimes(1));
    expect(mockFetchClient).toHaveBeenCalledWith(
      '/student/kurse/beitreten/k-1',
      { method: 'POST' },
    );
    await waitFor(() =>
      expect(mockToastSuccess).toHaveBeenCalledWith('Beitrittsanfrage gesendet'),
    );
    expect(mockReplace).toHaveBeenCalledWith('/(app)/dashboard');
  });

  it('surfaces a 409-toast and stays on the screen on conflict', async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(409, 'Conflict'));
    stubQuery({ data: SAMPLE_KURSE });
    renderStep2();

    fireEvent.press(screen.getByLabelText('TINF24A1, Jahrgang 2024'));
    fireEvent.press(screen.getByLabelText('Beitrittsanfrage senden'));

    await waitFor(() =>
      expect(mockToastError).toHaveBeenCalledWith(
        'Bereits in Kurs oder offene Anfrage',
      ),
    );
    expect(mockReplace).not.toHaveBeenCalled();
  });

  it('surfaces a network-error toast on status 0', async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(0, 'Network error'));
    stubQuery({ data: SAMPLE_KURSE });
    renderStep2();

    fireEvent.press(screen.getByLabelText('TINF24B2, Jahrgang 2024'));
    fireEvent.press(screen.getByLabelText('Beitrittsanfrage senden'));

    await waitFor(() =>
      expect(mockToastError).toHaveBeenCalledWith(
        'Netzwerkfehler. Bitte erneut versuchen.',
      ),
    );
    expect(mockReplace).not.toHaveBeenCalled();
  });

  it('renders an error block with retry when the catalogue fails to load', async () => {
    const refetch = jest.fn(() => Promise.resolve({}));
    stubQuery({
      isError: true,
      error: new ApiError(0, 'Network error'),
      refetch,
    });
    renderStep2();

    expect(screen.getByLabelText('Erneut laden')).toBeTruthy();
    fireEvent.press(screen.getByLabelText('Erneut laden'));
    expect(refetch).toHaveBeenCalledTimes(1);

    // Skip is still reachable on the error branch.
    fireEvent.press(screen.getByLabelText('Überspringen'));
    expect(mockReplace).toHaveBeenCalledWith('/(app)/dashboard');
  });

  it('opens the new-course dialog from the toolbar button', () => {
    stubQuery({ data: SAMPLE_KURSE });
    renderStep2();

    // Before opening the dialog the inputs are not mounted.
    expect(screen.queryByLabelText('Kursname')).toBeNull();
    fireEvent.press(screen.getByLabelText('Neuen Kurs anlegen'));
    // Dialog inputs appear once the dialog mounts.
    expect(screen.getByLabelText('Kursname')).toBeTruthy();
    expect(screen.getByLabelText('Jahrgang')).toBeTruthy();
    expect(screen.getByLabelText('Anfrage senden')).toBeTruthy();
  });

  it('NewKursDialog: surfaces the soft cohort-pattern warning for a non-matching kursName', () => {
    stubQuery({ data: SAMPLE_KURSE });
    renderStep2();

    fireEvent.press(screen.getByLabelText('Neuen Kurs anlegen'));
    // "AB!" contains '!' which is outside the allowed character set,
    // so the warning should render. (toUpperCase() leaves '!' untouched.)
    fireEvent.changeText(screen.getByLabelText('Kursname'), 'AB!');
    fireEvent.changeText(screen.getByLabelText('Jahrgang'), '2024');

    // Value misses the BE pattern → warning is rendered.
    expect(screen.getByText(/Hinweis: Ungewöhnliches Format/i)).toBeTruthy();
    // Submit stays enabled — the warning is advisory, not blocking.
    const submit = screen.getByLabelText('Anfrage senden');
    expect(submit.props.accessibilityState?.disabled).not.toBe(true);
  });

  it('NewKursDialog: hides the cohort-pattern warning for a matching kursName', () => {
    stubQuery({ data: SAMPLE_KURSE });
    renderStep2();

    fireEvent.press(screen.getByLabelText('Neuen Kurs anlegen'));
    // "INVALID" is letters only / 7 chars → matches the loosened regex.
    fireEvent.changeText(screen.getByLabelText('Kursname'), 'INVALID');
    fireEvent.changeText(screen.getByLabelText('Jahrgang'), '2024');

    expect(screen.queryByText(/Hinweis: Ungewöhnliches Format/i)).toBeNull();
  });

  it('NewKursDialog: posts to /student/kurse/erstellen, toasts and replaces', async () => {
    mockFetchClient.mockResolvedValueOnce({});
    stubQuery({ data: SAMPLE_KURSE });
    renderStep2();

    fireEvent.press(screen.getByLabelText('Neuen Kurs anlegen'));
    fireEvent.changeText(screen.getByLabelText('Kursname'), 'TINF24B3');
    fireEvent.changeText(screen.getByLabelText('Jahrgang'), '2024');
    fireEvent.press(screen.getByLabelText('Anfrage senden'));

    await waitFor(() => expect(mockFetchClient).toHaveBeenCalledTimes(1));
    expect(mockFetchClient).toHaveBeenCalledWith(
      '/student/kurse/erstellen',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          kursName: 'TINF24B3',
          jahrgang: '2024',
          additionalNotes: null,
        }),
      }),
    );
    await waitFor(() =>
      expect(mockToastSuccess).toHaveBeenCalledWith(
        'Kurs-Erstellungsanfrage gesendet',
      ),
    );
    expect(mockReplace).toHaveBeenCalledWith('/(app)/dashboard');
  });

  it('NewKursDialog: 409 surfaces "Bereits offene Anfrage" and keeps dialog open', async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(409, 'Conflict'));
    stubQuery({ data: SAMPLE_KURSE });
    renderStep2();

    fireEvent.press(screen.getByLabelText('Neuen Kurs anlegen'));
    fireEvent.changeText(screen.getByLabelText('Kursname'), 'TINF24B3');
    fireEvent.changeText(screen.getByLabelText('Jahrgang'), '2024');
    fireEvent.press(screen.getByLabelText('Anfrage senden'));

    await waitFor(() =>
      expect(mockToastError).toHaveBeenCalledWith('Bereits offene Anfrage'),
    );
    expect(mockReplace).not.toHaveBeenCalled();
    // Dialog stays mounted — Kursname input still queryable.
    expect(screen.getByLabelText('Kursname')).toBeTruthy();
  });

  it('beitreten 422 surfaces the backend message verbatim', async () => {
    mockFetchClient.mockRejectedValueOnce(
      new ApiError(422, 'Beitritt momentan gesperrt'),
    );
    stubQuery({ data: SAMPLE_KURSE });
    renderStep2();

    fireEvent.press(screen.getByLabelText('TINF24B2, Jahrgang 2024'));
    fireEvent.press(screen.getByLabelText('Beitrittsanfrage senden'));

    await waitFor(() =>
      expect(mockToastError).toHaveBeenCalledWith(
        'Beitritt momentan gesperrt',
      ),
    );
    expect(mockReplace).not.toHaveBeenCalled();
  });

  it('NewKursDialog 422 surfaces the backend message verbatim and keeps dialog open', async () => {
    mockFetchClient.mockRejectedValueOnce(
      new ApiError(422, 'Kursname ist gesperrt'),
    );
    stubQuery({ data: SAMPLE_KURSE });
    renderStep2();

    fireEvent.press(screen.getByLabelText('Neuen Kurs anlegen'));
    fireEvent.changeText(screen.getByLabelText('Kursname'), 'TINF24B3');
    fireEvent.changeText(screen.getByLabelText('Jahrgang'), '2024');
    fireEvent.press(screen.getByLabelText('Anfrage senden'));

    await waitFor(() =>
      expect(mockToastError).toHaveBeenCalledWith('Kursname ist gesperrt'),
    );
    expect(mockReplace).not.toHaveBeenCalled();
    expect(screen.getByLabelText('Kursname')).toBeTruthy();
  });

  it('beitreten 404 (catalogue cache stale) toasts, refetches and clears the selection', async () => {
    // 404 lands in fetchClient's generic non-2xx branch as
    // ApiError(0, 'HTTP 404').
    const refetch = jest.fn(() => Promise.resolve({}));
    stubQuery({ data: SAMPLE_KURSE, refetch });
    mockFetchClient.mockRejectedValueOnce(new ApiError(0, 'HTTP 404'));
    renderStep2();

    fireEvent.press(screen.getByLabelText('TINF24B2, Jahrgang 2024'));
    const submit = screen.getByLabelText('Beitrittsanfrage senden');
    // The selection enables the join button; we re-check this so the
    // post-404 disabled assertion below is meaningful.
    expect(submit.props.accessibilityState?.disabled).toBe(false);
    fireEvent.press(submit);

    await waitFor(() =>
      expect(mockToastError).toHaveBeenCalledWith(
        'Dieser Kurs ist nicht mehr verfügbar — Liste wird neu geladen.',
      ),
    );
    expect(refetch).toHaveBeenCalledTimes(1);
    expect(mockReplace).not.toHaveBeenCalled();
    // Selection is cleared after a stale-catalogue 404 — the join button
    // becomes disabled again until the user picks a still-available course.
    await waitFor(() =>
      expect(
        screen.getByLabelText('Beitrittsanfrage senden').props.accessibilityState
          ?.disabled,
      ).toBe(true),
    );
  });

  it('beitreten fires only one POST on rapid double-submit', async () => {
    let resolveCall: ((value: unknown) => void) | undefined;
    mockFetchClient.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveCall = resolve;
        }),
    );
    stubQuery({ data: SAMPLE_KURSE });
    renderStep2();

    fireEvent.press(screen.getByLabelText('TINF24B2, Jahrgang 2024'));
    const submit = screen.getByLabelText('Beitrittsanfrage senden');
    fireEvent.press(submit);
    fireEvent.press(submit);

    expect(mockFetchClient).toHaveBeenCalledTimes(1);

    resolveCall?.({});
    await waitFor(() => expect(mockReplace).toHaveBeenCalled());
    expect(mockFetchClient).toHaveBeenCalledTimes(1);
  });

  it('NewKursDialog fires only one POST on rapid double-submit', async () => {
    let resolveCall: ((value: unknown) => void) | undefined;
    mockFetchClient.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveCall = resolve;
        }),
    );
    stubQuery({ data: SAMPLE_KURSE });
    renderStep2();

    fireEvent.press(screen.getByLabelText('Neuen Kurs anlegen'));
    fireEvent.changeText(screen.getByLabelText('Kursname'), 'TINF24B3');
    fireEvent.changeText(screen.getByLabelText('Jahrgang'), '2024');

    const submit = screen.getByLabelText('Anfrage senden');
    fireEvent.press(submit);
    fireEvent.press(submit);

    expect(mockFetchClient).toHaveBeenCalledTimes(1);

    resolveCall?.({});
    await waitFor(() => expect(mockReplace).toHaveBeenCalled());
    expect(mockFetchClient).toHaveBeenCalledTimes(1);
  });

  describe('consent-rejected gates', () => {
    it('refuses to fire the join request when consent is rejected', () => {
      mockConsent = 'rejected';
      stubQuery({ data: SAMPLE_KURSE });
      renderStep2();

      fireEvent.press(screen.getByLabelText('TINF24B2, Jahrgang 2024'));
      const submit = screen.getByLabelText('Beitrittsanfrage senden');
      // canJoin folds `consent !== 'rejected'` into the disabled-state, so
      // the button is inert even with a course selected.
      expect(submit.props.accessibilityState?.disabled).toBe(true);
      fireEvent.press(submit);

      expect(mockFetchClient).not.toHaveBeenCalled();
      expect(mockReplace).not.toHaveBeenCalled();
    });

    it('refuses to submit NewKursDialog when consent is rejected', () => {
      mockConsent = 'rejected';
      stubQuery({ data: SAMPLE_KURSE });
      renderStep2();

      fireEvent.press(screen.getByLabelText('Neuen Kurs anlegen'));
      fireEvent.changeText(screen.getByLabelText('Kursname'), 'TINF24B3');
      fireEvent.changeText(screen.getByLabelText('Jahrgang'), '2024');

      const submit = screen.getByLabelText('Anfrage senden');
      // canSubmit gates on `consent !== 'rejected'` — the button is
      // disabled even with a fully filled form.
      expect(submit.props.accessibilityState?.disabled).toBe(true);
      fireEvent.press(submit);

      expect(mockFetchClient).not.toHaveBeenCalled();
      expect(mockReplace).not.toHaveBeenCalled();
    });
  });
});
