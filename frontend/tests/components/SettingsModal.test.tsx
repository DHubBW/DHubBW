import { act, fireEvent, render, screen } from "@testing-library/react-native";
import React from "react";

// ── Dialog mock ──────────────────────────────────────────────────────────────
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
    DialogDescription: ({ children }: { children: React.ReactNode }) => (
      <Text>{children}</Text>
    ),
    DialogFooter: ({ children }: { children: React.ReactNode }) => (
      <View>{children}</View>
    ),
  };
});

// ── Separator mock ───────────────────────────────────────────────────────────
jest.mock("@/components/ui/separator", () => {
  const { View } = require("react-native"); // eslint-disable-line @typescript-eslint/no-require-imports
  return { Separator: () => <View testID="separator" /> };
});

// ── Auth mock ────────────────────────────────────────────────────────────────
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

// ── expo-router mock ─────────────────────────────────────────────────────────
const mockReplace = jest.fn();
jest.mock("expo-router", () => ({
  useRouter: () => ({ replace: mockReplace }),
}));

// ── useLeaveKurs mock ────────────────────────────────────────────────────────
const mockLeaveKurs = jest.fn<Promise<void>, []>();
jest.mock("@/hooks/useLeaveKurs", () => ({
  useLeaveKurs: () => ({ isPending: false, leaveKurs: mockLeaveKurs }),
}));

// ── useDeleteAccount mock ────────────────────────────────────────────────────
const mockDeleteAccount = jest.fn<Promise<void>, []>();
jest.mock("@/hooks/useDeleteAccount", () => ({
  useDeleteAccount: () => ({
    isPending: false,
    deleteAccount: mockDeleteAccount,
  }),
}));

// ── Subject ──────────────────────────────────────────────────────────────────
import { SettingsModal } from "@/components/SettingsModal";

// ── Tests ────────────────────────────────────────────────────────────────────

describe("SettingsModal", () => {
  beforeEach(() => {
    jest.clearAllMocks();
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
    await act(async () => { resolveLogout(); });
  });

  it("blocks dialog close via onOpenChange while logout is in flight", async () => {
    const onOpenChange = jest.fn();
    let resolveLogout!: () => void;
    const pendingLogout = new Promise<void>((resolve) => { resolveLogout = resolve; });
    mockLogout.mockReturnValue(pendingLogout);
    render(<SettingsModal open={true} onOpenChange={onOpenChange} />);
    act(() => { fireEvent.press(screen.getByTestId("settings-logout-button")); });
    act(() => { fireEvent.press(screen.getByTestId("dialog-x-button")); });
    expect(onOpenChange).not.toHaveBeenCalled();
    await act(async () => { resolveLogout(); });
  });

  // ── Kurs verlassen ────────────────────────────────────────────────────────

  it("shows 'Kurs verlassen' button when user has kursName", () => {
    render(<SettingsModal open={true} onOpenChange={jest.fn()} />);
    expect(screen.getByTestId("settings-leave-kurs-button")).toBeTruthy();
  });

  it("hides 'Kurs verlassen' button when user has no kursName", () => {
    mockUseAuth.mockReturnValueOnce({
      user: { ...MOCK_USER, kursName: null },
      isLoading: false,
      login: jest.fn(),
      register: jest.fn(),
      logout: mockLogout,
      refresh: jest.fn(),
      setOnCookieDisabled: jest.fn(),
    });
    render(<SettingsModal open={true} onOpenChange={jest.fn()} />);
    expect(screen.queryByTestId("settings-leave-kurs-button")).toBeNull();
  });

  it("opens leave-kurs confirm dialog when 'Kurs verlassen' is pressed", () => {
    render(<SettingsModal open={true} onOpenChange={jest.fn()} />);
    fireEvent.press(screen.getByTestId("settings-leave-kurs-button"));
    expect(screen.getByText("Kurs verlassen?")).toBeTruthy();
  });

  it("calls leaveKurs, closes both dialogs, shows success toast and navigates to dashboard on confirm", async () => {
    mockLeaveKurs.mockResolvedValue(undefined);
    const onOpenChange = jest.fn();
    render(<SettingsModal open={true} onOpenChange={onOpenChange} />);
    fireEvent.press(screen.getByTestId("settings-leave-kurs-button"));
    expect(screen.getByTestId("confirm-dialog-confirm")).toBeTruthy();
    await act(async () => {
      fireEvent.press(screen.getByTestId("confirm-dialog-confirm"));
    });
    expect(mockLeaveKurs).toHaveBeenCalledTimes(1);
    expect(onOpenChange).toHaveBeenCalledWith(false);
    expect(mockToastSuccess).toHaveBeenCalledWith("Kurs verlassen");
  });

  it("closes dialogs and shows KURSADMIN toast when leaveKurs returns 400 (status 0, KURSADMIN body)", async () => {
    const { ApiError } = jest.requireActual(
      "@/lib/api/errors",
    ) as typeof import("@/lib/api/errors");
    // fetchClient maps HTTP 400 → ApiError(0) because 400 is not in ApiErrorStatus.
    // The body contains the plain-text response from RequestStatusResponseDTO.
    mockLeaveKurs.mockRejectedValue(
      new ApiError(0, "HTTP 400", {
        response:
          "Cannot leave course: You are the last KURSADMIN in this course.",
        persistenceOperation: "DELETE",
      }),
    );
    const onOpenChange = jest.fn();
    render(<SettingsModal open={true} onOpenChange={onOpenChange} />);
    fireEvent.press(screen.getByTestId("settings-leave-kurs-button"));
    await act(async () => {
      fireEvent.press(screen.getByTestId("confirm-dialog-confirm"));
    });
    expect(mockToastError).toHaveBeenCalledWith(
      "Tritt zuerst als Kursadmin zurück",
    );
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it("closes dialogs and shows generic toast when leaveKurs fails with unknown error", async () => {
    const { ApiError } = jest.requireActual(
      "@/lib/api/errors",
    ) as typeof import("@/lib/api/errors");
    mockLeaveKurs.mockRejectedValue(new ApiError(0, "Network error"));
    const onOpenChange = jest.fn();
    render(<SettingsModal open={true} onOpenChange={onOpenChange} />);
    fireEvent.press(screen.getByTestId("settings-leave-kurs-button"));
    await act(async () => {
      fireEvent.press(screen.getByTestId("confirm-dialog-confirm"));
    });
    expect(mockToastError).toHaveBeenCalledWith("Kurs verlassen fehlgeschlagen");
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  // ── Account löschen ───────────────────────────────────────────────────────

  it("always shows 'Account löschen' button", () => {
    render(<SettingsModal open={true} onOpenChange={jest.fn()} />);
    expect(screen.getByTestId("settings-delete-account-button")).toBeTruthy();
  });

  it("opens delete-account strict confirm dialog when button is pressed", () => {
    render(<SettingsModal open={true} onOpenChange={jest.fn()} />);
    fireEvent.press(screen.getByTestId("settings-delete-account-button"));
    expect(screen.getByText("Account löschen?")).toBeTruthy();
    expect(screen.getByTestId("confirm-dialog-strict-input")).toBeTruthy();
  });

  it("confirm button is disabled until 'LÖSCHEN' is typed", () => {
    render(<SettingsModal open={true} onOpenChange={jest.fn()} />);
    fireEvent.press(screen.getByTestId("settings-delete-account-button"));
    expect(screen.getByTestId("confirm-dialog-strict-confirm")).toBeDisabled();
    fireEvent.changeText(
      screen.getByTestId("confirm-dialog-strict-input"),
      "LÖSCHEN",
    );
    expect(
      screen.getByTestId("confirm-dialog-strict-confirm"),
    ).not.toBeDisabled();
  });

  it("calls deleteAccount, shows success toast and calls logout for AuthContext cleanup on confirm", async () => {
    mockDeleteAccount.mockResolvedValue(undefined);
    mockLogout.mockResolvedValue(undefined);
    const onOpenChange = jest.fn();
    render(<SettingsModal open={true} onOpenChange={onOpenChange} />);
    fireEvent.press(screen.getByTestId("settings-delete-account-button"));
    fireEvent.changeText(
      screen.getByTestId("confirm-dialog-strict-input"),
      "LÖSCHEN",
    );
    await act(async () => {
      fireEvent.press(screen.getByTestId("confirm-dialog-strict-confirm"));
    });
    expect(mockDeleteAccount).toHaveBeenCalledTimes(1);
    expect(mockToastSuccess).toHaveBeenCalledWith("Konto gelöscht");
    // logout() is called for AuthContext cleanup after successful account deletion
    expect(mockLogout).toHaveBeenCalledTimes(1);
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it("closes dialogs and shows last-Sysadmin toast when deleteAccount returns 409 (SYSTEMADMIN body)", async () => {
    const { ApiError } = jest.requireActual(
      "@/lib/api/errors",
    ) as typeof import("@/lib/api/errors");
    // GlobalExceptionHandler → ResponseEntity<String> → plain text → ApiError(409, msg, msg)
    mockDeleteAccount.mockRejectedValue(
      new ApiError(
        409,
        "Cannot delete account: You are the last SYSTEMADMIN",
        "Cannot delete account: You are the last SYSTEMADMIN",
      ),
    );
    const onOpenChange = jest.fn();
    render(<SettingsModal open={true} onOpenChange={onOpenChange} />);
    fireEvent.press(screen.getByTestId("settings-delete-account-button"));
    fireEvent.changeText(
      screen.getByTestId("confirm-dialog-strict-input"),
      "LÖSCHEN",
    );
    await act(async () => {
      fireEvent.press(screen.getByTestId("confirm-dialog-strict-confirm"));
    });
    expect(mockToastError).toHaveBeenCalledWith(
      "Du bist der letzte Systemadmin. Ernenne zuerst einen anderen.",
    );
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it("closes dialogs and shows last-Kursadmin toast when deleteAccount returns 409 (KURSADMIN body)", async () => {
    const { ApiError } = jest.requireActual(
      "@/lib/api/errors",
    ) as typeof import("@/lib/api/errors");
    mockDeleteAccount.mockRejectedValue(
      new ApiError(
        409,
        "Cannot leave course or delete account: You are the last KURSADMIN in this course",
        "Cannot leave course or delete account: You are the last KURSADMIN in this course",
      ),
    );
    const onOpenChange = jest.fn();
    render(<SettingsModal open={true} onOpenChange={onOpenChange} />);
    fireEvent.press(screen.getByTestId("settings-delete-account-button"));
    fireEvent.changeText(
      screen.getByTestId("confirm-dialog-strict-input"),
      "LÖSCHEN",
    );
    await act(async () => {
      fireEvent.press(screen.getByTestId("confirm-dialog-strict-confirm"));
    });
    expect(mockToastError).toHaveBeenCalledWith(
      "Tritt zuerst als Kursadmin zurück, bevor du den Account löschst.",
    );
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it("closes dialogs and shows generic toast when deleteAccount fails with unknown error", async () => {
    const { ApiError } = jest.requireActual(
      "@/lib/api/errors",
    ) as typeof import("@/lib/api/errors");
    mockDeleteAccount.mockRejectedValue(new ApiError(0, "Network error"));
    const onOpenChange = jest.fn();
    render(<SettingsModal open={true} onOpenChange={onOpenChange} />);
    fireEvent.press(screen.getByTestId("settings-delete-account-button"));
    fireEvent.changeText(
      screen.getByTestId("confirm-dialog-strict-input"),
      "LÖSCHEN",
    );
    await act(async () => {
      fireEvent.press(screen.getByTestId("confirm-dialog-strict-confirm"));
    });
    expect(mockToastError).toHaveBeenCalledWith("Konto löschen fehlgeschlagen");
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });
});
