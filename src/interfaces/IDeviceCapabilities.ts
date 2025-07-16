import { PaymentMethod } from '../enums';

export interface IDeviceCapabilities {
  PaymentMethods(): PaymentMethod[];
  Display(): boolean;
  PINPad(): boolean;
  Signature(): boolean;
  SRED(): boolean;
  MSRPowerSaver(): boolean;
  BatteryBackedClock(): boolean;
  BarCode(): boolean;
  USB(): boolean;
  RS232(): boolean;
  Ethernet(): boolean;
  Wifi(): boolean;
  BLE(): boolean;
}
