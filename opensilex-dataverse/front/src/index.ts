// src/modules/dataverse/index.ts
import type { App } from 'vue';
import { ApiServiceBinder } from './lib';
// import DatasetList from "./components/datasets/DatasetList.vue";
// import DatasetForm from "./components/datasets/DatasetForm.vue";
// import DataverseHelp from "./components/datasets/DataverseHelp.vue";

import fr from "./lang/dataverse-fr.json";
import en from "./lang/dataverse-en.json";

export default {
  install(app: App, options: any) {
    // Accès au service container via les options, si nécessaire
    const serviceContainer = options?.opensilex?.getServiceContainer?.();
    if (serviceContainer) {
      ApiServiceBinder.with(serviceContainer);
    }

    // Enregistrement global si souhaité (ou à utiliser localement)
    // app.component("opensilex-dataverse-DatasetList", DatasetList);
    // app.component("opensilex-dataverse-DatasetForm", DatasetForm);
    // app.component("opensilex-dataverse-DataverseHelp", DataverseHelp);
  },

  components: {
    // "opensilex-dataverse-DatasetList": DatasetList,
    // "opensilex-dataverse-DatasetForm": DatasetForm,
    // "opensilex-dataverse-DataverseHelp": DataverseHelp,
  },

  lang: {
    fr: fr,
    en: en,
  }
};
