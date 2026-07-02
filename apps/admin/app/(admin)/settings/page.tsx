"use client";

import { useState, useCallback } from "react";
import { motion } from "framer-motion";
import { toast } from "sonner";
import { useTheme } from "next-themes";
import {
  Settings,
  Bell,
  Shield,
  UserCog,
  Save,
  Eye,
  EyeOff,
  Moon,
  Sun,
  Monitor,
} from "lucide-react";
import {
  Button,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Input,
  Select,
  Skeleton,
} from "@suvidha/ui";

export default function SettingsPage() {
  const { theme, setTheme } = useTheme();

  const [loading, setLoading] = useState(false);
  const [profileSaving, setProfileSaving] = useState(false);
  const [notifSaving, setNotifSaving] = useState(false);
  const [passwordSaving, setPasswordSaving] = useState(false);

  const [profile, setProfile] = useState({
    name: "Admin User",
    email: "admin@suvidha.gov.in",
  });

  const [notifications, setNotifications] = useState({
    emailAlerts: true,
    smsAlerts: false,
  });

  const [password, setPassword] = useState({
    current: "",
    newPassword: "",
    confirm: "",
  });

  const [showPassword, setShowPassword] = useState({
    current: false,
    new: false,
    confirm: false,
  });

  const handleSaveProfile = useCallback(async () => {
    if (!profile.name.trim() || !profile.email.trim()) {
      toast.error("Name and email are required");
      return;
    }
    setProfileSaving(true);
    await new Promise((r) => setTimeout(r, 800));
    setProfileSaving(false);
    toast.success("Profile updated successfully");
  }, [profile]);

  const handleSaveNotifications = useCallback(async () => {
    setNotifSaving(true);
    await new Promise((r) => setTimeout(r, 600));
    setNotifSaving(false);
    toast.success("Notification preferences saved");
  }, [notifications]);

  const handleChangePassword = useCallback(async () => {
    if (!password.current) {
      toast.error("Current password is required");
      return;
    }
    if (!password.newPassword) {
      toast.error("New password is required");
      return;
    }
    if (password.newPassword.length < 8) {
      toast.error("New password must be at least 8 characters");
      return;
    }
    if (password.newPassword !== password.confirm) {
      toast.error("Passwords do not match");
      return;
    }
    setPasswordSaving(true);
    await new Promise((r) => setTimeout(r, 1000));
    setPasswordSaving(false);
    setPassword({ current: "", newPassword: "", confirm: "" });
    toast.success("Password changed successfully");
  }, [password]);

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-48 w-full rounded-lg" />
        <Skeleton className="h-32 w-full rounded-lg" />
        <Skeleton className="h-48 w-full rounded-lg" />
        <Skeleton className="h-32 w-full rounded-lg" />
      </div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="space-y-6"
    >
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
          Settings
        </h1>
        <p className="text-sm text-gray-500 dark:text-gray-400">
          Manage your account settings and preferences
        </p>
      </div>

      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <UserCog className="h-5 w-5 text-gray-500" />
            <CardTitle className="text-base font-semibold">Profile</CardTitle>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Full Name
              </label>
              <Input
                value={profile.name}
                onChange={(e) =>
                  setProfile((p) => ({ ...p, name: e.target.value }))
                }
                placeholder="Enter your name"
              />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Email Address
              </label>
              <Input
                type="email"
                value={profile.email}
                onChange={(e) =>
                  setProfile((p) => ({ ...p, email: e.target.value }))
                }
                placeholder="Enter your email"
              />
            </div>
          </div>
          <div className="flex justify-end">
            <Button onClick={handleSaveProfile} disabled={profileSaving}>
              <Save className="mr-2 h-4 w-4" />
              {profileSaving ? "Saving..." : "Save"}
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Bell className="h-5 w-5 text-gray-500" />
            <CardTitle className="text-base font-semibold">
              Notifications
            </CardTitle>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <label className="flex cursor-pointer items-center justify-between rounded-lg border border-gray-200 p-4 dark:border-gray-700">
            <div>
              <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                Email Notifications
              </p>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                Receive email alerts for important updates
              </p>
            </div>
            <button
              type="button"
              role="switch"
              aria-checked={notifications.emailAlerts}
              onClick={() =>
                setNotifications((n) => ({
                  ...n,
                  emailAlerts: !n.emailAlerts,
                }))
              }
              className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors focus:outline-none ${
                notifications.emailAlerts
                  ? "bg-brand-500"
                  : "bg-gray-200 dark:bg-gray-700"
              }`}
            >
              <span
                className={`pointer-events-none inline-block h-5 w-5 rounded-full bg-white shadow transition-transform ${
                  notifications.emailAlerts ? "translate-x-5" : "translate-x-0"
                }`}
              />
            </button>
          </label>

          <label className="flex cursor-pointer items-center justify-between rounded-lg border border-gray-200 p-4 dark:border-gray-700">
            <div>
              <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                SMS Alerts
              </p>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                Receive SMS notifications for critical alerts
              </p>
            </div>
            <button
              type="button"
              role="switch"
              aria-checked={notifications.smsAlerts}
              onClick={() =>
                setNotifications((n) => ({
                  ...n,
                  smsAlerts: !n.smsAlerts,
                }))
              }
              className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors focus:outline-none ${
                notifications.smsAlerts
                  ? "bg-brand-500"
                  : "bg-gray-200 dark:bg-gray-700"
              }`}
            >
              <span
                className={`pointer-events-none inline-block h-5 w-5 rounded-full bg-white shadow transition-transform ${
                  notifications.smsAlerts ? "translate-x-5" : "translate-x-0"
                }`}
              />
            </button>
          </label>

          <div className="flex justify-end">
            <Button
              onClick={handleSaveNotifications}
              disabled={notifSaving}
            >
              <Save className="mr-2 h-4 w-4" />
              {notifSaving ? "Saving..." : "Save Preferences"}
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Shield className="h-5 w-5 text-gray-500" />
            <CardTitle className="text-base font-semibold">
              Security
            </CardTitle>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Current Password
              </label>
              <div className="relative">
                <Input
                  type={showPassword.current ? "text" : "password"}
                  value={password.current}
                  onChange={(e) =>
                    setPassword((p) => ({ ...p, current: e.target.value }))
                  }
                  placeholder="Enter current password"
                />
                <button
                  type="button"
                  onClick={() =>
                    setShowPassword((s) => ({
                      ...s,
                      current: !s.current,
                    }))
                  }
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
                >
                  {showPassword.current ? (
                    <EyeOff className="h-4 w-4" />
                  ) : (
                    <Eye className="h-4 w-4" />
                  )}
                </button>
              </div>
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                New Password
              </label>
              <div className="relative">
                <Input
                  type={showPassword.new ? "text" : "password"}
                  value={password.newPassword}
                  onChange={(e) =>
                    setPassword((p) => ({ ...p, newPassword: e.target.value }))
                  }
                  placeholder="Enter new password"
                />
                <button
                  type="button"
                  onClick={() =>
                    setShowPassword((s) => ({
                      ...s,
                      new: !s.new,
                    }))
                  }
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
                >
                  {showPassword.new ? (
                    <EyeOff className="h-4 w-4" />
                  ) : (
                    <Eye className="h-4 w-4" />
                  )}
                </button>
              </div>
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Confirm New Password
              </label>
              <div className="relative">
                <Input
                  type={showPassword.confirm ? "text" : "password"}
                  value={password.confirm}
                  onChange={(e) =>
                    setPassword((p) => ({ ...p, confirm: e.target.value }))
                  }
                  placeholder="Confirm new password"
                />
                <button
                  type="button"
                  onClick={() =>
                    setShowPassword((s) => ({
                      ...s,
                      confirm: !s.confirm,
                    }))
                  }
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
                >
                  {showPassword.confirm ? (
                    <EyeOff className="h-4 w-4" />
                  ) : (
                    <Eye className="h-4 w-4" />
                  )}
                </button>
              </div>
            </div>
          </div>
          <div className="flex justify-end">
            <Button onClick={handleChangePassword} disabled={passwordSaving}>
              <Shield className="mr-2 h-4 w-4" />
              {passwordSaving ? "Changing..." : "Change Password"}
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Settings className="h-5 w-5 text-gray-500" />
            <CardTitle className="text-base font-semibold">Theme</CardTitle>
          </div>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col gap-4 sm:flex-row">
            {[
              { value: "light", label: "Light", icon: <Sun className="h-5 w-5" /> },
              { value: "dark", label: "Dark", icon: <Moon className="h-5 w-5" /> },
              { value: "system", label: "System", icon: <Monitor className="h-5 w-5" /> },
            ].map((option) => (
              <button
                key={option.value}
                type="button"
                onClick={() => setTheme(option.value)}
                className={`flex flex-1 items-center gap-3 rounded-lg border-2 p-4 text-left transition-all ${
                  theme === option.value
                    ? "border-brand-500 bg-brand-50 dark:bg-brand-900/20"
                    : "border-gray-200 bg-white hover:border-gray-300 dark:border-gray-700 dark:bg-gray-900 dark:hover:border-gray-600"
                }`}
              >
                <span
                  className={
                    theme === option.value
                      ? "text-brand-600 dark:text-brand-400"
                      : "text-gray-500"
                  }
                >
                  {option.icon}
                </span>
                <div>
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                    {option.label}
                  </p>
                  <p className="text-xs text-gray-500">
                    {option.value === "light"
                      ? "Always use light mode"
                      : option.value === "dark"
                      ? "Always use dark mode"
                      : "Follow system preference"}
                  </p>
                </div>
              </button>
            ))}
          </div>
        </CardContent>
      </Card>
    </motion.div>
  );
}
