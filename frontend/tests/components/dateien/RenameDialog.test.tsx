/**
 * Tests for {@link RenameDialog}.
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

import { ApiError } from "@/lib/api/errors";

jest.mock("lucide-react-native", () => {
  const { View: V } = require("react-native"); // eslint-disable-line @typescript-eslint/no-require-imports
  const stub = (props: object) => <V {...props} />;
  return new Proxy({}, { get: () => stub });
});

// Stub the Dialog primitives — the real ones use `@rn-primitives/dialog`
// with a Portal that does not render its children inside the test
// renderer. The stub mirrors the same pattern used by
// `tests/components/ui/confirm-dialog.test.tsx`.
jest.mock("@/components/ui/dialog", () => {
  const { View, Text } = require("react-native"); // eslint-disable-line @typescript-eslint/no-require-imports
  return {
    Dialog: ({
      children,
      open,
    }: {
      children: React.ReactNode;
      open: boolean;
    }) => (open ? <View testID="dialog-root">{children}</View> : null),
    DialogContent: ({ children }: { children: React.ReactNode }) => (
      <View>{children}</View>
    ),
    DialogHeader: ({ children }: { children: React.ReactNode }) => (
      <View>{children}</View>
    ),
    DialogFooter: ({ children }: { children: React.ReactNode }) => (
      <View>{children}</View>
    ),
    DialogTitle: ({ children }: { children: React.ReactNode }) => (
      <Text>{children}</Text>
    ),
    DialogDescription: ({ children }: { children: React.ReactNode }) => (
      <Text>{children}</Text>
    ),
  };
});

const mockToastSuccess = jest.fn();
const mockToastError = jest.fn();
jest.mock("sonner-native", () => ({
  toast: {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    success: (...args: any[]) => mockToastSuccess(...args),
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    error: (...args: any[]) => mockToastError(...args),
  },
}));

const mockFetchClient = jest.fn<Promise<Response>, [string, RequestInit?]>();
jest.mock("@/lib/api/fetchClient", () => ({
  fetchClient: (...args: unknown[]) =>
    mockFetchClient(args[0] as string, args[1] as RequestInit | undefined),
  setUnauthorizedHandler: jest.fn(() => () => undefined),
}));

import { RenameDialog } from "@/components/dateien/RenameDialog";

const DATEI_ID = "11111111-1111-1111-1111-111111111111";
const ORDNER_ID = "22222222-2222-2222-2222-222222222222";

function buildOk(): Response {
  return new Response(
    JSON.stringify({
      response: "Datei renamed successfully.",
      persistenceOperation: "UPDATE",
    }),
    { status: 200, headers: { "Content-Type": "application/json" } },
  );
}

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>,
  );
}

describe("RenameDialog", () => {
  beforeEach(() => {
    mockFetchClient.mockReset();
    mockToastSuccess.mockReset();
    mockToastError.mockReset();
  });

  it("submits the trimmed new name, fires success toast, calls onOpenChange(false)", async () => {
    mockFetchClient.mockResolvedValueOnce(buildOk());
    const onOpenChange = jest.fn();

    renderWithClient(
      <RenameDialog
        open
        onOpenChange={onOpenChange}
        dateiId={DATEI_ID}
        currentName="alt.pdf"
        ordnerId={ORDNER_ID}
      />,
    );

    fireEvent.changeText(screen.getByTestId("rename-dialog-input"), "  neu.pdf  ");

    await act(async () => {
      fireEvent.press(screen.getByTestId("rename-dialog-confirm"));
    });

    await waitFor(() =>
      expect(mockFetchClient).toHaveBeenCalledTimes(1),
    );

    const [, init] = mockFetchClient.mock.calls[0];
    expect(JSON.parse(init?.body as string)).toEqual({ newName: "neu.pdf" });

    await waitFor(() => expect(mockToastSuccess).toHaveBeenCalled());
    await waitFor(() => expect(onOpenChange).toHaveBeenCalledWith(false));
  });

  it("disables submit when the input equals the current name (after trim)", () => {
    renderWithClient(
      <RenameDialog
        open
        onOpenChange={jest.fn()}
        dateiId={DATEI_ID}
        currentName="alt.pdf"
        ordnerId={ORDNER_ID}
      />,
    );
    // Initial value === currentName → submit disabled, no fetch.
    fireEvent.press(screen.getByTestId("rename-dialog-confirm"));
    expect(mockFetchClient).not.toHaveBeenCalled();
  });

  it("disables submit when the trimmed input is empty", () => {
    renderWithClient(
      <RenameDialog
        open
        onOpenChange={jest.fn()}
        dateiId={DATEI_ID}
        currentName="alt.pdf"
        ordnerId={ORDNER_ID}
      />,
    );
    fireEvent.changeText(screen.getByTestId("rename-dialog-input"), "    ");
    fireEvent.press(screen.getByTestId("rename-dialog-confirm"));
    expect(mockFetchClient).not.toHaveBeenCalled();
  });

  it("shows an error toast on 403 and keeps the dialog open", async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(403, "Forbidden"));
    const onOpenChange = jest.fn();

    renderWithClient(
      <RenameDialog
        open
        onOpenChange={onOpenChange}
        dateiId={DATEI_ID}
        currentName="alt.pdf"
        ordnerId={ORDNER_ID}
      />,
    );

    fireEvent.changeText(screen.getByTestId("rename-dialog-input"), "neu.pdf");

    await act(async () => {
      fireEvent.press(screen.getByTestId("rename-dialog-confirm"));
    });

    await waitFor(() => expect(mockToastError).toHaveBeenCalled());
    expect(onOpenChange).not.toHaveBeenCalledWith(false);
  });

  it("shows an error toast on 404 (HTTP 404 collapsed to ApiError(0))", async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(0, "HTTP 404"));

    renderWithClient(
      <RenameDialog
        open
        onOpenChange={jest.fn()}
        dateiId={DATEI_ID}
        currentName="alt.pdf"
        ordnerId={ORDNER_ID}
      />,
    );

    fireEvent.changeText(screen.getByTestId("rename-dialog-input"), "neu.pdf");

    await act(async () => {
      fireEvent.press(screen.getByTestId("rename-dialog-confirm"));
    });

    await waitFor(() => expect(mockToastError).toHaveBeenCalled());
  });
});
