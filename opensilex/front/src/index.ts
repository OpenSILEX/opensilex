import { ApiServiceBinder } from './lib';
import UserList from './components/UserList.vue';
import UserCreate from './components/UserCreate.vue';

export default {
    install(Vue, options) {
        ApiServiceBinder.with(Vue.$opensilex.getServiceContainer());
    },

    components: {
        "opensilex.UserList": UserList,
        "opensilex.UserCreate": UserCreate
    }
};
