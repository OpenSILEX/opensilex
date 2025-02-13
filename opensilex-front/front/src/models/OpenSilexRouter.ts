import { User } from './User';
import Vue from 'vue';
import { ModuleComponentDefinition } from './ModuleComponentDefinition';
import {MenuItemDTO, FrontConfigDTO, UserFrontConfigDTO} from '../lib';
// import { createRouter, createWebHistory, Router, RouteRecordRaw } from 'vue-router';
import { createRouter, createWebHistory, type Router, type RouteRecordRaw } from 'vue-router';

// const { createRouter, createWebHistory, Router, RouteRecordRaw } = VueRouter;

import OpenSilexVuePlugin from './OpenSilexVuePlugin';

export class OpenSilexRouter {

    private frontConfig: FrontConfigDTO;
    private userFrontConfig: UserFrontConfigDTO;
    private menu: Array<MenuItemDTO> = [];
    private router: Router;
    private pathPrefix: string
    private PUBLIC_ROUTE: string = "public";
    private sectionAttributes : any = {};

    constructor(pathPrefix: string) {
        this.pathPrefix = pathPrefix;
        this.router = this.createRouter(User.ANONYMOUS());
    }
    
    public getSectionAttributes() {
        return this.sectionAttributes;
    }

    public getMenu() {
        return this.menu;
    }

    public setConfig(config: FrontConfigDTO) {
        this.frontConfig = config;
    }

    public setUserConfig(userConfig: UserFrontConfigDTO) {
        this.userFrontConfig = userConfig;
    }

    public getRouter() {
        return this.router;
    }

    private createRouter(user: User) {
        let routes: Array<RouteRecordRaw> = this.computeMenuRoutes(user);
    
        this.router = createRouter({
            history: createWebHistory(this.pathPrefix + "/app"),
            routes: routes,
        });
    
        return this.router;
    }

    // public resetRouter(user: User) {
    //     const newRouter: any = this.createRouter(user);
    //     this.router.matcher = newRouter.matcher;
    //     return this.router;
    // }
    
    public resetRouter(user: User) {
        const newRouter = this.createRouter(user);
        this.router.getRoutes().forEach(route => this.router.removeRoute(route.name as string));
        newRouter.getRoutes().forEach(route => this.router.addRoute(route));
    }
    

    public computeMenuRoutes(user: User) {
        let routes: Array<any> = [];

        let $opensilex: OpenSilexVuePlugin = Vue["$opensilex"];
        let frontConfig = this.frontConfig;
        if (frontConfig != undefined) {
            routes.push({
                path: "/",
                component: this.getAsyncComponentLoader($opensilex, frontConfig.homeComponent)
            });

            for (let i in frontConfig.routes) {
                let route = frontConfig.routes[i];

                if (user.hasAllCredentials(route.credentials)) {
                    routes.push({
                        path: route.path,
                        component: this.getAsyncComponentLoader($opensilex, route.component),
                        meta:{public: false}
                    });
                }
                if(route.credentials.includes(this.PUBLIC_ROUTE)){ 
                    routes.push({
                        path: route.path,
                        component: this.getAsyncComponentLoader($opensilex, route.component),
                        meta:{public: true}
                    });
                    
                }
            }

            
            routes.push({
                // path: "*",
                path: "/:catchAll(.*)",
                component: this.getAsyncComponentLoader($opensilex, frontConfig.notFoundComponent)
            });
        }

        if (this.userFrontConfig) {
            this.menu = this.buildMenu(this.userFrontConfig.menu, routes, user);
        }

        return routes;
    }

    private getAsyncComponentLoader($opensilex, componentId) {
        return () => {
            return new Promise((resolve, reject) => {
                let componentDef = ModuleComponentDefinition.fromString(componentId);
                let override = $opensilex.themeConfig.componentOverrides[componentId];
                if (override) {
                    componentDef = ModuleComponentDefinition.fromString(override);
                }
                $opensilex.loadComponentModule(componentDef)
                    .then(() => {
                        let component: any = Vue.component(componentDef.getId());
                        if (component) {
                            resolve(component)
                        } else {
                            let result = this.getAsyncComponentLoader($opensilex, this.frontConfig.notFoundComponent)();
                            if (result instanceof Promise) {
                                result
                                    .then(resolve)
                                    .catch(reject);
                            } else {
                                resolve(result);
                            }
                        }
                    })
                    .catch(reject);
            })
        }
    }

    public refresh() {
        // this.router.go();
        this.router.push(this.router.currentRoute.value.fullPath);

    }

    private buildMenu(items: Array<MenuItemDTO>, routes: Array<any>, user: User) {
        let $opensilex: OpenSilexVuePlugin = Vue["$opensilex"];
        let menu: Array<MenuItemDTO> = [];
        for (let i in items) {
            let item: MenuItemDTO = items[i];


            
            if (item.route) {
                let route = item.route;
                menu.push(item);
                routes.push({
                    path: route.path,
                    component: this.getAsyncComponentLoader($opensilex, route.component)
                });
                this.sectionAttributes[route.path] = {
                    icon: route.icon,
                    title: route.title,
                    description: route.description
                };
            }

            let childItems: Array<MenuItemDTO> = [];
            if (item.children.length > 0) {
                childItems = this.buildMenu(item.children, routes, user);
            }

            if (!item.route && childItems.length > 0) {
                item.children = childItems;
                menu.push(item);
            }
        }

        return menu;
    }
}