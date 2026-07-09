"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { Zap, Droplets, Flame, Loader2 } from "lucide-react";
import { Button, Card, CardContent, CardHeader, CardTitle } from "@suvidha/ui";
import { useLinkAccount } from "@/hooks/billing/useAccounts";
import { PageHeader } from "@/components/shared/PageHeader";
import { toast } from "sonner";

const linkAccountSchema = z.object({
  provider: z.enum(["ELECTRICITY", "WATER", "GAS"], {
    required_error: "Please select a utility type",
  }),
  accountNumber: z.string()
    .min(5, "Account number must be at least 5 characters")
    .max(20, "Account number cannot exceed 20 characters")
    .regex(/^[A-Z0-9-]+$/i, "Invalid character format"),
});

type LinkAccountFormData = z.infer<typeof linkAccountSchema>;

export default function LinkAccountPage() {
  const router = useRouter();
  const linkAccountMutation = useLinkAccount();
  const [selectedType, setSelectedType] = useState<"ELECTRICITY" | "WATER" | "GAS" | null>(null);

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<LinkAccountFormData>({
    resolver: zodResolver(linkAccountSchema),
  });

  const selectUtilityType = (type: "ELECTRICITY" | "WATER" | "GAS") => {
    setSelectedType(type);
    setValue("provider", type, { shouldValidate: true });
  };

  const onSubmit = async (data: LinkAccountFormData) => {
    try {
      await linkAccountMutation.mutateAsync({
        accountNumber: data.accountNumber,
        provider: data.provider,
      });
      toast.success("Account linked successfully!");
      router.push("/billing");
    } catch (err: any) {
      const msg = err?.response?.data?.message || err?.message || "Failed to link account. Please try again.";
      toast.error(msg);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <PageHeader
        title="Link Utility Account"
        description="Add a new utility account to track your bills and payments"
        breadcrumbs={[
          { label: "Connections", href: "/connections" },
          { label: "Link Account" },
        ]}
      />

      <div className="px-4 sm:px-6 lg:px-8 pb-12 max-w-2xl mx-auto">
        <Card className="border border-border bg-white dark:bg-surface-dark shadow-sm">
          <CardHeader className="border-b border-border/40 p-6">
            <CardTitle className="text-xl font-semibold">Utility Details</CardTitle>
          </CardHeader>
          <CardContent className="p-6">
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
              {/* Utility Type Selector */}
              <div className="space-y-3">
                <label className="text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Select Utility Type
                </label>
                <div className="grid grid-cols-3 gap-4">
                  {[
                    { type: "ELECTRICITY", label: "Electricity", icon: Zap, color: "text-amber-500 bg-amber-50 dark:bg-amber-500/10" },
                    { type: "WATER", label: "Water", icon: Droplets, color: "text-blue-500 bg-blue-50 dark:bg-blue-500/10" },
                    { type: "GAS", label: "Gas", icon: Flame, color: "text-orange-500 bg-orange-50 dark:bg-orange-500/10" },
                  ].map((item) => {
                    const Icon = item.icon;
                    const isSelected = selectedType === item.type;
                    return (
                      <button
                        key={item.type}
                        type="button"
                        onClick={() => selectUtilityType(item.type as any)}
                        className={`flex flex-col items-center justify-center p-4 rounded-xl border-2 transition-all duration-200 gap-2 ${
                          isSelected
                            ? "border-brand-500 bg-brand-50/50 dark:bg-brand-500/5 ring-2 ring-brand-500/20"
                            : "border-border hover:border-gray-300 dark:hover:border-gray-700 bg-transparent"
                        }`}
                      >
                        <div className={`p-3 rounded-lg ${item.color}`}>
                          <Icon className="h-6 w-6" />
                        </div>
                        <span className="text-sm font-medium">{item.label}</span>
                      </button>
                    );
                  })}
                </div>
                {errors.provider && (
                  <p className="text-sm text-red-500">{errors.provider.message}</p>
                )}
              </div>

              {/* Account / Consumer ID Input */}
              <div className="space-y-2">
                <label htmlFor="accountNumber" className="text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Consumer ID / Account Number
                </label>
                <input
                  id="accountNumber"
                  type="text"
                  placeholder="e.g. E123456789"
                  {...register("accountNumber")}
                  className="w-full px-4 py-2.5 rounded-lg border border-border bg-white dark:bg-surface-dark text-gray-900 dark:text-gray-100 placeholder-muted-foreground/60 focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 transition-all font-mono"
                />
                <p className="text-xs text-muted dark:text-muted-dark">
                  Refer to your physical utility bill to find your consumer ID or account number.
                </p>
                {errors.accountNumber && (
                  <p className="text-sm text-red-500">{errors.accountNumber.message}</p>
                )}
              </div>

              {/* Action Buttons */}
              <div className="flex items-center justify-end gap-3 pt-4 border-t border-border/40">
                <Link href="/connections">
                  <Button type="button" variant="outline">
                    Cancel
                  </Button>
                </Link>
                <Button type="submit" disabled={isSubmitting} className="min-w-[120px]">
                  {isSubmitting ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Linking...
                    </>
                  ) : (
                    "Link Account"
                  )}
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
