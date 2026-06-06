import * as React from "react";
import { ActivityIndicator } from "react-native";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Text } from "@/components/ui/text";
import { t } from "@/lib/i18n/t";
import type { ConfirmDialogProps } from "@/components/ConfirmDialog";

/**
 * Props for the {@link ConfirmDialogStrict} component.
 *
 * Extends the base confirm-dialog props (minus `variant`, which defaults here
 * to `'destructive'`) with a mandatory `verificationText` field. The confirm
 * button stays disabled until the user has typed that exact string into the
 * verification input (case-sensitive).
 *
 * @public
 */
export type ConfirmDialogStrictProps = Omit<ConfirmDialogProps, "variant"> & {
  /**
   * The exact text the user must type to enable the confirm button.
   * Comparison is case-sensitive (strict equality).
   */
  verificationText: string;
  /**
   * Visual variant of the confirm button.
   * Defaults to `'destructive'` — override only when a non-destructive
   * strict confirmation is intentional.
   */
  variant?: "default" | "destructive";
};

/**
 * A confirmation dialog that requires the user to type a specific string
 * before the confirm action becomes available.
 *
 * Useful for irreversible operations (e.g. deleting a course, deleting an
 * account) where an accidental tap must be prevented. The confirm button is
 * disabled until `inputValue === verificationText` (case-sensitive). While
 * `onConfirm` is in flight, both buttons and the input are disabled and a
 * loading spinner is shown in the confirm button.
 *
 * The dialog cannot be dismissed (Cancel, X button, or backdrop) while
 * `onConfirm` is in flight. The input value resets to `''` whenever the
 * dialog closes or after `onConfirm` settles (resolve or reject).
 *
 * @param props - {@link ConfirmDialogStrictProps}
 * @returns A controlled strict-confirm `Dialog` element.
 */
export function ConfirmDialogStrict({
  open,
  onOpenChange,
  title,
  message,
  confirmLabel,
  cancelLabel,
  onConfirm,
  verificationText,
  variant = "destructive",
}: ConfirmDialogStrictProps): React.JSX.Element {
  const [inputValue, setInputValue] = React.useState("");
  const [isPending, setIsPending] = React.useState(false);

  const isMatch = inputValue === verificationText;

  // Reset local state when the dialog closes — handles both onOpenChange(false)
  // and programmatic close (parent sets open=false directly).
  React.useEffect(() => {
    if (!open) {
      setInputValue("");
      setIsPending(false);
    }
  }, [open]);

  function handleOpenChange(nextOpen: boolean) {
    if (isPending) return;
    onOpenChange(nextOpen);
  }

  async function handleConfirm() {
    // Set pending synchronously before calling onConfirm so the button is
    // disabled in the same render batch, preventing double-invocation.
    setIsPending(true);
    try {
      await Promise.resolve(onConfirm());
    } catch {
      // Rejection is handled by the caller (e.g. toast / error state).
      // The dialog stays open; state is cleaned up in finally.
    } finally {
      setInputValue("");
      setIsPending(false);
    }
  }

  const resolvedConfirmLabel =
    confirmLabel ?? t("confirm.confirm", "Bestätigen");
  const resolvedCancelLabel = cancelLabel ?? t("confirm.cancel", "Abbrechen");
  // TODO(i18n): when real i18n is wired in, replace the template-literal fallback
  // with a parameterized translation key, e.g. t("confirmStrict.hint", { text: verificationText }).
  const hint = t(
    "confirmStrict.hint",
    `Gib «${verificationText}» ein um fortzufahren`,
  );

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{message}</DialogDescription>
        </DialogHeader>
        <Text className="mt-1 text-sm text-muted-foreground">{hint}</Text>
        <Input
          testID="confirm-dialog-strict-input"
          placeholder={verificationText}
          value={inputValue}
          onChangeText={setInputValue}
          editable={!isPending}
          autoCapitalize="none"
          autoCorrect={false}
        />
        <DialogFooter>
          <Button
            testID="confirm-dialog-strict-cancel"
            variant="outline"
            onPress={() => handleOpenChange(false)}
            disabled={isPending}
          >
            <Text>{resolvedCancelLabel}</Text>
          </Button>
          <Button
            testID="confirm-dialog-strict-confirm"
            variant={variant}
            onPress={handleConfirm}
            disabled={!isMatch || isPending}
          >
            {isPending ? (
              // ActivityIndicator.color cannot accept a NativeWind className.
              // --destructive-foreground and --primary-foreground both resolve
              // to white (#fff) in all themes, so this value is correct
              // regardless of the active theme.
              <ActivityIndicator
                testID="confirm-dialog-strict-spinner"
                size="small"
                color="white"
              />
            ) : (
              <Text>{resolvedConfirmLabel}</Text>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
