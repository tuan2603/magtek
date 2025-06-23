import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Transaction {
  msr?: boolean;
  contact?: boolean;
  contactless?: boolean;
  vas?: boolean;
  gvas?: boolean;
  nfc?: boolean;
  bcr?: boolean;
  amount: number;
  cashBack?: string;
  quickChip?: boolean;
  emvOnly?: boolean;
  nfcReadOnlyMode?: boolean;
  signature?: boolean;
  fallback?: boolean;
  showAmount?: boolean;
  showTipOptions?: boolean;
  showTax?: boolean;
}

export interface Spec extends TurboModule {
  startTransaction(transaction: Transaction): Promise<void>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('RMTMagtek');
