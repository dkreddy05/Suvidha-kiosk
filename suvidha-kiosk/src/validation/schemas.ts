import { z } from 'zod'

export const mobileSchema = z
  .string()
  .regex(/^\d{10}$/, { message: 'mobile' })

export const otpSchema = z
  .string()
  .regex(/^\d{6}$/, { message: 'otp' })

export const nameSchema = z
  .string()
  .min(1)
  .max(80)

export const billIdsSchema = z.array(z.string()).min(1).max(10)

export const paymentModeSchema = z.enum(['UPI', 'CARD', 'CASH'])

export const pageSchema = z.number().int().min(1)
export const pageSizeSchema = z.number().int().min(1).max(100)

export const descriptionSchema = z.string().min(10).max(1000)

export const attachmentSchema = z.object({
  filename: z.string().min(1),
  base64: z.string().min(1),
  mime: z.string().min(1),
  size: z.number().int().nonnegative(),
})

export const attachmentsSchema = z
  .array(attachmentSchema)
  .max(5)
  .refine(
    (files) =>
      files.every((f) =>
        ['image/jpeg', 'image/png', 'application/pdf'].includes(f.mime),
      ),
    { message: 'attachment_type' },
  )
  .refine((files) => files.every((f) => f.size <= 5 * 1024 * 1024), {
    message: 'attachment_size',
  })
