import { ApiServiceBinder, IAPIConfiguration } from '@/lib';
import { Container } from 'inversify';
import IHttpClient from '@/lib/IHttpClient';
import HttpClient from '@/lib/HttpClient';

export class ModuleFrontVuePlugin {

    private container: Container;

    constructor(baseApi: string) {
        this.container = new Container();
        this.container.bind<IHttpClient>("IApiHttpClient").to(HttpClient).inSingletonScope();
        this.container.bind<IAPIConfiguration>("IAPIConfiguration").toConstantValue({
            basePath: baseApi
        });

        ApiServiceBinder.with(this.container);
    }

    getService<T>(id: string): T {
        return this.getServiceContainer().get<T>(id);
    }

    callLog(str) {
        console.log(str);
    }

    getServiceContainer() {
        return this.container;
    }

    install(Vue, options) {
        Vue.opensilex = this;

        // Vue.opensilex.registerComponents([

        // ]);
    }
}