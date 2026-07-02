"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { motion } from "framer-motion";
import {
  User,
  Shield,
  AlertTriangle,
  ChevronRight,
  Lock,
  Settings,
  Trash2,
} from "lucide-react";
import {
  Button,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Input,
  Select,
  Tabs,
  TabsList,
  TabsTrigger,
  TabsContent,
  ConfirmDialog,
  ConfirmDialogTrigger,
  ConfirmDialogContent,
  ConfirmDialogHeader,
  ConfirmDialogTitle,
  ConfirmDialogDescription,
  ConfirmDialogFooter,
  ConfirmDialogCancel,
  ConfirmDialogAction,
} from "@suvidha/ui";
import { maskAadhaar, maskMobile } from "@/lib/utils";
import { useProfile } from "@/hooks/auth/useAuth";
import { useUpdateProfile, useDeleteAccount } from "@/hooks/profile/useProfile";
import { LoadingSkeleton } from "@/components/shared/LoadingSkeleton";
import { PageHeader } from "@/components/shared/PageHeader";
import { useAuthStore } from "@/store/auth.store";
import { toast } from "sonner";
import { LANGUAGES } from "@/lib/constants";

const profileSchema = z.object({
  name: z
    .string()
    .min(2, "Name must be at least 2 characters")
    .max(100, "Name must not exceed 100 characters"),
  languagePreference: z.enum(["en", "hi", "te", "ta"]),
});

type ProfileFormValues = z.infer<typeof profileSchema>;

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.06, delayChildren: 0.08 },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 16 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.3, ease: "easeOut" } },
};

export default function ProfilePage() {
  const router = useRouter();
  const { data: profile, isLoading: profileLoading, error: profileError } = useProfile();
  const updateProfileMutation = useUpdateProfile();
  const deleteAccountMutation = useDeleteAccount();
  const logout = useAuthStore((s) => s.logout);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      name: "",
      languagePreference: "en",
    },
  });

  useEffect(() => {
    if (profile && !updateProfileMutation.isPending) {
      reset({
        name: profile.name ?? "",
        languagePreference: profile.languagePref ?? "en",
      });
    }
  }, [profile, reset, updateProfileMutation.isPending]);

  const onSubmit = async (data: ProfileFormValues) => {
    try {
      await updateProfileMutation.mutateAsync({
        name: data.name.trim(),
        languagePreference: data.languagePreference,
      });
      toast.success("Profile updated successfully");
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : "Failed to update profile";
      toast.error(msg);
    }
  };

  const handleDeleteAccount = async () => {
    try {
      await deleteAccountMutation.mutateAsync();
      toast.success("Your account has been deleted");
      logout();
      router.push("/");
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : "Failed to delete account";
      toast.error(msg);
    }
  };

  if (profileLoading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
        <PageHeader title="Profile & Settings" description="Manage your account" />
        <div className="px-4 sm:px-6 lg:px-8 pb-8 space-y-6">
          <div className="rounded-xl border border-border bg-white dark:bg-surface-dark p-6 space-y-5">
            <LoadingSkeleton rows={1} />
            <LoadingSkeleton rows={1} />
            <LoadingSkeleton rows={1} />
            <LoadingSkeleton rows={1} className="w-1/3" />
          </div>
        </div>
      </div>
    );
  }

  if (profileError) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
        <PageHeader title="Profile & Settings" />
        <div className="px-4 sm:px-6 lg:px-8 pb-8">
          <Card>
            <CardContent className="flex flex-col items-center justify-center py-16">
              <AlertTriangle className="h-10 w-10 text-danger mb-4" />
              <p className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1">
                Failed to load profile
              </p>
              <p className="text-sm text-muted dark:text-muted-dark">
                Please try again later
              </p>
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <PageHeader
        title="Profile & Settings"
        description="Manage your personal information and account settings"
      />

      <motion.div
        variants={containerVariants}
        initial="hidden"
        animate="visible"
        className="px-4 sm:px-6 lg:px-8 pb-8"
      >
        <Tabs defaultValue="personal-info" className="w-full">
          <motion.div variants={itemVariants}>
            <TabsList className="mb-6">
              <TabsTrigger value="personal-info" className="flex items-center gap-2">
                <User className="h-4 w-4" />
                Personal Info
              </TabsTrigger>
              <TabsTrigger value="privacy" className="flex items-center gap-2">
                <Shield className="h-4 w-4" />
                Privacy Center
              </TabsTrigger>
              <TabsTrigger value="danger" className="flex items-center gap-2">
                <AlertTriangle className="h-4 w-4" />
                Danger Zone
              </TabsTrigger>
            </TabsList>
          </motion.div>

          {/* Tab 1: Personal Info */}
          <TabsContent value="personal-info">
            <motion.div variants={itemVariants}>
              <Card>
                <CardHeader>
                  <CardTitle>Personal Information</CardTitle>
                  <p className="text-sm text-muted dark:text-muted-dark">
                    Update your profile details
                  </p>
                </CardHeader>
                <CardContent>
                  <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
                    <Input
                      label="Full Name"
                      placeholder="Enter your full name"
                      error={errors.name?.message}
                      {...register("name")}
                    />

                    <div className="relative">
                      <Input
                        label="Mobile Number"
                        value={maskMobile(profile?.mobile ?? "")}
                        disabled
                      />
                      <Lock className="absolute right-3 top-[38px] h-4 w-4 text-muted dark:text-muted-dark pointer-events-none" />
                    </div>

                    <div className="relative">
                      <Input
                        label="Consumer ID"
                        value={profile?.consumerId ?? "Not assigned"}
                        disabled
                      />
                      <Lock className="absolute right-3 top-[38px] h-4 w-4 text-muted dark:text-muted-dark pointer-events-none" />
                    </div>

                    <Select
                      label="Language Preference"
                      options={LANGUAGES.map((l) => ({
                        value: l.code,
                        label: l.label,
                      }))}
                      {...register("languagePreference")}
                    />

                    <div className="relative">
                      <Input
                        label="Aadhaar Number"
                        value={profile?.aadhaarLast4 ? `xxxx-xxxx-${profile.aadhaarLast4}` : "Not provided"}
                        disabled
                      />
                      <Lock className="absolute right-3 top-[38px] h-4 w-4 text-muted dark:text-muted-dark pointer-events-none" />
                    </div>

                    <div className="pt-2">
                      <Button
                        type="submit"
                        disabled={!isDirty}
                        loading={updateProfileMutation.isPending}
                      >
                        <Settings className="h-4 w-4 mr-2" />
                        Save Changes
                      </Button>
                    </div>
                  </form>
                </CardContent>
              </Card>
            </motion.div>
          </TabsContent>

          {/* Tab 2: Privacy Center */}
          <TabsContent value="privacy">
            <motion.div variants={itemVariants}>
              <Link href="/profile/privacy">
                <Card className="cursor-pointer transition-all duration-200 hover:shadow-md hover:-translate-y-0.5 group">
                  <CardContent className="flex items-center gap-5 p-6">
                    <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-xl bg-brand-50 dark:bg-brand-900/20 text-brand-600 dark:text-brand-400">
                      <Shield className="h-7 w-7" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <h3 className="text-lg font-semibold font-display text-gray-900 dark:text-gray-100">
                        Privacy Center
                      </h3>
                      <p className="text-sm text-muted dark:text-muted-dark mt-1">
                        Manage your data, consents, and privacy settings
                      </p>
                    </div>
                    <ChevronRight className="h-5 w-5 shrink-0 text-muted dark:text-muted-dark group-hover:text-gray-900 dark:group-hover:text-gray-100 transition-colors" />
                  </CardContent>
                </Card>
              </Link>
            </motion.div>
          </TabsContent>

          {/* Tab 3: Danger Zone */}
          <TabsContent value="danger">
            <motion.div variants={itemVariants}>
              <Card className="border-danger/30 dark:border-danger/20">
                <CardHeader>
                  <CardTitle className="flex items-center gap-2 text-danger">
                    <AlertTriangle className="h-5 w-5" />
                    Danger Zone
                  </CardTitle>
                  <p className="text-sm text-muted dark:text-muted-dark">
                    Irreversible actions that affect your account
                  </p>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="rounded-lg bg-danger/5 dark:bg-danger/10 border border-danger/20 p-4 space-y-3">
                    <div className="flex items-start gap-3">
                      <Trash2 className="h-5 w-5 text-danger shrink-0 mt-0.5" />
                      <div>
                        <h4 className="font-semibold text-gray-900 dark:text-gray-100 text-sm">
                          Delete Account
                        </h4>
                        <p className="text-sm text-muted dark:text-muted-dark mt-1">
                          Permanently delete your account and all associated data. This action
                          cannot be undone. All your bills, grievances, and connection requests
                          will be removed.
                        </p>
                      </div>
                    </div>

                    <ConfirmDialog>
                      <ConfirmDialogTrigger asChild>
                        <Button variant="destructive" className="mt-2">
                          <Trash2 className="h-4 w-4 mr-2" />
                          Delete Account
                        </Button>
                      </ConfirmDialogTrigger>
                      <ConfirmDialogContent>
                        <ConfirmDialogHeader>
                          <ConfirmDialogTitle>Are you sure?</ConfirmDialogTitle>
                          <ConfirmDialogDescription>
                            This action cannot be undone. All your data, including bills,
                            grievances, and connection requests, will be permanently deleted.
                          </ConfirmDialogDescription>
                        </ConfirmDialogHeader>
                        <ConfirmDialogFooter>
                          <ConfirmDialogCancel>Cancel</ConfirmDialogCancel>
                          <ConfirmDialogAction
                            className="bg-danger hover:bg-red-700 text-white"
                            onClick={handleDeleteAccount}
                          >
                            {deleteAccountMutation.isPending ? (
                              <span className="flex items-center gap-2">
                                <span className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                                Deleting...
                              </span>
                            ) : (
                              "Delete My Account"
                            )}
                          </ConfirmDialogAction>
                        </ConfirmDialogFooter>
                      </ConfirmDialogContent>
                    </ConfirmDialog>
                  </div>
                </CardContent>
              </Card>
            </motion.div>
          </TabsContent>
        </Tabs>
      </motion.div>
    </div>
  );
}


