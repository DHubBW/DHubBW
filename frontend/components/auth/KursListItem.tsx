import { Pressable, Text, View } from 'react-native';

import { cn } from '@/lib/utils';
import type { KursYearDTO } from '@/lib/api/types.gen';

/**
 * Props for {@link KursListItem}.
 */
export interface KursListItemProps {
  /** The course to render. */
  kurs: KursYearDTO;
  /** Whether this row is currently selected. */
  selected: boolean;
  /** Press handler — typically toggles `selected` in the parent. */
  onPress: () => void;
}

/**
 * Single row in the course-selection list of registration Step 2.
 *
 * Visually distinguishes the selected row with a primary-coloured border;
 * unselected rows fall back to the muted border. Layout is two stacked
 * lines: course name (semibold) and cohort year (muted, smaller).
 *
 * Accessibility: rendered as a `radio`-style pressable so screen readers
 * announce the row as part of a single-select group, with `selected`
 * reflected via `accessibilityState`.
 *
 * @param props - {@link KursListItemProps}
 * @returns The row element.
 */
export function KursListItem({ kurs, selected, onPress }: KursListItemProps) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="radio"
      accessibilityState={{ selected }}
      accessibilityLabel={`${kurs.kursName}, Jahrgang ${kurs.jahrgang}`}
      className={cn(
        'mb-2 rounded-md border px-3 py-3',
        selected
          ? 'border-primary bg-primary/10'
          : 'border-border bg-background',
      )}
    >
      <View>
        <Text className="text-sm font-semibold text-foreground">
          {kurs.kursName}
        </Text>
        <Text className="text-xs text-muted-foreground">
          Jahrgang {kurs.jahrgang}
        </Text>
      </View>
    </Pressable>
  );
}
