import {OpensilexModulePlugin} from "../../../opensilex-front/front/src/models/OpensilexModulePlugin";

export default {
    install(app, options) {
        // This module's services are bound statically in the OpenSilexVuePlugin constructor
        // (opensilex-front/front/src/models/OpenSilexVuePlugin.ts), as opensilex-core is a hard
        // dependency of the front-end. Calling ApiServiceBinder.with() here would bind them a second
        // time and make inversify throw "Ambiguous match found for serviceIdentifier".
    }
} as OpensilexModulePlugin;
