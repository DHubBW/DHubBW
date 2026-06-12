/**
 * Upload button rendered in the {@link DateienToolbar}'s `actions` slot.
 *
 * Web is the only target for the MVP — the button hosts a hidden
 * `<input type="file">` and forwards the picked file to
 * {@link useFileUpload}. The toast surface maps every
 * {@link UploadErrorCode} to a localised message; on success a
 * "Datei hochgeladen" toast fires and the per-folder list query is
 * invalidated automatically by the upload hook (no extra plumbing here).
 *
 * Native is intentionally out of MVP scope — `expo-document-picker` would
 * be a new dependency and the demo target is web-only. The button is
 * still rendered so the toolbar layout is identical, but it surfaces a
 * "Demnächst auf Native" toast on press.
 *
 * The button is disabled when no folder is selected (`ueberordnerId ===
 * null`), because the backend's `@RequestParam("ueberordnerId")` is
 * required: the synthetic "Alle Dateien" root has no folder UUID to
 * upload into.
 */
import { Upload } from "lucide-react-native";
import { useRef } from "react";
import type * as React from "react";
import { Platform, View } from "react-native";
import { toast } from "sonner-native";

import { Button } from "@/components/ui/button";
import { Text } from "@/components/ui/text";
import {
  FileUploadError,
  type UploadFileLike,
  useFileUpload,
} from "@/hooks/useFileUpload";
import { t } from "@/lib/i18n/t";

export interface UploadButtonProps {
  /**
   * UUID of the folder the file should be uploaded into, or `null` for
   * the synthetic "Alle Dateien" root view (button is disabled).
   */
  ueberordnerId: string | null;
}

/**
 * Maps a {@link FileUploadError} to the toast message that should be
 * shown to the user.
 *
 * <p>For every code the helper first prefers the backend's own
 * {@code serverMessage} so the user sees the precise reason
 * ("Dateityp nicht erlaubt: .exe", "Datei zu groß: 67.2 MB. Limit:
 *  50.0 MB.", "Eine Datei mit diesem Namen existiert bereits …").
 * The localised fall-back text is only used when no body text is
 * available (e.g. on a network error before the server even
 * answered).</p>
 *
 * @param err - The error rejected by the upload mutation.
 * @returns The localised toast message.
 */
function _toastForError(err: FileUploadError): string {
  if (err.serverMessage) return err.serverMessage;
  switch (err.code) {
    case "too-large":
      if (err.actualMb !== undefined && err.limitMb !== undefined) {
        return t(
          "dateien.upload.error.tooLargeWithSize",
          "Datei ist {actual} MB groß – das Limit liegt bei {limit} MB.",
        )
          .replace("{actual}", String(err.actualMb))
          .replace("{limit}", String(err.limitMb));
      }
      if (err.limitMb !== undefined) {
        return t(
          "dateien.upload.error.tooLargeWithLimit",
          "Datei ist zu groß – das Limit liegt bei {limit} MB.",
        ).replace("{limit}", String(err.limitMb));
      }
      return t(
        "dateien.upload.error.tooLarge",
        "Datei ist zu groß für den Server.",
      );
    case "duplicate-name":
      return t(
        "dateien.upload.error.duplicateName",
        "Eine Datei mit diesem Namen existiert bereits in diesem Ordner.",
      );
    case "invalid-type":
      return t(
        "dateien.upload.error.invalidType",
        "Dateityp ist aus Sicherheitsgründen nicht erlaubt.",
      );
    case "empty-file":
      return t(
        "dateien.upload.error.emptyFile",
        "Datei ist leer.",
      );
    case "invalid-name":
      return t(
        "dateien.upload.error.invalidName",
        "Dateiname enthält ungültige Zeichen.",
      );
    case "forbidden":
      return t(
        "dateien.upload.error.forbidden",
        "Keine Berechtigung für diesen Ordner.",
      );
    case "not-found":
      return t(
        "dateien.upload.error.notFound",
        "Ordner nicht gefunden.",
      );
    case "storage-full":
      return t(
        "dateien.upload.error.storageFull",
        "Speicherkontingent voll.",
      );
    case "network":
      return t(
        "error.network",
        "Netzwerkfehler – Server nicht erreichbar",
      );
    default:
      return t(
        "dateien.upload.error.unknown",
        "Upload fehlgeschlagen.",
      );
  }
}

/**
 * Renders the "Hochladen" button. Wires the Web `<input type="file">`
 * picker to {@link useFileUpload}.
 *
 * @param props - {@link UploadButtonProps}
 * @returns The button element.
 */
export function UploadButton({ ueberordnerId }: UploadButtonProps) {
  const upload = useFileUpload();
  const inputRef = useRef<HTMLInputElement | null>(null);

  const isWeb = Platform.OS === "web";
  const disabled =
    ueberordnerId === null || upload.isPending || !isWeb ? true : false;

  function openPicker() {
    if (!isWeb) {
      toast(
        t(
          "dateien.upload.nativeStub",
          "Datei-Upload ist auf Native im MVP noch nicht verfügbar.",
        ),
      );
      return;
    }
    inputRef.current?.click();
  }

  function handleFileChange(file: UploadFileLike, name: string) {
    if (ueberordnerId === null) return;
    upload.mutate(
      { file, ueberordnerId, name },
      {
        onSuccess: () => {
          toast.success(
            t("dateien.upload.success", "Datei hochgeladen"),
          );
        },
        onError: (err) => {
          toast.error(_toastForError(err));
        },
      },
    );
  }

  return (
    <View>
      <Button
        size="sm"
        onPress={openPicker}
        disabled={disabled || upload.isPending}
        accessibilityLabel={t("dateien.upload.button", "Hochladen")}
        testID="dateien-upload-button"
      >
        <Upload size={14} color="hsl(var(--primary-foreground))" />
        <Text>
          {upload.isPending
            ? t("dateien.upload.pending", "Lädt …")
            : t("dateien.upload.button", "Hochladen")}
        </Text>
      </Button>
      {isWeb && <HiddenWebInput onPicked={handleFileChange} inputRef={inputRef} />}
    </View>
  );
}

/**
 * Hidden Web-only `<input type="file">`. React Native has no `<input>`
 * tag, so we hand-cast a string-tag literal as a typed component — same
 * trick the dev upload-spike uses (see `app/(app)/_dev/upload-spike.tsx`).
 * The element stays in the DOM (zero-size, off-screen) so `inputRef.click()`
 * always finds it. Wrapped in a {@link View} with a `testID` so unit tests
 * can drive the picker by firing a synthetic `change` event on the input
 * directly via the wrapper's first child.
 *
 * @param props - Slot props.
 * @param props.onPicked - Called when the user picks a file.
 * @param props.inputRef - Imperative ref the parent uses to trigger
 *   `click()` on the hidden input.
 * @returns The wrapped hidden input element.
 */
function HiddenWebInput({
  onPicked,
  inputRef,
}: {
  onPicked: (file: UploadFileLike, name: string) => void;
  inputRef: React.MutableRefObject<HTMLInputElement | null>;
}) {
  const HtmlInput = "input" as unknown as React.ComponentType<{
    type: string;
    style: React.CSSProperties;
    ref: React.Ref<HTMLInputElement>;
    onChange: (ev: { target: { files: FileList | null; value: string } }) => void;
  }>;
  return (
    <View testID="dateien-upload-input-wrapper">
      <HtmlInput
        type="file"
        style={{ display: "none" }}
        ref={inputRef}
        onChange={(ev) => {
          const target = ev?.target;
          const file = target?.files?.[0];
          if (!file) return;
          onPicked(file as unknown as UploadFileLike, file.name);
          // Reset so the same file can be picked twice in a row.
          if (target) target.value = "";
        }}
      />
    </View>
  );
}
