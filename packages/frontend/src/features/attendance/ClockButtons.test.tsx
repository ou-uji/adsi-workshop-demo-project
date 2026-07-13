import { cleanup, render, screen } from "@testing-library/react";
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
});
