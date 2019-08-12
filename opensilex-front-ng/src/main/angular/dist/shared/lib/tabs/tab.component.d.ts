import { Tab } from './tab.interface';
import { TabsComponent } from './tabs.component';
export declare class TabComponent implements Tab {
    title: string;
    hidden: boolean;
    constructor(tabsComponent: TabsComponent);
}
