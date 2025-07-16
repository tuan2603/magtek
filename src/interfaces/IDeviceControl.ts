import type { IData } from './IData';
import type { IResult } from './IResult';
import type { ImageData } from './ImageData';
import type { BarCodeRequest } from './BarCodeRequest';

export interface IDeviceControl {
  open(): boolean;
  close(): boolean;
  send(data: IData): boolean;
  send(data: IData, timeout: number): boolean;
  sendSync(data: IData): IResult;
  sendSync(data: IData, timeout: number): IResult;
  sendExtendedCommand(data: IData): boolean;
  endSession(): boolean;
  setDateTime(data: IData): boolean;
  playSound(data: IData): boolean;
  getInput(data: IData): boolean;
  displayMessage(param1: number, param2: number): boolean;
  showImage(param1: number): boolean;
  showImage(imageData: ImageData, param2: number): boolean;
  showBarCode(
    barCodeRequest: BarCodeRequest,
    param2: number,
    data: IData
  ): boolean;
  startBarCodeReader(param1: number, param2: number): boolean;
  stopBarCodeReader(): boolean;
  setLatch(value: boolean): boolean;
  deviceReset(): boolean;
  showUIPageWithTextLines(
    param1: number,
    textLines: string[],
    param3: number[]
  ): boolean;
  showUIPageWithTextLines(
    param1: number,
    text1: string,
    text2: string,
    text3: string,
    text4: string,
    text5: string,
    param7: number[]
  ): boolean;
  showUIPageWithTextButtons(
    param1: number,
    param2: number[],
    param3: number[],
    param4: number[],
    param5: number[],
    param6: number[],
    param7: number[],
    param8: number[],
    param9: number[],
    param10: number[],
    param11: number[],
    param12: number,
    param13: number,
    param14: number
  ): boolean;
  showUIPageWithAmountButtons(
    param1: number,
    param2: number[],
    amounts: string[],
    param4: number[],
    param5: number[],
    param6: number[],
    param7: number,
    param8: number,
    param9: number
  ): boolean;
  showUIPageWithAmountButtons(
    param1: number,
    param2: number[],
    amount1: string,
    amount2: string,
    amount3: string,
    amount4: string,
    amount5: string,
    amount6: string,
    param9: number[],
    param10: number[],
    param11: number[],
    param12: number,
    param13: number,
    param14: number
  ): boolean;
  showUIPageWithAmountButtons(
    param1: number,
    param2: number[],
    param3: number[],
    param4: number[],
    param5: number[],
    param6: number[],
    param7: number[],
    param8: number[],
    param9: number[],
    param10: number[],
    param11: number[],
    param12: number,
    param13: number,
    param14: number
  ): boolean;
  showUIPageWithImage(
    param1: number,
    param2: number[],
    param3: number[],
    param4: number[],
    param5: number[],
    param6: number[]
  ): boolean;
  showUIPage(
    param1: number,
    param2: number,
    param3: number[],
    text1: string,
    text2: string,
    text3: string,
    text4: string,
    text5: string,
    param9: number[],
    param10: number[],
    param11: number[],
    param12: number[],
    param13: number[],
    param14: number[],
    param15: number[],
    param16: number[],
    param17: number[],
    param18: number[],
    param19: number[],
    param20: number[],
    param21: number[],
    param22: number[],
    param23: number[],
    param24: number,
    param25: number,
    param26: number,
    param27: number[],
    param28: number[],
    param29: number[]
  ): boolean;
}
