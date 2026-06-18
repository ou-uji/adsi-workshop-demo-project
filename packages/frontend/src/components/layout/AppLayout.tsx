"use client";

import { SidebarProvider } from "@/components/ui/sidebar";
import { Header } from "./Header";
import { AppSidebar } from "./Sidebar";

interface AppLayoutProps {
  children: React.ReactNode;
}

export function AppLayout({ children }: AppLayoutProps) {
  return (
    <SidebarProvider>
      <AppSidebar />
      <div className="flex flex-1 flex-col">
        <Header />
        <main className="flex-1 p-6">{children}</main>
      </div>
    </SidebarProvider>
  );
}
