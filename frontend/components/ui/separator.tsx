import { cn } from '@/lib/utils';
import * as SeparatorPrimitive from '@rn-primitives/separator';

/**
 * Horizontal or vertical visual separator line.
 *
 * @param className - Additional Tailwind classes.
 * @param orientation - Direction of the separator: `'horizontal'` (default) or `'vertical'`.
 * @param decorative - Whether the separator is purely decorative (default: `true`).
 * @param props - All other `SeparatorPrimitive.Root` props.
 * @returns The styled separator element.
 */
function Separator({
  className,
  orientation = 'horizontal',
  decorative = true,
  ...props
}: React.ComponentProps<typeof SeparatorPrimitive.Root>) {
  return (
    <SeparatorPrimitive.Root
      decorative={decorative}
      orientation={orientation}
      className={cn(
        'bg-border shrink-0',
        orientation === 'horizontal' ? 'h-[1px] w-full' : 'h-full w-[1px]',
        className
      )}
      {...props}
    />
  );
}

export { Separator };
