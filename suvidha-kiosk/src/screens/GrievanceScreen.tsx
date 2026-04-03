import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import type { ApiError, GrievanceStatus, UtilityType } from '../api/types'
import { KioskButton } from '../components/KioskButton'
import { EmptyState } from '../components/EmptyState'
import { Skeleton } from '../components/Skeleton'
import { useToastStore } from '../stores/useToastStore'
import { descriptionSchema } from '../validation/schemas'
import { fileToBase64 } from '../utils/files'
import { useGrievanceCategories, useMyGrievances, useSubmitGrievance } from '../hooks/useGrievance'

type Tab = 'SUBMIT' | 'MINE'

const submitSchema = z.object({
  utilityType: z.enum(['ELECTRICITY', 'GAS', 'WATER']),
  category: z.string().min(1),
  description: descriptionSchema,
})

export function GrievanceScreen() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const pushToast = useToastStore((s) => s.push)

  const [tab, setTab] = useState<Tab>('SUBMIT')
  const [attachments, setAttachments] = useState<{ filename: string; base64: string; mime: string; size: number }[]>([])

  const categoriesQ = useGrievanceCategories()
  const submitM = useSubmitGrievance()

  const [filters, setFilters] = useState<{ status?: GrievanceStatus; utilityType?: UtilityType }>({})
  const [page, setPage] = useState(1)
  const mineQ = useMyGrievances({ page, pageSize: 10, ...filters })

  const form = useForm<z.infer<typeof submitSchema>>({
    resolver: zodResolver(submitSchema),
    defaultValues: { utilityType: 'ELECTRICITY', category: '', description: '' },
  })

  const categoryOptions = useMemo(() => {
    const u = form.watch('utilityType')
    const data = categoriesQ.data
    if (!data) return []
    return data[u]
  }, [categoriesQ.data, form])

  const onPickFiles = async (files: FileList | null) => {
    if (!files) return
    const list = Array.from(files)
    const next = [...attachments]
    for (const f of list) {
      if (next.length >= 5) break
      if (!['image/jpeg', 'image/png', 'application/pdf'].includes(f.type)) continue
      if (f.size > 5 * 1024 * 1024) continue
      const base64 = await fileToBase64(f)
      next.push({ filename: f.name, base64, mime: f.type, size: f.size })
    }
    setAttachments(next)
  }

  const onSubmit = form.handleSubmit(async (v) => {
    try {
      const res = await submitM.mutateAsync({
        utilityType: v.utilityType as UtilityType,
        category: v.category,
        description: v.description,
        attachments: attachments.map((a) => ({ filename: a.filename, base64: a.base64 })),
      })
      pushToast({ kind: 'success', message: t('grievance.submitSuccess', { ref: res.referenceNumber }) })
      form.reset({ utilityType: v.utilityType, category: '', description: '' })
      setAttachments([])
      setTab('MINE')
    } catch (e) {
      const err = e as ApiError
      pushToast({ kind: 'error', message: err.error.message })
      if (err.error.fields) {
        for (const [k, msg] of Object.entries(err.error.fields)) {
          form.setError(k as any, { type: 'server', message: msg })
        }
      }
    }
  })

  return (
    <div className="min-h-screen bg-slate-950 px-14 py-10">
      <div className="mx-auto w-full max-w-[1600px]">
        <div className="flex items-center justify-between">
          <div className="text-[40px] font-extrabold">{t('grievance.title')}</div>
          <div className="flex gap-4">
            <KioskButton variant="secondary" onClick={() => navigate('/grievance/track')}>
              {t('grievance.trackLink')}
            </KioskButton>
            <KioskButton variant="secondary" onClick={() => navigate('/dashboard')}>
              {t('common.home')}
            </KioskButton>
          </div>
        </div>

        <div className="mt-8 flex gap-4">
          <button
            type="button"
            onClick={() => setTab('SUBMIT')}
            className={`min-h-16 rounded-2xl border px-6 py-4 text-[26px] font-bold ${
              tab === 'SUBMIT' ? 'border-sky-400/40 bg-sky-500/10' : 'border-white/12 bg-white/5'
            }`}
          >
            {t('grievance.tabs.submit')}
          </button>
          <button
            type="button"
            onClick={() => setTab('MINE')}
            className={`min-h-16 rounded-2xl border px-6 py-4 text-[26px] font-bold ${
              tab === 'MINE' ? 'border-sky-400/40 bg-sky-500/10' : 'border-white/12 bg-white/5'
            }`}
          >
            {t('grievance.tabs.mine')}
          </button>
        </div>

        <div className="mt-10 rounded-3xl border border-white/10 bg-white/5 p-10">
          {tab === 'SUBMIT' ? (
            <div className="grid grid-cols-2 gap-10">
              <div>
                <label className="text-[24px] text-white/75">{t('billing.utilityType')}</label>
                <select
                  {...form.register('utilityType')}
                  className="mt-2 w-full rounded-2xl border border-white/15 bg-white/5 px-5 py-4 text-[26px]"
                >
                  <option value="ELECTRICITY">{t('enum.utilityType.ELECTRICITY')}</option>
                  <option value="GAS">{t('enum.utilityType.GAS')}</option>
                  <option value="WATER">{t('enum.utilityType.WATER')}</option>
                </select>
              </div>
              <div>
                <label className="text-[24px] text-white/75">{t('grievance.category')}</label>
                <select
                  {...form.register('category')}
                  className="mt-2 w-full rounded-2xl border border-white/15 bg-white/5 px-5 py-4 text-[26px]"
                >
                  <option value="">—</option>
                  {categoryOptions.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </div>
              <div className="col-span-2">
                <label className="text-[24px] text-white/75">{t('grievance.description')}</label>
                <textarea
                  {...form.register('description')}
                  rows={6}
                  className="mt-2 w-full rounded-2xl border border-white/15 bg-white/5 px-5 py-4 text-[26px]"
                />
                {form.formState.errors.description?.message ? (
                  <div className="mt-3 text-[24px] text-rose-300">{form.formState.errors.description.message}</div>
                ) : null}
              </div>
              <div className="col-span-2">
                <div className="text-[24px] text-white/75">{t('grievance.attachments')}</div>
                <input
                  type="file"
                  multiple
                  accept="image/jpeg,image/png,application/pdf"
                  onChange={(e) => void onPickFiles(e.target.files)}
                  className="mt-3 block w-full text-[24px]"
                />
                <div className="mt-4 flex flex-wrap gap-3">
                  {attachments.map((a) => (
                    <button
                      key={a.filename}
                      type="button"
                      onClick={() => setAttachments((cur) => cur.filter((x) => x.filename !== a.filename))}
                      className="min-h-16 rounded-2xl border border-white/12 bg-white/5 px-5 py-3 text-[22px] active:bg-white/10"
                    >
                      {a.filename}
                    </button>
                  ))}
                </div>
              </div>
              <div className="col-span-2">
                <KioskButton disabled={submitM.isPending} onClick={onSubmit}>
                  {t('common.submit')}
                </KioskButton>
              </div>
            </div>
          ) : null}

          {tab === 'MINE' ? (
            <div>
              <div className="flex items-end justify-between gap-8">
                <div className="flex gap-4">
                  <select
                    value={filters.status ?? ''}
                    onChange={(e) => setFilters((s) => ({ ...s, status: (e.target.value || undefined) as any }))}
                    className="min-h-16 rounded-2xl border border-white/15 bg-white/5 px-5 py-4 text-[24px]"
                  >
                    <option value="">{t('admin.filters.status')}</option>
                    <option value="SUBMITTED">{t('enum.grievanceStatus.SUBMITTED')}</option>
                    <option value="IN_PROGRESS">{t('enum.grievanceStatus.IN_PROGRESS')}</option>
                    <option value="RESOLVED">{t('enum.grievanceStatus.RESOLVED')}</option>
                    <option value="CLOSED">{t('enum.grievanceStatus.CLOSED')}</option>
                  </select>
                  <select
                    value={filters.utilityType ?? ''}
                    onChange={(e) => setFilters((s) => ({ ...s, utilityType: (e.target.value || undefined) as any }))}
                    className="min-h-16 rounded-2xl border border-white/15 bg-white/5 px-5 py-4 text-[24px]"
                  >
                    <option value="">{t('admin.filters.utility')}</option>
                    <option value="ELECTRICITY">{t('enum.utilityType.ELECTRICITY')}</option>
                    <option value="GAS">{t('enum.utilityType.GAS')}</option>
                    <option value="WATER">{t('enum.utilityType.WATER')}</option>
                  </select>
                </div>
                <div className="flex gap-3">
                  <KioskButton variant="secondary" onClick={() => setPage((p) => Math.max(1, p - 1))}>
                    {t('pagination.prev')}
                  </KioskButton>
                  <KioskButton variant="secondary" onClick={() => setPage((p) => p + 1)}>
                    {t('pagination.next')}
                  </KioskButton>
                </div>
              </div>

              {mineQ.isLoading ? (
                <div className="mt-8 grid grid-cols-2 gap-6">
                  <Skeleton className="h-40" />
                  <Skeleton className="h-40" />
                </div>
              ) : mineQ.isError ? (
                <div className="mt-8">
                  <EmptyState title={t('common.error')} actionLabel={t('common.retry')} onAction={() => mineQ.refetch()} />
                </div>
              ) : (mineQ.data?.items?.length ?? 0) === 0 ? (
                <div className="mt-8">
                  <EmptyState title={t('grievance.noGrievances')} />
                </div>
              ) : (
                <div className="mt-8 grid grid-cols-2 gap-6">
                  {(mineQ.data?.items ?? []).map((g) => (
                    <div key={g.id} className="rounded-3xl border border-white/12 bg-white/5 p-7">
                      <div className="flex items-center justify-between">
                        <div className="text-[26px] font-bold">{g.referenceNumber}</div>
                        <div className="rounded-full border border-white/15 bg-white/5 px-4 py-2 text-[22px] font-bold">
                          {t(`enum.grievanceStatus.${g.status}`)}
                        </div>
                      </div>
                      <div className="mt-2 text-[24px] text-white/70">{t(`enum.utilityType.${g.utilityType}`)} · {g.category}</div>
                      <div className="mt-4 text-[24px] text-white/85 line-clamp-3">{g.description}</div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ) : null}
        </div>
      </div>
    </div>
  )
}
