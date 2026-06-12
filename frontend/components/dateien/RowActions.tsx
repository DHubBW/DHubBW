/**
 * Row-level action icons for a single file.
 *
 * <p>Layout:</p>
 * <ul>
 *   <li><b>Vorschau</b> — primary, always visible.</li>
 *   <li><b>Download</b> — primary, always visible.</li>
 *   <li><b>3-Punkte-Menü</b> — secondary actions (Verschieben,
 *       Umbenennen, Löschen) collapse behind {@code MoreVertical}.
 *       The menu is owned by the parent screen via
 *       {@link onMore}; this component just exposes the trigger.</li>
 * </ul>
 *
 * <p>The two-tier layout keeps the row visually quiet: only two
 * icons sit in every line, and destructive / less-frequent actions
 * stay one click away without cluttering the layout.</p>
 *
 * <p>Visuelles:</p>
 * <ul>
 *   <li>Icons sind im Ruhezustand zurückhaltend ({@code opacity-70})
 *       und werden erst beim Hover/Press farbig — die Aktionsspalte
 *       überstrahlt das Schriftbild der Zeile nicht.</li>
 *   <li>Auf Web tragen die Buttons einen nativen {@code title}-Tooltip,
 *       damit der Aktionsname ohne Long-Press lesbar wird.</li>
 * </ul>
 */
import {
  Download,
  Eye,
  MoreVertical,
  type LucideIcon,
} from "lucide-react-native";
import { Platform, TouchableOpacity, View } from "react-native";

import { t } from "@/lib/i18n/t";
import { cn } from "@/lib/utils";

export interface RowActionsProps {
  /** Optional preview trigger. When omitted the icon is suppressed. */
  onPreview?: () => void;
  /** Optional download trigger. When omitted the icon is suppressed. */
  onDownload?: () => void;
  /**
   * Optional opener for the secondary actions menu (Verschieben,
   * Umbenennen, Löschen). When omitted the 3-dot icon is hidden.
   */
  onMore?: () => void;
  /** Disable interaction while a mutation is in flight for this row. */
  disabled?: boolean;
  /** Extra className for the container — used to align inside columns. */
  className?: string;
  /**
   * `compact` is the desktop-table variant with 16-px icons; `comfortable`
   * widens the hit-target to 40 px for the mobile card layout. Defaults
   * to `compact`.
   */
  size?: "compact" | "comfortable";
}

/**
 * Renders the row's action icon group.
 *
 * @param props - {@link RowActionsProps}
 * @returns The action button group.
 */
export function RowActions({
  onPreview,
  onDownload,
  onMore,
  disabled = false,
  className,
  size = "compact",
}: RowActionsProps) {
  const iconSize = size === "comfortable" ? 18 : 16;
  return (
    <View
      className={cn(
        "flex-row items-center justify-end gap-0.5",
        className,
      )}
    >
      {onPreview && (
        <ActionButton
          icon={Eye}
          iconSize={iconSize}
          onPress={onPreview}
          disabled={disabled}
          size={size}
          label={t("dateien.preview.open", "Vorschau öffnen")}
          testID="row-action-preview"
        />
      )}
      {onDownload && (
        <ActionButton
          icon={Download}
          iconSize={iconSize}
          onPress={onDownload}
          disabled={disabled}
          size={size}
          label={t("dateien.download", "Herunterladen")}
          testID="row-action-download"
        />
      )}
      {onMore && (
        <ActionButton
          icon={MoreVertical}
          iconSize={iconSize}
          onPress={onMore}
          disabled={disabled}
          size={size}
          label={t("dateien.menu.open", "Weitere Aktionen")}
          testID="row-action-more"
        />
      )}
    </View>
  );
}

interface ActionButtonProps {
  icon: LucideIcon;
  iconSize: number;
  onPress: () => void;
  disabled: boolean;
  size: "compact" | "comfortable";
  label: string;
  testID: string;
}

/**
 * Single action button. Encapsulates the shared layout, hover/press
 * affordances and accessibility labels so {@link RowActions} stays
 * free of repetition.
 */
function ActionButton({
  icon: Icon,
  iconSize,
  onPress,
  disabled,
  size,
  label,
  testID,
}: ActionButtonProps) {
  const containerClass = cn(
    "items-center justify-center rounded-md",
    "transition-colors",
    size === "comfortable" ? "h-10 w-10" : "h-8 w-8",
    "active:bg-accent web:hover:bg-accent",
    disabled && "opacity-50",
  );
  const webProps =
    Platform.OS === "web"
      ? ({ "aria-label": label, title: label } as object)
      : {};
  return (
    <TouchableOpacity
      activeOpacity={0.7}
      onPress={onPress}
      disabled={disabled}
      accessibilityRole="button"
      accessibilityLabel={label}
      testID={testID}
      className={containerClass}
      {...webProps}
    >
      <View className="opacity-70 web:hover:opacity-100">
        <Icon size={iconSize} color="hsl(var(--foreground))" />
      </View>
    </TouchableOpacity>
  );
}
