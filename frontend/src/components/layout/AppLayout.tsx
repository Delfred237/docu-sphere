import { useState } from "react";
import { Outlet, Link, useLocation } from "react-router-dom";
import {
  LayoutDashboard,
  FileText,
  FolderTree,
  Share2,
  Bell,
  Settings,
  LogOut,
  User,
  Menu,
  X,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useAuthStore } from "@/features/auth/stores/authStore";
import { Logo } from "@/components/shared/Logo";

const navigation = [
  { name: "Dashboard", href: "/", icon: LayoutDashboard },
  { name: "Documents", href: "/documents", icon: FileText },
  { name: "Folders", href: "/folders", icon: FolderTree },
  { name: "Shared", href: "/shared", icon: Share2 },
];

export function AppLayout() {
  const location = useLocation();
  const { user, logout } = useAuthStore();
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  const currentPageTitle =
    navigation.find((item) => item.href === location.pathname)?.name ||
    "DocuSphere";

  const handleNavigationClick = () => {
    setIsSidebarOpen(false);
  };

  return (
    <div className="flex h-screen bg-slate-50 overflow-hidden">
      {/* Mobile overlay */}
      {isSidebarOpen && (
        <div
          role="presentation"
          className="fixed inset-0 bg-black/50 z-40 lg:hidden"
          onClick={() => setIsSidebarOpen(false)}
        />
      )}

      {/* Sidebar - Desktop */}
      <aside className="hidden lg:flex lg:w-60 bg-white border-r border-slate-200 flex-col shrink-0">
        <SidebarContent onNavigate={handleNavigationClick} />
      </aside>

      {/* Sidebar - Mobile Drawer */}
      <aside
        className={`fixed inset-y-0 left-0 z-50 w-64 bg-white border-r border-slate-200 flex flex-col transform transition-transform duration-300 ease-in-out lg:hidden ${
          isSidebarOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        {/* Close button for mobile */}
        <div className="h-16 flex items-center justify-between px-4 border-b border-slate-200">
          <Logo size="sm" />
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setIsSidebarOpen(false)}
          >
            <X className="h-5 w-5 text-slate-600" />
            <span className="sr-only">Close menu</span>
          </Button>
        </div>
        <SidebarContent onNavigate={handleNavigationClick} showLogo={false} />
      </aside>

      {/* Main content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Header */}
        <header className="h-16 bg-white border-b border-slate-200 flex items-center justify-between px-4 md:px-6 shrink-0">
          {/* Left side: hamburger + title */}
          <div className="flex items-center gap-3">
            {/* Hamburger button - mobile only */}
            <Button
              variant="ghost"
              size="icon"
              className="lg:hidden"
              onClick={() => setIsSidebarOpen(true)}
            >
              <Menu className="h-5 w-5 text-slate-600" />
              <span className="sr-only">Open menu</span>
            </Button>

            {/* Page title */}
            <h2 className="text-lg font-semibold text-slate-900 truncate">
              {currentPageTitle}
            </h2>
          </div>

          {/* Right side: notifications + user menu */}
          <div className="flex items-center gap-1 md:gap-2">
            {/* Notifications */}
            <Button variant="ghost" size="icon" className="relative">
              <Bell className="h-5 w-5 text-slate-600" />
              <span className="absolute top-2 right-2 h-2 w-2 bg-red-500 rounded-full" />
              <span className="sr-only">Notifications</span>
            </Button>

            {/* User menu */}
            <DropdownMenu>
              <DropdownMenuTrigger
                render={
                  <Button
                    variant="ghost"
                    className="relative h-9 w-9 rounded-full"
                  >
                    <Avatar className="h-9 w-9">
                      <AvatarFallback className="bg-primary-100 text-primary-700 text-sm font-medium">
                        {user?.firstName?.[0] || ""}
                        {user?.lastName?.[0] || ""}
                      </AvatarFallback>
                    </Avatar>
                  </Button>
                }
              />
              <DropdownMenuContent className="w-56" align="end" sideOffset={8}>
                <DropdownMenuGroup>
                  <DropdownMenuLabel>
                    <div className="flex flex-col space-y-1">
                      <p className="text-sm font-medium text-slate-900">
                        {user?.firstName} {user?.lastName}
                      </p>
                      <p className="text-xs text-slate-500 truncate">
                        {user?.email}
                      </p>
                    </div>
                  </DropdownMenuLabel>
                </DropdownMenuGroup>
                <DropdownMenuSeparator />
                <DropdownMenuItem>
                  <User className="mr-2 h-4 w-4" />
                  <span>Profile</span>
                </DropdownMenuItem>
                <DropdownMenuItem>
                  <Settings className="mr-2 h-4 w-4" />
                  <span>Settings</span>
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem
                  onClick={logout}
                  className="text-red-600 focus:text-red-600 focus:bg-red-50"
                >
                  <LogOut className="mr-2 h-4 w-4" />
                  <span>Logout</span>
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

// Composant réutilisable pour le contenu de la sidebar
interface SidebarContentProps {
  onNavigate: () => void;
  showLogo?: boolean;
}

function SidebarContent({
  onNavigate,
  showLogo = true,
}: Readonly<SidebarContentProps>) {
  const location = useLocation();

  return (
    <>
      {/* Logo (desktop uniquement, mobile a déjà le logo dans le header du drawer) */}
      {showLogo && (
        <div className="h-16 flex items-center px-6 border-b border-slate-200">
          <Logo size="sm" />
        </div>
      )}

      {/* Navigation */}
      <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
        {navigation.map((item) => {
          const Icon = item.icon;
          const isActive = location.pathname === item.href;

          return (
            <Link
              key={item.name}
              to={item.href}
              onClick={onNavigate}
              className={`flex items-center gap-3 px-3 py-2.5 rounded-md text-sm font-medium transition-colors ${
                isActive
                  ? "bg-primary-50 text-primary-700"
                  : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
              }`}
            >
              <Icon className="h-5 w-5 shrink-0" />
              <span>{item.name}</span>
            </Link>
          );
        })}
      </nav>

      {/* Settings */}
      <div className="px-3 py-4 border-t border-slate-200">
        <Link
          to="/settings"
          onClick={onNavigate}
          className="flex items-center gap-3 px-3 py-2.5 rounded-md text-sm font-medium text-slate-600 hover:bg-slate-50 hover:text-slate-900 transition-colors"
        >
          <Settings className="h-5 w-5 shrink-0" />
          <span>Settings</span>
        </Link>
      </div>
    </>
  );
}
