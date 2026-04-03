import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { KioskButton } from '../components/KioskButton'

export function StatusTrackingScreen() {
  const { t } = useTranslation()
  const navigate = useNavigate()

  return (
    <div className="min-h-screen bg-slate-950 px-14 py-10">
      <div className="mx-auto w-full max-w-[1200px]">
        <div className="flex items-center justify-between">
          <div className="text-[40px] font-extrabold">{t('status.title')}</div>
          <KioskButton variant="secondary" onClick={() => navigate('/dashboard')}>
            {t('common.home')}
          </KioskButton>
        </div>

        <div className="mt-10 rounded-3xl border border-white/10 bg-white/5 p-10 text-[28px] text-white/80">
          {t('status.note')}
        </div>
      </div>
    </div>
  )
}
