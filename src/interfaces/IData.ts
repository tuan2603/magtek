export interface IData {
  StringValue(): string;
  ByteArray(): number[];
  Clone(): IData;
}
