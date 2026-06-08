import { NativeOnlyAnimatedView } from '@/components/ui/native-only-animated-view';
import { TextClassContext } from '@/components/ui/text';
import { cn } from '@/lib/utils';
import * as TooltipPrimitive from '@rn-primitives/tooltip';
import * as React from 'react';
import { Platform, StyleSheet } from 'react-native';
import { FadeInDown, FadeInUp, FadeOut } from 'react-native-reanimated';
import { FullWindowOverlay as RNFullWindowOverlay } from 'react-native-screens';

/** Root tooltip component – wraps `@rn-primitives/tooltip` Root. */
const Tooltip = TooltipPrimitive.Root;

/** Tooltip trigger – wraps `@rn-primitives/tooltip` Trigger. */
const TooltipTrigger = TooltipPrimitive.Trigger;

const FullWindowOverlay = Platform.OS === 'ios' ? RNFullWindowOverlay : React.Fragment;

/**
 * Compute entering/exiting animations only on native to avoid Reanimated web
 * warnings about unsupported modifiers (`.withInitialValues` is not supported
 * on web). On web, CSS animations via Tailwind handle the tooltip animation.
 */
function getEnteringAnimation(side: string) {
  if (Platform.OS === 'web') return undefined;
  return side === 'top'
    ? FadeInDown.withInitialValues({ transform: [{ translateY: 3 }] }).duration(150)
    : FadeInUp.withInitialValues({ transform: [{ translateY: -5 }] });
}

function getExitingAnimation() {
  if (Platform.OS === 'web') return undefined;
  return FadeOut;
}

/**
 * Animated tooltip content with portal support.
 *
 * Renders inside a portal and applies an entrance/exit animation depending on
 * the `side` prop. On iOS the content is wrapped in `FullWindowOverlay` to
 * ensure correct z-ordering above modals.
 *
 * @param className - Additional Tailwind classes.
 * @param sideOffset - Distance in pixels between trigger and tooltip (default: `4`).
 * @param portalHost - Named portal host to render into (optional).
 * @param side - Side relative to the trigger where the tooltip appears (default: `'top'`).
 * @param props - All other `TooltipPrimitive.Content` props.
 * @returns The animated tooltip content element.
 */
function TooltipContent({
  className,
  sideOffset = 4,
  portalHost,
  side = 'top',
  ...props
}: React.ComponentProps<typeof TooltipPrimitive.Content> & {
    portalHost?: string;
  }) {
  return (
    <TooltipPrimitive.Portal hostName={portalHost}>
      <FullWindowOverlay>
        <TooltipPrimitive.Overlay style={Platform.select({ native: StyleSheet.absoluteFill })}>
          <NativeOnlyAnimatedView
            entering={
              side === 'top'
                ? FadeInDown.withInitialValues({ transform: [{ translateY: 3 }] }).duration(150)
                : FadeInUp.withInitialValues({ transform: [{ translateY: -5 }] })
            }
            exiting={FadeOut}>
            <TextClassContext.Provider value="text-xs text-primary-foreground">
              <TooltipPrimitive.Content
                sideOffset={sideOffset}
                className={cn(
                  'bg-primary z-50 rounded-md px-3 py-2 sm:py-1.5',
                  Platform.select({
                    web: cn(
                      'animate-in fade-in-0 zoom-in-95 origin-(--radix-tooltip-content-transform-origin) w-fit text-balance',
                      side === 'bottom' && 'slide-in-from-top-2',
                      side === 'left' && 'slide-in-from-right-2',
                      side === 'right' && 'slide-in-from-left-2',
                      side === 'top' && 'slide-in-from-bottom-2'
                    ),
                  }),
                  className
                )}
                side={side}
                {...props}
              />
            </TextClassContext.Provider>
          </NativeOnlyAnimatedView>
        </TooltipPrimitive.Overlay>
      </FullWindowOverlay>
    </TooltipPrimitive.Portal>
  );
}

export { Tooltip, TooltipContent, TooltipTrigger };
