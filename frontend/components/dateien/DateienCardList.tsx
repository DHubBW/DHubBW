/**
 * Card-based file list used on screens narrower than the table breakpoint.
 *
 * Each card shows the file name with the matching extension icon, the
 * uploader, modified date, file size, and download count. The same three
 * action slots as the table are reserved on the right so a follow-up
 * issue can drop in download / rename / delete buttons in one place.
 */
import { ActivityIndicator, View } from "react-native";

import { Text } from "@/components/ui/text";
import type { DateiDTO } from "@/lib/api/types.gen";
import { fileIconFor } from "@/lib/files/fileIcons";
import { formatFileSize, formatIsoDate } from "@/lib/files/formatters";
import { sortDateien } from "@/lib/files/sortDateien";
import { t } from "@/lib/i18n/t";

import { EmptyFolderHint, ErrorHint } from "./EmptyStates";

const ACTION_SLOT_COUNT = 3;

export interface DateienCardListProps {
  dateien: DateiDTO[];
  isLoading: boolean;
  isError: boolean;
}

/**
 * Renders a card list of files for narrow viewports.
 *
 * @param props - {@link DateienCardListProps}
 * @returns The card list view.
 */
export function DateienCardList({
  dateien,
  isLoading,
  isError,
}: DateienCardListProps) {
  if (isLoading) {
    return (
      <View className="flex-1 items-center justify-center py-16">
        <ActivityIndicator color="hsl(var(--muted-foreground))" />
      </View>
    );
  }
  if (isError) return <ErrorHint />;
  if (dateien.length === 0) return <EmptyFolderHint />;

  // Default ordering on mobile = name asc; sorting controls live in the
  // table view to keep the card UX simple. Same comparator as the table
  // so the two never disagree on equal data.
  const sorted = sortDateien(dateien, { column: "name", direction: "asc" });

  return (
    <View className="px-4 py-3">
      {sorted.map((datei) => (
        <Card key={datei.dateiID} datei={datei} />
      ))}
    </View>
  );
}

/**
 * Single file card.
 *
 * @param props - The card's file payload.
 * @returns The card element.
 */
function Card({ datei }: { datei: DateiDTO }) {
  const FileIconCmp = fileIconFor(datei.name);
  return (
    <View className="mb-3 flex-row items-start gap-3 rounded-lg border border-border bg-card p-3">
      <FileIconCmp size={20} color="hsl(var(--muted-foreground))" />
      <View className="flex-1">
        <Text
          className="text-sm font-medium text-foreground"
          numberOfLines={1}
        >
          {datei.name}
        </Text>
        <Text className="mt-0.5 text-xs text-muted-foreground">
          {datei.uploaderName ?? "—"}
          {"  ·  "}
          {formatIsoDate(datei.zuletztBearbeitet)}
        </Text>
        <Text className="mt-0.5 text-xs text-muted-foreground">
          {formatFileSize(datei.fileSize)}
          {"  ·  "}
          {t("dateien.card.downloads", "Downloads")}: {datei.downloadAnzahl}
        </Text>
      </View>
      <View className="flex-row items-center gap-2">
        {Array.from({ length: ACTION_SLOT_COUNT }).map((_, idx) => (
          <View
            key={idx}
            className="h-7 w-7"
            accessibilityElementsHidden
            importantForAccessibility="no-hide-descendants"
          />
        ))}
      </View>
    </View>
  );
}
