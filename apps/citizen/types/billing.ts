export type ServiceType = 'ELECTRICITY' | 'WATER' | 'GAS';
export type BillStatus = 'PENDING' | 'PAYMENT_IN_PROGRESS' | 'PAID' | 'OVERDUE' | 'PARTIALLY_PAID' | 'CANCELLED';
export type PaymentMethod = 'UPI' | 'CARD' | 'CASH' | 'MOCK_CARD' | 'MOCK_UPI' | 'MOCK_NEFT';
export type PaymentStatus = 'SUCCESS' | 'FAILED' | 'PENDING' | 'REFUNDED';

export interface UtilityAccount {
  id: string;
  citizenId: string;
  accountNumber: string;
  serviceType: ServiceType;
  providerName: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
}

export interface Bill {
  id: string;
  accountId: string;
  billNumber: string;
  billMonth: string;
  amount: number;
  dueDate: string;
  status: BillStatus;
  paidAmount?: number;
  paidAt?: string;
  lateFee?: number;
}

export interface Payment {
  transactionId: string;
  accountId: string;
  amount: number;
  method: PaymentMethod;
  status: PaymentStatus;
  createdAt: string;
  billIds: string[];
  receiptUrl?: string;
}

export interface PaymentRequest {
  billIds: string[];
  amount: number;
  method: PaymentMethod;
  idempotencyKey: string;
}

export interface PaymentConfirmRequest {
  razorpayOrderId: string;
  razorpayPaymentId: string;
  razorpaySignature: string;
  idempotencyKey: string;
}

export interface Provider {
  id: string;
  name: string;
  serviceType: ServiceType;
}

export interface ServiceTypeInfo {
  type: ServiceType;
  label: string;
  icon: string;
}

export const SERVICE_TYPES: ServiceTypeInfo[] = [
  { type: 'ELECTRICITY', label: 'Electricity', icon: 'Zap' },
  { type: 'WATER', label: 'Water', icon: 'Droplets' },
  { type: 'GAS', label: 'Gas', icon: 'Flame' },
];

export const SERVICE_ICONS: Record<ServiceType, string> = {
  ELECTRICITY: 'Zap',
  WATER: 'Droplets',
  GAS: 'Flame',
};
