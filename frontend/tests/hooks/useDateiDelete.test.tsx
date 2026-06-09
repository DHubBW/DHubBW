/**
 * Tests for {@link useDateiDelete}.
 */
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook } from "@testing-library/react-native";
import React from "react";

import { ApiError } from "@/lib/api/errors";

const mockFetchClient = jest.fn<Promise<Response>, [string, RequestInit?]>();

jest.mock("@/lib/api/fetchClient", () => ({
  fetchClient: (...args: unknown[]) =>
    mockFetchClient(args[0] as string, args[1] as RequestInit | undefined),
  setUnauthorizedHandler: jest.fn(() => () => undefined),
}));

import { useDateiDelete } from "@/hooks/useDateiDelete";
import { DATEIEN_LISTE_QUERY_KEY } from "@/hooks/useDateienListe";

const DATEI_ID = "11111111-1111-1111-1111-111111111111";
const ORDNER_ID = "22222222-2222-2222-2222-222222222222";

function buildOkResponse(): Response {
  // The hook only needs `res.ok`. The real backend returns 204 with a
  // JSON body, but the Response constructor in whatwg-fetch rejects
  // status 204 with a body (`new Response(body, { status: 204 })` →
  // TypeError). 200 is close enough for the unit test.
  return new Response(
    JSON.stringify({
      response: "Datei deleted succesfully.",
      persistenceOperation: "DELETE",
    }),
    { status: 200, headers: { "Content-Type": "application/json" } },
  );
}

function renderDeleteHook() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const invalidateSpy = jest.spyOn(queryClient, "invalidateQueries");
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  const result = renderHook(() => useDateiDelete(), { wrapper });
  return { ...result, invalidateSpy };
}

describe("useDateiDelete", () => {
  beforeEach(() => {
    mockFetchClient.mockReset();
  });

  it("DELETEs /student/dateien/{id} and invalidates the per-folder list", async () => {
    mockFetchClient.mockResolvedValueOnce(buildOkResponse());

    const { result, invalidateSpy } = renderDeleteHook();

    await act(async () => {
      await result.current.mutateAsync({
        dateiId: DATEI_ID,
        ordnerId: ORDNER_ID,
      });
    });

    expect(mockFetchClient).toHaveBeenCalledTimes(1);
    const [path, init] = mockFetchClient.mock.calls[0];
    expect(path).toBe(`/student/dateien/${DATEI_ID}`);
    expect(init?.method).toBe("DELETE");

    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: DATEIEN_LISTE_QUERY_KEY(ORDNER_ID),
    });
  });

  it("propagates 403 so the caller can show a toast", async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(403, "Forbidden"));

    const { result, invalidateSpy } = renderDeleteHook();

    await expect(
      act(async () => {
        await result.current.mutateAsync({
          dateiId: DATEI_ID,
          ordnerId: ORDNER_ID,
        });
      }),
    ).rejects.toMatchObject({ status: 403 });

    expect(invalidateSpy).not.toHaveBeenCalled();
  });

  it("propagates 404 (collapsed to ApiError(0, 'HTTP 404'))", async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(0, "HTTP 404"));

    const { result } = renderDeleteHook();

    await expect(
      act(async () => {
        await result.current.mutateAsync({
          dateiId: DATEI_ID,
          ordnerId: ORDNER_ID,
        });
      }),
    ).rejects.toMatchObject({ status: 0, message: expect.stringContaining("404") });
  });
});
