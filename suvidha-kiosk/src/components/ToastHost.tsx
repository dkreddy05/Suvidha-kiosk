import { motion, AnimatePresence } from 'framer-motion'
import { useToastStore } from '../stores/useToastStore'
import clsx from 'clsx'

export function ToastHost() {
  const { toasts, remove } = useToastStore()

  return (
    <div className="fixed right-6 top-6 z-50 flex w-[560px] max-w-[calc(100vw-3rem)] flex-col gap-3">
      <AnimatePresence initial={false}>
        {toasts.map((t) => (
          <motion.button
            key={t.id}
            type="button"
            onClick={() => remove(t.id)}
            initial={{ opacity: 0, y: -16 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -16 }}
            className={clsx(
              'min-h-16 rounded-2xl border px-5 py-4 text-left text-[24px] leading-snug shadow-lg',
              t.kind === 'error' && 'border-rose-400/40 bg-rose-950/60 text-rose-50',
              t.kind === 'success' && 'border-emerald-400/40 bg-emerald-950/60 text-emerald-50',
              t.kind === 'info' && 'border-sky-400/40 bg-sky-950/60 text-sky-50',
            )}
          >
            {t.message}
          </motion.button>
        ))}
      </AnimatePresence>
    </div>
  )
}
