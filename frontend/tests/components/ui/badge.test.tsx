import { render, screen } from "@testing-library/react-native";
import { Badge } from "@/components/ui/badge";
import { Text } from "@/components/ui/text";

/**
 * Smoke test for the Badge component (FE-S2 UI Foundation).
 *
 * Verifies that the Badge renders its children without crashing and
 * that all visual variants work correctly.
 */
describe("Badge", () => {
  it("renders children without crashing", () => {
    render(
      <Badge>
        <Text>Aktiv</Text>
      </Badge>,
    );

    expect(screen.getByText("Aktiv")).toBeTruthy();
  });

  it("renders with secondary variant", () => {
    render(
      <Badge variant="secondary">
        <Text>Entwurf</Text>
      </Badge>,
    );

    expect(screen.getByText("Entwurf")).toBeTruthy();
  });

  it("renders with destructive variant", () => {
    render(
      <Badge variant="destructive">
        <Text>Fehler</Text>
      </Badge>,
    );

    expect(screen.getByText("Fehler")).toBeTruthy();
  });

  it("renders with outline variant", () => {
    render(
      <Badge variant="outline">
        <Text>Ausstehend</Text>
      </Badge>,
    );

    expect(screen.getByText("Ausstehend")).toBeTruthy();
  });
});
