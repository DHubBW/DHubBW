/**
 * Modal dialog for the "Passwort ändern" action.
 *
 * Hosts three secure-text inputs with show/hide toggles (Eye / EyeOff)
 * and forwards the values to {@link useChangePassword}. Submission is
 * disabled while any field is empty, while the new and repeated
 * passwords disagree, while the new password fails to satisfy the
 * shared password policy (see {@link evaluatePassword}), or while the
 * API call is in flight.
 *
 * Errors surface as toasts mapped from the {@link ApiError} status
 * via {@link toastForApiError}; the dialog stays open on failure so
 * the user can retry. On success, a success toast fires, the dialog
 * closes, and inputs are cleared.
 */
import { Eye, EyeOff } from "lucide-react-native";
import * as React from "react";
import { ActivityIndicator, Pressable, View } from "react-native";
import { toast } from "sonner-native";

import { PasswordStrengthList } from "@/components/auth/PasswordStrengthList";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Icon } from "@/components/ui/icon";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Text } from "@/components/ui/text";
import { useChangePassword } from "@/hooks/useChangePassword";
import { toastForApiError } from "@/lib/api/toastForApiError";
import { evaluatePassword } from "@/lib/auth/passwordPolicy";
import { t } from "@/lib/i18n/t";

/** Props for the {@link ChangePasswordDialog} component. */
export interface ChangePasswordDialogProps {
  /** Controls dialog visibility. */
  open: boolean;
  /** Called when the dialog should open or close. */
  onOpenChange: (open: boolean) => void;
}

/**
 * Maps an error from the change-password endpoint to the toast text
 * shown to the user. Backend body text wins (German operator-written
 * copy); the fallback map only kicks in when no body is present.
 *
 * @param err - The thrown error from the change-password call.
 * @returns The localised toast message.
 */
function _changePasswordErrorMessage(err: unknown): string {
  return toastForApiError(err, {
    400: t(
      "settings.password.error.invalid",
      "Eingaben ungültig. Bitte prüfen.",
    ),
    401: t(
      "settings.password.error.wrongCurrent",
      "Aktuelles Passwort ist falsch",
    ),
  });
}

/**
 * Renders the change-password dialog.
 *
 * @param props - {@link ChangePasswordDialogProps}
 * @returns The dialog element.
 */
export function ChangePasswordDialog({
  open,
  onOpenChange,
}: ChangePasswordDialogProps): React.JSX.Element {
  const { changePassword, isPending } = useChangePassword();

  const [currentPassword, setCurrentPassword] = React.useState("");
  const [newPassword, setNewPassword] = React.useState("");
  const [repeatPassword, setRepeatPassword] = React.useState("");
  const [showCurrent, setShowCurrent] = React.useState(false);
  const [showNew, setShowNew] = React.useState(false);
  const [showRepeat, setShowRepeat] = React.useState(false);
  const [newTouched, setNewTouched] = React.useState(false);
  const [repeatTouched, setRepeatTouched] = React.useState(false);

  // Reset the form whenever the dialog reopens — a previous failed
  // submit could otherwise leak state into the next attempt.
  const lastOpenRef = React.useRef(open);
  React.useEffect(() => {
    if (open && !lastOpenRef.current) {
      setCurrentPassword("");
      setNewPassword("");
      setRepeatPassword("");
      setShowCurrent(false);
      setShowNew(false);
      setShowRepeat(false);
      setNewTouched(false);
      setRepeatTouched(false);
    }
    lastOpenRef.current = open;
  }, [open]);

  const passwordsMismatch =
    newPassword.length > 0
    && repeatPassword.length > 0
    && newPassword !== repeatPassword;
  const showMismatch =
    newTouched && repeatTouched && passwordsMismatch;

  const canSubmit =
    !isPending
    && currentPassword.length > 0
    && evaluatePassword(newPassword).allPassed
    && repeatPassword.length > 0
    && newPassword === repeatPassword;

  function handleOpenChange(nextOpen: boolean) {
    if (isPending) return;
    onOpenChange(nextOpen);
  }

  async function handleSubmit() {
    if (!canSubmit) return;
    try {
      await changePassword(currentPassword, newPassword, repeatPassword);
      toast.success(
        t("settings.password.success", "Passwort geändert"),
      );
      setCurrentPassword("");
      setNewPassword("");
      setRepeatPassword("");
      setNewTouched(false);
      setRepeatTouched(false);
      onOpenChange(false);
    } catch (err) {
      toast.error(_changePasswordErrorMessage(err));
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {t("settings.password.title", "Passwort ändern")}
          </DialogTitle>
          <DialogDescription>
            {t(
              "settings.password.description",
              "Gib dein aktuelles Passwort ein und wähle ein neues, "
                + "das alle unten gezeigten Anforderungen erfüllt.",
            )}
          </DialogDescription>
        </DialogHeader>

        {/* Current password */}
        <View className="gap-1.5 py-1">
          <Label nativeID="change-password-current-label">
            {t(
              "settings.password.currentLabel",
              "Aktuelles Passwort",
            )}
          </Label>
          <View className="flex-row items-center gap-2">
            <View className="flex-1">
              <Input
                aria-labelledby="change-password-current-label"
                testID="change-password-current"
                value={currentPassword}
                onChangeText={setCurrentPassword}
                secureTextEntry={!showCurrent}
                autoCapitalize="none"
                autoComplete="current-password"
                autoCorrect={false}
                textContentType="password"
                editable={!isPending}
              />
            </View>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={
                showCurrent
                  ? t(
                      "settings.password.hide",
                      "Passwort verbergen",
                    )
                  : t(
                      "settings.password.show",
                      "Passwort anzeigen",
                    )
              }
              onPress={() => setShowCurrent((v) => !v)}
              disabled={isPending}
              className="h-10 w-10 items-center justify-center rounded-md border border-border sm:h-9 sm:w-9"
            >
              <Icon
                as={showCurrent ? EyeOff : Eye}
                className="size-4 text-muted-foreground"
              />
            </Pressable>
          </View>
        </View>

        {/* New password */}
        <View className="gap-1.5 py-1">
          <Label nativeID="change-password-new-label">
            {t("settings.password.newLabel", "Neues Passwort")}
          </Label>
          <View className="flex-row items-center gap-2">
            <View className="flex-1">
              <Input
                aria-labelledby="change-password-new-label"
                testID="change-password-new"
                value={newPassword}
                onChangeText={(v) => {
                  setNewPassword(v);
                  setNewTouched(true);
                }}
                secureTextEntry={!showNew}
                autoCapitalize="none"
                autoComplete="new-password"
                autoCorrect={false}
                textContentType="newPassword"
                editable={!isPending}
              />
            </View>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={
                showNew
                  ? t(
                      "settings.password.hide",
                      "Passwort verbergen",
                    )
                  : t(
                      "settings.password.show",
                      "Passwort anzeigen",
                    )
              }
              onPress={() => setShowNew((v) => !v)}
              disabled={isPending}
              className="h-10 w-10 items-center justify-center rounded-md border border-border sm:h-9 sm:w-9"
            >
              <Icon
                as={showNew ? EyeOff : Eye}
                className="size-4 text-muted-foreground"
              />
            </Pressable>
          </View>
          <PasswordStrengthList value={newPassword} />
        </View>

        {/* Repeat new password */}
        <View className="gap-1.5 py-1">
          <Label nativeID="change-password-repeat-label">
            {t(
              "settings.password.repeatLabel",
              "Neues Passwort wiederholen",
            )}
          </Label>
          <View className="flex-row items-center gap-2">
            <View className="flex-1">
              <Input
                aria-labelledby="change-password-repeat-label"
                testID="change-password-repeat"
                value={repeatPassword}
                onChangeText={(v) => {
                  setRepeatPassword(v);
                  setRepeatTouched(true);
                }}
                secureTextEntry={!showRepeat}
                autoCapitalize="none"
                autoComplete="new-password"
                autoCorrect={false}
                textContentType="newPassword"
                editable={!isPending}
                onSubmitEditing={handleSubmit}
              />
            </View>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={
                showRepeat
                  ? t(
                      "settings.password.hide",
                      "Passwort verbergen",
                    )
                  : t(
                      "settings.password.show",
                      "Passwort anzeigen",
                    )
              }
              onPress={() => setShowRepeat((v) => !v)}
              disabled={isPending}
              className="h-10 w-10 items-center justify-center rounded-md border border-border sm:h-9 sm:w-9"
            >
              <Icon
                as={showRepeat ? EyeOff : Eye}
                className="size-4 text-muted-foreground"
              />
            </Pressable>
          </View>
          {showMismatch && (
            <Text
              testID="change-password-mismatch"
              className="mt-1 text-xs text-destructive"
            >
              {t(
                "settings.password.mismatch",
                "Neue Passwörter stimmen nicht überein",
              )}
            </Text>
          )}
        </View>

        <DialogFooter>
          <Button
            testID="change-password-cancel"
            variant="outline"
            onPress={() => handleOpenChange(false)}
            disabled={isPending}
          >
            <Text>{t("confirm.cancel", "Abbrechen")}</Text>
          </Button>
          <Button
            testID="change-password-confirm"
            onPress={handleSubmit}
            disabled={!canSubmit}
          >
            {isPending ? (
              <ActivityIndicator
                testID="change-password-spinner"
                size="small"
                color="white"
              />
            ) : (
              <Text>
                {t("settings.password.confirm", "Speichern")}
              </Text>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
