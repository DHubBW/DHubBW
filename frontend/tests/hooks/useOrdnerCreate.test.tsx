/**
 * Tests for {@link useOrdnerCreate}.
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

import { useOrdnerCreate } from "@/hooks/useOrdnerCreate";
import { ORDNER_CHILDREN_QUERY_KEY } from "@/hooks/useOrdnerChildren";
import { ORDNER_TREE_QUERY_KEY } from "@/hooks/useOrdnerTree";

const NEW_ID = "33333333-3333-3333-3333-333333333333";
const PARENT_ID = "22222222-2222-2222-2222-222222222222";

function buildJsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function renderCreateHook() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const invalidateSpy = jest.spyOn(queryClient, "invalidateQueries");
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  const result = renderHook(() => useOrdnerCreate(), { wrapper });
  return { ...result, invalidateSpy };
}

describe("useOrdnerCreate", () => {
  beforeEach(() => {
    mockFetchClient.mockReset();
  });

  it("PUTs /student/ordner with the JSON body and invalidates the tree", async () => {
    mockFetchClient.mockResolvedValueOnce(buildJsonResponse(201, NEW_ID));

    const { result, invalidateSpy } = renderCreateHook();

    await act(async () => {
      const id = await result.current.mutateAsync({
        ueberOrdnerId: null,
        name: "Mathematik",
        isPublic: true,
        farbe: "BLUE",
      });
      expect(id).toBe(NEW_ID);
    });

    expect(mockFetchClient).toHaveBeenCalledTimes(1);
    const [path, init] = mockFetchClient.mock.calls[0];
    expect(path).toBe("/student/ordner");
    expect(init?.method).toBe("PUT");
    expect(typeof init?.body).toBe("string");
    expect(JSON.parse(init?.body as string)).toEqual({
      ueberOrdnerId: null,
      name: "Mathematik",
      isPublic: true,
      farbe: "BLUE",
    });

    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: ORDNER_TREE_QUERY_KEY,
    });
  });

  it("trims the name before sending", async () => {
    mockFetchClient.mockResolvedValueOnce(buildJsonResponse(201, NEW_ID));

    const { result } = renderCreateHook();

    await act(async () => {
      await result.current.mutateAsync({
        ueberOrdnerId: null,
        name: "  Padded  ",
        isPublic: false,
        farbe: "DEFAULT",
      });
    });

    const [, init] = mockFetchClient.mock.calls[0];
    expect(JSON.parse(init?.body as string)).toMatchObject({ name: "Padded" });
  });

  it("invalidates the parent's children query when ueberOrdnerId is set", async () => {
    mockFetchClient.mockResolvedValueOnce(buildJsonResponse(201, NEW_ID));

    const { result, invalidateSpy } = renderCreateHook();

    await act(async () => {
      await result.current.mutateAsync({
        ueberOrdnerId: PARENT_ID,
        name: "Sub",
        isPublic: false,
        farbe: "GREEN",
      });
    });

    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: ORDNER_TREE_QUERY_KEY,
    });
    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: ORDNER_CHILDREN_QUERY_KEY(PARENT_ID),
    });
  });

  it("propagates 403 from fetchClient as ApiError so the caller can map to a toast", async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(403, "Forbidden"));

    const { result, invalidateSpy } = renderCreateHook();

    await expect(
      act(async () => {
        await result.current.mutateAsync({
          ueberOrdnerId: null,
          name: "Mathematik",
          isPublic: true,
          farbe: "BLUE",
        });
      }),
    ).rejects.toMatchObject({ status: 403 });

    expect(invalidateSpy).not.toHaveBeenCalled();
  });

  it("propagates 507 (storage quota exceeded)", async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(507, "Insufficient Storage"));

    const { result } = renderCreateHook();

    await expect(
      act(async () => {
        await result.current.mutateAsync({
          ueberOrdnerId: null,
          name: "Mathematik",
          isPublic: true,
          farbe: "BLUE",
        });
      }),
    ).rejects.toMatchObject({ status: 507 });
  });

  it("propagates network errors as ApiError(0, 'Network error')", async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(0, "Network error"));

    const { result } = renderCreateHook();

    await expect(
      act(async () => {
        await result.current.mutateAsync({
          ueberOrdnerId: null,
          name: "Mathematik",
          isPublic: true,
          farbe: "BLUE",
        });
      }),
    ).rejects.toMatchObject({ status: 0 });
  });
});
