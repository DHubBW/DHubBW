import { Stack } from 'expo-router';
import { type ReactNode } from 'react';
import { ScrollView, Text, View } from 'react-native';

import { Footer } from '@/components/layout/Footer';
import { t } from '@/lib/i18n/t';

/**
 * Static Datenschutzerklärung screen (DSGVO Art. 13 / TTDSG).
 *
 * Content is a placeholder. Replace with verified legal text before
 * production deployment.
 *
 * @returns The Datenschutz screen.
 */
export default function DatenschutzScreen() {
  return (
    <>
      <Stack.Screen options={{ title: t('legal.datenschutz.title', 'Datenschutzerklärung') }} />
      <ScrollView className="flex-1 bg-background">
        <View className="px-6 py-8">
          <Text className="mb-6 text-2xl font-bold text-foreground">
            {t('legal.datenschutz.title', 'Datenschutzerklärung')}
          </Text>

          <DSSection title="1. Verantwortliche Stelle">
            <DSText>
              Verantwortlich im Sinne der DSGVO ist die Projektgruppe TINF24B2 Gruppe 4 an der DHBW
              Karlsruhe, betreut durch Prof. Dr. Roland Schätzle.
            </DSText>
          </DSSection>

          <DSSection title="2. Welche Daten werden gespeichert?">
            <DSText>Wir verarbeiten folgende personenbezogene Daten:</DSText>
            <View className="mt-2 gap-1">
              {[
                'Name',
                'DHBW-E-Mail-Adresse',
                'Kursmitgliedschaft (Kursname)',
                'Hochgeladene Dateien und Ordnerstrukturen',
                'Session-Cookie (jwt) für die Authentifizierung',
              ].map((item) => (
                <Text key={item} className="text-sm text-card-foreground">
                  {'• '}{item}
                </Text>
              ))}
            </View>
          </DSSection>

          <DSSection title="3. Zweck und Rechtsgrundlage">
            <DSText>
              Die Verarbeitung erfolgt zur Erfüllung des Nutzungsvertrags (Art. 6 Abs. 1 lit. b DSGVO)
              sowie auf Grundlage Ihrer Einwilligung (Art. 6 Abs. 1 lit. a DSGVO) beim Akzeptieren des
              Cookie-Banners.
            </DSText>
          </DSSection>

          <DSSection title="4. Session-Cookie (jwt)">
            <DSText>
              Die Anwendung setzt nach dem Login ein technisch notwendiges HttpOnly-Cookie mit dem Namen
              `jwt`. Dieses Cookie enthält ein signiertes JSON Web Token (JWT) und dient ausschließlich
              der sicheren Sitzungsverwaltung. Das Cookie wird beim Logout oder nach Ablauf der Sitzung
              automatisch gelöscht. Eine Weitergabe an Dritte findet nicht statt.
            </DSText>
          </DSSection>

          <DSSection title="5. Speicherdauer">
            <DSText>
              Personenbezogene Daten werden gelöscht, sobald sie für den Zweck der Verarbeitung nicht
              mehr benötigt werden. Account-Daten werden auf Anfrage gelöscht. Das Session-Cookie
              läuft nach Ablauf des definierten Gültigkeitszeitraums automatisch ab.
            </DSText>
          </DSSection>

          <DSSection title="6. Ihre Rechte">
            <DSText>Sie haben das Recht auf:</DSText>
            <View className="mt-2 gap-1">
              {[
                'Auskunft über Ihre gespeicherten Daten (Art. 15 DSGVO)',
                'Berichtigung unrichtiger Daten (Art. 16 DSGVO)',
                'Löschung Ihrer Daten ("Recht auf Vergessenwerden", Art. 17 DSGVO)',
                'Einschränkung der Verarbeitung (Art. 18 DSGVO)',
                'Widerspruch gegen die Verarbeitung (Art. 21 DSGVO)',
                'Datenübertragbarkeit (Art. 20 DSGVO)',
              ].map((item) => (
                <Text key={item} className="text-sm text-card-foreground">
                  {'• '}{item}
                </Text>
              ))}
            </View>
            <DSText className="mt-2">
              Um Ihre Rechte geltend zu machen, wenden Sie sich an die verantwortliche Stelle (siehe
              Impressum) oder kontaktieren Sie Ihren zuständigen DHBW-Datenschutzbeauftragten.
            </DSText>
          </DSSection>

          <DSSection title="7. Beschwerderecht">
            <DSText>
              Sie haben das Recht, eine Beschwerde bei der zuständigen Datenschutzaufsichtsbehörde
              einzureichen. Zuständig ist der Landesbeauftragte für den Datenschutz und die
              Informationsfreiheit Baden-Württemberg.
            </DSText>
          </DSSection>

          <Text className="mt-8 text-xs text-muted-foreground">
            Stand: Juni 2026 — Diese Datenschutzerklärung ist ein Entwurf für Lehr- und
            Demonstrationszwecke und muss vor einem produktiven Betrieb rechtlich geprüft werden.
          </Text>
        </View>
        <Footer />
      </ScrollView>
    </>
  );
}

type DSSectionProps = { title: string; children: ReactNode };

function DSSection({ title, children }: DSSectionProps) {
  return (
    <View className="mb-6">
      <Text className="mb-2 text-base font-semibold text-foreground">{title}</Text>
      <View className="rounded-lg bg-card px-4 py-3">{children}</View>
    </View>
  );
}

type DSTextProps = { children: ReactNode; className?: string };

function DSText({ children, className }: DSTextProps) {
  return <Text className={`text-sm leading-relaxed text-card-foreground${className ? ` ${className}` : ''}`}>{children}</Text>;
}
