/**
 * Modal dialog for the "Ordner erstellen" action.
 *
 * Posts {@link CreateOrdnerRequestDTO} to `PUT /student/ordner` via
 * {@link useOrdnerCreate}. The dialog covers all four fields the backend
 * expects:
 *
 * - `name` — text input, trimmed before submit, non-blank required.
 * - `isPublic` — boolean toggle (Public / Privat).
 * - `farbe` — one of {@link OrdnerFarbe}, picked from four colour swatches
 *   sourced from {@link folderColorClasses}.
 * - `ueberOrdnerId` — passed in via prop. `null` creates a root-level
 *   folder; a UUID creates a sub-folder under that parent.
 *
 * Errors surface as toasts mapped from the {@link ApiError} status —
 * dialog stays open on failure so the user can retry. On success the
 * caller's `onOpenChange(false)` runs after a success toast.
 */
import { useEffect, useRef, useState } from "react";
import { ActivityIndicator, Pressable, View } from "react-native";
import { toast } from "sonner-native";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Text } from "@/components/ui/text";
import { useOrdnerCreate } from "@/hooks/useOrdnerCreate";
import { ApiError } from "@/lib/api/errors";
import type { OrdnerFarbe } from "@/lib/api/types.gen";
import { t } from "@/lib/i18n/t";
import { folderColorClasses } from "@/lib/theme/folderColors";
import { cn } from "@/lib/utils";

/** Order of the colour swatches — left-to-right reading order. */
const FARBE_ORDER: readonly OrdnerFarbe[] = [
  "DEFAULT",
  "RED",
  "BLUE",
  "GREEN",
] as const;

/** Human-readable label per colour, used as the swatch's a11y label. */
const FARBE_LABEL: Record<OrdnerFarbe, string> = {
  DEFAULT: "Grau",
  RED: "Rot",
  BLUE: "Blau",
  GREEN: "Grün",
};

export interface CreateOrdnerDialogProps {
  /** Controls dialog visibility. */
  open: boolean;
  /** Called when the dialog should open or close. */
  onOpenChange: (open: boolean) => void;
  /**
   * Parent folder UUID for the new folder, or `null` to create a root
   * folder. Forwarded verbatim to the create mutation.
   */
  ueberOrdnerId: string | null;
}

/**
 * Maps an {@link ApiError} from the create endpoint to the toast text
 * shown to the user.
 *
 * @param err - The thrown error from the mutation's onError callback.
 * @returns The localised toast message.
 */
function _createErrorMessage(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.status === 403) {
      return t(
        "ordner.create.error.forbidden",
        "Keine Berechtigung zum Anlegen eines Ordners.",
      );
    }
    if (err.status === 507) {
      return t(
        "ordner.create.error.storageFull",
        "Speicherkontingent voll – Ordner-Anlage abgelehnt.",
      );
    }
    if (err.status === 0 && err.message.includes("404")) {
      return t(
        "ordner.create.error.notFound",
        "Übergeordneter Ordner nicht gefunden.",
      );
    }
    if (err.status === 0) {
      return t("error.network", "Netzwerkfehler – Server nicht erreichbar");
    }
  }
  return t("ordner.create.error.unknown", "Ordner-Anlage fehlgeschlagen.");
}

/**
 * Renders the create-folder dialog.
 *
 * @param props - {@link CreateOrdnerDialogProps}
 * @returns The dialog element.
 */
export function CreateOrdnerDialog({
  open,
  onOpenChange,
  ueberOrdnerId,
}: CreateOrdnerDialogProps) {
  const create = useOrdnerCreate();
  const [name, setName] = useState("");
  const [isPublic, setIsPublic] = useState(true);
  const [farbe, setFarbe] = useState<OrdnerFarbe>("DEFAULT");
  // Re-entry lock against rapid double-taps — see NewKursDialog.
  const submittingRef = useRef(false);
  // Avoid `setSubmitting` on an already-unmounted dialog when the user
  // closes the parent screen mid-flight.
  const mountedRef = useRef(true);
  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  // Reset form state whenever the dialog closes — keeps stale input out
  // of the next open. Mirror of the NewKursDialog pattern.
  useEffect(() => {
    if (!open) {
      setName("");
      setIsPublic(true);
      setFarbe("DEFAULT");
      submittingRef.current = false;
    }
  }, [open]);

  const trimmed = name.trim();
  const canSubmit = !create.isPending && trimmed.length > 0;

  function handleOpenChange(nextOpen: boolean) {
    if (create.isPending) return;
    onOpenChange(nextOpen);
  }

  async function handleSubmit() {
    if (!canSubmit) return;
    if (submittingRef.current) return;
    submittingRef.current = true;
    try {
      await create.mutateAsync({
        ueberOrdnerId,
        name: trimmed,
        isPublic,
        farbe,
      });
      toast.success(t("ordner.create.success", "Ordner angelegt"));
      onOpenChange(false);
    } catch (err) {
      toast.error(_createErrorMessage(err));
    } finally {
      if (mountedRef.current) {
        submittingRef.current = false;
      }
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {t("ordner.create.title", "Ordner erstellen")}
          </DialogTitle>
          <DialogDescription>
            {ueberOrdnerId === null
              ? t(
                  "ordner.create.description.root",
                  "Lege einen neuen Ordner auf oberster Ebene an.",
                )
              : t(
                  "ordner.create.description.child",
                  "Lege einen neuen Unterordner an.",
                )}
          </DialogDescription>
        </DialogHeader>

        <View className="gap-4 py-2">
          {/* Name */}
          <View className="gap-1.5">
            <Label nativeID="create-ordner-name-label">
              {t("ordner.create.label.name", "Name")}
            </Label>
            <Input
              aria-labelledby="create-ordner-name-label"
              testID="create-ordner-name-input"
              value={name}
              onChangeText={setName}
              autoCapitalize="none"
              autoCorrect={false}
              editable={!create.isPending}
              onSubmitEditing={handleSubmit}
            />
          </View>

          {/* Sichtbarkeit */}
          <View className="gap-1.5">
            <Label>{t("ordner.create.label.visibility", "Sichtbarkeit")}</Label>
            <View className="flex-row gap-2">
              <VisibilityChoice
                testID="create-ordner-public-toggle"
                label={t("ordner.create.visibility.public", "Öffentlich")}
                selected={isPublic}
                disabled={create.isPending}
                onPress={() => setIsPublic(true)}
              />
              <VisibilityChoice
                testID="create-ordner-private-toggle"
                label={t("ordner.create.visibility.private", "Privat")}
                selected={!isPublic}
                disabled={create.isPending}
                onPress={() => setIsPublic(false)}
              />
            </View>
          </View>

          {/* Farbe */}
          <View className="gap-1.5">
            <Label>{t("ordner.create.label.color", "Farbe")}</Label>
            <View className="flex-row gap-2">
              {FARBE_ORDER.map((value) => (
                <ColorSwatch
                  key={value}
                  value={value}
                  selected={farbe === value}
                  disabled={create.isPending}
                  onPress={() => setFarbe(value)}
                />
              ))}
            </View>
          </View>
        </View>

        <DialogFooter>
          <Button
            testID="create-ordner-cancel"
            variant="outline"
            onPress={() => handleOpenChange(false)}
            disabled={create.isPending}
          >
            <Text>{t("confirm.cancel", "Abbrechen")}</Text>
          </Button>
          <Button
            testID="create-ordner-confirm"
            onPress={handleSubmit}
            disabled={!canSubmit}
          >
            {create.isPending ? (
              <ActivityIndicator
                testID="create-ordner-spinner"
                size="small"
                color="white"
              />
            ) : (
              <Text>{t("ordner.create.confirm", "Anlegen")}</Text>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

interface VisibilityChoiceProps {
  testID: string;
  label: string;
  selected: boolean;
  disabled: boolean;
  onPress: () => void;
}

/**
 * Two-state pill toggle used for the Public/Privat selector. Stays inline
 * here because no other call site needs it and the component-level test
 * already covers the behaviour through `selected` + press events.
 *
 * @param props - {@link VisibilityChoiceProps}
 * @returns The pressable choice element.
 */
function VisibilityChoice({
  testID,
  label,
  selected,
  disabled,
  onPress,
}: VisibilityChoiceProps) {
  return (
    <Pressable
      testID={testID}
      accessibilityRole="button"
      accessibilityState={{ selected, disabled }}
      onPress={onPress}
      disabled={disabled}
      className={cn(
        "rounded-md border px-3 py-2",
        selected
          ? "border-primary bg-primary/10"
          : "border-border bg-background",
        disabled && "opacity-50",
      )}
    >
      <Text
        className={cn(
          "text-sm",
          selected
            ? "font-semibold text-primary"
            : "text-foreground",
        )}
      >
        {label}
      </Text>
    </Pressable>
  );
}

interface ColorSwatchProps {
  value: OrdnerFarbe;
  selected: boolean;
  disabled: boolean;
  onPress: () => void;
}

/**
 * 40×40 colour bubble with a selection border. The bubble's background
 * comes from {@link folderColorClasses} so the swatch palette stays in
 * sync with the rendered icon colours in {@link OrdnerTree}.
 *
 * @param props - {@link ColorSwatchProps}
 * @returns The pressable swatch element.
 */
function ColorSwatch({
  value,
  selected,
  disabled,
  onPress,
}: ColorSwatchProps) {
  const classes = folderColorClasses(value);
  return (
    <Pressable
      testID={`create-ordner-color-${value.toLowerCase()}`}
      accessibilityRole="button"
      accessibilityLabel={FARBE_LABEL[value]}
      accessibilityState={{ selected, disabled }}
      onPress={onPress}
      disabled={disabled}
      className={cn(
        "h-10 w-10 items-center justify-center rounded-full border-2",
        selected ? "border-primary" : "border-transparent",
        disabled && "opacity-50",
      )}
    >
      <View className={cn("h-7 w-7 rounded-full", classes.bg)} />
    </Pressable>
  );
}
