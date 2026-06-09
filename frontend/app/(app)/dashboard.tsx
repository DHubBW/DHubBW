/**
 * Dashboard screen — role-dependent layout per `studentType`.
 *
 * - No course: "Kurs einrichten" card with join/create actions
 * - STUDENT: greeting, mensa preview (all dishes, scrollable), latest files, quick-access
 * - KURSADMIN: student view + course stats card
 * - SYSTEMADMIN: platform KPI card + kursadmin block + mensa + latest files
 */
import { useQueryClient } from "@tanstack/react-query";
import { useRouter } from "expo-router";
import {
  ArrowRight,
  Clock,
  File,
  FolderOpen,
  Hourglass,
  Plus,
  Star,
  Upload,
  Users,
  Utensils,
} from "lucide-react-native";
import { useState } from "react";
import { ScrollView, TouchableOpacity, View } from "react-native";

import { NewKursDialog } from "@/components/auth/NewKursDialog";
import { RegisterStep2 } from "@/components/auth/RegisterStep2";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { Text } from "@/components/ui/text";
import { useDashboardDateien } from "@/hooks/useDashboardDateien";
import { useDashboardMeals } from "@/hooks/useDashboardMeals";
import { useKursAdminStats } from "@/hooks/useKursAdminStats";
import { useSysAdminStats } from "@/hooks/useSysAdminStats";
import type {
  DateiDTO,
  KursAdminStatsDTO,
  MensaDTO,
  PendingKursRequestDTO,
  SystemAdminStatsDTO,
} from "@/lib/api/types.gen";
import { useAuth } from "@/lib/auth/AuthContext";
import { PROFILE_QUERY_KEY } from "@/lib/auth/useProfile";
import { t } from "@/lib/i18n/t";

// ─── Date helpers ────────────────────────────────────────────────────────────

const WEEKDAY_NAMES = [
  "Sonntag",
  "Montag",
  "Dienstag",
  "Mittwoch",
  "Donnerstag",
  "Freitag",
  "Samstag",
] as const;

const MONTH_NAMES = [
  "Januar",
  "Februar",
  "März",
  "April",
  "Mai",
  "Juni",
  "Juli",
  "August",
  "September",
  "Oktober",
  "November",
  "Dezember",
] as const;

function formatTodayLong(): string {
  const d = new Date();
  return `${WEEKDAY_NAMES[d.getDay()]}, ${d.getDate()}. ${MONTH_NAMES[d.getMonth()]} ${d.getFullYear()}`;
}

// ─── Dietary helpers ─────────────────────────────────────────────────────────

type Ernaehrung = "VEGETARISCH" | "VEGAN" | "FLEISCH";

function ernaehrungColor(e: Ernaehrung): string {
  if (e === "VEGAN") return "hsl(var(--success))";
  if (e === "VEGETARISCH") return "hsl(var(--warning))";
  return "hsl(var(--destructive))";
}

function ernaehrungLabel(e: Ernaehrung): string {
  if (e === "VEGAN") return "Vegan";
  if (e === "VEGETARISCH") return "Vegetarisch";
  return "Fleisch";
}

// ─── Section header ──────────────────────────────────────────────────────────

export type SectionHeaderProps = {
  icon: typeof Utensils;
  label: string;
  iconColor?: string;
};

function SectionHeader({ icon: Icon, label, iconColor }: SectionHeaderProps) {
  return (
    <View className="mb-3 flex-row items-center gap-2">
      <Icon
        size={16}
        color={iconColor ?? "hsl(var(--foreground))"}
        strokeWidth={2}
      />
      <Text className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">
        {label}
      </Text>
    </View>
  );
}

// ─── Mensa card ──────────────────────────────────────────────────────────────

export type MensaPreviewCardProps = {
  meals: MensaDTO[];
  isLoading: boolean;
  isError: boolean;
};

function MealRow({ meal }: { meal: MensaDTO }) {
  const dotColor = ernaehrungColor(meal.ernaehrung as Ernaehrung);
  const label = ernaehrungLabel(meal.ernaehrung as Ernaehrung);
  return (
    <View className="flex-row items-center justify-between py-2.5">
      <View className="flex-1 flex-row items-start gap-2.5 pr-3">
        <View
          className="mt-1 h-2.5 w-2.5 shrink-0 rounded-full"
          style={{ backgroundColor: dotColor }}
          accessibilityLabel={label}
        />
        <View className="flex-1">
          <Text className="text-sm font-medium leading-snug text-foreground">
            {meal.essensbezeichnung}
          </Text>
          <Text className="mt-0.5 text-xs text-muted-foreground">
            {meal.linie}
          </Text>
        </View>
      </View>
      <Text className="shrink-0 text-sm font-semibold tabular-nums text-primary">
        {meal.preis.toFixed(2).replace(".", ",")}€
      </Text>
    </View>
  );
}

function MensaPreviewCard({
  meals,
  isLoading,
  isError,
}: MensaPreviewCardProps) {
  const router = useRouter();

  return (
    <View className="flex-1">
      <SectionHeader
        icon={Utensils}
        label={t("dashboard.mensa.title", "Mensa Heute")}
      />
      {isLoading && (
        <Text className="text-sm text-muted-foreground">
          {t("dashboard.mensa.loading", "Wird geladen …")}
        </Text>
      )}
      {isError && (
        <Text className="text-sm text-destructive">
          {t(
            "dashboard.mensa.error",
            "Speiseplan konnte nicht geladen werden.",
          )}
        </Text>
      )}
      {!isLoading && !isError && meals.length === 0 && (
        <Text className="text-sm text-muted-foreground">
          {t("dashboard.mensa.empty", "Kein Speiseplan für heute.")}
        </Text>
      )}
      {!isLoading && !isError && meals.length > 0 && (
        <ScrollView
          className="max-h-56"
          showsVerticalScrollIndicator
          indicatorStyle="black"
          nestedScrollEnabled
        >
          <View>
            {meals.map((meal, idx) => (
              <View key={meal.gerichtID}>
                {idx > 0 && <Separator className="opacity-40" />}
                <MealRow meal={meal} />
              </View>
            ))}
          </View>
        </ScrollView>
      )}
      <TouchableOpacity
        onPress={() => router.push("/(app)/mensa")}
        accessibilityRole="link"
        className="mt-3 flex-row items-center gap-1"
      >
        <Text className="text-sm text-primary">
          {t("dashboard.mensa.showAll", "Vollständigen Speiseplan anzeigen")}
        </Text>
        <ArrowRight size={13} color="hsl(var(--primary))" />
      </TouchableOpacity>
    </View>
  );
}

// ─── Latest files card ───────────────────────────────────────────────────────

export type LatestFilesCardProps = {
  dateien: DateiDTO[];
  isLoading: boolean;
  isError: boolean;
};

function FileRow({ datei }: { datei: DateiDTO }) {
  return (
    <View className="flex-row items-center justify-between py-2.5">
      <View className="flex-1 pr-3">
        <Text className="text-sm font-medium text-foreground" numberOfLines={1}>
          {datei.name}
        </Text>
        <Text
          className="mt-0.5 text-xs text-muted-foreground"
          numberOfLines={1}
        >
          {datei.uploaderName ?? "—"}
          {"  ·  "}
          {datei.zuletztBearbeitet}
        </Text>
      </View>
    </View>
  );
}

function LatestFilesCard({
  dateien,
  isLoading,
  isError,
}: LatestFilesCardProps) {
  const router = useRouter();

  const sorted = dateien
    .slice()
    .sort((a, b) => (a.zuletztBearbeitet > b.zuletztBearbeitet ? -1 : 1));

  return (
    <View className="flex-1">
      <SectionHeader
        icon={File}
        label={t("dashboard.dateien.title", "Neueste Dateien")}
      />
      {isLoading && (
        <Text className="text-sm text-muted-foreground">
          {t("dashboard.dateien.loading", "Wird geladen …")}
        </Text>
      )}
      {isError && (
        <Text className="text-sm text-destructive">
          {t(
            "dashboard.dateien.error",
            "Dateien konnten nicht geladen werden.",
          )}
        </Text>
      )}
      {!isLoading && !isError && sorted.length === 0 && (
        <Text className="text-sm text-muted-foreground">
          {t("dashboard.dateien.empty", "Noch keine Dateien vorhanden.")}
        </Text>
      )}
      {!isLoading && !isError && sorted.length > 0 && (
        <ScrollView
          className="max-h-56"
          showsVerticalScrollIndicator
          indicatorStyle="black"
          nestedScrollEnabled
        >
          <View>
            {sorted.map((datei, idx) => (
              <View key={datei.dateiID}>
                {idx > 0 && <Separator className="opacity-40" />}
                <FileRow datei={datei} />
              </View>
            ))}
          </View>
        </ScrollView>
      )}
      {!isLoading && !isError && (
        <TouchableOpacity
          onPress={() => router.push("/(app)/dateien")}
          accessibilityRole="link"
          className="mt-3 flex-row items-center gap-1"
        >
          <Text className="text-sm text-primary">
            {t("dashboard.dateien.showAll", "Alle Dateien anzeigen")}
          </Text>
          <ArrowRight size={13} color="hsl(var(--primary))" />
        </TouchableOpacity>
      )}
    </View>
  );
}

// ─── Pending Request card (waiting for approval) ─────────────────────────────

export type PendingRequestCardProps = {
  pending: PendingKursRequestDTO;
};

function PendingRequestCard({ pending }: PendingRequestCardProps) {
  const bodyText =
    pending.type === "BEITRITT"
      ? t(
          "dashboard.pendingRequest.beitrittBody",
          `Deine Beitrittsanfrage für ${pending.kursName ?? "einen Kurs"} ist eingegangen und wartet auf Bearbeitung durch den Kursadmin.`,
        )
      : t(
          "dashboard.pendingRequest.erstellungBody",
          `Deine Kursanfrage${pending.kursName ? ` für ${pending.kursName}` : ""} ist eingegangen und wartet auf Bearbeitung durch den Systemadmin.`,
        );

  return (
    <Card className="gap-0 px-5 py-5">
      <View className="mb-3 flex-row items-center gap-2">
        <Hourglass size={16} color="hsl(var(--warning))" strokeWidth={2} />
        <Text className="text-base font-semibold text-foreground">
          {t("dashboard.pendingRequest.title", "Anfrage ausstehend")}
        </Text>
      </View>
      <Text className="mb-3 text-sm text-muted-foreground">{bodyText}</Text>
      <Text className="text-xs text-muted-foreground">
        {t("dashboard.pendingRequest.date", "Eingereicht am")}:{" "}
        {pending.anfrageDatum}
      </Text>
    </Card>
  );
}

// ─── Kurs-Einrichten card (no-course state) ──────────────────────────────────

type KursMode = "idle" | "join" | "create";

function KursEinrichtenCard() {
  const queryClient = useQueryClient();
  const [mode, setMode] = useState<KursMode>("idle");
  const [newKursOpen, setNewKursOpen] = useState(false);

  if (mode === "join") {
    return (
      <Card className="gap-0 px-5 py-5">
        <Text className="mb-4 text-base font-semibold text-foreground">
          {t("dashboard.kursEinrichten.title", "Kurs einrichten")}
        </Text>
        <RegisterStep2 onBack={() => setMode("idle")} hideSkip />
      </Card>
    );
  }

  return (
    <>
      <Card className="gap-0 px-5 py-5">
        <Text className="mb-1 text-base font-semibold text-foreground">
          {t("dashboard.kursEinrichten.title", "Kurs einrichten")}
        </Text>
        <Text className="mb-4 text-sm text-muted-foreground">
          {t(
            "dashboard.kursEinrichten.subtitle",
            "Du bist noch keinem Kurs zugeordnet.",
          )}
        </Text>
        <View className="flex-row gap-3">
          <TouchableOpacity
            onPress={() => setMode("join")}
            accessibilityRole="button"
            className="flex-1 rounded-lg border border-border bg-card p-4 active:bg-accent"
          >
            <View className="flex-row items-center gap-2">
              <ArrowRight size={16} color="hsl(var(--primary))" />
              <Text className="text-sm font-semibold text-primary">
                {t("dashboard.kursEinrichten.beitreten", "Kurs beitreten")}
              </Text>
            </View>
            <Text className="mt-1 text-xs text-muted-foreground">
              {t(
                "dashboard.kursEinrichten.beitretenHint",
                "Dein Kurs existiert bereits? Stelle eine Beitrittsanfrage.",
              )}
            </Text>
          </TouchableOpacity>
          <TouchableOpacity
            onPress={() => setNewKursOpen(true)}
            accessibilityRole="button"
            className="flex-1 rounded-lg border border-border bg-card p-4 active:bg-accent"
          >
            <View className="flex-row items-center gap-2">
              <Plus size={16} color="hsl(var(--primary))" />
              <Text className="text-sm font-semibold text-primary">
                {t("dashboard.kursEinrichten.erstellen", "Neuen Kurs anfragen")}
              </Text>
            </View>
            <Text className="mt-1 text-xs text-muted-foreground">
              {t(
                "dashboard.kursEinrichten.erstellenHint",
                "Dein Kurs ist noch nicht vorhanden? Stelle eine Kursanfrage.",
              )}
            </Text>
          </TouchableOpacity>
        </View>
      </Card>
      <NewKursDialog
        open={newKursOpen}
        onOpenChange={setNewKursOpen}
        onCreated={async () => {
          setNewKursOpen(false);
          if (
            typeof window !== "undefined" &&
            typeof window.location?.replace === "function"
          ) {
            window.location.replace("/dashboard");
          } else {
            await queryClient.invalidateQueries({ queryKey: PROFILE_QUERY_KEY });
          }
        }}
      />
    </>
  );
}

// ─── Stat item ───────────────────────────────────────────────────────────────

export type StatItemProps = {
  icon: typeof Users;
  iconColor: string;
  value: number;
  label: string;
  badge?: number;
};

function StatItem({
  icon: Icon,
  iconColor,
  value,
  label,
  badge,
}: StatItemProps) {
  return (
    <View className="min-w-16 flex-1 items-center gap-1">
      <View className="relative">
        <Icon size={26} color={iconColor} strokeWidth={1.75} />
        {badge !== undefined && (
          <View className="absolute -right-2 -top-2 h-4 min-w-4 items-center justify-center rounded-full bg-destructive px-0.5">
            <Text className="text-[10px] font-bold text-destructive-foreground">
              {badge}
            </Text>
          </View>
        )}
      </View>
      <Text className="text-xl font-bold text-foreground">{value}</Text>
      <Text className="text-center text-xs text-muted-foreground">{label}</Text>
    </View>
  );
}

// ─── Kursadmin stats section ─────────────────────────────────────────────────

export type KursAdminStatsSectionProps = {
  stats: KursAdminStatsDTO | null | undefined;
  isLoading: boolean;
};

function KursAdminStatsSection({
  stats,
  isLoading,
}: KursAdminStatsSectionProps) {
  const router = useRouter();

  // null = 403/no-course (e.g. SYSTEMADMIN without own course) — hide the section
  if (!isLoading && stats === null) return null;

  return (
    <View>
      <SectionHeader
        icon={Users}
        label={t("dashboard.kursadmin.title", "Kurs-Übersicht")}
      />
      {isLoading || !stats ? (
        <Text className="text-sm text-muted-foreground">
          {t("dashboard.kursadmin.loading", "Wird geladen …")}
        </Text>
      ) : (
        <TouchableOpacity
          onPress={() => router.push("/(app)/verwaltung")}
          accessibilityRole="button"
          className="flex-row justify-between gap-4"
        >
          <StatItem
            icon={Users}
            iconColor="hsl(var(--primary))"
            value={stats.anzahlMitglieder}
            label={t("dashboard.kursadmin.mitglieder", "Mitglieder")}
          />
          <StatItem
            icon={FolderOpen}
            iconColor="hsl(var(--success))"
            value={stats.anzahlDateien}
            label={t("dashboard.kursadmin.dateien", "Dateien")}
          />
          <StatItem
            icon={Clock}
            iconColor="hsl(var(--destructive))"
            value={stats.anzahlAnfragen}
            label={t("dashboard.kursadmin.anfragen", "Anfragen")}
            badge={stats.anzahlAnfragen > 0 ? stats.anzahlAnfragen : undefined}
          />
        </TouchableOpacity>
      )}
      <TouchableOpacity
        onPress={() => router.push("/(app)/verwaltung")}
        accessibilityRole="link"
        className="mt-3 flex-row items-center gap-1"
      >
        <Text className="text-sm text-primary">
          {t("dashboard.kursadmin.link", "Zur Verwaltung")}
        </Text>
        <ArrowRight size={13} color="hsl(var(--primary))" />
      </TouchableOpacity>
    </View>
  );
}

// ─── Sysadmin stats section ──────────────────────────────────────────────────

export type SysAdminStatsSectionProps = {
  stats: SystemAdminStatsDTO | undefined;
  isLoading: boolean;
};

function SysAdminStatsSection({ stats, isLoading }: SysAdminStatsSectionProps) {
  const router = useRouter();
  const totalStorage = stats
    ? stats.megabyteBelegt + stats.megabyteVerfuegbar
    : 1;
  const usedPercent = stats
    ? Math.min(100, (stats.megabyteBelegt / totalStorage) * 100)
    : 0;

  return (
    <View>
      <SectionHeader
        icon={Star}
        label={t("dashboard.sysadmin.title", "Plattform-Übersicht")}
      />
      {isLoading || !stats ? (
        <Text className="text-sm text-muted-foreground">
          {t("dashboard.sysadmin.loading", "Wird geladen …")}
        </Text>
      ) : (
        <TouchableOpacity
          onPress={() => router.push("/(app)/verwaltung")}
          accessibilityRole="button"
          className="flex-row flex-wrap gap-4"
        >
          <StatItem
            icon={FolderOpen}
            iconColor="hsl(var(--primary))"
            value={Number(stats.anzahlKurse)}
            label={t("dashboard.sysadmin.kurse", "Kurse aktiv")}
          />
          <StatItem
            icon={Users}
            iconColor="hsl(var(--primary))"
            value={Number(stats.anzahlNutzer)}
            label={t("dashboard.sysadmin.nutzer", "Nutzer aktiv")}
          />
          <StatItem
            icon={File}
            iconColor="hsl(var(--success))"
            value={Number(stats.anzahlDateien)}
            label={t("dashboard.sysadmin.dateien", "Dateien gesamt")}
          />
          <StatItem
            icon={Clock}
            iconColor="hsl(var(--destructive))"
            value={Number(stats.anzahlKurserstellungsanfragen)}
            label={t("dashboard.sysadmin.anfragen", "Kursanfragen")}
            badge={
              stats.anzahlKurserstellungsanfragen > 0
                ? Number(stats.anzahlKurserstellungsanfragen)
                : undefined
            }
          />
        </TouchableOpacity>
      )}
      {stats && (
        <View className="mt-4">
          <Text className="mb-1 text-xs text-muted-foreground">
            {t("dashboard.sysadmin.storage", "Speicher")}:{" "}
            {stats.megabyteBelegt.toFixed(0)} MB / {totalStorage.toFixed(0)} MB
          </Text>
          <View className="h-2 overflow-hidden rounded-full bg-secondary">
            <View
              className="h-full rounded-full bg-primary"
              style={{ width: `${usedPercent}%` }}
            />
          </View>
        </View>
      )}
      <TouchableOpacity
        onPress={() => router.push("/(app)/verwaltung")}
        accessibilityRole="link"
        className="mt-3 flex-row items-center gap-1"
      >
        <Text className="text-sm text-primary">
          {t("dashboard.sysadmin.link", "Zur Verwaltung")}
        </Text>
        <ArrowRight size={13} color="hsl(var(--primary))" />
      </TouchableOpacity>
    </View>
  );
}

// ─── Quick access ────────────────────────────────────────────────────────────

function QuickAccess({ showFiles }: { showFiles: boolean }) {
  const router = useRouter();

  return (
    <View>
      <Text className="mb-3 text-base font-semibold text-foreground">
        {t("dashboard.quickAccess.title", "Schnellzugriff")}
      </Text>
      <View className="flex-row gap-3">
        {showFiles && (
          <Button
            variant="outline"
            className="flex-1"
            onPress={() => router.push("/(app)/dateien")}
            accessibilityLabel={t(
              "dashboard.quickAccess.upload",
              "Datei hochladen",
            )}
          >
            <Upload size={15} color="hsl(var(--foreground))" />
            <Text className="text-foreground">
              {t("dashboard.quickAccess.upload", "Datei hochladen")}
            </Text>
          </Button>
        )}
        <Button
          variant="outline"
          className="flex-1"
          onPress={() => router.push("/(app)/mensa")}
          accessibilityLabel={t("dashboard.quickAccess.mensa", "Mensa öffnen")}
        >
          <Utensils size={15} color="hsl(var(--foreground))" />
          <Text className="text-foreground">
            {t("dashboard.quickAccess.mensa", "Mensa öffnen")}
          </Text>
        </Button>
      </View>
    </View>
  );
}

// ─── Screen ──────────────────────────────────────────────────────────────────

/**
 * Dashboard screen entry point — renders the role-appropriate layout.
 *
 * @returns The dashboard screen.
 */
export default function DashboardScreen() {
  const { user } = useAuth();
  const role = user?.studentType ?? "STUDENT";
  const hasKurs = Boolean(user?.kursName);
  const isSysAdmin = role === "SYSTEMADMIN";

  const meals = useDashboardMeals();
  const dateien = useDashboardDateien();
  const kursAdminStats = useKursAdminStats(
    role === "KURSADMIN" || (role === "SYSTEMADMIN" && hasKurs),
  );
  const sysAdminStats = useSysAdminStats(isSysAdmin);

  const today = formatTodayLong();

  // SYSTEMADMIN sees files even without a course
  const showFiles = hasKurs || isSysAdmin;

  return (
    <ScrollView
      className="flex-1 bg-background"
      contentContainerStyle={{ flexGrow: 1 }}
    >
      <View className="flex-1 px-5 py-6">
        <View className="w-full">
          {/* ── Header ──────────────────────────────────────── */}
          <View className="mb-5 flex-row items-start justify-between gap-2">
            <View className="flex-1 flex-row flex-wrap items-center gap-2">
              <Text className="text-2xl font-bold text-foreground">
                {`Willkommen, ${user?.name ?? "…"}`}
              </Text>
              {user?.kursName ? (
                <Badge variant="secondary">
                  <Text className="text-xs font-semibold text-secondary-foreground">
                    {user.kursName}
                  </Text>
                </Badge>
              ) : null}
            </View>
            <Text className="shrink-0 text-sm text-muted-foreground">
              {today}
            </Text>
          </View>

          {/* ── No-course / pending-request state ───────────── */}
          {!hasKurs && (
            <View className="mb-4">
              {user?.pendingKursRequest ? (
                <PendingRequestCard pending={user.pendingKursRequest} />
              ) : (
                <KursEinrichtenCard />
              )}
            </View>
          )}

          {/* ── Main content card ────────────────────────────── */}
          <Card className="mb-4 gap-0 px-5 py-5">
            {/* Sysadmin KPI block */}
            {isSysAdmin && (
              <>
                <SysAdminStatsSection
                  stats={sysAdminStats.data}
                  isLoading={sysAdminStats.isLoading}
                />
                <Separator className="my-5" />
              </>
            )}

            {/* Kursadmin / Sysadmin (with kurs): course stats — hidden when no-course (stats===null) */}
            {(role === "KURSADMIN" || (isSysAdmin && hasKurs)) &&
              (kursAdminStats.isLoading || kursAdminStats.data !== null) && (
                <>
                  <KursAdminStatsSection
                    stats={kursAdminStats.data}
                    isLoading={kursAdminStats.isLoading}
                  />
                  <Separator className="my-5" />
                </>
              )}

            {/* Mensa + Dateien side by side on wide screens */}
            <View className="gap-5 md:flex-row md:gap-6">
              <MensaPreviewCard
                meals={meals.data ?? []}
                isLoading={meals.isLoading}
                isError={meals.isError}
              />
              {showFiles && (
                <View className="hidden md:flex">
                  <Separator orientation="vertical" className="h-full" />
                </View>
              )}
              {showFiles && (
                <LatestFilesCard
                  dateien={dateien.data ?? []}
                  isLoading={dateien.isLoading}
                  isError={dateien.isError}
                />
              )}
            </View>

            {/* Quick access */}
            <Separator className="my-5" />
            <QuickAccess showFiles={showFiles} />
          </Card>
        </View>
      </View>
    </ScrollView>
  );
}
