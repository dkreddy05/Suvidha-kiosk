import React from 'react';
import { cn } from '@/lib/utils';

interface FormFieldProps extends React.HTMLAttributes<HTMLDivElement> {
  label: string;
  error?: string;
  hint?: string;
  required?: boolean;
}

export function FormField({
  label,
  error,
  hint,
  required,
  children,
  className,
  ...props
}: FormFieldProps) {
  return (
    <div className={cn("space-y-1.5 w-full", className)} {...props}>
      <div className="flex items-center justify-between">
        <label className="text-sm font-semibold text-gray-700 dark:text-gray-300">
          {label}
          {required && <span className="text-danger ml-1">*</span>}
        </label>
      </div>
      {children}
      {error && (
        <p className="text-xs font-medium text-danger animate-fadeIn">{error}</p>
      )}
      {!error && hint && (
        <p className="text-xs text-muted dark:text-muted-dark">{hint}</p>
      )}
    </div>
  );
}
