"use client";

import { useState, useRef, useCallback, type ClipboardEvent, type KeyboardEvent } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { motion, AnimatePresence } from "framer-motion";
import { ArrowLeft, Smartphone, ShieldCheck, CheckCircle, AlertCircle, Globe } from "lucide-react";
import { Button, Select } from "@suvidha/ui";
import { cn, maskMobile } from "@/lib/utils";
import { useOtp } from "@/hooks/auth/useOtp";
import { toast } from "sonner";
import { OTP_LENGTH } from "@/lib/constants";
import { LanguageSwitcher } from "@/components/shared/LanguageSwitcher";

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.06, delayChildren: 0.1 },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 16 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.35, ease: "easeOut" } },
};

export default function LoginPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const redirectPath = searchParams.get("redirect");

  const {
    phone,
    step,
    sendOtp,
    verifyOtp,
    reset,
    countdown,
  } = useOtp();

  const [phoneInput, setPhoneInput] = useState("");
  const [otpValues, setOtpValues] = useState<string[]>(Array(OTP_LENGTH).fill(""));
  const [otpError, setOtpError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const otpRefs = useRef<(HTMLInputElement | null)[]>([]);

  const phoneValid = /^[0-9]{10}$/.test(phoneInput);
  const otpFilled = otpValues.every((d) => d !== "");

  const handlePhoneSubmit = async () => {
    if (!phoneValid || isSubmitting) return;
    setIsSubmitting(true);
    try {
      await sendOtp(phoneInput);
      otpRefs.current[0]?.focus();
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : typeof err === "object" && err !== null && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message ?? "Failed to send OTP"
          : "Failed to send OTP";
      toast.error(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleOtpChange = (index: number, value: string) => {
    if (!/^\d*$/.test(value)) return;
    setOtpError("");
    const digit = value.slice(-1);
    const next = [...otpValues];
    next[index] = digit;
    setOtpValues(next);
    if (digit && index < OTP_LENGTH - 1) {
      otpRefs.current[index + 1]?.focus();
    }
  };

  const handleOtpKeyDown = (index: number, e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Backspace" && !otpValues[index] && index > 0) {
      otpRefs.current[index - 1]?.focus();
    }
  };

  const handleOtpPaste = (e: ClipboardEvent<HTMLInputElement>) => {
    e.preventDefault();
    const text = e.clipboardData.getData("text").replace(/\D/g, "").slice(0, OTP_LENGTH);
    if (!text) return;
    const next = [...otpValues];
    for (let i = 0; i < OTP_LENGTH; i++) {
      next[i] = text[i] ?? "";
    }
    setOtpValues(next);
    setOtpError("");
    const focusIdx = Math.min(text.length, OTP_LENGTH - 1);
    otpRefs.current[focusIdx]?.focus();
  };

  const handleVerifyOtp = useCallback(async () => {
    if (!otpFilled || isSubmitting) return;
    setIsSubmitting(true);
    setOtpError("");
    try {
      const response = await verifyOtp(otpValues.join(""));
      const isNewUser = response.isNewUser;
      toast.success(isNewUser ? "Account found! Complete your profile" : "Welcome back!");
      if (isNewUser) {
        router.push("/register");
      } else {
        router.push(redirectPath ?? "/dashboard");
      }
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : typeof err === "object" && err !== null && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message ?? "Invalid OTP"
          : "Invalid OTP";
      setOtpError(msg);
      toast.error(msg);
      setOtpValues(Array(OTP_LENGTH).fill(""));
      otpRefs.current[0]?.focus();
    } finally {
      setIsSubmitting(false);
    }
  }, [otpFilled, isSubmitting, verifyOtp, otpValues, redirectPath, router]);

  const handleBackToPhone = () => {
    reset();
    setPhoneInput("");
    setOtpValues(Array(OTP_LENGTH).fill(""));
    setOtpError("");
  };

  const handleResend = async () => {
    if (countdown > 0 || isSubmitting) return;
    setOtpValues(Array(OTP_LENGTH).fill(""));
    setOtpError("");
    try {
      await sendOtp(phone);
      toast.success("OTP resent successfully");
      otpRefs.current[0]?.focus();
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : "Failed to resend OTP";
      toast.error(msg);
    }
  };

  return (
    <div className="flex min-h-screen">
      {/* Left Brand Section */}
      <div className="hidden lg:flex lg:w-1/2 relative bg-gradient-to-br from-brand-600 to-brand-900 p-12 flex-col justify-between">
        <div className="space-y-2">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/20 backdrop-blur text-white font-bold text-lg">
              S
            </div>
            <span className="text-2xl font-display font-bold text-white">Suvidha</span>
          </div>
          <p className="text-white/70 text-sm font-medium tracking-wide">
            Your Gateway to Citizen Services
          </p>
        </div>
        <div className="space-y-6 max-w-md">
          <div className="space-y-3">
            <div className="flex items-center gap-3 text-white/80">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-white/10">
                <Smartphone className="h-5 w-5" />
              </div>
              <p className="text-sm">Quick OTP-based login with your mobile number</p>
            </div>
            <div className="flex items-center gap-3 text-white/80">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-white/10">
                <ShieldCheck className="h-5 w-5" />
              </div>
              <p className="text-sm">Secure &amp; encrypted — your data stays private</p>
            </div>
            <div className="flex items-center gap-3 text-white/80">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-white/10">
                <CheckCircle className="h-5 w-5" />
              </div>
              <p className="text-sm">Pay bills, track grievances &amp; manage connections</p>
            </div>
          </div>
        </div>
        <div className="text-white/40 text-xs">
          &copy; {new Date().getFullYear()} Suvidha. All rights reserved.
        </div>
      </div>

      {/* Right Form Section */}
      <div className="flex-1 flex items-center justify-center px-4 sm:px-6 lg:px-8 bg-gray-50 dark:bg-gray-950 relative">
        {/* Language Selector */}
        <div className="absolute top-4 right-4 sm:top-6 sm:right-6">
          <LanguageSwitcher />
        </div>
        <div className="w-full max-w-sm">
          {redirectPath && (
            <motion.div
              initial={{ opacity: 0, y: -8 }}
              animate={{ opacity: 1, y: 0 }}
              className="mb-6 flex items-center gap-2 rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 px-4 py-3 text-sm text-amber-800 dark:text-amber-300"
            >
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>Please sign in to access that page</span>
            </motion.div>
          )}

          <AnimatePresence mode="wait">
            {step === "phone" ? (
              <motion.div
                key="phone"
                variants={containerVariants}
                initial="hidden"
                animate="visible"
                exit="exit"
                className="space-y-6"
              >
                <motion.div variants={itemVariants} className="space-y-1.5">
                  <h1 className="text-2xl font-display font-bold text-gray-900 dark:text-gray-100">
                    Sign In
                  </h1>
                  <p className="text-sm text-muted dark:text-muted-dark">
                    Enter your registered mobile number
                  </p>
                </motion.div>

                <motion.div variants={itemVariants} className="space-y-2">
                  <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
                    Mobile Number
                  </label>
                  <div className="relative">
                    <span className="absolute left-3 top-1/2 -translate-y-1/2 text-sm font-medium text-gray-700 dark:text-gray-300 pointer-events-none z-10">
                      +91
                    </span>
                    <input
                      type="tel"
                      inputMode="numeric"
                      pattern="[0-9]{10}"
                      maxLength={10}
                      value={phoneInput}
                      onChange={(e) => setPhoneInput(e.target.value.replace(/\D/g, "").slice(0, 10))}
                      onKeyDown={(e) => {
                        if (e.key === "Enter" && phoneValid) handlePhoneSubmit();
                      }}
                      placeholder="Enter 10-digit number"
                      className="flex h-12 w-full rounded-xl border border-border bg-white dark:bg-surface-dark pl-14 pr-4 text-base tracking-wider placeholder:text-muted dark:placeholder:text-muted-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                      autoComplete="tel"
                      aria-label="Mobile number"
                    />
                  </div>
                </motion.div>

                <motion.div variants={itemVariants}>
                  <Button
                    className="w-full h-12 text-base rounded-xl"
                    disabled={!phoneValid}
                    loading={isSubmitting}
                    onClick={handlePhoneSubmit}
                  >
                    Send OTP
                  </Button>
                </motion.div>

                <motion.p variants={itemVariants} className="text-center text-xs text-muted dark:text-muted-dark">
                  By continuing, you agree to our{" "}
                  <a href="/terms" className="text-brand-500 hover:underline">Terms</a>{" "}
                  and{" "}
                  <a href="/privacy" className="text-brand-500 hover:underline">Privacy Policy</a>
                </motion.p>
              </motion.div>
            ) : (
              <motion.div
                key="otp"
                variants={containerVariants}
                initial="hidden"
                animate="visible"
                exit="exit"
                className="space-y-6"
              >
                <motion.div variants={itemVariants}>
                  <button
                    type="button"
                    onClick={handleBackToPhone}
                    className="flex items-center gap-1.5 text-sm text-muted dark:text-muted-dark hover:text-gray-900 dark:hover:text-gray-100 transition-colors"
                    aria-label="Back to phone input"
                  >
                    <ArrowLeft className="h-4 w-4" />
                    Back
                  </button>
                </motion.div>

                <motion.div variants={itemVariants} className="space-y-1.5">
                  <h1 className="text-2xl font-display font-bold text-gray-900 dark:text-gray-100">
                    Verify OTP
                  </h1>
                  <p className="text-sm text-muted dark:text-muted-dark">
                    OTP sent to{" "}
                    <span className="font-medium text-gray-700 dark:text-gray-300">
                      {maskMobile(phone)}
                    </span>
                  </p>
                </motion.div>

                <motion.div variants={itemVariants} className="space-y-3">
                  <div className="flex items-center justify-center gap-2 sm:gap-3">
                    {otpValues.map((digit, idx) => (
                      <input
                        key={idx}
                        ref={(el) => { otpRefs.current[idx] = el; }}
                        type="text"
                        inputMode="numeric"
                        maxLength={1}
                        value={digit}
                        onChange={(e) => handleOtpChange(idx, e.target.value)}
                        onKeyDown={(e) => handleOtpKeyDown(idx, e)}
                        onPaste={idx === 0 ? handleOtpPaste : undefined}
                        className={cn(
                          "flex h-14 w-12 sm:h-16 sm:w-14 items-center justify-center rounded-xl border-2 text-center text-xl font-bold transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2",
                          otpError
                            ? "border-danger focus-visible:ring-danger bg-red-50 dark:bg-red-900/10"
                            : digit
                              ? "border-brand-500 bg-brand-50 dark:bg-brand-900/10"
                              : "border-border bg-white dark:bg-surface-dark focus-visible:border-brand-500 focus-visible:ring-brand-500"
                        )}
                        aria-label={`OTP digit ${idx + 1}`}
                        autoComplete="one-time-code"
                      />
                    ))}
                  </div>
                  {otpError && (
                    <p className="text-center text-xs text-danger" role="alert">
                      {otpError}
                    </p>
                  )}
                </motion.div>

                <motion.div variants={itemVariants}>
                  <Button
                    className="w-full h-12 text-base rounded-xl"
                    disabled={!otpFilled}
                    loading={isSubmitting}
                    onClick={handleVerifyOtp}
                  >
                    Verify OTP
                  </Button>
                </motion.div>

                <motion.div variants={itemVariants} className="text-center">
                  {countdown > 0 ? (
                    <p className="text-xs text-muted dark:text-muted-dark">
                      Resend OTP in{" "}
                      <span className="font-mono font-medium text-gray-700 dark:text-gray-300">
                        {countdown}s
                      </span>
                    </p>
                  ) : (
                    <button
                      type="button"
                      onClick={handleResend}
                      className="text-sm font-medium text-brand-500 hover:text-brand-600 transition-colors"
                    >
                      Resend OTP
                    </button>
                  )}
                </motion.div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    </div>
  );
}
