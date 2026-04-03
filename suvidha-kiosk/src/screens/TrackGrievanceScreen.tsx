import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { KioskButton } from '../components/KioskButton'
import { Skeleton } from '../components/Skeleton'
import { EmptyState } from '../components/EmptyState'
import { useTrackGrievance } from '../hooks/useGrievance'
import type { ApiError } from '../api/types'
import { useToastStore } from '../stores/useToastStore'

export function TrackGrievanceScreen() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const pushToast = useToastStore((s) => s.push)

  const [ref, setRef] = useState('')
  const [submitted, setSubmitted] = useState<string | null>(null)
  const q = useTrackGrievance(submitted)

  return (
    <div className="min-h-screen bg-slate-950 px-14 py-10">
      <div className="mx-auto w-full max-w-[1400px]">
        <div className="flex items-center justify-between">
          <div className="text-[40px] font-extrabold">{t('track.title')}</div>
          <KioskButton variant="secondary" onClick={() => navigate('/welcome')}>
            {t('common.home')}
          </KioskButton>
        </div>

        <div className="mt-10 rounded-3xl border border-white/10 bg-white/5 p-10">
          <label className="text-[24px] text-white/75">{t('track.reference')}</label>
          <input
            value={ref}
            onChange={(e) => setRef(e.target.value)}
            className="mt-3 w-full rounded-2xl border border-white/15 bg-white/5 px-6 py-5 text-[28px] font-semibold"
          />
          <div className="mt-6">
            <KioskButton
              onClick={() => {
                setSubmitted(ref.trim() || null)
              }}
            >
              {t('track.lookup')}
            </KioskButton>
          </div>
        </div>

        <div className="mt-10 rounded-3xl border border-white/10 bg-white/5 p-10">
          {q.isLoading ? (
            <Skeleton className="h-40" />
          ) : q.isError ? (
            <EmptyState
              title={t('common.notFound')}
              actionLabel={t('common.retry')}
              onAction={() => {
                const err = q.error as unknown as ApiError
                pushToast({ kind: 'error', message: err.error.message })
                q.refetch()
              }}
            />
          ) : q.data ? (
            <div>
              <div className="flex items-center justify-between">
                <div className="text-[30px] font-bold">{q.data.referenceNumber}</div>
                <div className="rounded-full border border-white/15 bg-white/5 px-4 py-2 text-[22px] font-bold">
                  {t(`enum.grievanceStatus.${q.data.status}`)}
                </div>
              </div>
              <div className="mt-3 text-[24px] text-white/70">
                {t(`enum.utilityType.${q.data.utilityType}`)} · {q.data.category}
              </div>
              <div className="mt-6 text-[26px] text-white/90">{q.data.description}</div>

              <div className="mt-8">
                <div className="text-[26px] font-bold">{t('track.timeline')}</div>
                <div className="mt-4 flex flex-col gap-4">
                  {q.data.timeline.map((e) => (
                    <div key={e.id} className="rounded-2xl border border-white/12 bg-white/5 p-6">
                      <div className="flex items-center justify-between">
                        <div className="text-[24px] font-bold">{t(`enum.grievanceStatus.${e.status}`)}</div>
                        <div className="text-[22px] text-white/60">{e.updatedAt}</div>
                      </div>
                      <div className="mt-2 text-[24px] text-white/80">{e.note}</div>
                      <div className="mt-2 text-[22px] text-white/60">{e.updatedBy}</div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ) : (
            <EmptyState title={t('common.loading')} />
          )}
        </div>
      </div>
    </div>
  )
}
