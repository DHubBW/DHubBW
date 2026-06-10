/**
 * Tests for {@link useOrdnerRename}.
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

import { useOrdnerRename } from "@/hooks/useOrdnerRename";
import { ORDNER_CHILDREN_QUERY_KEY } from "@/hooks/useOrdnerChildren";
import { ORDNER_TREE_QUERY_KEY } from "@/hooks/useOrdnerTree";

const ORDNER_ID = "11111111-1111-1111-1111-111111111111";
const PARENT_ID = "22222222-2222-2222-2222-222222222222";

function buildJsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function renderRenameHook() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const invalidateSpy = jest.spyOn(queryClient, "invalidateQueries");
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  const result = renderHook(() => useOrdnerRename(), { wrapper });
  return { ...result, invalidateSpy };
}

describe("useOrdnerRename", () => {
  beforeEach(() => {
    mockFetchClient.mockReset();
  });

  it("PATCHes /student/ordner/{id} with JSON body { newName } and invalidates the tree", async () => {
    mockFetchClient.mockResolvedValueOnce(
      buildJsonResponse(200, { response: "Folder renamed successfully" }),
    );

    const { result, invalidateSpy } = renderRenameHook();

    await act(async () => {
      await result.current.mutateAsync({
        ordnerId: ORDNER_ID,
        newName: "Neu",
        ueberOrdnerId: null,
      });
    });

    expect(mockFetchClient).toHaveBeenCalledTimes(1);
    const [path, init] = mockFetchClient.mock.calls[0];
    expect(path).toBe(`/student/ordner/${ORDNER_ID}`);
    expect(init?.method).toBe("PATCH");
    expect(JSON.parse(init?.body as string)).toEqual({ newName: "Neu" });

    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: ORDNER_TREE_QUERY_KEY,
    });
  });

  it("trims whitespace before sending newName", async () => {
    mockFetchClient.mockResolvedValueOnce(
      buildJsonResponse(200, { response: "ok" }),
    );

    const { result } = renderRenameHook();

    await act(async () => {
      await result.current.mutateAsync({
        ordnerId: ORDNER_ID,
        newName: "  spaced  ",
        ueberOrdnerId: null,
      });
    });

    const [, init] = mockFetchClient.mock.calls[0];
    expect(JSON.parse(init?.body as string)).toEqual({ newName: "spaced" });
  });

  it("invalidates the parent's children query when ueberOrdnerId is set", async () => {
    mockFetchClient.mockResolvedValueOnce(
      buildJsonResponse(200, { response: "ok" }),
    );

    const { result, invalidateSpy } = renderRenameHook();

    await act(async () => {
      await result.current.mutateAsync({
        ordnerId: ORDNER_ID,
        newName: "Neu",
        ueberOrdnerId: PARENT_ID,
      });
    });

    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: ORDNER_CHILDREN_QUERY_KEY(PARENT_ID),
    });
  });

  it("propagates 403 from fetchClient", async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(403, "Forbidden"));

    const { result, invalidateSpy } = renderRenameHook();

    await expect(
      act(async () => {
        await result.current.mutateAsync({
          ordnerId: ORDNER_ID,
          newName: "Neu",
          ueberOrdnerId: null,
        });
      }),
    ).rejects.toMatchObject({ status: 403 });

    expect(invalidateSpy).not.toHaveBeenCalled();
  });

  it("propagates 404 (collapsed by fetchClient to ApiError(0, 'HTTP 404'))", async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(0, "HTTP 404"));

    const { result } = renderRenameHook();

    await expect(
      act(async () => {
        await result.current.mutateAsync({
          ordnerId: ORDNER_ID,
          newName: "Neu",
          ueberOrdnerId: null,
        });
      }),
    ).rejects.toMatchObject({
      status: 0,
      message: expect.stringContaining("404"),
    });
  });
});
