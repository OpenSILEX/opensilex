/// <reference path="../../../opensilex-dataverse/front/types/opensilex-dataverse.d.ts" />
import {ApiServiceBinder} from './lib';
import DatasetList from "./components/datasets/DatasetList.vue";
import DatasetForm from "./components/datasets/DatasetForm.vue";
import DataverseHelp from "./components/datasets/DataverseHelp.vue";

export default {
    install(Vue, options) {
        ApiServiceBinder.with(Vue.$opensilex.getServiceContainer());
    },
    components : {
        "opensilex-dataverse-DatasetList" : DatasetList,
        "opensilex-dataverse-DatasetForm" : DatasetForm,
        "opensilex-dataverse-DataverseHelp" : DataverseHelp
    },

    lang: {
        "fr": require("./lang/dataverse-fr.json"),
        "en": require("./lang/dataverse-en.json")
    }
};