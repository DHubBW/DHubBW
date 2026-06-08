/**
 * Mensa screen — weekly meal plan with week toggle and weekday tab selector.
 *
 * Card layout per dish:
 * - Name (bold)
 * - Essenstyp mit Icon (z.B. "🌿 Vegan") als normale Textzeile
 * - Preis groß
 * - Allergen-Chips ohne Icon, mit Tooltip
 *
 * Caching: one API call per week; switching tabs triggers no further requests.
 */
import { ScrollView, TouchableOpacity, View } from "react-native";

import { Beef, Leaf, Milk } from "lucide-react-native";

import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { Text } from "@/components/ui/text";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { useMensaNextWeek } from "@/hooks/useMensaNextWeek";
import { useMensaThisWeek } from "@/hooks/useMensaThisWeek";
import type { Ernaehrung, MensaDTO } from "@/lib/api/types.gen";
import { t } from "@/lib/i18n/t";
import { cn } from "@/lib/utils";
import { useState } from "react";

// ─── Constants ───────────────────────────────────────────────────────────────

const WEEKDAY_KEYS = ["Mo", "Di", "Mi", "Do", "Fr"] as const;
type WeekdayKey = (typeof WEEKDAY_KEYS)[number];
type WeekSelector = "this" | "next";

/**
 * Full German names for all allergen codes and additive numbers.
 * Source: SW Karlsruhe official allergen list (Stand 01.09.2025).
 */
const ALLERGEN_NAMES: Record<string, string> = {
  Ca: "Cashewnüsse",
  Di: "Dinkel / Dinkelgluten",
  Ei: "Eier",
  Er: "Erdnüsse",
  Fi: "Fisch",
  Ge: "Gerste / Gerstengluten",
  Hf: "Hafer / Hafergluten",
  Ha: "Haselnüsse",
  Ka: "Kamut / Kamutgluten",
  Kr: "Krebstiere",
  Lu: "Lupine",
  Ma: "Mandeln",
  ML: "Milch / Laktose",
  Pa: "Paranüsse",
  Pe: "Pekannüsse",
  Pi: "Pistazie",
  Qu: "Macadamianüsse",
  Ro: "Roggen / Roggengluten",
  Sa: "Sesam",
  Se: "Sellerie",
  Sf: "Schwefeldioxid / Sulfit",
  Sn: "Senf",
  So: "Soja",
  Wa: "Walnüsse",
  We: "Weizen / Weizengluten",
  Wt: "Weichtiere",
  LAB: "tierisches Lab",
  GEL: "Gelatine",
  "1": "Farbstoff",
  "2": "Konservierungsstoff",
  "3": "Antioxidationsmittel",
  "4": "Geschmacksverstärker",
  "5": "Phosphat",
  "6": "Oberfläche gewachst",
  "7": "geschwefelt",
  "8": "Oliven geschwärzt",
  "9": "Süßungsmittel",
  "10": "abführend (überm. Verzehr)",
  "11": "Phenylalaninquelle",
  "12": "Restalkohol",
  "14": "aus Fleischstücken",
  "15": "kakaohalt. Fettglasur",
  "27": "aus Fischstücken",
};

// ─── Date helpers ────────────────────────────────────────────────────────────

/**
 * Returns the Monday of the ISO week offset by `offsetWeeks` from today.
 *
 * @param offsetWeeks - 0 = current week, 1 = next week.
 * @returns The Monday date of the target ISO week.
 */
function getMondayOf(offsetWeeks: number): Date {
  const today = new Date();
  const dow = today.getDay();
  const distToMonday = dow === 0 ? -6 : 1 - dow;
  const monday = new Date(today);
  monday.setDate(today.getDate() + distToMonday + offsetWeeks * 7);
  return monday;
}

/**
 * Formats a date as `"D.M."` (compact German format, no leading zeros).
 *
 * @param date - Date to format.
 * @returns String like `"9.6."`.
 */
function formatDayMonth(date: Date): string {
  return `${date.getDate()}.${date.getMonth() + 1}.`;
}

// ─── Ernaehrung helpers ──────────────────────────────────────────────────────

/**
 * Returns the left-border accent class for the dish card (theme-token based).
 * Vegan=success(grün), Vegetarisch=warning(amber), Fleisch=destructive(rot).
 *
 * @param ernaehrung - Dietary classification.
 * @returns Tailwind border-left class.
 */
function ernaehrungBorderClass(ernaehrung: Ernaehrung): string {
  if (ernaehrung === "VEGAN") return "border-l-4 border-l-success";
  if (ernaehrung === "VEGETARISCH") return "border-l-4 border-l-warning";
  return "border-l-4 border-l-destructive";
}

/**
 * Returns the Lucide icon colour for the dietary type.
 * Vegan=success, Vegetarisch=warning, Fleisch=destructive.
 *
 * @param ernaehrung - Dietary classification.
 * @returns CSS colour string for Lucide `color` prop.
 */
function ernaehrungIconColor(ernaehrung: Ernaehrung): string {
  if (ernaehrung === "VEGAN") return "hsl(var(--success))";
  if (ernaehrung === "VEGETARISCH") return "hsl(var(--warning))";
  return "hsl(var(--destructive))";
}

/**
 * Returns the Tailwind class for the tooltip background matching the dietary type.
 *
 * @param ernaehrung - Dietary classification.
 * @returns Tailwind class string.
 */
function ernaehrungTooltipClass(ernaehrung: Ernaehrung): string {
  if (ernaehrung === "VEGAN") return "bg-success border-success";
  if (ernaehrung === "VEGETARISCH") return "bg-warning border-warning";
  return "bg-destructive border-destructive";
}

/**
 * Returns the Lucide icon component for the dietary type.
 * Leaf = Vegan, Milk = Vegetarisch, Beef = Fleisch.
 *
 * @param ernaehrung - Dietary classification.
 * @returns Lucide icon component.
 */
function ernaehrungIconComponent(
  ernaehrung: Ernaehrung,
): typeof Leaf | typeof Milk | typeof Beef {
  if (ernaehrung === "VEGAN") return Leaf;
  if (ernaehrung === "VEGETARISCH") return Milk;
  return Beef;
}

/**
 * Returns the localised label for the dietary type (used in tooltip).
 *
 * @param ernaehrung - Dietary classification.
 * @returns Localised label string.
 */
function ernaehrungLabel(ernaehrung: Ernaehrung): string {
  if (ernaehrung === "VEGAN") return t("mensa.badge.vegan", "Vegan");
  if (ernaehrung === "VEGETARISCH")
    return t("mensa.badge.vegetarisch", "Vegetarisch");
  return t("mensa.badge.fleisch", "Fleisch");
}

// ─── Grouping ────────────────────────────────────────────────────────────────

/**
 * Groups dishes by `linie`, preserving insertion order.
 *
 * @param meals - Flat list of {@link MensaDTO} for one day.
 * @returns Ordered map from linie label to dish list.
 */
function groupByLinie(meals: MensaDTO[]): Map<string, MensaDTO[]> {
  const map = new Map<string, MensaDTO[]>();
  for (const meal of meals) {
    const existing = map.get(meal.linie);
    if (existing) {
      existing.push(meal);
    } else {
      map.set(meal.linie, [meal]);
    }
  }
  return map;
}

// ─── Components ──────────────────────────────────────────────────────────────

type AllergenChipProps = { code: string };

/**
 * Single allergen/additive chip — shows the code, tooltip on hover/press.
 * No icon prefix (per design spec).
 *
 * @param props - {@link AllergenChipProps}
 * @returns Chip with tooltip.
 */
function AllergenChip({ code }: AllergenChipProps) {
  const knownLabel = ALLERGEN_NAMES[code];
  const tooltipText =
    knownLabel ??
    t("mensa.allergen.unknown", "Unbekannter Zusatzstoff / Allergen");
  return (
    <Tooltip delayDuration={200}>
      <TooltipTrigger>
        <Badge variant="outline" className="border-border px-1.5 py-0">
          <Text className="text-xs text-muted-foreground">{code}</Text>
        </Badge>
      </TooltipTrigger>
      <TooltipContent side="top" sideOffset={4}>
        <Text>{tooltipText}</Text>
      </TooltipContent>
    </Tooltip>
  );
}

type MealCardProps = { meal: MensaDTO };

/**
 * Dish card layout:
 * - Großes Icon links (mit Tooltip für Essenstyp)
 * - Rechts davon: Name bold oben, darunter Allergen-Chips + Preis in einer Zeile
 *
 * @param props - {@link MealCardProps}
 * @returns Meal card.
 */
function MealCard({ meal }: MealCardProps) {
  const priceStr = meal.preis.toFixed(2).replace(".", ",");
  const DietIcon = ernaehrungIconComponent(meal.ernaehrung);
  const dietLabel = ernaehrungLabel(meal.ernaehrung);
  const allergenItems = meal.allergene
    .replace(/^\[|\]$/g, "")
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);

  return (
    <Card
      className={cn(
        "mb-4 gap-0 overflow-hidden py-5",
        ernaehrungBorderClass(meal.ernaehrung),
      )}
    >
      <View className="flex-row items-center gap-5 px-6">
        {/* Large icon left with tooltip */}
        <Tooltip delayDuration={200}>
          <TooltipTrigger>
            <DietIcon
              size={34}
              color={ernaehrungIconColor(meal.ernaehrung)}
              strokeWidth={1.75}
            />
          </TooltipTrigger>
          <TooltipContent
            side="top"
            sideOffset={4}
            className={ernaehrungTooltipClass(meal.ernaehrung)}
          >
            <Text className="text-primary-foreground">{dietLabel}</Text>
          </TooltipContent>
        </Tooltip>
        {/* Right column: name, then allergens + price */}
        <View className="flex-1">
          <Text className="text-lg font-bold leading-snug text-foreground">
            {meal.essensbezeichnung}
          </Text>
          <View className="mt-2.5 flex-row items-end justify-between gap-2">
            <View className="flex-1 flex-row flex-wrap gap-1.5">
              {allergenItems.map((code) => (
                <AllergenChip key={code} code={code} />
              ))}
            </View>
            <Text className="shrink-0 text-base font-semibold tabular-nums text-muted-foreground">
              {priceStr} €
            </Text>
          </View>
        </View>
      </View>
    </Card>
  );
}

type LinieGroupProps = { linie: string; meals: MensaDTO[] };

/**
 * Section for one serving line with a bold heading and its meal cards.
 *
 * @param props - {@link LinieGroupProps}
 * @returns Linie section.
 */
function LinieGroup({ linie, meals }: LinieGroupProps) {
  return (
    <View className="mb-2">
      <Text className="mb-3 text-base font-semibold text-foreground">
        {linie}
      </Text>
      {meals.map((meal) => (
        <MealCard key={meal.gerichtID} meal={meal} />
      ))}
    </View>
  );
}

type DayContentProps = {
  meals: MensaDTO[] | undefined;
  isLoading: boolean;
  isError: boolean;
};

/**
 * Renders the "Mensa Speiseplan" heading + grouped meal list, or loading / error / empty states.
 *
 * @param props - {@link DayContentProps}
 * @returns Day content area.
 */
function DayContent({ meals, isLoading, isError }: DayContentProps) {
  if (isLoading) {
    return (
      <View className="items-center py-16">
        <Text className="text-muted-foreground">
          {t("mensa.loading", "Speiseplan wird geladen …")}
        </Text>
      </View>
    );
  }

  if (isError) {
    return (
      <View className="items-center py-16">
        <Text className="text-destructive">
          {t("mensa.error", "Speiseplan konnte nicht geladen werden.")}
        </Text>
      </View>
    );
  }

  if (!meals || meals.length === 0) {
    return (
      <View className="items-center py-16">
        <Text className="text-muted-foreground">
          {t("mensa.empty", "Für diesen Tag liegt kein Speiseplan vor.")}
        </Text>
      </View>
    );
  }

  const grouped = groupByLinie(meals);

  return (
    <>
      {Array.from(grouped.entries()).map(([linie, lineMeals]) => (
        <LinieGroup key={linie} linie={linie} meals={lineMeals} />
      ))}
    </>
  );
}

// ─── Screen ──────────────────────────────────────────────────────────────────

/**
 * Mensa screen — entry point rendered by Expo Router.
 *
 * @returns The Mensa screen with toggle, day tabs and meal cards.
 */
export default function MensaScreen() {
  const [weekSelector, setWeekSelector] = useState<WeekSelector>("this");
  const [activeDay, setActiveDay] = useState<WeekdayKey>("Mo");

  const thisWeek = useMensaThisWeek();
  const nextWeek = useMensaNextWeek();

  const activeQuery = weekSelector === "this" ? thisWeek : nextWeek;
  const monday = getMondayOf(weekSelector === "this" ? 0 : 1);
  const dayMeals = activeQuery.data?.[activeDay];

  return (
    <ScrollView className="flex-1 bg-background">
      {/* ── Week toggle ──────────────────────────────────────── */}
      <View className="items-center px-4 pb-0 pt-4">
        <View className="flex-row rounded-full bg-secondary p-1">
          {(["this", "next"] as const).map((sel) => {
            const active = weekSelector === sel;
            return (
              <TouchableOpacity
                key={sel}
                onPress={() => setWeekSelector(sel)}
                className={cn(
                  "rounded-full px-5 py-2",
                  active && "bg-foreground",
                )}
                accessibilityRole="button"
                accessibilityState={{ selected: active }}
              >
                <Text
                  className={cn(
                    "text-sm font-semibold",
                    active ? "text-background" : "text-muted-foreground",
                  )}
                >
                  {sel === "this"
                    ? t("mensa.toggle.thisWeek", "Diese Woche")
                    : t("mensa.toggle.nextWeek", "Nächste Woche")}
                </Text>
              </TouchableOpacity>
            );
          })}
        </View>
      </View>

      {/* ── Weekday tabs ─────────────────────────────────────── */}
      <View className="mt-2 flex-row border-b border-border">
        {WEEKDAY_KEYS.map((day, idx) => {
          const active = activeDay === day;
          const date = new Date(monday);
          date.setDate(monday.getDate() + idx);
          return (
            <TouchableOpacity
              key={day}
              onPress={() => setActiveDay(day)}
              className="relative flex-1 items-center pb-2 pt-3"
              accessibilityRole="tab"
              accessibilityState={{ selected: active }}
            >
              <Text
                className={cn(
                  "text-sm font-bold",
                  active ? "text-foreground" : "text-muted-foreground",
                )}
              >
                {day}
              </Text>
              <Text
                className={cn(
                  "text-xs",
                  active ? "text-foreground" : "text-muted-foreground",
                )}
              >
                {formatDayMonth(date)}
              </Text>
              {active && (
                <View className="absolute bottom-0 left-0 right-0 h-0.5 bg-primary" />
              )}
            </TouchableOpacity>
          );
        })}
      </View>

      {/* ── Meal list ────────────────────────────────────────── */}
      <View className="items-center px-4 pb-10 pt-5">
        <View className="w-full max-w-4xl">
          <DayContent
            meals={dayMeals}
            isLoading={activeQuery.isLoading}
            isError={activeQuery.isError}
          />
        </View>
      </View>
    </ScrollView>
  );
}
