/**
 * Tests for {@link UploadButton}.
 *
 * The hidden Web `<input type="file">` is rendered inside a React Native
 * `<View testID="dateien-upload-input-wrapper">`; we drive the picker by
 * grabbing that wrapper's first child (the input) from the test
 * renderer's tree and firing a synthetic `change` event on it.
 */
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react-native";
import React from "react";
import { Platform } from "react-native";

jest.mock("lucide-react-native", () => {
  const { View: V } = require("react-native"); // eslint-disable-line @typescript-eslint/no-require-imports
  const stub = (props: object) => <V {...props} />;
  return new Proxy({}, { get: () => stub });
});

const mockToastSuccess = jest.fn();
const mockToastError = jest.fn();
const mockToast = jest.fn();
jest.mock("sonner-native", () => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const fn: any = (...args: any[]) => mockToast(...args);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  fn.success = (...args: any[]) => mockToastSuccess(...args);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  fn.error = (...args: any[]) => mockToastError(...args);
  return { toast: fn };
});

const mockFetchClient = jest.fn<Promise<Response>, [string, RequestInit?]>();
jest.mock("@/lib/api/fetchClient", () => ({
  fetchClient: (...args: unknown[]) =>
    mockFetchClient(args[0] as string, args[1] as RequestInit | undefined),
  setUnauthorizedHandler: jest.fn(() => () => undefined),
}));

import { UploadButton } from "@/components/dateien/UploadButton";

const FOLDER_ID = "11111111-1111-1111-1111-111111111111";

const originalOS = Platform.OS;
beforeAll(() => {
  (Platform as { OS: string }).OS = "web";
});
afterAll(() => {
  (Platform as { OS: string }).OS = originalOS;
});

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>,
  );
}

/**
 * Drive the hidden picker by invoking its `onChange` directly. The input
 * lives as a string-tag child of the testID wrapper, so we walk the test
 * tree to find it.
 */
function pickFile(name = "demo.pdf", size = 1024) {
  const wrapper = screen.getByTestId("dateien-upload-input-wrapper");
  // The wrapper is a `View`; `props.children` is the React element for
  // the hidden input. Its `props.onChange` is the handler we want.
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const inputElement: any = (wrapper.props as { children: unknown }).children;
  const onChange = inputElement.props.onChange as (ev: {
    target: { files: { 0: Blob; length: number }; value: string };
  }) => void;
  const file = new File(["a".repeat(size)], name, {
    type: "application/octet-stream",
  });
  onChange({
    target: {
      files: { 0: file, length: 1 } as unknown as FileList,
      value: "",
    },
  });
}

describe("UploadButton", () => {
  beforeEach(() => {
    mockFetchClient.mockReset();
    mockToast.mockReset();
    mockToastSuccess.mockReset();
    mockToastError.mockReset();
    process.env.EXPO_PUBLIC_MAX_UPLOAD_MB = "10";
  });

  it("disables the button when no folder is selected", () => {
    renderWithClient(<UploadButton ueberordnerId={null} />);
    const btn = screen.getByTestId("dateien-upload-button");
    expect(btn.props.accessibilityState?.disabled).toBe(true);
  });

  it("enables the button when a folder is selected", () => {
    renderWithClient(<UploadButton ueberordnerId={FOLDER_ID} />);
    const btn = screen.getByTestId("dateien-upload-button");
    expect(btn.props.accessibilityState?.disabled).toBeFalsy();
  });

  it("forwards a picked file to the upload endpoint and fires success toast on 201", async () => {
    mockFetchClient.mockResolvedValueOnce(
      new Response(
        JSON.stringify({ dateiID: "22222222-2222-2222-2222-222222222222" }),
        { status: 201, headers: { "Content-Type": "application/json" } },
      ),
    );

    renderWithClient(<UploadButton ueberordnerId={FOLDER_ID} />);
    fireEvent.press(screen.getByTestId("dateien-upload-button"));

    await act(async () => {
      pickFile();
    });

    await waitFor(() => expect(mockFetchClient).toHaveBeenCalledTimes(1));
    const [path, init] = mockFetchClient.mock.calls[0];
    expect(path).toBe("/student/dateien/upload");
    expect(init?.method).toBe("PUT");

    await waitFor(() => expect(mockToastSuccess).toHaveBeenCalled());
  });

  it("fires an error toast when the backend returns 403", async () => {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const { ApiError } = require("@/lib/api/errors");
    mockFetchClient.mockRejectedValueOnce(new ApiError(403, "Forbidden"));

    renderWithClient(<UploadButton ueberordnerId={FOLDER_ID} />);
    await act(async () => {
      pickFile();
    });

    await waitFor(() => expect(mockToastError).toHaveBeenCalled());
  });

  it("fires an error toast when the backend returns 507 (storage-full)", async () => {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const { ApiError } = require("@/lib/api/errors");
    mockFetchClient.mockRejectedValueOnce(
      new ApiError(507, "Insufficient Storage"),
    );

    renderWithClient(<UploadButton ueberordnerId={FOLDER_ID} />);
    await act(async () => {
      pickFile();
    });

    await waitFor(() => expect(mockToastError).toHaveBeenCalled());
  });

  it("fires an error toast for fetchClient network errors", async () => {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const { ApiError } = require("@/lib/api/errors");
    mockFetchClient.mockRejectedValueOnce(new ApiError(0, "Network error"));

    renderWithClient(<UploadButton ueberordnerId={FOLDER_ID} />);
    await act(async () => {
      pickFile();
    });

    await waitFor(() => expect(mockToastError).toHaveBeenCalled());
  });

  it("renders a size-aware toast when the local pre-validation rejects an oversize file", async () => {
    process.env.EXPO_PUBLIC_MAX_UPLOAD_MB = "1";
    renderWithClient(<UploadButton ueberordnerId={FOLDER_ID} />);
    await act(async () => {
      pickFile("zu-gross.bin", 2 * 1024 * 1024);
    });

    await waitFor(() => expect(mockToastError).toHaveBeenCalled());
    const message = mockToastError.mock.calls[0][0];
    expect(typeof message).toBe("string");
    // The size-aware variant must include both the actual size and
    // the configured cap so the user understands what to do next.
    expect(message).toContain("2");
    expect(message).toContain("1 MB");
    process.env.EXPO_PUBLIC_MAX_UPLOAD_MB = "10";
  });
});
