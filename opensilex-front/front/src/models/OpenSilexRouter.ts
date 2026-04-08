import { User } from './User';
import { App, defineAsyncComponent, markRaw } from 'vue';
import { ModuleComponentDefinition } from './ModuleComponentDefinition';
import { MenuItemDTO, FrontConfigDTO, UserFrontConfigDTO } from '../lib';
import { useStore } from 'vuex';
import { createRouter, createWebHistory, NavigationGuardNext, RouteLocationNormalized, type Router, type RouteRecordRaw } from 'vue-router';

import OpenSilexVuePlugin from './OpenSilexVuePlugin';
import store from './Store';

const PUBLIC_ROUTE: string = "public";
export const DEFAULT_ROUTE_NAME: string = "default";

export class OpenSilexRouter {

    private frontConfig: FrontConfigDTO;
    private userFrontConfig: UserFrontConfigDTO;
    private menu: Array<MenuItemDTO> = [];
    private router: Router;
    private pathPrefix: string;
    private sectionAttributes: any = {};
    private app: App;
    private $opensilex: OpenSilexVuePlugin;

    // Evite plusieurs initialisations simultanées de l'auth
    private authInitializationPromise: Promise<void> | null = null;

    // Indique si on a déjà tenté d'initialiser l'auth côté router
    private authInitializationDone: boolean = false;

    constructor(pathPrefix: string, app: App) {
        this.pathPrefix = pathPrefix;
        this.app = app;
        this.$opensilex = this.app.config.globalProperties.$opensilex;

        // Mark router as raw to prevent Vue from unwrapping its internal refs
        this.router = markRaw(this.createRouter(User.ANONYMOUS()));
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
        const routes: Array<RouteRecordRaw> = this.computeMenuRoutes(user);

        this.router = createRouter({
            history: createWebHistory(this.pathPrefix + "/app"),
            routes: routes,
        });

        // Add this line to handle navigation errors
        this.router.onError((handler) => {
            console.error("Navigation error:", handler);
        });

        this.router.beforeEach(async (to, from, next: NavigationGuardNext) => {
            try {
                const isLoggedIn = !!store.state.user.loggedIn;
                const redirectTo = typeof to.query.redirect === "string"
                    ? to.query.redirect
                    : undefined;

                const isPublicRoute = this.isPublicRoute(to);

                /**
                 * si pas deja log et veut aller sur autre chose que /app : renvoi sur /app
                 *  n ne bloque que les routes non publiques.
                 */
                if (!isLoggedIn && !isPublicRoute) {
                    return next({ path: "/", query: { redirect: to.fullPath } });
                }

                // si deja log et veut aller sur /app : renvoi sur /dash
                if (isLoggedIn && to.path === "/" && !redirectTo) {
                    return next({ path: "/dash" });
                }

                /** a la premiere co, au moment ou l'utilisateur se log et viens donc bien de /app :
                * redirect soit sur /dash si pas d'historique,
                * sinon renvoi sur la derniere page consulté
                */
                if (isLoggedIn && to.path === "/" && redirectTo) {
                    return next({ path: redirectTo });
                }

                // Vérification pour éviter la redirection infinie
                return next();

            } catch (error) {
                console.error("Router guard error:", error);

                // En cas d'erreur pendant l'initialisation de l'auth,
                // on redirige proprement vers la page d'entrée.
                return next({ path: "/" });
            }
        });

        this.router.afterEach((to, from, failure) => {
            if (failure) {
                console.log(to, from, failure);
            }
        });

        return this.router;
    }

    /**
     * Détermine si une route est publique.
     * On garde la page "/" comme point d'entrée public.
     * On gère aussi les routes marquées meta.public = true.
     */
    private isPublicRoute(to: RouteLocationNormalized): boolean {
        if (to.path === "/") {
            return true;
        }

        // Une 404 ne doit pas être considérée comme une route publique
        // dans le cas où l'utilisateur anonyme tente d'accéder à une ancienne route privée.
        if (to.name === "NotFound") {
            return false;
        }

        return to.matched.some(record => record.meta?.public === true);
    }

    public resetRouter(user: User) {
        // Navigate to default route BEFORE removing routes
        // Navigate to default route BEFORE removing routes
        // This ensures router.currentRoute.value remains valid during the reset
        // preventing DevTools errors when inspecting the router
        //
        // Ancien comportement volontairement désactivé :
        // this.router.replace('/').catch(() => {});
        //
        // En pratique, cette redirection forcée peut casser l'expérience utilisateur
        // et provoquer des retours non désirés vers le point d'entrée.

        // Instead of creating a new router, update routes dynamically
        // First, remove all existing routes except the base ones
        const currentRoutes = this.router.getRoutes();

        // Clear dynamic routes (keep only the initial routes)
        currentRoutes.forEach(route => {
            if (route.name && route.name !== "default" && route.name !== "NotFound") {
                this.router.removeRoute(route.name);
            }
        });

        // Compute new routes based on user permissions
        const newRoutes = this.computeMenuRoutes(user);

        // Add new routes to the existing router
        newRoutes.forEach(route => {
            // Skip routes that are already registered (default, NotFound)
            if (route.name) {
                if (!this.router.hasRoute(route.name)) {
                    this.router.addRoute(route);
                }
            } else {
                // Les routes sans nom doivent aussi être ajoutées,
                // sinon elles seront NotFound
                this.router.addRoute(route);
            }
        });

        return this.router;
    }

    public computeMenuRoutes(user: User): Array<RouteRecordRaw> {
        const routes: Array<RouteRecordRaw> = [];
        const frontConfig = this.frontConfig;

        // Reset propre des attributs de section avant reconstruction
        this.sectionAttributes = {};
        this.menu = [];

        // Function to load components asynchronously
        const loadComponent = (componentId: string) => {
            return async () => await this.getComponentImport(componentId);
        };

        // 📌 General routes from frontConfig
        if (frontConfig) {
            // Default route
            routes.push({
                path: "/",
                name: DEFAULT_ROUTE_NAME,
                component: loadComponent(frontConfig.loginComponent),
                meta: {
                    public: true
                }
            });

            for (const routeConfig of frontConfig.routes) {
                routes.push({
                    path: routeConfig.path,
                    name: routeConfig.name || undefined,
                    component: loadComponent(routeConfig.component),
                    meta: {
                        public: routeConfig.credentials?.includes(PUBLIC_ROUTE) === true
                    }
                });
            }
        }

        // 📌 Dynamic routes from user menu
        if (this.userFrontConfig) {
            this.menu = this.buildMenu(this.userFrontConfig.menu, user);

            const addMenuRoutes = (menuItems: Array<MenuItemDTO>) => {
                menuItems.forEach(item => {
                    if (item.route) {
                        routes.push({
                            path: item.route.path,
                            name: item.id || undefined,
                            component: loadComponent(item.route.component)
                        });
                    }

                    if (item.children && item.children.length > 0) {
                        addMenuRoutes(item.children);
                    }
                });
            };

            addMenuRoutes(this.userFrontConfig.menu);
        }

        // 📌 Catch-all route (404)
        if (frontConfig?.notFoundComponent) {
            routes.push({
                path: "/:catchAll(.*)",
                name: "NotFound",
                component: loadComponent(frontConfig.notFoundComponent),
                meta: { public: true }
            });
        }

        return routes;
    }

    // New method using dynamic imports instead of the complex promise-based loader
    private async getComponentImport(componentId: string): Promise<any> {
        try {
            const componentDef = ModuleComponentDefinition.fromString(componentId);

            // Load the component module using your existing method
            await this.$opensilex.loadComponentModule(componentDef);

            // Get the component from the app
            const component = this.app.component(componentDef.getId());

            if (component) {
                return component;
            } else {
                // Fallback to not found component
                if (this.frontConfig?.notFoundComponent) {
                    return this.getComponentImport(this.frontConfig.notFoundComponent);
                }

                throw new Error(`Component ${componentId} not found`);
            }
        } catch (error: any) {
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
                    <pre>${error?.message || error}</pre>
                </div>`
            };
        }
    }

    public refresh() {
        this.router.go(0);
    }

    private buildMenu(items: Array<MenuItemDTO>, user: User): Array<MenuItemDTO> {
        const menu: Array<MenuItemDTO> = [];

        for (const item of items) {
            if (item.route) {
                const route = item.route;
                menu.push(item);

                // Note: route registration is now handled in computeMenuRoutes
                this.sectionAttributes[route.path] = {
                    icon: route.icon,
                    title: route.title,
                    description: route.description
                };
            }

            let childItems: Array<MenuItemDTO> = [];
            if (item.children && item.children.length > 0) {
                childItems = this.buildMenu(item.children, user);
            }

            if (!item.route && childItems.length > 0) {
                // Create a new item to avoid mutating the original
                const newItem = { ...item };
                newItem.children = childItems;
                menu.push(newItem);
            }
        }
        return menu;
    }
}