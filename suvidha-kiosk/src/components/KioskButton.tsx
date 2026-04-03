import clsx from 'clsx'
import type { ButtonHTMLAttributes } from 'react'

type Props = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'danger'
}

export function KioskButton({ variant = 'primary', className, ...props }: Props) {
  return (
    <button
      {...props}
      className={clsx(
        'min-h-16 select-none rounded-2xl px-6 py-4 text-[24px] font-semibold leading-none transition',
        'disabled:opacity-50',
        variant === 'primary' && 'bg-sky-500 text-slate-950 active:bg-sky-400',
        variant === 'secondary' && 'border border-white/20 bg-white/10 text-white active:bg-white/15',
        variant === 'danger' && 'bg-rose-500 text-slate-950 active:bg-rose-400',
        className,
      )}
    />
  )
}
