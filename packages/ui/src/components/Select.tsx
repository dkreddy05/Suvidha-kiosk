import * as React from "react";
import { cn } from "../lib/utils";
import { ChevronDown } from "lucide-react";

export interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  error?: string;
  options?: Array<{ value: string; label: string }>;
  placeholder?: string;
  children?: React.ReactNode;
}

const Select = React.forwardRef<HTMLSelectElement, SelectProps>(
  ({ className, label, error, options, placeholder, children, id, ...props }, ref) => {
    const selectId = id || label?.toLowerCase().replace(/\s+/g, "-");
    return (
      <div className="space-y-1.5">
        {label && (
          <label htmlFor={selectId} className="text-sm font-medium text-gray-700 dark:text-gray-300">
            {label}
          </label>
        )}
        <div className="relative">
          <select
            id={selectId}
            className={cn(
              "flex h-10 w-full appearance-none rounded-lg border border-border bg-white dark:bg-surface-dark px-3 py-2 pr-8 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50",
              error && "border-danger",
              className
            )}
            ref={ref}
            aria-invalid={!!error}
            {...props}
          >
            {placeholder && <option value="">{placeholder}</option>}
            {options
              ? options.map((opt) => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))
              : children}
          </select>
          <ChevronDown className="pointer-events-none absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted dark:text-muted-dark" />
        </div>
        {error && <p className="text-xs text-danger" role="alert">{error}</p>}
      </div>
    );
  }
);
Select.displayName = "Select";
export { Select };
