/**
 * Reusable star-rating display + control.
 *
 * <p>Renders a row of 1..5 stars. The component supports two modes:</p>
 * <ul>
 *   <li><strong>Display</strong> ({@code interactive=false}): shows the
 *       file's average rating, with full stars filled up to the rounded
 *       average. The vote count is rendered next to the stars.</li>
 *   <li><strong>Interactive</strong> ({@code interactive=true}): clicks
 *       on a star send a `PUT /…/bewertung` to set the caller's vote;
 *       the {@code value} prop tracks the caller's own rating
 *       (0 if not yet rated). Half-stars are deliberately not supported
 *       — the rule is whole stars only.</li>
 * </ul>
 *
 * <p>Display-mode integrates directly with {@link DateiDTO}'s
 * {@code bewertungDurchschnitt} / {@code bewertungAnzahl} fields so the
 * file table can show the rating without an extra request.</p>
 */
import { Star } from "lucide-react-native";
import { Pressable, View } from "react-native";

import { Text } from "@/components/ui/text";
import { cn } from "@/lib/utils";

const MAX_STERNE = 5;

export interface StarRatingProps {
  /**
   * The displayed value. In display mode this is typically the
   * file's average rating. In interactive mode this is the caller's
   * own rating (0 if not yet rated).
   */
  value: number;
  /** Total number of votes — only rendered in display mode. */
  count?: number;
  /** When true, stars accept clicks and call {@link onChange}. */
  interactive?: boolean;
  /**
   * Callback invoked with the picked star count (1..5) when the user
   * clicks a star. Only fired in interactive mode.
   */
  onChange?: (sterne: number) => void;
  /** Marker test ID applied to the wrapper for e2e selectors. */
  testID?: string;
  /** Visual size of each star icon. */
  size?: number;
}

/**
 * Render the star rating.
 *
 * @param props - {@link StarRatingProps}
 * @returns the rating element.
 */
export function StarRating({
  value,
  count,
  interactive = false,
  onChange,
  testID,
  size = 16,
}: StarRatingProps) {
  // Display-only rendering with zero votes is intentionally blank
  // — the user spec is "kein - sondern einfach nix". The
  // interactive picker still renders so users can place their
  // first rating.
  if (!interactive && count !== undefined && count <= 0) {
    return null;
  }
  // For display mode the value is a float (e.g. 3.7); for the active
  // tint we round to the nearest integer so a 3.7 average shows 4
  // filled stars. This keeps the visual mark consistent with the
  // numeric label rendered next to the row.
  const filled = Math.round(value);
  return (
    <View
      className="flex-row items-center gap-1"
      testID={testID}
      accessibilityLabel={
        count !== undefined
          ? `${value.toFixed(1)} von ${MAX_STERNE} (${count})`
          : `${value} von ${MAX_STERNE}`
      }
    >
      {Array.from({ length: MAX_STERNE }, (_, index) => {
        const starValue = index + 1;
        const isFilled = starValue <= filled;
        const Icon = (
          <Star
            size={size}
            color={
              isFilled ? "hsl(var(--primary))" : "hsl(var(--muted-foreground))"
            }
            fill={isFilled ? "hsl(var(--primary))" : "transparent"}
          />
        );
        if (!interactive) {
          return <View key={starValue}>{Icon}</View>;
        }
        return (
          <Pressable
            key={starValue}
            accessibilityRole="button"
            accessibilityLabel={`${starValue} Sterne`}
            onPress={() => onChange?.(starValue)}
            className={cn("rounded-md p-0.5 active:bg-accent")}
            testID={`${testID ?? "star-rating"}-star-${starValue}`}
          >
            {Icon}
          </Pressable>
        );
      })}
      {!interactive && count !== undefined && count > 0 && (
        <Text className="ml-1 text-xs text-muted-foreground">
          {value.toFixed(1)} ({count})
        </Text>
      )}
    </View>
  );
}
