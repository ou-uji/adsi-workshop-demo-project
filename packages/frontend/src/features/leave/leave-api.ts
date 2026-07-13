import { apiClient } from "@/lib/api-client";

export type LeaveStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface LeaveRequestResponse {
  id: string;
  requesterId: string;
  requesterName: string;
  approverId: string | null;
  approverName: string | null;
  targetDate: string;
  reason: string;
  status: LeaveStatus;
  rejectReason: string | null;
  version: number;
  createdAt: string;
}

export interface PendingLeaveRequestResponse {
  id: string;
  requesterId: string;
  requesterName: string;
  targetDate: string;
  reason: string;
  version: number;
  createdAt: string;
}

export interface LeaveRequestCreateRequest {
  targetDate: string;
  reason: string;
}

export function createLeaveRequest(
  requesterId: string,
  request: LeaveRequestCreateRequest,
): Promise<LeaveRequestResponse> {
  return apiClient.post<LeaveRequestResponse>(
    `/api/leave-requests?requesterId=${requesterId}`,
    request,
  );
}

export function fetchLeaveRequests(
  requesterId: string,
  status?: LeaveStatus,
): Promise<LeaveRequestResponse[]> {
  const params = new URLSearchParams({ requesterId });
  if (status) {
    params.set("status", status);
  }
  return apiClient.get<LeaveRequestResponse[]>(`/api/leave-requests?${params.toString()}`);
}

export function fetchPendingLeaveRequests(
  managerId: string,
): Promise<PendingLeaveRequestResponse[]> {
  return apiClient.get<PendingLeaveRequestResponse[]>(
    `/api/leave-requests/pending?managerId=${managerId}`,
  );
}

export function approveLeaveRequest(
  id: string,
  approverId: string,
  version: number,
): Promise<LeaveRequestResponse> {
  return apiClient.patch<LeaveRequestResponse>(
    `/api/leave-requests/${id}/approve?approverId=${approverId}&version=${version}`,
  );
}

export function rejectLeaveRequest(
  id: string,
  approverId: string,
  reason: string,
  version: number,
): Promise<LeaveRequestResponse> {
  return apiClient.patch<LeaveRequestResponse>(
    `/api/leave-requests/${id}/reject?approverId=${approverId}`,
    { reason, version },
  );
}
