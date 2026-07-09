"use client";

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useDepartments } from "@/features/department/useDepartments";

interface DepartmentFilterProps {
  value: string;
  onChange: (departmentId: string) => void;
}

export function DepartmentFilter({ value, onChange }: DepartmentFilterProps) {
  const { data: departments = [] } = useDepartments();

  const handleChange = (newValue: string | null) => {
    onChange(newValue ?? "all");
  };

  const items = Object.fromEntries([
    ["all", "全部署"],
    ...departments.map((d) => [d.id, d.name]),
  ]);

  return (
    <Select value={value} onValueChange={handleChange} items={items}>
      <SelectTrigger>
        <SelectValue placeholder="全部署" />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value="all">全部署</SelectItem>
        {departments.map((dept) => (
          <SelectItem key={dept.id} value={dept.id}>
            {dept.name}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
