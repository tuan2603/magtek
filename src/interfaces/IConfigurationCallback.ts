import { StatusCode } from '../enums';
import type { IResult } from './IResult';

export interface IConfigurationCallback {
  OnProgress(progress: number): void;
  OnResult(status: StatusCode, data: number[]): void;
  OnCalculateMAC(param1: number, param2: number[]): IResult;
}
