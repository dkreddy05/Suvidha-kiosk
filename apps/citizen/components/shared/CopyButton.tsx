"use client";

import { useState } from "react";
import { Copy, Check } from "lucide-react";
import { Button } from "@suvidha/ui";
import { toast } from "sonner";

interface CopyButtonProps {
  value: string;
  label?: string;
}

export function CopyButton({ value, label = "Reference number" }: CopyButtonProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(value);
      setCopied(true);
      toast.success(`${label} copied to clipboard`);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      toast.error("Failed to copy text");
    }
  };

  return (
    <Button
      variant="ghost"
      size="icon"
      onClick={handleCopy}
      className="h-8 w-8 text-muted hover:text-foreground hover:bg-gray-100 dark:hover:bg-gray-800"
      title={`Copy ${label}`}
    >
      {copied ? <Check className="h-4 w-4 text-success" /> : <Copy className="h-4 w-4" />}
    </Button>
  );
}
