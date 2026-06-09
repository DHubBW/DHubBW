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
  DATEIEN_LIST_ALL_KEY,
  DATEIEN_LISTE_QUERY_KEY,
  useDateienListe,
} from "@/hooks/useDateienListe";
import { DATEIEN_LIST_QUERY_KEY } from "@/hooks/useFileUpload";

const FOLDER_ID = "11111111-1111-1111-1111-111111111111";

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function renderListHook(ordnerId: string | null) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return renderHook(() => useDateienListe(ordnerId), { wrapper });
}

describe("useDateienListe", () => {
  beforeEach(() => mockFetchClient.mockReset());

  it("calls /student/dateien/ordner/{id} for a folder id", async () => {
    mockFetchClient.mockResolvedValueOnce(jsonResponse(200, []));
    const { result } = renderListHook(FOLDER_ID);
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(mockFetchClient.mock.calls[0][0]).toBe(
      `/student/dateien/ordner/${FOLDER_ID}`,
    );
  });

  it("calls /student/dateien for the root listing (null id)", async () => {
    mockFetchClient.mockResolvedValueOnce(jsonResponse(200, []));
    const { result } = renderListHook(null);
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(mockFetchClient.mock.calls[0][0]).toBe("/student/dateien");
  });

  it("query key for a folder matches DATEIEN_LIST_QUERY_KEY from useFileUpload", () => {
    // Auto-invalidation contract — see useFileUpload.ts.
    expect(DATEIEN_LISTE_QUERY_KEY(FOLDER_ID)).toEqual(
      DATEIEN_LIST_QUERY_KEY(FOLDER_ID),
    );
  });

  it("uses the 'all' sentinel for the root listing", () => {
    expect(DATEIEN_LISTE_QUERY_KEY(null)).toEqual([
      "dateien",
      "list",
      DATEIEN_LIST_ALL_KEY,
    ]);
  });

  it("maps 403 to an empty list", async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(403, "Forbidden"));
    const { result } = renderListHook(FOLDER_ID);
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([]);
  });

  it("propagates non-403 errors", async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(0, "Network error"));
    const { result } = renderListHook(FOLDER_ID);
    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});
