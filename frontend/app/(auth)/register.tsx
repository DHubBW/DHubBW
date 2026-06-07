import { Link } from 'expo-router';
import { Eye, EyeOff } from 'lucide-react-native';
import { useRef, useState } from 'react';
import {
  KeyboardAvoidingView,
  Platform,
  Pressable,
  Text,
  View,
} from 'react-native';
import { toast } from 'sonner-native';

import { CookieBanner } from '@/components/CookieBanner';
import { RegisterStep2 } from '@/components/auth/RegisterStep2';
import { StepIndicator } from '@/components/auth/StepIndicator';
import { Footer } from '@/components/layout/Footer';
import { Button } from '@/components/ui/button';
import { Icon } from '@/components/ui/icon';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { ApiError } from '@/lib/api/errors';
import { useAuth } from '@/lib/auth/AuthContext';
import {
  getMailErrorMessage,
  getNameErrorMessage,
  isValidDhbwMail,
  isValidName,
} from '@/lib/auth/validators';
import { useCookieConsent } from '@/lib/consent/useCookieConsent';

/** Hard cap on the password input — see {@link LoginScreen} for rationale. */
const PASSWORD_MAX_LENGTH = 256;

/**
 * Registration screen — two-step flow.
 *
 * **Step 1:** Name + DHBW-Mail + Password against
 * `POST /public/auth/register`. On success the screen advances to Step 2 via
 * a *local* `step` state — explicitly **not** `router.push`, so a back-swipe
 * doesn't strand the user with a half-registered account and lost form data.
 *
 * **Step 2:** delegated to {@link RegisterStep2}, which loads the course
 * catalogue, lets the user join an existing course or open a creation
 * request, and navigates to the dashboard on success or skip. The
 * registration screen itself owns only the `onBack` transition back to
 * Step 1; navigation away from Step 2 is handled inside the component.
 *
 * The DSGVO cookie-consent gate mirrors the login screen: the
 * {@link CookieBanner} is rendered, an explicit `'rejected'` decision blocks
 * the Step-1 submit (the registration response would set the session cookie
 * the same way as login), and Step 2 likewise refuses to submit while
 * consent is `'rejected'` so a network round-trip is never attempted.
 *
 * Implementation notes mirror {@link LoginScreen}: plain `useState`,
 * validators from `lib/auth/validators.ts`, no `react-hook-form` / `zod`,
 * own `submitting` flag (not `useAuth().isLoading`), `useRef` re-entry lock
 * against rapid double-taps.
 *
 * @returns The registration screen.
 */
export default function RegisterScreen() {
  const { register } = useAuth();
  const { consent, reject } = useCookieConsent();

  const [step, setStep] = useState<1 | 2>(1);
  const [name, setName] = useState('');
  const [mail, setMail] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [nameError, setNameError] = useState<string | null>(null);
  const [mailError, setMailError] = useState<string | null>(null);
  const submittingRef = useRef(false);

  function handleNameBlur() {
    setNameError(getNameErrorMessage(name.trim()));
  }
  function handleMailBlur() {
    setMailError(getMailErrorMessage(mail.trim()));
  }
  function handleCookieReject() {
    reject();
  }

  const canSubmitStep1 =
    isValidName(name.trim()) &&
    isValidDhbwMail(mail.trim()) &&
    password.length > 0 &&
    !submitting &&
    consent !== 'rejected';

  async function handleSubmitStep1() {
    if (!canSubmitStep1) return;
    if (submittingRef.current) return;
    submittingRef.current = true;
    setSubmitting(true);
    try {
      await register(mail.trim(), name.trim(), password);
      // Local-state transition only — see component-level JSDoc.
      setStep(2);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 409) {
          toast.error('E-Mail ist bereits registriert.');
        } else if (err.status === 422) {
          toast.error(err.message);
        } else if (err.status === 0) {
          toast.error('Netzwerkfehler. Bitte erneut versuchen.');
        } else {
          toast.error('Registrierung fehlgeschlagen.');
        }
      } else {
        toast.error('Registrierung fehlgeschlagen.');
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
                  {'Cookies sind erforderlich, um sich zu registrieren. Bitte akzeptiere die Cookies über „Cookie-Einstellungen" im Footer, um fortzufahren.'}
                </Text>
              </View>
            )}

            <StepIndicator current={step} total={2} className="mb-6" />
            <Text className="mb-6 text-2xl font-semibold text-foreground">
              {step === 1 ? 'Registrieren' : 'Kurs auswählen'}
            </Text>

            {step === 1 ? (
              <>
                <View className="mb-4">
                  <Label nativeID="register-name-label" className="mb-1.5">
                    Vollständiger Name
                  </Label>
                  <Input
                    accessibilityLabel="Vollständiger Name"
                    aria-labelledby="register-name-label"
                    value={name}
                    onChangeText={setName}
                    onBlur={handleNameBlur}
                    autoCapitalize="words"
                    autoComplete="name"
                    autoCorrect={false}
                    placeholder="Max Mustermann"
                    editable={!submitting}
                  />
                  {nameError !== null && (
                    <Text className="mt-1 text-xs text-destructive">{nameError}</Text>
                  )}
                </View>

                <View className="mb-4">
                  <Label nativeID="register-mail-label" className="mb-1.5">
                    DHBW-Mail
                  </Label>
                  <Input
                    accessibilityLabel="DHBW-Mail"
                    aria-labelledby="register-mail-label"
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
                  <Label nativeID="register-password-label" className="mb-1.5">
                    Passwort
                  </Label>
                  <View className="flex-row items-center gap-2">
                    <View className="flex-1">
                      <Input
                        accessibilityLabel="Passwort"
                        aria-labelledby="register-password-label"
                        value={password}
                        onChangeText={setPassword}
                        secureTextEntry={!showPassword}
                        autoCapitalize="none"
                        autoComplete="new-password"
                        autoCorrect={false}
                        textContentType="newPassword"
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
                      <Icon
                        as={showPassword ? EyeOff : Eye}
                        className="size-4 text-muted-foreground"
                      />
                    </Pressable>
                  </View>
                </View>

                <Button
                  accessibilityLabel="Weiter"
                  onPress={handleSubmitStep1}
                  disabled={!canSubmitStep1}
                  className="mb-4"
                >
                  <Text className="text-sm font-medium text-primary-foreground">
                    {submitting ? 'Registriere…' : 'Weiter'}
                  </Text>
                </Button>

                <Link href="/(auth)/login" asChild>
                  <Pressable accessibilityRole="link">
                    <Text className="text-center text-sm text-primary">
                      Bereits ein Konto? Anmelden
                    </Text>
                  </Pressable>
                </Link>
              </>
            ) : (
              <RegisterStep2 onBack={() => setStep(1)} />
            )}
          </View>
        </View>
      </KeyboardAvoidingView>
      <CookieBanner onRejectOverride={handleCookieReject} />
      <Footer />
    </View>
  );
}
