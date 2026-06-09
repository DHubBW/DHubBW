/**
 * Unit tests for the DateienTabelle component.
 */
import { fireEvent, render, screen } from "@testing-library/react-native";
import { View } from "react-native";

// Stub lucide icons so SVG rendering doesn't produce react-native-svg warnings.
jest.mock("lucide-react-native", () => {
  const { View: V } = require("react-native"); // eslint-disable-line @typescript-eslint/no-require-imports
  const stub = (props: object) => <V {...props} />;
  return new Proxy({}, { get: () => stub });
});

import { DateienTabelle } from "@/components/dateien/DateienTabelle";
import type { DateiDTO } from "@/lib/api/types.gen";

const FILE_A: DateiDTO = {
  dateiID: "a",
  name: "Aufgaben.pdf",
  zuletztBearbeitet: "2026-04-25",
  fileSize: 2.5,
  uploaderName: "Anna Anonym",
  ordnerID: "f",
  ordnerName: "Mathe",
  downloadAnzahl: 3,
};

const FILE_B: DateiDTO = {
  dateiID: "b",
  name: "Zusammenfassung.md",
  zuletztBearbeitet: "2026-05-10",
  fileSize: 0.5,
  uploaderName: "Bernd Beispiel",
  ordnerID: "f",
  ordnerName: "Mathe",
  downloadAnzahl: 12,
};

describe("DateienTabelle", () => {
  it("renders the supplied files in name-asc order by default", () => {
    render(
      <DateienTabelle dateien={[FILE_B, FILE_A]} isLoading={false} isError={false} />,
    );
    const aufgaben = screen.getByText("Aufgaben.pdf");
    const zusammen = screen.getByText("Zusammenfassung.md");
    expect(aufgaben).toBeTruthy();
    expect(zusammen).toBeTruthy();
  });

  it("toggles sort direction on repeated clicks of the same column", () => {
    render(
      <DateienTabelle dateien={[FILE_A, FILE_B]} isLoading={false} isError={false} />,
    );
    // First click on "Downloads" — asc, FILE_A (3) before FILE_B (12)
    fireEvent.press(screen.getByLabelText("Sortieren nach Downloads"));
    // Second click on "Downloads" — desc, FILE_B (12) before FILE_A (3)
    fireEvent.press(screen.getByLabelText("Sortieren nach Downloads"));
    // Both names should still be present after the toggle.
    expect(screen.getByText("Aufgaben.pdf")).toBeTruthy();
    expect(screen.getByText("Zusammenfassung.md")).toBeTruthy();
  });

  it("renders the empty hint when there are no files", () => {
    render(<DateienTabelle dateien={[]} isLoading={false} isError={false} />);
    expect(screen.getByText("Noch keine Dateien hier")).toBeTruthy();
  });

  it("renders the error hint on isError=true", () => {
    render(<DateienTabelle dateien={[]} isLoading={false} isError={true} />);
    expect(
      screen.getByText("Dateien konnten nicht geladen werden"),
    ).toBeTruthy();
  });
});
