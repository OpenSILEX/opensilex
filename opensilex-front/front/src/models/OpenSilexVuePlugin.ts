import { ApiServiceBinder, IAPIConfiguration, FrontConfigDTO } from '../lib';
import { Container } from 'inversify';
import IHttpClient from '../lib/IHttpClient';
import HttpClient from '../lib/HttpClient';
import { ModuleComponentDefinition } from './ModuleComponentDefinition';
import Vue from 'vue';
import { User } from './User';
import { Store } from 'vuex';
import { VueCookies } from 'vue-cookies'
import VueI18n from 'vue-i18n';
declare var $cookies: VueCookies;

declare var window: any;

export default class OpenSilexVuePlugin {

    private container: Container;
    private baseApi: string;
    private config: FrontConfigDTO;
    public $store: Store<any>;
    public $i18n: VueI18n;
    public $bvToast: any;

    constructor(baseApi: string, store: Store<any>, i18n: VueI18n) {
        this.container = new Container();
        this.container.bind<IHttpClient>("IApiHttpClient").to(HttpClient).inSingletonScope();
        this.container.bind<IAPIConfiguration>("IAPIConfiguration").toConstantValue({
            basePath: baseApi
        });
        this.baseApi = baseApi;
        this.$store = store;
        this.$i18n = i18n;
        ApiServiceBinder.with(this.container);
    }

    getUser() {
        return this.$store.state.user;
    }

    getBaseAPI() {
        return this.baseApi;
    }

    getResourceURI(path: string): string {
        if (this.config.themeModule && this.config.themeName) {
            let resourceURI = this.baseApi + "/front/theme/" + encodeURIComponent(this.config.themeModule) + "/" + encodeURIComponent(this.config.themeName) + "/resource";
            return resourceURI + "?filePath=" + encodeURIComponent(path);
        } else {
            return "/app/" + path;
        }
    }

    setConfig(config: FrontConfigDTO) {
        this.config = config;
    }

    getConfig() {
        return this.config;
    }

    showLoader() {
        this.$store.commit("showLoader");
    }

    hideLoader() {
        this.$store.commit("hideLoader");
    }

    public install(Vue, options) {
        Vue.prototype.$opensilex = this;
        Vue.$opensilex = this;
    }

    public loadService<T>(id: string): Promise<T> {
        return new Promise((resolve, reject) => {
            try {
                let result: T | null = this.getServiceSync(id);
                if (result == null) {
                    let idParts = this.parseServiceId(id);
                    this.loadModule(idParts.module)
                        .then(() => {
                            resolve(this.getService(id));
                        })
                        .catch(reject)
                } else {
                    resolve(result);
                }
            } catch (error) {
                reject(error);
            }
        });

    }

    public getService<T>(id: string): T {
        let result: T | null = this.getServiceSync(id);
        if (result == null) {
            let errorMessage = "Module is not loaded for service " + id;
            console.error(errorMessage);
            throw new Error(errorMessage);
        }

        return result;
    }

    public parseServiceId(id) {
        let idParts = id.split(".");
        if (idParts.length == 1) {
            return {
                module: null,
                service: idParts[0]
            };
        } else if (idParts.length == 2) {
            return {
                module: idParts[0],
                service: idParts[1]
            };
        } else {
            let errorMessage = "Invalid service identifier: " + id;
            console.error(errorMessage);
            throw new Error(errorMessage);
        }
    }

    public getServiceSync<T>(id: string): T | null {
        console.debug("Get API service", this.baseApi, id);
        let idParts = this.parseServiceId(id);
        if (idParts.module == null) {
            return this.getServiceContainer().get<T>(idParts.service);
        } else {
            if (this.loadedModules.indexOf(idParts.module) >= 0) {
                return this.getServiceContainer().get<T>(idParts.service);
            } else {
                return null;
            }
        }
    }

    private loadedModules: Array<string> = [
        "opensilex", "opensilex-front"

    ];

    private loadingModules = {
        "opensilex": Promise.resolve(null),
        "opensilex-front": Promise.resolve(null)
    };

    public loadModules(modules: Array<string>) {
        let promises: Array<Promise<any>> = [];
        for (let i in modules) {
            let moduleName = modules[i];

            if (!this.loadingModules[moduleName]) {
                this.loadingModules[moduleName] = this.loadModule(moduleName);
            }

            promises.push(this.loadingModules[moduleName])
        }

        return Promise.all(promises);
    }

    public loadComponentModules(components: Array<ModuleComponentDefinition>) {
        let promises: Array<Promise<any>> = [];

        for (let i in components) {
            promises.push(this.loadComponentModule(components[i]))
        }

        return Promise.all(promises);
    }

    public loadComponentModule(componentDef: ModuleComponentDefinition) {
        console.debug("Load component", componentDef.getId());
        let moduleName = componentDef.getModule();

        if (!this.loadingModules[moduleName]) {
            this.loadingModules[moduleName] = this.loadModule(moduleName);
        }

        if (this.loadingModules[moduleName] instanceof Promise) {
            return this.loadingModules[moduleName];
        }

        return Promise.resolve(this.loadingModules[moduleName]);
    }

    private loadTranslations(lang) {
        for (let langId in lang) {
            let translations: any = lang[langId];
            this.$i18n.mergeLocaleMessage(langId, translations);
        }
    }

    public loadModule(name) {
        if (window[name]) return window[name];

        console.debug("Load module", name);
        this.showLoader();
        let url = this.baseApi + "/front/extension/js/" + name + ".js";
        let cssURI = this.baseApi + "/front/extension/css/" + name + ".css";
        let self = this;

        var link = document.createElement('link');
        link.setAttribute("rel", "stylesheet");
        link.setAttribute("type", "text/css");
        link.setAttribute("href", cssURI);
        document.getElementsByTagName("head")[0].appendChild(link);

        window[name] = new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.async = true;
            script.addEventListener('load', () => {
                self.loadedModules.push(name);
                const plugin = window[name].default;
                Vue.use(plugin);

                if (plugin.lang) {
                    self.loadTranslations(plugin.lang);
                }

                self.initAsyncComponents(plugin.components)
                    .then(function (_module) {
                        self.hideLoader();
                        resolve(_module);
                    })
                    .catch(function (error) {
                        self.hideLoader();
                        reject(error);
                    });
            });
            script.addEventListener('error', () => {
                self.hideLoader();
                reject(new Error(`Error loading ${url}`));
            });
            script.src = url;
            document.head.appendChild(script);
        });

        return window[name];
    }

    public initAsyncComponents(components) {
        let promises: Array<Promise<any>> = [];
        if (components) {
            for (let componentId in components) {
                let component = components[componentId];
                if (component.asyncInit) {
                    try {
                        console.debug("Start component async init...", componentId);
                        promises.push(component.asyncInit(this));
                    } catch (error) {
                        promises.push(Promise.reject(error));
                    }
                }
                console.debug("Register component", componentId, component);
                Vue.component(componentId, components[componentId]);
            }
        }

        return new Promise((resolve, reject) => {
            Promise.all(promises)
                .then(() => {
                    console.debug("All components in module are initialized !");
                    resolve(window[name]);
                })
                .catch(reject);
        });

    }
    public getServiceContainer() {
        return this.container;
    }

    public get user(): User {
        return this.$store.state.user;
    }


    private static COOKIE_NAME = "opensilex-token";

    private cookieSuffix: string = "";

    public setCookieSuffix(suffix: string) {
        this.cookieSuffix = Math.abs(OpenSilexVuePlugin.hashCode(suffix)) + "";
    }

    private getCookieName() {
        let cookieName = OpenSilexVuePlugin.COOKIE_NAME + "-" + this.cookieSuffix;
        console.debug("Read cookie name:", cookieName);
        return cookieName;
    }

    public clearCookie() {
        $cookies.remove(this.getCookieName());
    }

    public loadUserFromCookie(): User {
        let token = $cookies.get(this.getCookieName());
        console.debug("Loaded token from cookie", token, this.getCookieName());
        let user: User = User.ANONYMOUS();
        if (token != null) {
            try {
                user = User.fromToken(token);
                this.setCookieValue(user);
            } catch (error) {
                console.error(error);
            }
        }

        return user;
    }

    public setCookieValue(user: User) {
        let secure: boolean = ('https:' == document.location.protocol);
        console.debug("Set cookie value:", this.getCookieName(), user.getToken());
        $cookies.set(this.getCookieName(), user.getToken(), user.getExpiration() + "s", "/", undefined, secure);
    }

    public static hashCode(str: string) {
        let hash = 0;
        if (str.length === 0) return hash;
        for (let i = 0; i < str.length; i++) {
            let chr = str.charCodeAt(i);
            hash = ((hash << 5) - hash) + chr;
            hash |= 0; // Convert to 32bit integer
        }
        return hash;
    }

    private handleError(error, message?) {
        switch (error.status) {
            case 400:
                console.error("Constraint validation error", error);
                this.handleConstraintError(error, message);
                break;
            case 401:
                console.error("Unhautorized error", error);
                this.handleUnauthorizedError(error, message);
                this.$store.commit("logout");
                break;
            case 403:
                console.error("Forbidden error", error);
                this.handleForbiddenError(error, message);
                break;
            case 500:
                console.error("Internal server error", error);
                this.handleServerError(error, message);
                break;
            default:
                console.error("Unhandled error", error);
                this.handleUnexpectedError(error, message);
                break;
        }
    }

    public errorHandler = this.handleError.bind(this);

    public handleConstraintError(error, message?) {
        if (message == null) {
            message = this.$i18n.t("component.common.errors.constraint-error");
        }
        this.showErrorToast(message);
    }

    public handleForbiddenError(error, message?) {
        if (message == null) {
            message = this.$i18n.t("component.common.errors.forbidden-error");
        }
        console.warn(message);
        this.showErrorToast(message);
    }

    public handleServerError(error, message?) {
        if (message == null) {
            message = this.$i18n.t("component.common.errors.server-error");
        }
        this.showErrorToast(message);
    }

    public handleUnexpectedError(error, message?) {
        if (message == null) {
            message = this.$i18n.t("component.common.errors.unexpected-error");
        }
        this.showErrorToast(message);
    }

    public handleUnauthorizedError(error, message?) {
        if (message == null) {
            message = this.$i18n.t("component.common.errors.unauthorized-error");
        }
        this.showErrorToast(message);
    }

    public showErrorToast(message: string) {
        this.showToast(message, {
            variant: "danger",
            toaster: "b-toaster-bottom-full",
            solid: true,
            title: this.$i18n.t("component.common.errors.error-title")
        });
    }

    public showToast(message: string, options: any) {
        this.$bvToast.toast(message, options);
    }
}