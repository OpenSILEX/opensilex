import { ApiServiceBinder } from './lib'

import ProjectForm from './components/projects/ProjectForm.vue';
import ProjectTable from './components/projects/ProjectTable.vue';
import FilterTable from './components/projects/FilterTable.vue';
import ProjectView from './components/projects/ProjectView.vue';



export default {
    install(Vue, options) {
        ApiServiceBinder.with(Vue.$opensilex.getServiceContainer());
    },
    components: {
        // projects
        "opensilex-core-ProjectView":ProjectView,
        "opensilex-core-ProjectTable": ProjectTable,
        "opensilex-core-ProjectForm": ProjectForm,
        "opensilex-core-FilterTable": FilterTable,
    },
    lang: {
        "fr": require("./lang/message-fr.json"),
        "en-US": require("./lang/message-en-US.json"),
    }
};