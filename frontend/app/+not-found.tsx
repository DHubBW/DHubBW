import { NotFoundScreen } from '@/components/NotFoundScreen';

/**
 * Route handler for unknown paths – renders the {@link NotFoundScreen}.
 *
 * @returns The 404 screen component.
 */
export default function NotFoundRoute() {
  return <NotFoundScreen />;
}
