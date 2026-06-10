/**
 * Tests for {@link CreateOrdnerDialog}.
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

// Stub the Dialog primitives — same pattern used by the RenameDialog test.
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

import { CreateOrdnerDialog } from "@/components/dateien/CreateOrdnerDialog";

const NEW_ID = "33333333-3333-3333-3333-333333333333";

function buildOk(): Response {
  return new Response(JSON.stringify(NEW_ID), {
    status: 201,
    headers: { "Content-Type": "application/json" },
  });
}

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>,
  );
}

describe("CreateOrdnerDialog", () => {
  beforeEach(() => {
    mockFetchClient.mockReset();
    mockToastSuccess.mockReset();
    mockToastError.mockReset();
  });

  it("submits the trimmed name + selected colour + visibility, fires success toast and closes", async () => {
    mockFetchClient.mockResolvedValueOnce(buildOk());
    const onOpenChange = jest.fn();

    renderWithClient(
      <CreateOrdnerDialog
        open
        onOpenChange={onOpenChange}
        ueberOrdnerId={null}
      />,
    );

    fireEvent.changeText(
      screen.getByTestId("create-ordner-name-input"),
      "  Mathematik  ",
    );
    fireEvent.press(screen.getByTestId("create-ordner-color-blue"));
    fireEvent.press(screen.getByTestId("create-ordner-private-toggle"));

    await act(async () => {
      fireEvent.press(screen.getByTestId("create-ordner-confirm"));
    });

    await waitFor(() => expect(mockFetchClient).toHaveBeenCalledTimes(1));
    const [path, init] = mockFetchClient.mock.calls[0];
    expect(path).toBe("/student/ordner");
    expect(init?.method).toBe("PUT");
    expect(JSON.parse(init?.body as string)).toEqual({
      ueberOrdnerId: null,
      name: "Mathematik",
      isPublic: false,
      farbe: "BLUE",
    });

    await waitFor(() => expect(mockToastSuccess).toHaveBeenCalled());
    await waitFor(() => expect(onOpenChange).toHaveBeenCalledWith(false));
  });

  it("disables submit when the trimmed name is empty", () => {
    renderWithClient(
      <CreateOrdnerDialog
        open
        onOpenChange={jest.fn()}
        ueberOrdnerId={null}
      />,
    );

    // Input starts empty → submit disabled → press has no effect.
    fireEvent.press(screen.getByTestId("create-ordner-confirm"));
    expect(mockFetchClient).not.toHaveBeenCalled();

    fireEvent.changeText(
      screen.getByTestId("create-ordner-name-input"),
      "    ",
    );
    fireEvent.press(screen.getByTestId("create-ordner-confirm"));
    expect(mockFetchClient).not.toHaveBeenCalled();
  });

  it("forwards a non-null ueberOrdnerId verbatim", async () => {
    mockFetchClient.mockResolvedValueOnce(buildOk());
    const PARENT_ID = "22222222-2222-2222-2222-222222222222";

    renderWithClient(
      <CreateOrdnerDialog
        open
        onOpenChange={jest.fn()}
        ueberOrdnerId={PARENT_ID}
      />,
    );

    fireEvent.changeText(
      screen.getByTestId("create-ordner-name-input"),
      "Sub",
    );

    await act(async () => {
      fireEvent.press(screen.getByTestId("create-ordner-confirm"));
    });

    await waitFor(() => expect(mockFetchClient).toHaveBeenCalledTimes(1));
    const [, init] = mockFetchClient.mock.calls[0];
    expect(JSON.parse(init?.body as string)).toMatchObject({
      ueberOrdnerId: PARENT_ID,
    });
  });

  it("shows an error toast on 403 and keeps the dialog open", async () => {
    mockFetchClient.mockRejectedValueOnce(new ApiError(403, "Forbidden"));
    const onOpenChange = jest.fn();

    renderWithClient(
      <CreateOrdnerDialog
        open
        onOpenChange={onOpenChange}
        ueberOrdnerId={null}
      />,
    );

    fireEvent.changeText(
      screen.getByTestId("create-ordner-name-input"),
      "Mathematik",
    );

    await act(async () => {
      fireEvent.press(screen.getByTestId("create-ordner-confirm"));
    });

    await waitFor(() => expect(mockToastError).toHaveBeenCalled());
    expect(onOpenChange).not.toHaveBeenCalledWith(false);
  });

  it("shows the storage-full toast on 507", async () => {
    mockFetchClient.mockRejectedValueOnce(
      new ApiError(507, "Insufficient Storage"),
    );

    renderWithClient(
      <CreateOrdnerDialog
        open
        onOpenChange={jest.fn()}
        ueberOrdnerId={null}
      />,
    );

    fireEvent.changeText(
      screen.getByTestId("create-ordner-name-input"),
      "Mathematik",
    );

    await act(async () => {
      fireEvent.press(screen.getByTestId("create-ordner-confirm"));
    });

    await waitFor(() => expect(mockToastError).toHaveBeenCalled());
    const message = mockToastError.mock.calls[0][0] as string;
    expect(message.toLowerCase()).toContain("speicher");
  });
});
