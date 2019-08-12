/**
 * @fileoverview added by tsickle
 * @suppress {checkTypes,constantProperty,extraRequire,missingOverride,missingReturn,unusedPrivateMembers,uselessCode} checked by tsc
 */
import { Component, HostBinding, Input } from '@angular/core';
import { TabsComponent } from './tabs.component';
export class TabComponent {
    /**
     * @param {?} tabsComponent
     */
    constructor(tabsComponent) {
        this.hidden = true;
        tabsComponent.addTab(this);
    }
}
TabComponent.decorators = [
    { type: Component, args: [{
                selector: 'shared-tab',
                template: '<ng-content></ng-content>'
            }] }
];
/** @nocollapse */
TabComponent.ctorParameters = () => [
    { type: TabsComponent }
];
TabComponent.propDecorators = {
    title: [{ type: Input }],
    hidden: [{ type: HostBinding, args: ['hidden',] }]
};
if (false) {
    /** @type {?} */
    TabComponent.prototype.title;
    /** @type {?} */
    TabComponent.prototype.hidden;
}
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoidGFiLmNvbXBvbmVudC5qcyIsInNvdXJjZVJvb3QiOiJuZzovL3NoYXJlZC8iLCJzb3VyY2VzIjpbImxpYi90YWJzL3RhYi5jb21wb25lbnQudHMiXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6Ijs7OztBQUFBLE9BQU8sRUFBRSxTQUFTLEVBQUUsV0FBVyxFQUFFLEtBQUssRUFBRSxNQUFNLGVBQWUsQ0FBQztBQUU5RCxPQUFPLEVBQUUsYUFBYSxFQUFFLE1BQU0sa0JBQWtCLENBQUM7QUFNakQsTUFBTSxPQUFPLFlBQVk7Ozs7SUFLdkIsWUFBWSxhQUE0QjtRQUZqQixXQUFNLEdBQUcsSUFBSSxDQUFDO1FBR25DLGFBQWEsQ0FBQyxNQUFNLENBQUMsSUFBSSxDQUFDLENBQUM7SUFDN0IsQ0FBQzs7O1lBWEYsU0FBUyxTQUFDO2dCQUNULFFBQVEsRUFBRSxZQUFZO2dCQUN0QixRQUFRLEVBQUUsMkJBQTJCO2FBQ3RDOzs7O1lBTFEsYUFBYTs7O29CQU9uQixLQUFLO3FCQUVMLFdBQVcsU0FBQyxRQUFROzs7O0lBRnJCLDZCQUF1Qjs7SUFFdkIsOEJBQXFDIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IHsgQ29tcG9uZW50LCBIb3N0QmluZGluZywgSW5wdXQgfSBmcm9tICdAYW5ndWxhci9jb3JlJztcbmltcG9ydCB7IFRhYiB9IGZyb20gJy4vdGFiLmludGVyZmFjZSc7XG5pbXBvcnQgeyBUYWJzQ29tcG9uZW50IH0gZnJvbSAnLi90YWJzLmNvbXBvbmVudCc7XG5cbkBDb21wb25lbnQoe1xuICBzZWxlY3RvcjogJ3NoYXJlZC10YWInLFxuICB0ZW1wbGF0ZTogJzxuZy1jb250ZW50PjwvbmctY29udGVudD4nXG59KVxuZXhwb3J0IGNsYXNzIFRhYkNvbXBvbmVudCBpbXBsZW1lbnRzIFRhYiB7XG4gIEBJbnB1dCgpIHRpdGxlOiBzdHJpbmc7XG5cbiAgQEhvc3RCaW5kaW5nKCdoaWRkZW4nKSBoaWRkZW4gPSB0cnVlO1xuXG4gIGNvbnN0cnVjdG9yKHRhYnNDb21wb25lbnQ6IFRhYnNDb21wb25lbnQpIHtcbiAgICB0YWJzQ29tcG9uZW50LmFkZFRhYih0aGlzKTtcbiAgfVxufVxuIl19