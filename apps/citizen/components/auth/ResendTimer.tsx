"use client";

import React from "react";

interface ResendTimerProps {
  countdown: number;
  onResend: () => void;
  loading?: boolean;
}

export function ResendTimer({ countdown, onResend, loading }: ResendTimerProps) {
  return (
    <div className="flex justify-center text-sm">
      {countdown > 0 ? (
        <span className="text-muted dark:text-muted-dark">
          Resend OTP in <span className="font-semibold text-gray-900 dark:text-gray-100">{countdown}s</span>
        </span>
      ) : (
        <button
          type="button"
          onClick={onResend}
          disabled={loading}
          className="text-brand-500 font-semibold hover:text-brand-600 dark:hover:text-brand-400 transition-colors focus:outline-none disabled:opacity-50"
        >
          {loading ? "Resending..." : "Resend OTP"}
        </button>
      )}
    </div>
  );
}
