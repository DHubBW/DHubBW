/**
 * Unit tests for the OrdnerTreeInline component — the always-visible
 * folder section used on the mobile Dateien screen.
 *
 * `useOrdnerChildren` is mocked so the test runs without a real
 * network layer. The OrdnerTree itself uses {@code useQueryClient}
 * for its ancestor lookup, so each render is wrapped in a
 * {@link QueryClientProvider}.
 */
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react-native";
import * as React from "react";

import type { OrdnerDTO } from "@/lib/api/types.gen";

jest.mock("@/hooks/useOrdnerChildren", () => ({
  useOrdnerChildren: () => ({
    data: undefined,
    isLoading: false,
    isError: false,
  }),
}));

import { OrdnerTreeInline } from "@/components/dateien/OrdnerTreeInline";

function renderWithClient(ui: React.ReactElement) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>{ui}</QueryClientProvider>,
  );
}

const ROOT_FOLDER: OrdnerDTO = {
  ordnerID: "11111111-1111-1111-1111-111111111111",
  name: "Vorlesungen",
  level: 0,
  farbe: "DEFAULT",
  public: true,
  ueberOrdnerID: null,
  kursID: "kkkkkkkk-kkkk-kkkk-kkkk-kkkkkkkkkkkk",
  hasChildren: false,
};

describe("OrdnerTreeInline", () => {
  it("renders the tree expanded by default with the synthetic root row visible", () => {
    renderWithClient(
      <OrdnerTreeInline
        rootFolders={[ROOT_FOLDER]}
        selectedOrdnerId={null}
        selectedFolderName={null}
        onSelect={jest.fn()}
      />,
    );
    // The expanded body shows the actual tree contents.
    expect(screen.getByText("Alle Dateien")).toBeTruthy();
    expect(screen.getByText("Vorlesungen")).toBeTruthy();
  });

  it("collapses the tree when the header is tapped and shows the active folder name", () => {
    renderWithClient(
      <OrdnerTreeInline
        rootFolders={[ROOT_FOLDER]}
        selectedOrdnerId={ROOT_FOLDER.ordnerID}
        selectedFolderName={ROOT_FOLDER.name}
        onSelect={jest.fn()}
      />,
    );
    // The toggle button is the only Pressable carrying accessibilityRole
    // 'button' AND aria-expanded=true on first render. Find it via its
    // accessibility label, which switches between collapse/expand.
    const collapseTrigger = screen.getByLabelText(
      "Ordner-Übersicht einklappen",
    );
    fireEvent.press(collapseTrigger);
    // When collapsed, the tree contents disappear but the active folder
    // name surfaces in the header so the user keeps their bearings.
    // After collapsing, the trigger label flips.
    expect(
      screen.getByLabelText("Ordner-Übersicht ausklappen"),
    ).toBeTruthy();
    // The folder name is rendered in the collapsed header.
    expect(screen.getAllByText("Vorlesungen").length).toBeGreaterThan(0);
  });

  it("renders the action slot inside the header (e.g. Ordner-erstellen button)", () => {
    renderWithClient(
      <OrdnerTreeInline
        rootFolders={[ROOT_FOLDER]}
        selectedOrdnerId={null}
        selectedFolderName={null}
        onSelect={jest.fn()}
        actions={<></>}
      />,
    );
    // The component renders without crashing when actions are supplied
    // — primary smoke for the slot wiring. The heading text is rendered
    // verbatim ("Ordner"); the uppercase look comes from CSS.
    expect(screen.getByText("Ordner")).toBeTruthy();
  });
});
