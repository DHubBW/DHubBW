import { Link, router, useLocalSearchParams } from 'expo-router';
import { Eye, EyeOff } from 'lucide-react-native';
import { useCallback, useEffect, useRef, useState } from 'react';
import {
  KeyboardAvoidingView,
  Platform,
  Pressable,
  Text,
  View,
} from 'react-native';
import { toast } from 'sonner-native';

import { CookieBanner } from '@/components/CookieBanner';
import { Footer } from '@/components/layout/Footer';
import { Button } from '@/components/ui/button';
import { Icon } from '@/components/ui/icon';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { ApiError } from '@/lib/api/errors';
import { useAuth } from '@/lib/auth/AuthContext';
import { resolveReturnTo } from '@/lib/auth/resolveReturnTo';
import {
  getMailErrorMessage,
  isValidDhbwMail,
} from '@/lib/auth/validators';
import { useExpiredReason } from '@/lib/auth/useExpiredReason';
import { useCookieConsent } from '@/lib/consent/useCookieConsent';

/** Hard cap on the password input — guards the form against accidental
 *  multi-megabyte paste loads while leaving plenty of room for any sane
 *  passphrase. The backend would reject longer values with 422 anyway. */
const PASSWORD_MAX_LENGTH = 256;

/**
 * Login screen — DHBW-Mail + Password against `POST /public/auth/login`.
 *
 * Implementation notes:
 *
 * - **Form library:** plain `useState` + `lib/auth/validators.ts`. No
 *   `react-hook-form`, no `zod` — the form has two fields and one validator
 *   each, and the project policy is to keep auth screens free of form-lib
 *   dependencies.
 * - **Submit-Spinner:** the local `submitting` flag drives the disabled state
 *   and label of the submit button. `useAuth().isLoading` is intentionally
 *   *not* read for the spinner — it represents the bootstrap-profile call,
 *   not the credential POST. A `useRef` lock additionally short-circuits
 *   re-entry from rapid double-taps that fire before the `setSubmitting`
 *   flush.
 * - **401 handling:** the form catches the `ApiError(401)` from
 *   {@link useAuth#login} and shows a toast. The global 401-handler in
 *   {@link AuthProvider} only fires for *authenticated* requests (e.g.
 *   profile refetch); it would never fire for `/public/auth/login`. So a
 *   wrong-password submit needs its own toast.
 * - **Cookie-Banner:** the `CookieBanner` + `consent === 'rejected'`
 *   warning gate the form. `consent !== 'rejected'` gates the submit button
 *   so a user who explicitly rejected cookies cannot even try to log in
 *   (the cookie would be dropped on round-trip → infinite "wrong-credentials"
 *   feel).
 * - **`?reason=expired`:** delegated to {@link useExpiredReason} — *not*
 *   parsed manually.
 * - **Cookie-disabled detection:** `setOnCookieDisabled` is registered in a
 *   `useEffect` and unsubscribed via the cleanup return so leaving the screen
 *   doesn't leak the toast trigger into other screens.
 * - **Post-login redirect:** the screen calls `router.replace` with the
 *   resolved `?returnTo` target on successful login. The `(auth)/_layout.tsx`
 *   guard provides the same redirect declaratively for the case where an
 *   already-authenticated user lands on `/login` directly — both paths run
 *   the open-redirect whitelist via {@link resolveReturnTo}.
 *
 * @returns The login screen.
 */
export default function LoginScreen() {
  const { login, setOnCookieDisabled } = useAuth();
  const { consent, reject } = useCookieConsent();
  const params = useLocalSearchParams<{ returnTo?: string | string[] }>();

  const [mail, setMail] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [mailError, setMailError] = useState<string | null>(null);
  // Re-entry lock for rapid double-taps. `setSubmitting(true)` only takes
  // effect after the current render commits, so a second press scheduled
  // within the same tick can still see `submitting === false` via closure.
  // The ref flips synchronously and short-circuits the second call.
  const submittingRef = useRef(false);

  // ---------------------------------------------------------------------
  // useExpiredReason: fires a toast when the screen is reached via the
  // /(auth)/login?reason=expired redirect from the global 401-handler. The
  // hook itself owns the once-per-mount ref-guard.
  // ---------------------------------------------------------------------
  const onExpired = useCallback(() => {
    toast.error('Deine Sitzung ist abgelaufen. Bitte erneut anmelden.');
  }, []);
  useExpiredReason(onExpired);

  // ---------------------------------------------------------------------
  // setOnCookieDisabled: Web-only toast wiring for the cookie-disabled
  // detector in AuthProvider. Cleanup-arm with `null` guarantees we don't
  // keep firing toasts after the screen unmounts.
  // ---------------------------------------------------------------------
  useEffect(() => {
    if (Platform.OS !== 'web') return;
    setOnCookieDisabled(() => {
      toast.error(
        'Cookies sind blockiert — Login ist ohne Cookie nicht möglich. Bitte Cookies für diese Seite erlauben.',
      );
    });
    return () => {
      setOnCookieDisabled(null);
    };
  }, [setOnCookieDisabled]);

  function handleMailBlur() {
    setMailError(getMailErrorMessage(mail.trim()));
  }

  function handleCookieReject() {
    reject();
  }

  // Submit-Gate: client-side mail-regex green AND password non-empty AND
  // not currently submitting AND consent not explicitly rejected. The
  // rejected-cookie path also shows the banner-warning at the top of the
  // screen, but disabling the button is the belt-and-braces backup.
  const canSubmit =
    isValidDhbwMail(mail.trim()) &&
    password.length > 0 &&
    !submitting &&
    consent !== 'rejected';

  async function handleSubmit() {
    if (!canSubmit) return;
    if (submittingRef.current) return;
    submittingRef.current = true;
    setSubmitting(true);
    try {
      await login(mail.trim(), password);
      // Replace to the post-login target. The auth-group layout would also
      // redirect on the next render, but going via the screen ensures the
      // returnTo param (read from the focused route) is applied even on the
      // form-submit path where the layout's global params hook can be empty
      // mid-mutation on Web.
      router.replace(resolveReturnTo(params) as never);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 401) {
          toast.error('Falsche Zugangsdaten.');
        } else if (err.status === 422) {
          // Backend liefert die Begründung im message-Feld
          // (AuthenticationController.java Z. 91-105).
          toast.error(err.message);
        } else if (err.status === 0) {
          toast.error('Netzwerkfehler. Bitte erneut versuchen.');
        } else {
          toast.error('Anmeldung fehlgeschlagen.');
        }
      } else {
        toast.error('Anmeldung fehlgeschlagen.');
      }
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  }

  return (
    <View className="flex-1 bg-background">
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        className="flex-1"
      >
        <View className="flex-1 items-center justify-center px-6">
          <View className="w-full max-w-sm">
            {consent === 'rejected' && (
              <View className="mb-4 rounded-lg bg-destructive/10 px-4 py-3">
                <Text className="text-sm text-destructive">
                  {'Cookies sind erforderlich, um sich anzumelden. Bitte akzeptiere die Cookies über „Cookie-Einstellungen" im Footer, um fortzufahren.'}
                </Text>
              </View>
            )}

            <Text className="mb-6 text-2xl font-semibold text-foreground">
              Anmelden
            </Text>

            <View className="mb-4">
              <Label nativeID="login-mail-label" className="mb-1.5">
                DHBW-Mail
              </Label>
              <Input
                accessibilityLabel="DHBW-Mail"
                aria-labelledby="login-mail-label"
                value={mail}
                onChangeText={setMail}
                onBlur={handleMailBlur}
                autoCapitalize="none"
                autoComplete="email"
                autoCorrect={false}
                keyboardType="email-address"
                inputMode="email"
                placeholder="nachname.vorname.a24@student.dhbw-karlsruhe.de"
                editable={!submitting}
              />
              {mailError !== null && (
                <Text className="mt-1 text-xs text-destructive">{mailError}</Text>
              )}
            </View>

            <View className="mb-4">
              <Label nativeID="login-password-label" className="mb-1.5">
                Passwort
              </Label>
              <View className="flex-row items-center gap-2">
                <View className="flex-1">
                  <Input
                    accessibilityLabel="Passwort"
                    aria-labelledby="login-password-label"
                    value={password}
                    onChangeText={setPassword}
                    secureTextEntry={!showPassword}
                    autoCapitalize="none"
                    autoComplete="current-password"
                    autoCorrect={false}
                    textContentType="password"
                    maxLength={PASSWORD_MAX_LENGTH}
                    editable={!submitting}
                  />
                </View>
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel={
                    showPassword ? 'Passwort verbergen' : 'Passwort anzeigen'
                  }
                  onPress={() => setShowPassword((v) => !v)}
                  className="h-10 w-10 items-center justify-center rounded-md border border-border sm:h-9 sm:w-9"
                  disabled={submitting}
                >
                  <Icon as={showPassword ? EyeOff : Eye} className="size-4 text-muted-foreground" />
                </Pressable>
              </View>
            </View>

            <Button
              accessibilityLabel="Anmelden"
              onPress={handleSubmit}
              disabled={!canSubmit}
              className="mb-4"
            >
              <Text className="text-sm font-medium text-primary-foreground">
                {submitting ? 'Anmelden…' : 'Anmelden'}
              </Text>
            </Button>

            <Link href="/(auth)/register" asChild>
              <Pressable accessibilityRole="link">
                <Text className="text-center text-sm text-primary">
                  Noch kein Konto? Registrieren
                </Text>
              </Pressable>
            </Link>
          </View>
        </View>
      </KeyboardAvoidingView>
      <CookieBanner onRejectOverride={handleCookieReject} />
      <Footer />
    </View>
  );
}
