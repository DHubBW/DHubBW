/**
 * Tabular file view used on the Dateien screen for wide layouts.
 *
 * Sorting is client-side (the backend has no `sort=` parameter) and is
 * exposed via clickable column headers that toggle through asc → desc →
 * asc on repeat clicks. Each row reserves three icon-sized slots on the
 * right for download / rename / delete actions; the slot row is empty
 * until a follow-up issue fills it.
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

export interface DateienTabelleProps {
  dateien: DateiDTO[];
  isLoading: boolean;
  isError: boolean;
}

const ACTION_SLOT_COUNT = 3;
const COLUMN_WIDTHS = {
  name: "flex-[3] min-w-[180px]",
  downloads: "w-24",
  uploader: "flex-1 min-w-[120px]",
  modified: "w-28",
  size: "w-24",
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
      key: "downloads",
      label: t("dateien.table.downloads", "Downloads"),
      widthClass: COLUMN_WIDTHS.downloads,
      align: "right",
    },
    {
      key: "uploader",
      label: t("dateien.table.uploader", "Uploader"),
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
    <ScrollView className="flex-1">
      <View className="flex-row border-b border-border bg-muted/30 px-4 py-2">
        {columns.map((col) => (
          <HeaderCell
            key={col.key}
            column={col}
            sort={sort}
            onPress={() => setSort((s) => toggleSort(s, col.key))}
          />
        ))}
        <View className={cn("flex-row justify-end", COLUMN_WIDTHS.actions)}>
          <Text className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            {t("dateien.table.actions", "Aktionen")}
          </Text>
        </View>
      </View>
      <View>
        {sortedDateien.map((datei) => (
          <Row key={datei.dateiID} datei={datei} />
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

/**
 * Single sortable column header.
 *
 * @param props - {@link HeaderCellProps}
 * @returns The header-cell pressable.
 */
function HeaderCell({ column, sort, onPress }: HeaderCellProps) {
  const active = sort.column === column.key;
  const Icon = !active ? ArrowUpDown : sort.direction === "asc" ? ArrowUp : ArrowDown;
  const justify = column.align === "right" ? "justify-end" : "justify-start";
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={t("dateien.table.sortBy", "Sortieren nach") + " " + column.label}
      className={cn("flex-row items-center gap-1", justify, column.widthClass)}
    >
      <Text
        className={cn(
          "text-xs font-semibold uppercase tracking-wide",
          active ? "text-foreground" : "text-muted-foreground",
        )}
      >
        {column.label}
      </Text>
      <Icon
        size={12}
        color={
          active ? "hsl(var(--foreground))" : "hsl(var(--muted-foreground))"
        }
      />
    </Pressable>
  );
}

/**
 * Single data row — file icon + columns + action slots.
 *
 * @param props - The row's file payload.
 * @returns The row element.
 */
function Row({ datei }: { datei: DateiDTO }) {
  const FileIconCmp = fileIconFor(datei.name);
  return (
    <View className="flex-row items-center border-b border-border px-4 py-2.5">
      <View
        className={cn("flex-row items-center gap-2", COLUMN_WIDTHS.name)}
      >
        <FileIconCmp size={16} color="hsl(var(--muted-foreground))" />
        <Text className="flex-1 text-sm text-foreground" numberOfLines={1}>
          {datei.name}
        </Text>
      </View>
      <View className={cn("items-end", COLUMN_WIDTHS.downloads)}>
        <Text className="text-sm tabular-nums text-foreground">
          {datei.downloadAnzahl}
        </Text>
      </View>
      <View className={COLUMN_WIDTHS.uploader}>
        <Text className="text-sm text-foreground" numberOfLines={1}>
          {datei.uploaderName ?? "—"}
        </Text>
      </View>
      <View className={COLUMN_WIDTHS.modified}>
        <Text className="text-sm tabular-nums text-foreground">
          {formatIsoDate(datei.zuletztBearbeitet)}
        </Text>
      </View>
      <View className={cn("items-end", COLUMN_WIDTHS.size)}>
        <Text className="text-sm tabular-nums text-foreground">
          {formatFileSize(datei.fileSize)}
        </Text>
      </View>
      <View
        className={cn("flex-row items-center justify-end gap-2", COLUMN_WIDTHS.actions)}
      >
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
