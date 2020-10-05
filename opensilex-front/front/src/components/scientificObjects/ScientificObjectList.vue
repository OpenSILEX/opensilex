<template>

    <div class="container-fluid">

        <opensilex-PageHeader
            icon="ik#ik-target"
            title="component.menu.scientificObjects"
            description="component.scientificObjects.search.description"
        ></opensilex-PageHeader>

        <opensilex-SearchForm
            labelTitle="component.scientificObjects.search.filter.label"
            :resetMethod="reset"
            :searchMethod="refresh"
            showAdvancedSearch="true"
        >
        
        <template v-slot:standardSearch>
            
            <!-- Experiments -->
            <div class="filter-group col col-xl-3 col-sm-6 col-12">
                <opensilex-SelectForm
                    label="component.scientificObjects.search.filter.experiments"
                    placeholder="component.scientificObjects.search.placeholder.experiments"
                    :selected.sync="filter.projects"
                    :conversionMethod="experimentGetListDTOToSelectNode"
                    modalComponent="opensilex-ProjectModalList"
                    :isModalSearch="true"
                ></opensilex-SelectForm>
            </div>

            <!-- Germplasm -->
            <div class="filter-group col col-xl-3 col-sm-6 col-12">
                <opensilex-SelectForm
                    label="component.scientificObjects.search.filter.germplasm"
                    placeholder="component.scientificObjects.search.placeholder.germplasm"
                    :selected.sync="filter.germplasm"
                    :conversionMethod="germplasmGetDTOToSelectNode"
                    modalComponent="opensilex-ProjectModalList"
                    :isModalSearch="true"
                ></opensilex-SelectForm>
            </div>

            <div class="filter-group col col-xl-3 col-sm-6 col-12">
                <opensilex-InputForm
                    :value.sync="filter.label"
                    label="component.scientificObjects.search.filter.alias"
                    type="text"
                    placeholder="component.scientificObjects.search.placeholder.alias"
                ></opensilex-InputForm>
            </div>

        </template>

        <!-- Search form -->
        <template v-slot:advancedSearch>

            <div class="row">

                <!-- campains -->
                <div class="filter-group col col-xl-3 col-sm-6 col-12">
                    <opensilex-SelectForm
                        label="component.scientificObjects.search.filter.campains"
                        placeholder="component.scientificObjects.search.placeholder.campains"
                        :selected.sync="filter.campaign"
                        :optionsLoadingMethod="loadCampaigns"
                    ></opensilex-SelectForm>
                </div>

                <!-- hasExperimentModalities -->
                <div class="filter-group col col-xl-3 col-sm-6 col-12">
                    <opensilex-SelectForm
                        label="component.scientificObjects.search.filter.hasExperimentModalities"
                        placeholder="component.scientificObjects.search.placeholder.hasExperimentModalities"
                        :multiple="true"
                        :selected.sync="filter.hasExperimentModalities"
                        :optionsLoadingMethod="loadExperimentModalities"
                        :conversionMethod="ontologyToSelectNode"
                    ></opensilex-SelectForm>
                </div>

                <!-- hasReplication -->
                <div class="filter-group col col-xl-3 col-sm-6 col-12">
                    <opensilex-SelectForm
                        label="component.scientificObjects.search.filter.hasReplication"
                        placeholder="component.scientificObjects.search.placeholder.hasReplication"
                        :multiple="true"
                        :selected.sync="filter.hasReplication"
                        :optionsLoadingMethod="loadReplications"
                        :conversionMethod="ontologyToSelectNode"
                    ></opensilex-SelectForm>
                </div>

                <!-- hasSpecies -->
                <div class="filter-group col col-xl-3 col-sm-6 col-12">
                    <opensilex-SelectForm
                        label="component.scientificObjects.search.filter.hasSpecies"
                        placeholder="component.scientificObjects.search.placeholder.hasSpecies"
                        :multiple="true"
                        :selected.sync="filter.hasSpecies"
                        :optionsLoadingMethod="loadSpecies"
                        :conversionMethod="speciesToSelectNode"
                    ></opensilex-SelectForm>
                </div>

                <!-- hasVariety -->
                <div class="filter-group col col-xl-3 col-sm-6 col-12">
                    <opensilex-SelectForm
                        label="component.scientificObjects.search.filter.hasVariety"
                        placeholder="component.scientificObjects.search.placeholder.hasVariety"
                        :multiple="true"
                        :selected.sync="filter.hasVariety"
                        :optionsLoadingMethod="loadVariety"
                        :conversionMethod="ontologyToSelectNode"
                    ></opensilex-SelectForm>
                </div>

                <!-- hasPart -->
                <div class="filter-group col col-xl-3 col-sm-6 col-12">
                   <opensilex-InputForm
                        :value.sync="filter.hasPart"
                        label="component.scientificObjects.search.filter.hasPart"
                        type="text"
                        placeholder="component.scientificObjects.search.placeholder.hasPart"
                    ></opensilex-InputForm>
                </div>

                <!-- isPartOf -->
                <div class="filter-group col col-xl-3 col-sm-6 col-12">
                   <opensilex-InputForm
                        :value.sync="filter.isPartOf"
                        label="component.scientificObjects.search.filter.isPartOf"
                        type="text"
                        placeholder="component.scientificObjects.search.placeholder.isPartOf"
                    ></opensilex-InputForm>
                </div>

            </div>

        </template>
        
        </opensilex-SearchForm>
        <!-- End search form -->

        <opensilex-PageContent>

            <template v-slot>

                <opensilex-TableAsyncView
                    ref="tableRef"
                    :searchMethod="searchScientificObject"
                    :fields="fields"
                    defaultSortBy="label"
                    isSelectable="true"
                    labelNumberOfSelectedRow="component.scientificObjects.search.selected"
                    iconNumberOfSelectedRow="ik#ik-target"
                >

                    <template v-slot:firstActionsSelectableTable>
                        <div class="card-options d-inline-block">
                            <a href="data.html" class="btn btn-icon btn-outline-primary" title="Voir les données"><i class="ik ik ik-bar-chart-line-"></i></a>
                        </div>
                    </template>

                    <template v-slot:secondActionsSelectableTable>
                         <opensilex-ScientificObjectPropertyConfiguration :scientificObjectPropertyTypeConfiguration.sync="scientificObjectPropertyTypeConfiguration"></opensilex-ScientificObjectPropertyConfiguration>
                    </template>

                    <template v-slot:cell(uri)="{data}">
                        <opensilex-UriLink
                            :uri="data.item.uri"
                            :to="{path: '/experiment/'+ encodeURIComponent(data.item.uri)}"
                        ></opensilex-UriLink>
                    </template>
                
                    <template v-slot:cell(properties)="{data}">
                        <opensilex-ScientificObjectPropertyList :properties="data.item.properties" :scientificObjectPropertyTypeConfiguration="scientificObjectPropertyTypeConfiguration"></opensilex-ScientificObjectPropertyList>
                    </template>

                    <template v-slot:cell(actions)>
                        <a href="data.html" class="btn btn-icon btn-row-action btn-outline-primary" title="Visualiser les données de cet objet scientifique"><i class="ik ik-bar-chart-line-"></i></a>
                    </template>

                </opensilex-TableAsyncView>

            </template>

        </opensilex-PageContent>
        <!-- End results table -->

    </div>
</template>

<script lang="ts">

    import { Component, Ref } from "vue-property-decorator";
    import Vue from "vue";
    import VueConstructor from "vue";
    import VueRouter from "vue-router";
    import moment from "moment";
    import copy from "copy-to-clipboard"; 
    import VueI18n from 'vue-i18n';
    import { BDropdown } from 'bootstrap-vue';
    import { 
        GermplasmGetAllDTO,
        ProjectCreationDTO, 
        SpeciesDTO, 
        ExperimentGetDTO, 
        ResourceTreeDTO,
        InfrastructureGetDTO,
        ExperimentGetListDTO,
        ExperimentsService, 
        InfrastructuresService,
        ProjectsService, 
        SpeciesService,
        OntologyService
    } from "opensilex-core/index";
    import {
        PropertyDTO, 
        ScientificObjectDTO,
        ScientificObjectsService
    } from "opensilex-phis/index";
    import Oeso from "../../ontologies/Oeso";
    import HttpResponse, {OpenSilexResponse, MetadataDTO, PaginationDTO}  from "../../lib/HttpResponse";
    //import HttpResponse, {OpenSilexResponse, MetadataDTO}  from "opensilex-core/HttpResponse";

    class MockPropertie {
        
        rdfType;
        value;

        constructor(rdfType, value) {
            this.rdfType = rdfType;
            this.value = value;
        }

    }

    class MockExperiment {

        uri;
        label;
        campaign;

        constructor(uri, label, campaign) {
            this.uri = uri;
            this.label = label;
            this.campaign = campaign;
        }

    }

    class MockData {

        uri;
        label;
        properties;
        uriExperiment;

        constructor(uri, label, properties, uriExperiment) {
            this.uri = uri;
            this.label = label;
            this.properties = properties;
            this.uriExperiment = uriExperiment;
        }

    }

    class ScientificObjectFilter {
        experiments = [];
        germplasm = [];
        label = undefined;
        hasExperimentModalities = [];
        hasReplication = [];
        hasSpecies = [];
        hasPart = undefined;
        isPartOf = undefined;
        hasVariety = [];
        campaigns = [];

        constructor() {
            this.reset();
        }

        reset() {
            this.experiments = [];
            this.germplasm = [];
            this.label = undefined;
            this.hasExperimentModalities = [];
            this.hasReplication = [];
            this.hasSpecies = [];
            this.hasPart = undefined;
            this.isPartOf = undefined;
            this.hasVariety = [];
            this.campaigns = [];
        }
    }

    @Component
    export default class ScientificObjectList extends Vue {
        $opensilex: any;
        $store: any;
        $router: VueRouter;

        @Ref("tableRef") readonly tableRef!: any;
        
        fields = [
            {
                key: "uri",
                label: "component.scientificObjects.search.column.uri",
                sortable: true
            },
            {
                key: "label",
                label: "component.scientificObjects.search.column.alias",
                sortable: true
            },
            {
                key: "experiments",
                label: "component.scientificObjects.search.column.experiments"
            },
            {
                key: "campaigns",
                label: "component.scientificObjects.search.column.campaigns",
                sortable: true
            },
            {
                key: "type",
                label: "component.scientificObjects.search.column.type",
                sortable: true
            },
            {
                key: "properties",
                label: "component.scientificObjects.search.column.properties"
            },
            {
                key: "actions",
                label: "component.scientificObjects.search.column.actions"
            },
        ];

        experimentsByScientificObject: Map<String, Array<ExperimentGetDTO>> = new Map<String, Array<ExperimentGetDTO>>();

        filter = new ScientificObjectFilter();

        scientificObjectPropertyTypeConfiguration = [
            {show: true, class: 'badge-dark',    title: 'experimentModalities', label: 'component.scientificObjects.search.filter.hasExperimentModalities', uri: "http://www.opensilex.org/vocabulary/oeso#hasExperimentModalities"},
            {show: true, class: 'badge-warning', title: 'replication',          label: 'component.scientificObjects.search.filter.hasReplication',          uri: "http://www.opensilex.org/vocabulary/oeso#hasReplication"},
            {show: true, class: 'badge-success', title: 'species',              label: 'component.scientificObjects.search.filter.hasSpecies',              uri: "http://www.opensilex.org/vocabulary/oeso#hasSpecies"},
            {show: true, class: 'badge-info',    title: 'variety',              label: 'component.scientificObjects.search.filter.hasVariety',              uri: "http://www.opensilex.org/vocabulary/oeso#hasVariety"}
        ];
        
        get user() {
            return this.$store.state.user;
        }

        reset() {
            this.filter.reset();
            this.refresh();
        }

        refresh() {
            this.tableRef.refresh();
        }

        searchScientificObject(options) {
            return new Promise((resolve, reject) => {
                let paginationDTO = new PaginationDTO(10, 1, 10, 1);
                let metadataDTO = new MetadataDTO(paginationDTO, undefined, undefined);
                let openSilexResponse = new OpenSilexResponse<Array<any>>(this.mock(), metadataDTO);
                let httpResponse = new HttpResponse<OpenSilexResponse<Array<any>>>(openSilexResponse, 200, undefined);
                resolve(httpResponse);
            });
        }

        mock() {
            let experimentModalities = [];
            let replications = [];
            let varieties = [];
            let species = [];

            experimentModalities.push(new MockPropertie(Oeso.HAS_EXPERIMENT_MODALITIES, 'N-'));
            experimentModalities.push(new MockPropertie(Oeso.HAS_EXPERIMENT_MODALITIES, 'WD'));
            experimentModalities.push(new MockPropertie(Oeso.HAS_EXPERIMENT_MODALITIES, 'WD_SHD'));

            replications.push(new MockPropertie(Oeso.HAS_REPLICATION, 'x2'));
            replications.push(new MockPropertie(Oeso.HAS_REPLICATION, 'x5'));
            replications.push(new MockPropertie(Oeso.HAS_REPLICATION, 'x16'));
            replications.push(new MockPropertie(Oeso.HAS_REPLICATION, 'x24'));

            varieties.push(new MockPropertie(Oeso.HAS_VARIETY, 'voilur'));
            varieties.push(new MockPropertie(Oeso.HAS_VARIETY, 'nil-007'));
            varieties.push(new MockPropertie(Oeso.HAS_VARIETY, 'dkc4590'));

            species.push(new MockPropertie(Oeso.HAS_SPECIES, 'Maize'));
            species.push(new MockPropertie(Oeso.HAS_SPECIES, 'Dry wheat'));
            species.push(new MockPropertie(Oeso.HAS_SPECIES, 'Durum wheat'));

            let scientificObjects = new Array<ScientificObjectDTO>();

            for(let i=0; i<10; i++) {
                scientificObjects.push(this.mockExperiment(i, experimentModalities, replications, varieties, species));
            }

            return scientificObjects;
        }

        mockExperiment(i, experimentModalities, replications, varieties, species) {
            let uri = "uri-experiment-" + i;
            let label = "experiment-" + i;
            let campaign = 2000 + i;
            let experiment = new MockExperiment(uri, label, campaign);

            let uriSO = "uri-scientific-object-" + i;
            let labelSO = "label-" + i;
            let properties = [];
            properties.push(experimentModalities[i%2]);
            properties.push(replications[i%4]);
            properties.push(varieties[i%2]);
            properties.push(species[i%2]);

            let data = new MockData(uriSO, labelSO, properties, experiment.uri);

            let experiments = [];
            experiments.push(experiment);            

            this.experimentsByScientificObject.set(data.uri, experiments);

            return data;
        }
        
        loadExperimentModalities() {
            return this.loadOntology(Oeso.HAS_EXPERIMENT_MODALITIES);
        }

        loadReplications() {
            return this.loadOntology(Oeso.HAS_REPLICATION);
        }

        loadVariety() {
            return this.loadOntology(Oeso.HAS_VARIETY);
        }

        loadOntology(type) {
            let ontoService: OntologyService = this.$opensilex.getService("opensilex.OntologyService");
            return ontoService.getSubClassesOf(type, true).then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => http.response.result);
        }

        ontologyToSelectNode(dto: ResourceTreeDTO) {
            return {
                id: dto.uri,
                label: dto.name
            };
        }

        experimentGetListDTOToSelectNode(dto: ExperimentGetListDTO) {
            if(dto) {
                return {
                    id: dto.uri,
                    label: dto.label
                };
            }
            return null;
        }

        germplasmGetDTOToSelectNode(dto: GermplasmGetAllDTO) {
            if(dto) {
                return {
                    id: dto.uri,
                    label: dto.label
                };
            }
            return null;
        }

        loadSpecies() {
            return this.$opensilex
                .getService("opensilex.SpeciesService")
                .getAllSpecies()
                .then((http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>>) => http.response.result);
        }

        speciesToSelectNode(dto: SpeciesDTO) {
            return {
                id: dto.uri,
                label: dto.label
            };
        }

        loadCampaigns() {
            return new Promise((resolve, reject) => {
                let campaigns = [];
                campaigns.push({id: 2010, label: 2010});
                campaigns.push({id: 2011, label: 2011});
                campaigns.push({id: 2012, label: 2012});
                campaigns.push({id: 2013, label: 2013});
                campaigns.push({id: 2014, label: 2014});
                campaigns.push({id: 2015, label: 2015});
                campaigns.push({id: 2016, label: 2016});
                campaigns.push({id: 2017, label: 2017});
                campaigns.push({id: 2018, label: 2018});
                campaigns.push({id: 2019, label: 2019});
                campaigns.push({id: 2020, label: 2020});
                resolve(campaigns);
            });
        }

    }

</script>

<style scoped lang="scss">

</style>
