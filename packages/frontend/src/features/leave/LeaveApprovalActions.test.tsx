import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, type Mock, vi } from "vitest";
import { LeaveApprovalActions } from "./LeaveApprovalActions";
import { useApproveLeaveRequest, useRejectLeaveRequest } from "./useLeaveRequests";

vi.mock("./useLeaveRequests", () => ({
  useApproveLeaveRequest: vi.fn(),
  useRejectLeaveRequest: vi.fn(),
}));

const mockApprove = vi.fn();
const mockReject = vi.fn();

describe("LeaveApprovalActions", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (useApproveLeaveRequest as Mock).mockReturnValue({
      mutate: mockApprove,
      isPending: false,
    });
    (useRejectLeaveRequest as Mock).mockReturnValue({
      mutate: mockReject,
      isPending: false,
    });
  });

  afterEach(() => {
    cleanup();
  });

  it("承認ボタンで approve mutation が id と version で呼ばれる", async () => {
    const user = userEvent.setup();
    render(<LeaveApprovalActions leaveRequestId="leave-1" version={2} />);

    await user.click(screen.getByRole("button", { name: "承認" }));

    expect(mockApprove).toHaveBeenCalledWith({ id: "leave-1", version: 2 });
  });

  it("却下ダイアログで理由を入力して却下すると reject mutation が呼ばれる", async () => {
    const user = userEvent.setup();
    render(<LeaveApprovalActions leaveRequestId="leave-1" version={2} />);

    await user.click(screen.getByRole("button", { name: "却下" }));
    await user.type(screen.getByLabelText("却下理由"), "業務都合により");
    await user.click(screen.getByRole("button", { name: "却下する" }));

    expect(mockReject).toHaveBeenCalledWith(
      { id: "leave-1", reason: "業務都合により", version: 2 },
      expect.anything(),
    );
  });

  it("却下理由が空だと却下する ボタンが無効", async () => {
    const user = userEvent.setup();
    render(<LeaveApprovalActions leaveRequestId="leave-1" version={2} />);

    await user.click(screen.getByRole("button", { name: "却下" }));

    expect(screen.getByRole("button", { name: "却下する" })).toBeDisabled();
  });
});
