import { KioskButton } from './KioskButton'

export function EmptyState({
  title,
  actionLabel,
  onAction,
}: {
  title: string
  actionLabel?: string
  onAction?: () => void
}) {
  return (
    <div className="rounded-3xl border border-white/10 bg-white/5 p-10">
      <div className="text-[28px] font-semibold">{title}</div>
      {actionLabel && onAction ? (
        <div className="mt-6">
          <KioskButton onClick={onAction}>{actionLabel}</KioskButton>
        </div>
      ) : null}
    </div>
  )
}
