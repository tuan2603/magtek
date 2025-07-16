import { EventType } from '../enums';
import type { IData } from './IData';

export interface IEventSubscriber {
  OnEvent(eventType: EventType, data: IData): void;
}
