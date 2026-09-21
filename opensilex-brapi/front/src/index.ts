import {OpensilexModulePlugin} from "../../../opensilex-front/front/src/models/OpensilexModulePlugin";
import { ApiServiceBinder } from './lib';
export default {
    install(App, options) {
        ApiServiceBinder.with(options.opensilex.getServiceContainer());
    }
} as OpensilexModulePlugin;