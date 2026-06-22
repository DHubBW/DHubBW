/**
 * Modal dialog that lets the user rate a single file with 1..5 stars.
 *
 * <p>The dialog reads the caller's current rating via
 * {@link useDateiBewertung} (so the picker can pre-select the
 * existing pick) and submits new picks via {@link useBewertungSet}.
 * Half-stars are intentionally not supported — the picker is whole
 * stars only.</p>
 */
import { useEffect, useState } from "react";
import { ActivityIndicator, View } from "react-native";
import { toast } from "sonner-native";

import { StarRating } from "@/components/dateien/StarRating";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Text } from "@/components/ui/text";
import { useBewertungSet } from "@/hooks/useBewertungSet";
import { useDateiBewertung } from "@/hooks/useDateiBewertung";
import { toastForApiError } from "@/lib/api/toastForApiError";
import type { DateiDTO } from "@/lib/api/types.gen";
import { t } from "@/lib/i18n/t";

export interface BewertungDialogProps {
  /** Controls dialog visibility. */
  open: boolean;
  /** Called when the dialog should open or close. */
  onOpenChange: (open: boolean) => void;
  /** File to rate, or {@code null} while no row is targeted. */
  datei: DateiDTO | null;
  /**
   * UUID of the folder the file belongs to, or {@code null} for the
   * root view. Forwarded to the rating mutation for cache
   * invalidation of the right list.
   */
  ordnerId: string | null;
}

/**
 * Maps an error from the rating endpoint to the toast text.
 *
 * @param err - thrown error from the mutation.
 * @returns the localised toast text.
 */
function _bewertungErrorMessage(err: unknown): string {
  return toastForApiError(err, {
    400: t("bewertung.error.invalidStars", "Bitte 1 bis 5 Sterne auswählen."),
    403: t("bewertung.error.forbidden", "Keine Berechtigung zum Bewerten."),
    404: t("bewertung.error.notFound", "Datei nicht gefunden."),
  });
}

/**
 * Renders the rating dialog.
 *
 * @param props - {@link BewertungDialogProps}
 * @returns the dialog element.
 */
export function BewertungDialog({
  open,
  onOpenChange,
  datei,
  ordnerId,
}: BewertungDialogProps) {
  const dateiId = datei?.dateiID ?? null;
  const aggregate = useDateiBewertung(dateiId);
  const setRating = useBewertungSet();

  const [picked, setPicked] = useState<number>(0);

  // Whenever the dialog opens for a different file pre-fill the
  // picker with the caller's existing pick (or 0 if none).
  useEffect(() => {
    if (open && aggregate.data) {
      setPicked(aggregate.data.eigeneSterne);
    }
    if (!open) {
      setPicked(0);
    }
  }, [open, dateiId, aggregate.data]);

  function handleOpenChange(nextOpen: boolean) {
    if (setRating.isPending) return;
    onOpenChange(nextOpen);
  }

  function handleSubmit() {
    if (!dateiId || picked < 1) return;
    setRating.mutate(
      { dateiId, sterne: picked, ordnerId },
      {
        onSuccess: () => {
          toast.success(t("bewertung.success", "Bewertung gespeichert"));
          onOpenChange(false);
        },
        onError: (err) => {
          toast.error(_bewertungErrorMessage(err));
        },
      },
    );
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent testID="bewertung-dialog">
        <DialogHeader>
          <DialogTitle>
            {t("bewertung.title", "Bewerten")}
            {datei?.name ? ` – ${datei.name}` : ""}
          </DialogTitle>
          <DialogDescription>
            {t(
              "bewertung.description",
              "Vergib 1 bis 5 Sterne. Du kannst deine Bewertung " +
                "jederzeit ändern.",
            )}
          </DialogDescription>
        </DialogHeader>
        <View className="items-center gap-3 py-3">
          <StarRating
            value={picked}
            interactive
            onChange={setPicked}
            size={28}
            testID="bewertung-picker"
          />
          {aggregate.data && (
            <Text className="text-xs text-muted-foreground">
              {aggregate.data.anzahl > 0
                ? t("bewertung.average", "Durchschnitt {avg} ({count} Stimmen)")
                    .replace("{avg}", aggregate.data.durchschnitt.toFixed(1))
                    .replace("{count}", String(aggregate.data.anzahl))
                : t("bewertung.noVotesYet", "Noch keine Bewertungen.")}
            </Text>
          )}
        </View>
        <DialogFooter>
          <Button
            variant="outline"
            onPress={() => handleOpenChange(false)}
            disabled={setRating.isPending}
            testID="bewertung-cancel"
          >
            <Text>{t("confirm.cancel", "Abbrechen")}</Text>
          </Button>
          <Button
            onPress={handleSubmit}
            disabled={picked < 1 || setRating.isPending}
            testID="bewertung-submit"
          >
            {setRating.isPending ? (
              <ActivityIndicator size="small" color="white" />
            ) : (
              <Text>{t("bewertung.submit", "Speichern")}</Text>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
