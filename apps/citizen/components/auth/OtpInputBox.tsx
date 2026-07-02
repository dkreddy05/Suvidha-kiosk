"use client";

import React from "react";

interface OtpInputBoxProps {
  length: number;
  value: string[];
  onChange: (index: number, val: string) => void;
  onKeyDown: (index: number, e: React.KeyboardEvent<HTMLInputElement>) => void;
  onPaste: (e: React.ClipboardEvent<HTMLInputElement>) => void;
  error?: boolean;
  inputRefs?: React.RefObject<(HTMLInputElement | null)[]>;
}

export function OtpInputBox({
  length,
  value,
  onChange,
  onKeyDown,
  onPaste,
  error,
  inputRefs,
}: OtpInputBoxProps) {
  return (
    <div className="flex gap-2.5 justify-center">
      {Array.from({ length }).map((_, index) => (
        <input
          key={index}
          ref={(el) => {
            if (inputRefs && inputRefs.current) {
              inputRefs.current[index] = el;
            }
          }}
          type="text"
          maxLength={1}
          value={value[index] || ""}
          onChange={(e) => onChange(index, e.target.value)}
          onKeyDown={(e) => onKeyDown(index, e)}
          onPaste={index === 0 ? onPaste : undefined}
          className={`h-12 w-12 rounded-xl border text-center text-lg font-bold transition-all focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500 bg-white dark:bg-surface-dark ${
            error
              ? "border-danger text-danger focus:ring-danger"
              : "border-border text-gray-900 dark:text-gray-100"
          }`}
          inputMode="numeric"
          pattern="[0-9]*"
          aria-label={`Digit ${index + 1}`}
        />
      ))}
    </div>
  );
}
