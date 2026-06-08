import { LogOut, Trash2, UserMinus } from "lucide-react-native";
import * as React from "react";
import { ActivityIndicator, Platform, Pressable, View } from "react-native";
import { toast } from "sonner-native";
import { useRouter } from "expo-router";

import { ConfirmDialog } from "@/components/ConfirmDialog";
import { ConfirmDialogStrict } from "@/components/ConfirmDialogStrict";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Icon } from "@/components/ui/icon";
import { Separator } from "@/components/ui/separator";
import { Text } from "@/components/ui/text";
import { useDeleteAccount } from "@/hooks/useDeleteAccount";
import { useLeaveKurs } from "@/hooks/useLeaveKurs";
import { ApiError } from "@/lib/api/errors";
import { useAuth } from "@/lib/auth/AuthContext";
import { t } from "@/lib/i18n/t";
import { useTheme, type ThemePreference } from "@/lib/theme/ThemeProvider";
import { cn } from "@/lib/utils";

/**
 * Extracts a human-readable string from an ApiError body for error-case
 * detection. Handles both plain-text bodies (GlobalExceptionHandler returns
 * ResponseEntity<String>) and JSON objects with a `response` or `message`
 * field.
 */
function getBodyText(body: unknown): string {
  if (typeof body === "string") return body;
  if (body && typeof body === "object") {
    const r = (body as Record<string, unknown>).response;
    if (typeof r === "string") return r;
    const m = (body as Record<string, unknown>).message;
    if (typeof m === "string") return m;
  }
  return "";
}

const THEME_OPTIONS: { value: ThemePreference; label: string }[] = [
  { value: "system", label: t("settings.theme.system", "System") },
  { value: "dark", label: t("settings.theme.dark", "Dunkel") },
  { value: "light", label: t("settings.theme.light", "Hell") },
];

/** Props for the {@link SettingsModal} component. */
export type SettingsModalProps = {
  /** Controls dialog visibility. */
  open: boolean;
  /** Called when the dialog should open or close. */
  onOpenChange: (open: boolean) => void;
};

/**
 * Settings modal dialog reachable via the settings icon in Sidebar / Header.
 *
 * Shows the logged-in user's name and role, a three-segment theme toggle, a
 * flat logout row, a "Kurs verlassen" action (only when the user has a course)
 * and a destructive "Account löschen" action.
 *
 * "Kurs verlassen" is guarded by a {@link ConfirmDialog} (variant destructive).
 * "Account löschen" is guarded by a {@link ConfirmDialogStrict} that requires
 * the user to type "LÖSCHEN" before the confirm button becomes active.
 *
 * @param props - {@link SettingsModalProps}
 * @returns A controlled `Dialog` element.
 */
export function SettingsModal({
  open,
  onOpenChange,
}: SettingsModalProps): React.JSX.Element {
  const { user, logout } = useAuth();
  const { theme, setTheme, effectiveTheme } = useTheme();
  const router = useRouter();
  const { leaveKurs, isPending: isLeavingKurs } = useLeaveKurs();
  const { deleteAccount, isPending: isDeletingAccount } = useDeleteAccount();

  const [isLoggingOut, setIsLoggingOut] = React.useState(false);
  const [leaveKursDialogOpen, setLeaveKursDialogOpen] = React.useState(false);
  const [deleteAccountDialogOpen, setDeleteAccountDialogOpen] =
    React.useState(false);

  // Spinner color cannot use NativeWind className — derive from active theme.
  const spinnerColor = effectiveTheme === "dark" ? "#9ca3af" : "#888";

  React.useEffect(() => {
    if (!open) setIsLoggingOut(false);
  }, [open]);

  async function handleLogout() {
    setIsLoggingOut(true);
    try {
      await logout();
      // logout() does window.location.replace on Web — no extra refresh needed.
    } catch {
      toast.error(t("settings.logout.error", "Abmelden fehlgeschlagen"));
      setIsLoggingOut(false);
    }
  }

  async function handleLeaveKurs() {
    try {
      await leaveKurs();
      setLeaveKursDialogOpen(false);
      onOpenChange(false);
      toast.success(t("settings.leaveKurs.success", "Kurs verlassen"));
      // Navigate to dashboard. leaveKurs() already invalidated the profile
      // query so nav items update on the next render.
      if (Platform.OS === "web" && typeof window !== "undefined") {
        window.location.replace("/dashboard");
      } else {
        router.replace("/(app)/dashboard");
      }
    } catch (err) {
      // Always close dialogs first so the error toast renders over the app.
      setLeaveKursDialogOpen(false);
      onOpenChange(false);
      if (err instanceof ApiError) {
        // Backend StudentDashboardController catches IllegalStateException
        // locally and returns HTTP 400. fetchClient maps unmapped status codes
        // (400 is not in ApiErrorStatus) to ApiError(0). The KURSADMIN case
        // is identified via the body text.
        const bodyText = getBodyText(err.body);
        if (err.status === 0 && bodyText.includes("KURSADMIN")) {
          toast.error(
            t(
              "settings.leaveKurs.errorAdmin",
              "Tritt zuerst als Kursadmin zurück",
            ),
          );
          return;
        }
      }
      toast.error(
        t("settings.leaveKurs.error", "Kurs verlassen fehlgeschlagen"),
      );
    }
  }

  async function handleDeleteAccount() {
    try {
      await deleteAccount();
      setDeleteAccountDialogOpen(false);
      onOpenChange(false);
      toast.success(t("settings.deleteAccount.success", "Konto gelöscht"));
      // logout() clears secureStorage (Native), invalidates the queryClient
      // and navigates to /login. The backend /logout POST will 401 because
      // the account is already deleted — logout() catches that gracefully.
      await logout();
    } catch (err) {
      // Always close dialogs first so the error toast renders over the app.
      setDeleteAccountDialogOpen(false);
      onOpenChange(false);
      if (err instanceof ApiError) {
        // GlobalExceptionHandler maps IllegalStateException → HTTP 409 Conflict
        // with a plain-text body. fetchClient preserves the raw text in err.body.
        if (err.status === 409) {
          const bodyText = getBodyText(err.body);
          if (bodyText.includes("KURSADMIN")) {
            toast.error(
              t(
                "settings.deleteAccount.errorLastKursadmin",
                "Tritt zuerst als Kursadmin zurück, bevor du den Account löschst.",
              ),
            );
          } else {
            toast.error(
              t(
                "settings.deleteAccount.errorLastAdmin",
                "Du bist der letzte Systemadmin. Ernenne zuerst einen anderen.",
              ),
            );
          }
          return;
        }
      }
      toast.error(
        t("settings.deleteAccount.error", "Konto löschen fehlgeschlagen"),
      );
    }
  }

  const roleLabel = React.useMemo(() => {
    switch (user?.studentType) {
      case "KURSADMIN":
        return t("settings.role.kursadmin", "Kursadmin");
      case "SYSTEMADMIN":
        return t("settings.role.systemadmin", "Systemadmin");
      default:
        return t("settings.role.student", "Student");
    }
  }, [user?.studentType]);

  return (
    <>
      <Dialog
        open={open}
        onOpenChange={(next) => {
          if (isLoggingOut) return;
          onOpenChange(next);
        }}
      >
        <DialogContent className="w-80 gap-0 p-0 sm:w-96">
          {/* Title */}
          <DialogHeader className="border-b border-border px-5 py-4">
            <DialogTitle className="text-base">
              {t("settings.title", "Einstellungen")}
            </DialogTitle>
          </DialogHeader>

          {/* User info */}
          {user && (
            <View testID="settings-user-info" className="px-5 py-4">
              <Text className="text-sm font-semibold text-foreground">
                {user.name}
              </Text>
              <Text className="mt-0.5 text-xs text-muted-foreground">
                {roleLabel}
              </Text>
            </View>
          )}

          <Separator />

          {/* Theme segmented control */}
          <View className="px-5 py-4">
            <Text className="mb-2.5 text-xs font-medium uppercase tracking-widest text-muted-foreground">
              {t("settings.theme.label", "Design")}
            </Text>
            <View
              testID="settings-theme-toggle"
              className="flex-row rounded-lg border border-border bg-muted p-1"
            >
              {THEME_OPTIONS.map((option) => {
                const isActive = theme === option.value;
                return (
                  <Pressable
                    key={option.value}
                    testID={`settings-theme-${option.value}`}
                    onPress={() => setTheme(option.value)}
                    accessibilityRole="button"
                    accessibilityState={{ selected: isActive }}
                    className={cn(
                      "flex-1 items-center rounded-md py-1.5",
                      isActive && "bg-background shadow-sm shadow-black/10",
                    )}
                  >
                    <Text
                      className={cn(
                        "text-xs font-medium",
                        isActive ? "text-foreground" : "text-muted-foreground",
                      )}
                    >
                      {option.label}
                    </Text>
                  </Pressable>
                );
              })}
            </View>
          </View>

          <Separator />

          {/* Logout row */}
          <View className="px-2 py-2">
            <Pressable
              testID="settings-logout-button"
              onPress={handleLogout}
              disabled={isLoggingOut}
              className="flex-row items-center gap-3 rounded-lg px-3 py-3 active:bg-accent disabled:opacity-50"
            >
              {isLoggingOut ? (
                <ActivityIndicator
                  testID="settings-logout-spinner"
                  size="small"
                  color={spinnerColor}
                />
              ) : (
                <Icon as={LogOut} size={17} className="text-foreground" />
              )}
              <Text className="text-sm font-medium text-foreground">
                {t("settings.logout.button", "Abmelden")}
              </Text>
            </Pressable>
          </View>

          {/* Kurs verlassen — only visible when user has a course */}
          {user?.kursName != null && (
            <>
              <Separator />
              <View className="px-2 py-2">
                <Pressable
                  testID="settings-leave-kurs-button"
                  onPress={() => setLeaveKursDialogOpen(true)}
                  disabled={isLeavingKurs}
                  className="flex-row items-center gap-3 rounded-lg px-3 py-3 active:bg-accent disabled:opacity-50"
                >
                  <Icon as={UserMinus} size={17} className="text-destructive" />
                  <Text className="text-sm font-medium text-destructive">
                    {t("settings.leaveKurs.button", "Kurs verlassen")}
                  </Text>
                </Pressable>
              </View>
            </>
          )}

          <Separator />

          {/* Account löschen */}
          <View className="px-2 py-2">
            <Pressable
              testID="settings-delete-account-button"
              onPress={() => setDeleteAccountDialogOpen(true)}
              disabled={isDeletingAccount}
              className="flex-row items-center gap-3 rounded-lg px-3 py-3 active:bg-accent disabled:opacity-50"
            >
              <Icon as={Trash2} size={17} className="text-destructive" />
              <Text className="text-sm font-medium text-destructive">
                {t("settings.deleteAccount.button", "Account löschen")}
              </Text>
            </Pressable>
          </View>
        </DialogContent>
      </Dialog>

      {/* Kurs verlassen – ConfirmDialog (destructive) */}
      <ConfirmDialog
        open={leaveKursDialogOpen}
        onOpenChange={setLeaveKursDialogOpen}
        title={t("settings.leaveKurs.title", "Kurs verlassen?")}
        message={t(
          "settings.leaveKurs.message",
          "Du verlierst Zugriff auf alle Kursdateien. Sicher?",
        )}
        confirmLabel={t("settings.leaveKurs.confirmLabel", "Kurs verlassen")}
        variant="destructive"
        onConfirm={handleLeaveKurs}
      />

      {/* Account löschen – ConfirmDialogStrict (requires typing LÖSCHEN) */}
      <ConfirmDialogStrict
        open={deleteAccountDialogOpen}
        onOpenChange={setDeleteAccountDialogOpen}
        title={t("settings.deleteAccount.title", "Account löschen?")}
        message={t(
          "settings.deleteAccount.message",
          "Alle Daten gehen unwiderruflich verloren. Diese Aktion ist irreversibel (DSGVO).",
        )}
        confirmLabel={t(
          "settings.deleteAccount.confirmLabel",
          "Account endgültig löschen",
        )}
        verificationText="LÖSCHEN"
        variant="destructive"
        onConfirm={handleDeleteAccount}
      />
    </>
  );
}
