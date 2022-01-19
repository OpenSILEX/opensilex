/// <reference path="../../../opensilex-security/front/types/opensilex-security.d.ts" />
/// <reference path="../../../opensilex-core/front/types/opensilex-core.d.ts" />

import { ApiServiceBinder } from './lib';
import ProcessView from "./components/process/ProcessView.vue";
import ProcessForm from "./components/process/ProcessForm.vue";
import ProcessList from "./components/process/ProcessList.vue";
import StepView from "./components/process/step/StepView.vue";
import StepForm from "./components/process/step/StepForm.vue";
import StepList from "./components/process/step/StepList.vue";

export default {
    install(Vue, options) {
        ApiServiceBinder.with(Vue.$opensilex.getServiceContainer());
    },
    components: {
        "opensilex-process-ProcessView": ProcessView,
        "opensilex-process-ProcessForm": ProcessForm,
        "opensilex-process-ProcessList": ProcessList,
        "opensilex-process-StepView": StepView,
        "opensilex-process-StepForm": StepForm,
        "opensilex-process-StepList": StepList
    },
    lang: {
        "fr": require("./lang/process-fr.json"),
        "en": require("./lang/process-en.json"),
    }
};