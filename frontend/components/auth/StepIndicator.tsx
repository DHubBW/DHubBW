import { View } from 'react-native';

import { cn } from '@/lib/utils';

/**
 * Minimal multi-step indicator for the registration flow.
 *
 * Renders `total` dots in a horizontal row; the dot at index `current - 1`
 * (1-based) is filled with the primary color, the rest are muted. Reused
 * across the multi-step register flow.
 *
 * Accessibility: the View carries an `accessibilityLabel` like
 * `"Schritt 1 von 2"` so screen readers announce the current progress.
 */
export interface StepIndicatorProps {
  /** 1-based index of the active step. Out-of-range values clamp visually. */
  current: number;
  /** Total number of steps (e.g. 2 for the registration flow). */
  total: number;
  /** Optional Tailwind classes for the wrapper. */
  className?: string;
}

/**
 * Renders the dot row.
 *
 * @param props - {@link StepIndicatorProps}
 * @returns The progress-dot row, or `null` when `total < 1`.
 */
export function StepIndicator({ current, total, className }: StepIndicatorProps) {
  // Defensive: total < 1 collapses to a no-op (nothing to render). Keeps the
  // component safe to drop into screens that haven't decided their step count
  // yet.
  if (total < 1) return null;

  const dots = Array.from({ length: total }, (_, i) => i + 1);

  return (
    <View
      accessibilityRole="progressbar"
      accessibilityLabel={`Schritt ${current} von ${total}`}
      className={cn('flex-row items-center justify-center gap-2', className)}
    >
      {dots.map((step) => {
        const active = step === current;
        return (
          <View
            key={step}
            className={cn(
              'h-2 w-2 rounded-full',
              active ? 'bg-primary' : 'bg-muted',
            )}
          />
        );
      })}
    </View>
  );
}
