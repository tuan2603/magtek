import { DeviceType, ConnectionType } from '../enums';
import type { CertificateInfo } from './CertificateInfo';

export interface ConnectionInfo {
  deviceType: DeviceType;
  connectionType: ConnectionType;
  address: string;
  certificateInfo: CertificateInfo | null;
  setCertificateInfo(certificateInfo: CertificateInfo): void;
  getDeviceType(): DeviceType;
  getConnectionType(): ConnectionType;
  getAddress(): string;
  getCertificateInfo(): CertificateInfo | null;
}
