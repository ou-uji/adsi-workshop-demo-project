"use client";

import { useRouter } from "next/navigation";
import { type FormEvent, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useCreateLeaveRequest } from "./useLeaveRequests";

export function LeaveRequestForm() {
  const router = useRouter();
  const createMutation = useCreateLeaveRequest();

  const [targetDate, setTargetDate] = useState("");
  const [reason, setReason] = useState("");

  const isValid = targetDate && reason.trim().length > 0;

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!isValid) return;

    createMutation.mutate(
      {
        targetDate,
        reason: reason.trim(),
      },
      {
        onSuccess: () => {
          router.push("/leave");
        },
      },
    );
  }

  return (
    <form onSubmit={handleSubmit} className="max-w-md space-y-6">
      <div className="space-y-2">
        <Label htmlFor="targetDate">取得日</Label>
        <Input
          id="targetDate"
          type="date"
          value={targetDate}
          onChange={(e) => setTargetDate(e.target.value)}
          required
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="reason">申請理由</Label>
        <textarea
          id="reason"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          required
          maxLength={500}
          rows={3}
          className="w-full min-w-0 rounded-lg border border-input bg-transparent px-2.5 py-2 text-base transition-colors outline-none placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50 md:text-sm dark:bg-input/30"
          placeholder="私用、通院等の理由を入力してください"
        />
      </div>

      <div className="flex gap-2">
        <Button type="submit" disabled={!isValid || createMutation.isPending}>
          {createMutation.isPending ? "送信中..." : "申請する"}
        </Button>
        <Button type="button" variant="outline" onClick={() => router.push("/leave")}>
          キャンセル
        </Button>
      </div>
    </form>
  );
}
