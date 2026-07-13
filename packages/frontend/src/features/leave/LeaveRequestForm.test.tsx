import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, type Mock, vi } from "vitest";
import { LeaveRequestForm } from "./LeaveRequestForm";
import { useCreateLeaveRequest } from "./useLeaveRequests";

vi.mock("./useLeaveRequests", () => ({
  useCreateLeaveRequest: vi.fn(),
}));

const mockPush = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush }),
}));

const mockMutate = vi.fn();

describe("LeaveRequestForm", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (useCreateLeaveRequest as Mock).mockReturnValue({
      mutate: mockMutate,
      isPending: false,
    });
  });

  afterEach(() => {
    cleanup();
  });

  it("必須項目が未入力だと申請ボタンが無効", () => {
    render(<LeaveRequestForm />);

    expect(screen.getByRole("button", { name: "申請する" })).toBeDisabled();
  });

  it("取得日と理由を入力すると申請ボタンが有効になる", async () => {
    const user = userEvent.setup();
    render(<LeaveRequestForm />);

    await user.type(screen.getByLabelText("取得日"), "2025-10-20");
    await user.type(screen.getByLabelText("申請理由"), "私用のため");

    expect(screen.getByRole("button", { name: "申請する" })).toBeEnabled();
  });

  it("送信すると createLeaveRequest が targetDate と reason で呼ばれる", async () => {
    const user = userEvent.setup();
    render(<LeaveRequestForm />);

    await user.type(screen.getByLabelText("取得日"), "2025-10-20");
    await user.type(screen.getByLabelText("申請理由"), "私用のため");
    await user.click(screen.getByRole("button", { name: "申請する" }));

    expect(mockMutate).toHaveBeenCalledWith(
      { targetDate: "2025-10-20", reason: "私用のため" },
      expect.anything(),
    );
  });

  it("理由の textarea は maxLength が 500", () => {
    render(<LeaveRequestForm />);

    expect(screen.getByLabelText("申請理由")).toHaveAttribute("maxLength", "500");
  });
});
