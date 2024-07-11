// <reference path="../../../opensilex-security/front/types/opensilex-security.d.ts" />
//import SiduriLoginComponent from "./components/layout/SiduriLoginComponent.vue";
import SiduriFooterComponent from "./components/layout/SiduriFooterComponent.vue";
import SiduriMenuComponent from "./components/layout/SiduriMenuComponent.vue";
import SiduriHeaderComponent from "./components/layout/SiduriHeaderComponent.vue";
import SiduriHomeComponent from "./components/layout/SiduriHomeComponent.vue";

export default {
    install(Vue, options) {
    },
    components: {
        "siduri-module-SiduriHomeComponent": SiduriHomeComponent,
        //"siduri-module-SiduriLoginComponent": SiduriLoginComponent,
        "siduri-module-SiduriFooterComponent": SiduriFooterComponent,
        "siduri-module-SiduriMenuComponent": SiduriMenuComponent,
        "siduri-module-SiduriHeaderComponent": SiduriHeaderComponent
    },
    /* lang: {
        "fr": require("./lang/siduri-fr.json"),
        "en": require("./lang/siduri-en.json"),
    }
    */
};
