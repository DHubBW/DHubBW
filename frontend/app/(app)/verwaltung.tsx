/**
 * Verwaltung screen — role-dependent:
 * - SYSTEMADMIN: platform KPIs, course-creation requests, courses table
 *   with per-member KA-assign/revoke inline, students table with SA-role
 *   actions and scrollable list.
 * - KURSADMIN: stats, enrollment requests, member list with role actions,
 *   resign button.
 * - Other roles: redirected by AdminRoute.
 */
import {
  AlertCircle,
  Check,
  ChevronDown,
  ChevronRight,
  File,
  BookOpen,
  HardDrive,
  Search,
  Trash2,
  UserCheck,
  UserMinus,
  Users,
  X,
} from "lucide-react-native";
import { useState } from "react";
import {
  ActivityIndicator,
  Platform,
  ScrollView,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";
import { useLocalSearchParams, useRouter } from "expo-router";
import { toast } from "sonner-native";

import { AdminRoute } from "@/components/auth/AdminRoute";
import { ConfirmDialog } from "@/components/ConfirmDialog";
import { ConfirmDialogStrict } from "@/components/ConfirmDialogStrict";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { Text } from "@/components/ui/text";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  useKursAdminAddKursadmin,
  useKursAdminApproveAnfrage,
  useKursAdminDeclineAnfrage,
  useKursAdminRemoveKursadmin,
  useKursAdminRemoveMember,
  useKursAdminResign,
} from "@/hooks/useKursAdminActions";
import { useKursAdminAnfragen } from "@/hooks/useKursAdminAnfragen";
import { useKursAdminMitglieder } from "@/hooks/useKursAdminMitglieder";
import { useKursAdminStats } from "@/hooks/useKursAdminStats";
import {
  useAddKursadminBySysAdmin,
  useRemoveKursadminBySysAdmin,
} from "@/hooks/useKursadminBySysAdmin";
import {
  useAddSysAdmin,
  useApproveAnfrage,
  useDeclineAnfrage,
  useDeleteKurs,
  useRemoveSysAdmin,
} from "@/hooks/useSysAdminActions";
import { useSysAdminAnfragen } from "@/hooks/useSysAdminAnfragen";
import { useSysAdminKurse } from "@/hooks/useSysAdminKurse";
import { useSysAdminKursMitglieder } from "@/hooks/useSysAdminKursMitglieder";
import { useSysAdminStats } from "@/hooks/useSysAdminStats";
import { useSysAdminStudenten } from "@/hooks/useSysAdminStudenten";
import { ApiError } from "@/lib/api/errors";
import type {
  KursDTO,
  KursErstellungsAnfrageDTO,
  KursmemberDTO,
  KursanfrageDTO,
  KursMitgliedRolleDTO,
  StudentDTO,
  SystemAdminStatsDTO,
} from "@/lib/api/types.gen";
import { useAuth } from "@/lib/auth/AuthContext";
import { t } from "@/lib/i18n/t";
import { cn } from "@/lib/utils";

// ─── Helpers ──────────────────────────────────────────────────────────────────

function formatStorage(mb: number): string {
  if (mb >= 1024) return `${(mb / 1024).toFixed(1)} GB`;
  return `${mb.toFixed(0)} MB`;
}

// ─── Role badge with tooltip ──────────────────────────────────────────────────

const ROLLE_CONFIG = {
  SYSTEMADMIN: {
    label: "SA",
    full: "Systemadmin",
    bg: "bg-primary",
    text: "text-primary-foreground",
    tooltipBg: "bg-primary",
    tooltipText: "text-primary-foreground",
  },
  KURSADMIN: {
    label: "KA",
    full: "Kursadmin",
    bg: "bg-[hsl(var(--warning)/0.2)]",
    text: "text-[hsl(var(--warning))]",
    tooltipBg: "bg-[hsl(var(--warning))]",
    tooltipText: "text-foreground",
  },
  STUDENT: {
    label: "S",
    full: "Student",
    bg: "bg-secondary",
    text: "text-secondary-foreground",
    tooltipBg: "bg-secondary",
    tooltipText: "text-secondary-foreground",
  },
} as const;

function RolleBadge({ rolle }: { rolle: string }) {
  const cfg =
    ROLLE_CONFIG[rolle as keyof typeof ROLLE_CONFIG] ?? ROLLE_CONFIG.STUDENT;

  const badge = (
    <View
      className={cn(
        "h-6 min-w-6 items-center justify-center rounded-full px-1.5",
        cfg.bg,
      )}
    >
      <Text className={cn("text-[11px] font-bold", cfg.text)}>{cfg.label}</Text>
    </View>
  );

  if (Platform.OS !== "web") return badge;

  return (
    <Tooltip>
      <TooltipTrigger asChild>{badge}</TooltipTrigger>
      <TooltipContent side="top" className={cfg.tooltipBg}>
        <Text className={cn("text-xs font-semibold", cfg.tooltipText)}>
          {cfg.full}
        </Text>
      </TooltipContent>
    </Tooltip>
  );
}

// ─── KPI cards ────────────────────────────────────────────────────────────────

type KpiCardProps = {
  icon: typeof Users;
  iconColor: string;
  value: string | number;
  label: string;
  progressPercent?: number;
  badge?: number;
};

function KpiCard({
  icon: Icon,
  iconColor,
  value,
  label,
  progressPercent,
  badge,
}: KpiCardProps) {
  return (
    <Card className="min-w-36 flex-1 gap-0 px-5 py-5">
      <View className="mb-3 flex-row items-center justify-between">
        <View className="relative">
          <Icon size={24} color={iconColor} strokeWidth={1.75} />
          {badge !== undefined && badge > 0 && (
            <View className="absolute -right-2 -top-2 h-4 min-w-4 items-center justify-center rounded-full bg-destructive px-0.5">
              <Text className="text-[10px] font-bold text-destructive-foreground">
                {badge}
              </Text>
            </View>
          )}
        </View>
      </View>
      <Text className="text-2xl font-bold text-foreground">{value}</Text>
      <Text className="mt-1 text-xs text-muted-foreground">{label}</Text>
      {progressPercent !== undefined && (
        <View className="mt-3 h-2 overflow-hidden rounded-full bg-secondary">
          <View
            className="h-full rounded-full bg-primary"
            style={{ width: `${progressPercent}%` }}
          />
        </View>
      )}
    </Card>
  );
}

function KpiRow({ stats }: { stats: SystemAdminStatsDTO }) {
  const totalMb = stats.megabyteBelegt + stats.megabyteVerfuegbar;
  const usedPct = totalMb > 0 ? (stats.megabyteBelegt / totalMb) * 100 : 0;
  const storageLabel = `${formatStorage(stats.megabyteBelegt)} / ${formatStorage(totalMb)}`;

  return (
    <View className="flex-row flex-wrap gap-3">
      <KpiCard
        icon={BookOpen}
        iconColor="hsl(var(--primary))"
        value={stats.anzahlKurse}
        label={t("verwaltung.kpi.kurse", "Kurse aktiv")}
      />
      <KpiCard
        icon={Users}
        iconColor="hsl(var(--primary))"
        value={stats.anzahlNutzer}
        label={t("verwaltung.kpi.nutzer", "Nutzer aktiv")}
      />
      <KpiCard
        icon={File}
        iconColor="hsl(var(--success))"
        value={stats.anzahlDateien}
        label={t("verwaltung.kpi.dateien", "Dateien gesamt")}
      />
      <KpiCard
        icon={HardDrive}
        iconColor="hsl(var(--warning))"
        value={formatStorage(stats.megabyteBelegt)}
        label={`${t("verwaltung.kpi.speicher", "Speicher")} · ${storageLabel}`}
        progressPercent={Math.min(100, usedPct)}
      />
      <KpiCard
        icon={AlertCircle}
        iconColor="hsl(var(--destructive))"
        value={stats.anzahlKurserstellungsanfragen}
        label={t("verwaltung.kpi.kursanfragen", "Offene Kursanfragen")}
        badge={
          stats.anzahlKurserstellungsanfragen > 0
            ? Number(stats.anzahlKurserstellungsanfragen)
            : undefined
        }
      />
    </View>
  );
}

// ─── Section header ───────────────────────────────────────────────────────────

function SectionHeader({
  icon: Icon,
  label,
  badge,
}: {
  icon: typeof Users;
  label: string;
  badge?: number;
}) {
  return (
    <View className="mb-4 flex-row items-center gap-2">
      <Icon size={18} color="hsl(var(--foreground))" strokeWidth={2} />
      <Text className="text-base font-semibold text-foreground">{label}</Text>
      {badge !== undefined && badge > 0 && (
        <View className="h-5 min-w-5 items-center justify-center rounded-full bg-destructive px-1">
          <Text className="text-[11px] font-bold text-destructive-foreground">
            {badge}
          </Text>
        </View>
      )}
    </View>
  );
}

// ─── Kursanfragen ─────────────────────────────────────────────────────────────

function AnfrageCard({
  anfrage,
  onApprove,
  onDecline,
}: {
  anfrage: KursErstellungsAnfrageDTO;
  onApprove: () => Promise<void>;
  onDecline: () => Promise<void>;
}) {
  const [approveOpen, setApproveOpen] = useState(false);
  const [declineOpen, setDeclineOpen] = useState(false);

  return (
    <View className="rounded-xl border border-border bg-card px-5 py-4">
      <View className="mb-1 flex-row items-center gap-2">
        <View className="h-2.5 w-2.5 rounded-full bg-destructive" />
        <Text className="text-base font-semibold text-foreground">
          {anfrage.studentName}
        </Text>
      </View>
      <Text className="mb-1 text-sm text-muted-foreground">
        {t("verwaltung.anfragen.kurs", "Kurs")}:{" "}
        <Text className="font-semibold text-foreground">
          {anfrage.kursName}
        </Text>
        {"  ·  "}
        {t("verwaltung.anfragen.jahrgang", "Jahrgang")} {anfrage.jahrgang}
        {"  ·  "}
        {t("verwaltung.anfragen.gesendet", "Gesendet")}: {anfrage.anfrageDatum}
      </Text>
      {anfrage.requestNotes ? (
        <Text className="mb-3 text-sm italic text-muted-foreground">
          &ldquo;{anfrage.requestNotes}&rdquo;
        </Text>
      ) : (
        <View className="mb-3" />
      )}
      <View className="flex-row gap-3">
        <Button
          variant="default"
          className="flex-1"
          onPress={() => setApproveOpen(true)}
        >
          <Check size={14} color="white" />
          <Text className="text-sm text-primary-foreground">
            {t("verwaltung.anfragen.genehmigen", "Genehmigen")}
          </Text>
        </Button>
        <Button
          variant="outline"
          className="flex-1 border-destructive"
          onPress={() => setDeclineOpen(true)}
        >
          <X size={14} color="hsl(var(--destructive))" />
          <Text className="text-sm text-destructive">
            {t("verwaltung.anfragen.ablehnen", "Ablehnen")}
          </Text>
        </Button>
      </View>

      <ConfirmDialog
        open={approveOpen}
        onOpenChange={setApproveOpen}
        title={t("verwaltung.anfragen.approveTitle", "Kursanfrage genehmigen")}
        message={t(
          "verwaltung.anfragen.approveMsg",
          `Kurs "${anfrage.kursName}" (${anfrage.jahrgang}) wird erstellt und ${anfrage.studentName} zum Kursadmin ernannt.`,
        )}
        confirmLabel={t("verwaltung.anfragen.genehmigen", "Genehmigen")}
        onConfirm={onApprove}
      />
      <ConfirmDialog
        open={declineOpen}
        onOpenChange={setDeclineOpen}
        title={t("verwaltung.anfragen.declineTitle", "Kursanfrage ablehnen")}
        message={t(
          "verwaltung.anfragen.declineMsg",
          `Die Anfrage von ${anfrage.studentName} für Kurs "${anfrage.kursName}" wird abgelehnt.`,
        )}
        confirmLabel={t("verwaltung.anfragen.ablehnen", "Ablehnen")}
        variant="destructive"
        onConfirm={onDecline}
      />
    </View>
  );
}

function KursanfragenSection({
  anfragen,
  isLoading,
}: {
  anfragen: KursErstellungsAnfrageDTO[];
  isLoading: boolean;
}) {
  const { approveAnfrage } = useApproveAnfrage();
  const { declineAnfrage } = useDeclineAnfrage();

  if (isLoading)
    return (
      <View className="items-center py-8">
        <ActivityIndicator />
      </View>
    );
  if (anfragen.length === 0)
    return (
      <Text className="py-2 text-sm text-muted-foreground">
        {t("verwaltung.anfragen.empty", "Keine offenen Kursanfragen.")}
      </Text>
    );

  return (
    <View className="gap-3">
      {anfragen.map((anfrage) => (
        <AnfrageCard
          key={anfrage.anfrageID}
          anfrage={anfrage}
          onApprove={async () => {
            try {
              await approveAnfrage(anfrage.anfrageID);
              toast.success(
                t(
                  "verwaltung.anfragen.approveSuccess",
                  `Kurs "${anfrage.kursName}" genehmigt`,
                ),
              );
            } catch (err) {
              if (err instanceof ApiError && err.status === 409) {
                toast.error(
                  err instanceof ApiError && err.message
                    ? err.message
                    : t(
                        "verwaltung.anfragen.approveError",
                        "Genehmigung fehlgeschlagen",
                      ),
                );
              } else {
                toast.error(
                  t(
                    "verwaltung.anfragen.approveError",
                    "Genehmigung fehlgeschlagen",
                  ),
                );
              }
              throw err;
            }
          }}
          onDecline={async () => {
            await declineAnfrage(anfrage.anfrageID);
            toast.success(t("verwaltung.anfragen.declineSuccess", "Abgelehnt"));
          }}
        />
      ))}
    </View>
  );
}

// ─── Member row with inline KA-assign / KA-revoke ────────────────────────────

function MitgliedRow({
  mitglied,
  kursId,
  onAddKa,
  onRemoveKa,
}: {
  mitglied: KursMitgliedRolleDTO;
  kursId: string;
  onAddKa: () => Promise<void>;
  onRemoveKa: () => Promise<void>;
}) {
  const [addKaOpen, setAddKaOpen] = useState(false);
  const [removeKaOpen, setRemoveKaOpen] = useState(false);

  const isKa = mitglied.rolle === "KURSADMIN";
  const isSa = mitglied.rolle === "SYSTEMADMIN";

  return (
    <View className="flex-row items-center gap-3 py-2.5">
      <View className="h-8 w-8 shrink-0 items-center justify-center rounded-full bg-secondary">
        <Text className="text-xs font-semibold text-secondary-foreground">
          {mitglied.studentName.slice(0, 2).toUpperCase()}
        </Text>
      </View>
      <Text className="flex-1 text-sm text-foreground" numberOfLines={1}>
        {mitglied.studentName}
      </Text>
      <RolleBadge rolle={mitglied.rolle} />

      {!isSa && (
        <>
          {!isKa ? (
            <TouchableOpacity
              onPress={() => setAddKaOpen(true)}
              accessibilityRole="button"
              className="rounded border border-border px-2 py-1"
            >
              <UserCheck size={13} color="hsl(var(--primary))" />
            </TouchableOpacity>
          ) : (
            <TouchableOpacity
              onPress={() => setRemoveKaOpen(true)}
              accessibilityRole="button"
              className="rounded border border-destructive/50 px-2 py-1"
            >
              <UserMinus size={13} color="hsl(var(--destructive))" />
            </TouchableOpacity>
          )}

          <ConfirmDialog
            open={addKaOpen}
            onOpenChange={setAddKaOpen}
            title={t("verwaltung.kurse.addKaTitle", "Zum Kursadmin ernennen")}
            message={t(
              "verwaltung.kurse.addKaMsg",
              `${mitglied.studentName} wird Kursadmin für diesen Kurs.`,
            )}
            confirmLabel={t("verwaltung.kurse.addKaBtn", "Ernennen")}
            onConfirm={async () => {
              try {
                await onAddKa();
                toast.success(
                  t(
                    "verwaltung.kurse.addKaSuccess",
                    `${mitglied.studentName} ist jetzt Kursadmin`,
                  ),
                );
              } catch (err) {
                if (err instanceof ApiError && err.status === 409) {
                  toast.error(
                    t("verwaltung.kurse.alreadyKa", "Bereits Kursadmin"),
                  );
                } else {
                  toast.error(
                    t("verwaltung.kurse.actionError", "Aktion fehlgeschlagen"),
                  );
                }
                throw err;
              }
            }}
          />
          <ConfirmDialog
            open={removeKaOpen}
            onOpenChange={setRemoveKaOpen}
            title={t(
              "verwaltung.kurse.removeKaTitle",
              "Kursadmin-Rolle entziehen",
            )}
            message={t(
              "verwaltung.kurse.removeKaMsg",
              `${mitglied.studentName} wird auf Student zurückgestuft.`,
            )}
            confirmLabel={t("verwaltung.kurse.removeKaBtn", "Entziehen")}
            variant="destructive"
            onConfirm={async () => {
              try {
                await onRemoveKa();
                toast.success(
                  t(
                    "verwaltung.kurse.removeKaSuccess",
                    `${mitglied.studentName} ist kein Kursadmin mehr`,
                  ),
                );
              } catch (err) {
                if (err instanceof ApiError && err.status === 409) {
                  toast.error(
                    t(
                      "verwaltung.kurse.lastKaError",
                      "Letzter Kursadmin — Entziehen nicht möglich",
                    ),
                  );
                } else {
                  toast.error(
                    t("verwaltung.kurse.actionError", "Aktion fehlgeschlagen"),
                  );
                }
                throw err;
              }
            }}
          />
        </>
      )}
    </View>
  );
}

// ─── Kurs row with expanded member drawer ────────────────────────────────────

function KursRow({ kurs }: { kurs: KursDTO }) {
  const [expanded, setExpanded] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const { deleteKurs } = useDeleteKurs();
  const { addKursadmin } = useAddKursadminBySysAdmin();
  const { removeKursadmin } = useRemoveKursadminBySysAdmin();

  const { data: mitglieder, isLoading: mitgliederLoading } =
    useSysAdminKursMitglieder(kurs.kursID);

  const kaList = mitglieder?.filter((m) => m.rolle === "KURSADMIN") ?? [];
  const kaNames = kaList.map((k) => k.studentName).join(", ");

  return (
    <View>
      <TouchableOpacity
        onPress={() => setExpanded((v) => !v)}
        accessibilityRole="button"
        className="flex-row items-center justify-between py-3.5"
      >
        <View className="flex-1 pr-3">
          <Text className="text-base font-semibold text-foreground">
            {kurs.kursName}
          </Text>
          <Text className="text-sm text-muted-foreground">
            {t("verwaltung.kurse.jahrgang", "Jahrgang")} {kurs.jahrgang}
            {kaNames
              ? `  ·  ${kaNames}`
              : `  ·  ${t("verwaltung.kurse.noKa", "(kein Kursadmin)")}`}
          </Text>
        </View>
        {expanded ? (
          <ChevronDown size={18} color="hsl(var(--muted-foreground))" />
        ) : (
          <ChevronRight size={18} color="hsl(var(--muted-foreground))" />
        )}
      </TouchableOpacity>

      {expanded && (
        <View className="mb-3 rounded-xl border border-border bg-muted/30 px-4 py-4">
          <View className="mb-2 flex-row items-center justify-between">
            <Text className="text-sm font-semibold text-foreground">
              {t("verwaltung.kurse.mitglieder", "Mitglieder")}
            </Text>
            <TouchableOpacity
              onPress={() => setExpanded(false)}
              accessibilityRole="button"
            >
              <ChevronDown size={16} color="hsl(var(--muted-foreground))" />
            </TouchableOpacity>
          </View>

          {mitgliederLoading ? (
            <ActivityIndicator size="small" />
          ) : !mitglieder || mitglieder.length === 0 ? (
            <Text className="mb-3 text-sm text-muted-foreground">
              {t("verwaltung.kurse.noMembers", "Keine Mitglieder.")}
            </Text>
          ) : (
            <View className="mb-3">
              {mitglieder.map((m: KursMitgliedRolleDTO) => (
                <MitgliedRow
                  key={m.studentID}
                  mitglied={m}
                  kursId={kurs.kursID}
                  onAddKa={() => addKursadmin(kurs.kursID, m.studentID)}
                  onRemoveKa={() => removeKursadmin(kurs.kursID, m.studentID)}
                />
              ))}
            </View>
          )}

          <Separator className="mb-3 opacity-40" />
          <Button
            variant="destructive"
            size="sm"
            onPress={() => setDeleteOpen(true)}
          >
            <Trash2 size={13} color="white" />
            <Text className="text-xs text-destructive-foreground">
              {t("verwaltung.kurse.loeschen", "Kurs löschen")}
            </Text>
          </Button>
        </View>
      )}

      <ConfirmDialogStrict
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        title={t("verwaltung.kurse.deleteTitle", "Kurs löschen")}
        message={t(
          "verwaltung.kurse.deleteMsg",
          `Alle Mitgliedschaften, private Ordner und Dateien des Kurses "${kurs.kursName}" werden unwiderruflich gelöscht.`,
        )}
        verificationText={kurs.kursName}
        confirmLabel={t("verwaltung.kurse.loeschen", "Kurs löschen")}
        onConfirm={async () => {
          try {
            await deleteKurs(kurs.kursID);
            setExpanded(false);
            toast.success(
              t(
                "verwaltung.kurse.deleteSuccess",
                `Kurs "${kurs.kursName}" gelöscht`,
              ),
            );
          } catch {
            toast.error(t("verwaltung.kurse.deleteError", "Kurs löschen fehlgeschlagen"));
          }
        }}
      />
    </View>
  );
}

function KurseSection({
  kurse,
  isLoading,
}: {
  kurse: KursDTO[];
  isLoading: boolean;
}) {
  if (isLoading)
    return (
      <View className="items-center py-8">
        <ActivityIndicator />
      </View>
    );
  if (kurse.length === 0)
    return (
      <Text className="py-2 text-sm text-muted-foreground">
        {t("verwaltung.kurse.empty", "Keine Kurse vorhanden.")}
      </Text>
    );

  return (
    <View>
      {kurse.map((kurs, idx) => (
        <View key={kurs.kursID}>
          {idx > 0 && <Separator className="opacity-40" />}
          <KursRow kurs={kurs} />
        </View>
      ))}
    </View>
  );
}

// ─── Studenten section ────────────────────────────────────────────────────────

function StudentRow({
  student,
  saCount,
}: {
  student: StudentDTO;
  saCount: number;
}) {
  const [addOpen, setAddOpen] = useState(false);
  const [removeOpen, setRemoveOpen] = useState(false);
  const { addSysAdmin } = useAddSysAdmin();
  const { removeSysAdmin } = useRemoveSysAdmin();

  const isSa = student.studentType === "SYSTEMADMIN";
  const isLastSa = isSa && saCount <= 1;

  return (
    <View className="flex-row items-center gap-3 py-3">
      <View className="h-9 w-9 shrink-0 items-center justify-center rounded-full bg-secondary">
        <Text className="text-sm font-semibold text-secondary-foreground">
          {student.name.slice(0, 2).toUpperCase()}
        </Text>
      </View>

      <View className="flex-1">
        <Text
          className="text-sm font-semibold text-foreground"
          numberOfLines={1}
        >
          {student.name}
        </Text>
        <Text className="text-xs text-muted-foreground" numberOfLines={1}>
          {student.dhbwMail}
        </Text>
      </View>

      <RolleBadge rolle={student.studentType} />

      {!isSa ? (
        <>
          <TouchableOpacity
            onPress={() => setAddOpen(true)}
            accessibilityRole="button"
            className="rounded border border-border px-2 py-1"
          >
            <Text className="text-xs text-primary">
              {t("verwaltung.studenten.addSaShort", "SA ernennen")}
            </Text>
          </TouchableOpacity>
          <ConfirmDialog
            open={addOpen}
            onOpenChange={setAddOpen}
            title={t(
              "verwaltung.studenten.addSaTitle",
              "Zum Systemadmin ernennen",
            )}
            message={t(
              "verwaltung.studenten.addSaMsg",
              `${student.name} erhält die Systemadmin-Rolle.`,
            )}
            confirmLabel={t("verwaltung.studenten.addSa", "SA ernennen")}
            onConfirm={async () => {
              try {
                await addSysAdmin(student.studentID);
                toast.success(
                  t(
                    "verwaltung.studenten.addSaSuccess",
                    `${student.name} ist jetzt Systemadmin`,
                  ),
                );
              } catch (err) {
                if (err instanceof ApiError && err.status === 409) {
                  toast.error(
                    t(
                      "verwaltung.studenten.alreadySa",
                      "Bereits Systemadmin",
                    ),
                  );
                } else {
                  toast.error(
                    t(
                      "verwaltung.studenten.actionError",
                      "Aktion fehlgeschlagen",
                    ),
                  );
                }
                throw err;
              }
            }}
          />
        </>
      ) : (
        <>
          <TouchableOpacity
            onPress={() => !isLastSa && setRemoveOpen(true)}
            accessibilityRole="button"
            disabled={isLastSa}
            className={cn(
              "rounded border px-2 py-1",
              isLastSa ? "border-muted opacity-40" : "border-destructive/50",
            )}
          >
            <Text
              className={cn(
                "text-xs",
                isLastSa ? "text-muted-foreground" : "text-destructive",
              )}
            >
              {t("verwaltung.studenten.removeSaShort", "SA entziehen")}
            </Text>
          </TouchableOpacity>
          <ConfirmDialog
            open={removeOpen}
            onOpenChange={setRemoveOpen}
            title={t(
              "verwaltung.studenten.removeSaTitle",
              "Systemadmin entziehen",
            )}
            message={t(
              "verwaltung.studenten.removeSaMsg",
              `${student.name} wird auf Student zurückgestuft.`,
            )}
            confirmLabel={t(
              "verwaltung.studenten.removeSa",
              "SA-Rolle entziehen",
            )}
            variant="destructive"
            onConfirm={async () => {
              try {
                await removeSysAdmin(student.studentID);
                toast.success(
                  t(
                    "verwaltung.studenten.removeSaSuccess",
                    `${student.name} ist kein Systemadmin mehr`,
                  ),
                );
              } catch (err) {
                if (err instanceof ApiError && err.status === 409) {
                  toast.error(
                    t(
                      "verwaltung.studenten.lastSaError",
                      "Aktion fehlgeschlagen — diese Rolle ist die letzte",
                    ),
                  );
                } else {
                  toast.error(
                    t(
                      "verwaltung.studenten.actionError",
                      "Aktion fehlgeschlagen",
                    ),
                  );
                }
                throw err;
              }
            }}
          />
        </>
      )}
    </View>
  );
}

function StudentenSection({
  studenten,
  isLoading,
}: {
  studenten: StudentDTO[];
  isLoading: boolean;
}) {
  const [search, setSearch] = useState("");

  const saCount = studenten.filter(
    (s) => s.studentType === "SYSTEMADMIN",
  ).length;

  const filtered = search.trim()
    ? studenten.filter(
        (s) =>
          s.name.toLowerCase().includes(search.toLowerCase()) ||
          s.dhbwMail.toLowerCase().includes(search.toLowerCase()),
      )
    : studenten;

  return (
    <View>
      <View className="mb-3 flex-row items-center gap-2 rounded-lg border border-border bg-background px-3 py-2.5">
        <Search size={15} color="hsl(var(--muted-foreground))" />
        <TextInput
          value={search}
          onChangeText={setSearch}
          placeholder={t(
            "verwaltung.studenten.search",
            "Name oder E-Mail suchen …",
          )}
          placeholderTextColor="hsl(var(--muted-foreground))"
          className="flex-1 text-sm text-foreground"
          autoCapitalize="none"
          autoCorrect={false}
          clearButtonMode="while-editing"
        />
      </View>

      {isLoading ? (
        <View className="items-center py-8">
          <ActivityIndicator />
        </View>
      ) : filtered.length === 0 ? (
        <Text className="py-2 text-sm text-muted-foreground">
          {search.trim()
            ? t("verwaltung.studenten.noResults", "Keine Treffer.")
            : t("verwaltung.studenten.empty", "Keine Studenten vorhanden.")}
        </Text>
      ) : (
        <ScrollView
          className="max-h-96"
          nestedScrollEnabled
          showsVerticalScrollIndicator
        >
          {filtered.map((student, idx) => (
            <View key={student.studentID}>
              {idx > 0 && <Separator className="opacity-30" />}
              <StudentRow student={student} saCount={saCount} />
            </View>
          ))}
        </ScrollView>
      )}
    </View>
  );
}

// ─── Systemverwaltung content ─────────────────────────────────────────────────

function SystemverwaltungContent() {
  const stats = useSysAdminStats();
  const anfragen = useSysAdminAnfragen();
  const kurse = useSysAdminKurse();
  const studenten = useSysAdminStudenten();

  const anfrageCount = anfragen.data?.length ?? 0;

  return (
    <ScrollView
      className="flex-1 bg-background"
      contentContainerStyle={{ flexGrow: 1 }}
    >
      <View className="flex-1 gap-5 px-5 py-6">
        <Text className="text-2xl font-bold text-foreground">
          {t("verwaltung.title", "Systemverwaltung")}
        </Text>

        {stats.isLoading ? (
          <View className="items-center py-4">
            <ActivityIndicator />
          </View>
        ) : stats.isError ? (
          <View className="flex-row items-center gap-2 rounded-xl bg-destructive/10 px-4 py-3">
            <AlertCircle size={16} color="hsl(var(--destructive))" />
            <Text className="text-sm text-destructive">
              {t(
                "verwaltung.stats.error",
                "KPIs konnten nicht geladen werden.",
              )}
            </Text>
          </View>
        ) : stats.data ? (
          <KpiRow stats={stats.data} />
        ) : null}

        <Card className="gap-0 px-5 py-5">
          <SectionHeader
            icon={AlertCircle}
            label={t("verwaltung.anfragen.title", "Kursanfragen")}
            badge={anfrageCount}
          />
          <KursanfragenSection
            anfragen={anfragen.data ?? []}
            isLoading={anfragen.isLoading}
          />
        </Card>

        <Card className="gap-0 px-5 py-5">
          <SectionHeader
            icon={BookOpen}
            label={t("verwaltung.kurse.title", "Kurse")}
          />
          <KurseSection kurse={kurse.data ?? []} isLoading={kurse.isLoading} />
        </Card>

        <Card className="gap-0 px-5 py-5">
          <SectionHeader
            icon={Users}
            label={t("verwaltung.studenten.title", "Studenten (alle)")}
          />
          <StudentenSection
            studenten={studenten.data ?? []}
            isLoading={studenten.isLoading}
          />
        </Card>
      </View>
    </ScrollView>
  );
}

// ─── KURSADMIN view ───────────────────────────────────────────────────────────

function KaAnfrageCard({
  anfrage,
  onApprove,
  onDecline,
}: {
  anfrage: KursanfrageDTO;
  onApprove: () => Promise<void>;
  onDecline: () => Promise<void>;
}) {
  const [approveOpen, setApproveOpen] = useState(false);
  const [declineOpen, setDeclineOpen] = useState(false);

  return (
    <View className="rounded-xl border border-border bg-card px-5 py-4">
      <View className="mb-1 flex-row items-center gap-2">
        <View className="h-2.5 w-2.5 rounded-full bg-destructive" />
        <Text className="text-base font-semibold text-foreground">
          {anfrage.studentName}
        </Text>
      </View>
      <Text className="mb-3 text-sm text-muted-foreground">
        {t("verwaltung.ka.anfragen.datum", "Eingereicht am")}:{" "}
        {anfrage.anfrageDatum}
      </Text>
      <View className="flex-row gap-3">
        <Button
          variant="default"
          className="flex-1"
          onPress={() => setApproveOpen(true)}
        >
          <Check size={14} color="white" />
          <Text className="text-sm text-primary-foreground">
            {t("verwaltung.ka.anfragen.annehmen", "Annehmen")}
          </Text>
        </Button>
        <Button
          variant="outline"
          className="flex-1 border-destructive"
          onPress={() => setDeclineOpen(true)}
        >
          <X size={14} color="hsl(var(--destructive))" />
          <Text className="text-sm text-destructive">
            {t("verwaltung.ka.anfragen.ablehnen", "Ablehnen")}
          </Text>
        </Button>
      </View>

      <ConfirmDialog
        open={approveOpen}
        onOpenChange={setApproveOpen}
        title={t("verwaltung.ka.anfragen.approveTitle", "Anfrage annehmen")}
        message={t(
          "verwaltung.ka.anfragen.approveMsg",
          `${anfrage.studentName} wird dem Kurs hinzugefügt.`,
        )}
        confirmLabel={t("verwaltung.ka.anfragen.annehmen", "Annehmen")}
        onConfirm={onApprove}
      />
      <ConfirmDialog
        open={declineOpen}
        onOpenChange={setDeclineOpen}
        title={t("verwaltung.ka.anfragen.declineTitle", "Anfrage ablehnen")}
        message={t(
          "verwaltung.ka.anfragen.declineMsg",
          `Die Beitrittsanfrage von ${anfrage.studentName} wird abgelehnt.`,
        )}
        confirmLabel={t("verwaltung.ka.anfragen.ablehnen", "Ablehnen")}
        variant="destructive"
        onConfirm={onDecline}
      />
    </View>
  );
}

function KaAnfragenSection({
  anfragen,
  isLoading,
}: {
  anfragen: KursanfrageDTO[];
  isLoading: boolean;
}) {
  const { approveAnfrage } = useKursAdminApproveAnfrage();
  const { declineAnfrage } = useKursAdminDeclineAnfrage();

  if (isLoading)
    return (
      <View className="items-center py-8">
        <ActivityIndicator />
      </View>
    );
  if (anfragen.length === 0)
    return (
      <Text className="py-2 text-sm text-muted-foreground">
        {t("verwaltung.ka.anfragen.empty", "Keine offenen Anfragen.")}
      </Text>
    );

  return (
    <View className="gap-3">
      {anfragen.map((anfrage) => (
        <KaAnfrageCard
          key={anfrage.anfrageID}
          anfrage={anfrage}
          onApprove={async () => {
            try {
              await approveAnfrage(anfrage.anfrageID);
              toast.success(
                t(
                  "verwaltung.ka.anfragen.approveSuccess",
                  `${anfrage.studentName} aufgenommen`,
                ),
              );
            } catch (err) {
              if (err instanceof ApiError && err.status === 409) {
                toast.error(
                  err.message ?? t("verwaltung.ka.error", "Aktion fehlgeschlagen"),
                );
              } else {
                toast.error(t("verwaltung.ka.error", "Aktion fehlgeschlagen"));
              }
              throw err;
            }
          }}
          onDecline={async () => {
            await declineAnfrage(anfrage.anfrageID);
            toast.success(t("verwaltung.ka.anfragen.declineSuccess", "Abgelehnt"));
          }}
        />
      ))}
    </View>
  );
}

function KaMitgliedRow({
  mitglied,
  currentUserMail,
  onSelfDemoted,
}: {
  mitglied: KursmemberDTO;
  currentUserMail: string | undefined;
  onSelfDemoted?: () => void;
}) {
  const [addKaOpen, setAddKaOpen] = useState(false);
  const [removeKaOpen, setRemoveKaOpen] = useState(false);
  const [removeOpen, setRemoveOpen] = useState(false);
  const { addKursadmin } = useKursAdminAddKursadmin();
  const { removeKursadmin } = useKursAdminRemoveKursadmin();
  const { removeMember } = useKursAdminRemoveMember();

  const isKa = mitglied.studentType === "KURSADMIN";
  const isSa = mitglied.studentType === "SYSTEMADMIN";
  const isSelf = mitglied.dhbwMail === currentUserMail;

  const actionButton = (
    icon: React.ReactNode,
    onPress: () => void,
    borderClass: string,
    tooltip: string,
  ) => {
    const btn = (
      <TouchableOpacity
        onPress={onPress}
        accessibilityRole="button"
        className={cn("rounded border px-2 py-1", borderClass)}
      >
        {icon}
      </TouchableOpacity>
    );
    if (Platform.OS !== "web") return btn;
    return (
      <Tooltip>
        <TooltipTrigger asChild>{btn}</TooltipTrigger>
        <TooltipContent side="top">
          <Text className="text-xs font-semibold text-primary-foreground">
            {tooltip}
          </Text>
        </TooltipContent>
      </Tooltip>
    );
  };

  return (
    <View className="flex-row items-center gap-3 py-2.5">
      <View className="h-9 w-9 shrink-0 items-center justify-center rounded-full bg-secondary">
        <Text className="text-sm font-semibold text-secondary-foreground">
          {mitglied.name.slice(0, 2).toUpperCase()}
        </Text>
      </View>
      <View className="flex-1">
        <Text
          className="text-sm font-semibold text-foreground"
          numberOfLines={1}
        >
          {mitglied.name}
        </Text>
        <Text className="text-xs text-muted-foreground" numberOfLines={1}>
          {mitglied.dhbwMail}
        </Text>
      </View>
      <RolleBadge rolle={mitglied.studentType} />

      {isSa ? null : (
        <>
          {!isKa
            ? actionButton(
                <UserCheck size={13} color="hsl(var(--primary))" />,
                () => setAddKaOpen(true),
                "border-border",
                t("verwaltung.ka.mitglieder.addKaBtn", "Ernennen"),
              )
            : actionButton(
                <UserMinus size={13} color="hsl(var(--destructive))" />,
                () => setRemoveKaOpen(true),
                "border-destructive/50",
                t("verwaltung.ka.mitglieder.removeKaBtn", "Entziehen"),
              )}

          {!isKa &&
            !isSelf &&
            actionButton(
              <Trash2 size={13} color="hsl(var(--destructive))" />,
              () => setRemoveOpen(true),
              "border-destructive/50",
              t("verwaltung.ka.mitglieder.removeBtn", "Entfernen"),
            )}

          <ConfirmDialog
            open={addKaOpen}
            onOpenChange={setAddKaOpen}
            title={t(
              "verwaltung.ka.mitglieder.addKaTitle",
              "Zum Kursadmin ernennen",
            )}
            message={t(
              "verwaltung.ka.mitglieder.addKaMsg",
              `${mitglied.name} erhält die Kursadmin-Rolle.`,
            )}
            confirmLabel={t("verwaltung.ka.mitglieder.addKaBtn", "Ernennen")}
            onConfirm={async () => {
              try {
                await addKursadmin(mitglied.studentID);
                toast.success(
                  t(
                    "verwaltung.ka.mitglieder.addKaSuccess",
                    `${mitglied.name} ist jetzt Kursadmin`,
                  ),
                );
              } catch (err) {
                if (err instanceof ApiError && err.status === 409) {
                  toast.error(
                    t(
                      "verwaltung.ka.mitglieder.alreadyKa",
                      "Bereits Kursadmin",
                    ),
                  );
                } else {
                  toast.error(
                    t("verwaltung.ka.error", "Aktion fehlgeschlagen"),
                  );
                }
                throw err;
              }
            }}
          />
          <ConfirmDialog
            open={removeKaOpen}
            onOpenChange={setRemoveKaOpen}
            title={t(
              "verwaltung.ka.mitglieder.removeKaTitle",
              "Kursadmin-Rolle entziehen",
            )}
            message={t(
              "verwaltung.ka.mitglieder.removeKaMsg",
              `${mitglied.name} wird auf Student zurückgestuft.`,
            )}
            confirmLabel={t(
              "verwaltung.ka.mitglieder.removeKaBtn",
              "Entziehen",
            )}
            variant="destructive"
            onConfirm={async () => {
              try {
                await removeKursadmin(mitglied.studentID);
                toast.success(
                  t(
                    "verwaltung.ka.mitglieder.removeKaSuccess",
                    `${mitglied.name} ist kein Kursadmin mehr`,
                  ),
                );
                if (isSelf) onSelfDemoted?.();
              } catch (err) {
                if (err instanceof ApiError && err.status === 409) {
                  toast.error(
                    t(
                      "verwaltung.ka.mitglieder.lastKaError",
                      "Mind. 1 Kursadmin pro Kurs erforderlich",
                    ),
                  );
                } else {
                  toast.error(
                    t("verwaltung.ka.error", "Aktion fehlgeschlagen"),
                  );
                }
                throw err;
              }
            }}
          />
          <ConfirmDialog
            open={removeOpen}
            onOpenChange={setRemoveOpen}
            title={t(
              "verwaltung.ka.mitglieder.removeTitle",
              "Aus Kurs entfernen",
            )}
            message={t(
              "verwaltung.ka.mitglieder.removeMsg",
              `${mitglied.name} wird aus dem Kurs entfernt.`,
            )}
            confirmLabel={t(
              "verwaltung.ka.mitglieder.removeBtn",
              "Entfernen",
            )}
            variant="destructive"
            onConfirm={async () => {
              try {
                await removeMember(mitglied.studentID);
                toast.success(
                  t(
                    "verwaltung.ka.mitglieder.removeSuccess",
                    `${mitglied.name} entfernt`,
                  ),
                );
              } catch (err) {
                if (err instanceof ApiError && err.status === 409) {
                  toast.error(
                    t(
                      "verwaltung.ka.mitglieder.removeRoleError",
                      "Kursadmins müssen zuerst zurückgestuft werden",
                    ),
                  );
                } else {
                  toast.error(
                    t("verwaltung.ka.error", "Aktion fehlgeschlagen"),
                  );
                }
                throw err;
              }
            }}
          />
        </>
      )}
    </View>
  );
}

function KaMitgliederSection({
  mitglieder,
  isLoading,
  currentUserMail,
  onSelfDemoted,
}: {
  mitglieder: KursmemberDTO[];
  isLoading: boolean;
  currentUserMail: string | undefined;
  onSelfDemoted?: () => void;
}) {
  const [search, setSearch] = useState("");

  const filtered = search.trim()
    ? mitglieder.filter(
        (m) =>
          m.name.toLowerCase().includes(search.toLowerCase()) ||
          m.dhbwMail.toLowerCase().includes(search.toLowerCase()),
      )
    : mitglieder;

  return (
    <View>
      <View className="mb-3 flex-row items-center gap-2 rounded-lg border border-border bg-background px-3 py-2.5">
        <Search size={15} color="hsl(var(--muted-foreground))" />
        <TextInput
          value={search}
          onChangeText={setSearch}
          placeholder={t(
            "verwaltung.ka.mitglieder.search",
            "Name oder E-Mail suchen …",
          )}
          placeholderTextColor="hsl(var(--muted-foreground))"
          className="flex-1 text-sm text-foreground"
          autoCapitalize="none"
          autoCorrect={false}
          clearButtonMode="while-editing"
        />
      </View>

      {isLoading ? (
        <View className="items-center py-8">
          <ActivityIndicator />
        </View>
      ) : filtered.length === 0 ? (
        <Text className="py-2 text-sm text-muted-foreground">
          {search.trim()
            ? t("verwaltung.ka.mitglieder.noResults", "Keine Treffer.")
            : t("verwaltung.ka.mitglieder.empty", "Keine Mitglieder.")}
        </Text>
      ) : (
        <ScrollView
          className="max-h-96"
          nestedScrollEnabled
          showsVerticalScrollIndicator
        >
          {filtered.map((m, idx) => (
            <View key={m.studentID}>
              {idx > 0 && <Separator className="opacity-30" />}
              <KaMitgliedRow mitglied={m} currentUserMail={currentUserMail} onSelfDemoted={onSelfDemoted} />
            </View>
          ))}
        </ScrollView>
      )}
    </View>
  );
}

function KursadminContent() {
  const { user, refresh } = useAuth();
  const router = useRouter();
  const [resignOpen, setResignOpen] = useState(false);
  const { resign } = useKursAdminResign();

  const stats = useKursAdminStats();
  const anfragen = useKursAdminAnfragen();
  const mitglieder = useKursAdminMitglieder();

  const anfrageCount = anfragen.data?.length ?? 0;

  return (
    <ScrollView
      className="flex-1 bg-background"
      contentContainerStyle={{ flexGrow: 1 }}
    >
      <View className="flex-1 gap-5 px-5 py-6">
        {/* Header */}
        <Text className="text-2xl font-bold text-foreground">
          {t("verwaltung.ka.title", "Kursverwaltung")}
        </Text>

        {/* KPI-Kacheln inkl. Kursname */}
        {stats.isLoading ? (
          <View className="items-center py-4">
            <ActivityIndicator />
          </View>
        ) : stats.isError ? (
          <View className="flex-row items-center gap-2 rounded-xl bg-destructive/10 px-4 py-3">
            <AlertCircle size={16} color="hsl(var(--destructive))" />
            <Text className="text-sm text-destructive">
              {t(
                "verwaltung.stats.error",
                "KPIs konnten nicht geladen werden.",
              )}
            </Text>
          </View>
        ) : stats.data ? (
          <View className="flex-row flex-wrap gap-3">
            {user?.kursName ? (
              <KpiCard
                icon={BookOpen}
                iconColor="hsl(var(--primary))"
                value={user.kursName}
                label={t("verwaltung.ka.kursname", "Dein Kurs")}
              />
            ) : null}
            <KpiCard
              icon={Users}
              iconColor="hsl(var(--primary))"
              value={stats.data.anzahlMitglieder}
              label={t("verwaltung.ka.stats.mitglieder", "Mitglieder")}
            />
            <KpiCard
              icon={File}
              iconColor="hsl(var(--success))"
              value={stats.data.anzahlDateien}
              label={t("verwaltung.ka.stats.dateien", "Dateien")}
            />
            <KpiCard
              icon={AlertCircle}
              iconColor="hsl(var(--destructive))"
              value={stats.data.anzahlAnfragen}
              label={t("verwaltung.ka.stats.anfragen", "Offene Anfragen")}
              badge={
                stats.data.anzahlAnfragen > 0
                  ? stats.data.anzahlAnfragen
                  : undefined
              }
            />
          </View>
        ) : null}

        <Card className="gap-0 px-5 py-5">
          <SectionHeader
            icon={AlertCircle}
            label={t("verwaltung.ka.anfragen.title", "Beitrittsanfragen")}
            badge={anfrageCount}
          />
          <KaAnfragenSection
            anfragen={anfragen.data ?? []}
            isLoading={anfragen.isLoading}
          />
        </Card>

        <Card className="gap-0 px-5 py-5">
          <SectionHeader
            icon={Users}
            label={t("verwaltung.ka.mitglieder.title", "Mitglieder")}
          />
          <KaMitgliederSection
            mitglieder={mitglieder.data ?? []}
            isLoading={mitglieder.isLoading}
            currentUserMail={user?.dhbwMail}
            onSelfDemoted={async () => {
              await refresh();
              router.replace("/(app)/dashboard");
            }}
          />
        </Card>
      </View>

      <ConfirmDialog
        open={resignOpen}
        onOpenChange={setResignOpen}
        title={t("verwaltung.ka.resignTitle", "Als Kursadmin zurücktreten")}
        message={t(
          "verwaltung.ka.resignMsg",
          "Du wirst zum normalen Mitglied. Es muss mindestens ein weiterer Kursadmin im Kurs sein.",
        )}
        confirmLabel={t("verwaltung.ka.resignConfirm", "Zurücktreten")}
        variant="destructive"
        onConfirm={async () => {
          try {
            await resign();
            await refresh();
            router.replace("/(app)/dashboard");
          } catch (err) {
            if (err instanceof ApiError && err.status === 409) {
              toast.error(
                t(
                  "verwaltung.ka.resignLastError",
                  "Bist letzter Kursadmin — ernenne zuerst einen anderen",
                ),
              );
            } else {
              toast.error(t("verwaltung.ka.error", "Aktion fehlgeschlagen"));
            }
            throw err;
          }
        }}
      />
    </ScrollView>
  );
}

// ─── Screen ───────────────────────────────────────────────────────────────────

/**
 * Verwaltung screen — guarded by {@link AdminRoute}.
 *
 * @returns The administration screen.
 */
export default function VerwaltungScreen() {
  const { user } = useAuth();
  const router = useRouter();
  const isSysAdmin = user?.studentType === "SYSTEMADMIN";
  const hasKurs = Boolean(user?.kursName);
  const showTabs = isSysAdmin && hasKurs;

  const { tab } = useLocalSearchParams<{ tab?: string }>();
  const activeTab: "system" | "kurs" = tab === "kurs" ? "kurs" : "system";

  return (
    <AdminRoute>
      {showTabs ? (
        <View className="flex-1 bg-background">
          {/* Tab switcher pill */}
          <View className="flex-row items-center gap-1 self-center rounded-full bg-muted px-1 py-1 mt-4 mb-2">
            <TouchableOpacity
              accessibilityRole="button"
              onPress={() => router.setParams({ tab: "system" })}
              className={cn(
                "rounded-full px-4 py-1.5",
                activeTab === "system" ? "bg-background" : "",
              )}
            >
              <Text
                className={cn(
                  "text-sm font-medium",
                  activeTab === "system"
                    ? "text-foreground"
                    : "text-muted-foreground",
                )}
              >
                {t("verwaltung.tab.system", "Systemverwaltung")}
              </Text>
            </TouchableOpacity>
            <TouchableOpacity
              accessibilityRole="button"
              onPress={() => router.setParams({ tab: "kurs" })}
              className={cn(
                "rounded-full px-4 py-1.5",
                activeTab === "kurs" ? "bg-background" : "",
              )}
            >
              <Text
                className={cn(
                  "text-sm font-medium",
                  activeTab === "kurs"
                    ? "text-foreground"
                    : "text-muted-foreground",
                )}
              >
                {t("verwaltung.tab.kurs", "Kursverwaltung")}
              </Text>
            </TouchableOpacity>
          </View>
          {/* Only mount the active content — prevents inactive tab's hooks from firing */}
          {activeTab === "system" ? (
            <SystemverwaltungContent />
          ) : (
            <KursadminContent />
          )}
        </View>
      ) : isSysAdmin ? (
        <SystemverwaltungContent />
      ) : (
        <KursadminContent />
      )}
    </AdminRoute>
  );
}
