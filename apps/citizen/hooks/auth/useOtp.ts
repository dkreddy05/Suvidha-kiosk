import { useState, useEffect, useCallback } from 'react';
import { useMutation } from '@tanstack/react-query';
import { authApi } from '@/lib/api/auth';
import { useAuthStore } from '@/store/auth.store';
import { OTP_RESEND_SECONDS } from '@/lib/constants';
import { useLogin } from './useAuth';

export function useOtp() {
  const [phone, setPhone] = useState('');
  const [sessionId, setSessionId] = useState('');
  const [step, setStep] = useState<'phone' | 'otp'>('phone');
  const [countdown, setCountdown] = useState(0);

  const isNewUser = useAuthStore((s) => s.isNewUser);
  const loginMutation = useLogin();

  const sendOtpMutation = useMutation({
    mutationFn: (mobile: string) => authApi.sendOtp({ mobile }),
    onSuccess: (data, mobile) => {
      setSessionId(data.sessionId);
      setStep('otp');
      setCountdown(OTP_RESEND_SECONDS);
      if (typeof window !== 'undefined') {
        sessionStorage.setItem('temp_mobile', mobile);
        sessionStorage.setItem('temp_sessionId', data.sessionId);
      }
    },
  });

  useEffect(() => {
    if (countdown <= 0) return;
    const timer = setInterval(() => setCountdown((prev) => prev - 1), 1000);
    return () => clearInterval(timer);
  }, [countdown]);

  const sendOtp = useCallback(
    (mobile: string) => {
      setPhone(mobile);
      return sendOtpMutation.mutateAsync(mobile);
    },
    [sendOtpMutation],
  );

  const verifyOtp = useCallback(
    (otp: string) => loginMutation.mutateAsync({ sessionId, otp }),
    [loginMutation, sessionId],
  );

  const reset = useCallback(() => {
    setPhone('');
    setSessionId('');
    setStep('phone');
    setCountdown(0);
  }, []);

  return {
    phone,
    sessionId,
    step,
    sendOtp,
    verifyOtp,
    reset,
    isNewUser,
    countdown,
  };
}
