import { cn } from '@/lib/utils';
import * as AvatarPrimitive from '@rn-primitives/avatar';

/**
 * Circular avatar container.
 *
 * @param className - Additional Tailwind classes.
 * @param props - All other `AvatarPrimitive.Root` props.
 * @returns The avatar root element.
 */
function Avatar({
  className,
  ...props
}: React.ComponentProps<typeof AvatarPrimitive.Root>) {
  return (
    <AvatarPrimitive.Root
      className={cn('relative flex size-8 shrink-0 overflow-hidden rounded-full', className)}
      {...props}
    />
  );
}

/**
 * Avatar image – fills the circular container.
 *
 * @param className - Additional Tailwind classes.
 * @param props - All other `AvatarPrimitive.Image` props.
 * @returns The avatar image element.
 */
function AvatarImage({
  className,
  ...props
}: React.ComponentProps<typeof AvatarPrimitive.Image>) {
  return <AvatarPrimitive.Image className={cn('aspect-square size-full', className)} {...props} />;
}

/**
 * Fallback shown when the avatar image fails to load.
 *
 * @param className - Additional Tailwind classes.
 * @param props - All other `AvatarPrimitive.Fallback` props.
 * @returns The avatar fallback element.
 */
function AvatarFallback({
  className,
  ...props
}: React.ComponentProps<typeof AvatarPrimitive.Fallback>) {
  return (
    <AvatarPrimitive.Fallback
      className={cn(
        'bg-muted flex size-full flex-row items-center justify-center rounded-full',
        className
      )}
      {...props}
    />
  );
}

export { Avatar, AvatarFallback, AvatarImage };
