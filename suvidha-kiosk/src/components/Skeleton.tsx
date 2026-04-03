import clsx from 'clsx'

export function Skeleton({ className }: { className?: string }) {
  return (
    <div
      className={clsx(
        'animate-pulse rounded-2xl bg-white/10',
        className,
      )}
    />
  )
}
