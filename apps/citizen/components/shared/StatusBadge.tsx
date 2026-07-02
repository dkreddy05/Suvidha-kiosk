import { Badge, statusConfig } from '@suvidha/ui';

interface StatusBadgeProps {
  status: string;
  className?: string;
}

export function StatusBadge({ status, className }: StatusBadgeProps) {
  const config = statusConfig[status] ?? {
    label: status ?? 'Unknown',
    variant: 'default' as const,
  };

  return (
    <Badge variant={config.variant} className={className} aria-label={`Status: ${config.label}`}>
      {config.label}
    </Badge>
  );
}
