import { act, fireEvent, render, screen } from "@testing-library/react-native";
import React from "react";

// ── Dialog mock (same pattern as confirm-dialog.test.tsx) ────────────────────
jest.mock("@/components/ui/dialog", () => {
  const { View, Text } = require("react-native"); // eslint-disable-line @typescript-eslint/no-require-imports
  return {
    Dialog: ({
      children,
      open,
      onOpenChange,
    }: {
      children: React.ReactNode;
      open: boolean;
      onOpenChange?: (open: boolean) => void;
    }) =>
      open ? (
        <View testID="dialog-root">
          {children}
          <View
            testID="dialog-x-button"
            onTouchEnd={() => onOpenChange?.(false)}
            accessibilityRole="button"
          />
        </View>
      ) : null,
    DialogContent: ({ children }: { children: React.ReactNode }) => (
      <View testID="dialog-content">{children}</View>
    ),
    DialogHeader: ({ children }: { children: React.ReactNode }) => (
      <View>{children}</View>
    ),
    DialogTitle: ({ children }: { children: React.ReactNode }) => (
      <Text>{children}</Text>
    ),
  };
});

// ── Separator mock ───────────────────────────────────────────────────────────
jest.mock("@/components/ui/separator", () => {
  const { View } = require("react-native"); // eslint-disable-line @typescript-eslint/no-require-imports
  return { Separator: () => <View testID="separator" /> };
});

// ── Auth mock (dynamic via jest.fn so tests can override the user) ────────────
const mockLogout = jest.fn<Promise<void>, []>();

const MOCK_USER = {
  name: "Anna Schmidt",
  dhbwMail: "anna.schmidt@dhbw-karlsruhe.de",
  studentType: "STUDENT" as const,
  kursName: "TINF24B2",
  pendingKursRequest: null,
};

const mockUseAuth = jest.fn(() => ({
  user: MOCK_USER as typeof MOCK_USER | null,
  isLoading: false,
  login: jest.fn(),
  register: jest.fn(),
  logout: mockLogout,
  refresh: jest.fn(),
  setOnCookieDisabled: jest.fn(),
}));

jest.mock("@/lib/auth/AuthContext", () => ({
  useAuth: (...args: []) => mockUseAuth(...args),
}));

// ── Theme mock ───────────────────────────────────────────────────────────────
const mockSetTheme = jest.fn();

jest.mock("@/lib/theme/ThemeProvider", () => ({
  useTheme: () => ({
    theme: "system",
    effectiveTheme: "light",
    setTheme: mockSetTheme,
  }),
}));

// ── Toast mock ───────────────────────────────────────────────────────────────
// Wrapper functions defer the variable read to call-time, avoiding the
// babel-jest hoisting problem (factory runs before const assignments).
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

// ── Subject ──────────────────────────────────────────────────────────────────
import { SettingsModal } from "@/components/SettingsModal";

// ── Tests ────────────────────────────────────────────────────────────────────

describe("SettingsModal", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Reset to default mock user after each test
    mockUseAuth.mockReturnValue({
      user: MOCK_USER,
      isLoading: false,
      login: jest.fn(),
      register: jest.fn(),
      logout: mockLogout,
      refresh: jest.fn(),
      setOnCookieDisabled: jest.fn(),
    });
  });

  it("does not render when open is false", () => {
    render(<SettingsModal open={false} onOpenChange={jest.fn()} />);

    expect(screen.queryByTestId("dialog-root")).toBeNull();
  });

  it("renders title, user info and logout button when open", () => {
    render(<SettingsModal open={true} onOpenChange={jest.fn()} />);

    expect(screen.getByText("Einstellungen")).toBeTruthy();
    expect(screen.getByTestId("settings-user-info")).toBeTruthy();
    expect(screen.getByText("Anna Schmidt")).toBeTruthy();
    expect(screen.getByText("Student")).toBeTruthy();
    expect(screen.getByTestId("settings-logout-button")).toBeTruthy();
  });

  it("renders theme toggle with System, Dunkel and Hell segments", () => {
    render(<SettingsModal open={true} onOpenChange={jest.fn()} />);

    expect(screen.getByTestId("settings-theme-system")).toBeTruthy();
    expect(screen.getByTestId("settings-theme-dark")).toBeTruthy();
    expect(screen.getByTestId("settings-theme-light")).toBeTruthy();
  });

  it("calls setTheme with 'dark' when Dunkel segment is pressed", () => {
    render(<SettingsModal open={true} onOpenChange={jest.fn()} />);

    fireEvent.press(screen.getByTestId("settings-theme-dark"));

    expect(mockSetTheme).toHaveBeenCalledWith("dark");
  });

  it("calls setTheme with 'light' when Hell segment is pressed", () => {
    render(<SettingsModal open={true} onOpenChange={jest.fn()} />);

    fireEvent.press(screen.getByTestId("settings-theme-light"));

    expect(mockSetTheme).toHaveBeenCalledWith("light");
  });

  it("calls setTheme with 'system' when System segment is pressed", () => {
    render(<SettingsModal open={true} onOpenChange={jest.fn()} />);

    fireEvent.press(screen.getByTestId("settings-theme-system"));

    expect(mockSetTheme).toHaveBeenCalledWith("system");
  });

  it("shows role label 'Kursadmin' for KURSADMIN studentType", () => {
    mockUseAuth.mockReturnValueOnce({
      user: { ...MOCK_USER, studentType: "KURSADMIN" as const },
      isLoading: false,
      login: jest.fn(),
      register: jest.fn(),
      logout: mockLogout,
      refresh: jest.fn(),
      setOnCookieDisabled: jest.fn(),
    });

    render(<SettingsModal open={true} onOpenChange={jest.fn()} />);

    expect(screen.getByText("Kursadmin")).toBeTruthy();
  });

  it("shows role label 'Systemadmin' for SYSTEMADMIN studentType", () => {
    mockUseAuth.mockReturnValueOnce({
      user: { ...MOCK_USER, studentType: "SYSTEMADMIN" as const },
      isLoading: false,
      login: jest.fn(),
      register: jest.fn(),
      logout: mockLogout,
      refresh: jest.fn(),
      setOnCookieDisabled: jest.fn(),
    });

    render(<SettingsModal open={true} onOpenChange={jest.fn()} />);

    expect(screen.getByText("Systemadmin")).toBeTruthy();
  });

  it("calls logout when Abmelden is pressed", async () => {
    mockLogout.mockResolvedValue(undefined);

    render(<SettingsModal open={true} onOpenChange={jest.fn()} />);

    await act(async () => {
      fireEvent.press(screen.getByTestId("settings-logout-button"));
    });

    expect(mockLogout).toHaveBeenCalledTimes(1);
  });

  it("shows spinner and disables logout button while logout is pending", async () => {
    let resolveLogout!: () => void;
    const pendingLogout = new Promise<void>((resolve) => {
      resolveLogout = resolve;
    });
    mockLogout.mockReturnValue(pendingLogout);

    render(<SettingsModal open={true} onOpenChange={jest.fn()} />);

    act(() => {
      fireEvent.press(screen.getByTestId("settings-logout-button"));
    });

    expect(screen.getByTestId("settings-logout-spinner")).toBeTruthy();
    expect(screen.getByTestId("settings-logout-button")).toBeDisabled();

    await act(async () => {
      resolveLogout();
    });
  });

  it("blocks dialog close via onOpenChange while logout is in flight", async () => {
    const onOpenChange = jest.fn();
    let resolveLogout!: () => void;
    const pendingLogout = new Promise<void>((resolve) => {
      resolveLogout = resolve;
    });
    mockLogout.mockReturnValue(pendingLogout);

    render(<SettingsModal open={true} onOpenChange={onOpenChange} />);

    act(() => {
      fireEvent.press(screen.getByTestId("settings-logout-button"));
    });

    // Simulate X-button / backdrop calling onOpenChange(false) while pending
    act(() => {
      fireEvent.press(screen.getByTestId("dialog-x-button"));
    });

    expect(onOpenChange).not.toHaveBeenCalled();

    await act(async () => {
      resolveLogout();
    });
  });
});
