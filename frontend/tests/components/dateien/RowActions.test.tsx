/**
 * Tests for {@link RowActions}.
 *
 * The component renders three optional action buttons:
 * Vorschau, Download, and a 3-dot menu trigger ("More"). All three
 * are conditional on their callback being wired.
 */
import { fireEvent, render, screen } from "@testing-library/react-native";
import { View } from "react-native";

jest.mock("lucide-react-native", () => {
  const { View: V } = require("react-native"); // eslint-disable-line @typescript-eslint/no-require-imports
  const stub = (props: object) => <V {...props} />;
  return new Proxy({}, { get: () => stub });
});

import { RowActions } from "@/components/dateien/RowActions";

describe("RowActions", () => {
  it("invokes onDownload when the download icon is pressed", () => {
    const onDownload = jest.fn();
    render(
      <RowActions
        onPreview={jest.fn()}
        onDownload={onDownload}
        onMore={jest.fn()}
      />,
    );
    fireEvent.press(screen.getByTestId("row-action-download"));
    expect(onDownload).toHaveBeenCalledTimes(1);
  });

  it("invokes onPreview when the preview icon is pressed", () => {
    const onPreview = jest.fn();
    render(
      <RowActions
        onPreview={onPreview}
        onDownload={jest.fn()}
        onMore={jest.fn()}
      />,
    );
    fireEvent.press(screen.getByTestId("row-action-preview"));
    expect(onPreview).toHaveBeenCalledTimes(1);
  });

  it("invokes onMore when the 3-dot icon is pressed", () => {
    const onMore = jest.fn();
    render(
      <RowActions
        onPreview={jest.fn()}
        onDownload={jest.fn()}
        onMore={onMore}
      />,
    );
    fireEvent.press(screen.getByTestId("row-action-more"));
    expect(onMore).toHaveBeenCalledTimes(1);
  });

  it("hides icons whose callback is not wired", () => {
    render(<RowActions onDownload={jest.fn()} />);
    expect(screen.queryByTestId("row-action-preview")).toBeNull();
    expect(screen.getByTestId("row-action-download")).toBeTruthy();
    expect(screen.queryByTestId("row-action-more")).toBeNull();
  });

  it("blocks presses while disabled", () => {
    const onPreview = jest.fn();
    const onDownload = jest.fn();
    const onMore = jest.fn();
    render(
      <RowActions
        onPreview={onPreview}
        onDownload={onDownload}
        onMore={onMore}
        disabled
      />,
    );
    fireEvent.press(screen.getByTestId("row-action-preview"));
    fireEvent.press(screen.getByTestId("row-action-download"));
    fireEvent.press(screen.getByTestId("row-action-more"));
    expect(onPreview).not.toHaveBeenCalled();
    expect(onDownload).not.toHaveBeenCalled();
    expect(onMore).not.toHaveBeenCalled();
  });
});

// Silence the unused-import warning — `View` is imported above for
// the JSX scope of the lucide mock factory.
void View;
