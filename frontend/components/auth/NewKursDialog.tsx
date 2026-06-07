import { useEffect, useRef, useState } from 'react';
import { Pressable, Text, TextInput, View } from 'react-native';
import { toast } from 'sonner-native';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { ApiError } from '@/lib/api/errors';
import { fetchClient } from '@/lib/api/fetchClient';
import type {
  KursErstellungsAnfrageRequestDTO,
} from '@/lib/api/types.gen';
import { useCookieConsent } from '@/lib/consent/useCookieConsent';

/**
 * Length and pattern constants for the new-course form.
 *
 * Length bounds are enforced client-side to give immediate feedback; the
 * backend has only `@NotBlank` on `kursName` / `jahrgang` and no length
 * constraint on `additionalNotes`. The form does not gate submit on the
 * Jahrgang format — the backend accepts free text and the UI hint just
 * suggests the cohort year (`2024`).
 *
 * Kursname pattern matches the backend regex in `KursServiceImpl.java`
 * (`[A-Z]{2,4}[0-9]{2}[A-Z][0-9]`, e.g. `TINF24B2`, `WWI24B1`). The form
 * surfaces a soft inline warning when the value does not match — submit
 * is still allowed (the backend would respond with 422 and the toast
 * forwards the backend message verbatim) so that legitimate edge cases
 * the regex misses can still pass through.
 */
const KURSNAME_MIN = 3;
const KURSNAME_MAX = 100;
const NOTES_MAX = 500;
const KURSNAME_PATTERN_RE = /^[A-Z]{2,4}\d{2}[A-Z]\d$/;

/**
 * Props for {@link NewKursDialog}.
 */
export interface NewKursDialogProps {
  /** Controls dialog visibility. */
  open: boolean;
  /** Called when the dialog should open or close (cancel, X, backdrop). */
  onOpenChange: (open: boolean) => void;
  /**
   * Invoked once the backend has accepted the creation request (HTTP 2xx).
   * The parent typically navigates away — see `RegisterStep2`. The dialog
   * itself does not navigate.
   */
  onCreated: () => void;
}

/**
 * Modal form to submit a course-creation request during registration.
 *
 * Posts {@link KursErstellungsAnfrageRequestDTO} to
 * `POST /student/kurse/erstellen`. On HTTP 2xx fires `onCreated` after a
 * success toast; on 409 the dialog stays open so the user can adjust the
 * input. 422 surfaces the backend-supplied message verbatim and likewise
 * keeps the dialog open. Network errors (status `0`) and other non-2xx
 * responses surface a generic toast; the dialog also stays open so the
 * user can retry.
 *
 * The dialog is uncontrolled with respect to its inputs — values reset
 * whenever `open` flips back to `false`. This keeps stale data out of the
 * form when the user reopens the dialog after cancelling.
 *
 * @param props - {@link NewKursDialogProps}
 * @returns The dialog element.
 */
export function NewKursDialog({
  open,
  onOpenChange,
  onCreated,
}: NewKursDialogProps) {
  const { consent } = useCookieConsent();
  const [kursName, setKursName] = useState('');
  const [jahrgang, setJahrgang] = useState('');
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  // Re-entry lock against rapid double-taps — see LoginScreen.
  const submittingRef = useRef(false);
  // Tracks whether the dialog instance is still mounted. The submit
  // continuation may resolve after the parent unmounts the dialog (e.g.
  // user pressed Skip while the request was in flight); a `setSubmitting`
  // call after unmount would log a warning and is plain wasteful.
  const mountedRef = useRef(true);
  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  // Reset all form state when the dialog closes — see component-level JSDoc.
  useEffect(() => {
    if (!open) {
      setKursName('');
      setJahrgang('');
      setNotes('');
      setSubmitting(false);
    }
  }, [open]);

  const trimmedName = kursName.trim();
  const trimmedJahrgang = jahrgang.trim();
  const trimmedNotes = notes.trim();
  const nameLengthOk =
    trimmedName.length >= KURSNAME_MIN && trimmedName.length <= KURSNAME_MAX;
  const jahrgangOk = trimmedJahrgang.length > 0;
  const notesOk = trimmedNotes.length <= NOTES_MAX;
  const canSubmit = nameLengthOk && jahrgangOk && notesOk && !submitting;
  // Submit is gated on `consent !== 'rejected'` — kept off `canSubmit` so
  // the early-return in `handleSubmit` does not type-narrow the consent
  // check away, and folded into the button's disabled state below.
  const cookiesBlocked = consent === 'rejected';

  async function handleSubmit() {
    if (!canSubmit) return;
    if (submittingRef.current) return;
    if (consent === 'rejected') return;
    submittingRef.current = true;
    setSubmitting(true);
    const body: KursErstellungsAnfrageRequestDTO = {
      kursName: trimmedName,
      jahrgang: trimmedJahrgang,
      // Send `null` for empty notes — backend treats the field as optional;
      // an empty-string would needlessly hit `@NotBlank`-style validators on
      // future field-level constraints.
      additionalNotes: trimmedNotes.length > 0 ? trimmedNotes : null,
    };
    try {
      await fetchClient('/student/kurse/erstellen', {
        method: 'POST',
        body: JSON.stringify(body),
      });
      toast.success('Kurs-Erstellungsanfrage gesendet');
      onCreated();
      onOpenChange(false);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 409) {
          toast.error('Bereits offene Anfrage');
        } else if (err.status === 422) {
          // Backend liefert die Begründung im message-Feld.
          toast.error(err.message);
        } else if (err.status === 0) {
          toast.error('Netzwerkfehler. Bitte erneut versuchen.');
        } else {
          toast.error('Kurs-Anlage fehlgeschlagen.');
        }
      } else {
        toast.error('Kurs-Anlage fehlgeschlagen.');
      }
    } finally {
      submittingRef.current = false;
      if (mountedRef.current) {
        setSubmitting(false);
      }
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Neuen Kurs anlegen</DialogTitle>
          <DialogDescription>
            Sende eine Anfrage zur Anlage eines neuen Kurses. Die Freigabe
            erfolgt durch einen System Admin.
          </DialogDescription>
        </DialogHeader>

        <View className="gap-4">
          <View>
            <Label nativeID="new-kurs-name-label" className="mb-1.5">
              Kursname
            </Label>
            <Input
              accessibilityLabel="Kursname"
              aria-labelledby="new-kurs-name-label"
              value={kursName}
              onChangeText={setKursName}
              autoCapitalize="characters"
              autoCorrect={false}
              placeholder="z. B. TINF24B2"
              editable={!submitting}
            />
            <Text className="mt-1 text-xs text-muted-foreground">
              {KURSNAME_MIN}–{KURSNAME_MAX} Zeichen, Format z. B.{' '}
              <Text className="font-mono">TINF24B2</Text> (zwei bis vier
              Buchstaben, zweistelliges Jahr, ein Buchstabe, eine Ziffer).
            </Text>
            {trimmedName.length > 0 && !KURSNAME_PATTERN_RE.test(trimmedName) && (
              <Text className="mt-1 text-xs text-amber-600">
                Hinweis: Der Name folgt nicht dem üblichen Format. Du kannst
                trotzdem absenden — der System Admin entscheidet final.
              </Text>
            )}
          </View>

          <View>
            <Label nativeID="new-kurs-jahrgang-label" className="mb-1.5">
              Jahrgang
            </Label>
            <Input
              accessibilityLabel="Jahrgang"
              aria-labelledby="new-kurs-jahrgang-label"
              value={jahrgang}
              onChangeText={setJahrgang}
              autoCapitalize="none"
              autoCorrect={false}
              keyboardType="number-pad"
              inputMode="numeric"
              placeholder="z. B. 2024"
              editable={!submitting}
            />
            <Text className="mt-1 text-xs text-muted-foreground">
              Empfohlenes Format: vierstelliges Jahr (Freitext erlaubt).
            </Text>
          </View>

          <View>
            <Label nativeID="new-kurs-notes-label" className="mb-1.5">
              Notizen (optional)
            </Label>
            <TextInput
              accessibilityLabel="Notizen"
              aria-labelledby="new-kurs-notes-label"
              value={notes}
              onChangeText={setNotes}
              multiline
              numberOfLines={3}
              maxLength={NOTES_MAX}
              editable={!submitting}
              placeholder="Hinweise an die System-Admins"
              className="dark:bg-input/30 border-input bg-background text-foreground min-h-[80px] w-full rounded-md border px-3 py-2 text-base"
            />
            <Text className="mt-1 text-xs text-muted-foreground">
              {trimmedNotes.length}/{NOTES_MAX}
            </Text>
          </View>
        </View>

        <DialogFooter>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Abbrechen"
            onPress={() => onOpenChange(false)}
            disabled={submitting}
            className="h-10 items-center justify-center rounded-md border border-border px-4 sm:h-9"
          >
            <Text className="text-sm font-medium">Abbrechen</Text>
          </Pressable>
          <Button
            accessibilityLabel="Anfrage senden"
            onPress={handleSubmit}
            disabled={!canSubmit || cookiesBlocked}
          >
            <Text className="text-sm font-medium text-primary-foreground">
              {submitting ? 'Sende…' : 'Anfrage senden'}
            </Text>
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
