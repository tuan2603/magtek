import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  startTransaction(transaction: string): Promise<void>;
  refreshList(): void;
  addListener(eventName: string): void;
  connect(device: string): Promise<void>;
  disconnect(): Promise<void>;
  resetDevice(): Promise<void>;
  isConnected(): Promise<boolean>;
  isDisconnected(): Promise<boolean>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('RMTMagtek');
