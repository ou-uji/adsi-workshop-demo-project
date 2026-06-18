"use client";

import { Badge } from "@/components/ui/badge";

type StatusVariant = "default" | "secondary" | "destructive" | "outline";

interface StatusConfig {
  label: string;
  variant: StatusVariant;
}

interface StatusBadgeProps {
  status: string;
  configMap: Record<string, StatusConfig>;
}

export function StatusBadge({ status, configMap }: StatusBadgeProps) {
  const config = configMap[status] ?? { label: status, variant: "outline" as const };
  return <Badge variant={config.variant}>{config.label}</Badge>;
}
