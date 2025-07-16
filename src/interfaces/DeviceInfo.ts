export interface DeviceInfo {
  name: string;
  model: string;
  serial: string;
  inBootloaderMode: boolean;
  setInBootloaderMode(value: boolean): void;
  getName(): string;
  getModel(): string;
  getSerial(): string;
  isInBootloaderMode(): boolean;
}
