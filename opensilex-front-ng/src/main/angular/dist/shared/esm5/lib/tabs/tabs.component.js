/**
 * @fileoverview added by tsickle
 * @suppress {checkTypes,constantProperty,extraRequire,missingOverride,missingReturn,unusedPrivateMembers,uselessCode} checked by tsc
 */
import { Component, EventEmitter, Output } from '@angular/core';
var TabsComponent = /** @class */ (function () {
    function TabsComponent() {
        this.tabs = [];
        this.selected = new EventEmitter();
    }
    /**
     * @param {?} tabComponent
     * @return {?}
     */
    TabsComponent.prototype.addTab = /**
     * @param {?} tabComponent
     * @return {?}
     */
    function (tabComponent) {
        if (!this.tabs.length) {
            tabComponent.hidden = false;
        }
        this.tabs.push(tabComponent);
    };
    /**
     * @param {?} tabComponent
     * @return {?}
     */
    TabsComponent.prototype.selectTab = /**
     * @param {?} tabComponent
     * @return {?}
     */
    function (tabComponent) {
        this.tabs.map((/**
         * @param {?} tab
         * @return {?}
         */
        function (tab) { return (tab.hidden = true); }));
        tabComponent.hidden = false;
        this.selected.emit(tabComponent);
    };
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
    return TabsComponent;
}());
export { TabsComponent };
if (false) {
    /** @type {?} */
    TabsComponent.prototype.tabs;
    /** @type {?} */
    TabsComponent.prototype.selected;
}
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoidGFicy5jb21wb25lbnQuanMiLCJzb3VyY2VSb290Ijoibmc6Ly9zaGFyZWQvIiwic291cmNlcyI6WyJsaWIvdGFicy90YWJzLmNvbXBvbmVudC50cyJdLCJuYW1lcyI6W10sIm1hcHBpbmdzIjoiOzs7O0FBQUEsT0FBTyxFQUFFLFNBQVMsRUFBRSxZQUFZLEVBQUUsTUFBTSxFQUFFLE1BQU0sZUFBZSxDQUFDO0FBR2hFO0lBQUE7UUFNRSxTQUFJLEdBQVUsRUFBRSxDQUFDO1FBRVAsYUFBUSxHQUFHLElBQUksWUFBWSxFQUFFLENBQUM7SUFjMUMsQ0FBQzs7Ozs7SUFaQyw4QkFBTTs7OztJQUFOLFVBQU8sWUFBaUI7UUFDdEIsSUFBSSxDQUFDLElBQUksQ0FBQyxJQUFJLENBQUMsTUFBTSxFQUFFO1lBQ3JCLFlBQVksQ0FBQyxNQUFNLEdBQUcsS0FBSyxDQUFDO1NBQzdCO1FBQ0QsSUFBSSxDQUFDLElBQUksQ0FBQyxJQUFJLENBQUMsWUFBWSxDQUFDLENBQUM7SUFDL0IsQ0FBQzs7Ozs7SUFFRCxpQ0FBUzs7OztJQUFULFVBQVUsWUFBaUI7UUFDekIsSUFBSSxDQUFDLElBQUksQ0FBQyxHQUFHOzs7O1FBQUMsVUFBQSxHQUFHLElBQUksT0FBQSxDQUFDLEdBQUcsQ0FBQyxNQUFNLEdBQUcsSUFBSSxDQUFDLEVBQW5CLENBQW1CLEVBQUMsQ0FBQztRQUMxQyxZQUFZLENBQUMsTUFBTSxHQUFHLEtBQUssQ0FBQztRQUM1QixJQUFJLENBQUMsUUFBUSxDQUFDLElBQUksQ0FBQyxZQUFZLENBQUMsQ0FBQztJQUNuQyxDQUFDOztnQkFyQkYsU0FBUyxTQUFDO29CQUNULFFBQVEsRUFBRSxhQUFhO29CQUN2QixxUEFBb0M7O2lCQUVyQzs7OzJCQUlFLE1BQU07O0lBY1Qsb0JBQUM7Q0FBQSxBQXRCRCxJQXNCQztTQWpCWSxhQUFhOzs7SUFDeEIsNkJBQWlCOztJQUVqQixpQ0FBd0MiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgeyBDb21wb25lbnQsIEV2ZW50RW1pdHRlciwgT3V0cHV0IH0gZnJvbSAnQGFuZ3VsYXIvY29yZSc7XG5pbXBvcnQgeyBUYWIgfSBmcm9tICcuL3RhYi5pbnRlcmZhY2UnO1xuXG5AQ29tcG9uZW50KHtcbiAgc2VsZWN0b3I6ICdzaGFyZWQtdGFicycsXG4gIHRlbXBsYXRlVXJsOiAnLi90YWJzLmNvbXBvbmVudC5odG1sJyxcbiAgc3R5bGVVcmxzOiBbJy4vdGFicy5jb21wb25lbnQuc2NzcyddXG59KVxuZXhwb3J0IGNsYXNzIFRhYnNDb21wb25lbnQge1xuICB0YWJzOiBUYWJbXSA9IFtdO1xuXG4gIEBPdXRwdXQoKSBzZWxlY3RlZCA9IG5ldyBFdmVudEVtaXR0ZXIoKTtcblxuICBhZGRUYWIodGFiQ29tcG9uZW50OiBUYWIpIHtcbiAgICBpZiAoIXRoaXMudGFicy5sZW5ndGgpIHtcbiAgICAgIHRhYkNvbXBvbmVudC5oaWRkZW4gPSBmYWxzZTtcbiAgICB9XG4gICAgdGhpcy50YWJzLnB1c2godGFiQ29tcG9uZW50KTtcbiAgfVxuXG4gIHNlbGVjdFRhYih0YWJDb21wb25lbnQ6IFRhYikge1xuICAgIHRoaXMudGFicy5tYXAodGFiID0+ICh0YWIuaGlkZGVuID0gdHJ1ZSkpO1xuICAgIHRhYkNvbXBvbmVudC5oaWRkZW4gPSBmYWxzZTtcbiAgICB0aGlzLnNlbGVjdGVkLmVtaXQodGFiQ29tcG9uZW50KTtcbiAgfVxufVxuIl19