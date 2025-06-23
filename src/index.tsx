import RMTMagtek, { type Transaction } from './NativeRMTMagtek';

export function startTransaction(transaction: Transaction): Promise<void> {
  return RMTMagtek.startTransaction(transaction);
}
