import { User } from './User';
import { App } from 'vue';
import { ModuleComponentDefinition } from './ModuleComponentDefinition';
import {MenuItemDTO, FrontConfigDTO, UserFrontConfigDTO} from '../lib';
import {useStore} from 'vuex';
// import { createRouter, createWebHistory, Router, RouteRecordRaw } from 'vue-router';
import { createRouter, createWebHistory, NavigationGuardNext, type Router, type RouteRecordRaw } from 'vue-router';

// const { createRouter, createWebHistory, Router, RouteRecordRaw } = VueRouter;

import OpenSilexVuePlugin from './OpenSilexVuePlugin';
import store from './Store';

export class OpenSilexRouter {

    private frontConfig: FrontConfigDTO;
    private userFrontConfig: UserFrontConfigDTO;
    private menu: Array<MenuItemDTO> = [];
    private router: Router;
    private pathPrefix: string
    private PUBLIC_ROUTE: string = "public";
    private sectionAttributes : any = {};
    private app : App;

    constructor(pathPrefix: string, app: App) {
        this.pathPrefix = pathPrefix;
        this.app = app;
        this.router = this.createRouter(User.ANONYMOUS());
        // const store = useStore();
       
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
    
        console.log("this.router ", this.router)
        console.log("Routes avant beforeEach :", this.router.getRoutes().map(route => route.name));


        // this.router.beforeResolve(async (to, from) => {
        //     console.log ("😁 from ", from , " to : ", to)
        //     // si ce n'est pas faux - donc vrai - donc qu'il est log ET qu'on va pas vers test
        //     if ( store.state.user.isLoggedIn() && to.path !== '/test') {
        //       console.log("😀 if ")
        //       return { path: '/test' , replace : true}
        //     } else {
        //         console.log("😀 else ")
        //         // return { path: '/app'}
        //     }
        //   })
        //   this.refresh();
          

        this.router.beforeEach(async (to, from, next: NavigationGuardNext) => {
            console.log("routerBefore from  : ", from, " / to : ", to);

            const isLoggedIn = store.state.user.loggedIn;
            const redirectTo = to.query.redirect ? to.query.redirect.toString() : undefined;

            //////////////////////
            // si pas deja log et veut aller sur autre chose que /app : renvoi sur /app
            if (!isLoggedIn && to.path !== '/') {
                console.log ("🍫 pas deja log et veut aller sur autre chose que app -> renvoi /app")
                // return next({ path: this.pathPrefix }); 
                return next({ path: '/', query: { redirect: to.fullPath } });
            }

            // si deja log et veut aller sur /app : renvoi sur /test
            if (isLoggedIn && to.path === '/' && !redirectTo) {
                console.log("🍫 deja log et veut aller sur /app -> renvoi /test")
                return next({ path: '/test', query: { redirect: redirectTo } }); 
            }

            console.log("redirectTo : ", redirectTo)
            // a la premiere co, au moment ou l'utilisateur se log et viens donc bien de /app : 
                // redirect soit sur /test si pas d'historique, 
                // sinon renvoi sur la derniere page consulté
            // if (isLoggedIn && from.path === '/' && to.path !== '/') {
                if (isLoggedIn && to.path === '/' && redirectTo) {
            
                // to.redirectedFrom.query.redirect
                console.log("to.redirectedFrom : ", to.redirectedFrom)
                console.log("🍫 Redirection après login vers:", redirectTo);
                return next({ path: redirectTo });
            }


             // Vérification pour éviter la redirection infinie
            if (to.path === from.path) {
                console.log("🍫 déjà sur la même page, redirection annulée");
                return next(); 
            }

            next(); // aucun des cas ? on laisse passer
        });

        //   console.log(" 😶‍🌫️ Routes enregistrées :", this.router.getRoutes().map(route => route.name));

        return this.router;
    }

    // public resetRouter(user: User) {
    //     const newRouter: any = this.createRouter(user);
    //     this.router.matcher = newRouter.matcher;
    //     return this.router;
    // }
    
    public resetRouter(user: User) {
        const newRouter = this.createRouter(user);
        // this.router.getRoutes().forEach(route => this.router.removeRoute(route.name as string));
        // this.router.getRoutes().forEach(route => console.log("routes : " , routes));
        // newRouter.getRoutes().forEach(route => this.router.addRoute(route));  
        this.router = newRouter;
        return this.router
    }
    

    public computeMenuRoutes(user: User) {
        console.log("🧨 compute menu route")
        let routes: Array<any> = [];

        let $opensilex: OpenSilexVuePlugin = this.app.config.globalProperties.$opensilex ;
        let frontConfig = this.frontConfig;
        if (frontConfig != undefined) {
            console.log("frontConfigDefined")
            routes.push({
                path: "/",
                component: this.getAsyncComponentLoader($opensilex, frontConfig.loginComponent)
            });

            for (let routeConfig in frontConfig.routes) {
                let route = frontConfig.routes[routeConfig];
                // Crée la route en incluant la propriété name si elle est définie dans la config
                routes.push({
                  path: route.path,
                  name: route.name || undefined, // Si routeConfig.name est défini, l'utiliser, sinon undefined
                  component: this.getAsyncComponentLoader($opensilex, route.component),
                  meta: { public: true }
                });
              }

              console.log("🙃 routesz ", routes)
            // for (let i in frontConfig.routes) {
            //     let route = frontConfig.routes[i];

            //     if (user.hasAllCredentials(route.credentials)) {
            //         console.log("🧨 user have credentials")
            //         routes.push({
            //             path: route.path,
            //             name: route["name"],
            //             component: this.getAsyncComponentLoader($opensilex, route.component),
            //             meta:{public: false}
            //         });
            //     }
            //     if(route.credentials.includes(this.PUBLIC_ROUTE)){ 
            //         console.log("🧨 public routes")
            //         console.log(" 🧨 path : ", route.path, " component : ", route.component)
            //         routes.push({
            //             path: route.path,
            //             name: route["name"],
            //             component: this.getAsyncComponentLoader($opensilex, route.component),
            //             meta:{public: true}
            //         });
                    
            //     }
            // }

            // ??????
            // routes.push({
            //     // path: "*",
            //     path: "/:catchAll(.*)",
            //     component: this.getAsyncComponentLoader($opensilex, frontConfig.notFoundComponent)
            // });
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
                // let override = $opensilex.themeConfig.componentOverrides[componentId];
                // if (override) {
                //     componentDef = ModuleComponentDefinition.fromString(override);
                // }
                $opensilex.loadComponentModule(componentDef)
                    .then(() => {
                        let component: any = this.app.component(componentDef.getId());
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
        this.router.go(0);
        console.log( " 💩 ", this.router.currentRoute)
        // this.router.push(this.router.currentRoute.value.fullPath);

    }

    private buildMenu(items: Array<MenuItemDTO>, routes: Array<any>, user: User) {
        let $opensilex: OpenSilexVuePlugin = this.app["$opensilex"];
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