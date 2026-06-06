import { useRouter } from 'expo-router';
import { useState } from 'react';
import { Platform, Pressable, ScrollView, Text, View } from 'react-native';
import { toast } from 'sonner-native';

import { NotFoundScreen } from '@/components/NotFoundScreen';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Separator } from '@/components/ui/separator';
import { Text as UiText } from '@/components/ui/text';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';
import { type ThemePreference, useTheme } from '@/lib/theme/ThemeProvider';


/**
 * Dev-only showcase screen for rn-reusables components and the theme toggle.
 *
 * Renders {@link NotFoundScreen} in production builds (`__DEV__ === false`).
 *
 * @returns The dev showcase or the NotFoundScreen in production.
 */
export default function DevComponentsScreen() {
  if (!__DEV__) {
    return <NotFoundScreen />;
  }
  return <Showcase />;
}

function Showcase() {
  const router = useRouter();
  const { theme, effectiveTheme, setTheme } = useTheme();
  const [inputValue, setInputValue] = useState('');

  return (
    <ScrollView
      className="flex-1 bg-background"
      contentContainerStyle={{ paddingBottom: 48 }}
    >
      <View className="gap-8 px-6 py-8">
        <Section title="Dev Showcase">
          <UiText variant="muted">
            Visuelle QA für rn-reusables + Theme-Tokens. Nur in __DEV__ erreichbar.
            Tooltip ist Web-primary; auf Native rendert er meist nicht oder als Long-Press.
          </UiText>
          <View className="flex-row items-center gap-2">
            <UiText variant="small">Aktiv:</UiText>
            <Badge variant="secondary">
              <Text>{theme}</Text>
            </Badge>
            <UiText variant="small">→</UiText>
            <Badge variant="outline">
              <Text>{effectiveTheme}</Text>
            </Badge>
          </View>
          <View className="flex-row flex-wrap gap-2">
            {(['system', 'light', 'dark'] as const).map((option) => (
              <ThemeToggleButton
                key={option}
                option={option}
                active={theme === option}
                onPress={() => setTheme(option)}
              />
            ))}
          </View>
          <View className="flex-row flex-wrap gap-2">
            <Button variant="outline" size="sm" onPress={() => router.back()}>
              <UiText>← Zurück</UiText>
            </Button>
          </View>
        </Section>

        <Separator />

        <Section title="Buttons">
          <View className="flex-row flex-wrap gap-2">
            <Button>
              <UiText>Default</UiText>
            </Button>
            <Button variant="secondary">
              <UiText>Secondary</UiText>
            </Button>
            <Button variant="outline">
              <UiText>Outline</UiText>
            </Button>
            <Button variant="ghost">
              <UiText>Ghost</UiText>
            </Button>
            <Button variant="destructive">
              <UiText>Destructive</UiText>
            </Button>
            <Button variant="link">
              <UiText>Link</UiText>
            </Button>
          </View>
          <View className="flex-row flex-wrap items-center gap-2">
            <Button size="sm">
              <UiText>Small</UiText>
            </Button>
            <Button size="default">
              <UiText>Default</UiText>
            </Button>
            <Button size="lg">
              <UiText>Large</UiText>
            </Button>
            <Button disabled>
              <UiText>Disabled</UiText>
            </Button>
          </View>
        </Section>

        <Separator />

        <Section title="Cards">
          <Card>
            <CardHeader>
              <CardTitle>Mensa heute</CardTitle>
              <CardDescription>Vier Gerichte ab 11:30 Uhr</CardDescription>
            </CardHeader>
            <CardContent>
              <UiText>
                Beispiel-Card mit Header, Title, Description, Content. Tokens kommen aus
                <UiText variant="code"> tailwind.config.js</UiText>.
              </UiText>
            </CardContent>
          </Card>
          <Card>
            <CardHeader>
              <CardTitle>Dateien</CardTitle>
              <CardDescription>Zuletzt geöffnet</CardDescription>
            </CardHeader>
            <CardContent>
              <UiText variant="muted">Keine Einträge vorhanden.</UiText>
            </CardContent>
          </Card>
        </Section>

        <Separator />

        <Section title="Inputs + Labels">
          <View className="gap-2">
            <Label nativeID="dhbwMail">DHBW-Mail</Label>
            <Input
              aria-labelledby="dhbwMail"
              placeholder="vorname.nachname@lehre.dhbw-karlsruhe.de"
              value={inputValue}
              onChangeText={setInputValue}
              autoCapitalize="none"
            />
          </View>
          <View className="gap-2">
            <Label nativeID="disabledInput">Disabled-Beispiel</Label>
            <Input
              aria-labelledby="disabledInput"
              editable={false}
              value="Read-only"
            />
          </View>
        </Section>

        <Separator />

        <Section title="Dialog">
          <Dialog>
            <DialogTrigger asChild>
              <Button variant="outline">
                <UiText>Dialog öffnen</UiText>
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Showcase-Dialog</DialogTitle>
                <DialogDescription>
                  Dies ist ein rn-reusables Dialog über @rn-primitives/dialog. Animationen via
                  react-native-reanimated.
                </DialogDescription>
              </DialogHeader>
              <DialogFooter>
                <DialogClose asChild>
                  <Button variant="secondary">
                    <UiText>Schließen</UiText>
                  </Button>
                </DialogClose>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </Section>

        <Separator />

        <Section title="Avatar + Badge">
          <View className="flex-row items-center gap-3">
            <Avatar alt="Stub User Avatar">
              <AvatarFallback>
                <Text className="text-foreground">SU</Text>
              </AvatarFallback>
            </Avatar>
            <View className="gap-1">
              <UiText>Stub User</UiText>
              <View className="flex-row gap-2">
                <Badge>
                  <Text>STUDENT</Text>
                </Badge>
                <Badge variant="secondary">
                  <Text>TINF24B2</Text>
                </Badge>
                <Badge variant="destructive">
                  <Text>OFFLINE</Text>
                </Badge>
                <Badge variant="outline">
                  <Text>Beta</Text>
                </Badge>
              </View>
            </View>
          </View>
        </Section>

        <Separator />

        <Section title="Separator + Tooltip">
          <UiText variant="muted">Horizontal-Separator (oben/unten).</UiText>
          <Separator />
          <View className="flex-row items-center gap-2">
            {Platform.OS === 'web' ? (
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button variant="outline" size="sm">
                    <UiText>Hover (Web)</UiText>
                  </Button>
                </TooltipTrigger>
                <TooltipContent>
                  <Text className="text-primary-foreground text-xs">Web-only Tooltip</Text>
                </TooltipContent>
              </Tooltip>
            ) : (
              <View className="rounded-md border border-border px-3 py-2">
                <UiText variant="muted">Tooltip auf Native nicht angezeigt.</UiText>
              </View>
            )}
            <UiText variant="muted">↑ Tooltip nur Web-primary.</UiText>
          </View>
        </Section>

        <Separator />

        <Section title="Toast (sonner-native)">
          <View className="flex-row flex-wrap gap-2">
            <Button onPress={() => toast('Hallo aus dem Showcase')}>
              <UiText>Default-Toast</UiText>
            </Button>
            <Button variant="secondary" onPress={() => toast.success('Gespeichert')}>
              <UiText>Success</UiText>
            </Button>
            <Button variant="destructive" onPress={() => toast.error('Fehler beim Laden')}>
              <UiText>Error</UiText>
            </Button>
          </View>
        </Section>
      </View>
    </ScrollView>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <View className="gap-3">
      <UiText variant="h3">{title}</UiText>
      {children}
    </View>
  );
}

function ThemeToggleButton({
  option,
  active,
  onPress,
}: {
  option: ThemePreference;
  active: boolean;
  onPress: () => void;
}) {
  const label = option[0].toUpperCase() + option.slice(1);
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`Theme ${label}`}
      onPress={onPress}
      className={
        active
          ? 'rounded-md border border-primary bg-primary px-4 py-2'
          : 'rounded-md border border-border bg-background px-4 py-2'
      }
    >
      <Text className={active ? 'text-primary-foreground' : 'text-foreground'}>{label}</Text>
    </Pressable>
  );
}
