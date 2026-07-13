import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createElement } from "react";
import { afterEach, beforeEach, describe, expect, it, type Mock, vi } from "vitest";
import type { AttendanceRecordResponse } from "./attendance-api";
import { MemoEditDialog } from "./MemoEditDialog";
import { useUpdateMemo } from "./useAttendance";

vi.mock("./useAttendance", () => ({
  useUpdateMemo: vi.fn(),
}));

// FormDialog（base-ui の Dialog/Portal 依存）を軽量スタブに差し替え、
// children と「保存」ボタン（onSubmit）だけを描画する。
vi.mock("@/components/FormDialog", () => ({
  FormDialog: ({ children, onSubmit }: { children: React.ReactNode; onSubmit: () => void }) =>
    createElement(
      "div",
      null,
      children,
      createElement("button", { type: "button", onClick: onSubmit }, "保存"),
    ),
}));

const mockMutate = vi.fn();

function record(overrides: Partial<AttendanceRecordResponse> = {}): AttendanceRecordResponse {
  return {
    id: "rec-1",
    workDate: "2025-01-15",
    clockIn: "2025-01-15T00:00:00Z",
    clockOut: "2025-01-15T09:00:00Z",
    clockInMemo: "既存の出勤メモ",
    clockOutMemo: "既存の退勤メモ",
    corrected: false,
    version: 3,
    ...overrides,
  };
}

describe("MemoEditDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (useUpdateMemo as Mock).mockReturnValue({ mutate: mockMutate, isPending: false });
  });

  afterEach(() => {
    cleanup();
  });

  it("既存メモが textarea に初期表示される", () => {
    render(<MemoEditDialog open onOpenChange={vi.fn()} record={record()} />);

    expect(screen.getByLabelText("出勤メモ")).toHaveValue("既存の出勤メモ");
    expect(screen.getByLabelText("退勤メモ")).toHaveValue("既存の退勤メモ");
  });

  it("編集して保存すると updateMemo が version 付き payload で呼ばれる", async () => {
    const user = userEvent.setup();
    render(<MemoEditDialog open onOpenChange={vi.fn()} record={record()} />);

    const input = screen.getByLabelText("出勤メモ");
    await user.clear(input);
    await user.type(input, "修正後メモ");
    await user.click(screen.getByRole("button", { name: "保存" }));

    expect(mockMutate).toHaveBeenCalledWith(
      {
        recordId: "rec-1",
        request: { clockInMemo: "修正後メモ", clockOutMemo: "既存の退勤メモ", version: 3 },
      },
      expect.anything(),
    );
  });

  it("空にして保存すると null（削除）で呼ばれる", async () => {
    const user = userEvent.setup();
    render(<MemoEditDialog open onOpenChange={vi.fn()} record={record()} />);

    await user.clear(screen.getByLabelText("出勤メモ"));
    await user.clear(screen.getByLabelText("退勤メモ"));
    await user.click(screen.getByRole("button", { name: "保存" }));

    expect(mockMutate).toHaveBeenCalledWith(
      {
        recordId: "rec-1",
        request: { clockInMemo: null, clockOutMemo: null, version: 3 },
      },
      expect.anything(),
    );
  });

  it("record が null のときは保存しても mutate が呼ばれない", async () => {
    const user = userEvent.setup();
    render(<MemoEditDialog open onOpenChange={vi.fn()} record={null} />);

    await user.click(screen.getByRole("button", { name: "保存" }));

    expect(mockMutate).not.toHaveBeenCalled();
  });
});
