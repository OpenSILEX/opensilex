<template>
    <b-form v-if="form.description">
        <!-- URI or URL-->
        <opensilex-UriForm
            :editMode="editMode"
            :generated.sync="uriGenerated"
            :uri.sync="form.description.datasetUri"
            helpMessage="DatasetForm.uri-help"
            label="DatasetForm.uri"
        ></opensilex-UriForm>

        <!-- experiment -->
        <opensilex-ExperimentSelector
            :key="experimentSelectorRefreshKey"
            :experiments.sync="form.description.experiment"
            :required="true"
            :isModalSearch="true"
            helpMessage="DatasetForm.experiment-help"
            label="DatasetForm.experiment"
        ></opensilex-ExperimentSelector>

        <!-- title -->
        <opensilex-InputForm
            :required="true"
            :value.sync="form.description.title"
            helpMessage="DatasetForm.title-help"
            label="DatasetForm.title"
            type="text"
        ></opensilex-InputForm>

        <!-- production date -->
        <opensilex-DateForm
            :required="true"
            :value.sync="form.description.productionDate"
            helpMessage="DatasetForm.productionDate-help"
            label="DatasetForm.productionDate"
        ></opensilex-DateForm>

        <!-- Authors -->
        <opensilex-PersonSelector
            :multiple="true"
            :persons.sync="form.description.authors"
            :required="true"
            :isModalSearch="true"
            helpMessage="DatasetForm.authors-help"
            label="DatasetForm.authors"
        ></opensilex-PersonSelector>

        <!-- Contacts -->
        <opensilex-PersonSelector
            :multiple="true"
            :personPropertyExistsCondition="'email'"
            :persons.sync="form.description.contacts"
            :required="true"
            :isModalSearch="true"
            helpMessage="DatasetForm.contacts-help"
            label="DatasetForm.contacts"
        ></opensilex-PersonSelector>

        <!-- Dataset language -->
        <opensilex-SelectForm
            :options="datasetAvailableLanguages"
            :required="true"
            :selected.sync="form.description.datasetLanguage"
            helpMessage="DatasetForm.datasetLanguage-help"
            label="DatasetForm.datasetLanguage"
            placeholder="DatasetForm.placeholder-datasetLanguage"
        ></opensilex-SelectForm>

        <!-- Dataset Metadata language -->
        <opensilex-SelectForm
            :options="datasetAvailableMetadataLanguages"
            :required="true"
            :selected.sync="form.description.datasetMetadataLanguage"
            helpMessage="DatasetForm.datasetMetadataLanguage-help"
            label="DatasetForm.datasetMetadataLanguage"
            placeholder="DatasetForm.placeholder-datasetMetadataLanguage"
        ></opensilex-SelectForm>

        <!-- Deprecated -->
        <opensilex-CheckboxForm
            v-if="editMode"
            :value.sync="form.description.deprecated"
            helpMessage="DatasetForm.deprecated-help"
            label="DatasetForm.deprecated"
            title="DatasetForm.deprecated-title"
        ></opensilex-CheckboxForm>

        <!-- Parent Dataverse Alias -->
        <opensilex-InputForm
            :required="true"
            :value.sync="form.description.dataverseAlias"
            helpMessage="DatasetForm.dataverseAlias-help"
            label="DatasetForm.dataverseAlias"
            placeholder="DatasetForm.placeholder-dataverseAlias"
            type="text"
        ></opensilex-InputForm>

        <!-- Dataverse API key -->
        <opensilex-InputForm
            :helpMessage="helpMessageAPIToken"
            :html="true"
            :required="true"
            :value.sync="form.description.dataverseAPIKey"
            label="DatasetForm.dataverseAPIKey"
            placeholder="DatasetForm.placeholder-dataverseAPIKey"
            type="text"
        ></opensilex-InputForm>
    </b-form>
</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
import {DocumentsService} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import {DataverseService} from "@/lib";
import OesoDataverse from "@/ontology/OesoDataverse";
import {SelectableItem} from "../../../../../opensilex-front/front/src/components/common/forms/SelectForm.vue";
import OpenSilexVuePlugin from "../../../../../opensilex-front/front/src/models/OpenSilexVuePlugin";
import VueRouter, {Route} from "vue-router";
import VueI18n from "vue-i18n";
import {OpenSilexStore} from "../../../../../opensilex-front/front/src/models/Store";

@Component
export default class DatasetForm extends Vue {
    $opensilex: OpenSilexVuePlugin;
    dataverseService: DataverseService;
    $router: VueRouter;
    $route: Route;
    $store: OpenSilexStore;
    $t: typeof VueI18n.prototype.t;
    file;
    uriGenerated = true;
    datasetAvailableLanguages: Array<SelectableItem> = undefined;
    datasetAvailableMetadataLanguages: Array<SelectableItem> = undefined;
    experimentSelectorRefreshKey = 0;
    rechercheDataGouvBasePath = "";
    helpMessageAPIToken = "";
    @Prop()
    editMode;
    @Prop({
        default: () => {
            return {
                description: {
                    experiment: undefined,
                    title: undefined,
                    authors: undefined,
                    contacts: undefined,
                    datasetLanguage: undefined,
                    datasetMetadataLanguage: undefined,
                    deprecated: undefined,
                    productionDate: undefined,
                    datasetUri: undefined,
                    dataverseAlias: undefined,
                    dataverseAPIKey: undefined
                },
                file: undefined
            };
        }
    })
    form: any;

    get user() {
        return this.$store.state.user;
    }

    reset() {
        this.uriGenerated = true;
    }

    created() {
        this.dataverseService = this.$opensilex.getService("opensilex-dataverse.DataverseService");
        this.dataverseService.availableDatasetLanguages()
            .then((value: HttpResponse<OpenSilexResponse<{ [key: string]: any; }>>) => {
                this.datasetAvailableLanguages = [];
                Object.entries(value.response.result).forEach(([key, value]) => {
                    this.datasetAvailableLanguages.push({
                        id: key,
                        label: value
                    });
                });
            })
            .catch(this.$opensilex.errorHandler);
        this.dataverseService.availableDatasetMetadataLanguages()
            .then((value: HttpResponse<OpenSilexResponse<{ [key: string]: any; }>>) => {
                this.datasetAvailableMetadataLanguages = [];
                Object.entries(value.response.result).forEach(([key, value]) => {
                    this.datasetAvailableMetadataLanguages.push({
                        id: key,
                        label: value
                    });
                });
            })
            .catch(this.$opensilex.errorHandler);
        this.dataverseService.rechercheDataGouvBasePath()
            .then(response => {
                this.rechercheDataGouvBasePath = response.response.result
                this.helpMessageAPIToken = this.$t("DatasetForm.dataverseAPIKey-help", {
                    rechercheDataGouvAnchor: this.rechercheDataGouvBasePath
                }).toString();
            });
    }

    getEmptyForm() {
        // This manual refresh is needed because the ExperimentSelector as a cache
        this.experimentSelectorRefreshKey += 1
        return {
            description: {
                experiment: undefined,
                title: undefined,
                authors: undefined,
                contacts: undefined,
                datasetLanguage: undefined,
                datasetMetadataLanguage: undefined,
                deprecated: undefined,
                productionDate: undefined,
                datasetUri: undefined,
                dataverseAlias: undefined,
                dataverseAPIKey: undefined
            },
            file: undefined
        };
    }

    create(form: any) {
        this.$opensilex.enableLoader();
        this.$opensilex.showLoader();
        return this.dataverseService
            .createDataset(
                form.description.experiment,
                form.description.title,
                form.description.authors,
                form.description.contacts,
                form.description.datasetLanguage,
                form.description.datasetMetadataLanguage,
                OesoDataverse.RECHERCHE_DATA_GOUV_DATASET_TYPE_URI,
                form.description.productionDate,
                form.description.datasetUri,
                form.description.deprecated,
                undefined,
                form.description.dataverseAlias,
                form.description.dataverseAPIKey
            )
            .then((value: HttpResponse<OpenSilexResponse<string>>) => {
                let uri = value.response.result;
                console.debug("document created", uri);
                form.uri = uri;
                this.$opensilex.hideLoader();
                this.$opensilex.disableLoader();
                return form;
            })
            .catch(error => {
                if (error.status == 409) {
                    console.error("Document already exists", error);
                    this.$opensilex.errorHandler(
                        error,
                        this.$t("DatasetForm.error.document-already-exists")
                    );
                } else {
                    this.$opensilex.errorHandler(error);
                }
            });
    }

}
</script>

<style lang="scss" scoped>
</style>

<i18n>

</i18n>
