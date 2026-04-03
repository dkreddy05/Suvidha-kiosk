import { useMemo } from 'react'
import { motion } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import type { LanguagePref } from '../api/types'
import { useAuthStore } from '../stores/useAuthStore'
import { useLanguageStore } from '../stores/useLanguageStore'
import i18n from '../i18n'
import { useIdleSession } from '../hooks/useIdleSession'
import { KioskButton } from '../components/KioskButton'
import { useLogout } from '../hooks/useAuth'
import { useAccounts } from '../hooks/useBilling'
import { useMyGrievances } from '../hooks/useGrievance'
import { formatMmSs } from '../utils/format'

function Tile({
  title,
  color,
  badge,
  onClick,
}: {
  title: string
  color: string
  badge?: number
  onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      onPointerDown={(e) => {
        const r = e.currentTarget.getBoundingClientRect()
        e.currentTarget.style.setProperty('--rx', `${((e.clientX - r.left) / r.width) * 100}%`)
        e.currentTarget.style.setProperty('--ry', `${((e.clientY - r.top) / r.height) * 100}%`)
      }}
      className={`kiosk-ripple relative min-h-[200px] rounded-3xl border border-white/12 bg-white/5 p-8 text-left active:bg-white/10`}
    >
      <div className={`text-[24px] font-semibold ${color}`}>{title}</div>
      {typeof badge === 'number' && badge > 0 ? (
        <div className="absolute right-6 top-6 flex h-12 min-w-12 items-center justify-center rounded-full bg-rose-500 px-3 text-[22px] font-extrabold text-slate-950">
          {badge}
        </div>
      ) : null}
    </button>
  )
}

export function DashboardScreen() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const citizen = useAuthStore((s) => s.citizen)
  const token = useAuthStore((s) => s.token)
  const mobileRaw = useAuthStore((s) => s.mobileRaw)
  const { remainingLabel } = useIdleSession()
  const logout = useLogout()

  const lang = useLanguageStore((s) => s.lang)
  const setLang = useLanguageStore((s) => s.setLang)

  const accountsQ = useAccounts(mobileRaw)
  const grievancesQ = useMyGrievances({ page: 1, pageSize: 10 })

  const pendingBillsCount = useMemo(() => {
    const accts = accountsQ.data ?? []
    return accts.filter((a) => a.latestBill && a.latestBill.status !== 'PAID').length
  }, [accountsQ.data])

  const unresolvedGrievancesCount = useMemo(() => {
    const items = grievancesQ.data?.items ?? []
    return items.filter((g) => g.status !== 'RESOLVED' && g.status !== 'CLOSED').length
  }, [grievancesQ.data])

  const onSwitchLang = async (lng: LanguagePref) => {
    setLang(lng)
    await i18n.changeLanguage(lng)
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className="min-h-screen bg-slate-950 px-14 py-10"
    >
      <div className="sticky top-0 z-10 -mx-14 mb-10 border-b border-white/10 bg-slate-950/90 px-14 py-8 backdrop-blur">
        <div className="flex items-center justify-between gap-8">
          <div className="text-[34px] font-extrabold">
            {t('dashboard.hello', { name: citizen?.name || citizen?.mobile || 'SUVIDHA' })}
          </div>
          <div className="flex items-center gap-4">
            <div className="rounded-2xl border border-white/12 bg-white/5 px-5 py-4 text-[24px] text-white/85">
              {t('common.sessionEndsIn', { time: remainingLabel })}
            </div>
            <div className="grid grid-cols-4 gap-2 rounded-2xl border border-white/12 bg-white/5 p-2">
              {(['en', 'hi', 'te', 'ta'] as LanguagePref[]).map((lng) => (
                <button
                  key={lng}
                  type="button"
                  onClick={() => onSwitchLang(lng)}
                  className={`min-h-16 rounded-xl px-4 text-[22px] font-bold ${
                    lang === lng ? 'bg-sky-500/20 text-sky-100' : 'bg-transparent text-white/80'
                  }`}
                >
                  {t(`welcome.languages.${lng}`)}
                </button>
              ))}
            </div>
            <KioskButton
              variant="secondary"
              disabled={logout.isPending}
              onClick={async () => {
                await logout.mutateAsync().catch(() => {})
                navigate('/welcome', { replace: true })
              }}
            >
              {t('common.logout')}
            </KioskButton>
          </div>
        </div>
      </div>

      <div className="mx-auto w-full max-w-[1600px]">
        <div className="grid grid-cols-4 gap-8">
          <Tile
            title={t('dashboard.tiles.payBills')}
            color="text-sky-200"
            badge={pendingBillsCount}
            onClick={() => navigate('/billing')}
          />
          <Tile
            title={t('dashboard.tiles.newConnection')}
            color="text-teal-200"
            onClick={() => navigate('/connections')}
          />
          <Tile
            title={t('dashboard.tiles.trackComplaint')}
            color="text-slate-200"
            onClick={() => navigate('/grievance/track')}
          />
          <Tile
            title={t('dashboard.tiles.submitComplaint')}
            color="text-orange-200"
            badge={unresolvedGrievancesCount}
            onClick={() => navigate('/grievance')}
          />
          <Tile title={t('dashboard.tiles.viewAccount')} color="text-slate-200" onClick={() => navigate('/billing')} />
          <Tile title={t('dashboard.tiles.downloadReceipt')} color="text-slate-200" onClick={() => navigate('/billing')} />
          <Tile title={t('dashboard.tiles.serviceStatus')} color="text-slate-200" onClick={() => navigate('/status')} />
          <Tile title={t('dashboard.tiles.emergencyInfo')} color="text-rose-200" onClick={() => navigate('/status')} />
        </div>

        <div className="mt-10 flex items-center justify-between rounded-3xl border border-white/10 bg-white/5 px-8 py-6">
          <div className="flex gap-6">
            <KioskButton variant="secondary" onClick={() => navigate('/dashboard')}>
              {t('common.home')}
            </KioskButton>
            <KioskButton variant="secondary" onClick={() => {}}>
              {t('common.help')}
            </KioskButton>
            <KioskButton variant="danger" onClick={() => window.open('tel:112')}
            >
              {t('dashboard.quick.emergency')}
            </KioskButton>
          </div>
          <KioskButton variant="secondary" onClick={() => window.print()}>
            {t('common.print')}
          </KioskButton>
        </div>

        {token ? null : (
          <div className="mt-6 text-[24px] text-white/50">{formatMmSs(0)}</div>
        )}
      </div>
    </motion.div>
  )
}
