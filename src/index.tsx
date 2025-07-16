import RMTMagtek from './NativeRMTMagtek';
import type { Transaction } from './interfaces/Transaction';
import type { Device } from './interfaces/Device';

export function startTransaction(transaction: Transaction): Promise<void> {
  // Convert Transaction object to JSON string
  const transactionJson = JSON.stringify(transaction);
  return RMTMagtek.startTransaction(transactionJson);
}

export function refreshList(): Promise<Device[]> {
  return RMTMagtek.refreshList().then((deviceListJson: string) => {
    // Parse JSON string to Device array
    return JSON.parse(deviceListJson) as Device[];
  });
}
