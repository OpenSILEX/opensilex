/// <reference path="../../../opensilex-security/front/types/opensilex-security.d.ts" />
/// <reference path="../../../opensilex-core/front/types/opensilex-core.d.ts" />

// import { ApiServiceBinder } from './lib';
import SiduriLoginComponent from "./components/layout/SiduriLoginComponent.vue";
import SiduriMenuComponent from "./components/layout/SiduriMenuComponent.vue";
import SiduriHeaderComponent from "./components/layout/SiduriHeaderComponent.vue";

export default {
    install(Vue, options) {
    //     ApiServiceBinder.with(Vue.$opensilex.getServiceContainer());
    },
    components: {
        "opensilex-siduri-SiduriLoginComponent": SiduriLoginComponent,
        "opensilex-siduri-SiduriMenuComponent": SiduriMenuComponent,
        "opensilex-siduri-SiduriHeaderComponent": SiduriHeaderComponent
    },
    lang: {
        "fr": require("./lang/siduri-fr.json"),
        "en": require("./lang/siduri-en.json"),
    }
};
