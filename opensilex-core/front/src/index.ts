import { ApiServiceBinder } from './lib'

// import ExperimentForm from './components/experiments/ExperimentForm.vue';
// import ExperimentForm2 from './components/experiments/ExperimentForm2.vue';
// import ExperimentList from './components/experiments/ExperimentList.vue';
// import ExperimentCreate from './components/experiments/ExperimentCreate.vue';
// import ExperimentView from './components/experiments/ExperimentView.vue';


export default {
    install(Vue, options) {
        ApiServiceBinder.with(Vue.$opensilex.getServiceContainer());
    },
    // components: {
    //     // experiments
    //     "opensilex-core-ExperimentForm": ExperimentForm,
    //     "opensilex-core-ExperimentForm2": ExperimentForm2,
    //     "opensilex-core-ExperimentList": ExperimentList,
    //     "opensilex-core-ExperimentCreate": ExperimentCreate,
    //     "opensilex-core-ExperimentView": ExperimentView,
    // },
    lang: {
        "fr": require("./lang/message-fr.json"),
        "en-US": require("./lang/message-en-US.json"),
    }

};