/**
 * SYSTEMADMIN-only "Backups" section for the Verwaltung screen.
 *
 * <p>Surfaces:</p>
 * <ul>
 *   <li>The most recent successful run (timestamp + tally counts).</li>
 *   <li>A list of all backups, newest first.</li>
 *   <li>A "Backup jetzt erstellen" button to trigger a manual run.</li>
 *   <li>A "Wiederherstellen" affordance per row that requires the
 *       admin to confirm twice — exactly mirrors the backend's
 *       {@code confirmationCount >= 2} rule. The first confirm shows
 *       a warning, the second acts as the safety net.</li>
 * </ul>
 *
 * <p>The section is rendered only when {@code user.studentType ===
 * "SYSTEMADMIN"}; the parent screen is responsible for the gate.</p>
 */
import { useState } from "react";
import { ActivityIndicator, View } from "react-native";
import { toast } from "sonner-native";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Text } from "@/components/ui/text";
import {
  useBackupCreate,
  useBackupDelete,
  useBackupRestore,
  useBackups,
  useLatestBackup,
} from "@/hooks/useBackups";
import { toastForApiError } from "@/lib/api/toastForApiError";
import type { BackupDTO } from "@/lib/api/types.gen";
import { t } from "@/lib/i18n/t";

/**
 * Format an ISO LocalDateTime string into a German timestamp.
 *
 * @param iso - ISO date-time string.
 * @returns formatted timestamp like "26.04.2026, 03:00".
 */
function _formatTimestamp(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso;
  return date.toLocaleString("de-DE", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

/**
 * Render the backup management card.
 *
 * @returns the card element.
 */
export function BackupSection() {
  const backups = useBackups();
  const latest = useLatestBackup();
  const createBackup = useBackupCreate();
  const restoreBackup = useBackupRestore();
  const deleteBackup = useBackupDelete();

  // Two-step confirm state: first click marks the row as "armed",
  // second click within the same render-cycle actually runs the
  // restore. The backend additionally enforces confirmationCount >= 2.
  const [armedRestore, setArmedRestore] = useState<string | null>(null);
  const [armedDelete, setArmedDelete] = useState<string | null>(null);

  function handleCreate() {
    createBackup.mutate(undefined, {
      onSuccess: () => {
        toast.success(t("backup.create.success", "Backup erstellt"));
      },
      onError: (err) => {
        toast.error(
          toastForApiError(err, {
            403: t(
              "backup.error.forbidden",
              "Nur Systemadmins dürfen Backups verwalten.",
            ),
          }),
        );
      },
    });
  }

  function handleRestoreClick(backup: BackupDTO) {
    if (armedRestore !== backup.backupID) {
      setArmedRestore(backup.backupID);
      // Cancel an armed delete on the same row when the user
      // switches between actions — only one armed action at a time.
      setArmedDelete(null);
      toast(
        t(
          "backup.restore.firstConfirm",
          "Erneut auf Wiederherstellen klicken zum Bestätigen.",
        ),
      );
      return;
    }
    restoreBackup.mutate(
      { backupId: backup.backupID, confirmationCount: 2 },
      {
        onSuccess: () => {
          toast.success(
            t("backup.restore.success", "Backup wiederhergestellt"),
          );
          setArmedRestore(null);
        },
        onError: (err) => {
          toast.error(
            toastForApiError(err, {
              400: t(
                "backup.restore.error.confirm",
                "Backup-Wiederherstellung erfordert 2-fache Bestätigung.",
              ),
              403: t(
                "backup.error.forbidden",
                "Nur Systemadmins dürfen Backups verwalten.",
              ),
            }),
          );
        },
      },
    );
  }

  function handleDeleteClick(backup: BackupDTO) {
    if (armedDelete !== backup.backupID) {
      setArmedDelete(backup.backupID);
      setArmedRestore(null);
      toast(
        t(
          "backup.delete.firstConfirm",
          "Erneut auf Löschen klicken zum Bestätigen.",
        ),
      );
      return;
    }
    deleteBackup.mutate(
      { backupId: backup.backupID, confirmationCount: 2 },
      {
        onSuccess: () => {
          toast.success(t("backup.delete.success", "Backup gelöscht"));
          setArmedDelete(null);
        },
        onError: (err) => {
          toast.error(
            toastForApiError(err, {
              400: t(
                "backup.delete.error.confirm",
                "Backup-Löschen erfordert 2-fache Bestätigung.",
              ),
              403: t(
                "backup.error.forbidden",
                "Nur Systemadmins dürfen Backups verwalten.",
              ),
            }),
          );
        },
      },
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("backup.title", "Automatische Backups")}</CardTitle>
      </CardHeader>
      <CardContent>
        <View className="gap-3">
          {/* Latest run summary */}
          <View className="rounded-md border border-border bg-muted/30 p-3">
            {latest.isLoading && (
              <ActivityIndicator color="hsl(var(--muted-foreground))" />
            )}
            {!latest.isLoading && latest.data === null && (
              <Text className="text-sm text-muted-foreground">
                {t("backup.none", "Noch kein Backup vorhanden.")}
              </Text>
            )}
            {!latest.isLoading && latest.data && (
              <>
                <Text className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  {t("backup.latest", "Letzter erfolgreicher Lauf")}
                </Text>
                <Text className="text-sm font-medium text-foreground">
                  {_formatTimestamp(latest.data.erstelltAm)}
                  {" · "}
                  {latest.data.quelle === "AUTO"
                    ? t("backup.source.auto", "Automatisch")
                    : t("backup.source.manual", "Manuell")}
                </Text>
                <Text className="text-xs text-muted-foreground">
                  {`${latest.data.dateiAnzahl} Dateien · ${latest.data.kommentarAnzahl} Kommentare · ${latest.data.bewertungAnzahl} Bewertungen · ${latest.data.tagAnzahl} Tags`}
                </Text>
              </>
            )}
          </View>

          {/* Manual trigger */}
          <Button
            onPress={handleCreate}
            disabled={createBackup.isPending}
            testID="backup-create"
          >
            {createBackup.isPending ? (
              <ActivityIndicator size="small" color="white" />
            ) : (
              <Text>{t("backup.create.button", "Backup jetzt erstellen")}</Text>
            )}
          </Button>

          {/* History */}
          <View className="gap-1.5">
            <Text className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
              {t("backup.history", "Bisherige Backups")}
            </Text>
            {backups.isLoading && (
              <ActivityIndicator color="hsl(var(--muted-foreground))" />
            )}
            {!backups.isLoading && (backups.data?.length ?? 0) === 0 && (
              <Text className="text-xs text-muted-foreground">
                {t("backup.history.empty", "Noch keine Backups vorhanden.")}
              </Text>
            )}
            {(backups.data ?? []).map((b) => (
              <View
                key={b.backupID}
                className="flex-row items-center justify-between gap-2 rounded-md border border-border bg-card p-2"
                testID={`backup-row-${b.backupID}`}
              >
                <View className="flex-1">
                  <Text className="text-sm font-medium text-foreground">
                    {_formatTimestamp(b.erstelltAm)}
                    {" · "}
                    {b.quelle === "AUTO"
                      ? t("backup.source.auto", "Automatisch")
                      : t("backup.source.manual", "Manuell")}
                  </Text>
                  <Text className="text-[11px] text-muted-foreground">
                    {`${b.dateiAnzahl} D · ${b.kommentarAnzahl} K · ${b.bewertungAnzahl} B · ${b.tagAnzahl} T`}
                  </Text>
                </View>
                <View className="flex-row gap-1">
                  <Button
                    variant={
                      armedRestore === b.backupID ? "destructive" : "outline"
                    }
                    onPress={() => handleRestoreClick(b)}
                    disabled={restoreBackup.isPending}
                    testID={`backup-restore-${b.backupID}`}
                  >
                    <Text>
                      {armedRestore === b.backupID
                        ? t(
                            "backup.restore.confirm",
                            "Wirklich wiederherstellen?",
                          )
                        : t("backup.restore.button", "Wiederherstellen")}
                    </Text>
                  </Button>
                  <Button
                    variant={
                      armedDelete === b.backupID ? "destructive" : "outline"
                    }
                    onPress={() => handleDeleteClick(b)}
                    disabled={deleteBackup.isPending}
                    testID={`backup-delete-${b.backupID}`}
                  >
                    <Text>
                      {armedDelete === b.backupID
                        ? t("backup.delete.confirm", "Wirklich löschen?")
                        : t("backup.delete.button", "Löschen")}
                    </Text>
                  </Button>
                </View>
              </View>
            ))}
            <Text className="mt-1 text-[11px] text-muted-foreground">
              {t(
                "backup.retention.hint",
                "Backups werden nach 14 Tagen automatisch entfernt.",
              )}
            </Text>
          </View>
        </View>
      </CardContent>
    </Card>
  );
}
