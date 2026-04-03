import { AnimatePresence, motion } from 'framer-motion'
import { useTranslation } from 'react-i18next'
import { KioskButton } from './KioskButton'

export function IdleWarningModal({
  open,
  time,
  onContinue,
  onEnd,
}: {
  open: boolean
  time: string
  onContinue: () => void
  onEnd: () => void
}) {
  const { t } = useTranslation()

  return (
    <AnimatePresence>
      {open ? (
        <motion.div
          className="fixed inset-0 z-40 flex items-center justify-center bg-black/60 p-10"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          <motion.div
            initial={{ scale: 0.98, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0.98, opacity: 0 }}
            className="w-full max-w-[860px] rounded-3xl border border-white/15 bg-slate-900 p-10"
          >
            <div className="text-[36px] font-bold">{t('idle.title')}</div>
            <div className="mt-4 text-[28px] text-white/85">{t('idle.body', { time })}</div>
            <div className="mt-10 flex gap-6">
              <KioskButton className="flex-1" onClick={onContinue}>
                {t('common.continue')}
              </KioskButton>
              <KioskButton className="flex-1" variant="danger" onClick={onEnd}>
                {t('common.endSession')}
              </KioskButton>
            </div>
          </motion.div>
        </motion.div>
      ) : null}
    </AnimatePresence>
  )
}
