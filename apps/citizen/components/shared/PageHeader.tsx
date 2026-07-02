import Link from 'next/link';
import { ChevronRight, Home } from 'lucide-react';
import { cn } from '@/lib/utils';

interface Breadcrumb {
  label: string;
  href?: string;
}

interface PageHeaderProps {
  title: string;
  description?: string;
  breadcrumbs?: Breadcrumb[];
  actions?: React.ReactNode;
  className?: string;
}

export function PageHeader({
  title,
  description,
  breadcrumbs,
  actions,
  className,
}: PageHeaderProps) {
  return (
    <div className={cn('px-4 sm:px-6 lg:px-8 pt-6 pb-4', className)}>
      {breadcrumbs && breadcrumbs.length > 0 && (
        <nav className="flex items-center gap-1.5 text-sm text-muted dark:text-muted-dark mb-3" aria-label="Breadcrumb">
          <Link
            href="/dashboard"
            className="flex items-center gap-1 hover:text-gray-900 dark:hover:text-gray-100 transition-colors"
          >
            <Home className="h-3.5 w-3.5" />
            <span className="sr-only">Home</span>
          </Link>
          {breadcrumbs.map((crumb, idx) => (
            <span key={idx} className="flex items-center gap-1.5">
              <ChevronRight className="h-3.5 w-3.5" />
              {crumb.href ? (
                <Link
                  href={crumb.href}
                  className="hover:text-gray-900 dark:hover:text-gray-100 transition-colors"
                >
                  {crumb.label}
                </Link>
              ) : (
                <span className="text-gray-900 dark:text-gray-100 font-medium">
                  {crumb.label}
                </span>
              )}
            </span>
          ))}
        </nav>
      )}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="space-y-1">
          <h1 className="text-2xl font-display font-bold tracking-tight text-gray-900 dark:text-gray-100">
            {title}
          </h1>
          {description && (
            <p className="text-sm text-muted dark:text-muted-dark">{description}</p>
          )}
        </div>
        {actions && <div className="flex items-center gap-2 shrink-0">{actions}</div>}
      </div>
    </div>
  );
}
