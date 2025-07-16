export interface CertificateInfo {
  format: string;
  data: number[];
  password: string;
  getFormat(): string;
  getData(): number[];
  getPassword(): string;
}
