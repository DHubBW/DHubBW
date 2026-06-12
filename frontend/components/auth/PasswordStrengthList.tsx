import { CheckCircle2, XCircle } from "lucide-react-native";
import * as React from "react";
import { View } from "react-native";

import { Icon } from "@/components/ui/icon";
import { Text } from "@/components/ui/text";
import { evaluatePassword } from "@/lib/auth/passwordPolicy";

/** Props for {@link PasswordStrengthList}. */
export interface PasswordStrengthListProps {
  /** Current password input. */
  value: string;
}

/**
 * Live, per-rule visualisation of the project's password policy. Each rule
 * renders as a row with a check or x icon plus its German label. Rows that
 * have not yet passed are dimmed via opacity-60 + muted text colour.
 *
 * Tightly coupled to {@link evaluatePassword}; if the policy grows, the rule
 * list grows here automatically.
 *
 * @param props - {@link PasswordStrengthListProps}
 * @returns The rendered rule list.
 */
export function PasswordStrengthList(
  props: PasswordStrengthListProps,
): React.JSX.Element {
  const { value } = props;
  const { rules } = evaluatePassword(value);
  return (
    <View className="mt-1.5 gap-1">
      {rules.map((rule) => (
        <View key={rule.id} className="flex-row items-center gap-2">
          <Icon
            as={rule.passed ? CheckCircle2 : XCircle}
            className={
              rule.passed
                ? "size-3.5 text-foreground opacity-100"
                : "size-3.5 text-muted-foreground opacity-60"
            }
          />
          <Text
            className={
              rule.passed
                ? "text-xs text-foreground opacity-100"
                : "text-xs text-muted-foreground opacity-60"
            }
          >
            {rule.label}
          </Text>
        </View>
      ))}
    </View>
  );
}
