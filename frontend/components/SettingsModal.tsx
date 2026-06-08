import { LogOut } from "lucide-react-native";
import { ActivityIndicator, Pressable, View } from "react-native";
import { toast } from "sonner-native";

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Icon } from "@/components/ui/icon";
import { Separator } from "@/components/ui/separator";
import { Text } from "@/components/ui/text";
import { useAuth } from "@/lib/auth/AuthContext";
import { t } from "@/lib/i18n/t";
import { useTheme, type ThemePreference } from "@/lib/theme/ThemeProvider";
import { cn } from "@/lib/utils";
import * as React from "react";

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
 * Shows the logged-in user's name and role, a three-segment theme toggle and a
 * flat logout row. On logout the modal closes immediately and the auth context
 * redirects to login — {@link ProtectedRoute} unmounts the app subtree.
 *
 * @param props - {@link SettingsModalProps}
 * @returns A controlled `Dialog` element.
 */
export function SettingsModal({
  open,
  onOpenChange,
}: SettingsModalProps): React.JSX.Element {
  const { user, logout } = useAuth();
  const { theme, setTheme } = useTheme();
  const [isLoggingOut, setIsLoggingOut] = React.useState(false);

  React.useEffect(() => {
    if (!open) setIsLoggingOut(false);
  }, [open]);

  async function handleLogout() {
    setIsLoggingOut(true);
    try {
      // logout() writes a sessionStorage flag then does window.location.replace
      // on Web — the success toast is shown by the login screen after reload.
      await logout();
    } catch {
      toast.error(t("settings.logout.error", "Abmelden fehlgeschlagen"));
      setIsLoggingOut(false);
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
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (isLoggingOut) return;
        onOpenChange(next);
      }}
    >
      {/*
       * w-80 + sm:w-96 gives a fixed, readable width on all viewports.
       * The DialogContent base classes handle centering + overlay.
       */}
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
                color="#888"
              />
            ) : (
              <Icon as={LogOut} size={17} className="text-foreground" />
            )}
            <Text className="text-sm font-medium text-foreground">
              {t("settings.logout.button", "Abmelden")}
            </Text>
          </Pressable>
        </View>
      </DialogContent>
    </Dialog>
  );
}
