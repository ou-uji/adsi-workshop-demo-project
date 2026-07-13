"use client";

import { LeaveRequestList } from "@/features/leave/LeaveRequestList";

export default function LeavePage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">有給申請</h1>
      <LeaveRequestList />
    </div>
  );
}
