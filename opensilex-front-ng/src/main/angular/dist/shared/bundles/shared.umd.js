(function (global, factory) {
    typeof exports === 'object' && typeof module !== 'undefined' ? factory(exports, require('@angular/core'), require('@angular/common')) :
    typeof define === 'function' && define.amd ? define('shared', ['exports', '@angular/core', '@angular/common'], factory) :
    (global = global || self, factory(global.shared = {}, global.ng.core, global.ng.common));
}(this, function (exports, core, common) { 'use strict';

    /*! *****************************************************************************
    Copyright (c) Microsoft Corporation. All rights reserved.
    Licensed under the Apache License, Version 2.0 (the "License"); you may not use
    this file except in compliance with the License. You may obtain a copy of the
    License at http://www.apache.org/licenses/LICENSE-2.0

    THIS CODE IS PROVIDED ON AN *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION ANY IMPLIED
    WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A PARTICULAR PURPOSE,
    MERCHANTABLITY OR NON-INFRINGEMENT.

    See the Apache Version 2.0 License for specific language governing permissions
    and limitations under the License.
    ***************************************************************************** */

    function __read(o, n) {
        var m = typeof Symbol === "function" && o[Symbol.iterator];
        if (!m) return o;
        var i = m.call(o), r, ar = [], e;
        try {
            while ((n === void 0 || n-- > 0) && !(r = i.next()).done) ar.push(r.value);
        }
        catch (error) { e = { error: error }; }
        finally {
            try {
                if (r && !r.done && (m = i["return"])) m.call(i);
            }
            finally { if (e) throw e.error; }
        }
        return ar;
    }

    function __spread() {
        for (var ar = [], i = 0; i < arguments.length; i++)
            ar = ar.concat(__read(arguments[i]));
        return ar;
    }

    /**
     * @fileoverview added by tsickle
     * @suppress {checkTypes,constantProperty,extraRequire,missingOverride,missingReturn,unusedPrivateMembers,uselessCode} checked by tsc
     */
    var SharedComponent = /** @class */ (function () {
        function SharedComponent() {
        }
        SharedComponent.decorators = [
            { type: core.Component, args: [{
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
            this.selected = new core.EventEmitter();
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
            { type: core.Component, args: [{
                        selector: 'shared-tabs',
                        template: "<ul class=\"tabs\">\n  <li *ngFor=\"let tab of tabs\" (click)=\"selectTab(tab)\" class=\"tab\" [class.tab--active]=\"!tab.hidden\">\n      {{tab.title}}\n  </li>\n</ul>\n<div class=\"tab-body\">\n    <ng-content></ng-content>\n</div>",
                        styles: [":host{display:block}.tabs{display:flex;list-style:none;margin:0;padding:0;border-bottom:1px solid #ebeef2}.tab{position:relative;padding:0 20px;line-height:40px;cursor:pointer}.tab-body{padding:20px}.tab--active:before{content:\"\";position:absolute;bottom:0;left:0;right:0;height:3px;background:#03a9f4}"]
                    }] }
        ];
        TabsComponent.propDecorators = {
            selected: [{ type: core.Output }]
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
            { type: core.Component, args: [{
                        selector: 'shared-tab',
                        template: '<ng-content></ng-content>'
                    }] }
        ];
        /** @nocollapse */
        TabComponent.ctorParameters = function () { return [
            { type: TabsComponent }
        ]; };
        TabComponent.propDecorators = {
            title: [{ type: core.Input }],
            hidden: [{ type: core.HostBinding, args: ['hidden',] }]
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
            { type: core.Component, args: [{
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
            { type: core.NgModule, args: [{
                        imports: [common.CommonModule],
                        declarations: __spread(sharedComponents),
                        exports: __spread(sharedComponents)
                    },] }
        ];
        return SharedModule;
    }());

    exports.SharedComponent = SharedComponent;
    exports.SharedModule = SharedModule;
    exports.ɵa = ButtonComponent;
    exports.ɵb = TabComponent;
    exports.ɵc = TabsComponent;

    Object.defineProperty(exports, '__esModule', { value: true });

}));
//# sourceMappingURL=shared.umd.js.map
