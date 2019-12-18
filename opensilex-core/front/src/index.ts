import { ApiServiceBinder } from './lib'
import FactorList from './components/factors/FactorList.vue';

export default {
    install(Vue, options) {
        ApiServiceBinder.with(Vue.$opensilex.getServiceContainer());
    },
    components: {
        "opensilex-core-FactorList": FactorList
    }
};