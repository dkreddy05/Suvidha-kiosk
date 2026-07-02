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
  Zap,
  Droplets,
  Flame,
  Upload,
  X,
  AlertCircle,
  Cable,
  CheckCircle2,
  ExternalLink,
  MapPin,
  FileText,
} from "lucide-react";
import {
  Button,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Textarea,
  Skeleton,
  Progress,
} from "@suvidha/ui";
import { cn } from "@/lib/utils";
import { useCreateConnectionRequest } from "@/hooks/connections/useConnections";
import { PageHeader } from "@/components/shared/PageHeader";
import type { ServiceType } from "@/types/billing";

const SERVICE_OPTIONS: Array<{
  value: ServiceType;
  label: string;
  icon: typeof Zap;
  color: string;
  activeColor: string;
}> = [
  {
    value: "ELECTRICITY",
    label: "Electricity",
    icon: Zap,
    color:
      "border-gray-200 text-gray-500 hover:border-amber-300 hover:text-amber-600 dark:border-gray-600 dark:text-gray-400",
    activeColor:
      "border-amber-500 bg-amber-50 text-amber-700 dark:bg-amber-900/20 dark:border-amber-400 dark:text-amber-300",
  },
  {
    value: "WATER",
    label: "Water",
    icon: Droplets,
    color:
      "border-gray-200 text-gray-500 hover:border-blue-300 hover:text-blue-600 dark:border-gray-600 dark:text-gray-400",
    activeColor:
      "border-blue-500 bg-blue-50 text-blue-700 dark:bg-blue-900/20 dark:border-blue-400 dark:text-blue-300",
  },
  {
    value: "GAS",
    label: "Gas",
    icon: Flame,
    color:
      "border-gray-200 text-gray-500 hover:border-orange-300 hover:text-orange-600 dark:border-gray-600 dark:text-gray-400",
    activeColor:
      "border-orange-500 bg-orange-50 text-orange-700 dark:bg-orange-900/20 dark:border-orange-400 dark:text-orange-300",
  },
];

const MAX_ADDRESS_LENGTH = 512;

const connectionFormSchema = z.object({
  serviceType: z.enum(["ELECTRICITY", "WATER", "GAS"], {
    required_error: "Please select a service type",
  }),
  address: z
    .string()
    .min(10, "Please enter a complete address (at least 10 characters)")
    .max(MAX_ADDRESS_LENGTH, `Address cannot exceed ${MAX_ADDRESS_LENGTH} characters`),
});

type ConnectionFormData = z.infer<typeof connectionFormSchema>;

const convertToBase64 = (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => {
      const base64String = (reader.result as string).split(",")[1];
      resolve(base64String);
    };
    reader.onerror = (error) => reject(error);
  });
};

interface FileWithName {
  file: File;
  name: string;
  size: number;
}

function NewConnectionSuccessCard({
  referenceNumber,
  serviceType,
}: {
  referenceNumber: string;
  serviceType: ServiceType;
}) {
  const serviceLabel =
    serviceType === "ELECTRICITY"
      ? "Electricity"
      : serviceType === "WATER"
        ? "Water"
        : "Gas";

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
              Application Submitted!
            </h2>
            <p className="text-sm text-muted dark:text-muted-dark mb-6">
              Your {serviceLabel} connection request has been received.
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
              <Link href={`/connections/${referenceNumber}`} className="flex-1">
                <Button variant="default" className="w-full">
                  <ExternalLink className="h-4 w-4 mr-1.5" />
                  Track Application
                </Button>
              </Link>
              <Link href="/connections" className="flex-1">
                <Button variant="outline" className="w-full">
                  <Cable className="h-4 w-4 mr-1.5" />
                  Go to My Connections
                </Button>
              </Link>
            </div>
          </div>
        </CardContent>
      </Card>
    </motion.div>
  );
}

export default function NewConnectionPage() {
  const router = useRouter();
  const [files, setFiles] = useState<FileWithName[]>([]);
  const [isDragOver, setIsDragOver] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<ConnectionFormData>({
    resolver: zodResolver(connectionFormSchema),
    defaultValues: {
      serviceType: undefined,
      address: "",
    },
  });

  const createRequest = useCreateConnectionRequest();
  const [submittedRequest, setSubmittedRequest] = useState<{
    referenceNumber: string;
    serviceType: ServiceType;
  } | null>(null);

  const selectedServiceType = watch("serviceType");
  const addressValue = watch("address");

  const handleServiceSelect = useCallback(
    (type: ServiceType) => {
      setValue("serviceType", type, { shouldValidate: true });
    },
    [setValue]
  );

  const handleFileAdd = useCallback(
    (newFiles: FileList | File[]) => {
      const toAdd = Array.from(newFiles)
        .filter(
          (f) =>
            !files.some(
              (existing) => existing.name === f.name && existing.size === f.size
            )
        )
        .map((f) => ({ file: f, name: f.name, size: f.size }));
      if (toAdd.length > 0) {
        setFiles((prev) => [...prev, ...toAdd]);
      }
    },
    [files]
  );

  const handleRemoveFile = useCallback((index: number) => {
    setFiles((prev) => prev.filter((_, i) => i !== index));
  }, []);

  const handleDrop = useCallback(
    (e: React.DragEvent<HTMLDivElement>) => {
      e.preventDefault();
      setIsDragOver(false);
      if (e.dataTransfer.files.length > 0) {
        handleFileAdd(e.dataTransfer.files);
      }
    },
    [handleFileAdd]
  );

  const handleDragOver = useCallback((e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragOver(true);
  }, []);

  const handleDragLeave = useCallback(() => {
    setIsDragOver(false);
  }, []);

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const onSubmit = useCallback(
    async (data: ConnectionFormData) => {
      if (files.length === 0) {
        toast.error("Please upload at least one supporting document.");
        return;
      }
      try {
        const docPromises = files.map(async (f) => {
          const base64Data = await convertToBase64(f.file);
          return {
            type: "IDENTITY",
            base64: base64Data,
          };
        });
        const docsPayload = await Promise.all(docPromises);

        const result = await createRequest.mutateAsync({
          serviceType: data.serviceType,
          address: data.address,
          documents: docsPayload,
        });
        setSubmittedRequest({
          referenceNumber: result.referenceNumber,
          serviceType: data.serviceType,
        });
        toast.success("Connection request submitted successfully!");
      } catch (err: any) {
        const message =
          err?.response?.data?.message ??
          err?.message ??
          "Failed to submit request. Please try again.";
        toast.error(message, {
          duration: 5000,
        });
      }
    },
    [createRequest, files]
  );

  if (submittedRequest) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
        <PageHeader
          title="Apply for New Connection"
          breadcrumbs={[
            { label: "My Connections", href: "/connections" },
            { label: "New Application" },
          ]}
        />
        <div className="px-4 sm:px-6 lg:px-8 pb-8 max-w-2xl mx-auto">
          <NewConnectionSuccessCard
            referenceNumber={submittedRequest.referenceNumber}
            serviceType={submittedRequest.serviceType}
          />
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <PageHeader
        title="Apply for New Connection"
        breadcrumbs={[
          { label: "My Connections", href: "/connections" },
          { label: "New Application" },
        ]}
      />

      <div className="px-4 sm:px-6 lg:px-8 pb-8">
        <div className="max-w-2xl mx-auto">
          <form onSubmit={handleSubmit(onSubmit)}>
            <div className="space-y-6">
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">Service Type</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-3 gap-3">
                    {SERVICE_OPTIONS.map((option) => {
                      const isActive = selectedServiceType === option.value;
                      const Icon = option.icon;
                      return (
                        <button
                          key={option.value}
                          type="button"
                          onClick={() => handleServiceSelect(option.value)}
                          className={cn(
                            "flex flex-col items-center gap-2 rounded-xl border-2 p-4 transition-all duration-200",
                            isActive ? option.activeColor : option.color
                          )}
                        >
                          <Icon className="h-6 w-6" />
                          <span className="text-xs font-semibold">
                            {option.label}
                          </span>
                        </button>
                      );
                    })}
                  </div>
                  {errors.serviceType && (
                    <p className="text-xs text-danger mt-2" role="alert">
                      {errors.serviceType.message}
                    </p>
                  )}
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">Connection Address</CardTitle>
                </CardHeader>
                <CardContent className="space-y-3">
                  <div className="flex items-start gap-2 text-sm text-muted dark:text-muted-dark mb-1">
                    <MapPin className="h-4 w-4 mt-0.5 shrink-0" />
                    <span>
                      Provide the full address where the connection needs to be
                      installed.
                    </span>
                  </div>
                  <div className="relative">
                    <Textarea
                      {...register("address")}
                      placeholder="Enter the complete address..."
                      rows={4}
                      maxLength={MAX_ADDRESS_LENGTH}
                      error={errors.address?.message}
                      className="resize-none"
                    />
                    <div className="absolute bottom-2 right-3">
                      <span
                        className={cn(
                          "text-xs",
                          (addressValue?.length ?? 0) > MAX_ADDRESS_LENGTH * 0.9
                            ? "text-danger"
                            : "text-muted dark:text-muted-dark"
                        )}
                      >
                        {addressValue?.length ?? 0}/{MAX_ADDRESS_LENGTH}
                      </span>
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">
                    Supporting Documents
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <p className="text-sm text-muted dark:text-muted-dark">
                    Upload identity proof, address proof, and any other
                    supporting documents (optional).
                  </p>

                  <div
                    onDrop={handleDrop}
                    onDragOver={handleDragOver}
                    onDragLeave={handleDragLeave}
                    className={cn(
                      "flex flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed p-8 transition-colors cursor-pointer",
                      isDragOver
                        ? "border-brand-400 bg-brand-50/50 dark:bg-brand-900/10"
                        : "border-border hover:border-brand-300 hover:bg-gray-50 dark:hover:bg-gray-800/50"
                    )}
                    onClick={() =>
                      document.getElementById("file-upload")?.click()
                    }
                  >
                    <div className="flex h-12 w-12 items-center justify-center rounded-full bg-gray-100 dark:bg-gray-800">
                      <Upload className="h-6 w-6 text-muted dark:text-muted-dark" />
                    </div>
                    <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                      Drop files here or click to upload
                    </p>
                    <p className="text-xs text-muted dark:text-muted-dark">
                      PDF, JPG, PNG (max 10MB each)
                    </p>
                    <input
                      id="file-upload"
                      type="file"
                      multiple
                      accept=".pdf,.jpg,.jpeg,.png"
                      className="hidden"
                      onChange={(e) => {
                        if (e.target.files && e.target.files.length > 0) {
                          handleFileAdd(e.target.files);
                          e.target.value = "";
                        }
                      }}
                    />
                  </div>

                  {files.length > 0 && (
                    <div className="space-y-2">
                      {files.map((file, idx) => (
                        <div
                          key={`${file.name}-${file.size}`}
                          className="flex items-center justify-between rounded-lg border border-border bg-gray-50 dark:bg-gray-800/50 px-3 py-2"
                        >
                          <div className="flex items-center gap-2 min-w-0">
                            <FileText className="h-4 w-4 text-muted dark:text-muted-dark shrink-0" />
                            <span className="text-sm text-gray-900 dark:text-gray-100 truncate">
                              {file.name}
                            </span>
                            <span className="text-xs text-muted dark:text-muted-dark shrink-0">
                              {formatFileSize(file.size)}
                            </span>
                          </div>
                          <button
                            type="button"
                            onClick={() => handleRemoveFile(idx)}
                            className="shrink-0 p-1 text-muted dark:text-muted-dark hover:text-danger transition-colors"
                            title="Remove file"
                          >
                            <X className="h-4 w-4" />
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </CardContent>
              </Card>

              <div className="flex items-center gap-3 pt-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => router.push("/connections")}
                >
                  <ArrowLeft className="h-4 w-4 mr-1.5" />
                  Cancel
                </Button>
                <Button
                  type="submit"
                  loading={createRequest.isPending}
                  disabled={createRequest.isPending}
                  className="flex-1 sm:flex-none"
                >
                  {createRequest.isPending ? (
                    "Submitting..."
                  ) : (
                    <>
                      <Cable className="h-4 w-4 mr-1.5" />
                      Submit Application
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
