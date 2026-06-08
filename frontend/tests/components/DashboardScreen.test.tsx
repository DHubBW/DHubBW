/**
 * Unit tests for the DashboardScreen — one smoke test per role variant.
 *
 * The hooks are mocked so the component renders synchronously without
 * a real QueryClient or network layer.
 */
import { render, screen } from "@testing-library/react-native";

jest.mock("@/hooks/useDashboardMeals", () => ({
  useDashboardMeals: () => ({ data: [], isLoading: false, isError: false }),
}));

jest.mock("@/hooks/useDashboardDateien", () => ({
  useDashboardDateien: () => ({ data: [], isLoading: false, isError: false }),
}));

jest.mock("@/hooks/useKursAdminStats", () => ({
  useKursAdminStats: () => ({
    data: { anzahlMitglieder: 5, anzahlDateien: 67, anzahlAnfragen: 2 },
    isLoading: false,
  }),
}));

jest.mock("@/hooks/useSysAdminStats", () => ({
  useSysAdminStats: () => ({
    data: {
      anzahlKurse: 3,
      anzahlNutzer: 8,
      anzahlDateien: 67,
      megabyteBelegt: 120,
      megabyteVerfuegbar: 880,
      anzahlKurserstellungsanfragen: 1,
    },
    isLoading: false,
  }),
}));

jest.mock("@tanstack/react-query", () => ({
  ...jest.requireActual("@tanstack/react-query"),
  useQueryClient: () => ({ invalidateQueries: jest.fn() }),
}));

jest.mock("@/components/auth/NewKursDialog", () => ({
  NewKursDialog: () => null,
}));

jest.mock("@/components/auth/RegisterStep2", () => ({
  RegisterStep2: () => null,
}));

const mockUseAuth = jest.fn();
jest.mock("@/lib/auth/AuthContext", () => ({
  useAuth: () => mockUseAuth(),
}));

import DashboardScreen from "@/app/(app)/dashboard";

describe("DashboardScreen", () => {
  it("renders student variant with greeting and quick access", () => {
    mockUseAuth.mockReturnValue({
      user: {
        name: "Anna Schmidt",
        studentType: "STUDENT",
        kursName: "TINF24B2",
        dhbwMail: "anna.schmidt.a24@student.dhbw-karlsruhe.de",
        pendingKursRequest: null,
      },
    });

    render(<DashboardScreen />);

    expect(screen.getByText(/Anna Schmidt/)).toBeTruthy();
    expect(screen.getByText("TINF24B2")).toBeTruthy();
    expect(screen.getByText("Schnellzugriff")).toBeTruthy();
    expect(screen.getByText("Mensa Heute")).toBeTruthy();
    expect(screen.getByText("Neueste Dateien")).toBeTruthy();
  });

  it("renders kursadmin variant with course stats card", () => {
    mockUseAuth.mockReturnValue({
      user: {
        name: "Max Müller",
        studentType: "KURSADMIN",
        kursName: "TINF24B2",
        dhbwMail: "max.mueller.a24@student.dhbw-karlsruhe.de",
        pendingKursRequest: null,
      },
    });

    render(<DashboardScreen />);

    expect(screen.getByText(/Max Müller/)).toBeTruthy();
    expect(screen.getByText("Kurs-Übersicht")).toBeTruthy();
    expect(screen.getByText("Mitglieder")).toBeTruthy();
  });

  it("renders sysadmin variant with platform KPI and kurs stats", () => {
    mockUseAuth.mockReturnValue({
      user: {
        name: "System-Admin",
        studentType: "SYSTEMADMIN",
        kursName: "TINF24B2",
        dhbwMail: "system.admin.a24@student.dhbw-karlsruhe.de",
        pendingKursRequest: null,
      },
    });

    render(<DashboardScreen />);

    expect(screen.getByText(/System-Admin/)).toBeTruthy();
    expect(screen.getByText("Plattform-Übersicht")).toBeTruthy();
    expect(screen.getByText("Kurs-Übersicht")).toBeTruthy();
    expect(screen.getByText("Kurse aktiv")).toBeTruthy();
  });

  it("shows Kurs-einrichten card when student has no course", () => {
    mockUseAuth.mockReturnValue({
      user: {
        name: "Neuer Student",
        studentType: "STUDENT",
        kursName: null,
        dhbwMail: "neuer.student.a24@student.dhbw-karlsruhe.de",
        pendingKursRequest: null,
      },
    });

    render(<DashboardScreen />);

    expect(screen.getByText("Kurs einrichten")).toBeTruthy();
    expect(
      screen.getByText("Du bist noch keinem Kurs zugeordnet."),
    ).toBeTruthy();
    expect(screen.getByText("Kurs beitreten")).toBeTruthy();
    expect(screen.getByText("Neuen Kurs anfragen")).toBeTruthy();
  });

  it("sysadmin without kursName still sees Neueste Dateien", () => {
    mockUseAuth.mockReturnValue({
      user: {
        name: "System-Admin",
        studentType: "SYSTEMADMIN",
        kursName: null,
        dhbwMail: "system.admin.a24@student.dhbw-karlsruhe.de",
        pendingKursRequest: null,
      },
    });

    render(<DashboardScreen />);

    expect(screen.getByText("Neueste Dateien")).toBeTruthy();
  });

  it("shows PendingRequestCard for BEITRITT when student has pending request", () => {
    mockUseAuth.mockReturnValue({
      user: {
        name: "Warte Student",
        studentType: "STUDENT",
        kursName: null,
        dhbwMail: "warte.student.a24@student.dhbw-karlsruhe.de",
        pendingKursRequest: {
          type: "BEITRITT",
          kursName: "TINF24B2",
          anfrageDatum: "2026-06-01",
        },
      },
    });

    render(<DashboardScreen />);

    expect(screen.getByText("Anfrage ausstehend")).toBeTruthy();
    expect(
      screen.getByText(
        "Deine Beitrittsanfrage für TINF24B2 ist eingegangen und wartet auf Bearbeitung durch den Kursadmin.",
      ),
    ).toBeTruthy();
  });

  it("shows PendingRequestCard for ERSTELLUNG when student has pending creation request", () => {
    mockUseAuth.mockReturnValue({
      user: {
        name: "Gruender Student",
        studentType: "STUDENT",
        kursName: null,
        dhbwMail: "gruender.student.a24@student.dhbw-karlsruhe.de",
        pendingKursRequest: {
          type: "ERSTELLUNG",
          kursName: "TINF26A1",
          anfrageDatum: "2026-06-02",
        },
      },
    });

    render(<DashboardScreen />);

    expect(screen.getByText("Anfrage ausstehend")).toBeTruthy();
    expect(screen.queryByText("Kurs einrichten")).toBeNull();
  });
});
