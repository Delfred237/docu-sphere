import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Bell,
  CheckCheck,
  FileText,
  Share2,
  CheckCircle,
  XCircle,
  Info,
  Loader2,
  Trash2,
} from "lucide-react";
import { formatDistanceToNow, parseISO } from "date-fns";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  notificationService,
  type Notification,
  type NotificationType,
} from "../services/notification.service";

const typeConfig: Record<
  NotificationType,
  { icon: React.ElementType; color: string; bg: string }
> = {
  DOCUMENT_SUBMITTED: {
    icon: FileText,
    color: "text-amber-600",
    bg: "bg-amber-50",
  },
  DOCUMENT_APPROVED: {
    icon: CheckCircle,
    color: "text-green-600",
    bg: "bg-green-50",
  },
  DOCUMENT_REJECTED: { icon: XCircle, color: "text-red-600", bg: "bg-red-50" },
  DOCUMENT_SHARED: {
    icon: Share2,
    color: "text-primary-600",
    bg: "bg-primary-50",
  },
  SYSTEM: { icon: Info, color: "text-slate-600", bg: "bg-slate-50" },
};

export function NotificationsDropdown() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);

  // Polling du compteur toutes les 30 secondes
  const { data: unreadCount = 0 } = useQuery({
    queryKey: ["notifications", "unread-count"],
    queryFn: notificationService.getUnreadCount,
    refetchInterval: 30000,
  });

  const { data: notificationsPage, isLoading } = useQuery({
    queryKey: ["notifications"],
    queryFn: () => notificationService.getNotifications(false, 0, 10),
    enabled: open,
  });

  const markAsReadMutation = useMutation({
    mutationFn: (publicId: string) => notificationService.markAsRead(publicId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["notifications"] });
      await queryClient.invalidateQueries({
        queryKey: ["notifications", "unread-count"],
      });
    },
  });

  const markAllAsReadMutation = useMutation({
    mutationFn: () => notificationService.markAllAsRead(),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["notifications"] });
      await queryClient.invalidateQueries({
        queryKey: ["notifications", "unread-count"],
      });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (publicId: string) =>
      notificationService.deleteNotification(publicId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["notifications"] });
      await queryClient.invalidateQueries({
        queryKey: ["notifications", "unread-count"],
      });
    },
  });

  const notifications = notificationsPage?.content || [];

  const handleNotificationClick = async (notification: Notification) => {
    if (!notification.read) {
      markAsReadMutation.mutate(notification.publicId);
    }
    if (notification.targetUrl) {
      setOpen(false);
      await navigate(notification.targetUrl);
    }
  };

  return (
    <DropdownMenu open={open} onOpenChange={setOpen}>
      <DropdownMenuTrigger
        render={
          <Button variant="ghost" size="icon" className="relative">
            <Bell className="h-5 w-5 text-slate-600" />
            {unreadCount > 0 && (
              <span className="absolute top-1.5 right-1.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-medium text-white">
                {unreadCount > 99 ? "99+" : unreadCount}
              </span>
            )}
            <span className="sr-only">Notifications</span>
          </Button>
        }
      />
      <DropdownMenuContent
        className="w-[calc(100vw-2rem)] sm:w-96 max-h-125 flex flex-col"
        align="end"
        sideOffset={8}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-4 py-3 border-b border-slate-200">
          <h3 className="font-semibold text-slate-900">Notifications</h3>
          {unreadCount > 0 && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => markAllAsReadMutation.mutate()}
              disabled={markAllAsReadMutation.isPending}
              className="h-7 text-xs"
            >
              <CheckCheck className="h-3.5 w-3.5 mr-1" />
              Mark all read
            </Button>
          )}
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto">
          {isLoading ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="h-6 w-6 animate-spin text-slate-400" />
            </div>
          ) : notifications.length === 0 ? (
            <div className="py-12 text-center">
              <Bell className="h-10 w-10 text-slate-300 mx-auto mb-2" />
              <p className="text-sm text-slate-500">No notifications yet</p>
            </div>
          ) : (
            <div className="divide-y divide-slate-100">
              {notifications.map((notification) => {
                const config =
                  typeConfig[notification.type] || typeConfig.SYSTEM;
                const Icon = config.icon;

                return (
                  <div
                    role="presentation"
                    key={notification.publicId}
                    className={`group flex gap-3 px-4 py-3 hover:bg-slate-50 cursor-pointer transition-colors ${
                      !notification.read ? "bg-primary-50/30" : ""
                    }`}
                    onClick={() => handleNotificationClick(notification)}
                  >
                    <div
                      className={`h-9 w-9 rounded-full flex items-center justify-center shrink-0 ${config.bg}`}
                    >
                      <Icon className={`h-4 w-4 ${config.color}`} />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-start justify-between gap-2">
                        <p
                          className={`text-sm ${!notification.read ? "font-semibold text-slate-900" : "font-medium text-slate-700"}`}
                        >
                          {notification.title}
                        </p>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-6 w-6 opacity-0 group-hover:opacity-100 transition-opacity shrink-0"
                          onClick={(e) => {
                            e.stopPropagation();
                            deleteMutation.mutate(notification.publicId);
                          }}
                        >
                          <Trash2 className="h-3.5 w-3.5 text-slate-400" />
                        </Button>
                      </div>
                      <p className="text-xs text-slate-600 mt-0.5 line-clamp-2">
                        {notification.message}
                      </p>
                      <p className="text-xs text-slate-400 mt-1">
                        {formatDistanceToNow(parseISO(notification.createdAt), {
                          addSuffix: true,
                        })}
                      </p>
                    </div>
                    {!notification.read && (
                      <div className="h-2 w-2 rounded-full bg-primary-600 shrink-0 mt-2" />
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
