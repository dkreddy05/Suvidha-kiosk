"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { motion } from "framer-motion";
import { ArrowLeft } from "lucide-react";
import { Button, Input } from "@suvidha/ui";
import { cn } from "@/lib/utils";
import { useRegister } from "@/hooks/auth/useAuth";
import { toast } from "sonner";
import { LANGUAGES } from "@/lib/constants";

const registerSchema = z.object({
  fullName: z
    .string()
    .min(2, "Name must be at least 2 characters")
    .max(100, "Name must not exceed 100 characters"),
  languagePreference: z.enum(["en", "hi", "te", "ta"]),
  aadhaar: z
    .string()
    .length(12, "Aadhaar must be exactly 12 digits")
    .regex(/^\d{12}$/, "Aadhaar must be 12 digits")
    .optional()
    .or(z.literal("")),
});

type RegisterFormValues = z.infer<typeof registerSchema>;

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.07, delayChildren: 0.1 },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 16 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.35, ease: "easeOut" } },
};

export default function RegisterPage() {
  const router = useRouter();
  const registerMutation = useRegister();
  const [mobile, setMobile] = useState("");
  const [sessionId, setSessionId] = useState("");

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      fullName: "",
      languagePreference: "en",
      aadhaar: "",
    },
  });

  const selectedLanguage = watch("languagePreference");

  const [isLoaded, setIsLoaded] = useState(false);

  useEffect(() => {
    if (typeof window !== "undefined") {
      const m = sessionStorage.getItem("temp_mobile") || "";
      const s = sessionStorage.getItem("temp_sessionId") || "";
      if (!m || !s) {
        toast.error("Session expired or invalid. Please login first.");
        router.push("/login");
      } else {
        setMobile(m);
        setSessionId(s);
        setIsLoaded(true);
      }
    }
  }, [router]);

  const onSubmit = async (data: RegisterFormValues) => {
    try {
      await registerMutation.mutateAsync({
        sessionId,
        mobile,
        name: data.fullName.trim(),
        languagePreference: data.languagePreference,
        aadhar: data.aadhaar || undefined,
        role: "USER",
      });
      toast.success("Welcome to Suvidha!");
      router.push("/dashboard");
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : typeof err === "object" && err !== null && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message ?? "Registration failed"
          : "Registration failed";
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
          <blockquote className="text-white/90 text-lg font-display leading-relaxed">
            &ldquo;Complete your profile to unlock all citizen services in one place.&rdquo;
          </blockquote>
          <p className="text-white/60 text-sm">One-time setup — manage everything from here</p>
        </div>
        <div className="text-white/40 text-xs">
          &copy; {new Date().getFullYear()} Suvidha. All rights reserved.
        </div>
      </div>

      {/* Right Form Section */}
      <div className="flex-1 flex items-center justify-center px-4 sm:px-6 lg:px-8 bg-gray-50 dark:bg-gray-950">
        <div className="w-full max-w-sm">
          <motion.div
            variants={containerVariants}
            initial="hidden"
            animate="visible"
            className="space-y-6"
          >
            {/* Progress */}
            <motion.div variants={itemVariants} className="space-y-3">
              <div className="flex items-center justify-between text-xs text-muted dark:text-muted-dark">
                <span>Step 2 of 2</span>
                <span>Profile Setup</span>
              </div>
              <div className="flex h-2 w-full overflow-hidden rounded-full bg-gray-200 dark:bg-gray-800">
                <div
                  className="h-full w-full rounded-full bg-brand-500 transition-all duration-500"
                  role="progressbar"
                  aria-valuenow={100}
                  aria-valuemin={0}
                  aria-valuemax={100}
                />
              </div>
            </motion.div>

            {/* Heading */}
            <motion.div variants={itemVariants} className="space-y-1.5">
              <h1 className="text-2xl font-display font-bold text-gray-900 dark:text-gray-100">
                Complete Your Profile
              </h1>
              <p className="text-sm text-muted dark:text-muted-dark">
                Fill in your details to get started
              </p>
            </motion.div>

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
              {/* Full Name */}
              <motion.div variants={itemVariants}>
                <Input
                  label="Full Name"
                  placeholder="Enter your full name"
                  error={errors.fullName?.message}
                  {...register("fullName")}
                />
              </motion.div>

              {/* Language Preference */}
              <motion.div variants={itemVariants} className="space-y-2">
                <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
                  Language Preference
                </label>
                <div className="grid grid-cols-4 gap-1.5 rounded-xl border border-border bg-white dark:bg-surface-dark p-1">
                  {LANGUAGES.map((lang) => (
                    <button
                      key={lang.code}
                      type="button"
                      onClick={() =>
                        setValue("languagePreference", lang.code as "en" | "hi" | "te" | "ta", {
                          shouldValidate: true,
                        })
                      }
                      className={cn(
                        "rounded-lg px-2 py-2 text-xs font-medium transition-all",
                        selectedLanguage === lang.code
                          ? "bg-brand-500 text-white shadow-sm"
                          : "text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800"
                      )}
                    >
                      {lang.label}
                    </button>
                  ))}
                </div>
              </motion.div>

              {/* Aadhaar */}
              <motion.div variants={itemVariants}>
                <Input
                  label="Aadhaar Number (Optional)"
                  placeholder="12-digit Aadhaar number"
                  hint="Only last 4 digits stored per DPDP guidelines"
                  error={errors.aadhaar?.message}
                  maxLength={12}
                  inputMode="numeric"
                  {...register("aadhaar")}
                />
              </motion.div>

              {/* Submit */}
              <motion.div variants={itemVariants} className="space-y-4 pt-2">
                <Button
                  type="submit"
                  className="w-full h-12 text-base rounded-xl"
                  loading={registerMutation.isPending}
                >
                  Create Account
                </Button>

                <div className="text-center">
                  <button
                    type="button"
                    onClick={() => router.push("/login")}
                    className="inline-flex items-center gap-1 text-sm text-muted dark:text-muted-dark hover:text-gray-900 dark:hover:text-gray-100 transition-colors"
                  >
                    <ArrowLeft className="h-3.5 w-3.5" />
                    Go back to login
                  </button>
                </div>
              </motion.div>
            </form>
          </motion.div>
        </div>
      </div>
    </div>
  );
}
