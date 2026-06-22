/**
 * Inline pill chip used to render a single tag.
 *
 * <p>Used in the file table / card list to surface the tags attached
 * to a file. When {@code onRemove} is provided a small × button is
 * added — that variant is rendered inside the file detail dialog so
 * authorised users can detach tags directly from the chip.</p>
 */
import { X } from "lucide-react-native";
import { Pressable, View } from "react-native";

import { Text } from "@/components/ui/text";
import { cn } from "@/lib/utils";

export interface TagBadgeProps {
  /** Display name of the tag. */
  name: string;
  /**
   * When provided a small × button is rendered next to the name and
   * invokes the callback on press. Used for the detach affordance.
   */
  onRemove?: () => void;
  /** Visual variant — "filter-active" highlights the chip. */
  variant?: "default" | "filter-active";
  /** Test ID surfaced on the wrapper view / pressable. */
  testID?: string;
  /**
   * When provided the whole chip becomes a button — used by the
   * filter row. Mutually exclusive with {@code onRemove}.
   */
  onPress?: () => void;
}

/**
 * Render the tag chip.
 *
 * @param props - {@link TagBadgeProps}
 * @returns the chip element.
 */
export function TagBadge({
  name,
  onRemove,
  variant = "default",
  testID,
  onPress,
}: TagBadgeProps) {
  const baseClass = cn(
    "flex-row items-center gap-1 rounded-full px-2 py-0.5",
    variant === "filter-active" ? "bg-primary" : "bg-secondary",
  );
  const labelClass = cn(
    "text-[11px] font-medium",
    variant === "filter-active"
      ? "text-primary-foreground"
      : "text-secondary-foreground",
  );
  const Inner = (
    <>
      <Text className={labelClass} numberOfLines={1}>
        {name}
      </Text>
      {onRemove && (
        <Pressable
          accessibilityRole="button"
          accessibilityLabel={`Tag ${name} entfernen`}
          onPress={onRemove}
          className="rounded-full p-0.5 active:bg-accent/30"
          testID={`${testID ?? "tag-badge"}-remove`}
        >
          <X size={10} color="hsl(var(--secondary-foreground))" />
        </Pressable>
      )}
    </>
  );
  if (onPress) {
    return (
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={`Filter Tag ${name}`}
        onPress={onPress}
        className={baseClass}
        testID={testID}
      >
        {Inner}
      </Pressable>
    );
  }
  return (
    <View className={baseClass} testID={testID}>
      {Inner}
    </View>
  );
}
