/**
 * Tests for {@link useDateiRename}.
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

import { useDateiRename } from "@/hooks/useDateiRename";
import { DATEIEN_LISTE_QUERY_KEY } from "@/hooks/useDateienListe";

const DATEI_ID = "11111111-1111-1111-1111-111111111111";
const ORDNER_ID = "22222222-2222-2222-2222-222222222222";

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
  const result = renderHook(() => useDateiRename(), { wrapper });
  return { ...result, invalidateSpy };
}

describe("useDateiRename", () => {
  beforeEach(() => {
    mockFetchClient.mockReset();
  });

  it("PATCHes /student/dateien/{id} with JSON body { newName } and invalidates the per-folder list", async () => {
    mockFetchClient.mockResolvedValueOnce(
      buildJsonResponse(200, {
        response: "Datei renamed successfully.",
        persistenceOperation: "UPDATE",
      }),
    );

    const { result, invalidateSpy } = renderRenameHook();

    await act(async () => {
      await result.current.mutateAsync({
        dateiId: DATEI_ID,
        newName: "neu.pdf",
        ordnerId: ORDNER_ID,
      });
    });

    expect(mockFetchClient).toHaveBeenCalledTimes(1);
    const [path, init] = mockFetchClient.mock.calls[0];
    expect(path).toBe(`/student/dateien/${DATEI_ID}`);
    expect(init?.method).toBe("PATCH");
    expect(typeof init?.body).toBe("string");
    expect(JSON.parse(init?.body as string)).toEqual({ newName: "neu.pdf" });

    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: DATEIEN_LISTE_QUERY_KEY(ORDNER_ID),
    });
  });

  it("trims whitespace before sending newName", async () => {
    mockFetchClient.mockResolvedValueOnce(
      buildJsonResponse(200, { response: "ok", persistenceOperation: "UPDATE" }),
    );

    const { result } = renderRenameHook();

    await act(async () => {
      await result.current.mutateAsync({
        dateiId: DATEI_ID,
        newName: "  spaced.pdf  ",
        ordnerId: ORDNER_ID,
      });
    });

    const [, init] = mockFetchClient.mock.calls[0];
    expect(JSON.parse(init?.body as string)).toEqual({ newName: "spaced.pdf" });
  });

  it("propagates 403 from fetchClient as ApiError so the caller can map to a toast", async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(403, "Forbidden"));

    const { result, invalidateSpy } = renderRenameHook();

    await expect(
      act(async () => {
        await result.current.mutateAsync({
          dateiId: DATEI_ID,
          newName: "neu.pdf",
          ordnerId: ORDNER_ID,
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
          dateiId: DATEI_ID,
          newName: "neu.pdf",
          ordnerId: ORDNER_ID,
        });
      }),
    ).rejects.toMatchObject({ status: 0, message: expect.stringContaining("404") });
  });

  it("invalidates with the root-key sentinel when ordnerId is null", async () => {
    mockFetchClient.mockResolvedValueOnce(
      buildJsonResponse(200, { response: "ok", persistenceOperation: "UPDATE" }),
    );

    const { result, invalidateSpy } = renderRenameHook();

    await act(async () => {
      await result.current.mutateAsync({
        dateiId: DATEI_ID,
        newName: "neu.pdf",
        ordnerId: null,
      });
    });

    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: DATEIEN_LISTE_QUERY_KEY(null),
    });
  });
});
