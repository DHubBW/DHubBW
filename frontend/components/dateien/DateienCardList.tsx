/**
 * Card-based file list used on screens narrower than the table breakpoint.
 *
 * Each file renders as a standalone {@link Card}-styled tile rather
 * than a flat row separated by hairlines: rounded background, soft
 * border, and a clear two-zone layout (top = icon + title + meta,
 * bottom = action toolbar). This trades a few extra px of vertical
 * space for a much cleaner mobile read-out — every entry feels like a
 * tap-target instead of a list-row, the action icons get bigger
 * touch zones, and size / download count sit in their own pill chips
 * so the title line stays uncluttered.
 *
 * Action callbacks (`onDownload`, `onRename`, `onDelete`) are forwarded
 * to the bottom action bar. When none are provided, the bar is omitted
 * (back-compat).
 */
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  View,
} from "react-native";

import { Text } from "@/components/ui/text";
import type { DateiDTO } from "@/lib/api/types.gen";
import { fileIconFor } from "@/lib/files/fileIcons";
import { formatFileSize, formatIsoDate } from "@/lib/files/formatters";
import { sortDateien } from "@/lib/files/sortDateien";
import { t } from "@/lib/i18n/t";

import { EmptyFolderHint, ErrorHint } from "./EmptyStates";
import { RowActions } from "./RowActions";

export interface DateienCardListProps {
  dateien: DateiDTO[];
  isLoading: boolean;
  isError: boolean;
  onDownload?: (datei: DateiDTO) => void;
  onRename?: (datei: DateiDTO) => void;
  onDelete?: (datei: DateiDTO) => void;
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
  onDownload,
  onRename,
  onDelete,
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

  const sorted = sortDateien(dateien, { column: "name", direction: "asc" });

  return (
    <ScrollView className="flex-1">
      <View className="gap-2 px-4 py-3">
        {sorted.map((datei) => (
          <FileCard
            key={datei.dateiID}
            datei={datei}
            onDownload={onDownload}
            onRename={onRename}
            onDelete={onDelete}
          />
        ))}
      </View>
    </ScrollView>
  );
}

interface FileCardProps {
  datei: DateiDTO;
  onDownload?: (datei: DateiDTO) => void;
  onRename?: (datei: DateiDTO) => void;
  onDelete?: (datei: DateiDTO) => void;
}

/**
 * Single file tile. Tapping the card body (above the action bar)
 * triggers a download when the callback is wired — matches what the
 * table-row primary action does and saves a separate icon hit.
 *
 * @param props - {@link FileCardProps}
 * @returns The card.
 */
function FileCard({ datei, onDownload, onRename, onDelete }: FileCardProps) {
  const FileIconCmp = fileIconFor(datei.name);
  const hasActions = Boolean(onDownload || onRename || onDelete);
  const downloadsLabel = t("dateien.card.downloads", "Downloads");

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={datei.name}
      onPress={() => onDownload?.(datei)}
      className="flex-row items-start gap-3 overflow-hidden rounded-xl border border-border bg-card px-4 py-3 active:bg-accent/40"
    >
      {/* Icon — larger than the table variant; rounded square keeps
          the shape consistent with the upload button on the toolbar. */}
      <View className="rounded-xl bg-muted p-3">
        <FileIconCmp size={22} color="hsl(var(--primary))" />
      </View>

      {/* Info column */}
      <View className="flex-1 gap-1">
        <Text
          className="text-base font-semibold leading-snug text-foreground"
          numberOfLines={2}
        >
          {datei.name}
        </Text>
        <Text className="text-xs text-muted-foreground" numberOfLines={1}>
          {datei.uploaderName ?? "—"}
          {"  ·  "}
          {formatIsoDate(datei.zuletztBearbeitet)}
        </Text>
        {/* Pill chips for size + downloads — quieter than three loose
            meta lines, easier to scan at a glance. */}
        <View className="mt-1 flex-row flex-wrap gap-1.5">
          <MetaChip label={formatFileSize(datei.fileSize)} />
          <MetaChip label={`${datei.downloadAnzahl} ${downloadsLabel}`} />
        </View>
      </View>

      {/* Actions live inside the card body, top-right, aligned to the
          icon column so they sit naturally next to the title. */}
      {hasActions && (
        <RowActions
          onDownload={() => onDownload?.(datei)}
          onRename={() => onRename?.(datei)}
          onDelete={() => onDelete?.(datei)}
          size="comfortable"
          className="-mr-1 self-start"
        />
      )}
    </Pressable>
  );
}

interface MetaChipProps {
  label: string;
}

/**
 * Tiny rounded pill rendered inside the card meta row. Uses the
 * `muted` token so the chip sits softly against the card background.
 */
function MetaChip({ label }: MetaChipProps) {
  return (
    <View className="rounded-full bg-muted px-2 py-0.5">
      <Text className="text-[11px] font-medium text-muted-foreground">
        {label}
      </Text>
    </View>
  );
}
