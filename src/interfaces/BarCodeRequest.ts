import { BarCodeType, BarCodeFormat } from '../enums';

export interface BarCodeRequest {
  type: BarCodeType;
  format: BarCodeFormat;
  data: number[];
  blockColor: number[];
  backgroundColor: number[];
  errorCorrection: number;
  maskPattern: number;
  minVersion: number;
  maxVersion: number;
  Type(): BarCodeType;
  Format(): BarCodeFormat;
  Data(): number[];
  BlockColor(): number[];
  BackgroundColor(): number[];
  ErrorCorrection(): number;
  MaskPattern(): number;
  MinVersion(): number;
  MaxVersion(): number;
}
