/**
 * Unit tests for the OrdnerTree component.
 *
 * `useOrdnerChildren` is mocked so the test runs without a real
 * network layer. The component still uses {@code useQueryClient} for
 * its ancestor lookup, so each render is wrapped in a
 * {@link QueryClientProvider}.
 */
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react-native";
import * as React from "react";

import type { OrdnerDTO } from "@/lib/api/types.gen";

const childrenByParent: Record<string, OrdnerDTO[]> = {};
jest.mock("@/hooks/useOrdnerChildren", () => ({
  useOrdnerChildren: (ordnerId: string | null) => {
    if (ordnerId === null) {
      return { data: undefined, isLoading: false, isError: false };
    }
    return {
      data: childrenByParent[ordnerId] ?? [],
      isLoading: false,
      isError: false,
    };
  },
}));

import { OrdnerTree } from "@/components/dateien/OrdnerTree";

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
  hasChildren: true,
};

const CHILD_FOLDER: OrdnerDTO = {
  ordnerID: "22222222-2222-2222-2222-222222222222",
  name: "Mathematik",
  level: 1,
  farbe: "RED",
  public: false,
  ueberOrdnerID: ROOT_FOLDER.ordnerID,
  kursID: ROOT_FOLDER.kursID,
  hasChildren: false,
};

describe("OrdnerTree", () => {
  beforeEach(() => {
    Object.keys(childrenByParent).forEach((k) => delete childrenByParent[k]);
  });

  it("renders the synthetic 'Alle Dateien' root and the supplied root folders", () => {
    renderWithClient(
      <OrdnerTree
        rootFolders={[ROOT_FOLDER]}
        selectedOrdnerId={null}
        onSelect={jest.fn()}
      />,
    );
    expect(screen.getByText("Alle Dateien")).toBeTruthy();
    expect(screen.getByText("Vorlesungen")).toBeTruthy();
  });

  it("calls onSelect with the folder DTO when a folder row is tapped", () => {
    const onSelect = jest.fn();
    renderWithClient(
      <OrdnerTree
        rootFolders={[ROOT_FOLDER]}
        selectedOrdnerId={null}
        onSelect={onSelect}
      />,
    );
    fireEvent.press(screen.getByText("Vorlesungen"));
    expect(onSelect).toHaveBeenCalledWith(ROOT_FOLDER);
  });

  it("calls onSelect(null) when the root row is tapped", () => {
    const onSelect = jest.fn();
    renderWithClient(
      <OrdnerTree
        rootFolders={[ROOT_FOLDER]}
        selectedOrdnerId={ROOT_FOLDER.ordnerID}
        onSelect={onSelect}
      />,
    );
    fireEvent.press(screen.getByText("Alle Dateien"));
    expect(onSelect).toHaveBeenCalledWith(null);
  });

  it("expands a folder and lazy-loads its children on chevron click", () => {
    childrenByParent[ROOT_FOLDER.ordnerID] = [CHILD_FOLDER];
    renderWithClient(
      <OrdnerTree
        rootFolders={[ROOT_FOLDER]}
        selectedOrdnerId={null}
        onSelect={jest.fn()}
      />,
    );
    expect(screen.queryByText("Mathematik")).toBeNull();

    fireEvent.press(screen.getByLabelText("Ordner aufklappen"));

    expect(screen.getByText("Mathematik")).toBeTruthy();
  });

  it("hides the expand chevron for leaf folders (hasChildren=false)", () => {
    const LEAF: OrdnerDTO = { ...ROOT_FOLDER, hasChildren: false };
    renderWithClient(
      <OrdnerTree
        rootFolders={[LEAF]}
        selectedOrdnerId={null}
        onSelect={jest.fn()}
      />,
    );
    expect(screen.queryByLabelText("Ordner aufklappen")).toBeNull();
    expect(screen.queryByLabelText("Ordner einklappen")).toBeNull();
  });
});
