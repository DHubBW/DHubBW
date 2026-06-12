import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react-native';
import React from 'react';

import { ApiError } from '@/lib/api/errors';

// ---- Mocks ----------------------------------------------------------------
//
// `fetchClient` is the only collaborator we mock — everything else lives in
// the hook. Per-test the mock is reset and re-implemented so the failure
// modes stay isolated.

const mockFetchClient = jest.fn<Promise<Response>, [string, RequestInit?]>();

jest.mock('@/lib/api/fetchClient', () => ({
  fetchClient: (...args: unknown[]) =>
    mockFetchClient(args[0] as string, args[1] as RequestInit | undefined),
  setUnauthorizedHandler: jest.fn(() => () => undefined),
}));

import { DATEIEN_LIST_QUERY_KEY, FileUploadError, useFileUpload } from '@/hooks/useFileUpload';

// ---- Helpers --------------------------------------------------------------

const FOLDER_ID = '11111111-1111-1111-1111-111111111111';
const NEW_FILE_ID = '22222222-2222-2222-2222-222222222222';

function buildJsonResponse(status: number, body: unknown): Response {
  // jest-expo runs on whatwg-fetch which exposes a real Response constructor.
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

function makeFile(sizeBytes: number, name = 'beispiel.pdf'): Blob {
  // A Blob is enough — the hook only needs `size` for pre-validation.
  // `'a'.repeat()` keeps the underlying byte length predictable.
  return new Blob(['a'.repeat(sizeBytes)], { type: 'application/pdf' });
}

function renderUploadHook() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  const result = renderHook(() => useFileUpload(), { wrapper });
  return { ...result, queryClient, invalidateSpy };
}

// ---- Suite ----------------------------------------------------------------

describe('useFileUpload', () => {
  const originalMaxUploadMb = process.env.EXPO_PUBLIC_MAX_UPLOAD_MB;

  beforeEach(() => {
    mockFetchClient.mockReset();
    process.env.EXPO_PUBLIC_MAX_UPLOAD_MB = '10';
  });

  afterAll(() => {
    if (originalMaxUploadMb === undefined) {
      delete process.env.EXPO_PUBLIC_MAX_UPLOAD_MB;
    } else {
      process.env.EXPO_PUBLIC_MAX_UPLOAD_MB = originalMaxUploadMb;
    }
  });

  // ----- Happy path -------------------------------------------------------

  it('uploads on 201, returns the dateiID payload, invalidates the per-folder list', async () => {
    mockFetchClient.mockResolvedValueOnce(
      buildJsonResponse(201, { dateiID: NEW_FILE_ID }),
    );

    const { result, invalidateSpy } = renderUploadHook();

    let returned: unknown;
    await act(async () => {
      returned = await result.current.mutateAsync({
        file: makeFile(1024),
        ueberordnerId: FOLDER_ID,
        name: 'beispiel.pdf',
      });
    });

    expect(returned).toEqual({ dateiID: NEW_FILE_ID });
    expect(mockFetchClient).toHaveBeenCalledTimes(1);

    const [path, init] = mockFetchClient.mock.calls[0];
    expect(path).toBe('/student/dateien/upload');
    expect(init?.method).toBe('PUT');
    // Body is FormData — assert it carries the three @RequestParam fields
    // the controller expects. We do not assert ordering or boundary; that
    // is the platform's responsibility.
    expect(init?.body).toBeInstanceOf(FormData);
    const body = init?.body as FormData;
    expect(body.get('ueberordnerId')).toBe(FOLDER_ID);
    expect(body.get('name')).toBe('beispiel.pdf');
    expect(body.get('file')).not.toBeNull();

    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: DATEIEN_LIST_QUERY_KEY(FOLDER_ID),
    });
  });

  // ----- Pre-validation: size ---------------------------------------------

  it('rejects locally with too-large before any network call when size > MAX_UPLOAD_MB', async () => {
    process.env.EXPO_PUBLIC_MAX_UPLOAD_MB = '10';
    const elevenMb = 11 * 1024 * 1024;

    const { result } = renderUploadHook();

    await expect(
      act(async () => {
        await result.current.mutateAsync({
          file: makeFile(elevenMb),
          ueberordnerId: FOLDER_ID,
          name: 'big.pdf',
        });
      }),
    ).rejects.toMatchObject({
      name: 'FileUploadError',
      code: 'too-large',
      // The size-aware error carries the actual / cap MB so consumers
      // can render a precise "X MB > Y MB" toast.
      actualMb: 11,
      limitMb: 10,
    });

    expect(mockFetchClient).not.toHaveBeenCalled();
  });

  it('honours a custom EXPO_PUBLIC_MAX_UPLOAD_MB at mutateAsync time', async () => {
    process.env.EXPO_PUBLIC_MAX_UPLOAD_MB = '1';
    const twoMb = 2 * 1024 * 1024;

    const { result } = renderUploadHook();

    await expect(
      act(async () => {
        await result.current.mutateAsync({
          file: makeFile(twoMb),
          ueberordnerId: FOLDER_ID,
          name: 'small-but-over.pdf',
        });
      }),
    ).rejects.toMatchObject({ code: 'too-large' });

    expect(mockFetchClient).not.toHaveBeenCalled();
  });

  // ----- Error-path: BE-status mapping ------------------------------------

  it('maps BE 413 (HTTP 413 via ApiError(0)) to too-large', async () => {
    // fetchClient collapses 413 to ApiError(0, 'HTTP 413', body) because
    // 413 is not in the frozen ApiErrorStatus union.
    mockFetchClient.mockRejectedValueOnce(new ApiError(0, 'HTTP 413'));

    const { result, invalidateSpy } = renderUploadHook();

    await expect(
      act(async () => {
        await result.current.mutateAsync({
          file: makeFile(1024),
          ueberordnerId: FOLDER_ID,
          name: 'a.pdf',
        });
      }),
    ).rejects.toMatchObject({
      name: 'FileUploadError',
      code: 'too-large',
      // Backend-driven 413 still carries the configured cap so the
      // toast can show "max X MB" even when the server shipped no body.
      limitMb: 10,
    });
    expect(invalidateSpy).not.toHaveBeenCalled();
  });

  it('maps BE 403 to forbidden', async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(403, 'Forbidden'));

    const { result } = renderUploadHook();

    await expect(
      act(async () => {
        await result.current.mutateAsync({
          file: makeFile(1024),
          ueberordnerId: FOLDER_ID,
          name: 'a.pdf',
        });
      }),
    ).rejects.toMatchObject({ code: 'forbidden' });
  });

  it('maps BE 507 to storage-full', async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(507, 'Insufficient Storage'));

    const { result } = renderUploadHook();

    await expect(
      act(async () => {
        await result.current.mutateAsync({
          file: makeFile(1024),
          ueberordnerId: FOLDER_ID,
          name: 'a.pdf',
        });
      }),
    ).rejects.toMatchObject({ code: 'storage-full' });
  });

  it('maps BE 404 (HTTP 404 via ApiError(0)) to not-found', async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(0, 'HTTP 404'));

    const { result } = renderUploadHook();

    await expect(
      act(async () => {
        await result.current.mutateAsync({
          file: makeFile(1024),
          ueberordnerId: FOLDER_ID,
          name: 'a.pdf',
        });
      }),
    ).rejects.toMatchObject({ code: 'not-found' });
  });

  it('maps fetchClient network error (ApiError(0, "Network error")) to network', async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(0, 'Network error'));

    const { result } = renderUploadHook();

    await expect(
      act(async () => {
        await result.current.mutateAsync({
          file: makeFile(1024),
          ueberordnerId: FOLDER_ID,
          name: 'a.pdf',
        });
      }),
    ).rejects.toMatchObject({ code: 'network' });
  });

  it('maps any other ApiError or unexpected throw to unknown', async () => {
    // Pick a status that is genuinely unmapped on the upload path —
    // 500 collapses to ApiError(0, 'HTTP 500'). 409 used to land here
    // before duplicate-name detection was added; now it maps to
    // 'duplicate-name' (see _classify).
    mockFetchClient.mockRejectedValueOnce(new ApiError(0, 'HTTP 500'));

    const { result } = renderUploadHook();

    await expect(
      act(async () => {
        await result.current.mutateAsync({
          file: makeFile(1024),
          ueberordnerId: FOLDER_ID,
          name: 'a.pdf',
        });
      }),
    ).rejects.toMatchObject({ code: 'unknown' });
  });

  // ----- Defensive: malformed 201 body ------------------------------------

  it('reports unknown when a 201 response carries no dateiID field', async () => {
    mockFetchClient.mockResolvedValueOnce(buildJsonResponse(201, { wrong: 'shape' }));

    const { result, invalidateSpy } = renderUploadHook();

    await expect(
      act(async () => {
        await result.current.mutateAsync({
          file: makeFile(1024),
          ueberordnerId: FOLDER_ID,
          name: 'a.pdf',
        });
      }),
    ).rejects.toMatchObject({
      name: 'FileUploadError',
      code: 'unknown',
    });
    expect(invalidateSpy).not.toHaveBeenCalled();
  });

  // ----- Mutation state surface (smoke for consumers) ---------------------

  it('exposes isPending → isError on a rejected upload so consumers can drive the UI', async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(403, 'Forbidden'));

    const { result } = renderUploadHook();

    await act(async () => {
      result.current.mutate({
        file: makeFile(1024),
        ueberordnerId: FOLDER_ID,
        name: 'a.pdf',
      });
    });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });
    expect(result.current.error).toBeInstanceOf(FileUploadError);
    expect(result.current.error?.code).toBe('forbidden');
  });

  // ----- Global-toast opt-out --------------------------------------------

  it('marks the mutation with meta.suppressGlobalErrorToast so the global handler skips it', async () => {
    // The flag prevents the global mutationCache subscriber from
    // surfacing a second generic toast on top of the size-aware one
    // rendered by UploadButton.onError. Without it the user saw a
    // doubled toast on "too-large" rejections. We assert by triggering
    // a failing upload and reading meta off the recorded mutation in
    // the shared MutationCache.
    mockFetchClient.mockRejectedValueOnce(new ApiError(403, 'Forbidden'));

    const { result, queryClient } = renderUploadHook();
    await act(async () => {
      result.current.mutate({
        file: makeFile(1024),
        ueberordnerId: FOLDER_ID,
        name: 'a.pdf',
      });
    });
    await waitFor(() => expect(result.current.isError).toBe(true));

    const recorded = queryClient.getMutationCache().getAll();
    expect(recorded).toHaveLength(1);
    const meta = recorded[0].options.meta as
      | { suppressGlobalErrorToast?: boolean }
      | undefined;
    expect(meta?.suppressGlobalErrorToast).toBe(true);
  });
});
