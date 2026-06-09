/**
 * Empty-state hints for the Dateien screen — leere Ordner, kein Top-Level
 * und Generic-Error. Kept as small named exports so the screen can drop the
 * matching one in for whichever branch is currently visible.
 */
import { FileWarning, FolderPlus, Inbox } from "lucide-react-native";
import { View } from "react-native";

import { Text } from "@/components/ui/text";
import { t } from "@/lib/i18n/t";

interface EmptyShellProps {
  icon: React.ReactNode;
  title: string;
  body: string;
}

/**
 * Centred icon + headline + body block used by every empty hint.
 *
 * @param props - {@link EmptyShellProps}
 * @returns The empty-state shell.
 */
function EmptyShell({ icon, title, body }: EmptyShellProps): React.ReactElement {
  return (
    <View className="flex-1 items-center justify-center px-6 py-12">
      <View className="mb-3">{icon}</View>
      <Text className="mb-1 text-center text-base font-semibold text-foreground">
        {title}
      </Text>
      <Text className="text-center text-sm text-muted-foreground">{body}</Text>
    </View>
  );
}

/**
 * Hint shown when a folder is selected but contains no files.
 *
 * @returns The empty-folder hint.
 */
export function EmptyFolderHint() {
  return (
    <EmptyShell
      icon={<Inbox size={32} color="hsl(var(--muted-foreground))" />}
      title={t("dateien.empty.folder.title", "Noch keine Dateien hier")}
      body={t(
        "dateien.empty.folder.body",
        "Lade etwas hoch, um den Ordner zu füllen.",
      )}
    />
  );
}

/**
 * Hint shown when the course has no top-level folders at all.
 *
 * @returns The no-top-level hint.
 */
export function NoTopLevelHint() {
  return (
    <EmptyShell
      icon={<FolderPlus size={32} color="hsl(var(--muted-foreground))" />}
      title={t("dateien.empty.noTopLevel.title", "Noch keine Ordner")}
      body={t(
        "dateien.empty.noTopLevel.body",
        "Erstelle deinen ersten Ordner, um Dateien zu organisieren.",
      )}
    />
  );
}

/**
 * Hint shown when fetching the file list failed.
 *
 * @returns The error hint.
 */
export function ErrorHint() {
  return (
    <EmptyShell
      icon={<FileWarning size={32} color="hsl(var(--destructive))" />}
      title={t("dateien.empty.error.title", "Dateien konnten nicht geladen werden")}
      body={t(
        "dateien.empty.error.body",
        "Bitte später erneut versuchen oder die Seite neu laden.",
      )}
    />
  );
}
