/**
 * Tests for {@link DeleteOrdnerConfirm}.
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

import { DeleteOrdnerConfirm } from "@/components/dateien/DeleteOrdnerConfirm";

const ORDNER_ID = "11111111-1111-1111-1111-111111111111";

function buildOk(): Response {
  return new Response(
    JSON.stringify({ response: "Folder deleted successfully" }),
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

describe("DeleteOrdnerConfirm", () => {
  beforeEach(() => {
    mockFetchClient.mockReset();
    mockToastSuccess.mockReset();
    mockToastError.mockReset();
  });

  it("on confirm, DELETEs /student/ordner/{id}, fires success toast and closes", async () => {
    mockFetchClient.mockResolvedValueOnce(buildOk());
    const onOpenChange = jest.fn();

    renderWithClient(
      <DeleteOrdnerConfirm
        open
        onOpenChange={onOpenChange}
        ordnerId={ORDNER_ID}
        ordnerName="Mathematik"
        ueberOrdnerId={null}
      />,
    );

    await act(async () => {
      fireEvent.press(screen.getByTestId("confirm-dialog-confirm"));
    });

    await waitFor(() => expect(mockFetchClient).toHaveBeenCalledTimes(1));
    const [path, init] = mockFetchClient.mock.calls[0];
    expect(path).toBe(`/student/ordner/${ORDNER_ID}`);
    expect(init?.method).toBe("DELETE");

    await waitFor(() => expect(mockToastSuccess).toHaveBeenCalled());
    await waitFor(() => expect(onOpenChange).toHaveBeenCalledWith(false));
  });

  it("on cancel, fires onOpenChange(false) without calling the API", () => {
    const onOpenChange = jest.fn();
    renderWithClient(
      <DeleteOrdnerConfirm
        open
        onOpenChange={onOpenChange}
        ordnerId={ORDNER_ID}
        ordnerName="Mathematik"
        ueberOrdnerId={null}
      />,
    );

    fireEvent.press(screen.getByTestId("confirm-dialog-cancel"));

    expect(mockFetchClient).not.toHaveBeenCalled();
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it("shows an error toast on 403 and keeps the dialog open", async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(403, "Forbidden"));
    const onOpenChange = jest.fn();

    renderWithClient(
      <DeleteOrdnerConfirm
        open
        onOpenChange={onOpenChange}
        ordnerId={ORDNER_ID}
        ordnerName="Mathematik"
        ueberOrdnerId={null}
      />,
    );

    await act(async () => {
      fireEvent.press(screen.getByTestId("confirm-dialog-confirm"));
    });

    await waitFor(() => expect(mockToastError).toHaveBeenCalled());
    expect(onOpenChange).not.toHaveBeenCalledWith(false);
  });

  it("shows an error toast on 404 (collapsed to ApiError(0, 'HTTP 404'))", async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(0, "HTTP 404"));

    renderWithClient(
      <DeleteOrdnerConfirm
        open
        onOpenChange={jest.fn()}
        ordnerId={ORDNER_ID}
        ordnerName="Mathematik"
        ueberOrdnerId={null}
      />,
    );

    await act(async () => {
      fireEvent.press(screen.getByTestId("confirm-dialog-confirm"));
    });

    await waitFor(() => expect(mockToastError).toHaveBeenCalled());
  });
});
