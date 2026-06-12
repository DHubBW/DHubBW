/**
 * Tabular file view used on the Dateien screen for wide layouts.
 *
 * Sorting is client-side and exposed via clickable column headers.
 * Each row exposes a slot of action callbacks — when none are provided
 * the actions column is empty (back-compat for callers that have not yet
 * wired CRUD).
 */
import { ArrowDown, ArrowUp, ArrowUpDown } from "lucide-react-native";
import { useMemo, useState } from "react";
import { ActivityIndicator, Pressable, ScrollView, View } from "react-native";

import { Text } from "@/components/ui/text";
import type { DateiDTO } from "@/lib/api/types.gen";
import { fileIconFor } from "@/lib/files/fileIcons";
import { formatFileSize, formatIsoDate } from "@/lib/files/formatters";
import {
  sortDateien,
  toggleSort,
  type SortColumn,
  type SortState,
} from "@/lib/files/sortDateien";
import { t } from "@/lib/i18n/t";
import { cn } from "@/lib/utils";

import { EmptyFolderHint, ErrorHint } from "./EmptyStates";
import { RowActions } from "./RowActions";

export interface DateienTabelleProps {
  dateien: DateiDTO[];
  isLoading: boolean;
  isError: boolean;
  /** Per-row callbacks. When omitted, the actions column stays empty. */
  onDownload?: (datei: DateiDTO) => void;
  /**
   * Called when the user clicks a file's name. Wired to the inline
   * preview dialog. When omitted the name cell stays static text.
   */
  onPreview?: (datei: DateiDTO) => void;
  /**
   * Called when the user clicks the row's 3-dot icon. The parent
   * screen owns the resulting menu (Verschieben / Umbenennen / Löschen).
   */
  onMore?: (datei: DateiDTO) => void;
}

const COLUMN_WIDTHS = {
  name: "flex-[3] min-w-[200px]",
  uploader: "flex-[2] min-w-[120px]",
  modified: "w-28",
  size: "w-24",
  downloads: "w-28",
  // Hosts Vorschau · Download · 3-Punkte-Menü = 3 ActionButtons
  // (32 px each) plus their gap.
  actions: "w-28",
} as const;

interface ColumnDef {
  key: SortColumn;
  label: string;
  widthClass: string;
  align?: "left" | "right";
}

/**
 * Renders the sortable file table.
 *
 * @param props - {@link DateienTabelleProps}
 * @returns The table view.
 */
export function DateienTabelle({
  dateien,
  isLoading,
  isError,
  onDownload,
  onPreview,
  onMore,
}: DateienTabelleProps) {
  const [sort, setSort] = useState<SortState>({
    column: "name",
    direction: "asc",
  });
  const sortedDateien = useMemo(
    () => sortDateien(dateien, sort),
    [dateien, sort],
  );

  const columns: ColumnDef[] = [
    {
      key: "name",
      label: t("dateien.table.name", "Name"),
      widthClass: COLUMN_WIDTHS.name,
    },
    {
      key: "uploader",
      label: t("dateien.table.uploader", "Hochgeladen von"),
      widthClass: COLUMN_WIDTHS.uploader,
    },
    {
      key: "modified",
      label: t("dateien.table.modified", "Geändert"),
      widthClass: COLUMN_WIDTHS.modified,
    },
    {
      key: "size",
      label: t("dateien.table.size", "Größe"),
      widthClass: COLUMN_WIDTHS.size,
      align: "right",
    },
    {
      key: "downloads",
      label: t("dateien.table.downloads", "Downloads"),
      widthClass: COLUMN_WIDTHS.downloads,
      align: "right",
    },
  ];

  if (isLoading) {
    return (
      <View className="flex-1 items-center justify-center py-16">
        <ActivityIndicator color="hsl(var(--muted-foreground))" />
      </View>
    );
  }
  if (isError) return <ErrorHint />;
  if (dateien.length === 0) return <EmptyFolderHint />;

  return (
    <ScrollView className="flex-1" contentContainerStyle={{ flexGrow: 1 }}>
      {/* Header */}
      <View className="flex-row items-center border-b border-border bg-muted/40 px-5 py-2">
        {columns.map((col) => (
          <HeaderCell
            key={col.key}
            column={col}
            sort={sort}
            onPress={() => setSort((s) => toggleSort(s, col.key))}
          />
        ))}
        <View className={COLUMN_WIDTHS.actions} />
      </View>

      {/* Rows */}
      <View>
        {sortedDateien.map((datei, idx) => (
          <Row
            key={datei.dateiID}
            datei={datei}
            striped={idx % 2 === 1}
            onDownload={onDownload}
            onPreview={onPreview}
            onMore={onMore}
          />
        ))}
      </View>
    </ScrollView>
  );
}

interface HeaderCellProps {
  column: ColumnDef;
  sort: SortState;
  onPress: () => void;
}

function HeaderCell({ column, sort, onPress }: HeaderCellProps) {
  const active = sort.column === column.key;
  const Icon = !active
    ? ArrowUpDown
    : sort.direction === "asc"
      ? ArrowUp
      : ArrowDown;
  const justify = column.align === "right" ? "justify-end" : "justify-start";
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={t("dateien.table.sortBy", "Sortieren nach") + " " + column.label}
      className={cn(
        "flex-row items-center gap-1 py-0.5",
        justify,
        column.widthClass,
      )}
    >
      <Text
        className={cn(
          "text-[11px] font-semibold uppercase tracking-wide",
          active ? "text-foreground" : "text-muted-foreground",
        )}
      >
        {column.label}
      </Text>
      <Icon
        size={11}
        color={
          active
            ? "hsl(var(--foreground))"
            : "hsl(var(--muted-foreground))"
        }
      />
    </Pressable>
  );
}

interface RowProps {
  datei: DateiDTO;
  striped: boolean;
  onDownload?: (datei: DateiDTO) => void;
  onPreview?: (datei: DateiDTO) => void;
  onMore?: (datei: DateiDTO) => void;
}

function Row({
  datei,
  striped,
  onDownload,
  onPreview,
  onMore,
}: RowProps) {
  const FileIconCmp = fileIconFor(datei.name);
  const baseClass = cn(
    "flex-row items-center border-b border-border px-5 py-2.5",
    striped ? "bg-muted/20" : "bg-background",
  );

  // The data cells (name → downloads). When `onPreview` is wired
  // these sit inside a single Pressable so a tap anywhere across
  // them opens the preview. The Actions cell stays OUTSIDE the
  // Pressable — nesting a TouchableOpacity (which RN-Web renders as
  // a `<button>`) inside a Pressable (also `<button>`) is invalid
  // HTML and triggers a React hydration warning.
  const dataCells = (
    <>
      {/* Name */}
      <View className={cn("flex-row items-center gap-2.5", COLUMN_WIDTHS.name)}>
        <View className="rounded-md bg-muted p-1.5">
          <FileIconCmp size={14} color="hsl(var(--primary))" />
        </View>
        <Text
          className="flex-1 text-sm font-medium text-foreground"
          numberOfLines={1}
        >
          {datei.name}
        </Text>
      </View>

      {/* Uploader */}
      <View className={COLUMN_WIDTHS.uploader}>
        <Text
          className="text-sm text-muted-foreground"
          numberOfLines={1}
        >
          {datei.uploaderName ?? "—"}
        </Text>
      </View>

      {/* Modified */}
      <View className={COLUMN_WIDTHS.modified}>
        <Text className="text-sm tabular-nums text-muted-foreground">
          {formatIsoDate(datei.zuletztBearbeitet)}
        </Text>
      </View>

      {/* Size */}
      <View className={cn("items-end", COLUMN_WIDTHS.size)}>
        <Text className="text-sm tabular-nums text-muted-foreground">
          {formatFileSize(datei.fileSize)}
        </Text>
      </View>

      {/* Downloads */}
      <View className={cn("items-end", COLUMN_WIDTHS.downloads)}>
        <Text className="text-sm tabular-nums text-muted-foreground">
          {datei.downloadAnzahl}
        </Text>
      </View>
    </>
  );

  const actionsCell = (
    <View className={COLUMN_WIDTHS.actions}>
      <RowActions
        onPreview={onPreview ? () => onPreview(datei) : undefined}
        onDownload={onDownload ? () => onDownload(datei) : undefined}
        onMore={onMore ? () => onMore(datei) : undefined}
      />
    </View>
  );

  if (onPreview) {
    return (
      <View
        className={cn(
          baseClass,
          "web:cursor-pointer web:hover:bg-accent/30",
        )}
      >
        <Pressable
          accessibilityRole="button"
          accessibilityLabel={t("dateien.preview.open", "Vorschau öffnen")}
          onPress={() => onPreview(datei)}
          testID="row-preview"
          // Stretch the data-cell zone across all available width
          // so the entire left side of the row is clickable.
          className="flex-1 flex-row items-center active:bg-accent/40"
        >
          {dataCells}
        </Pressable>
        {actionsCell}
      </View>
    );
  }

  return (
    <View className={baseClass}>
      {dataCells}
      {actionsCell}
    </View>
  );
}
