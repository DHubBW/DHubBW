import { Href, Link } from 'expo-router';
import { openBrowserAsync, WebBrowserPresentationStyle } from 'expo-web-browser';
import { type ComponentProps } from 'react';

type Props = Omit<ComponentProps<typeof Link>, 'href'> & { href: Href & string };

/**
 * Link component for external URLs.
 *
 * On native platforms (iOS/Android) the URL is opened in an in-app browser
 * instead of the system browser. On web it behaves like a regular
 * `<Link target="_blank">`.
 *
 * @param href - Fully-qualified external URL.
 * @param rest - All other Expo Router `Link` props.
 * @returns The platform-appropriate external link element.
 */
export function ExternalLink({ href, ...rest }: Props) {
  return (
    <Link
      target="_blank"
      {...rest}
      href={href}
      onPress={async (event) => {
        if (process.env.EXPO_OS !== 'web') {
          // Prevent the default behavior of linking to the default browser on native.
          event.preventDefault();
          // Open the link in an in-app browser.
          await openBrowserAsync(href, {
            presentationStyle: WebBrowserPresentationStyle.AUTOMATIC,
          });
        }
      }}
    />
  );
}
