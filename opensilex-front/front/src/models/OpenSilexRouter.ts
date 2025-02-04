import { User } from './User';
import { ModuleComponentDefinition } from './ModuleComponentDefinition';
import { MenuItemDTO, FrontConfigDTO, UserFrontConfigDTO } from '../lib';
import { createRouter, createWebHistory, Router, RouteRecordRaw } from 'vue-router';
import OpenSilexVuePlugin from './OpenSilexVuePlugin';

export class OpenSilexRouter {

    private frontConfig: FrontConfigDTO;
    private userFrontConfig: UserFrontConfigDTO;
    private menu: Array<MenuItemDTO> = [];
    private router: Router;
    private pathPrefix: string;
    private PUBLIC_ROUTE: string = "public";
    private sectionAttributes: any = {};

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

    public getRouter(): Router {
        return this.router;
    }

    private createRouter(user: User): Router {
        const routes = this.computeMenuRoutes(user);
        
        return createRouter({
            history: createWebHistory("/app"),
            routes: routes
        });
    }

    public resetRouter(user: User): Router {
        this.router = this.createRouter(user);
        return this.router;
    }

    public computeMenuRoutes(user: User): Array<RouteRecordRaw> {
        const routes: Array<RouteRecordRaw> = [];

        const $opensilex: OpenSilexVuePlugin = (globalThis as any).$opensilex;
        const frontConfig = this.frontConfig;

        if (frontConfig) {
            routes.push({
                path: "/",
                component: this.getAsyncComponentLoader($opensilex, frontConfig.homeComponent)
            });

            for (const route of frontConfig.routes) {
                if (user.hasAllCredentials(route.credentials)) {
                    routes.push({
                        path: route.path,
                        component: this.getAsyncComponentLoader($opensilex, route.component),
                        meta: { public: false }
                    });
                }

                if (route.credentials.includes(this.PUBLIC_ROUTE)) {
                    routes.push({
                        path: route.path,
                        component: this.getAsyncComponentLoader($opensilex, route.component),
                        meta: { public: true }
                    });
                }
            }

            routes.push({
                path: "*",
                component: this.getAsyncComponentLoader($opensilex, frontConfig.notFoundComponent)
            });
        }

        if (this.userFrontConfig) {
            this.menu = this.buildMenu(this.userFrontConfig.menu, routes, user);
        }

        return routes;
    }

    private getAsyncComponentLoader($opensilex: OpenSilexVuePlugin, componentId: string) {
        return () => {
            return new Promise((resolve, reject) => {
                let componentDef = ModuleComponentDefinition.fromString(componentId);
                // const override = $opensilex.themeConfig.componentOverrides[componentId];
                const override = $opensilex.getThemeConfig()?.componentOverrides[componentId];

                
                if (override) {
                    componentDef = ModuleComponentDefinition.fromString(override);
                }

                $opensilex.loadComponentModule(componentDef)
                    .then(() => {
                        const component: any = (globalThis as any).Vue?.component(componentDef.getId());
                        if (component) {
                            resolve(component);
                        } else {
                            const result = this.getAsyncComponentLoader($opensilex, this.frontConfig.notFoundComponent)();
                            if (result instanceof Promise) {
                                result.then(resolve).catch(reject);
                            } else {
                                resolve(result);
                            }
                        }
                    })
                    .catch(reject);
            });
        };
    }

    public refresh() {
        this.router.go(0);
    }

    private buildMenu(items: Array<MenuItemDTO>, routes: Array<RouteRecordRaw>, user: User): Array<MenuItemDTO> {
        const $opensilex: OpenSilexVuePlugin = (globalThis as any).$opensilex;
        const menu: Array<MenuItemDTO> = [];

        for (const item of items) {
            if (item.route) {
                const route = item.route;
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

            if (item.children.length > 0) {
                const childItems = this.buildMenu(item.children, routes, user);
                if (!item.route && childItems.length > 0) {
                    item.children = childItems;
                    menu.push(item);
                }
            }
        }

        return menu;
    }
}