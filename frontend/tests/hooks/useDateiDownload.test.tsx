/**
 * Tests for {@link useDateiDownload}.
 *
 * jest-expo's default test environment is `node`, so we stand up a minimal
 * `document`/`URL` shim to drive the Web download path without pulling in
 * jsdom (which has its own incompatibilities with the Expo polyfills).
 */
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook } from "@testing-library/react-native";
import React from "react";
import { Platform } from "react-native";

import { ApiError } from "@/lib/api/errors";

const mockFetchClient = jest.fn<Promise<Response>, [string, RequestInit?]>();

jest.mock("@/lib/api/fetchClient", () => ({
  fetchClient: (...args: unknown[]) =>
    mockFetchClient(args[0] as string, args[1] as RequestInit | undefined),
  setUnauthorizedHandler: jest.fn(() => () => undefined),
}));

import { DATEIEN_LISTE_QUERY_KEY } from "@/hooks/useDateienListe";
import { useDateiDownload } from "@/hooks/useDateiDownload";

const DATEI_ID = "11111111-1111-1111-1111-111111111111";
const ORDNER_ID = "22222222-2222-2222-2222-222222222222";

const originalOS = Platform.OS;

interface MockAnchor {
  href: string;
  download: string;
  clicked: number;
}

let anchors: MockAnchor[];
let appended: number;
let createObjectURL: jest.Mock;
let revokeObjectURL: jest.Mock;

beforeAll(() => {
  (Platform as { OS: string }).OS = "web";
});

afterAll(() => {
  (Platform as { OS: string }).OS = originalOS;
});

function buildBlobResponse(status: number, body: string): Response {
  return new Response(body, {
    status,
    headers: { "Content-Type": "application/octet-stream" },
  });
}

function renderDownloadHook() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const invalidateSpy = jest.spyOn(queryClient, "invalidateQueries");
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  const result = renderHook(() => useDateiDownload(), { wrapper });
  return { ...result, invalidateSpy };
}

describe("useDateiDownload", () => {
  beforeEach(() => {
    mockFetchClient.mockReset();
    anchors = [];
    appended = 0;
    createObjectURL = jest.fn(() => "blob:mock-url");
    revokeObjectURL = jest.fn();

    const documentShim = {
      createElement: (tag: string) => {
        if (tag !== "a") {
          throw new Error(`Unexpected createElement(${tag}) in test`);
        }
        const a: MockAnchor = { href: "", download: "", clicked: 0 };
        anchors.push(a);
        // The hook calls .click() to start the download.
        return Object.assign(a, {
          click() {
            a.clicked += 1;
          },
        });
      },
      body: {
        appendChild: () => {
          appended += 1;
        },
        removeChild: () => undefined,
      },
    };

    (globalThis as unknown as { document: unknown }).document = documentShim;
    (globalThis as unknown as { URL: unknown }).URL = {
      createObjectURL,
      revokeObjectURL,
    };
  });

  afterEach(() => {
    delete (globalThis as { document?: unknown }).document;
    delete (globalThis as { URL?: unknown }).URL;
  });

  it("fetches the download endpoint, triggers a browser download, invalidates the list", async () => {
    mockFetchClient.mockResolvedValueOnce(buildBlobResponse(200, "filebytes"));

    const { result, invalidateSpy } = renderDownloadHook();

    await act(async () => {
      await result.current.mutateAsync({
        dateiId: DATEI_ID,
        name: "demo.pdf",
        ordnerId: ORDNER_ID,
      });
    });

    expect(mockFetchClient).toHaveBeenCalledTimes(1);
    expect(mockFetchClient.mock.calls[0][0]).toBe(
      `/student/dateien/${DATEI_ID}/download`,
    );
    expect(createObjectURL).toHaveBeenCalledTimes(1);
    expect(anchors).toHaveLength(1);
    expect(anchors[0].clicked).toBe(1);
    expect(anchors[0].download).toBe("demo.pdf");
    expect(appended).toBe(1);
    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: DATEIEN_LISTE_QUERY_KEY(ORDNER_ID),
    });
  });

  it("maps BE 403 to forbidden", async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(403, "Forbidden"));

    const { result } = renderDownloadHook();

    await expect(
      act(async () => {
        await result.current.mutateAsync({
          dateiId: DATEI_ID,
          name: "demo.pdf",
          ordnerId: ORDNER_ID,
        });
      }),
    ).rejects.toMatchObject({ name: "FileDownloadError", code: "forbidden" });

    expect(anchors).toHaveLength(0);
  });

  it("maps BE 404 (HTTP 404 via ApiError(0)) to not-found", async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(0, "HTTP 404"));

    const { result } = renderDownloadHook();

    await expect(
      act(async () => {
        await result.current.mutateAsync({
          dateiId: DATEI_ID,
          name: "demo.pdf",
          ordnerId: ORDNER_ID,
        });
      }),
    ).rejects.toMatchObject({ code: "not-found" });
  });

  it("maps fetchClient network error to network", async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(0, "Network error"));

    const { result } = renderDownloadHook();

    await expect(
      act(async () => {
        await result.current.mutateAsync({
          dateiId: DATEI_ID,
          name: "demo.pdf",
          ordnerId: ORDNER_ID,
        });
      }),
    ).rejects.toMatchObject({ code: "network" });
  });

  it("rejects on Native with native-unsupported and never calls fetchClient", async () => {
    (Platform as { OS: string }).OS = "ios";
    try {
      const { result } = renderDownloadHook();
      await expect(
        act(async () => {
          await result.current.mutateAsync({
            dateiId: DATEI_ID,
            name: "demo.pdf",
            ordnerId: ORDNER_ID,
          });
        }),
      ).rejects.toMatchObject({ code: "native-unsupported" });

      expect(mockFetchClient).not.toHaveBeenCalled();
    } finally {
      (Platform as { OS: string }).OS = "web";
    }
  });
});
