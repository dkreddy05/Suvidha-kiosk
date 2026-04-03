import { useEffect, useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import type { ApiError, LanguagePref } from '../api/types'
import { KioskButton } from '../components/KioskButton'
import { useSendOtp, useVerifyOtp } from '../hooks/useAuth'
import { mobileSchema, otpSchema, nameSchema } from '../validation/schemas'
import { useToastStore } from '../stores/useToastStore'
import { useAuthStore } from '../stores/useAuthStore'
import { useLanguageStore } from '../stores/useLanguageStore'
import i18n from '../i18n'

type Step = 'MOBILE' | 'OTP' | 'PROFILE'

const mobileFormSchema = z.object({ mobile: mobileSchema })
const otpFormSchema = z.object({ otp: otpSchema })
const profileSchema = z.object({ name: nameSchema, lang: z.enum(['en', 'hi', 'te', 'ta']) })

function setFieldErrors(err: ApiError, setError: (name: any, error: any) => void) {
  if (!err.error.fields) return
  for (const [k, v] of Object.entries(err.error.fields)) {
    setError(k as any, { type: 'server', message: v })
  }
}

function NumericKeypad({
  onKey,
  onBack,
  onClear,
}: {
  onKey: (d: string) => void
  onBack: () => void
  onClear: () => void
}) {
  const { t } = useTranslation()
  const keys = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '0']
  return (
    <div className="grid grid-cols-3 gap-4">
      {keys.slice(0, 9).map((k) => (
        <button
          key={k}
          type="button"
          onClick={() => onKey(k)}
          className="min-h-16 rounded-2xl border border-white/15 bg-white/5 text-[32px] font-bold active:bg-white/10"
        >
          {k}
        </button>
      ))}
      <button
        type="button"
        onClick={onClear}
        className="min-h-16 rounded-2xl border border-white/15 bg-white/5 text-[26px] font-semibold active:bg-white/10"
      >
        {t('auth.clear')}
      </button>
      <button
        type="button"
        onClick={() => onKey('0')}
        className="min-h-16 rounded-2xl border border-white/15 bg-white/5 text-[32px] font-bold active:bg-white/10"
      >
        0
      </button>
      <button
        type="button"
        onClick={onBack}
        className="min-h-16 rounded-2xl border border-white/15 bg-white/5 text-[26px] font-semibold active:bg-white/10"
      >
        {t('auth.delete')}
      </button>
    </div>
  )
}

function OtpBoxes({ value, onChange }: { value: string; onChange: (v: string) => void }) {
  const digits = Array.from({ length: 6 }, (_, i) => value[i] ?? '')
  return (
    <div className="flex gap-3">
      {digits.map((d, idx) => (
        <input
          key={idx}
          inputMode="numeric"
          pattern="\d*"
          maxLength={1}
          value={d}
          onChange={(e) => {
            const ch = e.target.value.replace(/\D/g, '').slice(-1)
            const next = value.split('')
            next[idx] = ch
            const joined = next.join('').slice(0, 6)
            onChange(joined)
            const el = e.currentTarget
            if (ch && el.nextElementSibling instanceof HTMLInputElement) el.nextElementSibling.focus()
          }}
          className="h-20 w-20 rounded-2xl border border-white/15 bg-white/5 text-center text-[36px] font-bold"
        />
      ))}
    </div>
  )
}

export function AuthScreen() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const pushToast = useToastStore((s) => s.push)
  const setMobileRaw = useAuthStore((s) => s.setMobileRaw)
  const setLang = useLanguageStore((s) => s.setLang)

  const sendOtp = useSendOtp()
  const verifyOtp = useVerifyOtp()

  const [step, setStep] = useState<Step>('MOBILE')
  const [sessionId, setSessionId] = useState<string | null>(null)
  const [devOtp, setDevOtp] = useState<string | null>(null)
  const [attempts, setAttempts] = useState(0)
  const [resendIn, setResendIn] = useState(0)
  const [isNewUser, setIsNewUser] = useState(false)

  const mobileForm = useForm<{ mobile: string }>({
    defaultValues: { mobile: '' },
    resolver: zodResolver(mobileFormSchema),
    mode: 'onSubmit',
  })
  const otpForm = useForm<{ otp: string }>({
    defaultValues: { otp: '' },
    resolver: zodResolver(otpFormSchema),
    mode: 'onSubmit',
  })
  const profileForm = useForm<{ name: string; lang: LanguagePref }>({
    defaultValues: { name: '', lang: 'en' },
    resolver: zodResolver(profileSchema),
    mode: 'onSubmit',
  })

  useEffect(() => {
    if (resendIn <= 0) return
    const tmr = window.setInterval(() => setResendIn((s) => s - 1), 1000)
    return () => window.clearInterval(tmr)
  }, [resendIn])

  const attemptsLeft = useMemo(() => Math.max(0, 3 - attempts), [attempts])

  const mobile = mobileForm.watch('mobile')
  const otp = otpForm.watch('otp')

  const onSend = mobileForm.handleSubmit(async ({ mobile }) => {
    try {
      setMobileRaw(mobile)
      const res = await sendOtp.mutateAsync({ mobile })
      setSessionId(res.sessionId)
      setDevOtp(res.devOtp ?? null)
      setResendIn(60)
      setStep('OTP')
    } catch (e) {
      const err = e as ApiError
      pushToast({ kind: 'error', message: err.error.message })
      setFieldErrors(err, mobileForm.setError)
    }
  })

  const onVerify = otpForm.handleSubmit(async ({ otp }) => {
    if (!sessionId) return
    if (attemptsLeft <= 0) return
    try {
      const res = await verifyOtp.mutateAsync({ sessionId, otp })
      setIsNewUser(res.isNewUser)
      if (res.isNewUser) {
        profileForm.setValue('lang', res.citizen.languagePref)
        setStep('PROFILE')
      } else {
        navigate('/dashboard', { replace: true })
      }
    } catch (e) {
      setAttempts((a) => a + 1)
      const err = e as ApiError
      pushToast({ kind: 'error', message: err.error.message })
      setFieldErrors(err, otpForm.setError)
    }
  })

  const onResend = async () => {
    if (resendIn > 0) return
    try {
      const res = await sendOtp.mutateAsync({ mobile })
      setSessionId(res.sessionId)
      setDevOtp(res.devOtp ?? null)
      setResendIn(60)
    } catch (e) {
      const err = e as ApiError
      pushToast({ kind: 'error', message: err.error.message })
    }
  }

  const onCompleteProfile = profileForm.handleSubmit(async ({ name, lang }) => {
    // NOTE: contract requires this step but no profile-update endpoint is defined.
    // We only apply language selection client-side and proceed.
    setLang(lang)
    await i18n.changeLanguage(lang)
    if (isNewUser) {
      useAuthStore.setState((s) => ({
        citizen: s.citizen ? { ...s.citizen, name, languagePref: lang } : s.citizen,
      }))
    }
    navigate('/dashboard', { replace: true })
  })

  return (
    <div className="min-h-screen bg-slate-950 px-14 py-12">
      <div className="mx-auto w-full max-w-[1200px]">
        <div className="text-[44px] font-extrabold">{t('app.title')}</div>
        <div className="mt-2 text-[28px] text-white/70">
          {step === 'MOBILE' ? t('auth.mobileTitle') : step === 'OTP' ? t('auth.otpTitle') : t('auth.profileTitle')}
        </div>

        <div className="mt-10 rounded-3xl border border-white/10 bg-white/5 p-10">
          {step === 'MOBILE' ? (
            <div className="grid grid-cols-2 gap-10">
              <div>
                <label className="text-[26px] text-white/75">{t('auth.mobile')}</label>
                <input
                  value={mobile}
                  readOnly
                  className="mt-3 w-full rounded-2xl border border-white/15 bg-white/5 px-6 py-5 text-[34px] font-bold tracking-widest"
                />
                {mobileForm.formState.errors.mobile?.message ? (
                  <div className="mt-3 text-[24px] text-rose-300">
                    {mobileForm.formState.errors.mobile.message === 'mobile'
                      ? t('validation.mobile')
                      : mobileForm.formState.errors.mobile.message}
                  </div>
                ) : null}
                <div className="mt-8">
                  <KioskButton disabled={sendOtp.isPending} onClick={onSend} className="w-full">
                    {t('auth.sendOtp')}
                  </KioskButton>
                </div>
              </div>
              <div>
                <NumericKeypad
                  onKey={(d) => mobileForm.setValue('mobile', (mobile + d).slice(0, 10))}
                  onBack={() => mobileForm.setValue('mobile', mobile.slice(0, -1))}
                  onClear={() => mobileForm.setValue('mobile', '')}
                />
              </div>
            </div>
          ) : null}

          {step === 'OTP' ? (
            <div>
              <div className="flex items-center justify-between gap-6">
                <div>
                  <div className="text-[26px] text-white/75">{t('auth.otp')}</div>
                  <div className="mt-4">
                    <OtpBoxes value={otp} onChange={(v) => otpForm.setValue('otp', v)} />
                  </div>
                  {otpForm.formState.errors.otp?.message ? (
                    <div className="mt-3 text-[24px] text-rose-300">
                      {otpForm.formState.errors.otp.message === 'otp'
                        ? t('validation.otp')
                        : otpForm.formState.errors.otp.message}
                    </div>
                  ) : null}
                  <div className="mt-5 text-[24px] text-white/70">{t('auth.attemptsLeft', { count: attemptsLeft })}</div>
                  {import.meta.env.DEV && devOtp ? (
                    <div className="mt-3 text-[24px] text-emerald-200">{t('auth.devOtp', { otp: devOtp })}</div>
                  ) : null}
                </div>
                <div className="min-w-[360px]">
                  <KioskButton disabled={verifyOtp.isPending || attemptsLeft <= 0} onClick={onVerify} className="w-full">
                    {t('auth.verifyOtp')}
                  </KioskButton>
                  <div className="mt-6">
                    <KioskButton
                      variant="secondary"
                      onClick={onResend}
                      disabled={resendIn > 0 || sendOtp.isPending}
                      className="w-full"
                    >
                      {resendIn > 0 ? t('auth.resendOtpIn', { sec: resendIn }) : t('auth.resendOtp')}
                    </KioskButton>
                  </div>
                </div>
              </div>
            </div>
          ) : null}

          {step === 'PROFILE' ? (
            <div className="grid grid-cols-2 gap-10">
              <div>
                <label className="text-[26px] text-white/75">{t('auth.name')}</label>
                <input
                  {...profileForm.register('name')}
                  className="mt-3 w-full rounded-2xl border border-white/15 bg-white/5 px-6 py-5 text-[30px] font-semibold"
                />
                {profileForm.formState.errors.name?.message ? (
                  <div className="mt-3 text-[24px] text-rose-300">{profileForm.formState.errors.name.message}</div>
                ) : null}
              </div>
              <div>
                <label className="text-[26px] text-white/75">{t('auth.confirmLanguage')}</label>
                <div className="mt-3 grid grid-cols-2 gap-4">
                  {(['en', 'hi', 'te', 'ta'] as LanguagePref[]).map((lng) => (
                    <button
                      key={lng}
                      type="button"
                      onClick={() => profileForm.setValue('lang', lng)}
                      className={`min-h-16 rounded-2xl border px-6 py-4 text-[26px] font-semibold active:bg-white/10 ${
                        profileForm.watch('lang') === lng
                          ? 'border-sky-400/50 bg-sky-500/15'
                          : 'border-white/15 bg-white/5'
                      }`}
                    >
                      {t(`welcome.languages.${lng}`)}
                    </button>
                  ))}
                </div>
                <div className="mt-8">
                  <KioskButton disabled={profileForm.formState.isSubmitting} onClick={onCompleteProfile} className="w-full">
                    {t('auth.start')}
                  </KioskButton>
                </div>
              </div>
            </div>
          ) : null}
        </div>
      </div>
    </div>
  )
}
