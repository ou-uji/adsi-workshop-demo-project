"use client";

import { Pencil } from "lucide-react";
import { type Column, DataTable } from "@/components/DataTable";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import type { AttendanceRecordResponse, DailyAttendanceResponse } from "./attendance-api";
import { formatDate, formatMinutes, formatTime } from "./format";

function firstClockIn(day: DailyAttendanceResponse): string {
  const record = day.records[0];
  return record ? formatTime(record.clockIn) : "--:--";
}

function lastClockOut(day: DailyAttendanceResponse): string {
  const last = day.records[day.records.length - 1];
  return last?.clockOut ? formatTime(last.clockOut) : "--:--";
}

function hasCorrected(day: DailyAttendanceResponse): boolean {
  return day.records.some((r) => r.corrected);
}

function memoText(day: DailyAttendanceResponse): string {
  const parts = day.records.flatMap((r) => [r.clockInMemo, r.clockOutMemo]);
  return parts.filter((m): m is string => !!m).join(" / ");
}

function editableRecord(day: DailyAttendanceResponse): AttendanceRecordResponse | null {
  return day.records[0] ?? null;
}

interface AttendanceTableProps {
  days: DailyAttendanceResponse[];
  onEditMemo?: (record: AttendanceRecordResponse) => void;
}

function buildColumns(
  onEditMemo?: (record: AttendanceRecordResponse) => void,
): Column<DailyAttendanceResponse>[] {
  return [
    { key: "date", header: "日付", render: (day) => formatDate(day.date) },
    { key: "clockIn", header: "出勤", render: (day) => firstClockIn(day) },
    { key: "clockOut", header: "退勤", render: (day) => lastClockOut(day) },
    {
      key: "workMinutes",
      header: "勤務時間",
      render: (day) => (day.workMinutes > 0 ? formatMinutes(day.workMinutes) : "-"),
    },
    {
      key: "breakMinutes",
      header: "休憩",
      render: (day) => (day.breakMinutes > 0 ? formatMinutes(day.breakMinutes) : "-"),
    },
    {
      key: "overtimeMinutes",
      header: "残業",
      render: (day) => (day.overtimeMinutes > 0 ? formatMinutes(day.overtimeMinutes) : "-"),
    },
    {
      key: "memo",
      header: "備考",
      render: (day) => {
        const text = memoText(day);
        return text ? (
          <span className="text-sm text-muted-foreground">{text}</span>
        ) : (
          <span className="text-sm text-muted-foreground">-</span>
        );
      },
    },
    {
      key: "corrected",
      header: "",
      render: (day) => (hasCorrected(day) ? <Badge variant="outline">修正</Badge> : null),
    },
    {
      key: "actions",
      header: "",
      render: (day) => {
        const record = editableRecord(day);
        if (!onEditMemo || !record) return null;
        return (
          <Button
            type="button"
            variant="ghost"
            size="icon"
            aria-label="メモを編集"
            onClick={() => onEditMemo(record)}
          >
            <Pencil className="h-4 w-4" />
          </Button>
        );
      },
    },
  ];
}

export function AttendanceTable({ days, onEditMemo }: AttendanceTableProps) {
  const columns = buildColumns(onEditMemo);
  return (
    <DataTable<DailyAttendanceResponse & Record<string, unknown>>
      columns={columns as Column<DailyAttendanceResponse & Record<string, unknown>>[]}
      data={days as (DailyAttendanceResponse & Record<string, unknown>)[]}
      rowKey={(item) => item.date}
      emptyMessage="勤怠データがありません"
    />
  );
}
