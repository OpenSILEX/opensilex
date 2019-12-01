import { ApiServiceBinder } from './lib'

const OpensilexExtension = {
    install(Vue, options) {
        ApiServiceBinder.with(Vue.opensilex.getServiceContainer());
        
        Vue.opensilex.registerComponents([
            
        ]);
    }
};

export default OpensilexExtension;