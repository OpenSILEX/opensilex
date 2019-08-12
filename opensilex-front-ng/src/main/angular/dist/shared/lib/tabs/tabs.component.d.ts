import { EventEmitter } from '@angular/core';
import { Tab } from './tab.interface';
export declare class TabsComponent {
    tabs: Tab[];
    selected: EventEmitter<any>;
    addTab(tabComponent: Tab): void;
    selectTab(tabComponent: Tab): void;
}
