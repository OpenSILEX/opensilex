import { ApiServiceBinder } from './lib'
import ExperimentList from './components/experiments/ExperimentList.vue';

import FactorView from './components/factors/FactorView.vue';
import FactorList from './components/factors/FactorList.vue';
import FactorForm from './components/factors/FactorForm.vue';

import FactorLevelView from './components/factors/FactorLevelView.vue';
import FactorLevelList from './components/factors/FactorLevelList.vue';
import FactorLevelForm from './components/factors/FactorLevelForm.vue';

export default {
    install(Vue, options) {
        ApiServiceBinder.with(Vue.$opensilex.getServiceContainer());
    },
    components: {
        // experiments
       "opensilex-core-ExperimentList": ExperimentList,
        // factors
       "opensilex-core-FactorView": FactorView,
       "opensilex-core-FactorList": FactorList,
       "opensilex-core-FactorForm": FactorForm,
       // factors levels
       "opensilex-core-FactorLevelView": FactorLevelView,
       "opensilex-core-FactorLevelList": FactorLevelList,
       "opensilex-core-FactorLevelForm": FactorLevelForm
    },
    lang: {
        "fr": require("./lang/message-fr.json"),
        "en-US": require("./lang/message-en-US.json"),
    }

};