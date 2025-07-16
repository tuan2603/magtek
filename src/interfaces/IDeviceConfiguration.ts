import { InfoType } from '../enums';
import type { IConfigurationCallback } from './IConfigurationCallback';

export interface IDeviceConfiguration {
  getDeviceInfo(infoType: InfoType): string;
  getConfigInfo(param1: number, param2: number[]): number[];
  getKeyInfo(param1: number, param2: number[]): number[];
  getChallengeToken(param1: number[]): number[];
  setConfigInfo(
    param1: number,
    param2: number[],
    callback: IConfigurationCallback
  ): number;
  updateKeyInfo(
    param1: number,
    param2: number[],
    callback: IConfigurationCallback
  ): number;
  updateFirmware(
    param1: number,
    param2: number[],
    callback: IConfigurationCallback
  ): number;
  getFile(param1: number[], callback: IConfigurationCallback): number;
  sendFile(
    param1: number[],
    param2: number[],
    callback: IConfigurationCallback
  ): number;
  sendSecureFile(
    param1: number[],
    param2: number[],
    callback: IConfigurationCallback
  ): number;
  sendImage(
    param1: number,
    param2: number[],
    callback: IConfigurationCallback
  ): number;
  setDisplayImage(param1: number): number;
}
