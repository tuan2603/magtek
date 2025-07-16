import { PaymentMethod } from '../enums';

export interface PANRequest {
  timeout: number;
  paymentMethods: PaymentMethod[] | null;
  Timeout(): number;
  PaymentMethods(): PaymentMethod[] | null;
}
