"use client";

import { type FormEvent, useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { useApproveLeaveRequest, useRejectLeaveRequest } from "./useLeaveRequests";

interface LeaveApprovalActionsProps {
  leaveRequestId: string;
  version: number;
}

export function LeaveApprovalActions({ leaveRequestId, version }: LeaveApprovalActionsProps) {
  const approveMutation = useApproveLeaveRequest();
  const rejectMutation = useRejectLeaveRequest();
  const [rejectReason, setRejectReason] = useState("");
  const [open, setOpen] = useState(false);

  const isPending = approveMutation.isPending || rejectMutation.isPending;

  function handleApprove() {
    approveMutation.mutate({ id: leaveRequestId, version });
  }

  function handleReject(e: FormEvent) {
    e.preventDefault();
    if (!rejectReason.trim()) return;

    rejectMutation.mutate(
      { id: leaveRequestId, reason: rejectReason.trim(), version },
      {
        onSuccess: () => {
          setOpen(false);
          setRejectReason("");
        },
      },
    );
  }

  return (
    <div className="flex gap-2">
      <Button size="sm" onClick={handleApprove} disabled={isPending}>
        {approveMutation.isPending ? "承認中..." : "承認"}
      </Button>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogTrigger render={<Button size="sm" variant="destructive" disabled={isPending} />}>
          却下
        </DialogTrigger>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>有給申請を却下</DialogTitle>
            <DialogDescription>却下理由を入力してください。</DialogDescription>
          </DialogHeader>
          <form onSubmit={handleReject}>
            <div className="space-y-2 py-2">
              <Label htmlFor="rejectReason">却下理由</Label>
              <textarea
                id="rejectReason"
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                required
                maxLength={500}
                rows={3}
                className="w-full min-w-0 rounded-lg border border-input bg-transparent px-2.5 py-2 text-base transition-colors outline-none placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 md:text-sm dark:bg-input/30"
                placeholder="却下理由を入力してください"
              />
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setOpen(false)}>
                キャンセル
              </Button>
              <Button
                type="submit"
                variant="destructive"
                disabled={!rejectReason.trim() || rejectMutation.isPending}
              >
                {rejectMutation.isPending ? "処理中..." : "却下する"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
