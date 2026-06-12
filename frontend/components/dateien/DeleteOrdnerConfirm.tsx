/**
 * Confirmation dialog for the "Ordner löschen" action.
 *
 * Wraps {@link ConfirmDialog} (variant `destructive`) with a cascade hint
 * — the backend recursively deletes child folders and all contained
 * files. The component is a thin orchestrator that drives
 * {@link useOrdnerDelete} and maps its errors to a toast; the dialog
 * stays open on failure so the user can retry.
 */
import { toast } from "sonner-native";

import { ConfirmDialog } from "@/components/ConfirmDialog";
import { useOrdnerDelete } from "@/hooks/useOrdnerDelete";
import { toastForApiError } from "@/lib/api/toastForApiError";
import { t } from "@/lib/i18n/t";

export interface DeleteOrdnerConfirmProps {
  /** Controls dialog visibility. */
  open: boolean;
  /** Called when the dialog should open or close. */
  onOpenChange: (open: boolean) => void;
  /** UUID of the folder to delete. `null` while no folder is targeted. */
  ordnerId: string | null;
  /** Display name of the folder, surfaced in the cascade hint. */
  ordnerName: string;
  /**
   * Parent folder UUID, or `null` for a top-level folder. Forwarded to
   * the delete mutation for cache invalidation.
   */
  ueberOrdnerId: string | null;
}

/**
 * Maps an error from the delete endpoint to the toast text shown to
 * the user. Always prefers the backend's own German message text and
 * only falls back to the localised hint when no body is available.
 *
 * @param err - The thrown error from the mutation's onError callback.
 * @returns The localised toast message.
 */
function _deleteErrorMessage(err: unknown): string {
  return toastForApiError(err, {
    403: t(
      "ordner.delete.error.forbidden",
      "Keine Berechtigung zum Löschen.",
    ),
    404: t("ordner.delete.error.notFound", "Ordner nicht gefunden."),
  });
}

/**
 * Renders the destructive delete-folder confirmation.
 *
 * @param props - {@link DeleteOrdnerConfirmProps}
 * @returns The confirm dialog element.
 */
export function DeleteOrdnerConfirm({
  open,
  onOpenChange,
  ordnerId,
  ordnerName,
  ueberOrdnerId,
}: DeleteOrdnerConfirmProps) {
  const del = useOrdnerDelete();

  async function handleConfirm() {
    if (ordnerId === null) return;
    try {
      await del.mutateAsync({ ordnerId, ueberOrdnerId });
      toast.success(t("ordner.delete.success", "Ordner gelöscht"));
    } catch (err) {
      toast.error(_deleteErrorMessage(err));
      // Rethrow so ConfirmDialog keeps itself open.
      throw err;
    }
  }

  return (
    <ConfirmDialog
      open={open}
      onOpenChange={onOpenChange}
      title={t("ordner.delete.title", "Ordner löschen?")}
      message={t(
        "ordner.delete.message",
        "Ordner '{name}' und alle enthaltenen Unterordner und Dateien werden unwiderruflich gelöscht.",
      ).replace("{name}", ordnerName)}
      confirmLabel={t("ordner.delete.confirm", "Löschen")}
      variant="destructive"
      onConfirm={handleConfirm}
    />
  );
}
