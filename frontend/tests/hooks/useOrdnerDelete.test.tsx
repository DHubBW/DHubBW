/**
 * Tests for {@link useOrdnerDelete}.
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

import { useOrdnerDelete } from "@/hooks/useOrdnerDelete";

const ORDNER_ID = "11111111-1111-1111-1111-111111111111";
const PARENT_ID = "22222222-2222-2222-2222-222222222222";

function buildOkResponse(): Response {
  // The hook only needs `res.ok`. The real backend returns 204 with a
  // JSON body, but the Response constructor in whatwg-fetch rejects
  // status 204 with a body — 200 is close enough for the unit test.
  return new Response(
    JSON.stringify({ response: "Folder deleted successfully" }),
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
  const result = renderHook(() => useOrdnerDelete(), { wrapper });
  return { ...result, invalidateSpy };
}

describe("useOrdnerDelete", () => {
  beforeEach(() => {
    mockFetchClient.mockReset();
  });

  it("DELETEs /student/ordner/{id} and invalidates tree + dateien family", async () => {
    mockFetchClient.mockResolvedValueOnce(buildOkResponse());

    const { result, invalidateSpy } = renderDeleteHook();

    await act(async () => {
      await result.current.mutateAsync({
        ordnerId: ORDNER_ID,
        ueberOrdnerId: null,
      });
    });

    expect(mockFetchClient).toHaveBeenCalledTimes(1);
    const [path, init] = mockFetchClient.mock.calls[0];
    expect(path).toBe(`/student/ordner/${ORDNER_ID}`);
    expect(init?.method).toBe("DELETE");

    // Coarse prefix-invalidation: ['ordner'] covers tree + children +
    // every other folder query in the cache. Cheaper than threading
    // the parent id through and just as correct under cascade-delete.
    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: ["ordner"],
    });
    // Cascade: every file-list cache slice that may have referenced
    // the deleted subtree must refetch.
    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: ["dateien"],
    });
  });

  it("uses the same coarse ['ordner'] prefix regardless of ueberOrdnerId", async () => {
    mockFetchClient.mockResolvedValueOnce(buildOkResponse());

    const { result, invalidateSpy } = renderDeleteHook();

    await act(async () => {
      await result.current.mutateAsync({
        ordnerId: ORDNER_ID,
        ueberOrdnerId: PARENT_ID,
      });
    });

    // The hook deliberately invalidates ['ordner'] only — TanStack
    // matches every query whose key starts with that prefix, which
    // includes ORDNER_CHILDREN_QUERY_KEY(PARENT_ID).
    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: ["ordner"],
    });
  });

  it("propagates 403 so the caller can show a toast", async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(403, "Forbidden"));

    const { result, invalidateSpy } = renderDeleteHook();

    await expect(
      act(async () => {
        await result.current.mutateAsync({
          ordnerId: ORDNER_ID,
          ueberOrdnerId: null,
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
          ordnerId: ORDNER_ID,
          ueberOrdnerId: null,
        });
      }),
    ).rejects.toMatchObject({
      status: 0,
      message: expect.stringContaining("404"),
    });
  });
});
