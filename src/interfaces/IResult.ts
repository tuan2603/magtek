import { StatusCode } from '../enums';
import type { IData } from './IData';

export interface IResult {
  Status(): StatusCode;
  Data(): IData;
}
