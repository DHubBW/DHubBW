import { useState } from 'react';
import type * as React from 'react';
import { Platform, ScrollView, View } from 'react-native';

import { NotFoundScreen } from '@/components/NotFoundScreen';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Separator } from '@/components/ui/separator';
import { Text as UiText } from '@/components/ui/text';
import {
  type UploadErrorCode,
  type UploadFileLike,
  useFileUpload,
} from '@/hooks/useFileUpload';
import { useAuth } from '@/lib/auth/AuthContext';

/**
 * Dev-only spike screen for the cross-platform `useFileUpload` hook.
 *
 * Lives under `(app)/_dev` so the surrounding {@link ProtectedRoute} guards
 * it — the upload endpoint requires a STUDENT session, and the screen
 * would otherwise just show 401 errors. Hidden behind `__DEV__`: in
 * production builds the {@link NotFoundScreen} is rendered instead.
 *
 * The screen is intentionally minimal — its job is manual verification of
 * the hook against the local backend, not a real upload UX. The
 * production picker / drop-zone is owned by FE-Dateien-2.
 *
 * @returns The spike screen on Web (in `__DEV__`); a "not supported"
 *   notice on Native; the not-found screen in production builds.
 */
export default function UploadSpikeScreen() {
  if (!__DEV__) {
    return <NotFoundScreen />;
  }
  return <Spike />;
}

function Spike() {
  const { user } = useAuth();
  const upload = useFileUpload();

  // Bound inputs for the two `@RequestParam` fields the controller wants
  // alongside the file — the hook itself does not care what the values
  // are, the screen exists to drive both with sensible defaults.
  const [ueberordnerId, setUeberordnerId] = useState('');
  const [name, setName] = useState('');
  const [picked, setPicked] = useState<{ file: UploadFileLike; label: string } | null>(null);

  const lastDateiID =
    upload.status === 'success' && upload.data ? upload.data.dateiID : null;
  const lastErrorCode: UploadErrorCode | null =
    upload.status === 'error' && upload.error ? upload.error.code : null;
  const lastErrorMessage =
    upload.status === 'error' && upload.error ? upload.error.message : null;

  const canSubmit =
    !upload.isPending && picked !== null && ueberordnerId.length > 0 && name.length > 0;

  const handleSubmit = () => {
    if (!picked) return;
    upload.mutate({ file: picked.file, ueberordnerId, name });
  };

  return (
    <ScrollView
      className="flex-1 bg-background"
      contentContainerStyle={{ paddingBottom: 48 }}
    >
      <View className="gap-6 px-6 py-8">
        <View>
          <UiText variant="h2">Datei-Upload-Spike</UiText>
          <UiText variant="muted" className="mt-1">
            Manueller Verifikations-Screen für den `useFileUpload`-Hook. Nur in `__DEV__`
            erreichbar.
          </UiText>
        </View>

        <SessionInfo userName={user?.name ?? null} userMail={user?.dhbwMail ?? null} />

        <Separator />

        <Card>
          <CardHeader>
            <CardTitle>Eingabe</CardTitle>
            <CardDescription>
              Ordner-UUID und Anzeigename ausfüllen, dann Datei wählen und absenden.
            </CardDescription>
          </CardHeader>
          <CardContent className="gap-4">
            <View className="gap-1.5">
              <Label nativeID="upload-spike-folder-label">Ordner-UUID (`ueberordnerId`)</Label>
              <Input
                aria-labelledby="upload-spike-folder-label"
                placeholder="00000000-0000-0000-0000-000000000000"
                value={ueberordnerId}
                onChangeText={setUeberordnerId}
                autoCapitalize="none"
                autoCorrect={false}
              />
            </View>
            <View className="gap-1.5">
              <Label nativeID="upload-spike-name-label">Anzeigename (`name`)</Label>
              <Input
                aria-labelledby="upload-spike-name-label"
                placeholder="vorlesung-woche-3.pdf"
                value={name}
                onChangeText={setName}
                autoCapitalize="none"
                autoCorrect={false}
              />
            </View>

            <View className="gap-1.5">
              <Label nativeID="upload-spike-file-label">Datei</Label>
              <FilePickerSlot
                onPicked={(file, label) => {
                  setPicked({ file, label });
                  // Convenience: pre-fill the name with the picked filename
                  // when the user has not typed one yet.
                  if (name === '' && label) setName(label);
                }}
              />
              {picked && (
                <UiText variant="small" className="text-muted-foreground">
                  Ausgewählt: {picked.label}
                </UiText>
              )}
            </View>

            <Button onPress={handleSubmit} disabled={!canSubmit}>
              <UiText>{upload.isPending ? 'Lade hoch …' : 'Hochladen'}</UiText>
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Status</CardTitle>
            <CardDescription>
              Live-Anzeige der Hook-Mutation. Spinner statt Progress ist Phase-1-Scope.
            </CardDescription>
          </CardHeader>
          <CardContent className="gap-2">
            <View className="flex-row items-center gap-2">
              <UiText variant="small" className="text-muted-foreground">
                Hook-Status:
              </UiText>
              <Badge variant={badgeVariantFor(upload.status)}>
                <UiText>{upload.status}</UiText>
              </Badge>
            </View>
            {lastDateiID && (
              <UiText variant="small">
                ✓ Upload erfolgreich. dateiID: {lastDateiID}
              </UiText>
            )}
            {lastErrorCode && (
              <UiText variant="small" className="text-destructive">
                ✗ Fehler ({lastErrorCode}): {lastErrorMessage}
              </UiText>
            )}
          </CardContent>
        </Card>
      </View>
    </ScrollView>
  );
}

/**
 * Renders a tiny session-info block so the smoke-tester sees at a glance
 * whether the screen has the STUDENT cookie/token it needs. The
 * surrounding {@link ProtectedRoute} guarantees `user !== null` once
 * rendered, but the explicit display still helps when verifying that the
 * AuthContext is correctly wired into this group.
 *
 * @param props - Display props.
 * @param props.userName - Profile name, or `null` when not loaded.
 * @param props.userMail - Profile mail, or `null` when not loaded.
 * @returns The session-info card.
 */
function SessionInfo({
  userName,
  userMail,
}: {
  userName: string | null;
  userMail: string | null;
}) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Session</CardTitle>
        <CardDescription>Eingeloggter Student (für Backend-Auth-Header).</CardDescription>
      </CardHeader>
      <CardContent>
        <UiText variant="small">
          {userName ? `${userName} (${userMail ?? '-'})` : 'Kein Profil geladen.'}
        </UiText>
      </CardContent>
    </Card>
  );
}

/**
 * Platform-specific picker slot. On Web we render a real `<input
 * type="file">`; on Native we surface a notice rather than ship a picker
 * — the production picker (`expo-document-picker`) is FE-Dateien-2 scope
 * and intentionally not pulled in by the spike.
 *
 * @param props - Slot props.
 * @param props.onPicked - Called when the user selects a file (Web only).
 *   Receives the binary handle ready for {@link useFileUpload} plus the
 *   filename for default-naming convenience.
 * @returns The picker on Web, a notice on Native.
 */
function FilePickerSlot({
  onPicked,
}: {
  onPicked: (file: UploadFileLike, label: string) => void;
}) {
  if (Platform.OS !== 'web') {
    return (
      <UiText variant="small" className="text-muted-foreground">
        Native-Picker ist FE-Dateien-2-Scope. Web-Browser für den Spike-Smoke nutzen.
      </UiText>
    );
  }
  // React Native does not type the bare HTML `<input>`; hand-render via
  // a `string`-tag literal, cast to a generic component type so the JSX
  // type-check accepts it. This is Web-only code (Platform-guard above).
  const HtmlInput = 'input' as unknown as React.ComponentType<{
    type: string;
    onChange: (ev: { target: { files: FileList | null } }) => void;
  }>;
  return (
    <HtmlInput
      type="file"
      onChange={(ev) => {
        const file = ev?.target?.files?.[0];
        if (!file) return;
        onPicked(file, file.name);
      }}
    />
  );
}

/**
 * Badge-variant lookup for the four mutation states. Centralised so the
 * status card stays declarative.
 *
 * @param status - The current TanStack-Query mutation status.
 * @returns The matching badge variant.
 */
function badgeVariantFor(
  status: 'idle' | 'pending' | 'success' | 'error',
): 'default' | 'secondary' | 'destructive' | 'outline' {
  switch (status) {
    case 'pending':
      return 'secondary';
    case 'success':
      return 'default';
    case 'error':
      return 'destructive';
    default:
      return 'outline';
  }
}
