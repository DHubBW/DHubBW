import { Text, TextClassContext } from '@/components/ui/text';
import { cn } from '@/lib/utils';
import { View } from 'react-native';

/**
 * Card container.
 *
 * Provides a rounded, bordered box and sets the `TextClassContext` to
 * `text-card-foreground` so child text elements automatically receive the
 * correct card color.
 *
 * @param className - Additional Tailwind classes.
 * @param props - All other `View` props.
 * @returns The card container.
 */
function Card({ className, ...props }: React.ComponentProps<typeof View> & React.RefAttributes<View>) {
  return (
    <TextClassContext.Provider value="text-card-foreground">
      <View
        className={cn(
          'bg-card border-border flex flex-col gap-6 rounded-xl border py-6 shadow-sm shadow-black/5',
          className
        )}
        {...props}
      />
    </TextClassContext.Provider>
  );
}

/**
 * Header area of a {@link Card}.
 *
 * @param className - Additional Tailwind classes.
 * @param props - All other `View` props.
 * @returns The card header.
 */
function CardHeader({ className, ...props }: React.ComponentProps<typeof View> & React.RefAttributes<View>) {
  return <View className={cn('flex flex-col gap-1.5 px-6', className)} {...props} />;
}

/**
 * Title of a {@link Card}.
 *
 * @param className - Additional Tailwind classes.
 * @param ref - Ref to the underlying `Text` element.
 * @param props - All other `Text` props.
 * @returns The card title.
 */
function CardTitle({
  className,
  ref,
  ...props
}: React.ComponentProps<typeof Text> & React.RefAttributes<typeof Text>) {

  return (
    <Text
      ref={ref}
      role="heading"
      aria-level={3}
      className={cn('font-semibold leading-none', className)}
      {...props}
    />
  );
}

/**
 * Description text of a {@link Card}.
 *
 * @param className - Additional Tailwind classes.
 * @param props - All other `Text` props.
 * @returns The card description.
 */
function CardDescription({
  className,
  ...props
}: React.ComponentProps<typeof Text> & React.RefAttributes<typeof Text>) {
  return <Text className={cn('text-muted-foreground text-sm', className)} {...props} />;
}

/**
 * Content area of a {@link Card}.
 *
 * @param className - Additional Tailwind classes.
 * @param props - All other `View` props.
 * @returns The card content area.
 */
function CardContent({ className, ...props }: React.ComponentProps<typeof View> & React.RefAttributes<View>) {
  return <View className={cn('px-6', className)} {...props} />;
}

/**
 * Footer area of a {@link Card}.
 *
 * @param className - Additional Tailwind classes.
 * @param props - All other `View` props.
 * @returns The card footer.
 */
function CardFooter({ className, ...props }: React.ComponentProps<typeof View> & React.RefAttributes<View>) {
  return <View className={cn('flex flex-row items-center px-6', className)} {...props} />;
}

export { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle };
