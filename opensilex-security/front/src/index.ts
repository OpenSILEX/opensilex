import { ApiServiceBinder } from './lib';
import { client } from './lib/generated/client.gen';

const plugin = {
    install(app, options) {
        console.log("Install opensilex-security plugin");
        if (app?.config?.globalProperties?.$opensilex?.registerClient) {
            app.config.globalProperties.$opensilex.registerClient(client);
        }
        ApiServiceBinder.with(app.$opensilex.getServiceContainer());
    }
};

export default plugin;
export { ApiServiceBinder };
export * from './lib';


// export default {
//     install(app, options) {
//         console.log("Install opensilex-service plugin")
//         ApiServiceBinder.with(app.$opensilex.getServiceContainer());
//     }
// };

// window["opensilex-security"] = {
//     default: {
//         install(app) {
//             console.log("Install opensilex-security plugin");
//             ApiServiceBinder.with(app.$opensilex.getServiceContainer());
//         }
//     }
// };
