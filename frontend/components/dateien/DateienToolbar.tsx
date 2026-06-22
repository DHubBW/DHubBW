/**
 * Toolbar above the file list. Hosts a slot on the right for the upload
 * button — populated by the upload work in a follow-up issue. Today the
 * toolbar shows the breadcrumb path of the active folder (or a generic
 * "Alle Dateien" label when the root view is selected).
 *
 * Search UX:
 *
 * The search lives in a pill-shaped, in-toolbar input that animates
 * open/closed to the LEFT of a magnifying-glass button. Closed, only
 * the lupe is visible; pressing it slides the rounded input out, with
 * the lupe icon migrating into the input (left edge) and the button
 * itself morphing into a close-X. The animation is a CSS transition on
 * web (cheap, smooth, no JS frames) and a Reanimated layout transition
 * on native via {@link NativeOnlyAnimatedView}.
 *
 * Filtering is purely client-side over the already-loaded list, which
 * means users only ever search files they are authorised to see — the
 * backend already restricts {@code GET /student/dateien} and
 * {@code /student/dateien/ordner/{id}} to readable files.
 */
import { ChevronRight, Search, X } from "lucide-react-native";
import { useRef, useState, type ReactNode } from "react";
import { Platform, Pressable, ScrollView, TextInput, View } from "react-native";

import { Text } from "@/components/ui/text";
import type { OrdnerDTO, TagDTO } from "@/lib/api/types.gen";
import { t } from "@/lib/i18n/t";

import { TagBadge } from "./TagBadge";

export interface DateienToolbarProps {
  /**
   * Folder currently displayed in the table, or `null` when the root view
   * (`Alle Dateien`) is active.
   */
  selected: OrdnerDTO | null;
  /** Right-aligned action slot — empty by default. */
  actions?: ReactNode;
  /** Current search term (controlled). */
  searchTerm?: string;
  /** Called when the user types in the search input. */
  onSearchTermChange?: (next: string) => void;
  /**
   * All tags known to the platform — populated by {@code useTags}.
   * Each tag renders as a toggle chip in a horizontal scroll row
   * below the search bar; tapping a chip adds or removes its UUID
   * from {@link #selectedTagIds}.
   */
  allTags?: TagDTO[];
  /** Currently active tag-filter UUIDs (controlled). */
  selectedTagIds?: string[];
  /** Called when the user toggles a tag chip. */
  onToggleTagFilter?: (tagId: string) => void;
}

/** Width of the expanded pill in pixels — wide enough for ~30 chars. */
const SEARCH_OPEN_WIDTH = 240;

/** Width of the collapsed pill — exactly the icon hit-target. */
const SEARCH_CLOSED_WIDTH = 36;

/**
 * Renders the file-list header with breadcrumb, an animated search
 * pill, and an action slot. The pill grows to the LEFT of the lupe
 * button when the toggle is active; tapping the (then X) icon
 * collapses the pill and clears the active filter so users don't leave
 * a stale search applied invisibly.
 *
 * @param props - {@link DateienToolbarProps}
 * @returns The toolbar element.
 */
export function DateienToolbar({
  selected,
  actions,
  searchTerm = "",
  onSearchTermChange,
  allTags,
  selectedTagIds,
  onToggleTagFilter,
}: DateienToolbarProps) {
  const rootLabel = t("dateien.toolbar.allFiles", "Alle Dateien");
  const placeholder = t(
    "dateien.toolbar.searchPlaceholder",
    "Dateien durchsuchen …",
  );
  const [searchOpen, setSearchOpen] = useState(false);
  const [focused, setFocused] = useState(false);
  const inputRef = useRef<TextInput>(null);
  const showTagFilter = Boolean(
    onToggleTagFilter && allTags && allTags.length > 0,
  );
  const activeFilterIds = selectedTagIds ?? [];

  // Note: we deliberately do NOT auto-focus when opening — the user
  // explicitly asked for the bar not to grab focus. They can tap the
  // input (or the lupe icon, which forwards focus) when they're ready
  // to type.

  function handleToggle() {
    if (!onSearchTermChange) return;
    if (searchOpen) {
      // Collapsing → clear so the table snaps back to the unfiltered list.
      onSearchTermChange("");
      setSearchOpen(false);
    } else {
      setSearchOpen(true);
    }
  }

  return (
    <View className="border-b border-border px-4 py-3">
      <View className="flex-row items-center justify-between gap-3">
        <View className="flex-1 flex-row items-center gap-1">
          <Text className="text-sm text-muted-foreground">{rootLabel}</Text>
          {selected && (
            <>
              <ChevronRight size={14} color="hsl(var(--muted-foreground))" />
              <Text
                className="text-sm font-semibold text-foreground"
                numberOfLines={1}
              >
                {selected.name}
              </Text>
            </>
          )}
        </View>
        <View className="flex-row items-center gap-2">
          {onSearchTermChange && (
            <SearchPill
              open={searchOpen}
              focused={focused}
              searchTerm={searchTerm}
              placeholder={placeholder}
              inputRef={inputRef}
              onSearchTermChange={onSearchTermChange}
              onToggle={handleToggle}
              onFocusChange={setFocused}
            />
          )}
          {actions}
        </View>
      </View>
      {showTagFilter && (
        <View className="mt-2">
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerClassName="flex-row items-center gap-1.5 py-0.5"
          >
            <Text className="mr-1 text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">
              {t("dateien.toolbar.tagFilter", "Tags filtern")}
            </Text>
            {(allTags ?? []).map((tag) => {
              const active = activeFilterIds.includes(tag.tagID);
              return (
                <TagBadge
                  key={tag.tagID}
                  name={tag.name}
                  variant={active ? "filter-active" : "default"}
                  onPress={() => onToggleTagFilter?.(tag.tagID)}
                  testID={`tag-filter-${tag.tagID}`}
                />
              );
            })}
            {activeFilterIds.length > 0 && (
              <Pressable
                accessibilityRole="button"
                onPress={() =>
                  // Clear by toggling each active id off — keeps the
                  // parent state machine in one place.
                  activeFilterIds.forEach(
                    (id) => onToggleTagFilter && onToggleTagFilter(id),
                  )
                }
                className="ml-1 flex-row items-center gap-1 rounded-full border border-border px-2 py-0.5 active:bg-accent"
                testID="tag-filter-clear"
              >
                <X size={11} color="hsl(var(--muted-foreground))" />
                <Text className="text-[11px] font-medium text-muted-foreground">
                  {t("dateien.toolbar.tagFilterClear", "Filter zurücksetzen")}
                </Text>
              </Pressable>
            )}
          </ScrollView>
        </View>
      )}
    </View>
  );
}

interface SearchPillProps {
  open: boolean;
  focused: boolean;
  searchTerm: string;
  placeholder: string;
  inputRef: React.RefObject<TextInput | null>;
  onSearchTermChange: (next: string) => void;
  onToggle: () => void;
  onFocusChange: (focused: boolean) => void;
}

/**
 * A single rounded "pill" that morphs between two states:
 *
 * - **Closed**: a 36 × 36 circle with just the lupe icon (acts as a
 *   button).
 * - **Open**: a wider rounded-full bar with a leading lupe icon, the
 *   text input, an inline clear-X (when there is text), and a
 *   trailing close-X that toggles back to the closed state.
 *
 * Width animates between the two via a CSS `transition` on web and a
 * Reanimated layout transition on native (Animated.View animates style
 * deltas including `width` automatically when wrapped). A focus ring
 * (`ring`-style box-shadow) lights up while the input has focus, using
 * the project's `--ring` token so it follows the active theme.
 */
function SearchPill({
  open,
  focused,
  searchTerm,
  placeholder,
  inputRef,
  onSearchTermChange,
  onToggle,
  onFocusChange,
}: SearchPillProps) {
  const isWeb = Platform.OS === "web";

  // The container morphs width + border-colour. On web we lean on a CSS
  // transition; on native the same style update triggers a layout
  // animation when wrapped in Animated.View. The shadow comes alive on
  // focus to give a real "feels-active" cue without the colour-noise
  // of a full primary fill.
  const containerStyle = [
    {
      width: open ? SEARCH_OPEN_WIDTH : SEARCH_CLOSED_WIDTH,
      height: 36,
    },
    isWeb && {
      transition:
        "width 220ms cubic-bezier(0.22, 1, 0.36, 1), " +
        "box-shadow 180ms ease, " +
        "border-color 180ms ease, " +
        "background-color 180ms ease",
      boxShadow: focused
        ? "0 0 0 3px hsl(var(--ring) / 0.25), 0 1px 2px rgba(0,0,0,0.04)"
        : "0 1px 2px rgba(0,0,0,0.04)",
    },
  ];

  const containerClass =
    "flex-row items-center overflow-hidden rounded-full border bg-card " +
    (focused
      ? "border-ring"
      : open
        ? "border-border"
        : "border-border hover:border-foreground/30");

  return (
    <View
      testID="dateien-search-pill"
      className={containerClass}
      style={containerStyle as never}
    >
      {/* Leading lupe icon — also acts as the open-toggle when closed. */}
      <Pressable
        testID="dateien-search-toggle"
        accessibilityRole="button"
        accessibilityLabel={
          open
            ? t("dateien.toolbar.searchClose", "Suche schließen")
            : t("dateien.toolbar.searchOpen", "Dateien durchsuchen")
        }
        accessibilityState={{ expanded: open }}
        onPress={open ? () => inputRef.current?.focus() : onToggle}
        className="h-9 w-9 items-center justify-center"
      >
        <Search
          size={16}
          color={
            focused
              ? "hsl(var(--ring))"
              : open
                ? "hsl(var(--foreground))"
                : "hsl(var(--muted-foreground))"
          }
          strokeWidth={2}
        />
      </Pressable>
      {/* Input + trailing affordances render only when open so the
          collapsed state stays cleanly the icon. */}
      {open && (
        <>
          <TextInput
            ref={inputRef}
            testID="dateien-search-input"
            value={searchTerm}
            onChangeText={onSearchTermChange}
            onFocus={() => onFocusChange(true)}
            onBlur={() => onFocusChange(false)}
            onSubmitEditing={onToggle}
            placeholder={placeholder}
            placeholderTextColor="hsl(var(--muted-foreground))"
            accessibilityLabel={placeholder}
            returnKeyType="search"
            className="flex-1 text-sm text-foreground"
            style={
              {
                paddingVertical: 0,
                outlineWidth: 0,
              } as never
            }
          />
          {searchTerm.length > 0 && (
            <Pressable
              testID="dateien-search-clear"
              accessibilityRole="button"
              accessibilityLabel={t(
                "dateien.toolbar.searchClear",
                "Suche zurücksetzen",
              )}
              onPress={() => {
                onSearchTermChange("");
                inputRef.current?.focus();
              }}
              className="mr-1 h-6 w-6 items-center justify-center rounded-full active:bg-muted"
            >
              <X size={12} color="hsl(var(--muted-foreground))" />
            </Pressable>
          )}
          {/* Trailing close button — collapses the pill back. */}
          <Pressable
            testID="dateien-search-close"
            accessibilityRole="button"
            accessibilityLabel={t(
              "dateien.toolbar.searchClose",
              "Suche schließen",
            )}
            onPress={onToggle}
            className="h-9 w-9 items-center justify-center border-l border-border active:bg-accent"
          >
            <X size={14} color="hsl(var(--muted-foreground))" />
          </Pressable>
        </>
      )}
    </View>
  );
}
