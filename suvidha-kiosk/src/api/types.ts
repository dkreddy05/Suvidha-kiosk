export type LanguagePref = 'en' | 'hi' | 'te' | 'ta'

// ─── Error ────────────────────────────────────────────────────────────────

export type ApiError = {
  error: {
    code: string
    message: string
    fields?: Record<string, string>
    requestId: string
  }
}

// ─── Auth ─────────────────────────────────────────────────────────────────

export type CitizenDTO = {
  id: string
  mobile: string
  aadhaarLast4?: string
  name?: string
  languagePref: LanguagePref
  createdAt: string
}

export type OtpSendResponseDTO = {
  sessionId: string
  message: string
  devOtp?: string
}

export type VerifyOtpResponseDTO = {
  accessToken: string
  citizen: CitizenDTO
  isNewUser: boolean
}

// ─── Billing ──────────────────────────────────────────────────────────────

export type UtilityType = 'ELECTRICITY' | 'GAS' | 'WATER'
export type BillStatus = 'PENDING' | 'PAID' | 'OVERDUE'

export type UtilityAccountDTO = {
  id: string
  citizenId: string
  accountNumber: string
  utilityType: UtilityType
  providerName: string
  address: string
  latestBill?: {
    billId: string
    billMonth: string
    amount: number
    dueDate: string
    status: BillStatus
  }
}

export type BillDTO = {
  id: string
  accountId: string
  billNumber: string
  billMonth: string
  amount: number
  dueDate: string
  paidAmount: number
  status: BillStatus
  paidAt?: string
}

export type PaymentMode = 'UPI' | 'CARD' | 'CASH'

export type PaymentOrderDTO = {
  orderId: string
  amount: number
  currency: 'INR'
  keyId: string
  qrCodeData?: string
  upiId?: string
}

export type PaymentConfirmDTO = {
  paymentId: string
  receiptUrl: string
}

// ─── Grievance ────────────────────────────────────────────────────────────

export type GrievanceStatus = 'SUBMITTED' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED'

export type GrievanceTimelineEntry = {
  id: string
  status: GrievanceStatus
  note: string
  updatedBy: string
  updatedAt: string
}

export type GrievanceDTO = {
  id: string
  referenceNumber: string
  utilityType: UtilityType
  category: string
  description: string
  attachments?: { filename: string; url: string }[]
  status: GrievanceStatus
  estimatedResolutionDays: number
  createdAt: string
  updatedAt: string
  timeline: GrievanceTimelineEntry[]
}

// ─── Connections ──────────────────────────────────────────────────────────

export type RequestType = 'NEW' | 'TRANSFER' | 'SURRENDER'
export type ConnectionStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export type ConnectionRequestDTO = {
  id: string
  referenceNumber: string
  utilityType: UtilityType
  requestType: RequestType
  address: string
  status: ConnectionStatus
  createdAt: string
}

// ─── Pagination ───────────────────────────────────────────────────────────

export type PageDTO<T> = {
  items: T[]
  page: number
  pageSize: number
  totalItems: number
  totalPages: number
}
