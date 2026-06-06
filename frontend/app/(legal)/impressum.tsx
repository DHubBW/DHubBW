import { Stack } from 'expo-router';
import { type ReactNode } from 'react';
import { ScrollView, Text, View } from 'react-native';

import { Footer } from '@/components/layout/Footer';
import { t } from '@/lib/i18n/t';

/**
 * Static Impressum screen (Anbieterkennzeichnung gemäß § 5 TMG).
 *
 * Content is a placeholder that must be replaced with verified legal
 * information before the application goes live.
 *
 * @returns The Impressum screen.
 */
export default function ImpressumScreen() {
  return (
    <>
      <Stack.Screen options={{ title: t('legal.impressum.title', 'Impressum') }} />
      <ScrollView className="flex-1 bg-background">
        <View className="px-6 py-8">
          <Text className="mb-6 text-2xl font-bold text-foreground">
            {t('legal.impressum.title', 'Impressum')}
          </Text>

          <Section title={t('legal.impressum.provider', 'Anbieter')}>
            <LegalText>DHBW Karlsruhe</LegalText>
            <LegalText>Kurs TINF24B2, Gruppe 4</LegalText>
            <LegalText>Erzbergerstraße 121</LegalText>
            <LegalText>76133 Karlsruhe</LegalText>
          </Section>

          <Section title={t('legal.impressum.representatives', 'Vertretungsberechtigte')}>
            <LegalText>Tom S</LegalText>
            <LegalText>Linus K</LegalText>
            <LegalText>Nils D</LegalText>
          </Section>

          <Section title={t('legal.impressum.contact', 'Kontakt')}>
            <LegalText>
              {t(
                'legal.impressum.contactNote',
                'Bei Fragen wenden Sie sich bitte an Ihren DHBW-Dozenten oder die Projektgruppe über GitLab.',
              )}
            </LegalText>
          </Section>

          <Section title={t('legal.impressum.supervisedBy', 'Betreut durch')}>
            <LegalText>Prof. Dr. Roland Schätzle</LegalText>
            <LegalText>DHBW Karlsruhe, Fakultät Technik</LegalText>
          </Section>

          <Text className="mt-8 text-xs text-muted-foreground">
            {t(
              'legal.impressum.disclaimer',
              'Dieses Angebot ist ein studentisches Lehrprojekt und kein kommerzielles Produkt. Alle Inhalte dienen ausschließlich Lehr- und Lernzwecken.',
            )}
          </Text>
        </View>
        <Footer />
      </ScrollView>
    </>
  );
}

type SectionProps = { title: string; children: ReactNode };

function Section({ title, children }: SectionProps) {
  return (
    <View className="mb-6">
      <Text className="mb-2 text-base font-semibold text-foreground">{title}</Text>
      <View className="rounded-lg bg-card px-4 py-3">{children}</View>
    </View>
  );
}

type LegalTextProps = { children: ReactNode };

function LegalText({ children }: LegalTextProps) {
  return <Text className="py-0.5 text-sm text-card-foreground">{children}</Text>;
}
