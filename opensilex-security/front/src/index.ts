import { ApiServiceBinder } from './lib';

const plugin = {
    install(app, options) {
        console.log("Install opensilex-security plugin");
        ApiServiceBinder.with(app.$opensilex.getServiceContainer());
    }
};

// Ne pas assigner manuellement à `window`
//  Garder uniquement l'export
export default plugin;
export { ApiServiceBinder };


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
