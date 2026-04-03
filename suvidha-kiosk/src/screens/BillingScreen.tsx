import { useEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { v4 as uuidv4 } from 'uuid'
import type { ApiError, PaymentMode, UtilityAccountDTO } from '../api/types'
import { KioskButton } from '../components/KioskButton'
import { EmptyState } from '../components/EmptyState'
import { Skeleton } from '../components/Skeleton'
import { useToastStore } from '../stores/useToastStore'
import { useAuthStore } from '../stores/useAuthStore'
import { billIdsSchema, paymentModeSchema } from '../validation/schemas'
import { useAccounts, useBills, useConfirmPayment, useLinkAccount, usePayBill, useReceipt } from '../hooks/useBilling'
import { formatInr } from '../utils/format'

type Step = 1 | 2 | 3 | 4 | 5

const linkAccountSchema = z.object({
  accountNumber: z.string().min(1),
  utilityType: z.enum(['ELECTRICITY', 'GAS', 'WATER']),
  providerName: z.string().min(1),
  address: z.string().min(1),
})

const paySchema = z.object({
  billIds: billIdsSchema,
  paymentMode: paymentModeSchema,
})

export function BillingScreen() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const pushToast = useToastStore((s) => s.push)
  const mobileRaw = useAuthStore((s) => s.mobileRaw)

  const [step, setStep] = useState<Step>(1)
  const [selectedAccount, setSelectedAccount] = useState<UtilityAccountDTO | null>(null)
  const [selectedBillIds, setSelectedBillIds] = useState<string[]>([])
  const [paymentMode, setPaymentMode] = useState<PaymentMode>('UPI')
  const [order, setOrder] = useState<{ orderId: string; qrCodeData?: string; upiId?: string } | null>(null)
  const [confirmFields, setConfirmFields] = useState<{ paymentId: string; signature: string }>({ paymentId: '', signature: '' })
  const [confirmedPaymentId, setConfirmedPaymentId] = useState<string | null>(null)
  const [polling, setPolling] = useState(false)
  const idempotencyKeyRef = useRef<string>('')

  const accountsQ = useAccounts(mobileRaw)
  const billsQ = useBills(selectedAccount?.id ?? null)
  const payM = usePayBill()
  const confirmM = useConfirmPayment()
  const receiptQ = useReceipt(confirmedPaymentId)
  const linkM = useLinkAccount()

  const linkForm = useForm<z.infer<typeof linkAccountSchema>>({
    resolver: zodResolver(linkAccountSchema),
    defaultValues: { accountNumber: '', utilityType: 'ELECTRICITY', providerName: '', address: '' },
  })

  const total = useMemo(() => {
    const map = new Map((billsQ.data ?? []).map((b) => [b.id, b.amount]))
    return selectedBillIds.reduce((sum, id) => sum + (map.get(id) ?? 0), 0)
  }, [billsQ.data, selectedBillIds])

  const steps = [t('billing.steps.accounts'), t('billing.steps.bills'), t('billing.steps.method'), t('billing.steps.processing'), t('billing.steps.receipt')]

  const onNextFromBills = () => {
    try {
      paySchema.parse({ billIds: selectedBillIds, paymentMode })
      setStep(3)
    } catch {
      pushToast({ kind: 'error', message: t('billing.selectBills') })
    }
  }

  const onPay = async () => {
    try {
      const res = await payM.mutateAsync({ billIds: selectedBillIds, paymentMode })
      if ('status' in res && res.status === 'PAID') {
        setConfirmedPaymentId('')
        setStep(5)
        return
      }
      if (!('orderId' in res)) {
        pushToast({ kind: 'error', message: t('common.retry') })
        return
      }
      idempotencyKeyRef.current = uuidv4()
      setOrder({ orderId: res.orderId, qrCodeData: res.qrCodeData, upiId: res.upiId })
      setStep(4)
    } catch (e) {
      const err = e as ApiError
      pushToast({ kind: 'error', message: err.error.message })
    }
  }

  useEffect(() => {
    if (!polling) return
    if (!order) return
    if (!confirmFields.paymentId || !confirmFields.signature) return

    const startedAt = Date.now()
    const tick = async () => {
      const elapsed = Date.now() - startedAt
      if (elapsed >= 5 * 60 * 1000) {
        setPolling(false)
        pushToast({ kind: 'error', message: t('billing.processingTimeout') })
        return
      }

      try {
        const res = await confirmM.mutateAsync({
          orderId: order.orderId,
          paymentId: confirmFields.paymentId,
          signature: confirmFields.signature,
          idempotencyKey: idempotencyKeyRef.current,
        })
        setConfirmedPaymentId(res.paymentId)
        setPolling(false)
        setStep(5)
      } catch (e) {
        const err = e as any
        const status = err?._httpStatus
        // Keep polling on conflicts/not-ready; stop on server errors.
        if (status && status >= 500) {
          setPolling(false)
          pushToast({ kind: 'error', message: (e as ApiError).error?.message ?? t('common.error') })
        }
      }
    }

    const interval = window.setInterval(() => void tick(), 3000)
    void tick()
    return () => window.clearInterval(interval)
  }, [polling, order, confirmFields.paymentId, confirmFields.signature, confirmM, pushToast, t])

  const onConfirm = async () => {
    if (!order) return
    setPolling(true)
  }

  return (
    <div className="min-h-screen bg-slate-950 px-14 py-10">
      <div className="mx-auto w-full max-w-[1600px]">
        <div className="flex items-center justify-between">
          <div>
            <div className="text-[40px] font-extrabold">{t('billing.title')}</div>
            <div className="mt-2 flex gap-3 text-[22px] text-white/70">
              {steps.map((s, idx) => (
                <div
                  key={s}
                  className={`rounded-xl border px-4 py-2 ${
                    idx + 1 === step ? 'border-sky-400/40 bg-sky-500/15 text-sky-50' : 'border-white/10 bg-white/5'
                  }`}
                >
                  {idx + 1}. {s}
                </div>
              ))}
            </div>
          </div>
          <div className="flex gap-4">
            <KioskButton variant="secondary" onClick={() => navigate('/dashboard')}>
              {t('common.home')}
            </KioskButton>
          </div>
        </div>

        <div className="mt-10 rounded-3xl border border-white/10 bg-white/5 p-10">
          {step === 1 ? (
            <div>
              {accountsQ.isLoading ? (
                <div className="grid grid-cols-3 gap-6">
                  <Skeleton className="h-48" />
                  <Skeleton className="h-48" />
                  <Skeleton className="h-48" />
                </div>
              ) : accountsQ.isError ? (
                <EmptyState title={t('common.error')} actionLabel={t('common.retry')} onAction={() => accountsQ.refetch()} />
              ) : (accountsQ.data?.length ?? 0) === 0 ? (
                <EmptyState
                  title={t('billing.noAccounts')}
                  actionLabel={t('billing.addAccount')}
                  onAction={() => linkForm.reset()}
                />
              ) : (
                <div className="grid grid-cols-3 gap-6">
                  {(accountsQ.data ?? []).map((a) => (
                    <button
                      key={a.id}
                      type="button"
                      onClick={() => {
                        setSelectedAccount(a)
                        setSelectedBillIds([])
                        setStep(2)
                      }}
                      className="min-h-[200px] rounded-3xl border border-white/12 bg-white/5 p-8 text-left active:bg-white/10"
                    >
                      <div className="text-[26px] font-bold">{t(`enum.utilityType.${a.utilityType}`)}</div>
                      <div className="mt-2 text-[24px] text-white/70">{a.providerName}</div>
                      <div className="mt-4 text-[28px] font-extrabold">{a.accountNumber}</div>
                      {a.latestBill ? (
                        <div className="mt-4 text-[24px] text-white/80">
                          {t('billing.outstanding')}: ₹{formatInr(a.latestBill.amount)} · {t('billing.dueDate')}: {a.latestBill.dueDate}
                        </div>
                      ) : null}
                    </button>
                  ))}
                </div>
              )}

              <div className="mt-10 rounded-3xl border border-white/10 bg-white/5 p-8">
                <div className="text-[28px] font-bold">{t('billing.addAccount')}</div>
                <div className="mt-6 grid grid-cols-2 gap-6">
                  <div>
                    <label className="text-[24px] text-white/75">{t('billing.accountNumber')}</label>
                    <input
                      {...linkForm.register('accountNumber')}
                      className="mt-2 w-full rounded-2xl border border-white/15 bg-white/5 px-5 py-4 text-[26px]"
                    />
                  </div>
                  <div>
                    <label className="text-[24px] text-white/75">{t('billing.utilityType')}</label>
                    <select
                      {...linkForm.register('utilityType')}
                      className="mt-2 w-full rounded-2xl border border-white/15 bg-white/5 px-5 py-4 text-[26px]"
                    >
                      <option value="ELECTRICITY">{t('enum.utilityType.ELECTRICITY')}</option>
                      <option value="GAS">{t('enum.utilityType.GAS')}</option>
                      <option value="WATER">{t('enum.utilityType.WATER')}</option>
                    </select>
                  </div>
                  <div>
                    <label className="text-[24px] text-white/75">{t('billing.providerName')}</label>
                    <input
                      {...linkForm.register('providerName')}
                      className="mt-2 w-full rounded-2xl border border-white/15 bg-white/5 px-5 py-4 text-[26px]"
                    />
                  </div>
                  <div>
                    <label className="text-[24px] text-white/75">{t('billing.address')}</label>
                    <input
                      {...linkForm.register('address')}
                      className="mt-2 w-full rounded-2xl border border-white/15 bg-white/5 px-5 py-4 text-[26px]"
                    />
                  </div>
                </div>
                <div className="mt-6">
                  <KioskButton
                    disabled={linkM.isPending}
                    onClick={linkForm.handleSubmit(async (v) => {
                      try {
                        await linkM.mutateAsync(v)
                        pushToast({ kind: 'success', message: t('common.submit') })
                      } catch (e) {
                        const err = e as ApiError
                        pushToast({ kind: 'error', message: err.error.message })
                      }
                    })}
                  >
                    {t('common.submit')}
                  </KioskButton>
                </div>
              </div>
            </div>
          ) : null}

          {step === 2 ? (
            <div>
              <div className="flex items-center justify-between">
                <div className="text-[30px] font-bold">{t('billing.selectBills')}</div>
                <div className="text-[34px] font-extrabold">{t('billing.total', { amount: formatInr(total) })}</div>
              </div>

              {billsQ.isLoading ? (
                <div className="mt-8 grid grid-cols-2 gap-6">
                  <Skeleton className="h-40" />
                  <Skeleton className="h-40" />
                </div>
              ) : billsQ.isError ? (
                <div className="mt-8">
                  <EmptyState title={t('common.error')} actionLabel={t('common.retry')} onAction={() => billsQ.refetch()} />
                </div>
              ) : (billsQ.data?.length ?? 0) === 0 ? (
                <div className="mt-8">
                  <EmptyState title={t('billing.noBills')} />
                </div>
              ) : (
                <div className="mt-8 grid grid-cols-2 gap-6">
                  {(billsQ.data ?? []).map((b) => {
                    const checked = selectedBillIds.includes(b.id)
                    return (
                      <button
                        key={b.id}
                        type="button"
                        onClick={() => {
                          setSelectedBillIds((cur) => {
                            if (checked) return cur.filter((x) => x !== b.id)
                            if (cur.length >= 10) return cur
                            return [...cur, b.id]
                          })
                        }}
                        className={`min-h-[160px] rounded-3xl border p-7 text-left active:bg-white/10 ${
                          checked ? 'border-sky-400/40 bg-sky-500/10' : 'border-white/12 bg-white/5'
                        }`}
                      >
                        <div className="flex items-center justify-between">
                          <div className="text-[26px] font-bold">{b.billMonth}</div>
                          <div className="text-[26px] font-extrabold">₹{formatInr(b.amount)}</div>
                        </div>
                        <div className="mt-2 text-[24px] text-white/70">
                          {t('billing.dueDate')}: {b.dueDate} · {t(`enum.billStatus.${b.status}`)}
                        </div>
                      </button>
                    )
                  })}
                </div>
              )}

              <div className="mt-10 flex gap-6">
                <KioskButton variant="secondary" onClick={() => setStep(1)}>
                  {t('billing.steps.accounts')}
                </KioskButton>
                <KioskButton disabled={selectedBillIds.length === 0} onClick={onNextFromBills}>
                  {t('billing.steps.method')}
                </KioskButton>
              </div>
            </div>
          ) : null}

          {step === 3 ? (
            <div>
              <div className="text-[30px] font-bold">{t('billing.steps.method')}</div>
              <div className="mt-8 grid grid-cols-3 gap-6">
                {(['UPI', 'CARD', 'CASH'] as PaymentMode[]).map((m) => (
                  <button
                    key={m}
                    type="button"
                    onClick={() => setPaymentMode(m)}
                    className={`min-h-[180px] rounded-3xl border p-8 text-left active:bg-white/10 ${
                      paymentMode === m ? 'border-sky-400/40 bg-sky-500/10' : 'border-white/12 bg-white/5'
                    }`}
                  >
                    <div className="text-[28px] font-extrabold">{t(`billing.paymentMode.${m}`)}</div>
                    <div className="mt-3 text-[24px] text-white/70">{t('billing.total', { amount: formatInr(total) })}</div>
                  </button>
                ))}
              </div>

              <div className="mt-10 flex gap-6">
                <KioskButton variant="secondary" onClick={() => setStep(2)}>
                  {t('billing.steps.bills')}
                </KioskButton>
                <KioskButton disabled={payM.isPending} onClick={onPay}>
                  {t('billing.steps.processing')}
                </KioskButton>
              </div>
            </div>
          ) : null}

          {step === 4 ? (
            <div>
              <div className="flex items-start justify-between gap-10">
                <div>
                  <div className="text-[30px] font-bold">{t('billing.steps.processing')}</div>
                  <div className="mt-3 text-[24px] text-white/70">{t('billing.processingWait')}</div>
                  <div className="mt-3 text-[24px] text-white/70">{order?.orderId}</div>
                  {paymentMode === 'UPI' && order?.qrCodeData ? (
                    <div className="mt-8 rounded-3xl border border-white/10 bg-white/5 p-8">
                      <img alt="UPI QR" src={order.qrCodeData} className="h-[360px] w-[360px] rounded-2xl bg-white p-4" />
                      {order.upiId ? <div className="mt-5 text-[24px]">{t('billing.upiId', { vpa: order.upiId })}</div> : null}
                    </div>
                  ) : null}
                </div>

                <div className="w-full max-w-[560px] rounded-3xl border border-white/10 bg-white/5 p-8">
                  <div className="text-[26px] font-bold">{t('billing.confirmPayment')}</div>
                  <div className="mt-6">
                    <label className="text-[24px] text-white/75">{t('billing.paymentId')}</label>
                    <input
                      value={confirmFields.paymentId}
                      onChange={(e) => setConfirmFields((s) => ({ ...s, paymentId: e.target.value }))}
                      disabled={polling}
                      className="mt-2 w-full rounded-2xl border border-white/15 bg-white/5 px-5 py-4 text-[24px]"
                    />
                  </div>
                  <div className="mt-6">
                    <label className="text-[24px] text-white/75">{t('billing.signature')}</label>
                    <input
                      value={confirmFields.signature}
                      onChange={(e) => setConfirmFields((s) => ({ ...s, signature: e.target.value }))}
                      disabled={polling}
                      className="mt-2 w-full rounded-2xl border border-white/15 bg-white/5 px-5 py-4 text-[24px]"
                    />
                  </div>
                  <div className="mt-8">
                    <KioskButton disabled={confirmM.isPending || polling} onClick={onConfirm} className="w-full">
                      {t('billing.confirmPayment')}
                    </KioskButton>
                  </div>
                </div>
              </div>

              <div className="mt-10">
                <KioskButton variant="secondary" onClick={() => setStep(3)}>
                  {t('billing.steps.method')}
                </KioskButton>
              </div>
            </div>
          ) : null}

          {step === 5 ? (
            <div>
              <div className="text-[34px] font-extrabold">{t('billing.success')}</div>
              {confirmedPaymentId ? (
                <div className="mt-6 rounded-3xl border border-white/10 bg-white/5 p-8">
                  {receiptQ.isLoading ? (
                    <Skeleton className="h-40" />
                  ) : receiptQ.data?.receiptHtml ? (
                    <div className="text-[24px] text-white/80">
                      <KioskButton
                        variant="secondary"
                        onClick={() => {
                          const w = window.open('', '_blank')
                          if (!w) return
                          w.document.write(receiptQ.data!.receiptHtml)
                          w.document.close()
                          w.focus()
                          w.print()
                        }}
                      >
                        {t('common.print')}
                      </KioskButton>
                    </div>
                  ) : null}
                </div>
              ) : null}

              <div className="mt-10">
                <KioskButton onClick={() => navigate('/dashboard', { replace: true })}>{t('common.done')}</KioskButton>
              </div>
            </div>
          ) : null}
        </div>
      </div>
    </div>
  )
}
