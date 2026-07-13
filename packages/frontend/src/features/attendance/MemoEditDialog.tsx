"use client";

import { useEffect, useState } from "react";
import { FormDialog } from "@/components/FormDialog";
import type { AttendanceRecordResponse } from "./attendance-api";
import { useUpdateMemo } from "./useAttendance";

const MEMO_MAX_LENGTH = 200;
const MEMO_CLASS =
  "w-full min-w-0 rounded-lg border border-input bg-transparent px-2.5 py-2 text-base transition-colors outline-none placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50 md:text-sm dark:bg-input/30";

interface MemoEditDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  record: AttendanceRecordResponse | null;
}

export function MemoEditDialog({ open, onOpenChange, record }: MemoEditDialogProps) {
  const updateMemoMutation = useUpdateMemo();
  const [clockInMemo, setClockInMemo] = useState("");
  const [clockOutMemo, setClockOutMemo] = useState("");

  useEffect(() => {
    setClockInMemo(record?.clockInMemo ?? "");
    setClockOutMemo(record?.clockOutMemo ?? "");
  }, [record]);

  const handleSubmit = () => {
    if (!record) return;
    updateMemoMutation.mutate(
      {
        recordId: record.id,
        request: {
          clockInMemo: clockInMemo.trim() === "" ? null : clockInMemo,
          clockOutMemo: clockOutMemo.trim() === "" ? null : clockOutMemo,
          version: record.version,
        },
      },
      { onSuccess: () => onOpenChange(false) },
    );
  };

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title="メモを編集"
      description="打刻メモ（備考）を編集できます。空にすると削除されます。"
      onSubmit={handleSubmit}
      isSubmitting={updateMemoMutation.isPending}
    >
      <div className="space-y-2">
        <label htmlFor="edit-clock-in-memo" className="text-sm font-medium">
          出勤メモ
        </label>
        <textarea
          id="edit-clock-in-memo"
          value={clockInMemo}
          onChange={(e) => setClockInMemo(e.target.value)}
          maxLength={MEMO_MAX_LENGTH}
          rows={2}
          className={MEMO_CLASS}
        />
      </div>
      <div className="space-y-2">
        <label htmlFor="edit-clock-out-memo" className="text-sm font-medium">
          退勤メモ
        </label>
        <textarea
          id="edit-clock-out-memo"
          value={clockOutMemo}
          onChange={(e) => setClockOutMemo(e.target.value)}
          maxLength={MEMO_MAX_LENGTH}
          rows={2}
          className={MEMO_CLASS}
        />
      </div>
    </FormDialog>
  );
}
