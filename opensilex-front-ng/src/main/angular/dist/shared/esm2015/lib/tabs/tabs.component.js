/**
 * @fileoverview added by tsickle
 * @suppress {checkTypes,constantProperty,extraRequire,missingOverride,missingReturn,unusedPrivateMembers,uselessCode} checked by tsc
 */
import { Component, EventEmitter, Output } from '@angular/core';
export class TabsComponent {
    constructor() {
        this.tabs = [];
        this.selected = new EventEmitter();
    }
    /**
     * @param {?} tabComponent
     * @return {?}
     */
    addTab(tabComponent) {
        if (!this.tabs.length) {
            tabComponent.hidden = false;
        }
        this.tabs.push(tabComponent);
    }
    /**
     * @param {?} tabComponent
     * @return {?}
     */
    selectTab(tabComponent) {
        this.tabs.map((/**
         * @param {?} tab
         * @return {?}
         */
        tab => (tab.hidden = true)));
        tabComponent.hidden = false;
        this.selected.emit(tabComponent);
    }
}
TabsComponent.decorators = [
    { type: Component, args: [{
                selector: 'shared-tabs',
                template: "<ul class=\"tabs\">\n  <li *ngFor=\"let tab of tabs\" (click)=\"selectTab(tab)\" class=\"tab\" [class.tab--active]=\"!tab.hidden\">\n      {{tab.title}}\n  </li>\n</ul>\n<div class=\"tab-body\">\n    <ng-content></ng-content>\n</div>",
                styles: [":host{display:block}.tabs{display:flex;list-style:none;margin:0;padding:0;border-bottom:1px solid #ebeef2}.tab{position:relative;padding:0 20px;line-height:40px;cursor:pointer}.tab-body{padding:20px}.tab--active:before{content:\"\";position:absolute;bottom:0;left:0;right:0;height:3px;background:#03a9f4}"]
            }] }
];
TabsComponent.propDecorators = {
    selected: [{ type: Output }]
};
if (false) {
    /** @type {?} */
    TabsComponent.prototype.tabs;
    /** @type {?} */
    TabsComponent.prototype.selected;
}
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoidGFicy5jb21wb25lbnQuanMiLCJzb3VyY2VSb290Ijoibmc6Ly9zaGFyZWQvIiwic291cmNlcyI6WyJsaWIvdGFicy90YWJzLmNvbXBvbmVudC50cyJdLCJuYW1lcyI6W10sIm1hcHBpbmdzIjoiOzs7O0FBQUEsT0FBTyxFQUFFLFNBQVMsRUFBRSxZQUFZLEVBQUUsTUFBTSxFQUFFLE1BQU0sZUFBZSxDQUFDO0FBUWhFLE1BQU0sT0FBTyxhQUFhO0lBTDFCO1FBTUUsU0FBSSxHQUFVLEVBQUUsQ0FBQztRQUVQLGFBQVEsR0FBRyxJQUFJLFlBQVksRUFBRSxDQUFDO0lBYzFDLENBQUM7Ozs7O0lBWkMsTUFBTSxDQUFDLFlBQWlCO1FBQ3RCLElBQUksQ0FBQyxJQUFJLENBQUMsSUFBSSxDQUFDLE1BQU0sRUFBRTtZQUNyQixZQUFZLENBQUMsTUFBTSxHQUFHLEtBQUssQ0FBQztTQUM3QjtRQUNELElBQUksQ0FBQyxJQUFJLENBQUMsSUFBSSxDQUFDLFlBQVksQ0FBQyxDQUFDO0lBQy9CLENBQUM7Ozs7O0lBRUQsU0FBUyxDQUFDLFlBQWlCO1FBQ3pCLElBQUksQ0FBQyxJQUFJLENBQUMsR0FBRzs7OztRQUFDLEdBQUcsQ0FBQyxFQUFFLENBQUMsQ0FBQyxHQUFHLENBQUMsTUFBTSxHQUFHLElBQUksQ0FBQyxFQUFDLENBQUM7UUFDMUMsWUFBWSxDQUFDLE1BQU0sR0FBRyxLQUFLLENBQUM7UUFDNUIsSUFBSSxDQUFDLFFBQVEsQ0FBQyxJQUFJLENBQUMsWUFBWSxDQUFDLENBQUM7SUFDbkMsQ0FBQzs7O1lBckJGLFNBQVMsU0FBQztnQkFDVCxRQUFRLEVBQUUsYUFBYTtnQkFDdkIscVBBQW9DOzthQUVyQzs7O3VCQUlFLE1BQU07Ozs7SUFGUCw2QkFBaUI7O0lBRWpCLGlDQUF3QyIsInNvdXJjZXNDb250ZW50IjpbImltcG9ydCB7IENvbXBvbmVudCwgRXZlbnRFbWl0dGVyLCBPdXRwdXQgfSBmcm9tICdAYW5ndWxhci9jb3JlJztcbmltcG9ydCB7IFRhYiB9IGZyb20gJy4vdGFiLmludGVyZmFjZSc7XG5cbkBDb21wb25lbnQoe1xuICBzZWxlY3RvcjogJ3NoYXJlZC10YWJzJyxcbiAgdGVtcGxhdGVVcmw6ICcuL3RhYnMuY29tcG9uZW50Lmh0bWwnLFxuICBzdHlsZVVybHM6IFsnLi90YWJzLmNvbXBvbmVudC5zY3NzJ11cbn0pXG5leHBvcnQgY2xhc3MgVGFic0NvbXBvbmVudCB7XG4gIHRhYnM6IFRhYltdID0gW107XG5cbiAgQE91dHB1dCgpIHNlbGVjdGVkID0gbmV3IEV2ZW50RW1pdHRlcigpO1xuXG4gIGFkZFRhYih0YWJDb21wb25lbnQ6IFRhYikge1xuICAgIGlmICghdGhpcy50YWJzLmxlbmd0aCkge1xuICAgICAgdGFiQ29tcG9uZW50LmhpZGRlbiA9IGZhbHNlO1xuICAgIH1cbiAgICB0aGlzLnRhYnMucHVzaCh0YWJDb21wb25lbnQpO1xuICB9XG5cbiAgc2VsZWN0VGFiKHRhYkNvbXBvbmVudDogVGFiKSB7XG4gICAgdGhpcy50YWJzLm1hcCh0YWIgPT4gKHRhYi5oaWRkZW4gPSB0cnVlKSk7XG4gICAgdGFiQ29tcG9uZW50LmhpZGRlbiA9IGZhbHNlO1xuICAgIHRoaXMuc2VsZWN0ZWQuZW1pdCh0YWJDb21wb25lbnQpO1xuICB9XG59XG4iXX0=