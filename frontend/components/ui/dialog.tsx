import { Icon } from '@/components/ui/icon';
import { NativeOnlyAnimatedView } from '@/components/ui/native-only-animated-view';
import { cn } from '@/lib/utils';
import * as DialogPrimitive from '@rn-primitives/dialog';
import { X } from 'lucide-react-native';
import * as React from 'react';
import { Platform, Text, View, type ViewProps } from 'react-native';
import { FadeIn, FadeOut } from 'react-native-reanimated';
import { FullWindowOverlay as RNFullWindowOverlay } from 'react-native-screens';

/** Root dialog component – wraps `@rn-primitives/dialog` Root. */
const Dialog = DialogPrimitive.Root;

/** Dialog trigger – wraps `@rn-primitives/dialog` Trigger. */
const DialogTrigger = DialogPrimitive.Trigger;

/** Dialog portal – wraps `@rn-primitives/dialog` Portal. */
const DialogPortal = DialogPrimitive.Portal;

/** Dialog close button – wraps `@rn-primitives/dialog` Close. */
const DialogClose = DialogPrimitive.Close;

const FullWindowOverlay = Platform.OS === 'ios' ? RNFullWindowOverlay : React.Fragment;

/**
 * Semi-transparent overlay behind the dialog content.
 *
 * On native platforms an animated fade-in/out is applied. On iOS the overlay
 * is wrapped in `FullWindowOverlay` to ensure correct z-ordering.
 *
 * @param className - Additional Tailwind classes.
 * @param children - Content rendered inside the overlay.
 * @param props - All other `DialogPrimitive.Overlay` props (except `asChild`).
 * @returns The dialog overlay element.
 */
function DialogOverlay({
  className,
  children,
  ...props
}: Omit<React.ComponentProps<typeof DialogPrimitive.Overlay>, 'asChild'> & {
    children?: React.ReactNode;
  }) {
  return (
    <FullWindowOverlay>
      <DialogPrimitive.Overlay
        className={cn(
          'absolute bottom-0 left-0 right-0 top-0 flex items-center justify-center bg-black/50 p-2',
          Platform.select({
            web: 'animate-in fade-in-0 fixed cursor-default [&>*]:cursor-auto',
          }),
          className
        )}
        {...props}
        asChild={Platform.OS !== 'web'}>
        <NativeOnlyAnimatedView entering={FadeIn.duration(200)} exiting={FadeOut.duration(150)}>
          <NativeOnlyAnimatedView entering={FadeIn.delay(50)} exiting={FadeOut.duration(150)}>
            <>{children}</>
          </NativeOnlyAnimatedView>
        </NativeOnlyAnimatedView>
      </DialogPrimitive.Overlay>
    </FullWindowOverlay>
  );
}

/**
 * Main dialog content area with a built-in close button.
 *
 * Renders inside a {@link DialogPortal} on top of a {@link DialogOverlay}.
 *
 * @param className - Additional Tailwind classes.
 * @param portalHost - Named portal host to render into (optional).
 * @param children - Dialog body content.
 * @param props - All other `DialogPrimitive.Content` props.
 * @returns The dialog content element.
 */
function DialogContent({
  className,
  portalHost,
  children,
  ...props
}: React.ComponentProps<typeof DialogPrimitive.Content> & {
    portalHost?: string;
  }) {
  return (
    <DialogPortal hostName={portalHost}>
      <DialogOverlay>
        <DialogPrimitive.Content
          className={cn(
            'bg-background border-border z-50 mx-auto flex w-full max-w-[calc(100%-2rem)] flex-col gap-4 rounded-lg border p-6 shadow-lg shadow-black/5 sm:max-w-lg',
            Platform.select({
              web: 'animate-in fade-in-0 zoom-in-95 duration-200',
            }),
            className
          )}
          {...props}>
          <>{children}</>
          <DialogPrimitive.Close
            className={cn(
              'absolute right-4 top-4 rounded opacity-70 active:opacity-100',
              Platform.select({
                web: 'ring-offset-background focus:ring-ring data-[state=open]:bg-accent transition-opacity hover:opacity-100 focus:outline-none focus:ring-2 focus:ring-offset-2',
              })
            )}
            hitSlop={12}>
            <Icon
              as={X}
              className={cn('text-accent-foreground web:pointer-events-none size-4 shrink-0')}
            />
            <Text className="sr-only">Close</Text>
          </DialogPrimitive.Close>
        </DialogPrimitive.Content>
      </DialogOverlay>
    </DialogPortal>
  );
}

/**
 * Header area of a dialog – stacks title and description vertically.
 *
 * @param className - Additional Tailwind classes.
 * @param props - All other `View` props.
 * @returns The dialog header element.
 */
function DialogHeader({ className, ...props }: ViewProps) {
  return (
    <View className={cn('flex flex-col gap-2 text-center sm:text-left', className)} {...props} />
  );
}

/**
 * Footer area of a dialog – arranges action buttons in a row.
 *
 * @param className - Additional Tailwind classes.
 * @param props - All other `View` props.
 * @returns The dialog footer element.
 */
function DialogFooter({ className, ...props }: ViewProps) {
  return (
    <View
      className={cn('flex flex-col-reverse gap-2 sm:flex-row sm:justify-end', className)}
      {...props}
    />
  );
}

/**
 * Dialog title text.
 *
 * @param className - Additional Tailwind classes.
 * @param props - All other `DialogPrimitive.Title` props.
 * @returns The dialog title element.
 */
function DialogTitle({
  className,
  ...props
}: React.ComponentProps<typeof DialogPrimitive.Title>) {
  return (
    <DialogPrimitive.Title
      className={cn('text-foreground text-lg font-semibold leading-none', className)}
      {...props}
    />
  );
}

/**
 * Dialog description text shown below the title.
 *
 * @param className - Additional Tailwind classes.
 * @param props - All other `DialogPrimitive.Description` props.
 * @returns The dialog description element.
 */
function DialogDescription({
  className,
  ...props
}: React.ComponentProps<typeof DialogPrimitive.Description>) {
  return (
    <DialogPrimitive.Description
      className={cn('text-muted-foreground text-sm', className)}
      {...props}
    />
  );
}

export {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogOverlay,
  DialogPortal,
  DialogTitle,
  DialogTrigger,
};
