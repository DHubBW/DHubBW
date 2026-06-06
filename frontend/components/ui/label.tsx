import { cn } from '@/lib/utils';
import * as LabelPrimitive from '@rn-primitives/label';
import { Platform } from 'react-native';

/**
 * Accessible label element for form fields.
 *
 * Based on `@rn-primitives/label` and supports a `disabled` state with
 * reduced opacity. On web the label is associated with its input via the
 * `htmlFor` prop (handled by `LabelPrimitive`).
 *
 * @param className - Additional Tailwind classes for the label text.
 * @param disabled - When `true`, the label is shown as disabled.
 * @param props - All other `LabelPrimitive.Text` props.
 * @returns The styled label element.
 */
function Label({
  className,
  onPress,
  onLongPress,
  onPressIn,
  onPressOut,
  disabled,
  ...props
}: React.ComponentProps<typeof LabelPrimitive.Text>) {
  return (
    <LabelPrimitive.Root
      className={cn(
        'flex select-none flex-row items-center gap-2',
        Platform.select({
          web: 'cursor-default leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-50 group-data-[disabled=true]:pointer-events-none group-data-[disabled=true]:opacity-50',
        }),
        disabled && 'opacity-50'
      )}
      onPress={onPress}
      onLongPress={onLongPress}
      onPressIn={onPressIn}
      onPressOut={onPressOut}
      disabled={disabled}>
      <LabelPrimitive.Text
        className={cn(
          'text-foreground text-sm font-medium',
          Platform.select({ web: 'leading-none' }),
          className
        )}
        {...props}
      />
    </LabelPrimitive.Root>
  );
}

export { Label };
