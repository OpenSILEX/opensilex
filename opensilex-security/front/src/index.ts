import { ApiServiceBinder } from './lib'

export default {
    install(app, options) {
        console.log("Install opensilex-service plugin")
        ApiServiceBinder.with(app.$opensilex.getServiceContainer());
    }
};
