/**
 * Web-only drag-and-drop wrapper around the file-list area.
 *
 * Active only when a folder is selected (`ueberordnerId !== null`):
 *
 * - Native (iOS/Android): renders its children unchanged.
 * - Web with no folder selected: renders the children unchanged so
 *   the user does not see a misleading drop hint over "Alle Dateien"
 *   (the synthetic root has no folder UUID to upload into).
 * - Web with a folder selected: layers a transparent
 *   {@code <div onDragEnter onDragOver onDrop>} on top of the
 *   children. While the user drags a file over the zone an inviting
 *   tinted overlay with a "Dateien hier ablegen" hint appears; on
 *   drop the picked file is forwarded to {@link useFileUpload} which
 *   reuses the same security-validated path the toolbar's
 *   {@link UploadButton} uses.
 *
 * The wrapper does not capture clicks — pointer events on the file
 * list, the Datei rows and their action buttons keep working. The
 * drag overlay only mounts while {@code dragActive === true} and is
 * marked as {@code pointer-events: none} when not active so it never
 * intercepts mouse-overs.
 */
import { Upload } from "lucide-react-native";
import * as React from "react";
import { useCallback, useEffect, useRef, useState } from "react";
import { Platform, View } from "react-native";
import { toast } from "sonner-native";

import { Text } from "@/components/ui/text";
import {
  FileUploadError,
  useFileUpload,
  type UploadFileLike,
} from "@/hooks/useFileUpload";
import { t } from "@/lib/i18n/t";

export interface DateienDropZoneProps {
  /**
   * UUID of the folder to upload into, or {@code null} when no folder
   * is selected. The drop overlay is disabled when {@code null}.
   */
  ueberordnerId: string | null;
  children: React.ReactNode;
}

/**
 * Maps a {@link FileUploadError} to a German toast string. Mirrors
 * the logic of {@code UploadButton._toastForError} but is duplicated
 * here so the drop zone stays self-contained.
 */
function _dropToast(err: FileUploadError): string {
  if (err.serverMessage) return err.serverMessage;
  switch (err.code) {
    case "too-large":
      if (err.actualMb !== undefined && err.limitMb !== undefined) {
        return `Datei ist ${err.actualMb} MB groß – das Limit liegt bei ${err.limitMb} MB.`;
      }
      return "Datei ist zu groß für den Server.";
    case "duplicate-name":
      return "Eine Datei mit diesem Namen existiert bereits in diesem Ordner.";
    case "invalid-type":
      return "Dateityp ist aus Sicherheitsgründen nicht erlaubt.";
    case "empty-file":
      return "Datei ist leer.";
    case "invalid-name":
      return "Dateiname enthält ungültige Zeichen.";
    case "forbidden":
      return "Keine Berechtigung für diesen Ordner.";
    case "not-found":
      return "Ordner nicht gefunden.";
    case "storage-full":
      return "Speicherkontingent voll.";
    case "network":
      return "Netzwerkfehler – Server nicht erreichbar";
    default:
      return "Upload fehlgeschlagen.";
  }
}

/**
 * Drag-and-drop wrapper. Web-only — passes through unchanged on
 * Native and when no folder is selected.
 *
 * @param props - {@link DateienDropZoneProps}
 * @returns The wrapped children plus the optional drop overlay.
 */
export function DateienDropZone({
  ueberordnerId,
  children,
}: DateienDropZoneProps) {
  const upload = useFileUpload();
  const [dragActive, setDragActive] = useState(false);
  // Use a counter for nested drag-enter / drag-leave events: every
  // child element fires its own enter / leave when the cursor crosses
  // its bounds, so a naive `setDragActive(false)` on leave would
  // flicker. Track depth instead.
  const dragDepth = useRef(0);

  const isWeb = Platform.OS === "web";
  const enabled = isWeb && ueberordnerId !== null;

  const handleFiles = useCallback(
    (files: FileList) => {
      if (!enabled || ueberordnerId === null) return;
      // Multi-file drop: iterate sequentially so per-file toasts stay
      // ordered and the storage-cap check fires once per file.
      for (let i = 0; i < files.length; i += 1) {
        const file = files.item(i);
        if (!file) continue;
        upload.mutate(
          {
            file: file as unknown as UploadFileLike,
            ueberordnerId,
            name: file.name,
          },
          {
            onSuccess: () => {
              toast.success(
                t("dateien.upload.success", "Datei hochgeladen"),
              );
            },
            onError: (err) => {
              toast.error(_dropToast(err));
            },
          },
        );
      }
    },
    [enabled, ueberordnerId, upload],
  );

  // When the folder selection changes mid-drag (e.g. user navigates
  // via keyboard), reset the overlay so it doesn't get stuck on.
  useEffect(() => {
    if (!enabled) {
      dragDepth.current = 0;
      setDragActive(false);
    }
  }, [enabled]);

  if (!enabled) {
    return <>{children}</>;
  }

  // Hand-cast a string-tag literal so React DOM accepts the typed
  // event handlers — RN's `View` does not expose drag events. Same
  // trick the upload-spike screen uses for the file `<input>`.
  const Div: React.ElementType =
    "div" as unknown as React.ElementType;

  return (
    <Div
      onDragEnter={(ev: React.DragEvent<HTMLDivElement>) => {
        if (!ev.dataTransfer.types.includes("Files")) return;
        ev.preventDefault();
        dragDepth.current += 1;
        setDragActive(true);
      }}
      onDragOver={(ev: React.DragEvent<HTMLDivElement>) => {
        if (!ev.dataTransfer.types.includes("Files")) return;
        // Required for `drop` to fire on the same target.
        ev.preventDefault();
        ev.dataTransfer.dropEffect = "copy";
      }}
      onDragLeave={(ev: React.DragEvent<HTMLDivElement>) => {
        if (!ev.dataTransfer.types.includes("Files")) return;
        dragDepth.current = Math.max(0, dragDepth.current - 1);
        if (dragDepth.current === 0) setDragActive(false);
      }}
      onDrop={(ev: React.DragEvent<HTMLDivElement>) => {
        if (!ev.dataTransfer.types.includes("Files")) return;
        ev.preventDefault();
        dragDepth.current = 0;
        setDragActive(false);
        handleFiles(ev.dataTransfer.files);
      }}
      style={{
        // The drop zone must scope itself to the list area (not the
        // whole page). Position-relative + flex grows it to fit the
        // available height inside its parent, while the overlay below
        // uses `absolute inset-0` to cover only this same box.
        position: "relative",
        display: "flex",
        flexDirection: "column",
        flex: 1,
        minHeight: 0,
      }}
    >
      {children}
      {dragActive && (
        <View
          pointerEvents="none"
          className="absolute inset-0 z-50 items-center justify-center bg-primary/15"
        >
          <View className="rounded-2xl border-2 border-dashed border-primary bg-background/95 px-8 py-6">
            <View className="items-center gap-2">
              <Upload size={32} color="hsl(var(--primary))" />
              <Text className="text-base font-semibold text-foreground">
                {t(
                  "dateien.dropzone.heading",
                  "Dateien hier ablegen",
                )}
              </Text>
              <Text className="text-xs text-muted-foreground">
                {t(
                  "dateien.dropzone.hint",
                  "Loslassen, um in den ausgewählten Ordner hochzuladen",
                )}
              </Text>
            </View>
          </View>
        </View>
      )}
    </Div>
  );
}
