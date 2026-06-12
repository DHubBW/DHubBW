/**
 * Modal dialog that previews a file inline in the browser.
 *
 * Wired to `GET /student/dateien/{dateiId}/preview` via
 * {@link useDateiPreview}. The endpoint streams the file with
 * `Content-Disposition: inline`, so embedding the resulting Object-URL
 * in a regular HTML element renders the file directly:
 *
 * - **PDF** / `application/pdf` → `<iframe>` (browser viewer plugin)
 * - **Image** (`image/*`)        → `<img>`
 * - **Video** (`video/*`)        → `<video controls>`
 * - **Audio** (`audio/*`)        → `<audio controls>`
 * - **Plain text / HTML / JSON / XML / SVG** → `<iframe>`
 * - **Anything else**            → "Vorschau nicht verfügbar"-Hinweis
 *   plus a Download-Button as the escape hatch.
 *
 * The dialog is web-only — on iOS/Android the parent surfaces a toast
 * and never opens the dialog. The Object-URL is revoked when the dialog
 * closes (or when the dateiId changes between two opens).
 *
 * Errors surface as toasts; the dialog stays open in the loading-state
 * and shows a fallback so the user can retry by reopening.
 */
import { Download as DownloadIcon } from "lucide-react-native";
import * as React from "react";
import { useEffect, useRef, useState } from "react";
import { ActivityIndicator, Platform, View } from "react-native";
import { toast } from "sonner-native";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Text } from "@/components/ui/text";
import {
  revokePreviewUrl,
  useDateiPreview,
  type FilePreviewError,
} from "@/hooks/useDateiPreview";
import type { DateiDTO } from "@/lib/api/types.gen";
import { t } from "@/lib/i18n/t";

export interface PreviewDialogProps {
  /** Controls dialog visibility. */
  open: boolean;
  /** Called when the dialog should open or close. */
  onOpenChange: (open: boolean) => void;
  /** File metadata (or `null` while no row is targeted). */
  datei: DateiDTO | null;
  /**
   * Optional download trigger. Surfaced as the fallback action when the
   * MIME type cannot be rendered inline. When omitted the fallback is a
   * plain "nicht verfügbar"-Hinweis.
   */
  onDownload?: (datei: DateiDTO) => void;
}

/**
 * Maps a {@link FilePreviewError} to a localised toast message.
 */
function _previewErrorMessage(err: FilePreviewError | null): string {
  if (!err) return t("dateien.preview.error.unknown", "Vorschau fehlgeschlagen.");
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
 * Inline preview modal. The body switches on the server-reported MIME
 * type so PDFs land in an `<iframe>`, images/videos/audio in their
 * native HTML element, and everything else falls back to a download
 * hint.
 *
 * @param props - {@link PreviewDialogProps}
 * @returns The dialog element.
 */
export function PreviewDialog({
  open,
  onOpenChange,
  datei,
  onDownload,
}: PreviewDialogProps) {
  const preview = useDateiPreview();
  const [url, setUrl] = useState<string | null>(null);
  const [contentType, setContentType] = useState<string>("");
  // Track the URL we created so we can revoke it on close / unmount even
  // when a new mutation overwrites local state.
  const activeUrlRef = useRef<string | null>(null);

  // Fire the mutation once whenever the dialog opens for a fresh file.
  // Reusing `useEffect` keeps the dialog purely controlled — parents only
  // toggle `open`, the dialog handles fetch + cleanup itself.
  useEffect(() => {
    if (!open || !datei) return;
    if (Platform.OS !== "web") {
      toast.error(_previewErrorMessage({
        code: "native-unsupported",
        message: "",
        name: "FilePreviewError",
      } as FilePreviewError));
      onOpenChange(false);
      return;
    }
    let cancelled = false;
    preview.mutate(
      { dateiId: datei.dateiID },
      {
        onSuccess: ({ url: nextUrl, contentType: nextType }) => {
          if (cancelled) {
            revokePreviewUrl(nextUrl);
            return;
          }
          // Free any previous preview URL still hanging around.
          revokePreviewUrl(activeUrlRef.current);
          activeUrlRef.current = nextUrl;
          setUrl(nextUrl);
          setContentType(nextType);
        },
        onError: (err) => {
          if (cancelled) return;
          toast.error(_previewErrorMessage(err));
        },
      },
    );
    return () => {
      cancelled = true;
    };
    // We deliberately depend on `datei?.dateiID` only — re-running on
    // mutation-state changes would re-trigger the fetch on every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, datei?.dateiID]);

  // Revoke + reset whenever the dialog closes so the next open starts
  // from a clean slate.
  useEffect(() => {
    if (!open) {
      revokePreviewUrl(activeUrlRef.current);
      activeUrlRef.current = null;
      setUrl(null);
      setContentType("");
    }
  }, [open]);

  // Final unmount cleanup.
  useEffect(() => {
    return () => {
      revokePreviewUrl(activeUrlRef.current);
      activeUrlRef.current = null;
    };
  }, []);

  const isLoading = preview.isPending && !url;
  const isError = preview.isError && !url;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        className="h-[92vh] w-[95vw] max-w-[95vw] sm:max-w-[95vw]"
        testID="preview-dialog"
      >
        <DialogHeader>
          <DialogTitle>{datei?.name ?? t("dateien.preview.title", "Vorschau")}</DialogTitle>
        </DialogHeader>
        <View className="min-h-[70vh] flex-1 overflow-hidden rounded-md border border-border bg-muted">
          {isLoading && (
            <View className="flex-1 items-center justify-center py-16">
              <ActivityIndicator color="hsl(var(--muted-foreground))" />
            </View>
          )}
          {isError && (
            <PreviewFallback
              datei={datei}
              onDownload={onDownload}
              message={_previewErrorMessage(preview.error)}
            />
          )}
          {!isLoading && !isError && url && datei && (
            <PreviewBody
              url={url}
              contentType={contentType}
              datei={datei}
              onDownload={onDownload}
            />
          )}
        </View>
      </DialogContent>
    </Dialog>
  );
}

interface PreviewBodyProps {
  url: string;
  contentType: string;
  datei: DateiDTO;
  onDownload?: (datei: DateiDTO) => void;
}

/**
 * Picks the right inline renderer for the given MIME type. The body
 * always fills the dialog viewport so PDFs/videos use the available
 * space without manual sizing.
 */
function PreviewBody({ url, contentType, datei, onDownload }: PreviewBodyProps) {
  const lower = contentType.toLowerCase();
  // The native HTML elements only render on Web — `View`-children on
  // Native would try to render React DOM nodes, so we hard-guard. The
  // parent already toasts + closes the dialog on Native; this branch is
  // a belt-and-braces fallback.
  if (Platform.OS !== "web") {
    return (
      <PreviewFallback
        datei={datei}
        onDownload={onDownload}
        message={t(
          "dateien.preview.error.native",
          "Datei-Vorschau ist auf Native im MVP noch nicht verfügbar.",
        )}
      />
    );
  }

  if (lower.startsWith("image/")) {
    const Img: React.ElementType = "img" as unknown as React.ElementType;
    return (
      <View className="flex-1 items-center justify-center bg-background">
        <Img
          src={url}
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
          src={url}
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
        <Audio src={url} controls style={{ width: "100%" }} />
      </View>
    );
  }

  // PDF, SVG, plain text, JSON, XML, HTML — browser handles all of them
  // natively in an iframe. Anything else falls through to the fallback.
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
        src={url}
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
    <PreviewFallback
      datei={datei}
      onDownload={onDownload}
      message={t(
        "dateien.preview.unsupported",
        "Vorschau für diesen Dateityp nicht verfügbar.",
      )}
    />
  );
}

interface PreviewFallbackProps {
  datei: DateiDTO | null;
  onDownload?: (datei: DateiDTO) => void;
  message: string;
}

/**
 * Hint shown when the MIME type cannot be rendered inline (or when the
 * fetch failed). Offers the download as the natural escape hatch.
 */
function PreviewFallback({ datei, onDownload, message }: PreviewFallbackProps) {
  return (
    <View className="flex-1 items-center justify-center gap-3 px-6 py-12">
      <Text className="text-center text-sm text-muted-foreground">
        {message}
      </Text>
      {datei && onDownload && (
        <Button
          variant="outline"
          onPress={() => onDownload(datei)}
          testID="preview-fallback-download"
        >
          <View className="flex-row items-center gap-2">
            <DownloadIcon size={14} color="hsl(var(--foreground))" />
            <Text>{t("dateien.preview.download", "Herunterladen")}</Text>
          </View>
        </Button>
      )}
    </View>
  );
}
