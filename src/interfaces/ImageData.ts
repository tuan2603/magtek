import { ImageType } from '../enums';

export interface ImageData {
  type: ImageType;
  data: number[];
  backgroundColor: number[];
  Type(): ImageType;
  Data(): number[];
  BackgroundColor(): number[];
}
