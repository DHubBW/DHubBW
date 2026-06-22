/**
 * Combined detail dialog for a single file.
 *
 * <p>This is the new primary "click on a file" target — it replaces
 * the standalone preview dialog with a split layout: on the left the
 * existing inline preview (PDF / image / video / audio / iframe);
 * on the right a sidebar of advanced metadata. The sidebar groups
 * everything that used to be hidden behind the row's 3-dot menu so
 * comments, tags, and ratings are now first-class:</p>
 * <ul>
 *   <li>File metadata (uploader, size, last modified, downloads).</li>
 *   <li>Star rating — clickable, stars submit a vote.</li>
 *   <li>Tag chips with add / remove affordances for authorised
 *       users.</li>
 *   <li>Comment thread with composer and inline edit / delete on
 *       individual entries.</li>
 * </ul>
 *
 * <p>On narrow viewports the layout collapses to a single column with
 * the preview on top and the sidebar below — both panes scroll
 * independently.</p>
 *
 * <p>Authorisation is enforced by the backend. The dialog reads
 * {@code canEdit} / {@code canDelete} flags from each comment row and
 * maps tag-management visibility to the file's uploader / KURSADMIN
 * status; on rejection downstream toasts surface the 403 message.</p>
 */
import { Download, Plus, Trash2 } from "lucide-react-native";
import * as React from "react";
import { useEffect, useRef, useState } from "react";
import {
  ActivityIndicator,
  Platform,
  Pressable,
  ScrollView,
  useWindowDimensions,
  View,
} from "react-native";
import { toast } from "sonner-native";

import { ConfirmDialog } from "@/components/ConfirmDialog";
import { StarRating } from "@/components/dateien/StarRating";
import { TagBadge } from "@/components/dateien/TagBadge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Text } from "@/components/ui/text";
import { useBewertungSet } from "@/hooks/useBewertungSet";
import { useDateiBewertung } from "@/hooks/useDateiBewertung";
import {
  revokePreviewUrl,
  useDateiPreview,
  type FilePreviewError,
} from "@/hooks/useDateiPreview";
import { useKommentarCreate } from "@/hooks/useKommentarCreate";
import { useKommentarDelete } from "@/hooks/useKommentarDelete";
import { useKommentarUpdate } from "@/hooks/useKommentarUpdate";
import { useKommentareForDatei } from "@/hooks/useKommentareForDatei";
import {
  useDateiTagAttach,
  useDateiTagDetach,
  useTagCreate,
  useTagDelete,
} from "@/hooks/useTagMutations";
import { useTags } from "@/hooks/useTags";
import { toastForApiError } from "@/lib/api/toastForApiError";
import type { DateiDTO, KommentarDTO, StudentType } from "@/lib/api/types.gen";
import { useAuth } from "@/lib/auth/AuthContext";
import { t } from "@/lib/i18n/t";
import { formatFileSize, formatIsoDate } from "@/lib/files/formatters";

const SIDEBAR_BREAKPOINT_PX = 900;

export interface DateiDetailDialogProps {
  /** Controls dialog visibility. */
  open: boolean;
  /** Called when the dialog should open or close. */
  onOpenChange: (open: boolean) => void;
  /** File to show, or {@code null} while no row is targeted. */
  datei: DateiDTO | null;
  /**
   * UUID of the folder the file belongs to, or {@code null} for the
   * root view. Used for cache invalidation of the right list.
   */
  ordnerId: string | null;
  /**
   * The folder DTO containing the file, when known. Used purely as
   * a UI gate for tag-management visibility — KURSADMINs only see
   * the tag picker for files of their own course; SYSTEMADMINs see
   * it always; uploaders see it for their own files.
   */
  selectedFolder?: { kursID: string | null } | null;
  /**
   * Optional download trigger forwarded to the preview fallback.
   */
  onDownload?: (datei: DateiDTO) => void;
}

/**
 * Decide whether the current user may add or remove tags on the
 * given file. Mirrors the backend rule:
 * SYSTEMADMIN ∨ uploader ∨ KURSADMIN of the file's course.
 *
 * @param datei - the file in question.
 * @param studentType - the caller's role.
 * @param userKursId - the caller's kursID (optional).
 * @param uploaderName - rough proxy for ownership when no IDs are
 *   exposed; the uploader is identified by name in the DTO. The
 *   backend remains authoritative — this is a UI gate only.
 * @returns whether the tag controls should be shown.
 */
function _canManageTags(
  datei: DateiDTO,
  studentType: StudentType | undefined,
  userName: string | undefined,
  userKursId: string | null | undefined,
  folderKursId: string | null | undefined,
): boolean {
  if (!studentType) return false;
  if (studentType === "SYSTEMADMIN") return true;
  if (studentType === "KURSADMIN") {
    // KURSADMINs only manage tags on files of their own course.
    // When the folder kursID isn't known (root list view), hide
    // the controls — the backend would 403 anyway, and showing
    // them would suggest the action is available when it's not.
    return Boolean(userKursId && folderKursId && userKursId === folderKursId);
  }
  return Boolean(
    studentType === "STUDENT" &&
    datei.uploaderName &&
    userName &&
    datei.uploaderName === userName,
  );
}

/**
 * Format an ISO LocalDateTime string into a German timestamp.
 *
 * @param iso - ISO date-time string.
 * @returns formatted timestamp.
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

function _previewErrorMessage(err: FilePreviewError | null): string {
  if (!err) {
    return t("dateien.preview.error.unknown", "Vorschau fehlgeschlagen.");
  }
  switch (err.code) {
    case "forbidden":
      return t(
        "dateien.preview.error.forbidden",
        "Keine Berechtigung für die Vorschau.",
      );
    case "not-found":
      return t("dateien.preview.error.notFound", "Datei nicht gefunden.");
    case "network":
      return t("error.network", "Netzwerkfehler – Server nicht erreichbar");
    case "native-unsupported":
      return t(
        "dateien.preview.error.native",
        "Datei-Vorschau ist auf Native im MVP noch nicht verfügbar.",
      );
    default:
      return t("dateien.preview.error.unknown", "Vorschau fehlgeschlagen.");
  }
}

/**
 * Renders the combined detail dialog.
 *
 * @param props - {@link DateiDetailDialogProps}
 * @returns the dialog element.
 */
export function DateiDetailDialog({
  open,
  onOpenChange,
  datei,
  ordnerId,
  selectedFolder,
  onDownload,
}: DateiDetailDialogProps) {
  const { width } = useWindowDimensions();
  const isWide = width >= SIDEBAR_BREAKPOINT_PX;
  const isWeb = Platform.OS === "web";
  const { user } = useAuth();

  const dateiId = datei?.dateiID ?? null;
  const preview = useDateiPreview();
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [contentType, setContentType] = useState<string>("");
  const activeUrlRef = useRef<string | null>(null);

  // Trigger the preview fetch whenever the dialog opens for a fresh
  // file — same lifecycle as the standalone PreviewDialog.
  useEffect(() => {
    if (!open || !datei || !isWeb) return;
    let cancelled = false;
    preview.mutate(
      { dateiId: datei.dateiID },
      {
        onSuccess: ({ url, contentType: ct }) => {
          if (cancelled) {
            revokePreviewUrl(url);
            return;
          }
          revokePreviewUrl(activeUrlRef.current);
          activeUrlRef.current = url;
          setPreviewUrl(url);
          setContentType(ct);
        },
        onError: () => {
          if (cancelled) return;
          // Preview failures are surfaced inline rather than as a
          // toast so the sidebar still works (comments / tags etc.).
        },
      },
    );
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, dateiId, isWeb]);

  useEffect(() => {
    if (!open) {
      revokePreviewUrl(activeUrlRef.current);
      activeUrlRef.current = null;
      setPreviewUrl(null);
      setContentType("");
    }
  }, [open]);

  useEffect(() => {
    return () => {
      revokePreviewUrl(activeUrlRef.current);
      activeUrlRef.current = null;
    };
  }, []);

  // --- Comments ---
  const kommentare = useKommentareForDatei(dateiId);
  const createKomm = useKommentarCreate();
  const updateKomm = useKommentarUpdate();
  const deleteKomm = useKommentarDelete();
  const [draft, setDraft] = useState("");
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editingDraft, setEditingDraft] = useState("");
  const [deleteKommTarget, setDeleteKommTarget] = useState<KommentarDTO | null>(
    null,
  );

  useEffect(() => {
    if (!open) {
      setDraft("");
      setEditingId(null);
      setEditingDraft("");
      setDeleteKommTarget(null);
    }
  }, [open, dateiId]);

  // --- Rating ---
  const aggregate = useDateiBewertung(dateiId);
  const setRating = useBewertungSet();

  // --- Tags ---
  const allTags = useTags();
  const tagAttach = useDateiTagAttach();
  const tagDetach = useDateiTagDetach();
  const tagCreate = useTagCreate();
  const tagDelete = useTagDelete();
  const [showTagPicker, setShowTagPicker] = useState(false);
  const [newTagName, setNewTagName] = useState("");
  const canManageTags =
    datei !== null &&
    _canManageTags(
      datei,
      user?.studentType,
      user?.name,
      user?.kursID ?? null,
      selectedFolder?.kursID ?? null,
    );

  function handleSubmitComment() {
    if (!dateiId) return;
    const trimmed = draft.trim();
    if (trimmed.length === 0) return;
    createKomm.mutate(
      { dateiId, inhalt: trimmed },
      {
        onSuccess: () => {
          setDraft("");
          toast.success(
            t("kommentare.create.success", "Kommentar veröffentlicht"),
          );
        },
        onError: (err) => {
          toast.error(
            toastForApiError(err, {
              400: t(
                "kommentare.create.error.invalidInhalt",
                "Kommentar darf nicht leer sein.",
              ),
              403: t(
                "kommentare.create.error.forbidden",
                "Keine Berechtigung zum Kommentieren.",
              ),
              404: t(
                "kommentare.create.error.notFound",
                "Datei nicht gefunden.",
              ),
            }),
          );
        },
      },
    );
  }

  function handleStartEdit(k: KommentarDTO) {
    setEditingId(k.kommentarID);
    setEditingDraft(k.inhalt);
  }

  function handleSaveEdit() {
    if (!dateiId || !editingId) return;
    const trimmed = editingDraft.trim();
    if (trimmed.length === 0) return;
    updateKomm.mutate(
      { kommentarId: editingId, inhalt: trimmed, dateiId },
      {
        onSuccess: () => {
          setEditingId(null);
          setEditingDraft("");
          toast.success(
            t("kommentare.update.success", "Kommentar aktualisiert"),
          );
        },
        onError: (err) => {
          toast.error(
            toastForApiError(err, {
              403: t(
                "kommentare.update.error.forbidden",
                "Nur der Autor darf den Kommentar bearbeiten.",
              ),
            }),
          );
        },
      },
    );
  }

  async function handleConfirmDeleteKomm() {
    if (!dateiId || !deleteKommTarget) return;
    try {
      await deleteKomm.mutateAsync({
        kommentarId: deleteKommTarget.kommentarID,
        dateiId,
      });
      toast.success(t("kommentare.delete.success", "Kommentar gelöscht"));
      setDeleteKommTarget(null);
    } catch (err) {
      toast.error(
        toastForApiError(err, {
          403: t(
            "kommentare.delete.error.forbidden",
            "Keine Berechtigung zum Löschen des Kommentars.",
          ),
        }),
      );
      throw err;
    }
  }

  function handleSetRating(stars: number) {
    if (!dateiId) return;
    setRating.mutate(
      { dateiId, sterne: stars, ordnerId },
      {
        onSuccess: () => {
          toast.success(t("bewertung.success", "Bewertung gespeichert"));
        },
        onError: (err) => {
          toast.error(
            toastForApiError(err, {
              400: t(
                "bewertung.error.invalidStars",
                "Bitte 1 bis 5 Sterne auswählen.",
              ),
              403: t(
                "bewertung.error.forbidden",
                "Keine Berechtigung zum Bewerten.",
              ),
            }),
          );
        },
      },
    );
  }

  function handleAttachTag(tagId: string) {
    if (!dateiId) return;
    tagAttach.mutate(
      { dateiId, tagId, ordnerId },
      {
        onSuccess: () => {
          toast.success(t("tags.attach.success", "Tag hinzugefügt"));
          setShowTagPicker(false);
        },
        onError: (err) => {
          toast.error(
            toastForApiError(err, {
              403: t(
                "tags.error.forbidden",
                "Keine Berechtigung zum Bearbeiten der Tags.",
              ),
            }),
          );
        },
      },
    );
  }

  function handleDetachTag(tagId: string) {
    if (!dateiId) return;
    tagDetach.mutate(
      { dateiId, tagId, ordnerId },
      {
        onSuccess: () => {
          toast.success(t("tags.detach.success", "Tag entfernt"));
        },
        onError: (err) => {
          toast.error(
            toastForApiError(err, {
              403: t(
                "tags.error.forbidden",
                "Keine Berechtigung zum Bearbeiten der Tags.",
              ),
            }),
          );
        },
      },
    );
  }

  function handleDeleteTag(tagId: string) {
    tagDelete.mutate(
      { tagId },
      {
        onSuccess: () => {
          toast.success(t("tags.delete.success", "Tag gelöscht"));
        },
        onError: (err) => {
          toast.error(
            toastForApiError(err, {
              403: t(
                "tags.delete.error.forbidden",
                "Nur Systemadmins dürfen Tags global löschen.",
              ),
              404: t("tags.delete.error.notFound", "Tag nicht gefunden."),
            }),
          );
        },
      },
    );
  }

  function handleCreateTag() {
    const trimmed = newTagName.trim();
    if (trimmed.length === 0 || !dateiId) return;
    tagCreate.mutate(
      { name: trimmed },
      {
        onSuccess: (created) => {
          setNewTagName("");
          // Auto-attach the freshly created tag — what the user
          // expects when they type a brand-new tag in the picker.
          tagAttach.mutate(
            { dateiId, tagId: created.tagID, ordnerId },
            {
              onSuccess: () => {
                toast.success(t("tags.create.success", "Tag erstellt"));
                setShowTagPicker(false);
              },
            },
          );
        },
        onError: (err) => {
          toast.error(
            toastForApiError(err, {
              403: t(
                "tags.create.error.forbidden",
                "Nur Kursadmins und Sysadmins dürfen Tags anlegen.",
              ),
              409: t(
                "tags.create.error.duplicate",
                "Ein Tag mit diesem Namen existiert bereits.",
              ),
            }),
          );
        },
      },
    );
  }

  if (!datei) return null;

  const attachedTagIds = new Set(datei.tags.map((t2) => t2.tagID));
  const availableTags =
    allTags.data?.filter((tag) => !attachedTagIds.has(tag.tagID)) ?? [];

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent
          className="h-[92vh] w-[97vw] max-w-[97vw] sm:max-w-[97vw]"
          testID="datei-detail-dialog"
        >
          {/* Hidden title for accessibility — the visible
              filename lives in the sidebar's first card so we
              don't show a duplicate above the preview iframe. */}
          <DialogHeader>
            <DialogTitle className="sr-only">{datei.name}</DialogTitle>
          </DialogHeader>

          <View
            className={
              isWide
                ? "min-h-[78vh] flex-1 flex-row gap-3"
                : "min-h-[78vh] flex-1 flex-col gap-3"
            }
          >
            {/* Preview pane */}
            <View
              className={
                isWide
                  ? "flex-[2] overflow-hidden rounded-md border border-border bg-muted"
                  : "min-h-[40vh] overflow-hidden rounded-md border border-border bg-muted"
              }
            >
              <PreviewBody
                previewUrl={previewUrl}
                contentType={contentType}
                datei={datei}
                isLoading={preview.isPending && !previewUrl}
                isError={preview.isError && !previewUrl}
                errorMessage={_previewErrorMessage(preview.error)}
                onDownload={onDownload}
              />
            </View>

            {/* Info sidebar */}
            <ScrollView
              className={
                isWide
                  ? "flex-1 rounded-md border border-border bg-card"
                  : "flex-1 rounded-md border border-border bg-card"
              }
              contentContainerClassName="p-5 gap-5"
            >
              {/* Detail card — single left-aligned column whose
                  rows follow the order the user asked for: Name,
                  Tags, Hochgeladen von, Bewertung, Geändert,
                  Größe, Downloads. Visual hierarchy is one row
                  per fact with a quiet label above the value;
                  interactive elements (tags, rating) sit in
                  their value slot so the layout stays uniform. */}
              <View className="rounded-md border border-border bg-background">
                {/* 1. Name + folder context + Download CTA */}
                <View className="gap-2 p-4 pb-3">
                  <DetailLabel label={t("dateien.detail.name", "Name")} />
                  <Text
                    className="text-base font-semibold text-foreground"
                    numberOfLines={3}
                  >
                    {datei.name}
                  </Text>
                  <Text className="text-xs text-muted-foreground">
                    {datei.ordnerName}
                  </Text>
                  {onDownload && (
                    <View className="mt-1">
                      <Button
                        variant="default"
                        onPress={() => onDownload(datei)}
                        testID="detail-download"
                      >
                        <View className="flex-row items-center gap-2">
                          <Download
                            size={14}
                            color="hsl(var(--primary-foreground))"
                          />
                          <Text className="text-primary-foreground">
                            {t("dateien.detail.download", "Herunterladen")}
                          </Text>
                        </View>
                      </Button>
                    </View>
                  )}
                </View>

                {/* 2. Tags — left-aligned chips. Hidden when the
                    file has none AND the user can't manage. */}
                {(datei.tags.length > 0 || canManageTags) && (
                  <>
                    <View className="h-px bg-border" />
                    <View className="gap-1.5 p-4">
                      <DetailLabel label={t("dateien.detail.tags", "Tags")} />
                      <View className="flex-row flex-wrap items-center justify-start gap-1.5">
                        {datei.tags.map((tag) => (
                          <TagBadge
                            key={tag.tagID}
                            name={tag.name}
                            onRemove={
                              canManageTags
                                ? () => handleDetachTag(tag.tagID)
                                : undefined
                            }
                            testID={`detail-tag-${tag.tagID}`}
                          />
                        ))}
                        {canManageTags && (
                          <Pressable
                            accessibilityRole="button"
                            accessibilityLabel={t("tags.add", "Tag hinzufügen")}
                            onPress={() => setShowTagPicker((v) => !v)}
                            className="flex-row items-center gap-1 rounded-full border border-dashed border-border px-2 py-0.5 active:bg-accent"
                            testID="detail-tag-add"
                          >
                            <Plus size={11} color="hsl(var(--foreground))" />
                            <Text className="text-[11px] font-medium text-foreground">
                              {t("tags.add", "Tag hinzufügen")}
                            </Text>
                          </Pressable>
                        )}
                      </View>
                      {showTagPicker && canManageTags && (
                        <View className="mt-2 gap-2 rounded-md border border-border bg-card p-3">
                          <Text className="text-xs font-semibold text-foreground">
                            {t("tags.picker.existing", "Vorhandene Tags")}
                          </Text>
                          <View className="flex-row flex-wrap items-center gap-1.5">
                            {availableTags.length === 0 && (
                              <Text className="text-xs text-muted-foreground">
                                {t(
                                  "tags.picker.empty",
                                  "Keine weiteren Tags verfügbar.",
                                )}
                              </Text>
                            )}
                            {availableTags.map((tag) => (
                              <View
                                key={tag.tagID}
                                className="flex-row items-center gap-0.5"
                              >
                                <TagBadge
                                  name={tag.name}
                                  onPress={() => handleAttachTag(tag.tagID)}
                                  testID={`detail-tag-attach-${tag.tagID}`}
                                />
                                {user?.studentType === "SYSTEMADMIN" && (
                                  <Pressable
                                    accessibilityRole="button"
                                    accessibilityLabel={t(
                                      "tags.delete",
                                      "Tag global löschen",
                                    )}
                                    onPress={() => handleDeleteTag(tag.tagID)}
                                    className="rounded-full p-1 active:bg-destructive/10"
                                    testID={`detail-tag-delete-${tag.tagID}`}
                                  >
                                    <Trash2
                                      size={10}
                                      color="hsl(var(--destructive))"
                                    />
                                  </Pressable>
                                )}
                              </View>
                            ))}
                          </View>
                          {(user?.studentType === "KURSADMIN" ||
                            user?.studentType === "SYSTEMADMIN") && (
                            <View className="gap-1">
                              <Text className="text-xs font-semibold text-foreground">
                                {t("tags.picker.create", "Neuen Tag anlegen")}
                              </Text>
                              <View className="flex-row gap-2">
                                <View className="flex-1">
                                  <Input
                                    value={newTagName}
                                    onChangeText={setNewTagName}
                                    placeholder={t(
                                      "tags.picker.placeholder",
                                      "Neuer Tag-Name",
                                    )}
                                    testID="detail-tag-create-input"
                                  />
                                </View>
                                <Button
                                  onPress={handleCreateTag}
                                  disabled={
                                    tagCreate.isPending ||
                                    newTagName.trim().length === 0
                                  }
                                  testID="detail-tag-create-submit"
                                >
                                  {tagCreate.isPending ? (
                                    <ActivityIndicator
                                      size="small"
                                      color="white"
                                    />
                                  ) : (
                                    <Text>
                                      {t(
                                        "tags.picker.create.submit",
                                        "Anlegen",
                                      )}
                                    </Text>
                                  )}
                                </Button>
                              </View>
                            </View>
                          )}
                        </View>
                      )}
                    </View>
                  </>
                )}

                {/* 3. Hochgeladen von */}
                <View className="h-px bg-border" />
                <View className="gap-1 p-4">
                  <DetailLabel
                    label={t("dateien.detail.uploader", "Hochgeladen von")}
                  />
                  <Text className="text-sm text-foreground">
                    {datei.uploaderName ?? "—"}
                  </Text>
                </View>

                {/* 4. Bewertung */}
                <View className="h-px bg-border" />
                <View className="gap-1.5 p-4">
                  <DetailLabel
                    label={t("dateien.detail.rating", "Bewertung")}
                  />
                  <View className="flex-row items-center gap-3">
                    <StarRating
                      value={aggregate.data?.eigeneSterne ?? 0}
                      interactive
                      onChange={handleSetRating}
                      size={22}
                      testID="detail-bewertung-picker"
                    />
                    {datei.bewertungAnzahl > 0 && (
                      <Text className="text-xs text-muted-foreground">
                        Ø {datei.bewertungDurchschnitt.toFixed(1)} (
                        {datei.bewertungAnzahl})
                      </Text>
                    )}
                  </View>
                </View>

                {/* 5. Geändert */}
                <View className="h-px bg-border" />
                <View className="gap-1 p-4">
                  <DetailLabel
                    label={t("dateien.detail.modified", "Geändert")}
                  />
                  <Text className="text-sm tabular-nums text-foreground">
                    {formatIsoDate(datei.zuletztBearbeitet)}
                  </Text>
                </View>

                {/* 6. Größe */}
                <View className="h-px bg-border" />
                <View className="gap-1 p-4">
                  <DetailLabel label={t("dateien.detail.size", "Größe")} />
                  <Text className="text-sm tabular-nums text-foreground">
                    {formatFileSize(datei.fileSize)}
                  </Text>
                </View>

                {/* 7. Downloads */}
                <View className="h-px bg-border" />
                <View className="gap-1 p-4">
                  <DetailLabel
                    label={t("dateien.detail.downloads", "Downloads")}
                  />
                  <Text className="text-sm tabular-nums text-foreground">
                    {datei.downloadAnzahl}
                  </Text>
                </View>
              </View>

              {/* Comments */}
              <View className="gap-1.5">
                <SectionLabel
                  label={t("dateien.detail.kommentare", "Kommentare")}
                />
                {kommentare.isLoading && (
                  <ActivityIndicator color="hsl(var(--muted-foreground))" />
                )}
                {!kommentare.isLoading &&
                  (kommentare.data?.length ?? 0) === 0 && (
                    <Text className="text-xs text-muted-foreground">
                      {t(
                        "kommentare.empty",
                        "Noch keine Kommentare. Sei der erste!",
                      )}
                    </Text>
                  )}
                {(kommentare.data ?? []).map((k) => (
                  <View
                    key={k.kommentarID}
                    className="rounded-md border border-border bg-background p-2"
                    testID={`detail-kommentar-${k.kommentarID}`}
                  >
                    <View className="flex-row items-start justify-between gap-1">
                      <View className="flex-1">
                        <Text className="text-xs font-semibold text-foreground">
                          {k.autorName ??
                            t("kommentare.unknownAuthor", "Unbekannt")}
                        </Text>
                        <Text className="text-[10px] text-muted-foreground">
                          {_formatTimestamp(k.erstelltAm)}
                          {k.bearbeitet && k.zuletztBearbeitet
                            ? ` · ${t("kommentare.editedAt", "bearbeitet")} ${_formatTimestamp(k.zuletztBearbeitet)}`
                            : ""}
                        </Text>
                      </View>
                      {(k.canEdit || k.canDelete) &&
                        editingId !== k.kommentarID && (
                          <View className="flex-row gap-1">
                            {k.canEdit && (
                              <Pressable
                                accessibilityRole="button"
                                onPress={() => handleStartEdit(k)}
                                className="rounded-md p-1 active:bg-accent"
                                testID={`detail-kommentar-edit-${k.kommentarID}`}
                              >
                                <Text className="text-[10px] text-muted-foreground">
                                  {t("kommentare.actions.editShort", "Bearb.")}
                                </Text>
                              </Pressable>
                            )}
                            {k.canDelete && (
                              <Pressable
                                accessibilityRole="button"
                                onPress={() => setDeleteKommTarget(k)}
                                className="rounded-md p-1 active:bg-accent"
                                testID={`detail-kommentar-delete-${k.kommentarID}`}
                              >
                                <Trash2
                                  size={11}
                                  color="hsl(var(--destructive))"
                                />
                              </Pressable>
                            )}
                          </View>
                        )}
                    </View>
                    {editingId === k.kommentarID ? (
                      <View className="mt-1.5 gap-1.5">
                        <Input
                          value={editingDraft}
                          onChangeText={setEditingDraft}
                          editable={!updateKomm.isPending}
                          onSubmitEditing={handleSaveEdit}
                        />
                        <View className="flex-row justify-end gap-1.5">
                          <Button
                            variant="outline"
                            onPress={() => {
                              setEditingId(null);
                              setEditingDraft("");
                            }}
                            disabled={updateKomm.isPending}
                          >
                            <Text>{t("confirm.cancel", "Abbrechen")}</Text>
                          </Button>
                          <Button
                            onPress={handleSaveEdit}
                            disabled={
                              updateKomm.isPending ||
                              editingDraft.trim().length === 0
                            }
                          >
                            <Text>
                              {t("kommentare.update.submit", "Speichern")}
                            </Text>
                          </Button>
                        </View>
                      </View>
                    ) : (
                      <Text className="mt-1 text-xs text-foreground">
                        {k.inhalt}
                      </Text>
                    )}
                  </View>
                ))}
                {/* Composer */}
                <View className="gap-1.5">
                  <Input
                    value={draft}
                    onChangeText={setDraft}
                    placeholder={t(
                      "kommentare.create.placeholder",
                      "Schreibe einen Kommentar …",
                    )}
                    editable={!createKomm.isPending && dateiId !== null}
                    onSubmitEditing={handleSubmitComment}
                    testID="detail-kommentar-input"
                  />
                  <Button
                    onPress={handleSubmitComment}
                    disabled={createKomm.isPending || draft.trim().length === 0}
                    testID="detail-kommentar-submit"
                  >
                    {createKomm.isPending ? (
                      <ActivityIndicator size="small" color="white" />
                    ) : (
                      <Text>
                        {t("kommentare.create.submit", "Kommentar senden")}
                      </Text>
                    )}
                  </Button>
                </View>
              </View>
            </ScrollView>
          </View>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={deleteKommTarget !== null}
        onOpenChange={(next) => {
          if (!next) setDeleteKommTarget(null);
        }}
        title={t("kommentare.delete.title", "Kommentar löschen?")}
        message={t(
          "kommentare.delete.message",
          "Der Kommentar wird unwiderruflich gelöscht.",
        )}
        confirmLabel={t("kommentare.delete.confirm", "Löschen")}
        variant="destructive"
        onConfirm={handleConfirmDeleteKomm}
      />
    </>
  );
}

interface SectionLabelProps {
  label: string;
}

function SectionLabel({ label }: SectionLabelProps) {
  return (
    <Text className="text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">
      {label}
    </Text>
  );
}

/**
 * Quiet inline label used inside the detail card. Same typography
 * as {@link SectionLabel} but explicitly left-aligned and tighter
 * vertically — fits the per-row "label above value" structure of
 * the new sidebar.
 */
function DetailLabel({ label }: SectionLabelProps) {
  return (
    <Text className="text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">
      {label}
    </Text>
  );
}

interface MetadataRowProps {
  label: string;
  value: string;
}

function MetadataRow({ label, value }: MetadataRowProps) {
  return (
    <View className="flex-row items-baseline justify-between gap-2">
      <Text className="text-xs text-muted-foreground">{label}</Text>
      <Text className="text-xs font-medium text-foreground">{value}</Text>
    </View>
  );
}

interface PreviewBodyProps {
  previewUrl: string | null;
  contentType: string;
  datei: DateiDTO;
  isLoading: boolean;
  isError: boolean;
  errorMessage: string;
  onDownload?: (datei: DateiDTO) => void;
}

/**
 * Inlined preview body — copy of the renderer used by the standalone
 * preview dialog. We duplicate it here rather than refactoring out
 * of {@code PreviewDialog.tsx} to keep the existing dialog contract
 * untouched while the detail dialog rolls out incrementally.
 */
function PreviewBody({
  previewUrl,
  contentType,
  datei,
  isLoading,
  isError,
  errorMessage,
  onDownload,
}: PreviewBodyProps) {
  if (Platform.OS !== "web") {
    return (
      <View className="flex-1 items-center justify-center gap-3 px-6 py-12">
        <Text className="text-center text-sm text-muted-foreground">
          {t(
            "dateien.preview.error.native",
            "Datei-Vorschau ist auf Native im MVP noch nicht verfügbar.",
          )}
        </Text>
      </View>
    );
  }
  if (isLoading) {
    return (
      <View className="flex-1 items-center justify-center py-16">
        <ActivityIndicator color="hsl(var(--muted-foreground))" />
      </View>
    );
  }
  if (isError || !previewUrl) {
    return (
      <View className="flex-1 items-center justify-center gap-3 px-6 py-12">
        <Text className="text-center text-sm text-muted-foreground">
          {errorMessage}
        </Text>
        {onDownload && (
          <Button variant="outline" onPress={() => onDownload(datei)}>
            <Text>{t("dateien.preview.download", "Herunterladen")}</Text>
          </Button>
        )}
      </View>
    );
  }
  const lower = contentType.toLowerCase();
  if (lower.startsWith("image/")) {
    const Img: React.ElementType = "img" as unknown as React.ElementType;
    return (
      <View className="flex-1 items-center justify-center bg-background">
        <Img
          src={previewUrl}
          alt={datei.name}
          style={{
            maxWidth: "100%",
            maxHeight: "100%",
            objectFit: "contain",
          }}
        />
      </View>
    );
  }
  if (lower.startsWith("video/")) {
    const Video: React.ElementType = "video" as unknown as React.ElementType;
    return (
      <View className="flex-1 items-center justify-center bg-black">
        <Video
          src={previewUrl}
          controls
          style={{ width: "100%", height: "100%", maxHeight: "100%" }}
        />
      </View>
    );
  }
  if (lower.startsWith("audio/")) {
    const Audio: React.ElementType = "audio" as unknown as React.ElementType;
    return (
      <View className="flex-1 items-center justify-center px-6 py-8">
        <Audio src={previewUrl} controls style={{ width: "100%" }} />
      </View>
    );
  }
  if (
    lower === "application/pdf" ||
    lower.startsWith("text/") ||
    lower === "application/json" ||
    lower === "application/xml" ||
    lower === "image/svg+xml"
  ) {
    const Iframe: React.ElementType = "iframe" as unknown as React.ElementType;
    return (
      <Iframe
        src={previewUrl}
        title={datei.name}
        style={{
          width: "100%",
          height: "100%",
          minHeight: "70vh",
          border: 0,
          background: "white",
        }}
      />
    );
  }
  return (
    <View className="flex-1 items-center justify-center gap-3 px-6 py-12">
      <Text className="text-center text-sm text-muted-foreground">
        {t(
          "dateien.preview.unsupported",
          "Vorschau für diesen Dateityp nicht verfügbar.",
        )}
      </Text>
      {onDownload && (
        <Button variant="outline" onPress={() => onDownload(datei)}>
          <Text>{t("dateien.preview.download", "Herunterladen")}</Text>
        </Button>
      )}
    </View>
  );
}
