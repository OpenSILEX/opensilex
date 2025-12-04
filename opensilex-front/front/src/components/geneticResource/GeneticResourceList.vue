<template>
    <div>
        <opensilex-PageContent
            class="pagecontent"
        >
            <!-- Toggle Sidebar -->
            <div class="searchMenuContainer"
                 v-on:click="SearchFiltersToggle = !SearchFiltersToggle"
                 :title="searchFiltersPannel()">
                <div class="searchMenuIcon">
                    <i class="ik ik-search"></i>
                </div>
            </div>
            <Transition>
                <div v-show="SearchFiltersToggle">
                    <opensilex-SearchFilterField
                        @search="refresh()"
                        @clear="reset()"
                        label="GeneticResourceList.filter.description"
                        :showAdvancedSearch="true"
                        class="searchFilterField"
                    >
                        <template v-slot:filters>
                            <!-- Type -->
                            <div>
                                <opensilex-FilterField>
                                    <opensilex-TypeForm
                                        :type.sync="filter.rdf_type"
                                        :baseType="$opensilex.Oeso.GENETIC_RESOURCE_TYPE_URI"
                                        placeholder="GeneticResourceList.filter.rdfType-placeholder"
                                        class="searchFilter"
                                        @handlingEnterKey="refresh()"
                                    ></opensilex-TypeForm>
                                </opensilex-FilterField>
                            </div>

                            <!-- Species -->
                            <div>
                                <opensilex-FilterField>
                                    <opensilex-FormSelector
                                        label="GeneticResourceList.filter.species"
                                        placeholder="GeneticResourceList.filter.species-placeholder"
                                        :multiple="false"
                                        :selected.sync="filter.species"
                                        :options="species"
                                        class="searchFilter"
                                        @handlingEnterKey="refresh()"
                                    ></opensilex-FormSelector>
                                </opensilex-FilterField>
                            </div>

                            <!-- Year -->
                            <div>
                                <opensilex-FilterField>
                                    <label>{{ $t('GeneticResourceList.filter.year') }}</label>
                                    <opensilex-StringFilter
                                        :filter.sync="filter.production_year"
                                        placeholder="GeneticResourceList.filter.year-placeholder"
                                        type="number"
                                        class="searchFilter"
                                        @handlingEnterKey="refresh()"
                                    ></opensilex-StringFilter>
                                </opensilex-FilterField>
                                <br>
                            </div>

                            <!-- Institute -->
                            <div>
                                <opensilex-FilterField>
                                    <label>{{ $t('GeneticResourceList.filter.institute') }}</label>
                                    <opensilex-StringFilter
                                        :filter.sync="filter.institute"
                                        placeholder="GeneticResourceList.filter.institute-placeholder"
                                        class="searchFilter"
                                        @handlingEnterKey="refresh()"
                                    ></opensilex-StringFilter>
                                </opensilex-FilterField>
                                <br>
                            </div>

                            <!-- Name -->
                            <div>
                                <opensilex-FilterField>
                                    <label>{{ $t('GeneticResourceList.filter.label') }}</label>
                                    <opensilex-StringFilter
                                        :filter.sync="filter.name"
                                        placeholder="GeneticResourceList.filter.label-placeholder"
                                        class="searchFilter"
                                        @handlingEnterKey="refresh()"
                                    ></opensilex-StringFilter>
                                </opensilex-FilterField>
                                <br>
                            </div>

                            <!-- Experiments -->
                            <div v-if="!experimentUri">
                                <opensilex-FilterField>
                                    <opensilex-ExperimentSelector
                                        label="GeneticResourceList.filter.experiment"
                                        :experiments.sync="filter.experiment"
                                        class="searchFilter"
                                        @handlingEnterKey="refresh()"
                                        :key="resetExperimentSelectorKey"
                                    ></opensilex-ExperimentSelector>
                                </opensilex-FilterField>
                            </div>

                          <!-- GeneticResource Parents filter -->
                          <div>
                            <opensilex-FilterField>
                              <opensilex-GeneticResourceSelector
                                  label="GeneticResourceList.filter.parents"
                                  :multiple="true"
                                  :geneticResource.sync="filter.parent_geneticResources"
                                  class="searchFilter"
                                  @handlingEnterKey="refresh()"
                              ></opensilex-GeneticResourceSelector>
                            </opensilex-FilterField>
                          </div>

                          <!-- GeneticResource Group -->
                          <div>
                            <opensilex-FilterField>
                              <opensilex-GeneticResourceGroupSelector
                                  label="GeneticResourceList.filter.geneticResource-group"
                                  :multiple="false"
                                  :geneticResourceGroup.sync="filter.geneticResource_group"
                                  class="searchFilter"
                                  @handlingEnterKey="refresh()"
                              ></opensilex-GeneticResourceGroupSelector>
                            </opensilex-FilterField>
                          </div>

                          <!-- URI -->
                            <div>
                                <opensilex-FilterField>
                                    <label>{{ $t('GeneticResourceList.filter.uri') }}</label>
                                    <opensilex-StringFilter
                                        :filter.sync="filter.uri"
                                        placeholder="GeneticResourceList.filter.uri-placeholder"
                                        class="searchFilter"
                                        @handlingEnterKey="refresh()"
                                    ></opensilex-StringFilter>
                                </opensilex-FilterField>
                                <br>
                            </div>
                        </template>

                        <template v-slot:advancedSearch>

                          <!-- GeneticResource Attributes -->
                            <div>
                                <opensilex-FilterField>
                                    <opensilex-GeneticResourceAttributesSelector
                                        :geneticResourceAttribute.sync="filter.metadataKey"
                                        label="GeneticResourceList.filter.metadataKey"
                                        class="searchFilter"
                                        @handlingEnterKey="refresh()"
                                    ></opensilex-GeneticResourceAttributesSelector>
                                </opensilex-FilterField>
                            </div>

                            <div>
                                <opensilex-FilterField>
                                    <opensilex-GeneticResourceAttributesValueSelector
                                        ref="attributesValueSelector"
                                        :attributeKey.sync="filter.metadataKey"
                                        :attributeValue.sync="filter.metadataValue"
                                        class="searchFilter"
                                        @handlingEnterKey="refresh()"
                                    ></opensilex-GeneticResourceAttributesValueSelector>
                                </opensilex-FilterField>
                            </div>

                          <!-- GeneticResource Visibility -->
                            <div>
                              <opensilex-FilterField :fullWidth="true">
                                <opensilex-FormSelector
                                    :label="$t('GeneticResourceList.filter.is_public')"
                                    :options="[
                                                { id: true, label: $t('GeneticResourceList.filter.is_public_true') },
                                                { id: false, label: $t('GeneticResourceList.filter.is_public_false') }
                                              ]"
                                    :selected.sync="filter.is_public"
                                    :async="false"
                                    :multiple="false"
                                    :showCount="false"
                                    :placeholder="$t('GeneticResourceList.filter.is_public-placeholder')"
                                />
                              </opensilex-FilterField>

                            </div>


                        </template>
                    </opensilex-SearchFilterField>
                </div>
            </Transition>
            <opensilex-TableAsyncView
                ref="tableRef"
                :searchMethod="searchGeneticResource"
                :fields="fields"
                :fieldKeyToSortableModelLabelMap="{[nameFieldLabel]:'label'}"
                :isSelectable="true"
                @refreshed="onRefreshed"
                @select="$emit('select', $event)"
                @unselect="$emit('unselect', $event)"
                @selectall="$emit('selectall', $event)"
                defaultSortBy="label"
                labelNumberOfSelectedRow="GeneticResourceList.selected"
                iconNumberOfSelectedRow="fa#seedling"
            >
                <template v-slot:selectableTableButtons="{ numberOfSelectedRows }">

                    <b-dropdown
                        dropright
                        class="mb-2 mr-2"
                        :small="true"
                        :text="$t('VariableList.display')">

                        <b-dropdown-item-button @click="clickOnlySelected()">
                            {{ onlySelected ? $t('GeneticResourceList.selected-all') : $t("component.common.selected-only") }}
                        </b-dropdown-item-button>
                        <b-dropdown-item-button @click="resetSelected()">{{ $t("component.common.resetSelected") }}
                        </b-dropdown-item-button>
                    </b-dropdown>

                    <b-dropdown
                        dropright
                        class="mb-2 mr-2"
                        :small="true"
                        :disabled="numberOfSelectedRows == 0"
                        text=actions>
                        <b-dropdown-item-button
                            v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
                            @click="createDocument()"
                        >{{ $t('component.common.addDocument') }}
                        </b-dropdown-item-button>
                        <b-dropdown-item-button
                            @click="exportCSV(false)"
                        >{{ $t('GeneticResourceList.export') }}
                        </b-dropdown-item-button>
                    </b-dropdown>

                    <opensilex-CreateButton
                        v-if="!noActions"
                        class="mb-2 mr-2"
                        @click="exportCSV(true)"
                        :disabled="tableRef.totalRow === 0"
                        label="ScientificObjectList.export-all"
                    ></opensilex-CreateButton>

                </template>
                <template v-slot:cell(name)="{data}">
                    <opensilex-UriLink
                        :uri="data.item.uri"
                        :value="data.item.name"
                        :to="{path: '/geneticResource/details/'+ encodeURIComponent(data.item.uri)}"
                    ></opensilex-UriLink>
                </template>

              <template v-slot:cell(geneticResource_is_public)="{ data }">
                <opensilex-Icon
                    v-if="data.item.is_public === false"
                    icon="ik#ik-lock"
                    class="text-secondary"
                    style="font-size: 1.2em"
                    :title="$t('GeneticResourceList.filter.is_public_false')"
                />
              </template>

              <template v-slot:cell(actions)="{data}">
                    <b-button-group size="sm">
                      <opensilex-EditButton
                            v-if="user.hasCredential(credentials.CREDENTIAL_GENETIC_RESOURCE_MODIFICATION_ID)"
                            @click="$emit('onEdit', data.item.uri)"
                            label="GeneticResourceList.update"
                            :small="true"
                        ></opensilex-EditButton>
                        <opensilex-DeleteButton
                            v-if="user.hasCredential(credentials.CREDENTIAL_GENETIC_RESOURCE_DELETE_ID)"
                            @click="deleteGeneticResource(data.item.uri)"
                            label="GeneticResourceList.delete"
                            :small="true"
                        ></opensilex-DeleteButton>
                    </b-button-group>
                </template>
            </opensilex-TableAsyncView>
            <opensilex-ModalForm
                v-if="user.hasCredential(credentials.CREDENTIAL_GENETIC_RESOURCE_MODIFICATION_ID)"
                ref="documentForm"
                component="opensilex-DocumentForm"
                createTitle="component.common.addDocument"
                modalSize="lg"
                :initForm="initForm"
                icon="ik#ik-file-text"
            ></opensilex-ModalForm>
        </opensilex-PageContent>
    </div>
</template>

<script lang="ts">
import {Component, Ref, Prop} from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import {
    GeneticResourceService,
    ExperimentGetListDTO,
    ExperimentsService,
    SpeciesService,
    SpeciesDTO,
    GeneticResourceSearchFilter
} from "opensilex-core/index";

import TableAsyncView from "../common/views/TableAsyncView.vue";
import GeneticResourceAttributesValueSelector from "./GeneticResourceAttributesValueSelector.vue";
import {GeneticResourceGetAllDTO} from "opensilex-core/model/geneticResourceGetAllDTO";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";


@Component
export default class GeneticResourceList extends Vue {
    $opensilex: any;
    $store: any;
    $route: any;
    $router: VueRouter;
    service: GeneticResourceService;

    @Ref("documentForm") readonly documentForm!: any;
    @Ref("tableRef") readonly tableRef!: TableAsyncView<GeneticResourceGetAllDTO>;
    @Ref("attributesValueSelector") attributeValueSelector: GeneticResourceAttributesValueSelector;


    resetExperimentSelectorKey = 0;

    @Prop({
        default: false
    })
    isSelectable;

    @Prop({
        default: false
    })
    noActions;


  /**
   * Set an experiment uri, in this case we don't show experiment filter and show only geneticResources of this experiment
   */
    @Prop()
    experimentUri: string;

    get user() {
        return this.$store.state.user;
    }

    get onlySelected() {
        return this.tableRef.onlySelected;
    }

    get credentials() {
        return this.$store.state.credentials;
    }

    get lang() {
        return this.$store.state.lang;
    }

    species = [];
    speciesByUri: Map<String, SpeciesDTO> = new Map<String, SpeciesDTO>();
    SearchFiltersToggle: boolean = true;
    nameFieldLabel = "name";


  filter = {
    rdf_type: undefined,
    name: undefined,
    species: undefined,
    production_year: undefined,
    institute: undefined,
    experiment: undefined,
    parent_geneticResources: [],
    parent_geneticResources_m: [],
    parent_geneticResources_f: [],
    geneticResource_group: undefined,
    uri: undefined,
    is_public: undefined,
    metadataKey: undefined,
    metadataValue: undefined
  };

    reset() {
        this.filter = {
          rdf_type: undefined,
          name: undefined,
          species: undefined,
          production_year: undefined,
          institute: undefined,
          experiment: undefined,
          parent_geneticResources: [],
          parent_geneticResources_m: [],
          parent_geneticResources_f: [],
          geneticResource_group: undefined,
          uri: undefined,
          is_public: undefined,
          metadataKey: undefined,
          metadataValue: undefined
        };

        this.refresh();
        this.resetExperimentSelectorKey++;
    }

    onItemUnselected(row) {
        this.tableRef.onItemUnselected(row);
    }

    onItemSelected(row) {
      this.tableRef.onItemSelected(row);
    }

    clickOnlySelected() {
        this.tableRef.clickOnlySelected();
    }

    resetSelected() {
        this.tableRef.resetSelected();
    }

  setInitiallySelectedItems(initiallySelectedItems:any){
    this.tableRef.setInitiallySelectedItems(initiallySelectedItems);
  }

    getSelected() {
        return this.tableRef.getSelected();
    }

    private langUnwatcher;

    mounted() {
        this.langUnwatcher = this.$store.watch(
            () => this.$store.getters.language,
            lang => {
                this.updateLang();
            }
        );
    }

    beforeDestroy() {
        this.langUnwatcher();
    }

    created() {
        this.service = this.$opensilex.getService("opensilex.GeneticResourceService")
        this.loadSpecies();
        this.$opensilex.updateFiltersFromURL(this.$route.query, this.filter);
    }

    get fields() {
        let tableFields = [
            {
                key: this.nameFieldLabel,
                label: "GeneticResourceList.name",
                sortable: true
            },
            {
                key: "rdf_type_name",
                label: "GeneticResourceList.rdfType",
                sortable: true
            },
            {
                key: "species_name",
                label: "GeneticResourceList.speciesLabel"
            },
            {
                key: "geneticResource_is_public",
                label: "GeneticResourceList.is_public"
            }

        ];
        if (!this.noActions) {
            tableFields.push({
                key: "actions",
                label: "component.common.actions"
            });
        }
        return tableFields;
    }

    @Ref("speciesSelector") readonly speciesSelector!: any;

  refresh() {
    this.tableRef.selectAll = false;
    this.updateSelectedGeneticResource()
    this.tableRef.changeCurrentPage(1);
    }

    updateSelectedGeneticResource(){
        this.tableRef.selectAll = false;
        this.$opensilex.updateURLParameters(this.filter);
        if (this.tableRef.onlySelected) {
            this.tableRef.onlySelected = false;
        }
        this.tableRef.refresh();
    }

  searchGeneticResource(options) {
    // this.updateExportFilters();
    return this.service.searchGeneticResource(
        this.filter.uri,
        this.filter.rdf_type,
        this.filter.name,
        undefined,
        this.filter.production_year,
        this.filter.species,
        undefined,
        undefined,
        this.filter.geneticResource_group,
        this.filter.institute,
        this.experimentUri || this.filter.experiment,
        this.filter.parent_geneticResources,
        this.filter.parent_geneticResources_m,
        this.filter.parent_geneticResources_f,
        this.addMetadataFilter(),
        this.filter.is_public,
        options.orderBy,
        options.currentPage,
        options.pageSize
    );
  }


  exportCSV(exportAll: boolean) {
        let path = "/core/geneticResource/export";
        let today = new Date();
        let filename = "export_geneticResource_" + today.getFullYear() + String(today.getMonth() + 1).padStart(2, '0') + String(today.getDate()).padStart(2, '0');

        let exportDto: GeneticResourceSearchFilter = {
            uri: this.filter.uri,
            rdf_type: this.filter.rdf_type,
            name: this.filter.name,
            production_year: this.filter.production_year,
            species: this.filter.species,
            institute: this.filter.institute,
            experiment: this.filter.experiment,
            order_by: this.tableRef.getOrderBy(),
            metadata: this.addMetadataFilter()
        };

        // export only selected URIS (+matching with filter)
        if (!exportAll) {
            let objectURIs = [];
            for (let select of this.tableRef.getSelected()) {
                objectURIs.push(select.uri);
            }
            Object.assign(exportDto, {
                uris: objectURIs
            });
            exportDto.page_size = objectURIs.length;
        } else {
            exportDto.page_size = this.tableRef.getTotalRow();
        }

        this.$opensilex
            .downloadFilefromPostService(path, filename, "csv", exportDto, this.lang);
    }

    loadSpecies() {
        let service: SpeciesService = this.$opensilex.getService(
            "opensilex.SpeciesService"
        );
        service
            .getAllSpecies()
            .then((http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>>) => {
                this.species = [];
                for (let i = 0; i < http.response.result.length; i++) {
                    this.speciesByUri.set(
                        http.response.result[i].uri,
                        http.response.result[i]
                    );
                    this.species.push({
                        id: http.response.result[i].uri,
                        label: http.response.result[i].name
                    });
                }
            })
            .catch(this.$opensilex.errorHandler);
    }

    updateLang() {
        this.loadSpecies();
        this.refresh();
    }

    addMetadataFilter() {

        // undefined, null or empty : https://262.ecma-international.org/5.1/#sec-9.2
        if (!this.filter.metadataKey) {
            return undefined;
        }

        // set empty string for JSON.stringify method, by default undefined fields are not serialized
        let value = this.filter.metadataValue ? this.filter.metadataValue : "";

        // if metadataValue is undefined, match all geneticResource which have the given metadata key
        // else, match geneticResource with the good key and value
        return JSON.stringify({
            [this.filter.metadataKey] : value
        });
    }

    createDocument() {
        this.documentForm.showCreateForm();
    }

    initForm() {
        let targetURI = [];
        for (let select of this.tableRef.getSelected()) {
            targetURI.push(select.uri);
        }

        return {
            description: {
                uri: undefined,
                identifier: undefined,
                rdf_type: undefined,
                title: undefined,
                date: undefined,
                description: undefined,
                targets: targetURI,
                authors: undefined,
                language: undefined,
                deprecated: undefined,
                keywords: undefined
            },
            file: undefined
        }
    }

    searchFiltersPannel() {
        return this.$t("searchfilter.label")
    }

    onRefreshed() {
        let that = this;
        setTimeout(function () {
            if (that.tableRef.selectAll === true && that.tableRef.selectedItems.length !== that.tableRef.totalRow) {
                that.tableRef.selectAll = false;
            }
        }, 1);
    }

    deleteGeneticResource(uri){
        this.tableRef.checkSelectedItems(uri);
        this.$emit('onDelete', uri);
    }
}
</script>

<style scoped lang="scss">
.clear-btn {
    color: rgb(229, 227, 227) !important;
    border-color: rgb(229, 227, 227) !important;
    border-left: none !important;
}
</style>

<i18n>

en:
    GeneticResourceList:
        uri: URI
        name: Name
        rdfType: Type
        fromSpecies: Species URI
        speciesLabel: Species
        update: Update Genetic Resource
        delete: Delete Genetic Resource
        selectLabel: Select Genetic Resource
        selected: Selected Genetic Resource
        export: Export Genetic Resource list
        selected-all: All Genetic Resource
        is_public: Visibility

        filter:
          description: Genetic Resource Search
          species: Species
          species-placeholder: Select a species
          year: Production year
          year-placeholder: Enter a year
          institute: Institute code
          institute-placeholder: Enter an institute code
          label: Name
          label-placeholder: Enter genetic resource name
          rdfType: Type
          rdfType-placeholder: Select a genetic resource type
          experiment: Experiment
          experiment-placeholder: Select an experiment
          uri: URI
          uri-placeholder: Enter a part of an uri
          search: Search
          reset: Reset
          metadataKey: Attribute name
          metadataValue: Attribute value
          geneticResource-group: Genetic Resource Group
          parents: Parents
          is_public: Visibility
          is_public_true: Public
          is_public_false: Private
          is_public-placeholder : Select genetic resource visibility

fr:
    GeneticResourceList:
        uri: URI
        label: Nom
        rdfType: Type
        fromSpecies: URI de l'espèce
        speciesLabel: Espèce
        update: Editer ressource génétique
        delete: Supprimer ressource génétique
        selectLabel: Sélection de Ressources Génétiques
        selected: Ressource(s) Génétique(s) Sélectionnée(s)
        export: Exporter la liste
        selected-all: Toutes les ressources génétiques
        is_public: Visibilité


        filter:
          description: Recherche de Ressources Génétiques
          species: Espèce
          species-placeholder: Sélectionner une espèce
          year: Année de production
          year-placeholder: Entrer une année
          institute: Code Institut
          institute-placeholder: Entrer le code d'un institut
          label: Nom
          label-placeholder: Entrer un nom de ressource génétique
          rdfType: Type
          rdfType-placeholder: Sélectionner un type de ressource génétique
          experiment: Expérimentation
          experiment-placeholder: Sélectionner une expérimentation
          uri: URI
          uri-placeholder: Entrer une partie d'une uri
          search: Rechercher
          reset: Réinitialiser
          metadataKey: Nom de l'attribut
          metadataValue: Valeur de l'attribut
          geneticResource-group: Groupe de ressources génétiques
          parents: Parents
          is_public: Visibilité
          is_public_true: Publique
          is_public_false: Privé
          is_public-placeholder : Sélectionner la visibilité de ressource génétique

</i18n>
