import { Skeleton } from '@suvidha/ui';
import { cn } from '@/lib/utils';

interface LoadingSkeletonProps {
  rows?: number;
  type?: 'card' | 'table' | 'text';
  className?: string;
}

export function LoadingSkeleton({
  rows = 3,
  type = 'text',
  className,
}: LoadingSkeletonProps) {
  if (type === 'card') {
    return (
      <div className={cn('grid gap-4 sm:grid-cols-2 lg:grid-cols-3', className)}>
        {Array.from({ length: rows }).map((_, i) => (
          <div
            key={i}
            className="rounded-xl border border-border bg-white dark:bg-surface-dark p-6 space-y-4"
          >
            <Skeleton className="h-4 w-1/3" />
            <Skeleton className="h-8 w-2/3" />
            <Skeleton className="h-4 w-1/2" />
            <div className="flex gap-2 pt-2">
              <Skeleton className="h-8 w-20 rounded-lg" />
              <Skeleton className="h-8 w-20 rounded-lg" />
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (type === 'table') {
    return (
      <div className={cn('rounded-xl border border-border bg-white dark:bg-surface-dark overflow-hidden', className)}>
        <div className="p-4 border-b border-border">
          <Skeleton className="h-10 w-full" />
        </div>
        {Array.from({ length: rows }).map((_, i) => (
          <div key={i} className="flex items-center gap-4 p-4 border-b border-border last:border-b-0">
            <Skeleton className="h-5 w-5 rounded" />
            <Skeleton className="h-4 flex-1" />
            <Skeleton className="h-4 w-24" />
            <Skeleton className="h-6 w-20 rounded-full" />
            <Skeleton className="h-4 w-16" />
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className={cn('space-y-3', className)}>
      {Array.from({ length: rows }).map((_, i) => (
        <Skeleton
          key={i}
          className={cn('h-4 rounded', i === rows - 1 ? 'w-3/4' : 'w-full')}
        />
      ))}
    </div>
  );
}
