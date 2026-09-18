import {OpensilexModulePlugin} from "../../../opensilex-front/front/src/models/OpensilexModulePlugin";
import { ApiServiceBinder } from './lib';
import fr from "./lang/dataverse-fr.json";
import en from "./lang/dataverse-en.json";

export default {
  install(App, options) {
    // Accès au service container via les options, si nécessaire
    const serviceContainer = options?.opensilex?.getServiceContainer?.();
    if (serviceContainer) {
      ApiServiceBinder.with(serviceContainer);
    }
  },
  lang: {
    fr: fr,
    en: en,
  }
} as OpensilexModulePlugin;
