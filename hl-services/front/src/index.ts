/// <reference path="../../../opensilex-security/front/types/opensilex-security.d.ts" />
/// <reference path="../../../opensilex-core/front/types/opensilex-core.d.ts" />
import EnvironmentalDataView from "./components/environmentalData/EnvironmentalDataView.vue"

export default {
    install(Vue, options) {
    },
    components: {
        "hl-services-environmentalData": EnvironmentalDataView,
        // "inrae-sinfonia-SinfoniaFooterComponent": SinfoniaFooterComponent,
        // "inrae-sinfonia-SinfoniaLoginComponent": SinfoniaLoginComponent,
        // "inrae-sinfonia-SinfoniaMenuComponent": SinfoniaMenuComponent,
        // "inrae-sinfonia-SinfoniaHeaderComponent": SinfoniaHeaderComponent
    },
    lang: {
        "fr": require("./lang/sinfonia-fr.json"),
        "en": require("./lang/sinfonia-en.json"),
    }
};