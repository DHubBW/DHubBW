/**
 * Tests for {@link RowActions}.
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
        onDownload={onDownload}
        onRename={jest.fn()}
        onDelete={jest.fn()}
      />,
    );
    fireEvent.press(screen.getByTestId("row-action-download"));
    expect(onDownload).toHaveBeenCalledTimes(1);
  });

  it("invokes onRename when the rename icon is pressed", () => {
    const onRename = jest.fn();
    render(
      <RowActions
        onDownload={jest.fn()}
        onRename={onRename}
        onDelete={jest.fn()}
      />,
    );
    fireEvent.press(screen.getByTestId("row-action-rename"));
    expect(onRename).toHaveBeenCalledTimes(1);
  });

  it("invokes onDelete when the delete icon is pressed", () => {
    const onDelete = jest.fn();
    render(
      <RowActions
        onDownload={jest.fn()}
        onRename={jest.fn()}
        onDelete={onDelete}
      />,
    );
    fireEvent.press(screen.getByTestId("row-action-delete"));
    expect(onDelete).toHaveBeenCalledTimes(1);
  });

  it("blocks presses while disabled", () => {
    const onDownload = jest.fn();
    const onRename = jest.fn();
    const onDelete = jest.fn();
    render(
      <RowActions
        onDownload={onDownload}
        onRename={onRename}
        onDelete={onDelete}
        disabled
      />,
    );
    fireEvent.press(screen.getByTestId("row-action-download"));
    fireEvent.press(screen.getByTestId("row-action-rename"));
    fireEvent.press(screen.getByTestId("row-action-delete"));
    expect(onDownload).not.toHaveBeenCalled();
    expect(onRename).not.toHaveBeenCalled();
    expect(onDelete).not.toHaveBeenCalled();
  });
});
