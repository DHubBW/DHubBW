import { useCallback, useRef } from 'react';
import { Platform, Text, TextInput, View } from 'react-native';

import { Input } from '@/components/ui/input';
import { cn } from '@/lib/utils';

/** The fixed email domain for all DHBW Karlsruhe students. */
export const DHBW_MAIL_DOMAIN = '@student.dhbw-karlsruhe.de';

/**
 * Extracts the local part from an email value.
 * If the value contains `@student.dhbw-karlsruhe.de`, strips it.
 * If the value contains any other `@`, returns the full value unchanged
 * (the user is typing a custom domain — don't interfere).
 */
export function extractLocalPart(value: string): string {
  const lower = value.toLowerCase();
  if (lower.endsWith(DHBW_MAIL_DOMAIN)) {
    return value.slice(0, -DHBW_MAIL_DOMAIN.length);
  }
  return value;
}

/**
 * Builds the full email from the local part by appending the DHBW domain.
 * If the value already contains `@`, it is returned as-is (user typed full email).
 */
export function buildFullEmail(localPart: string): string {
  if (localPart.includes('@')) {
    return localPart;
  }
  return localPart + DHBW_MAIL_DOMAIN;
}

export interface EmailInputProps {
  /** The full email value (with domain). */
  value: string;
  /** Called with the full email (domain appended automatically). */
  onChangeText: (fullEmail: string) => void;
  /** Called when input loses focus. */
  onBlur?: () => void;
  /** Accessibility label for the input. */
  accessibilityLabel?: string;
  /** Native ID link for the label. */
  'aria-labelledby'?: string;
  /** Whether the input is editable. */
  editable?: boolean;
  /** Test ID for testing purposes. */
  testID?: string;
}

/**
 * Email input with automatic `@student.dhbw-karlsruhe.de` domain suffix.
 *
 * The user only needs to type the local part (e.g. `nachname.vorname.a24`).
 * The domain is shown as a non-editable suffix next to the input. If the user
 * pastes or types a full email containing `@`, the domain suffix is hidden and
 * the full value is used as-is.
 *
 * @returns A composite input with domain suffix.
 */
export function EmailInput({
  value,
  onChangeText,
  onBlur,
  accessibilityLabel,
  'aria-labelledby': ariaLabelledBy,
  editable = true,
  testID,
}: EmailInputProps) {
  const inputRef = useRef<TextInput>(null);

  // Determine what to show in the input field
  const localPart = extractLocalPart(value);
  const showSuffix = !localPart.includes('@');

  const handleChangeText = useCallback(
    (text: string) => {
      // If user types/pastes something with @, use it as the full email
      if (text.includes('@')) {
        onChangeText(text);
      } else {
        // Otherwise append domain automatically
        onChangeText(text + DHBW_MAIL_DOMAIN);
      }
    },
    [onChangeText],
  );

  return (
    <View
      className={cn(
        'dark:bg-input/30 border-input bg-background flex h-10 w-full min-w-0 flex-row items-center rounded-md border shadow-sm shadow-black/5 sm:h-9',
        editable === false &&
          cn(
            'opacity-50',
            Platform.select({
              web: 'disabled:pointer-events-none disabled:cursor-not-allowed',
            }),
          ),
        Platform.select({
          web: cn(
            'transition-[color,box-shadow]',
            'focus-within:border-ring focus-within:ring-ring/50 focus-within:ring-[3px]',
          ),
        }),
      )}
      testID={testID}
    >
      <TextInput
        ref={inputRef}
        className={cn(
          'text-foreground flex-1 bg-transparent px-3 py-1 text-base leading-5 outline-none',
          Platform.select({
            web: cn('placeholder:text-muted-foreground md:text-sm'),
            native: 'placeholder:text-muted-foreground/50',
          }),
        )}
        value={localPart}
        onChangeText={handleChangeText}
        onBlur={onBlur}
        accessibilityLabel={accessibilityLabel}
        aria-labelledby={ariaLabelledBy}
        autoCapitalize="none"
        autoComplete="email"
        autoCorrect={false}
        keyboardType="email-address"
        inputMode="email"
        placeholder="nachname.vorname.a24"
        editable={editable}
        testID={testID ? `${testID}-input` : undefined}
      />
      {showSuffix && (
        <Text
          className={cn(
            'text-muted-foreground pr-3 text-base leading-5',
            Platform.select({ web: 'md:text-sm' }),
          )}
          testID={testID ? `${testID}-suffix` : undefined}
        >
          {DHBW_MAIL_DOMAIN}
        </Text>
      )}
    </View>
  );
}
