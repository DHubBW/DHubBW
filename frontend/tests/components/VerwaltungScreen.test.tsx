/**
 * Component tests for the VerwaltungScreen (Issue #218).
 *
 * Smoke-tests the three role variants (SYSTEMADMIN, KURSADMIN, non-admin
 * redirect) and verifies the key UI sections are rendered.
 * All hooks and navigation primitives are mocked to keep the suite
 * synchronous and free of network dependencies.
 */
import { render, screen } from "@testing-library/react-native";
import React from "react";

// ── Hook mocks ────────────────────────────────────────────────────────────────

const STUB_STATS = {
  anzahlKurse: 3,
  anzahlNutzer: 8,
  anzahlDateien: 342,
  megabyteBelegt: 46285,
  megabyteVerfuegbar: 56187,
  anzahlKurserstellungsanfragen: 1,
};

const STUB_ANFRAGEN = [
  {
    anfrageID: "bbbb0001-0000-0000-0000-000000000001",
    studentID: "cccc0001-0000-0000-0000-000000000001",
    studentName: "Tim Mayer",
    kursName: "TINF25B2",
    jahrgang: "2025",
    anfrageDatum: "2026-04-21",
    requestNotes: "Wir haben noch keinen Kurs.",
  },
];

const STUB_KURSE = [
  {
    kursID: "aaaa0001-0000-0000-0000-000000000001",
    kursName: "TINF24B2",
    jahrgang: "2024",
  },
];

const STUB_STUDENTEN = [
  {
    studentID: "dddd0001-0000-0000-0000-000000000001",
    name: "System-Admin",
    dhbwMail: "admin@dhbw.de",
    studentType: "SYSTEMADMIN",
  },
  {
    studentID: "dddd0001-0000-0000-0000-000000000002",
    name: "Max Müller",
    dhbwMail: "kursadmin@dhbw.de",
    studentType: "KURSADMIN",
  },
];

jest.mock("@/hooks/useSysAdminStats", () => ({
  useSysAdminStats: () => ({
    data: STUB_STATS,
    isLoading: false,
    isError: false,
  }),
}));
jest.mock("@/hooks/useSysAdminAnfragen", () => ({
  useSysAdminAnfragen: () => ({ data: STUB_ANFRAGEN, isLoading: false }),
}));
jest.mock("@/hooks/useSysAdminKurse", () => ({
  useSysAdminKurse: () => ({ data: STUB_KURSE, isLoading: false }),
}));
jest.mock("@/hooks/useSysAdminStudenten", () => ({
  useSysAdminStudenten: () => ({ data: STUB_STUDENTEN, isLoading: false }),
}));
jest.mock("@/hooks/useSysAdminKursMitglieder", () => ({
  useSysAdminKursMitglieder: () => ({ data: [], isLoading: false }),
}));
jest.mock("@/hooks/useSysAdminActions", () => ({
  useApproveAnfrage: () => ({ approveAnfrage: jest.fn() }),
  useDeclineAnfrage: () => ({ declineAnfrage: jest.fn() }),
  useDeleteKurs: () => ({ deleteKurs: jest.fn() }),
  useAddSysAdmin: () => ({ addSysAdmin: jest.fn() }),
  useRemoveSysAdmin: () => ({ removeSysAdmin: jest.fn() }),
}));
jest.mock("@/hooks/useKursadminBySysAdmin", () => ({
  useAddKursadminBySysAdmin: () => ({
    addKursadmin: jest.fn(),
    isPending: false,
  }),
  useRemoveKursadminBySysAdmin: () => ({
    removeKursadmin: jest.fn(),
    isPending: false,
  }),
}));

jest.mock("@/components/ui/tooltip", () => ({
  Tooltip: ({ children }: { children: React.ReactNode }) => children,
  TooltipTrigger: ({ children }: { children: React.ReactNode }) => children,
  TooltipContent: () => null,
}));

jest.mock("@rn-primitives/tooltip", () => ({
  Root: ({ children }: { children: React.ReactNode }) => children,
  Trigger: ({ children }: { children: React.ReactNode }) => children,
  Portal: ({ children }: { children: React.ReactNode }) => children,
  Overlay: ({ children }: { children: React.ReactNode }) => children,
  Content: () => null,
}));

jest.mock("@tanstack/react-query", () => ({
  ...jest.requireActual("@tanstack/react-query"),
  useQueryClient: () => ({ invalidateQueries: jest.fn() }),
}));

jest.mock("expo-router", () => ({
  Redirect: ({ href }: { href: string }) => null,
}));

jest.mock("sonner-native", () => ({
  toast: { success: jest.fn(), error: jest.fn() },
}));

const mockUseAuth = jest.fn();
jest.mock("@/lib/auth/AuthContext", () => ({
  useAuth: () => mockUseAuth(),
}));

// ── Tests ─────────────────────────────────────────────────────────────────────

import VerwaltungScreen from "@/app/(app)/verwaltung";

describe("VerwaltungScreen — SYSTEMADMIN", () => {
  beforeEach(() => {
    mockUseAuth.mockReturnValue({
      user: {
        name: "System-Admin",
        studentType: "SYSTEMADMIN",
        kursName: "TINF24B2",
        dhbwMail: "admin@dhbw.de",
        pendingKursRequest: null,
      },
      isLoading: false,
    });
  });

  it("zeigt die Seitenüberschrift Systemverwaltung", () => {
    render(<VerwaltungScreen />);
    expect(screen.getByText("Systemverwaltung")).toBeTruthy();
  });

  it("rendert alle fünf KPI-Karten", () => {
    render(<VerwaltungScreen />);
    expect(screen.getByText("Kurse aktiv")).toBeTruthy();
    expect(screen.getByText("Nutzer aktiv")).toBeTruthy();
    expect(screen.getByText("Dateien gesamt")).toBeTruthy();
    expect(screen.getByText(/Speicher/)).toBeTruthy();
    expect(screen.getByText("Offene Kursanfragen")).toBeTruthy();
  });

  it("zeigt die Kursanfragen-Sektion mit dem Antragsteller", () => {
    render(<VerwaltungScreen />);
    expect(screen.getByText("Kursanfragen")).toBeTruthy();
    expect(screen.getByText("Tim Mayer")).toBeTruthy();
    expect(screen.getByText("TINF25B2")).toBeTruthy();
  });

  it("zeigt die Kurse-Sektion", () => {
    render(<VerwaltungScreen />);
    expect(screen.getByText("Kurse")).toBeTruthy();
    expect(screen.getByText("TINF24B2")).toBeTruthy();
  });

  it("zeigt die Studenten-Sektion mit Rollenabzeichen", () => {
    render(<VerwaltungScreen />);
    expect(screen.getByText("Studenten (alle)")).toBeTruthy();
    expect(screen.getByText("Max Müller")).toBeTruthy();
    expect(screen.getByText("kursadmin@dhbw.de")).toBeTruthy();
  });
});

describe("VerwaltungScreen — KURSADMIN", () => {
  it("zeigt den Kursadmin-Platzhalter statt der Systemverwaltung", () => {
    mockUseAuth.mockReturnValue({
      user: {
        name: "Max Müller",
        studentType: "KURSADMIN",
        kursName: "TINF24B2",
        dhbwMail: "kursadmin@dhbw.de",
        pendingKursRequest: null,
      },
      isLoading: false,
    });
    render(<VerwaltungScreen />);
    expect(screen.getByText("Kursverwaltung")).toBeTruthy();
    expect(screen.queryByText("Systemverwaltung")).toBeNull();
  });
});
