"use client";

import React from "react";
import { Card, CardContent } from "@suvidha/ui";
import { Zap, Droplets, Flame } from "lucide-react";
import type { UtilityAccount } from "@/types/billing";

interface AccountCardProps {
  account: UtilityAccount;
  onClick?: () => void;
}

export function AccountCard({ account, onClick }: AccountCardProps) {
  const icons = {
    ELECTRICITY: Zap,
    WATER: Droplets,
    GAS: Flame,
  };

  const Icon = icons[account.serviceType] || Zap;

  return (
    <Card
      onClick={onClick}
      className={`cursor-pointer transition-all duration-200 hover:shadow-md hover:-translate-y-0.5 border border-border bg-white dark:bg-surface-dark ${
        onClick ? "active:scale-[0.98]" : ""
      }`}
    >
      <CardContent className="p-5 flex items-center gap-4">
        <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-brand-50 dark:bg-brand-900/20 text-brand-600 dark:text-brand-400">
          <Icon className="h-6 w-6" />
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-xs font-semibold text-muted dark:text-muted-dark uppercase tracking-wider">
            {account.serviceType}
          </p>
          <h4 className="text-base font-bold text-gray-900 dark:text-gray-100 truncate mt-0.5">
            {account.providerName}
          </h4>
          <p className="text-sm text-muted dark:text-muted-dark font-mono mt-0.5">
            #{account.accountNumber}
          </p>
        </div>
      </CardContent>
    </Card>
  );
}
