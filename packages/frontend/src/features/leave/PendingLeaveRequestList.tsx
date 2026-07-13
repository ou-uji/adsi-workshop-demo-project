"use client";

import { type Column, DataTable } from "@/components/DataTable";
import { Skeleton } from "@/components/ui/skeleton";
import { formatDate } from "@/features/attendance/format";
import { LeaveApprovalActions } from "./LeaveApprovalActions";
import type { PendingLeaveRequestResponse } from "./leave-api";
import { usePendingLeaveRequests } from "./useLeaveRequests";

const columns: Column<PendingLeaveRequestResponse>[] = [
  {
    key: "requesterName",
    header: "申請者",
  },
  {
    key: "targetDate",
    header: "取得日",
    render: (item) => formatDate(item.targetDate),
  },
  {
    key: "reason",
    header: "理由",
    render: (item) => <span className="max-w-[200px] truncate block">{item.reason}</span>,
  },
  {
    key: "actions",
    header: "",
    render: (item) => <LeaveApprovalActions leaveRequestId={item.id} version={item.version} />,
  },
];

export function PendingLeaveRequestList() {
  const { data, isLoading } = usePendingLeaveRequests();

  return (
    <div className="space-y-4">
      {isLoading ? (
        <div className="space-y-2">
          {["r1", "r2", "r3", "r4", "r5"].map((id) => (
            <Skeleton key={id} className="h-10 w-full" />
          ))}
        </div>
      ) : (
        <DataTable<PendingLeaveRequestResponse & Record<string, unknown>>
          columns={columns as Column<PendingLeaveRequestResponse & Record<string, unknown>>[]}
          data={(data ?? []) as (PendingLeaveRequestResponse & Record<string, unknown>)[]}
          rowKey={(item) => item.id}
          emptyMessage="承認待ちの有給申請はありません"
        />
      )}
    </div>
  );
}
