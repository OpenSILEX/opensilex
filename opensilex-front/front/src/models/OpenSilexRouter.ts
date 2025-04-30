import { User } from './User';
import { App, defineAsyncComponent } from 'vue';
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
    private $opensilex: OpenSilexVuePlugin;


    constructor(pathPrefix: string, app: App) {
        this.pathPrefix = pathPrefix;
        this.app = app;
        this.$opensilex = this.app.config.globalProperties.$opensilex;
        this.router = this.createRouter(User.ANONYMOUS());
        // const store = useStore();
        
    }
    
    public getSectionAttributes() {
        console.log("RETURN sectionAttributes ", this.sectionAttributes)
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

        console.log("Routes enregistrées - createRouter :", this.router.getRoutes());
        console.log("RETURN sectionAttributes ", this.sectionAttributes)
        


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

            // si deja log et veut aller sur /app : renvoi sur /dash
            if (isLoggedIn && to.path === '/' && !redirectTo) {
                console.log("🍫 deja log et veut aller sur /app -> renvoi /dash")
                return next({ path: '/dash', query: { redirect: redirectTo } }); 
            }

            console.log("redirectTo : ", redirectTo)
            // a la premiere co, au moment ou l'utilisateur se log et viens donc bien de /app : 
                // redirect soit sur /dash si pas d'historique, 
                // sinon renvoi sur la derniere page consulté
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
                
                console.log("to ", to, " from " , from)
            next(); // aucun des cas ? on laisse passer
        });
        //   console.log(" 😶‍🌫️ Routes enregistrées :", this.router.getRoutes().map(route => route.name));
           console.log("this.router ", this.router)

           this.router.afterEach((to, from, failure) => {
            if (failure) {
                console.log(  "failure");
                console.log(to, from, failure)
            }
          })
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
        console.log("🧨 compute menu route");
    
        const routes: Array<any> = [];
        const $opensilex: OpenSilexVuePlugin = this.$opensilex;
        const frontConfig = this.frontConfig;
    
        const loadComponent = (componentId: string) => {
            return defineAsyncComponent(() => this.getAsyncComponentLoader(componentId));
        };
    
        // 📌 Routes générales depuis frontConfig
        if (frontConfig) {
    
            // Route par défaut
            routes.push({
                path: "/",
                component: loadComponent(frontConfig.loginComponent),
                meta: { public: true }
            });
    
            for (const routeConfig of frontConfig.routes) {
                console.log("🛤️ Route from frontConfig", routeConfig);
                routes.push({
                    path: routeConfig.path,
                    name: routeConfig.name || undefined,
                    component: loadComponent(routeConfig.component),
                    // meta: routeConfig.meta || {}
                });
            }
        }
    
        // 📌 Routes dynamiques depuis le menu utilisateur à verifier si toujours necessaire (duplicata de declarations ?)
        if (this.userFrontConfig) {
            console.log("👤 userFrontConfig:", this.userFrontConfig);
            this.menu = this.buildMenu(this.userFrontConfig.menu, routes, user);
    
            const addMenuRoutes = (menuItems: any[]) => {
                menuItems.forEach(item => {
                    if (item.route) {
                        routes.push({
                            path: item.route.path,
                            name: item.id || undefined,
                            component: loadComponent(item.route.component),
                        });
                    }
                    if (item.children?.length) {
                        addMenuRoutes(item.children);
                    }
                });
            };
    
            addMenuRoutes(this.userFrontConfig.menu);
        }
    
        // 📌 Route "catch-all" (404) - idee de créer une page "not found"
        if (frontConfig?.notFoundComponent) {
            routes.push({
                path: "/:catchAll(.*)",
                name: "NotFound",
                component: loadComponent(frontConfig.notFoundComponent)
            });
        }

        console.log("✅ Final routes:", routes);
        return routes;
    }

    private getAsyncComponentLoader(componentId) {
            return new Promise((resolve, reject) => {
                let componentDef = ModuleComponentDefinition.fromString(componentId);
                // let override = $opensilex.themeConfig.componentOverrides[componentId];
                // if (override) {
                //     componentDef = ModuleComponentDefinition.fromString(override);
                // }

                this.$opensilex.loadComponentModule(componentDef)
                    .then(() => {
                        let component: any = this.app.component(componentDef.getId());
                        if (component) {
                            resolve(component)
                        } else {
                            let result = this.getAsyncComponentLoader( this.frontConfig.notFoundComponent);
                            if (result instanceof Promise) {
                                result
                                    .then(resolve)
                                    .catch((error) => {
                                        console.error(error);
                                        reject(error);

                                      });
                            } else {
                                console.error("result",result);
                                resolve(result);
                            }
                        }
                    })
                    .catch((error) => {
                        console.error(error);
                        reject(error);
                    });
            })
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
                    component: this.getAsyncComponentLoader(route.component)
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