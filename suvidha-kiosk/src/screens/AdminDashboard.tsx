import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts'
import type { GrievanceStatus, UtilityType } from '../api/types'
import { KioskButton } from '../components/KioskButton'
import { Skeleton } from '../components/Skeleton'
import { useAdminGrievances, useAnalyticsSummary, useUpdateGrievanceStatus } from '../hooks/useAdmin'
import { useToastStore } from '../stores/useToastStore'
import type { ApiError } from '../api/types'

export function AdminDashboard() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const pushToast = useToastStore((s) => s.push)

  const summaryQ = useAnalyticsSummary()
  const [params, setParams] = useState<{ status?: GrievanceStatus; utilityType?: UtilityType; page: number; pageSize: number }>({
    page: 1,
    pageSize: 20,
  })
  const grievancesQ = useAdminGrievances(params)
  const updateM = useUpdateGrievanceStatus()

  const lineData = useMemo(() => {
    const today = new Date()
    const days = Array.from({ length: 30 }, (_, i) => {
      const d = new Date(today)
      d.setDate(today.getDate() - (29 - i))
      const key = d.toISOString().slice(5, 10)
      return { day: key, count: 0 }
    })
    if (summaryQ.data) days[days.length - 1].count = summaryQ.data.billsPaidToday
    return days
  }, [summaryQ.data])

  const donutData = useMemo(() => {
    const items = grievancesQ.data?.items ?? []
    const counts: Record<string, number> = { ELECTRICITY: 0, GAS: 0, WATER: 0 }
    for (const g of items) counts[g.utilityType] = (counts[g.utilityType] ?? 0) + 1
    return Object.entries(counts).map(([k, v]) => ({ name: k, value: v }))
  }, [grievancesQ.data])

  return (
    <div className="min-h-screen bg-slate-950 px-14 py-10">
      <div className="mx-auto w-full max-w-[1800px]">
        <div className="flex items-center justify-between">
          <div className="text-[40px] font-extrabold">{t('admin.title')}</div>
          <KioskButton variant="secondary" onClick={() => navigate('/dashboard')}>
            {t('common.home')}
          </KioskButton>
        </div>

        <div className="mt-10 grid grid-cols-4 gap-6">
          {summaryQ.isLoading ? (
            Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-36" />)
          ) : summaryQ.isError ? (
            <div className="col-span-4">
              <div className="rounded-3xl border border-white/10 bg-white/5 p-8">
                <KioskButton variant="secondary" onClick={() => summaryQ.refetch()}>
                  {t('common.retry')}
                </KioskButton>
              </div>
            </div>
          ) : (
            <>
              <div className="rounded-3xl border border-white/10 bg-white/5 p-7">
                <div className="text-[22px] text-white/70">{t('admin.citizensToday')}</div>
                <div className="mt-3 text-[44px] font-extrabold">{summaryQ.data?.citizensToday ?? 0}</div>
              </div>
              <div className="rounded-3xl border border-white/10 bg-white/5 p-7">
                <div className="text-[22px] text-white/70">{t('admin.billsPaidToday')}</div>
                <div className="mt-3 text-[44px] font-extrabold">{summaryQ.data?.billsPaidToday ?? 0}</div>
              </div>
              <div className="rounded-3xl border border-white/10 bg-white/5 p-7">
                <div className="text-[22px] text-white/70">{t('admin.grievancesResolvedToday')}</div>
                <div className="mt-3 text-[44px] font-extrabold">{summaryQ.data?.grievancesResolvedToday ?? 0}</div>
              </div>
              <div className="rounded-3xl border border-white/10 bg-white/5 p-7">
                <div className="text-[22px] text-white/70">{t('admin.activeSessions')}</div>
                <div className="mt-3 text-[44px] font-extrabold">{summaryQ.data?.activeSessions ?? 0}</div>
              </div>
            </>
          )}
        </div>

        <div className="mt-10 grid grid-cols-2 gap-10">
          <div className="rounded-3xl border border-white/10 bg-white/5 p-8">
            <div className="text-[28px] font-bold">{t('admin.charts.payments30')}</div>
            <div className="mt-6 h-[320px]">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={lineData}>
                  <XAxis dataKey="day" stroke="rgba(255,255,255,0.5)" />
                  <YAxis stroke="rgba(255,255,255,0.5)" />
                  <Tooltip />
                  <Line type="monotone" dataKey="count" stroke="#38bdf8" strokeWidth={3} dot={false} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="rounded-3xl border border-white/10 bg-white/5 p-8">
            <div className="text-[28px] font-bold">{t('admin.charts.usage')}</div>
            <div className="mt-6 h-[320px]">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={donutData} dataKey="value" nameKey="name" innerRadius={70} outerRadius={120}>
                    {donutData.map((_, i) => (
                      <Cell key={i} fill={['#38bdf8', '#fb923c', '#2dd4bf'][i % 3]} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>

        <div className="mt-10 rounded-3xl border border-white/10 bg-white/5 p-8">
          <div className="flex items-end justify-between gap-6">
            <div className="text-[30px] font-bold">{t('admin.grievances')}</div>
            <div className="flex gap-4">
              <select
                value={params.status ?? ''}
                onChange={(e) => setParams((p) => ({ ...p, status: (e.target.value || undefined) as any, page: 1 }))}
                className="min-h-16 rounded-2xl border border-white/15 bg-white/5 px-5 py-4 text-[24px]"
              >
                <option value="">{t('admin.filters.status')}</option>
                <option value="SUBMITTED">{t('enum.grievanceStatus.SUBMITTED')}</option>
                <option value="IN_PROGRESS">{t('enum.grievanceStatus.IN_PROGRESS')}</option>
                <option value="RESOLVED">{t('enum.grievanceStatus.RESOLVED')}</option>
                <option value="CLOSED">{t('enum.grievanceStatus.CLOSED')}</option>
              </select>
              <select
                value={params.utilityType ?? ''}
                onChange={(e) => setParams((p) => ({ ...p, utilityType: (e.target.value || undefined) as any, page: 1 }))}
                className="min-h-16 rounded-2xl border border-white/15 bg-white/5 px-5 py-4 text-[24px]"
              >
                <option value="">{t('admin.filters.utility')}</option>
                <option value="ELECTRICITY">{t('enum.utilityType.ELECTRICITY')}</option>
                <option value="GAS">{t('enum.utilityType.GAS')}</option>
                <option value="WATER">{t('enum.utilityType.WATER')}</option>
              </select>
              <KioskButton variant="secondary" onClick={() => setParams((p) => ({ ...p, page: Math.max(1, p.page - 1) }))}>
                {t('pagination.prev')}
              </KioskButton>
              <KioskButton variant="secondary" onClick={() => setParams((p) => ({ ...p, page: p.page + 1 }))}>
                {t('pagination.next')}
              </KioskButton>
            </div>
          </div>

          {grievancesQ.isLoading ? (
            <div className="mt-8 grid gap-6">
              <Skeleton className="h-28" />
              <Skeleton className="h-28" />
              <Skeleton className="h-28" />
            </div>
          ) : grievancesQ.isError ? (
            <div className="mt-8">
              <KioskButton variant="secondary" onClick={() => grievancesQ.refetch()}>
                {t('common.retry')}
              </KioskButton>
            </div>
          ) : (
            <div className="mt-8 grid gap-4">
              {(grievancesQ.data?.items ?? []).map((g) => (
                <div key={g.id} className="grid grid-cols-12 items-center gap-4 rounded-3xl border border-white/12 bg-white/5 p-6">
                  <div className="col-span-3">
                    <div className="text-[24px] font-bold">{g.referenceNumber}</div>
                    <div className="text-[22px] text-white/70">{t(`enum.utilityType.${g.utilityType}`)}</div>
                  </div>
                  <div className="col-span-4 text-[22px] text-white/80">{g.category}</div>
                  <div className="col-span-2">
                    <div className="rounded-full border border-white/15 bg-white/5 px-4 py-2 text-center text-[22px] font-bold">
                      {t(`enum.grievanceStatus.${g.status}`)}
                    </div>
                  </div>
                  <div className="col-span-3 flex gap-3">
                    <select
                      defaultValue={g.status}
                      className="min-h-16 flex-1 rounded-2xl border border-white/15 bg-white/5 px-4 py-4 text-[22px]"
                      onChange={async (e) => {
                        const status = e.target.value as GrievanceStatus
                        const note = window.prompt(t('admin.note')) ?? ''
                        try {
                          await updateM.mutateAsync({ id: g.id, status, note })
                        } catch (err) {
                          const ae = err as ApiError
                          pushToast({ kind: 'error', message: ae.error.message })
                        }
                      }}
                    >
                      <option value="SUBMITTED">{t('enum.grievanceStatus.SUBMITTED')}</option>
                      <option value="IN_PROGRESS">{t('enum.grievanceStatus.IN_PROGRESS')}</option>
                      <option value="RESOLVED">{t('enum.grievanceStatus.RESOLVED')}</option>
                      <option value="CLOSED">{t('enum.grievanceStatus.CLOSED')}</option>
                    </select>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
