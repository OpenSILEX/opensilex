import { ApiServiceBinder } from './lib';
export default {
    install(Vue, options) {
        ApiServiceBinder.with(Vue.$opensilex.getServiceContainer());
    }
};