import { ApiServiceBinder, IAPIConfiguration } from '@/lib';
import { Container } from 'inversify';
import IHttpClient from '@/lib/IHttpClient';
import HttpClient from '@/lib/HttpClient';
import { ModuleComponentDefinition } from './ModuleComponentDefinition';
import Vue from 'vue';

declare var window: any;

export class OpenSilexVuePlugin {

    private container: Container;
    private baseApi: string;

    constructor(baseApi: string) {
        this.container = new Container();
        this.container.bind<IHttpClient>("IApiHttpClient").to(HttpClient).inSingletonScope();
        this.container.bind<IAPIConfiguration>("IAPIConfiguration").toConstantValue({
            basePath: baseApi
        });
        this.baseApi = baseApi;
        ApiServiceBinder.with(this.container);
    }

    getService<T>(id: string): T {
        let idParts = id.split("#");
        if (idParts.length == 1) {
            return this.getServiceContainer().get<T>(id);
        } else if (idParts.length == 2) {
            let moduleName = idParts[0];
            let serviceName = idParts[1];
            if (this.loadedModules.indexOf(moduleName) >= 0) {
                return this.getServiceContainer().get<T>(serviceName);
            } else {
                throw new Error("Module is not loaded: " + moduleName + " for service: " + serviceName);
            }
        } else {
            throw new Error("Invalid service id: " + id);
        }

    }

    private loadedModules: Array<string> = [];

    private loadingModules = {
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
        let moduleName = componentDef.getModule();

        if (!this.loadingModules[moduleName]) {
            this.loadingModules[moduleName] = this.loadModule(moduleName);
        }

        return this.loadingModules[moduleName];
    }

    public loadModule(name) {
        let url = this.baseApi + "/front/extension/" + name + ".js";
        if (window[name]) return window[name];

        window[name] = new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.async = true;
            script.addEventListener('load', () => {
                Vue.use(window[name].default);
                this.loadedModules.push(name);
                resolve(window[name]);
            });
            script.addEventListener('error', () => {
                reject(new Error(`Error loading ${url}`));
            });
            script.src = url;
            document.head.appendChild(script);
        });

        return window[name];
    }

    callLog(str) {
        console.log(str);
    }

    getServiceContainer() {
        return this.container;
    }

    install(Vue, options) {
        Vue.prototype.$opensilex = this;
        Vue.$opensilex = this;
    }
}