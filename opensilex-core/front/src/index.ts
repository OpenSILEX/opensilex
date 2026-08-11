import { ApiServiceBinder } from './lib';
import { client } from './lib/generated/client.gen';

export default {
    install(app, options) {
        if (app?.config?.globalProperties?.$opensilex?.registerClient) {
            app.config.globalProperties.$opensilex.registerClient(client);
        }
        ApiServiceBinder.with(app.$opensilex.getServiceContainer());
    }
};
export * from './lib';
