import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react-native";

import { ConfirmDialog } from "@/components/ConfirmDialog";
import { ConfirmDialogStrict } from "@/components/ConfirmDialogStrict";

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
          {/* Simulate the built-in X close button from DialogContent */}
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

// ─── ConfirmDialog ───────────────────────────────────────────────────────────

describe("ConfirmDialog", () => {
  it("renders title and message with all required props", () => {
    render(
      <ConfirmDialog
        open={true}
        onOpenChange={jest.fn()}
        title="Datei löschen"
        message="Möchtest du diese Datei wirklich löschen?"
        onConfirm={jest.fn()}
      />,
    );

    expect(screen.getByText("Datei löschen")).toBeTruthy();
    expect(
      screen.getByText("Möchtest du diese Datei wirklich löschen?"),
    ).toBeTruthy();
  });

  it("renders custom confirmLabel and cancelLabel when provided", () => {
    render(
      <ConfirmDialog
        open={true}
        onOpenChange={jest.fn()}
        title="Datei löschen"
        message="Sicher?"
        onConfirm={jest.fn()}
        confirmLabel="Ja, löschen"
        cancelLabel="Nein"
        variant="destructive"
      />,
    );

    expect(screen.getByText("Ja, löschen")).toBeTruthy();
    expect(screen.getByText("Nein")).toBeTruthy();
  });

  it("renders default labels when confirmLabel and cancelLabel are omitted", () => {
    render(
      <ConfirmDialog
        open={true}
        onOpenChange={jest.fn()}
        title="Test"
        message="Test"
        onConfirm={jest.fn()}
      />,
    );

    expect(screen.getByText("Bestätigen")).toBeTruthy();
    expect(screen.getByText("Abbrechen")).toBeTruthy();
  });

  it("calls onOpenChange(false) when Cancel is pressed", () => {
    const onOpenChange = jest.fn();

    render(
      <ConfirmDialog
        open={true}
        onOpenChange={onOpenChange}
        title="Datei löschen"
        message="Möchtest du diese Datei wirklich löschen?"
        onConfirm={jest.fn()}
      />,
    );

    fireEvent.press(screen.getByTestId("confirm-dialog-cancel"));

    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it("calls onConfirm when Confirm is pressed (async)", async () => {
    const onConfirm = jest.fn().mockResolvedValue(undefined);

    render(
      <ConfirmDialog
        open={true}
        onOpenChange={jest.fn()}
        title="Datei löschen"
        message="Möchtest du diese Datei wirklich löschen?"
        onConfirm={onConfirm}
      />,
    );

    await act(async () => {
      fireEvent.press(screen.getByTestId("confirm-dialog-confirm"));
    });

    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it("calls onConfirm when Confirm is pressed (synchronous)", async () => {
    const onConfirm = jest.fn();

    render(
      <ConfirmDialog
        open={true}
        onOpenChange={jest.fn()}
        title="Datei löschen"
        message="Möchtest du diese Datei wirklich löschen?"
        onConfirm={onConfirm}
      />,
    );

    await act(async () => {
      fireEvent.press(screen.getByTestId("confirm-dialog-confirm"));
    });

    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it("shows spinner and disables buttons while onConfirm is pending", async () => {
    let resolveConfirm!: () => void;
    const pendingPromise = new Promise<void>((resolve) => {
      resolveConfirm = resolve;
    });
    const onConfirm = jest.fn().mockReturnValue(pendingPromise);

    render(
      <ConfirmDialog
        open={true}
        onOpenChange={jest.fn()}
        title="Datei löschen"
        message="Möchtest du diese Datei wirklich löschen?"
        onConfirm={onConfirm}
      />,
    );

    act(() => {
      fireEvent.press(screen.getByTestId("confirm-dialog-confirm"));
    });

    expect(screen.getByTestId("confirm-dialog-spinner")).toBeTruthy();
    expect(screen.getByTestId("confirm-dialog-confirm")).toBeDisabled();
    expect(screen.getByTestId("confirm-dialog-cancel")).toBeDisabled();

    await act(async () => {
      resolveConfirm();
    });
  });

  it("does not call onConfirm a second time when pressed while pending (double-tap guard)", async () => {
    let resolveConfirm!: () => void;
    const pendingPromise = new Promise<void>((resolve) => {
      resolveConfirm = resolve;
    });
    const onConfirm = jest.fn().mockReturnValue(pendingPromise);

    render(
      <ConfirmDialog
        open={true}
        onOpenChange={jest.fn()}
        title="Datei löschen"
        message="Möchtest du diese Datei wirklich löschen?"
        onConfirm={onConfirm}
      />,
    );

    act(() => {
      fireEvent.press(screen.getByTestId("confirm-dialog-confirm"));
    });

    // Second press while pending — button is disabled, must not fire onConfirm again
    fireEvent.press(screen.getByTestId("confirm-dialog-confirm"));

    expect(onConfirm).toHaveBeenCalledTimes(1);

    await act(async () => {
      resolveConfirm();
    });
  });

  it("blocks dialog close via onOpenChange while isPending (X-button / backdrop guard)", async () => {
    const onOpenChange = jest.fn();
    let resolveConfirm!: () => void;
    const pendingPromise = new Promise<void>((resolve) => {
      resolveConfirm = resolve;
    });

    render(
      <ConfirmDialog
        open={true}
        onOpenChange={onOpenChange}
        title="Datei löschen"
        message="Möchtest du diese Datei wirklich löschen?"
        onConfirm={jest.fn().mockReturnValue(pendingPromise)}
      />,
    );

    act(() => {
      fireEvent.press(screen.getByTestId("confirm-dialog-confirm"));
    });

    // Simulate X-button / backdrop calling onOpenChange(false) while pending
    act(() => {
      fireEvent.press(screen.getByTestId("dialog-x-button"));
    });

    // onOpenChange must NOT have been called while isPending
    expect(onOpenChange).not.toHaveBeenCalled();

    await act(async () => {
      resolveConfirm();
    });
  });

  it("confirm button is enabled with destructive variant", () => {
    render(
      <ConfirmDialog
        open={true}
        onOpenChange={jest.fn()}
        title="Datei löschen"
        message="Möchtest du diese Datei wirklich löschen?"
        onConfirm={jest.fn()}
        variant="destructive"
      />,
    );

    const confirmButton = screen.getByTestId("confirm-dialog-confirm");
    expect(confirmButton).toBeTruthy();
    expect(confirmButton.props.accessibilityState?.disabled).toBeFalsy();
  });

  it("does not render when open is false", () => {
    render(
      <ConfirmDialog
        open={false}
        onOpenChange={jest.fn()}
        title="Datei löschen"
        message="Sicher?"
        onConfirm={jest.fn()}
      />,
    );

    expect(screen.queryByTestId("dialog-root")).toBeNull();
  });
});

// ─── ConfirmDialogStrict ─────────────────────────────────────────────────────

describe("ConfirmDialogStrict", () => {
  it("renders title, message and hint with all required props", () => {
    render(
      <ConfirmDialogStrict
        open={true}
        onOpenChange={jest.fn()}
        title="Kurs löschen"
        message="Diese Aktion kann nicht rückgängig gemacht werden."
        verificationText="TINF24B2"
        onConfirm={jest.fn()}
      />,
    );

    expect(screen.getByText("Kurs löschen")).toBeTruthy();
    expect(
      screen.getByText("Diese Aktion kann nicht rückgängig gemacht werden."),
    ).toBeTruthy();
    expect(screen.getByText("Gib «TINF24B2» ein um fortzufahren")).toBeTruthy();
  });

  it("renders custom confirmLabel and cancelLabel when provided", () => {
    render(
      <ConfirmDialogStrict
        open={true}
        onOpenChange={jest.fn()}
        title="Konto löschen"
        message="Endgültig?"
        verificationText="LÖSCHEN"
        onConfirm={jest.fn()}
        confirmLabel="Ja, endgültig löschen"
        cancelLabel="Abbrechen"
        variant="destructive"
      />,
    );

    expect(screen.getByText("Ja, endgültig löschen")).toBeTruthy();
    expect(screen.getByText("Abbrechen")).toBeTruthy();
  });

  it("calls onOpenChange(false) when Cancel is pressed", () => {
    const onOpenChange = jest.fn();

    render(
      <ConfirmDialogStrict
        open={true}
        onOpenChange={onOpenChange}
        title="Kurs löschen"
        message="Diese Aktion kann nicht rückgängig gemacht werden."
        verificationText="TINF24B2"
        onConfirm={jest.fn()}
      />,
    );

    fireEvent.press(screen.getByTestId("confirm-dialog-strict-cancel"));

    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it("confirm button is disabled when input is empty", () => {
    render(
      <ConfirmDialogStrict
        open={true}
        onOpenChange={jest.fn()}
        title="Kurs löschen"
        message="Diese Aktion kann nicht rückgängig gemacht werden."
        verificationText="TINF24B2"
        onConfirm={jest.fn()}
      />,
    );

    expect(screen.getByTestId("confirm-dialog-strict-confirm")).toBeDisabled();
  });

  it("confirm button is disabled when input does not match verificationText (case-sensitive)", () => {
    render(
      <ConfirmDialogStrict
        open={true}
        onOpenChange={jest.fn()}
        title="Kurs löschen"
        message="Diese Aktion kann nicht rückgängig gemacht werden."
        verificationText="TINF24B2"
        onConfirm={jest.fn()}
      />,
    );

    fireEvent.changeText(
      screen.getByTestId("confirm-dialog-strict-input"),
      "tinf24b2",
    );

    expect(screen.getByTestId("confirm-dialog-strict-confirm")).toBeDisabled();
  });

  it("confirm button becomes enabled when input exactly matches verificationText", () => {
    render(
      <ConfirmDialogStrict
        open={true}
        onOpenChange={jest.fn()}
        title="Kurs löschen"
        message="Diese Aktion kann nicht rückgängig gemacht werden."
        verificationText="TINF24B2"
        onConfirm={jest.fn()}
      />,
    );

    fireEvent.changeText(
      screen.getByTestId("confirm-dialog-strict-input"),
      "TINF24B2",
    );

    expect(
      screen.getByTestId("confirm-dialog-strict-confirm"),
    ).not.toBeDisabled();
  });

  it("calls onConfirm when input matches and Confirm is pressed (async)", async () => {
    const onConfirm = jest.fn().mockResolvedValue(undefined);

    render(
      <ConfirmDialogStrict
        open={true}
        onOpenChange={jest.fn()}
        title="Kurs löschen"
        message="Diese Aktion kann nicht rückgängig gemacht werden."
        verificationText="TINF24B2"
        onConfirm={onConfirm}
      />,
    );

    fireEvent.changeText(
      screen.getByTestId("confirm-dialog-strict-input"),
      "TINF24B2",
    );

    await act(async () => {
      fireEvent.press(screen.getByTestId("confirm-dialog-strict-confirm"));
    });

    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it("calls onConfirm when input matches and Confirm is pressed (synchronous)", async () => {
    const onConfirm = jest.fn();

    render(
      <ConfirmDialogStrict
        open={true}
        onOpenChange={jest.fn()}
        title="Kurs löschen"
        message="Diese Aktion kann nicht rückgängig gemacht werden."
        verificationText="TINF24B2"
        onConfirm={onConfirm}
      />,
    );

    fireEvent.changeText(
      screen.getByTestId("confirm-dialog-strict-input"),
      "TINF24B2",
    );

    await act(async () => {
      fireEvent.press(screen.getByTestId("confirm-dialog-strict-confirm"));
    });

    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it("shows spinner and disables input and buttons while onConfirm is pending", async () => {
    let resolveConfirm!: () => void;
    const pendingPromise = new Promise<void>((resolve) => {
      resolveConfirm = resolve;
    });
    const onConfirm = jest.fn().mockReturnValue(pendingPromise);

    render(
      <ConfirmDialogStrict
        open={true}
        onOpenChange={jest.fn()}
        title="Kurs löschen"
        message="Diese Aktion kann nicht rückgängig gemacht werden."
        verificationText="TINF24B2"
        onConfirm={onConfirm}
      />,
    );

    fireEvent.changeText(
      screen.getByTestId("confirm-dialog-strict-input"),
      "TINF24B2",
    );

    act(() => {
      fireEvent.press(screen.getByTestId("confirm-dialog-strict-confirm"));
    });

    expect(screen.getByTestId("confirm-dialog-strict-spinner")).toBeTruthy();
    expect(screen.getByTestId("confirm-dialog-strict-confirm")).toBeDisabled();
    expect(screen.getByTestId("confirm-dialog-strict-cancel")).toBeDisabled();
    expect(
      screen.getByTestId("confirm-dialog-strict-input").props.editable,
    ).toBe(false);

    await act(async () => {
      resolveConfirm();
    });
  });

  it("resets input after successful async confirm", async () => {
    const onConfirm = jest.fn().mockResolvedValue(undefined);

    render(
      <ConfirmDialogStrict
        open={true}
        onOpenChange={jest.fn()}
        title="Kurs löschen"
        message="Diese Aktion kann nicht rückgängig gemacht werden."
        verificationText="TINF24B2"
        onConfirm={onConfirm}
      />,
    );

    fireEvent.changeText(
      screen.getByTestId("confirm-dialog-strict-input"),
      "TINF24B2",
    );

    await act(async () => {
      fireEvent.press(screen.getByTestId("confirm-dialog-strict-confirm"));
    });

    expect(screen.getByTestId("confirm-dialog-strict-input").props.value).toBe(
      "",
    );
  });

  it("resets input after rejected async confirm (input cleared in finally)", async () => {
    const onConfirm = jest.fn().mockRejectedValue(new Error("Server error"));

    render(
      <ConfirmDialogStrict
        open={true}
        onOpenChange={jest.fn()}
        title="Kurs löschen"
        message="Diese Aktion kann nicht rückgängig gemacht werden."
        verificationText="TINF24B2"
        onConfirm={onConfirm}
      />,
    );

    fireEvent.changeText(
      screen.getByTestId("confirm-dialog-strict-input"),
      "TINF24B2",
    );

    await act(async () => {
      fireEvent.press(screen.getByTestId("confirm-dialog-strict-confirm"));
    });

    // Input must be cleared even when onConfirm rejects, so user must retype
    expect(screen.getByTestId("confirm-dialog-strict-input").props.value).toBe(
      "",
    );
  });

  it("blocks dialog close while isPending (X-button / backdrop guard)", async () => {
    const onOpenChange = jest.fn();
    let resolveConfirm!: () => void;
    const pendingPromise = new Promise<void>((resolve) => {
      resolveConfirm = resolve;
    });

    render(
      <ConfirmDialogStrict
        open={true}
        onOpenChange={onOpenChange}
        title="Kurs löschen"
        message="Diese Aktion kann nicht rückgängig gemacht werden."
        verificationText="TINF24B2"
        onConfirm={jest.fn().mockReturnValue(pendingPromise)}
      />,
    );

    fireEvent.changeText(
      screen.getByTestId("confirm-dialog-strict-input"),
      "TINF24B2",
    );

    act(() => {
      fireEvent.press(screen.getByTestId("confirm-dialog-strict-confirm"));
    });

    // Simulate X-button / backdrop calling onOpenChange(false) while pending
    act(() => {
      fireEvent.press(screen.getByTestId("dialog-x-button"));
    });

    expect(onOpenChange).not.toHaveBeenCalled();

    await act(async () => {
      resolveConfirm();
    });
  });

  it("resets input when dialog is closed programmatically (open=false)", async () => {
    const { rerender } = render(
      <ConfirmDialogStrict
        open={true}
        onOpenChange={jest.fn()}
        title="Kurs löschen"
        message="Diese Aktion kann nicht rückgängig gemacht werden."
        verificationText="TINF24B2"
        onConfirm={jest.fn()}
      />,
    );

    fireEvent.changeText(
      screen.getByTestId("confirm-dialog-strict-input"),
      "TINF24B2",
    );

    // Parent closes dialog without going through onOpenChange (programmatic close)
    rerender(
      <ConfirmDialogStrict
        open={false}
        onOpenChange={jest.fn()}
        title="Kurs löschen"
        message="Diese Aktion kann nicht rückgängig gemacht werden."
        verificationText="TINF24B2"
        onConfirm={jest.fn()}
      />,
    );

    // Re-open and check input is cleared
    rerender(
      <ConfirmDialogStrict
        open={true}
        onOpenChange={jest.fn()}
        title="Kurs löschen"
        message="Diese Aktion kann nicht rückgängig gemacht werden."
        verificationText="TINF24B2"
        onConfirm={jest.fn()}
      />,
    );

    expect(screen.getByTestId("confirm-dialog-strict-input").props.value).toBe(
      "",
    );
  });
});
