export interface PINRequest {
  timeout: number;
  pinMode: number;
  minLength: number;
  maxLength: number;
  tone: number;
  format: number;
  pan: string;
  Timeout(): number;
  PINMode(): number;
  MinLength(): number;
  MaxLength(): number;
  Tone(): number;
  Format(): number;
  PAN(): string;
  setTimeout(timeout: number): void;
  setPINMode(pinMode: number): void;
  setMinLength(minLength: number): void;
  setMaxLength(maxLength: number): void;
  setTone(tone: number): void;
  setFormat(format: number): void;
  setPAN(pan: string): void;
}
