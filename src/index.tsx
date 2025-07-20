import RMTMagtek from './NativeRMTMagtek';
import type { Transaction, Device } from './interfaces';

export function startTransaction(transaction: Transaction): Promise<void> {
  // Convert Transaction object to JSON string
  const transactionJson = JSON.stringify(transaction);
  return RMTMagtek.startTransaction(transactionJson);
}

export function refreshList() {
  return RMTMagtek.refreshList();
}

export function connect(device: Device): Promise<void> {
  const deviceJson = JSON.stringify(device);
  return RMTMagtek.connect(deviceJson);
}

export function disconnect(): Promise<void> {
  return RMTMagtek.disconnect();
}

export function resetDevice(): Promise<void> {
  return RMTMagtek.resetDevice();
}

export function isConnected(): Promise<boolean> {
  return RMTMagtek.isConnected();
}

export function isDisconnected(): Promise<boolean> {
  return RMTMagtek.isDisconnected();
}
