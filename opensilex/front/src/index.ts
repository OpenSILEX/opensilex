import { ApiServiceBinder } from './lib';
import UserList from './components/UserList.vue';
import UserForm from './components/UserForm.vue';
import UserView from './components/UserView.vue';

export default {
    install(Vue, options) {
        ApiServiceBinder.with(Vue.$opensilex.getServiceContainer());
    },

    components: {
        "opensilex-UserView": UserView,
        "opensilex-UserList": UserList,
        "opensilex-UserForm": UserForm
    }
};
