<template>
    <div>
        <opensilex-PageHeader
            :hasIcon="false"
            :hasImage="true"
            :imageSrc="$opensilex.getModuleFrontResourceURI('opensilex-dataverse', 'opensilex-dataverse', 'images/logo_RDG.png')"
            title="component.menu.publication.dataverse.label"
            class="detail-element-header"
            description="component.menu.publication.dataverse.description"
        ></opensilex-PageHeader>
        <opensilex-PageActions
            v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)">
            <opensilex-HelpButton
                class="helpButton"
                label="DatasetList.help-button"
                @click="helpModal.show()"
            ></opensilex-HelpButton>

            <b-modal ref="helpModal" hide-footer hide-header size="xl">
                <opensilex-dataverse-DataverseHelp @hideBtnIsClicked="helpModal.hide()"></opensilex-dataverse-DataverseHelp>
            </b-modal>
            <opensilex-CreateButton
                class="createButton"
                label="DatasetList.add"
                @click="datasetForm.showCreateForm()"
            ></opensilex-CreateButton>
            <img
                alt="work in progress"
                class="wip-icon"
                v-bind:src="$opensilex.getResourceURI('images/construction.png')"
            >
            <div v-html="$t('DatasetList.betaText')"></div>
        </opensilex-PageActions>

        <opensilex-PageContent
            class="pagecontent"
        >
            <!-- Toggle Sidebar-->
            <div :title="$t('searchfilter.label')"
                 class="searchMenuContainer"
                 v-on:click="searchFiltersToggle = !searchFiltersToggle">
                <div class="searchMenuIcon">
                    <i class="icon ik ik-search"></i>
                </div>
            </div>

            <!-- Filters -->
            <Transition>
                <div v-show="searchFiltersToggle">
                    <opensilex-SearchFilterField
                        :showAdvancedSearch="true"
                        class="searchFilterField"
                        withButton="false"
                        @clear="resetFilters()"
                        @search="refresh()"
                    >
                        <template v-slot:filters>
                            <!-- title -->
                            <br><br>
                            <div>
                                <opensilex-StringFilter
                                    :filter.sync="filters.title"
                                    class="searchFilter"
                                    placeholder="DatasetList.filter.title-placeholder"
                                    @handlingEnterKey="refresh()"
                                ></opensilex-StringFilter>
                                <br>
                            </div>
                        </template>

                        <template v-slot:advancedSearch>

                            <!-- experiment -->
                            <div>
                                <opensilex-ExperimentSelector
                                    label="DatasetList.filter.experiment"
                                    :experiments.sync="filters.experiment"
                                    class="searchFilter"
                                    placeholder="DatasetList.filter.experiment-placeholder"
                                    @handlingEnterKey="refresh()"
                                ></opensilex-ExperimentSelector>
                            </div>

                            <!-- authors -->
                            <div>
                                <label>{{ $t('DatasetList.filter.author') }}</label>
                                <opensilex-PersonSelector
                                    :persons.sync="filters.authors"
                                    class="searchFilter"
                                    placeholder="DatasetList.filter.author-placeholder"
                                    @handlingEnterKey="refresh()"
                                ></opensilex-PersonSelector>
                            </div>

                            <!-- deprecated -->
                            <div>
                                <opensilex-CheckboxForm
                                    :value.sync="filters.deprecated"
                                    class="searchFilter"
                                    label="DatasetList.filter.deprecated"
                                ></opensilex-CheckboxForm>
                            </div>
                        </template>
                    </opensilex-SearchFilterField>
                </div>
            </Transition>
            <opensilex-TableAsyncView
                ref="tableRef"
                :fields="fields"
                :searchMethod="searchDatasets"
                defaultSortBy="label"
            >

                <!-- Title -->
                <template v-slot:cell(uri)="{data}">
                    <opensilex-UriLink :to="{path: '/document/details/'+ encodeURIComponent(data.item.uri)}"
                                       :uri="data.item.uri"
                                       :value="data.item.title"
                    ></opensilex-UriLink>
                </template>

                <!-- Experiment -->
                <template v-slot:cell(experiment)="{data}">
                    <opensilex-UriLink :to="{path: '/experiment/details/'+ encodeURIComponent(data.item.targets[0])}"
                                       :uri="data.item.targets[0]"
                                       :value="experimentMap.get(data.item.targets[0])"
                    ></opensilex-UriLink>
                </template>

                <!-- Authors -->
                <template v-slot:cell(authors)="{data}">
                   <span v-if="data.item.authors">
                   <span v-for="(author, index) in data.item.authors" :key="index">
                    <opensilex-PersonContact
                        :personContact="authorMap.get(author)"
                    ></opensilex-PersonContact>
                    <span v-if="index + 1 < data.item.authors.length">, </span>
                    </span>
                    </span>
                </template>

                <!-- Production Date -->
                <template v-slot:cell(date)="{data}">
                    <span v-if="data.item.date">
                        <span>{{ data.item.date }}</span>
                    </span>
                </template>

                <!-- Actions -->
                <template v-slot:cell(actions)="{data}">
                    <b-button-group size="sm">
                        <opensilex-DeprecatedButton
                            v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
                            :deprecated="data.item.deprecated"
                            :small="true"
                            @click="deprecatedDocument(data.item.uri)"
                        ></opensilex-DeprecatedButton>

                        <opensilex-Button
                            v-if="data.item.source"
                            :small="true"
                            icon="ik#ik-link"
                            label="DatasetList.browseSource"
                            variant="outline-info"
                            @click="window.open(data.item.source)"
                        ></opensilex-Button>
                    </b-button-group>
                </template>
            </opensilex-TableAsyncView>
            <opensilex-ModalForm
                :key="refreshKey"
                ref="datasetForm"
                component="opensilex-dataverse-DatasetForm"
                createTitle="DatasetList.add"
                icon="ik#ik-file-text"
                modalSize="lg"
                :successMessage="successMessage"
                :overrideSuccessMessage="true"
                @onCreate="refreshOrRedirectAfterCreation"
                @onUpdate="refresh()"
            >
            </opensilex-ModalForm>
        </opensilex-PageContent>
    </div>
</template>

<script lang="ts">
import {Component, Ref, Prop} from "vue-property-decorator";
import Vue from "vue";
import {DocumentsService, DocumentGetDTO, ExperimentsService} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import {SecurityService, PersonDTO} from "opensilex-security/index";
import OpenSilexVuePlugin from "../../../../../opensilex-front/front/src/models/OpenSilexVuePlugin";
import {OpenSilexStore} from "../../../../../opensilex-front/front/src/models/Store";
import VueRouter, { Route } from "vue-router";
import OesoDataverse from "@/ontology/OesoDataverse";
import VueI18n from "vue-i18n";

@Component
export default class DatasetList extends Vue {
    $opensilex: OpenSilexVuePlugin;
    documentService: DocumentsService;
    securityService: SecurityService;
    experimentsService: ExperimentsService;
    $router: VueRouter;
    $route: Route;
    $store: OpenSilexStore;
    $t: typeof VueI18n.prototype.t;

    refreshKey = 0;
    experimentMap = new Map<string, string>();
    authorMap = new Map<string, PersonDTO>();
    searchFiltersToggle = false;


    rechercheDataGouvLogo: string = "";
    @Ref("datasetForm") readonly datasetForm!: any;
    @Ref("tableRef") readonly tableRef!: any;
    @Prop({
        default: false
    })
    redirectAfterCreation;
    @Ref("helpModal") readonly helpModal!: any;
    filters = {
        title: undefined,
        experiment: undefined,
        authors: undefined,
        multiple: undefined,
        deprecated: false
    };
    fields = [
        {
            key: "uri",
            label: "DatasetList.title",
            sortable: true
        },
        {
            key: "experiment",
            label: "DatasetList.experiment",
            sortable: false
        },
        {
            key: "authors",
            label: "DatasetList.author",
            sortable: false
        },
        {
            key: "date",
            label: "DatasetList.productionDate",
            sortable: true
        },
        {
            key: "actions",
            label: "component.common.actions"
        }
    ];
    private langUnwatcher;

    get user() {
        return this.$opensilex.$store.state.user;
    }

    get credentials() {
        return this.$opensilex.$store.state.credentials;
    }

    mounted() {
        this.langUnwatcher = this.$store.watch(
            () => this.$store.getters.language,
            () => this.refreshKey += 1
        );
    }

    beforeDestroy() {
        this.langUnwatcher();
    }

    created() {
        this.documentService = this.$opensilex.getService("opensilex.DocumentsService");
        this.securityService = this.$opensilex.getService("opensilex.SecurityService");
        this.experimentsService = this.$opensilex.getService("opensilex.ExperimentsService");
        this.rechercheDataGouvLogo = this.$opensilex.getModuleFrontResourceURI("opensilex-dataverse", "opensilex-dataverse", "images/logo_RDG.png");
    }

    refresh() {
        this.$opensilex.updateURLParameters(this.filters);
        this.tableRef.refresh();
    }

    refreshOrRedirectAfterCreation(document) {
        if (this.redirectAfterCreation) {
            this.$router.push({
                path: '/document/details/' + encodeURIComponent(document.uri)
            })
        } else {
            this.refresh();
        }
    }

    resetFilters() {
        this.filters = {
            title: undefined,
            experiment: undefined,
            authors: undefined,
            multiple: undefined,
            deprecated: false
        };
        this.refresh();
    }

    searchDatasets(options) {
        return this.documentService.searchDocuments(
            OesoDataverse.RECHERCHE_DATA_GOUV_DATASET_TYPE_URI,
            this.filters.title, //title filters
            undefined, // productionDate was removed because a single year isn't really practical and the service doesn't take a date range
            this.filters.experiment, // targets filters
            this.filters.authors, // user filters
            undefined, // keywords filters
            this.filters.multiple, // multiple filters
            this.filters.deprecated.toString(), // deprecated filters
            options.orderBy,
            options.currentPage,
            options.pageSize
        ).then(async (response) => {
            let experimentSet = new Set<string>();
            let authorSet = new Set<string>();

            response.response.result.forEach(document => {
                experimentSet.add(document.targets[0])
                document.authors.forEach(author => {
                    authorSet.add(author)
                })
            })

            let promises = [];

            experimentSet.forEach(experiment => {
                if (!this.experimentMap.has(experiment)){
                    promises.push(this.experimentsService.getExperiment(experiment).then(response => {
                        this.experimentMap.set(experiment, response.response.result.name)
                    }));
                }
            })
            authorSet.forEach(author => {
                if (!this.authorMap.has(author)){
                    promises.push(this.securityService.getPerson(author).then(response => {
                        this.authorMap.set(author, response.response.result)
                    }));
                }
            })

            await Promise.all(promises);
            console.log(response.response.result)
            return response
        })
    }

    deprecatedDocument(uri: string) {
        this.documentService
            .getDocumentMetadata(uri)
            .then((http: HttpResponse<OpenSilexResponse<DocumentGetDTO>>) => {
                let document = http.response.result;
                let form = {
                    description: {
                        uri: document.uri,
                        identifier: document.identifier,
                        rdf_type: document.rdf_type,
                        title: document.title,
                        date: document.date,
                        description: document.description,
                        targets: document.targets,
                        authors: document.authors,
                        language: document.language,
                        format: document.format,
                        deprecated: !document.deprecated,
                        keywords: document.keywords,
                        source: document.source
                    }
                };
                this.updateForDeprecated(form);
            })
            .catch(this.$opensilex.errorHandler);
    }

    updateForDeprecated(form) {
        return this.$opensilex
            .uploadFileToService("/core/documents", form, null, true)
            .then((http: any) => {
                this.$emit("onUpdate", form);
                this.refresh();
            })
            .catch(this.$opensilex.errorHandler);
    }

    successMessage(form) {
        return this.$t("DatasetList.success-message", {datasetDraftURI: form.uri});
    }
}
</script>

<style lang="scss" scoped>

.createButton, .helpButton {
    margin-bottom: 15px;
    margin-top: -10px;
    margin-right: 15px;
}

.createButton {
    margin-left: -10px;
}

.helpButton {
    margin-left: 0px;
    color: #00A28C;
    font-size: 1.2em;
    border: none
}

.helpButton:hover {
    background-color: #00A28C;
    color: #f1f1f1
}

.searchFilterField * {
    margin-left: 6px;
}

.wip-icon {
    margin-bottom: 15px;
    margin-top: -10px;
    display: inline-flex;
    justify-content: center;
    align-items: center;
}
</style>

<i18n>
en:
    DatasetList:
        betaText: This functionality is still in beta
fr:
    DatasetList:
        betaText: Cette fonctionnalité est encore en beta
</i18n>
