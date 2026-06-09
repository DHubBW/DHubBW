/**
 * Dateien screen — folder-tree navigation plus the matching file list.
 *
 * Layout:
 * - Wide (≥ 768 px): folder tree on the left, file table on the right.
 * - Narrow (< 768 px): hamburger drawer for the tree, card list for the
 *   files.
 *
 * Selection state holds the currently focused folder UUID, or `null`
 * for the synthetic "Alle Dateien" root view; the file query reacts to
 * that selection.
 */
import { useState } from "react";
import { ActivityIndicator, ScrollView, useWindowDimensions, View } from "react-native";

import { CourseRoute } from "@/components/auth/CourseRoute";
import { DateienCardList } from "@/components/dateien/DateienCardList";
import { DateienTabelle } from "@/components/dateien/DateienTabelle";
import { DateienToolbar } from "@/components/dateien/DateienToolbar";
import { ErrorHint, NoTopLevelHint } from "@/components/dateien/EmptyStates";
import { OrdnerTree } from "@/components/dateien/OrdnerTree";
import { OrdnerTreeDrawer } from "@/components/dateien/OrdnerTreeDrawer";
import { Text } from "@/components/ui/text";
import { useDateienListe } from "@/hooks/useDateienListe";
import { useOrdnerTree } from "@/hooks/useOrdnerTree";
import type { OrdnerDTO } from "@/lib/api/types.gen";
import { t } from "@/lib/i18n/t";

const TABLE_BREAKPOINT_PX = 768;

/**
 * Files screen. Wrapped in {@link CourseRoute} so users without a course
 * are redirected to the dashboard rather than seeing an unauthorised
 * empty list.
 *
 * @returns The Dateien screen.
 */
export default function DateienScreen() {
  return (
    <CourseRoute>
      <DateienScreenInner />
    </CourseRoute>
  );
}

/**
 * Inner screen body — extracted so the {@link CourseRoute} guard can wrap
 * it without re-rendering its hooks until the user is course-cleared.
 *
 * @returns The screen body.
 */
function DateienScreenInner() {
  const { width } = useWindowDimensions();
  const isWide = width >= TABLE_BREAKPOINT_PX;

  const [selectedFolder, setSelectedFolder] = useState<OrdnerDTO | null>(null);
  const selectedOrdnerId = selectedFolder?.ordnerID ?? null;

  const ordnerTree = useOrdnerTree();
  const dateienListe = useDateienListe(selectedOrdnerId);

  const rootFolders = ordnerTree.data ?? [];

  if (ordnerTree.isLoading) {
    return (
      <View className="flex-1 items-center justify-center bg-background">
        <ActivityIndicator color="hsl(var(--muted-foreground))" />
      </View>
    );
  }
  if (ordnerTree.isError) {
    return (
      <View className="flex-1 bg-background">
        <ErrorHint />
      </View>
    );
  }
  if (rootFolders.length === 0 && selectedOrdnerId === null) {
    // Course exists but has no top-level folders — show the hint and a
    // disabled-looking placeholder where the table would normally live.
    return (
      <View className="flex-1 bg-background">
        <Header />
        <NoTopLevelHint />
      </View>
    );
  }

  return (
    <View className="flex-1 flex-row bg-background">
      {isWide && (
        <View className="w-72 border-r border-border">
          <ScrollView className="flex-1">
            <View className="py-3">
              <Text className="px-4 pb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                {t("dateien.tree.heading", "Ordner")}
              </Text>
              <OrdnerTree
                rootFolders={rootFolders}
                selectedOrdnerId={selectedOrdnerId}
                onSelect={setSelectedFolder}
              />
            </View>
          </ScrollView>
        </View>
      )}
      <View className="flex-1">
        <Header />
        {!isWide && (
          <View className="px-4 pt-3">
            <OrdnerTreeDrawer
              rootFolders={rootFolders}
              selectedOrdnerId={selectedOrdnerId}
              onSelect={setSelectedFolder}
            />
          </View>
        )}
        <DateienToolbar selected={selectedFolder} />
        {isWide ? (
          <DateienTabelle
            dateien={dateienListe.data ?? []}
            isLoading={dateienListe.isLoading}
            isError={dateienListe.isError}
          />
        ) : (
          <DateienCardList
            dateien={dateienListe.data ?? []}
            isLoading={dateienListe.isLoading}
            isError={dateienListe.isError}
          />
        )}
      </View>
    </View>
  );
}

/**
 * Page header above the table / card list.
 *
 * @returns The header strip.
 */
function Header() {
  return (
    <View className="border-b border-border px-4 py-3">
      <Text className="text-xl font-semibold text-foreground">
        {t("dateien.title", "Dateien")}
      </Text>
    </View>
  );
}
