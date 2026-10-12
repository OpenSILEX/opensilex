import PhisLoginComponent from "./components/layout/PhisLoginComponent.vue";
import PhisHeaderComponent from "./components/layout/PhisHeaderComponent.vue";
import {OpensilexModulePlugin} from "../../../opensilex-front/front/src/models/OpensilexModulePlugin";

export default {
    install(app, options) {
    },
    components : {
        "opensilex-phis-PhisLoginComponent" : PhisLoginComponent,
        "opensilex-phis-PhisHeaderComponent" : PhisHeaderComponent
    }
} as OpensilexModulePlugin;