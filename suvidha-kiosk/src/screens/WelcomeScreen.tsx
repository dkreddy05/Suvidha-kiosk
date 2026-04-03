import { useEffect, useMemo, useState } from 'react'
import { motion } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import type { LanguagePref } from '../api/types'
import { useLanguageStore } from '../stores/useLanguageStore'
import i18n from '../i18n'
import { useSessionStore } from '../stores/useSessionStore'

const kioskId = 'SUVIDHA-KIOSK-01'

export function WelcomeScreen() {
  const navigate = useNavigate()
  const { t } = useTranslation()
  const setLang = useLanguageStore((s) => s.setLang)
  const resetSession = useSessionStore((s) => s.resetSession)
  const [now, setNow] = useState(() => new Date())

  useEffect(() => {
    resetSession()
    const timer = window.setInterval(() => setNow(new Date()), 1000)
    return () => window.clearInterval(timer)
  }, [resetSession])

  const dateStr = useMemo(
    () => new Intl.DateTimeFormat(undefined, { dateStyle: 'full' }).format(now),
    [now],
  )
  const timeStr = useMemo(
    () => new Intl.DateTimeFormat(undefined, { timeStyle: 'medium' }).format(now),
    [now],
  )

  const onPick = async (lang: LanguagePref) => {
    setLang(lang)
    await i18n.changeLanguage(lang)
    navigate('/auth')
  }

  const fixed = (lng: LanguagePref) => i18n.getFixedT(lng)

  const cardVariants = {
    hidden: { opacity: 0, y: 18 },
    show: (i: number) => ({ opacity: 1, y: 0, transition: { delay: i * 0.08 } }),
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-950 via-slate-950 to-slate-900 px-14 py-12">
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-6">
          <div className="rounded-2xl border border-white/15 bg-white/5 px-6 py-4 text-[28px] font-bold">
            C-DAC / NIC
          </div>
          <div>
            <div className="text-[44px] font-extrabold tracking-tight">SUVIDHA</div>
            <div className="text-[26px] text-white/70">{t('welcome.branding')}</div>
          </div>
        </div>
        <div className="text-right text-[26px] text-white/70">{t('common.kioskId', { id: kioskId })}</div>
      </div>

      <motion.div
        initial="hidden"
        animate="show"
        className="mt-12 grid grid-cols-2 gap-10"
      >
        {(['en', 'hi', 'te', 'ta'] as LanguagePref[]).map((lng, i) => (
          <motion.button
            key={lng}
            custom={i}
            variants={cardVariants}
            type="button"
            onClick={() => onPick(lng)}
            className="kiosk-ripple min-h-[220px] rounded-3xl border border-white/15 bg-white/5 px-10 py-10 text-left active:bg-white/10"
          >
            <div className="text-[40px] font-extrabold">{t(`welcome.languages.${lng}`)}</div>
            <div className="mt-4 text-[28px] text-white/70">{t('welcome.selectLanguage')}</div>
          </motion.button>
        ))}
      </motion.div>

      <div className="mt-12 rounded-3xl border border-white/10 bg-white/5 p-10">
        <div className="grid grid-cols-2 gap-6 text-[28px] text-white/85">
          <div>{fixed('en')('welcome.instructionAll.en')}</div>
          <div>{fixed('hi')('welcome.instructionAll.hi')}</div>
          <div>{fixed('te')('welcome.instructionAll.te')}</div>
          <div>{fixed('ta')('welcome.instructionAll.ta')}</div>
        </div>
      </div>

      <div className="mt-10 flex items-center justify-between rounded-2xl border border-white/10 bg-white/5 px-8 py-6 text-[26px] text-white/75">
        <div>{t('common.dateTime', { date: dateStr, time: timeStr })}</div>
        <div className="text-white/60">{kioskId}</div>
      </div>
    </div>
  )
}
