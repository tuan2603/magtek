import type { ConnectionInfo } from './ConnectionInfo';
import { ConnectionState } from '../enums';
import type { DeviceInfo } from './DeviceInfo';
import type { IDeviceCapabilities } from './IDeviceCapabilities';
import type { IDeviceControl } from './IDeviceControl';
import type { IDeviceConfiguration } from './IDeviceConfiguration';
import type { IEventSubscriber } from './IEventSubscriber';
import type { ITransaction } from './ITransaction';
import type { PINRequest } from './PINRequest';
import type { PANRequest } from './PANRequest';
import type { IData } from './IData';

export interface IDevice {
  Name(): string;
  getConnectionInfo(): ConnectionInfo;
  getConnectionState(): ConnectionState;
  getDeviceInfo(): DeviceInfo;
  getCapabilities(): IDeviceCapabilities;
  getDeviceControl(): IDeviceControl;
  getDeviceConfiguration(): IDeviceConfiguration;
  subscribeAll(subscriber: IEventSubscriber): boolean;
  unsubscribeAll(subscriber: IEventSubscriber): boolean;
  startTransaction(transaction: ITransaction): boolean;
  cancelTransaction(): boolean;
  sendSelection(data: IData): boolean;
  sendAuthorization(data: IData): boolean;
  requestPIN(pinRequest: PINRequest): boolean;
  requestPAN(panRequest: PANRequest, pinRequest: PINRequest): boolean;
  requestSignature(): boolean;
  sendNFCCommand(data: IData, param2: boolean, param3: boolean): boolean;
  sendClassicNFCCommand(data: IData, param2: boolean, param3: boolean): boolean;
  sendDESFireNFCCommand(data: IData, param2: boolean, param3: boolean): boolean;
}
