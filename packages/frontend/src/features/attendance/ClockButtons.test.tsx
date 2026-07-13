import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, type Mock, vi } from "vitest";
import type { TodayStatusResponse } from "./attendance-api";
import { ClockButtons } from "./ClockButtons";
import { useClockIn, useClockOut, useTodayStatus } from "./useAttendance";

vi.mock("./useAttendance", () => ({
  useTodayStatus: vi.fn(),
  useClockIn: vi.fn(),
  useClockOut: vi.fn(),
}));

const mockClockInMutate = vi.fn();
const mockClockOutMutate = vi.fn();

function setupStatus(status: TodayStatusResponse["status"]) {
  (useTodayStatus as Mock).mockReturnValue({
    data: { status, records: [] } satisfies TodayStatusResponse,
    isLoading: false,
  });
}

function getClockInButton() {
  return screen.getByRole("button", { name: "出勤" });
}

function getClockOutButton() {
  return screen.getByRole("button", { name: "退勤" });
}

describe("ClockButtons", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (useClockIn as Mock).mockReturnValue({ mutate: mockClockInMutate, isPending: false });
    (useClockOut as Mock).mockReturnValue({ mutate: mockClockOutMutate, isPending: false });
  });

  afterEach(() => {
    cleanup();
  });

  it("未出勤のときは出勤ボタンが押せ、退勤ボタンは無効", () => {
    setupStatus("NOT_CLOCKED_IN");
    render(<ClockButtons />);

    expect(getClockInButton()).toBeEnabled();
    expect(getClockOutButton()).toBeDisabled();
  });

  it("勤務中（出勤済み）のときは出勤ボタンが無効で、退勤ボタンが押せる", () => {
    setupStatus("CLOCKED_IN");
    render(<ClockButtons />);

    // 二重打刻防止: 出勤済みなら出勤ボタンは押せない
    expect(getClockInButton()).toBeDisabled();
    expect(getClockOutButton()).toBeEnabled();
  });

  it("退勤済みのときは出勤・退勤ボタンともに無効", () => {
    setupStatus("CLOCKED_OUT");
    render(<ClockButtons />);

    expect(getClockInButton()).toBeDisabled();
    expect(getClockOutButton()).toBeDisabled();
  });

  it("メモを入力して出勤すると、入力値付きで clockIn mutate が呼ばれる", async () => {
    const user = userEvent.setup();
    setupStatus("NOT_CLOCKED_IN");
    render(<ClockButtons />);

    await user.type(screen.getByLabelText("メモ（任意）"), "電車遅延のため遅刻");
    await user.click(getClockInButton());

    expect(mockClockInMutate).toHaveBeenCalledWith("電車遅延のため遅刻", expect.anything());
  });

  it("メモ未入力で出勤すると、memo は undefined で mutate が呼ばれる（空欄OK）", async () => {
    const user = userEvent.setup();
    setupStatus("NOT_CLOCKED_IN");
    render(<ClockButtons />);

    await user.click(getClockInButton());

    expect(mockClockInMutate).toHaveBeenCalledWith(undefined, expect.anything());
  });

  it("メモを入力して退勤すると、入力値付きで clockOut mutate が呼ばれる", async () => {
    const user = userEvent.setup();
    setupStatus("CLOCKED_IN");
    render(<ClockButtons />);

    await user.type(screen.getByLabelText("メモ（任意）"), "客先直行");
    await user.click(getClockOutButton());

    expect(mockClockOutMutate).toHaveBeenCalledWith("客先直行", expect.anything());
  });
});
