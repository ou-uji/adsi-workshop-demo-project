"use client";

import { useState } from "react";
import Link from "next/link";
import { type Column, DataTable } from "@/components/DataTable";
import { StatusBadge } from "@/components/StatusBadge";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { formatDate, formatTime } from "@/features/attendance/format";
import type { CorrectionResponse, CorrectionStatus } from "./correction-api";
import { useCorrections } from "./useCorrections";

const STATUS_CONFIG_MAP: Record<string, { label: string; variant: "default" | "secondary" | "destructive" | "outline" }> = {
  PENDING: { label: "申請中", variant: "secondary" },
  APPROVED: { label: "承認済", variant: "default" },
  REJECTED: { label: "却下", variant: "destructive" },
};

const STATUS_OPTIONS: { value: string; label: string }[] = [
  { value: "ALL", label: "すべて" },
  { value: "PENDING", label: "申請中" },
  { value: "APPROVED", label: "承認済" },
  { value: "REJECTED", label: "却下" },
];

const columns: Column<CorrectionResponse>[] = [
  {
    key: "targetDate",
    header: "対象日",
    render: (item) => formatDate(item.targetDate),
  },
  {
    key: "correctedClockIn",
    header: "修正出勤",
    render: (item) => formatTime(item.correctedClockIn),
  },
  {
    key: "correctedClockOut",
    header: "修正退勤",
    render: (item) => formatTime(item.correctedClockOut),
  },
  {
    key: "reason",
    header: "理由",
    render: (item) => (
      <span className="max-w-[200px] truncate block">{item.reason}</span>
    ),
  },
  {
    key: "status",
    header: "ステータス",
    render: (item) => (
      <StatusBadge status={item.status} configMap={STATUS_CONFIG_MAP} />
    ),
  },
  {
    key: "rejectReason",
    header: "却下理由",
    render: (item) =>
      item.rejectReason ? (
        <span className="max-w-[200px] truncate block text-destructive">
          {item.rejectReason}
        </span>
      ) : (
        "-"
      ),
  },
];

export function CorrectionList() {
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const status = statusFilter === "ALL" ? undefined : (statusFilter as CorrectionStatus);
  const { data, isLoading } = useCorrections(status);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <Select
          value={statusFilter}
          onValueChange={(v) => setStatusFilter(v ?? "ALL")}
          items={Object.fromEntries(STATUS_OPTIONS.map((o) => [o.value, o.label]))}
        >
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {STATUS_OPTIONS.map((opt) => (
              <SelectItem key={opt.value} value={opt.value}>
                {opt.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Button render={<Link href="/corrections/new" />}>新規申請</Button>
      </div>
      {isLoading ? (
        <div className="space-y-2">
          {["r1", "r2", "r3", "r4", "r5"].map((id) => (
            <Skeleton key={id} className="h-10 w-full" />
          ))}
        </div>
      ) : (
        <DataTable<CorrectionResponse & Record<string, unknown>>
          columns={columns as Column<CorrectionResponse & Record<string, unknown>>[]}
          data={(data ?? []) as (CorrectionResponse & Record<string, unknown>)[]}
          rowKey={(item) => item.id}
          emptyMessage="修正申請はありません"
        />
      )}
    </div>
  );
}
