/**
 * Unit tests for the OrdnerTree component.
 *
 * `useOrdnerChildren` is mocked so the test runs without a real
 * QueryClient or network layer.
 */
import { fireEvent, render, screen } from "@testing-library/react-native";

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

const ROOT_FOLDER: OrdnerDTO = {
  ordnerID: "11111111-1111-1111-1111-111111111111",
  name: "Vorlesungen",
  level: 0,
  farbe: "DEFAULT",
  public: true,
  ueberOrdnerID: null,
  kursID: "kkkkkkkk-kkkk-kkkk-kkkk-kkkkkkkkkkkk",
};

const CHILD_FOLDER: OrdnerDTO = {
  ordnerID: "22222222-2222-2222-2222-222222222222",
  name: "Mathematik",
  level: 1,
  farbe: "RED",
  public: false,
  ueberOrdnerID: ROOT_FOLDER.ordnerID,
  kursID: ROOT_FOLDER.kursID,
};

describe("OrdnerTree", () => {
  beforeEach(() => {
    Object.keys(childrenByParent).forEach((k) => delete childrenByParent[k]);
  });

  it("renders the synthetic 'Alle Dateien' root and the supplied root folders", () => {
    render(
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
    render(
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
    render(
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
    render(
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
});
