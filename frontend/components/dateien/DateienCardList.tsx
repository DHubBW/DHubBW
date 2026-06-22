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
import { ActivityIndicator, Pressable, ScrollView, View } from "react-native";

import { Text } from "@/components/ui/text";
import type { DateiDTO } from "@/lib/api/types.gen";
import { fileIconFor } from "@/lib/files/fileIcons";
import { formatFileSize, formatIsoDate } from "@/lib/files/formatters";
import { sortDateien } from "@/lib/files/sortDateien";
import { t } from "@/lib/i18n/t";

import { EmptyFolderHint, ErrorHint } from "./EmptyStates";
import { RowActions } from "./RowActions";
import { StarRating } from "./StarRating";
import { TagBadge } from "./TagBadge";

export interface DateienCardListProps {
  dateien: DateiDTO[];
  isLoading: boolean;
  isError: boolean;
  onDownload?: (datei: DateiDTO) => void;
  /**
   * Called when the user taps a card body. Wired to the inline preview
   * dialog. When omitted, tapping the card falls back to the download
   * action so older callers keep the previous behaviour.
   */
  onPreview?: (datei: DateiDTO) => void;
  /**
   * Called when the user taps the card's 3-dot icon. The parent
   * screen owns the resulting menu (Verschieben / Umbenennen /
   * Löschen).
   */
  onMore?: (datei: DateiDTO) => void;
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
  onPreview,
  onMore,
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
            onPreview={onPreview}
            onMore={onMore}
          />
        ))}
      </View>
    </ScrollView>
  );
}

interface FileCardProps {
  datei: DateiDTO;
  onDownload?: (datei: DateiDTO) => void;
  onPreview?: (datei: DateiDTO) => void;
  onMore?: (datei: DateiDTO) => void;
}

/**
 * Single file tile. Tapping the card body (above the action bar)
 * triggers a download when the callback is wired — matches what the
 * table-row primary action does and saves a separate icon hit.
 *
 * <p>The card body and the action icons are rendered as siblings of
 * an outer {@code View} (not nested inside the body's Pressable):
 * RN-Web translates Pressable to {@code <button>} and TouchableOpacity
 * inside RowActions to nested {@code <button>}, which is invalid
 * HTML and triggers a hydration warning.</p>
 *
 * @param props - {@link FileCardProps}
 * @returns The card.
 */
function FileCard({ datei, onDownload, onPreview, onMore }: FileCardProps) {
  const FileIconCmp = fileIconFor(datei.name);
  const downloadsLabel = t("dateien.card.downloads", "Downloads");

  // Tapping the card body opens the preview when wired; falls back to
  // download for older callers that haven't wired preview yet.
  const handleCardPress = onPreview
    ? () => onPreview(datei)
    : () => onDownload?.(datei);

  return (
    <View className="flex-row items-center gap-3 overflow-hidden rounded-xl border border-border bg-card px-4 py-3 web:cursor-pointer web:hover:bg-accent/20">
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={datei.name}
        onPress={handleCardPress}
        className="flex-1 flex-row items-center gap-3 active:bg-accent/40"
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
          {/* Size chip plus a combined Bewertung-then-Downloads
              row that mirrors the table layout (Bewertung sits to
              the left of Downloads). */}
          <View className="mt-1 flex-row flex-wrap items-center gap-1.5">
            <MetaChip label={formatFileSize(datei.fileSize)} />
          </View>
          {(datei.tags ?? []).length > 0 && (
            <View className="mt-1.5 flex-row flex-wrap gap-1">
              {datei.tags.map((tag) => (
                <TagBadge
                  key={tag.tagID}
                  name={tag.name}
                  testID={`card-tag-${tag.tagID}`}
                />
              ))}
            </View>
          )}
          <View className="mt-1.5 flex-row items-center gap-2">
            <StarRating
              value={datei.bewertungDurchschnitt}
              count={datei.bewertungAnzahl}
              size={14}
            />
            <MetaChip label={`${datei.downloadAnzahl} ${downloadsLabel}`} />
          </View>
        </View>
      </Pressable>

      {/* Actions — Vorschau + Download (always visible) plus the
          3-dot menu hosting Verschieben / Umbenennen / Löschen.
          Lives alongside the Pressable (not nested inside it) so
          the rendered DOM stays free of nested <button> elements.
          `self-center` zentriert die Icons vertikal mittig zur
          Kartenhöhe — bei zwei- bis drei-zeiligen Karten sieht das
          ruhiger aus als ein top-aligned Cluster. */}
      <RowActions
        onPreview={onPreview ? () => onPreview(datei) : undefined}
        onDownload={onDownload ? () => onDownload(datei) : undefined}
        onMore={onMore ? () => onMore(datei) : undefined}
        size="comfortable"
        className="-mr-1 self-center"
      />
    </View>
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
