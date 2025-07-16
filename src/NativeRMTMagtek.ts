import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  startTransaction(transaction: string): Promise<void>;
  refreshList(): Promise<string>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('RMTMagtek');
