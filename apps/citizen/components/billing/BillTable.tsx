"use client";

import React from "react";
import type { Bill } from "@/types/billing";
import { formatCurrency, formatDate } from "@/lib/utils";
import { StatusBadge } from "../shared/StatusBadge";
import { Button } from "@suvidha/ui";

interface BillTableProps {
  bills: Bill[];
  onPay?: (bill: Bill) => void;
}

export function BillTable({ bills, onPay }: BillTableProps) {
  if (bills.length === 0) {
    return (
      <div className="text-center py-8 text-sm text-muted dark:text-muted-dark">
        No bills found
      </div>
    );
  }

  return (
    <div className="overflow-x-auto w-full">
      <table className="w-full text-sm text-left">
        <thead>
          <tr className="border-b border-border text-xs uppercase tracking-wider text-muted font-semibold">
            <th className="py-3 px-4">Bill Month</th>
            <th className="py-3 px-4">Bill Number</th>
            <th className="py-3 px-4 text-right">Amount</th>
            <th className="py-3 px-4">Due Date</th>
            <th className="py-3 px-4">Status</th>
            {onPay && <th className="py-3 px-4 text-right">Actions</th>}
          </tr>
        </thead>
        <tbody>
          {bills.map((bill) => (
            <tr
              key={bill.id}
              className="border-b border-border hover:bg-gray-50 dark:hover:bg-gray-900/30 transition-colors"
            >
              <td className="py-3.5 px-4 font-medium">{bill.billMonth}</td>
              <td className="py-3.5 px-4 font-mono text-xs">{bill.billNumber}</td>
              <td className="py-3.5 px-4 text-right font-semibold">
                {formatCurrency(bill.amount)}
              </td>
              <td className="py-3.5 px-4">{formatDate(bill.dueDate, "short")}</td>
              <td className="py-3.5 px-4">
                <StatusBadge status={bill.status} />
              </td>
              {onPay && (
                <td className="py-3.5 px-4 text-right">
                  {bill.status === "PENDING" || bill.status === "OVERDUE" || bill.status === "PARTIALLY_PAID" ? (
                    <Button size="sm" onClick={() => onPay(bill)}>
                      Pay Now
                    </Button>
                  ) : (
                    "—"
                  )}
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
