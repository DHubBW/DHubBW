import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react-native";
import React from "react";

import { ApiError } from "@/lib/api/errors";

const mockFetchClient = jest.fn<Promise<Response>, [string, RequestInit?]>();

jest.mock("@/lib/api/fetchClient", () => ({
  fetchClient: (...args: unknown[]) =>
    mockFetchClient(args[0] as string, args[1] as RequestInit | undefined),
  setUnauthorizedHandler: jest.fn(() => () => undefined),
}));

import {
  ORDNER_TREE_QUERY_KEY,
  useOrdnerTree,
} from "@/hooks/useOrdnerTree";

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function renderTreeHook() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return renderHook(() => useOrdnerTree(), { wrapper });
}

describe("useOrdnerTree", () => {
  beforeEach(() => mockFetchClient.mockReset());

  it("calls /student/ordner WITHOUT trailing slash", async () => {
    mockFetchClient.mockResolvedValueOnce(jsonResponse(200, []));
    const { result } = renderTreeHook();
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(mockFetchClient).toHaveBeenCalledTimes(1);
    expect(mockFetchClient.mock.calls[0][0]).toBe("/student/ordner");
  });

  it("uses the canonical query key", () => {
    expect(ORDNER_TREE_QUERY_KEY).toEqual(["ordner", "tree"]);
  });

  it("returns the parsed list on 200", async () => {
    const rootFolder = {
      ordnerID: "11111111-1111-1111-1111-111111111111",
      name: "Vorlesungen",
      level: 0,
      farbe: "DEFAULT",
      public: true,
      ueberOrdnerID: null,
      kursID: "22222222-2222-2222-2222-222222222222",
    };
    mockFetchClient.mockResolvedValueOnce(jsonResponse(200, [rootFolder]));
    const { result } = renderTreeHook();
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([rootFolder]);
  });

  it("maps 403 to an empty list rather than an error", async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(403, "Forbidden"));
    const { result } = renderTreeHook();
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([]);
  });

  it("propagates non-403 errors", async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(0, "Network error"));
    const { result } = renderTreeHook();
    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.error).toBeInstanceOf(ApiError);
  });
});
