import { useQuery } from "@tanstack/react-query";
import { useRouter } from "expo-router";
import { Plus, Search } from "lucide-react-native";
import { useRef, useState } from "react";
import {
  ActivityIndicator,
  FlatList,
  Pressable,
  Text,
  View,
} from "react-native";
import { toast } from "sonner-native";

import { KursListItem } from "@/components/auth/KursListItem";
import { NewKursDialog } from "@/components/auth/NewKursDialog";
import { Button } from "@/components/ui/button";
import { Icon } from "@/components/ui/icon";
import { Input } from "@/components/ui/input";
import { ApiError } from "@/lib/api/errors";
import { fetchClient } from "@/lib/api/fetchClient";
import type { KursYearDTO } from "@/lib/api/types.gen";
import { useCookieConsent } from "@/lib/consent/useCookieConsent";
import { fuzzyKursSearch } from "@/lib/kurse/fuzzyKursSearch";

const DASHBOARD_PATH = "/(app)/dashboard" as const;

/**
 * Props for {@link RegisterStep2}.
 */
export interface RegisterStep2Props {
  /**
   * Invoked when the user wants to revisit Step 1 of the registration.
   * The parent screen typically flips its `step` state back to 1.
   */
  onBack: () => void;
  /**
   * When `true`, the "Später wählen" skip button is hidden.
   * Used when the component is embedded in the dashboard where a separate
   * "Zurück" button already serves as the exit action.
   */
  hideSkip?: boolean;
}

/**
 * Course-selection step shown after a successful account creation.
 *
 * Behaviour:
 * - Loads the catalogue from `GET /student/kurse` via TanStack Query and
 *   filters the result client-side via {@link fuzzyKursSearch}.
 * - „Beitrittsanfrage senden" posts to
 *   `POST /student/kurse/beitreten/{kursId}`. On HTTP 2xx a success toast
 *   fires and the user is redirected to the dashboard via `router.replace`.
 *   409 surfaces a dedicated toast and keeps the user on this screen so a
 *   different course can be picked. 422 forwards the backend message
 *   verbatim. A 404 (catalogue cached, course removed in the meantime)
 *   triggers an automatic catalogue refetch alongside an explanatory toast
 *   so the user picks a still-available course.
 * - „Neuen Kurs anlegen" opens {@link NewKursDialog}; on success the dialog
 *   triggers `router.replace` to the dashboard.
 * - „Überspringen" is always available — including while the catalogue is
 *   still loading or has errored — so the user can always reach the
 *   dashboard. „Zurück" calls `onBack()`.
 *
 * Submit is gated on `consent !== 'rejected'`: if the user rejected the
 * cookie banner the session cookie that authenticates the join request is
 * not in flight, so a network round-trip would only produce a confusing
 * 401. The page-level banner stays visible to explain the block.
 *
 * Loading and error states for the catalogue are rendered inline above the
 * list; they do not block the Skip / Back buttons.
 *
 * @param props - {@link RegisterStep2Props}
 * @returns The course-selection step.
 */
export function RegisterStep2({
  onBack,
  hideSkip = false,
}: RegisterStep2Props) {
  const router = useRouter();
  const { consent } = useCookieConsent();
  const [query, setQuery] = useState("");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [showNewKursDialog, setShowNewKursDialog] = useState(false);
  // Re-entry lock against rapid double-taps — see LoginScreen.
  const submittingRef = useRef(false);

  const kurseQuery = useQuery<KursYearDTO[], ApiError>({
    queryKey: ["kurse", "list"],
    queryFn: async ({ signal }) => {
      const res = await fetchClient("/student/kurse", { signal });
      return (await res.json()) as KursYearDTO[];
    },
  });

  const allKurse = kurseQuery.data ?? [];
  const filteredKurse = fuzzyKursSearch(allKurse, query);

  function goToDashboard() {
    if (
      typeof window !== "undefined" &&
      typeof window.location?.replace === "function"
    ) {
      window.location.replace("/dashboard");
    } else {
      router.replace(DASHBOARD_PATH);
    }
  }

  async function handleJoin() {
    if (selectedId === null || submitting) return;
    if (consent === "rejected") return;
    if (submittingRef.current) return;
    submittingRef.current = true;
    setSubmitting(true);
    try {
      await fetchClient(`/student/kurse/beitreten/${selectedId}`, {
        method: "POST",
      });
      toast.success("Beitrittsanfrage gesendet");
      goToDashboard();
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 409) {
          toast.error("Bereits in Kurs oder offene Anfrage");
        } else if (err.status === 422) {
          // Backend liefert die Begründung im message-Feld.
          toast.error(err.message);
        } else if (err.status === 0 && err.message === "HTTP 404") {
          // 404 lands in the generic non-2xx branch of fetchClient
          // (`ApiErrorStatus` whitelist does not list 404); detect it via
          // the documented fallback message. The catalogue cache is now
          // stale — refetch alongside the toast so the next pick reflects
          // reality.
          toast.error(
            "Dieser Kurs ist nicht mehr verfügbar — Liste wird neu geladen.",
          );
          setSelectedId(null);
          void kurseQuery.refetch();
        } else if (err.status === 0) {
          toast.error("Netzwerkfehler. Bitte erneut versuchen.");
        } else {
          toast.error("Beitrittsanfrage fehlgeschlagen.");
        }
      } else {
        toast.error("Beitrittsanfrage fehlgeschlagen.");
      }
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  }

  const canJoin = selectedId !== null && !submitting && consent !== "rejected";

  return (
    <View>
      <Text className="mb-4 text-sm text-muted-foreground">
        Wähle einen Kurs aus oder lege einen neuen Kurs an. Du kannst diesen
        Schritt überspringen und den Kurs später im Profil setzen.
      </Text>

      <View className="mb-3 flex-row items-center gap-2">
        <View className="flex-1 flex-row items-center rounded-md border border-input bg-background px-3">
          <Icon as={Search} className="size-4 text-muted-foreground" />
          <Input
            accessibilityLabel="Kurs suchen"
            value={query}
            onChangeText={setQuery}
            autoCapitalize="none"
            autoCorrect={false}
            placeholder="Kurs suchen (Name oder Jahrgang)"
            className="ml-2 flex-1 border-0 px-0 shadow-none"
            editable={!submitting}
          />
        </View>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Neuen Kurs anlegen"
          onPress={() => setShowNewKursDialog(true)}
          disabled={submitting}
          className="h-10 w-10 items-center justify-center rounded-md border border-border sm:h-9 sm:w-9"
        >
          <Icon as={Plus} className="size-4 text-muted-foreground" />
        </Pressable>
      </View>

      {kurseQuery.isPending ? (
        <View
          accessibilityLabel="Kurse werden geladen"
          className="my-4 items-center"
        >
          <ActivityIndicator />
        </View>
      ) : kurseQuery.isError ? (
        <View className="mb-3 rounded-md border border-destructive/30 bg-destructive/10 p-3">
          <Text className="text-xs text-destructive">
            Kursliste konnte nicht geladen werden. Du kannst die Auswahl
            überspringen oder es erneut versuchen.
          </Text>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Erneut laden"
            onPress={() => {
              void kurseQuery.refetch();
            }}
            className="mt-2 self-start"
          >
            <Text className="text-xs font-medium text-primary">
              Erneut laden
            </Text>
          </Pressable>
        </View>
      ) : filteredKurse.length === 0 ? (
        <Text className="my-4 text-center text-xs text-muted-foreground">
          {allKurse.length === 0
            ? "Keine Kurse verfügbar. Lege einen neuen Kurs an oder überspringe."
            : "Keine Kurse passen zur Suche."}
        </Text>
      ) : (
        // The list itself is rendered as a `radiogroup` so screen readers
        // announce the selection model. Individual rows already carry
        // `accessibilityRole="radio"` (see {@link KursListItem}).
        <View accessibilityRole="radiogroup" className="mb-3 max-h-72">
          <FlatList
            data={filteredKurse}
            keyExtractor={(k) => k.kursID}
            renderItem={({ item }) => (
              <KursListItem
                kurs={item}
                selected={selectedId === item.kursID}
                onPress={() => setSelectedId(item.kursID)}
              />
            )}
          />
        </View>
      )}

      <Button
        accessibilityLabel="Beitrittsanfrage senden"
        onPress={handleJoin}
        disabled={!canJoin}
        className="mb-3"
      >
        <Text className="text-sm font-medium text-primary-foreground">
          {submitting ? "Sende…" : "Beitrittsanfrage senden"}
        </Text>
      </Button>

      {!hideSkip && (
        <Button
          accessibilityLabel="Überspringen"
          variant="outline"
          onPress={goToDashboard}
          disabled={submitting}
          className="mb-3"
        >
          <Text className="text-sm font-medium">Später wählen</Text>
        </Button>
      )}

      <Button
        accessibilityLabel="Zurück"
        variant="ghost"
        onPress={onBack}
        disabled={submitting}
      >
        <Text className="text-sm font-medium">Zurück</Text>
      </Button>

      <NewKursDialog
        open={showNewKursDialog}
        onOpenChange={setShowNewKursDialog}
        onCreated={goToDashboard}
      />
    </View>
  );
}
