"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "@/components/Toast";
import { useAuth } from "@/features/auth/useAuth";
import {
  approveLeaveRequest,
  createLeaveRequest,
  fetchLeaveRequests,
  fetchPendingLeaveRequests,
  type LeaveRequestCreateRequest,
  type LeaveStatus,
  rejectLeaveRequest,
} from "./leave-api";

const LEAVE_KEY = ["leave-requests"] as const;
const PENDING_KEY = ["leave-requests", "pending"] as const;

export function useLeaveRequests(status?: LeaveStatus) {
  const { user } = useAuth();
  const requesterId = user?.id;

  return useQuery({
    queryKey: [...LEAVE_KEY, requesterId, status],
    queryFn: () => fetchLeaveRequests(requesterId!, status),
    enabled: !!requesterId,
  });
}

export function useCreateLeaveRequest() {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: LeaveRequestCreateRequest) => createLeaveRequest(user!.id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: LEAVE_KEY });
      toast.success("有給申請を送信しました");
    },
    onError: () => {
      toast.error("有給申請の送信に失敗しました");
    },
  });
}

export function usePendingLeaveRequests() {
  const { user } = useAuth();

  return useQuery({
    queryKey: [...PENDING_KEY, user?.id],
    queryFn: () => fetchPendingLeaveRequests(user!.id),
    enabled: !!user?.isManager,
  });
}

export function useApproveLeaveRequest() {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, version }: { id: string; version: number }) =>
      approveLeaveRequest(id, user!.id, version),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: PENDING_KEY });
      queryClient.invalidateQueries({ queryKey: LEAVE_KEY });
      toast.success("有給申請を承認しました");
    },
    onError: () => {
      toast.error("承認に失敗しました");
    },
  });
}

export function useRejectLeaveRequest() {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, reason, version }: { id: string; reason: string; version: number }) =>
      rejectLeaveRequest(id, user!.id, reason, version),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: PENDING_KEY });
      queryClient.invalidateQueries({ queryKey: LEAVE_KEY });
      toast.success("有給申請を却下しました");
    },
    onError: () => {
      toast.error("却下に失敗しました");
    },
  });
}
