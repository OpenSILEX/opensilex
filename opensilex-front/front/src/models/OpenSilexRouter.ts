import { User } from './User';
import { App, defineAsyncComponent } from 'vue';
import { ModuleComponentDefinition } from './ModuleComponentDefinition';
import {MenuItemDTO, FrontConfigDTO, UserFrontConfigDTO} from '../lib';
import {useStore} from 'vuex';
import { createRouter, createWebHistory, NavigationGuardNext, type Router, type RouteRecordRaw } from 'vue-router';

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

        // Add this line to handle navigation errors
        this.router.onError((handler)=>{
            console.error('Navigation error:', handler); 
        });

        console.log("Routes enregistrées - createRouter :", this.router.getRoutes());
        console.log("RETURN sectionAttributes ", this.sectionAttributes)

        this.router.beforeEach(async (to, from, next: NavigationGuardNext) => {
            console.log("routerBefore from  : ", from, " / to : ", to);

            const isLoggedIn = store.state.user.loggedIn;
            const redirectTo = to.query.redirect ? to.query.redirect.toString() : undefined;

            //////////////////////
            // si pas deja log et veut aller sur autre chose que /app : renvoi sur /app
            if (!isLoggedIn && to.path !== '/') {
                console.log ("🍫 pas deja log et veut aller sur autre chose que app -> renvoi /app")
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

        console.log("this.router ", this.router)

        this.router.afterEach((to, from, failure) => {
            if (failure) {
                console.log("failure");
                console.log(to, from, failure)
            }
        })

        return this.router; 
    }
    
    public resetRouter(user: User) {
        const newRouter = this.createRouter(user);
        this.router = newRouter;
        return this.router
    }

    public computeMenuRoutes(user: User) {
        console.log("🧨 compute menu route");
    
        const routes: Array<any> = [];
        const $opensilex: OpenSilexVuePlugin = this.$opensilex;
        const frontConfig = this.frontConfig;
    
        // Updated loadComponent function using dynamic imports
        const loadComponent = (componentId: string) => {
            return  () => this.getComponentImport(componentId) 
        };
    
        // 📌 Routes générales depuis frontConfig
        if (frontConfig) {
            // Route par défaut
            routes.push({
                path: "/",
                name : "default",
                component: loadComponent(frontConfig.loginComponent),
                meta: { public: true }
            });
    
            for (const routeConfig of frontConfig.routes) {
                console.log("🛤️ Route from frontConfig", routeConfig);
                routes.push({
                    path: routeConfig.path,
                    name: routeConfig.name || undefined,
                    component: loadComponent(routeConfig.component),
                });
            }
        }
    
        // 📌 Routes dynamiques depuis le menu utilisateur
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
    
        // 📌 Route "catch-all" (404)
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

    // New method using dynamic imports instead of the complex promise-based loader
    private async getComponentImport(componentId: string): Promise<any> {
        try {
            let componentDef = ModuleComponentDefinition.fromString(componentId);
            
            // Load the component module using your existing method
            await this.$opensilex.loadComponentModule(componentDef);
            
            // Get the component from the app
            let component = this.app.component(componentDef.getId());
            console.log("getComponentImport",component)
            if (component) {
                return component;
            } else {
                // Fallback to not found component
                if (this.frontConfig?.notFoundComponent) {
                    return this.getComponentImport(this.frontConfig.notFoundComponent);
                }
                throw new Error(`Component ${componentId} not found`);
            }
        } catch (error) {
            console.error(`Error loading component ${componentId}:`, error);
            
            // Return a fallback component or rethrow
            if (this.frontConfig?.notFoundComponent && componentId !== this.frontConfig.notFoundComponent) {
                return this.getComponentImport(this.frontConfig.notFoundComponent);
            }
            
            // Return a minimal error component as last resort
            return {
                template: `<div class="error-component">
                    <h3>Component Error</h3>
                    <p>Failed to load component: ${componentId}</p>
                    <pre>${error.message}</pre>
                </div>`
            };
        }
    }

 

    public refresh() {
        this.router.go(0);
        console.log(" 💩 ", this.router.currentRoute)
    }

    private buildMenu(items: Array<MenuItemDTO>, routes: Array<any>, user: User) {
        let $opensilex: OpenSilexVuePlugin = this.app["$opensilex"];
        let menu: Array<MenuItemDTO> = [];
        
        for (let i in items) {
            let item: MenuItemDTO = items[i];

            if (item.route) {
                let route = item.route;
                menu.push(item);
                
                // Updated to use the new import method
                routes.push({
                    path: route.path,
                    component:  () => this.getComponentImport(route.component)
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