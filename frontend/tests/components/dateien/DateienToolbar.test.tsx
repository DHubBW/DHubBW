/**
 * Unit tests for the DateienToolbar search-pill behaviour.
 *
 * The toolbar always shows the breadcrumb; when an `onSearchTermChange`
 * callback is wired, the magnifying-glass pill becomes interactive:
 * tapping it expands the input, typing forwards changes, and tapping
 * the close-X collapses the pill while clearing the search term so no
 * stale filter survives invisibly.
 */
import { fireEvent, render, screen } from "@testing-library/react-native";

import { DateienToolbar } from "@/components/dateien/DateienToolbar";

describe("DateienToolbar – search pill", () => {
  it("renders the breadcrumb root label when no folder is selected", () => {
    render(<DateienToolbar selected={null} />);
    expect(screen.getByText("Alle Dateien")).toBeTruthy();
  });

  it("renders the breadcrumb folder name when a folder is selected", () => {
    render(
      <DateienToolbar
        selected={{
          ordnerID: "11111111-1111-1111-1111-111111111111",
          name: "Mathematik",
          level: 0,
          farbe: "DEFAULT",
          public: false,
          ueberOrdnerID: null,
          kursID: "kkkkkkkk-kkkk-kkkk-kkkk-kkkkkkkkkkkk",
          hasChildren: false,
        }}
      />,
    );
    expect(screen.getByText("Mathematik")).toBeTruthy();
  });

  it("does not render the search toggle when onSearchTermChange is not wired", () => {
    render(<DateienToolbar selected={null} />);
    expect(screen.queryByTestId("dateien-search-toggle")).toBeNull();
  });

  it("renders only the toggle when search is wired but closed", () => {
    render(
      <DateienToolbar
        selected={null}
        searchTerm=""
        onSearchTermChange={jest.fn()}
      />,
    );
    expect(screen.getByTestId("dateien-search-toggle")).toBeTruthy();
    expect(screen.queryByTestId("dateien-search-input")).toBeNull();
  });

  it("opens the input when the lupe is pressed and forwards typed text via the callback", () => {
    const onSearchTermChange = jest.fn();
    const { rerender } = render(
      <DateienToolbar
        selected={null}
        searchTerm=""
        onSearchTermChange={onSearchTermChange}
      />,
    );
    fireEvent.press(screen.getByTestId("dateien-search-toggle"));

    const input = screen.getByTestId("dateien-search-input");
    fireEvent.changeText(input, "skript");
    expect(onSearchTermChange).toHaveBeenCalledWith("skript");

    // Simulate the parent driving the new value back in.
    rerender(
      <DateienToolbar
        selected={null}
        searchTerm="skript"
        onSearchTermChange={onSearchTermChange}
      />,
    );
    expect(screen.getByTestId("dateien-search-input").props.value).toBe(
      "skript",
    );
  });

  it("clears the term and collapses the pill when the close button is pressed", () => {
    const onSearchTermChange = jest.fn();
    render(
      <DateienToolbar
        selected={null}
        searchTerm="skript"
        onSearchTermChange={onSearchTermChange}
      />,
    );
    // Open the pill so the close button is visible.
    fireEvent.press(screen.getByTestId("dateien-search-toggle"));
    fireEvent.press(screen.getByTestId("dateien-search-close"));

    // Closing must clear the active filter so the table snaps back to
    // its unfiltered state instead of leaving a stale search hidden.
    expect(onSearchTermChange).toHaveBeenLastCalledWith("");
  });

  it("renders an inline clear-X next to the input when there is text and clears only the term on press", () => {
    const onSearchTermChange = jest.fn();
    render(
      <DateienToolbar
        selected={null}
        searchTerm="abc"
        onSearchTermChange={onSearchTermChange}
      />,
    );
    fireEvent.press(screen.getByTestId("dateien-search-toggle"));
    fireEvent.press(screen.getByTestId("dateien-search-clear"));
    expect(onSearchTermChange).toHaveBeenLastCalledWith("");
  });
});
