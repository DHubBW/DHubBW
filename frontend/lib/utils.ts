import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

/**
 * Merges multiple Tailwind CSS class values into a single deduplicated string.
 * Conflicts (e.g. `p-2` vs `p-4`) are resolved automatically by tailwind-merge.
 *
 * @param inputs - Any number of class values (strings, objects, arrays).
 * @returns A merged, deduplicated class string.
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
