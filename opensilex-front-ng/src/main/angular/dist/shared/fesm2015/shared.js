import { Component, EventEmitter, Output, Input, HostBinding, NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * @fileoverview added by tsickle
 * @suppress {checkTypes,constantProperty,extraRequire,missingOverride,missingReturn,unusedPrivateMembers,uselessCode} checked by tsc
 */
class SharedComponent {
}
SharedComponent.decorators = [
    { type: Component, args: [{
                selector: 'shared-component',
                template: `
    <h4>
      Shared component
    </h4>
  `
            }] }
];

/**
 * @fileoverview added by tsickle
 * @suppress {checkTypes,constantProperty,extraRequire,missingOverride,missingReturn,unusedPrivateMembers,uselessCode} checked by tsc
 */
class TabsComponent {
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

/**
 * @fileoverview added by tsickle
 * @suppress {checkTypes,constantProperty,extraRequire,missingOverride,missingReturn,unusedPrivateMembers,uselessCode} checked by tsc
 */
class TabComponent {
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

/**
 * @fileoverview added by tsickle
 * @suppress {checkTypes,constantProperty,extraRequire,missingOverride,missingReturn,unusedPrivateMembers,uselessCode} checked by tsc
 */
class ButtonComponent {
}
ButtonComponent.decorators = [
    { type: Component, args: [{
                // tslint:disable-next-line: component-selector
                selector: 'button[sharedBtn]',
                template: '<ng-content></ng-content>',
                styles: [":host{padding:0 15px;border:1px solid #d8dde6;border-radius:6px;line-height:40px;cursor:pointer;background:#fff}:host:hover{background:#f3f7fb}"]
            }] }
];

/**
 * @fileoverview added by tsickle
 * @suppress {checkTypes,constantProperty,extraRequire,missingOverride,missingReturn,unusedPrivateMembers,uselessCode} checked by tsc
 */
/** @type {?} */
const sharedComponents = [SharedComponent, ButtonComponent, TabComponent, TabsComponent];
class SharedModule {
}
SharedModule.decorators = [
    { type: NgModule, args: [{
                imports: [CommonModule],
                declarations: [...sharedComponents],
                exports: [...sharedComponents]
            },] }
];

export { SharedComponent, SharedModule, ButtonComponent as ɵa, TabComponent as ɵb, TabsComponent as ɵc };
//# sourceMappingURL=shared.js.map
