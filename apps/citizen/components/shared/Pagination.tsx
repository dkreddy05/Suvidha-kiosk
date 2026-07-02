"use client";

import { Button } from "@suvidha/ui";
import { ChevronLeft, ChevronRight } from "lucide-react";

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  disabled?: boolean;
}

export function Pagination({
  currentPage,
  totalPages,
  onPageChange,
  disabled = false,
}: PaginationProps) {
  if (totalPages <= 1) return null;

  return (
    <div className="flex items-center justify-between pt-4 border-t border-border">
      <p className="text-xs text-muted dark:text-muted-dark">
        Page {currentPage + 1} of {totalPages}
      </p>
      <div className="flex items-center gap-2">
        <Button
          variant="outline"
          size="sm"
          disabled={currentPage <= 0 || disabled}
          onClick={() => onPageChange(currentPage - 1)}
          className="h-8 px-3 text-xs"
        >
          <ChevronLeft className="h-4 w-4 mr-1 shrink-0" />
          Previous
        </Button>
        <Button
          variant="outline"
          size="sm"
          disabled={currentPage >= totalPages - 1 || disabled}
          onClick={() => onPageChange(currentPage + 1)}
          className="h-8 px-3 text-xs"
        >
          Next
          <ChevronRight className="h-4 w-4 ml-1 shrink-0" />
        </Button>
      </div>
    </div>
  );
}
