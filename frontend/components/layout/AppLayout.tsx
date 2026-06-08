import { useCallback, type ReactNode } from "react";
import { useWindowDimensions, View } from "react-native";

import { CookieBanner } from "@/components/CookieBanner";
import { useAuth } from "@/lib/auth/AuthContext";
import { useCookieConsent } from "@/lib/consent/useCookieConsent";

import { BottomNav } from "./BottomNav";
import { Footer } from "./Footer";
import { Header } from "./Header";
import { Sidebar } from "./Sidebar";

const BREAKPOINT = 768;

/**
 * Central app shell layout.
 *
 * Below 768 px the mobile layout is rendered (Header + Content + BottomNav).
 * At 768 px and above the desktop sidebar is shown to the left of the content.
 *
 * When cookie consent is `'unknown'` the {@link CookieBanner} overlay blocks
 * all interaction. If the user rejects cookies while logged in, `logout()` is
 * called immediately — same sequence as a manual logout (backend POST +
 * `window.location.replace('/login')` on Web).
 *
 * @param children - Page content rendered inside the content area.
 * @returns The responsive app layout.
 */
export function AppLayout({ children }: { children: ReactNode }) {
  const { width } = useWindowDimensions();
  const isWide = width >= BREAKPOINT;
  const { logout } = useAuth();
  const { reject } = useCookieConsent();

  // When the user rejects cookies from inside the app (already logged in),
  // persist the rejection and run the full logout sequence so the JWT cookie
  // is cleared server-side before the page reloads to /login.
  const handleCookieReject = useCallback(async () => {
    reject();
    await logout();
  }, [reject, logout]);

  const cookieBanner = <CookieBanner onRejectOverride={handleCookieReject} />;

  if (isWide) {
    return (
      <View className="flex-1 flex-row bg-background">
        <Sidebar />
        <View className="flex-1 flex-col">
          <View className="flex-1">{children}</View>
          <Footer />
        </View>
        {cookieBanner}
      </View>
    );
  }

  return (
    <View className="flex-1 flex-col bg-background">
      <Header />
      <View className="flex-1">{children}</View>
      <Footer />
      <BottomNav />
      {cookieBanner}
    </View>
  );
}
