"use client";

import { LeaveRequestForm } from "@/features/leave/LeaveRequestForm";

export default function NewLeavePage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">有給申請 - 新規作成</h1>
      <LeaveRequestForm />
    </div>
  );
}
