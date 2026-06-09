/**
 * Card-based file list used on screens narrower than the table breakpoint.
 */
import { Download } from "lucide-react-native";
import {
  ActivityIndicator,
  Platform,
  ScrollView,
  TouchableOpacity,
  View,
} from "react-native";
import { Text } from "@/components/ui/text";
import { Separator } from "@/components/ui/separator";
import type { DateiDTO } from "@/lib/api/types.gen";
import { fileIconFor } from "@/lib/files/fileIcons";
import { formatFileSize, formatIsoDate } from "@/lib/files/formatters";
import { sortDateien } from "@/lib/files/sortDateien";
import { t } from "@/lib/i18n/t";

import { EmptyFolderHint, ErrorHint } from "./EmptyStates";

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

  const sorted = sortDateien(dateien, { column: "name", direction: "asc" });

  return (
    <ScrollView className="flex-1">
      <View className="px-4 py-2">
        {sorted.map((datei, idx) => (
          <View key={datei.dateiID}>
            {idx > 0 && <Separator className="opacity-40" />}
            <FileRow datei={datei} />
          </View>
        ))}
      </View>
    </ScrollView>
  );
}

function FileRow({ datei }: { datei: DateiDTO }) {
  const FileIconCmp = fileIconFor(datei.name);
  const downloadUrl = `${process.env.EXPO_PUBLIC_API_BASE_URL ?? "http://localhost:8080"}/student/dateien/${datei.dateiID}/download`;

  function handleDownload() {
    if (Platform.OS === "web" && typeof window !== "undefined") {
      const a = document.createElement("a");
      a.href = downloadUrl;
      a.download = datei.name;
      a.click();
    }
  }

  return (
    <View className="flex-row items-center gap-3 py-3">
      {/* Icon */}
      <View className="rounded-lg bg-muted p-2.5">
        <FileIconCmp size={18} color="hsl(var(--primary))" />
      </View>

      {/* Info */}
      <View className="flex-1">
        <Text
          className="text-sm font-semibold text-foreground"
          numberOfLines={1}
        >
          {datei.name}
        </Text>
        <Text className="mt-0.5 text-xs text-muted-foreground" numberOfLines={1}>
          {datei.uploaderName ?? "—"}
          {"  ·  "}
          {formatIsoDate(datei.zuletztBearbeitet)}
        </Text>
        <View className="mt-0.5 flex-row items-center gap-2">
          <Text className="text-xs text-muted-foreground">
            {formatFileSize(datei.fileSize)}
          </Text>
          <Text className="text-xs text-muted-foreground">·</Text>
          <Text className="text-xs text-muted-foreground">
            {datei.downloadAnzahl} {t("dateien.card.downloads", "Downloads")}
          </Text>
        </View>
      </View>

      {/* Download */}
      <TouchableOpacity
        onPress={handleDownload}
        accessibilityRole="button"
        accessibilityLabel={t("dateien.download", "Herunterladen")}
        className="rounded-lg border border-border p-2 active:bg-accent"
      >
        <Download size={16} color="hsl(var(--primary))" />
      </TouchableOpacity>
    </View>
  );
}
