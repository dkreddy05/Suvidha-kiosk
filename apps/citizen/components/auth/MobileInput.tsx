"use client";

import React from "react";
import { Smartphone } from "lucide-react";
import { Input } from "@suvidha/ui";

interface MobileInputProps {
  value: string;
  onChange: (val: string) => void;
  error?: string;
  disabled?: boolean;
}

export function MobileInput({ value, onChange, error, disabled }: MobileInputProps) {
  return (
    <div className="relative">
      <Input
        label="Mobile Number"
        type="tel"
        maxLength={10}
        placeholder="Enter 10-digit number"
        value={value}
        onChange={(e) => onChange(e.target.value.replace(/\D/g, ""))}
        error={error}
        disabled={disabled}
        className="pl-10"
      />
      <Smartphone className="absolute left-3 top-[38px] h-5 w-5 text-muted pointer-events-none" />
    </div>
  );
}
