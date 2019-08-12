import { Component, Output, EventEmitter, Input, HostBinding, NgModule } from '@angular/core';
import { __spread } from 'tslib';
import { CommonModule } from '@angular/common';

/**
 * @fileoverview added by tsickle
 * @suppress {checkTypes,constantProperty,extraRequire,missingOverride,missingReturn,unusedPrivateMembers,uselessCode} checked by tsc
 */
var SharedComponent = /** @class */ (function () {
    function SharedComponent() {
    }
    SharedComponent.decorators = [
        { type: Component, args: [{
                    selector: 'shared-component',
                    template: "\n    <h4>\n      Shared component\n    </h4>\n  "
                }] }
    ];
    return SharedComponent;
}());

/**
 * @fileoverview added by tsickle
 * @suppress {checkTypes,constantProperty,extraRequire,missingOverride,missingReturn,unusedPrivateMembers,uselessCode} checked by tsc
 */
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

/**
 * @fileoverview added by tsickle
 * @suppress {checkTypes,constantProperty,extraRequire,missingOverride,missingReturn,unusedPrivateMembers,uselessCode} checked by tsc
 */
var TabComponent = /** @class */ (function () {
    function TabComponent(tabsComponent) {
        this.hidden = true;
        tabsComponent.addTab(this);
    }
    TabComponent.decorators = [
        { type: Component, args: [{
                    selector: 'shared-tab',
                    template: '<ng-content></ng-content>'
                }] }
    ];
    /** @nocollapse */
    TabComponent.ctorParameters = function () { return [
        { type: TabsComponent }
    ]; };
    TabComponent.propDecorators = {
        title: [{ type: Input }],
        hidden: [{ type: HostBinding, args: ['hidden',] }]
    };
    return TabComponent;
}());

/**
 * @fileoverview added by tsickle
 * @suppress {checkTypes,constantProperty,extraRequire,missingOverride,missingReturn,unusedPrivateMembers,uselessCode} checked by tsc
 */
var ButtonComponent = /** @class */ (function () {
    function ButtonComponent() {
    }
    ButtonComponent.decorators = [
        { type: Component, args: [{
                    // tslint:disable-next-line: component-selector
                    selector: 'button[sharedBtn]',
                    template: '<ng-content></ng-content>',
                    styles: [":host{padding:0 15px;border:1px solid #d8dde6;border-radius:6px;line-height:40px;cursor:pointer;background:#fff}:host:hover{background:#f3f7fb}"]
                }] }
    ];
    return ButtonComponent;
}());

/**
 * @fileoverview added by tsickle
 * @suppress {checkTypes,constantProperty,extraRequire,missingOverride,missingReturn,unusedPrivateMembers,uselessCode} checked by tsc
 */
/** @type {?} */
var sharedComponents = [SharedComponent, ButtonComponent, TabComponent, TabsComponent];
var SharedModule = /** @class */ (function () {
    function SharedModule() {
    }
    SharedModule.decorators = [
        { type: NgModule, args: [{
                    imports: [CommonModule],
                    declarations: __spread(sharedComponents),
                    exports: __spread(sharedComponents)
                },] }
    ];
    return SharedModule;
}());

export { SharedComponent, SharedModule, ButtonComponent as ɵa, TabComponent as ɵb, TabsComponent as ɵc };
//# sourceMappingURL=shared.js.map
