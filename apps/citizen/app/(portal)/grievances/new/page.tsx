"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, useCallback } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { motion } from "framer-motion";
import { toast } from "sonner";
import {
  ArrowLeft,
  FileText,
  CheckCircle2,
  ExternalLink,
  AlertCircle,
  MessageSquare,
} from "lucide-react";
import {
  Button,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Textarea,
  Select,
  Input,
} from "@suvidha/ui";
import { cn } from "@/lib/utils";
import { useSubmitGrievance, useGrievanceCategories } from "@/hooks/grievances/useGrievances";
import { PageHeader } from "@/components/shared/PageHeader";
import { EmptyState } from "@/components/shared/EmptyState";
import type { GrievanceCategory } from "@/types/grievances";

const grievanceFormSchema = z.object({
  category: z.string().min(1, "Please select a category"),
  description: z
    .string()
    .min(10, "Description must be at least 10 characters")
    .max(500, "Description cannot exceed 500 characters"),
  photoUrl: z
    .string()
    .url("Please enter a valid URL")
    .optional()
    .or(z.literal("")),
});

type GrievanceFormData = z.infer<typeof grievanceFormSchema>;

function SubmitSuccessCard({ referenceNumber }: { referenceNumber: string }) {
  const copyReference = useCallback(() => {
    navigator.clipboard.writeText(referenceNumber);
    toast.success("Reference number copied");
  }, [referenceNumber]);

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.4, ease: "easeOut" }}
    >
      <Card className="border-green-200 dark:border-green-800">
        <CardContent className="pt-6">
          <div className="flex flex-col items-center text-center py-6">
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400 mb-4">
              <CheckCircle2 className="h-8 w-8" />
            </div>
            <h2 className="text-xl font-bold text-gray-900 dark:text-gray-100 mb-1">
              Grievance Submitted!
            </h2>
            <p className="text-sm text-muted dark:text-muted-dark mb-6">
              Your grievance has been submitted successfully and is under review.
            </p>

            <div className="w-full max-w-sm rounded-lg border border-border bg-gray-50 dark:bg-gray-800/50 p-4 mb-6">
              <p className="text-xs text-muted dark:text-muted-dark uppercase tracking-wider font-semibold mb-1">
                Reference Number
              </p>
              <div className="flex items-center justify-between gap-2">
                <span className="text-lg font-mono font-bold text-gray-900 dark:text-gray-100 tracking-wider">
                  {referenceNumber}
                </span>
                <button
                  type="button"
                  onClick={copyReference}
                  className="shrink-0 text-muted dark:text-muted-dark hover:text-gray-900 dark:hover:text-gray-100 transition-colors p-1"
                  title="Copy reference number"
                >
                  <FileText className="h-4 w-4" />
                </button>
              </div>
            </div>

            <div className="flex flex-col sm:flex-row gap-3 w-full max-w-sm">
              <Link href={`/grievances/${referenceNumber}`} className="flex-1">
                <Button variant="default" className="w-full">
                  <ExternalLink className="h-4 w-4 mr-1.5" />
                  Track Grievance
                </Button>
              </Link>
              <Link href="/grievances" className="flex-1">
                <Button variant="outline" className="w-full">
                  <ArrowLeft className="h-4 w-4 mr-1.5" />
                  Back to My Grievances
                </Button>
              </Link>
            </div>
          </div>
        </CardContent>
      </Card>
    </motion.div>
  );
}

export default function NewGrievancePage() {
  const router = useRouter();
  const [submittedReference, setSubmittedReference] = useState<string | null>(null);

  const {
    data: categories,
    isLoading: categoriesLoading,
    error: categoriesError,
  } = useGrievanceCategories();

  const submitGrievance = useSubmitGrievance();

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<GrievanceFormData>({
    resolver: zodResolver(grievanceFormSchema),
    defaultValues: {
      category: "",
      description: "",
      photoUrl: "",
    },
  });

  const descriptionValue = watch("description");
  const charCount = descriptionValue?.length ?? 0;

  const categoryOptions =
    (categories ?? []).map((cat: GrievanceCategory) => ({
      value: cat.id,
      label: cat.name,
    })) ?? [];

  const onSubmit = useCallback(
    async (data: GrievanceFormData) => {
      try {
        const result = await submitGrievance.mutateAsync({
          category: data.category,
          description: data.description,
          photoUrl: data.photoUrl || undefined,
        });
        setSubmittedReference(result.referenceNumber);
        toast.success("Grievance submitted successfully");
      } catch (err: any) {
        const message =
          err?.response?.data?.message ??
          err?.message ??
          "Failed to submit grievance. Please try again.";
        toast.error(message, {
          duration: 5000,
        });
      }
    },
    [submitGrievance]
  );

  if (submittedReference) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
        <PageHeader
          title="Submit a Grievance"
          breadcrumbs={[
            { label: "My Grievances", href: "/grievances" },
            { label: "New Grievance" },
          ]}
        />
        <div className="px-4 sm:px-6 lg:px-8 pb-8 max-w-2xl mx-auto">
          <SubmitSuccessCard referenceNumber={submittedReference} />
        </div>
      </div>
    );
  }

  if (categoriesError) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
        <PageHeader
          title="Submit a Grievance"
          breadcrumbs={[
            { label: "My Grievances", href: "/grievances" },
            { label: "New Grievance" },
          ]}
        />
        <div className="px-4 sm:px-6 lg:px-8">
          <EmptyState
            icon={AlertCircle}
            title="Something went wrong"
            description="We couldn't load grievance categories. Please try again."
            actionLabel="Go Back"
            onAction={() => router.push("/grievances")}
          />
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <PageHeader
        title="Submit a Grievance"
        breadcrumbs={[
          { label: "My Grievances", href: "/grievances" },
          { label: "New Grievance" },
        ]}
      />

      <div className="px-4 sm:px-6 lg:px-8 pb-8">
        <div className="max-w-2xl mx-auto">
          <form onSubmit={handleSubmit(onSubmit)}>
            <div className="space-y-6">
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">Grievance Details</CardTitle>
                </CardHeader>
                <CardContent className="space-y-5">
                  <Select
                    {...register("category")}
                    label="Category"
                    placeholder="Select a category"
                    options={categoryOptions}
                    error={errors.category?.message}
                    disabled={categoriesLoading}
                  />

                  <div className="relative">
                    <Textarea
                      {...register("description")}
                      label="Description"
                      placeholder="Describe your grievance in detail..."
                      rows={5}
                      maxLength={500}
                      error={errors.description?.message}
                      className="resize-none pb-6"
                    />
                    <div className="absolute bottom-2 right-3">
                      <span
                        className={cn(
                          "text-xs",
                          charCount > 450
                            ? "text-danger"
                            : "text-muted dark:text-muted-dark"
                        )}
                      >
                        {charCount}/500
                      </span>
                    </div>
                  </div>

                  <Input
                    {...register("photoUrl")}
                    label="Photo URL (optional)"
                    type="url"
                    placeholder="https://example.com/image.jpg"
                    hint="Link to image (optional)"
                    error={errors.photoUrl?.message}
                  />
                </CardContent>
              </Card>

              <div className="flex items-center gap-3 pt-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => router.push("/grievances")}
                >
                  <ArrowLeft className="h-4 w-4 mr-1.5" />
                  Cancel
                </Button>
                <Button
                  type="submit"
                  loading={submitGrievance.isPending}
                  disabled={submitGrievance.isPending || categoriesLoading}
                  className="flex-1 sm:flex-none"
                >
                  {submitGrievance.isPending ? (
                    "Submitting..."
                  ) : (
                    <>
                      <MessageSquare className="h-4 w-4 mr-1.5" />
                      Submit Grievance
                    </>
                  )}
                </Button>
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
