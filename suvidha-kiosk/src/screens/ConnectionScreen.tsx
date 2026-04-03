import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import type { ApiError } from '../api/types'
import { KioskButton } from '../components/KioskButton'
import { EmptyState } from '../components/EmptyState'
import { Skeleton } from '../components/Skeleton'
import { useToastStore } from '../stores/useToastStore'
import { fileToBase64 } from '../utils/files'
import { useMyConnectionRequests, useRequestConnection } from '../hooks/useConnections'

const requestSchema = z.object({
  requestType: z.enum(['NEW', 'TRANSFER', 'SURRENDER']),
  utilityType: z.enum(['ELECTRICITY', 'GAS', 'WATER']),
  address: z.string().min(1),
})

export function ConnectionScreen() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const pushToast = useToastStore((s) => s.push)

  const requestM = useRequestConnection()
  const [docBase64, setDocBase64] = useState<string | null>(null)

  const [page, setPage] = useState(1)
  const mineQ = useMyConnectionRequests({ page, pageSize: 10 })

  const form = useForm<z.infer<typeof requestSchema>>({
    resolver: zodResolver(requestSchema),
    defaultValues: { requestType: 'NEW', utilityType: 'ELECTRICITY', address: '' },
  })

  const onPickDoc = async (file: File | null) => {
    if (!file) return
    const base64 = await fileToBase64(file)
    setDocBase64(base64)
  }

  const onSubmit = form.handleSubmit(async (v) => {
    try {
      const res = await requestM.mutateAsync({
        requestType: v.requestType,
        utilityType: v.utilityType,
        address: v.address,
        documents: docBase64 ? [{ type: 'ID_PROOF', base64: docBase64 }] : [],
      })
      pushToast({ kind: 'success', message: res.referenceNumber })
      form.reset({ ...v, address: '' })
      setDocBase64(null)
      mineQ.refetch()
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
          <div className="text-[40px] font-extrabold">{t('connections.title')}</div>
          <KioskButton variant="secondary" onClick={() => navigate('/dashboard')}>
            {t('common.home')}
          </KioskButton>
        </div>

        <div className="mt-10 grid grid-cols-2 gap-10">
          <div className="rounded-3xl border border-white/10 bg-white/5 p-10">
            <div className="grid grid-cols-2 gap-6">
              <div>
                <label className="text-[24px] text-white/75">{t('connections.requestType')}</label>
                <select
                  {...form.register('requestType')}
                  className="mt-2 w-full rounded-2xl border border-white/15 bg-white/5 px-5 py-4 text-[26px]"
                >
                  <option value="NEW">{t('enum.requestType.NEW')}</option>
                  <option value="TRANSFER">{t('enum.requestType.TRANSFER')}</option>
                  <option value="SURRENDER">{t('enum.requestType.SURRENDER')}</option>
                </select>
              </div>
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
              <div className="col-span-2">
                <label className="text-[24px] text-white/75">{t('billing.address')}</label>
                <textarea
                  {...form.register('address')}
                  rows={5}
                  className="mt-2 w-full rounded-2xl border border-white/15 bg-white/5 px-5 py-4 text-[26px]"
                />
              </div>
              <div className="col-span-2">
                <div className="text-[24px] text-white/75">{t('connections.documents')}</div>
                <input
                  type="file"
                  accept="image/jpeg,image/png,application/pdf"
                  onChange={(e) => void onPickDoc(e.target.files?.[0] ?? null)}
                  className="mt-3 block w-full text-[24px]"
                />
                {docBase64 ? (
                  <div className="mt-3 text-[22px] text-emerald-200">{t('connections.docOk')}</div>
                ) : null}
              </div>
            </div>
            <div className="mt-8">
              <KioskButton disabled={requestM.isPending} onClick={onSubmit}>
                {t('connections.submitRequest')}
              </KioskButton>
            </div>
          </div>

          <div className="rounded-3xl border border-white/10 bg-white/5 p-10">
            <div className="flex items-center justify-between">
              <div className="text-[30px] font-bold">{t('connections.myRequests')}</div>
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
              <div className="mt-8 grid gap-6">
                <Skeleton className="h-32" />
                <Skeleton className="h-32" />
              </div>
            ) : mineQ.isError ? (
              <div className="mt-8">
                <EmptyState title={t('common.error')} actionLabel={t('common.retry')} onAction={() => mineQ.refetch()} />
              </div>
            ) : (mineQ.data?.items?.length ?? 0) === 0 ? (
              <div className="mt-8">
                <EmptyState title={t('connections.noRequests')} />
              </div>
            ) : (
              <div className="mt-8 flex flex-col gap-6">
                {(mineQ.data?.items ?? []).map((r) => (
                  <div key={r.id} className="rounded-3xl border border-white/12 bg-white/5 p-7">
                    <div className="flex items-center justify-between">
                      <div className="text-[26px] font-bold">{r.referenceNumber}</div>
                      <div className="rounded-full border border-white/15 bg-white/5 px-4 py-2 text-[22px] font-bold">
                        {t(`enum.connectionStatus.${r.status}`)}
                      </div>
                    </div>
                    <div className="mt-2 text-[24px] text-white/70">{t(`enum.utilityType.${r.utilityType}`)} · {t(`enum.requestType.${r.requestType}`)}</div>
                    <div className="mt-3 text-[24px] text-white/85 line-clamp-2">{r.address}</div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
