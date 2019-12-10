import { User } from './User';
import { OpenSilexVuePlugin } from '@/models/OpenSilexVuePlugin';
import Vue from 'vue';
import { ModuleComponentDefinition } from '@/models/ModuleComponentDefinition';
import { MenuItemDTO, FrontConfigDTO } from '@/lib';
import VueRouter from 'vue-router';

export class OpenSilexRouter {

    private frontConfig: FrontConfigDTO;
    private menu = {};
    private router: any = this.createRouter(User.ANONYMOUS());

    public getMenu() {
        return this.menu;
    }

    public setConfig(config: FrontConfigDTO) {
        this.frontConfig = config;
    }

    public getRouter() {
        return this.router;
    }

    public createRouter(user: User) {
        let routes = this.computeMenuRoutes(user);

        this.router = new VueRouter({
            base: '/app',
            mode: 'history',
            routes: routes
        })

        return this.router;
    }

    public resetRouter(user: User) {
        const newRouter: any = this.createRouter(user);
        this.router.matcher = newRouter.matcher;
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

                if (user.hasAccess(route.access)) {
                    routes.push({
                        path: route.path,
                        component: this.getAsyncComponentLoader($opensilex, route.component)
                    });
                }
            }

            this.menu = {};
            this.walkMenuItems(frontConfig.menu, routes, user, this.menu, []);

            routes.push({
                path: "*",
                component: this.getAsyncComponentLoader($opensilex, frontConfig.notFoundComponent)
            });
        }

        console.log("Loaded Routes & Menu: ", routes, this.menu);
        return routes;
    }

    public getAsyncComponentLoader($opensilex, componentId) {
        return () => {
            return new Promise(function (resolve, reject) {
                let componentDef = ModuleComponentDefinition.fromString(componentId)
                $opensilex.loadComponentModule(componentDef)
                    .then(function () {
                        resolve(Vue.component(componentDef.getName()))
                    })
                    .catch(reject);
            })
        }
    }


    private walkMenuItems(items: Array<MenuItemDTO>, routes: Array<any>, user: User, menu: any, baseIds: Array<string>) {
        let $opensilex: OpenSilexVuePlugin = Vue["$opensilex"];

        for (let i in items) {
            let item: MenuItemDTO = items[i];

            let hasRoute = false;
            if (item.route) {
                let route = item.route;

                if (user.hasAccess(route.access)) {
                    hasRoute = true;
                    routes.push({
                        path: route.path,
                        component: this.getAsyncComponentLoader($opensilex, route.component)
                    });
                }
            }

            let localIds: Array<string> = [];
            localIds = localIds.concat(baseIds);
            localIds.push(item.id);
            let id = localIds.join("_");

            menu[id] = {
                id: item.id,
                label: item.label,
                path: null
            }

            if (hasRoute && item.route) {
                menu[id].path = item.route.path;
            }

            if (item.children.length > 0) {
                this.walkMenuItems(item.children, routes, user, menu, localIds);
            }
        }
    }

}