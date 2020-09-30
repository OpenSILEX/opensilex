<template>

    <b-modal ref="modalRef" size="xxl" :static="true">
        <template v-slot:modal-title>
            <i class="ik ik-search mr-1"></i> {{ $t('component.experiment.search.selectLabel') }}
        </template>
        <template v-slot:modal-footer>
            <button type="button" class="btn btn-secondary" v-on:click="hide(false)">{{ $t('component.common.close') }}</button>
            <button type="button" class="btn btn-primary" v-on:click="hide(true)">{{ $t('component.common.validateSelection') }}</button>
        </template>
        
        <div class="card">
            
             <opensilex-SearchFilterField
                @search="refresh()"
                @clear="reset()"
            >
                
                <template v-slot:filters>

                    <!-- Label -->
                    <opensilex-FilterField>
                    <opensilex-InputForm
                        :value.sync="filter.label"
                        label="component.experiment.search.filter.alias"
                        type="text"
                        placeholder="component.experiment.search.placeholder.alias"
                    ></opensilex-InputForm>
                    </opensilex-FilterField>
                    
                    <!-- Species -->
                    <opensilex-FilterField>
                    <opensilex-SelectForm
                        label="component.experiment.search.filter.species"
                        placeholder="component.experiment.search.placeholder.species"
                        :multiple="true"
                        :selected.sync="filter.species"
                        :optionsLoadingMethod="loadSpecies"
                        :conversionMethod="speciesToSelectNode"
                    ></opensilex-SelectForm>
                    </opensilex-FilterField>

                    <!-- Projects -->
                    <opensilex-FilterField>
                    <opensilex-SelectForm
                        label="component.experiment.search.filter.projects"
                        placeholder="component.experiment.search.placeholder.projects"
                        :selected.sync="filter.projects"
                        :conversionMethod="projectGetDTOToSelectNode"
                        modalComponent="opensilex-ProjectModalList"
                        :isModalSearch="true"
                    ></opensilex-SelectForm>
                    </opensilex-FilterField>

                    <!-- Installations -->
                    <opensilex-FilterField>
                    <opensilex-InputForm
                        :value.sync="filter.installations"
                        label="component.experiment.search.filter.installations"
                        type="text"
                        placeholder="component.experiment.search.placeholder.installations"
                    ></opensilex-InputForm>
                    </opensilex-FilterField>

                    <!-- Campaign -->
                    <opensilex-FilterField>
                    <opensilex-InputForm
                        :value.sync="filter.campaign"
                        label="component.experiment.search.filter.campaign"
                        type="text"
                        placeholder="component.experiment.search.placeholder.campaign"
                    ></opensilex-InputForm>
                    </opensilex-FilterField>

                    <!-- Sites -->
                    <opensilex-FilterField>
                    <opensilex-InputForm
                        :value.sync="filter.site"
                        label="component.experiment.search.filter.sites"
                        type="text"
                        placeholder="component.experiment.search.placeholder.sites"
                    ></opensilex-InputForm>
                    </opensilex-FilterField>

                    <!-- Start date -->
                    <opensilex-FilterField>
                    <opensilex-InputForm
                        :value.sync="filter.startDate"
                        label="component.experiment.search.filter.startDate"
                        type="text"
                        placeholder="component.experiment.search.placeholder.startDate"
                    ></opensilex-InputForm>
                    </opensilex-FilterField>

                    <!-- State -->
                    <opensilex-FilterField>
                    <opensilex-SelectForm
                        label="component.experiment.search.filter.state"
                        placeholder="component.experiment.search.placeholder.state"
                        :multiple="false"
                        :selected.sync="filter.state"
                        :optionsLoadingMethod="loadStates"
                    ></opensilex-SelectForm>
                    </opensilex-FilterField>
                    
                </template>

            </opensilex-SearchFilterField>

            <opensilex-TableAsyncView
            ref="tableRef" 
            :searchMethod="searchExperiments" 
            :fields="fields"
            :isSelectable="true"
            defaultSortBy="label"
            labelNumberOfSelectedRow="component.experiment.search.selectedLabel"
            iconNumberOfSelectedRow="ik#ik-layers"
            defaultPageSize="10">

            <template v-slot:actionsSelectableTable></template>
            
            <template v-slot:cell(uri)="{data}">
                <opensilex-UriLink
                :uri="data.item.uri"
                :to="{path: '/experiment/'+ encodeURIComponent(data.item.uri)}"
                ></opensilex-UriLink>
            </template>

            <template v-slot:cell(label)="{data}">{{data.item.label}}</template>

            <template v-slot:cell(species)="{data}">
                <span :key="index" v-for="(uri, index) in data.item.species">
                <span :title="uri">{{ getSpeciesName(uri) }}</span>
                <span v-if="index + 1 < data.item.species.length">,</span>
                </span>
            </template>

            <template v-slot:cell(projects)="{data}">
                <span :key="index" v-for="(uri, index) in data.item.projects">
                <span :title="uri">{{ getProjectName(uri) }}</span>
                <span v-if="index + 1 < data.item.projects.length">,</span>
                </span>
            </template>

            <template v-slot:cell(installations)>
                &nbsp;
            </template>

            <template v-slot:cell(sites)>
                &nbsp;
            </template>

            <template v-slot:cell(startDate)="{data}">
                <opensilex-DateView :value="data.item.startDate"></opensilex-DateView>
            </template>

            <template v-slot:cell(state)="{data}">
                <i
                v-if="!isEnded(data.item)"
                class="ik ik-zap badge-icon badge-info-phis"
                :title="$t('component.experiment.common.status.in-progress')"
                ></i>
                <i
                v-else
                class="ik ik-zap-off badge-icon badge-light"
                :title="$t('component.experiment.common.status.finished')"
                ></i>
                <i
                v-if="data.item.isPublic"
                class="ik ik-users badge-icon badge-info"
                :title="$t('component.experiment.common.status.public')"
                ></i>
            </template>

            </opensilex-TableAsyncView>
            
        </div>

    </b-modal>

</template>

<script lang="ts">

    import { Component } from "vue-property-decorator";
    import Vue from "vue";
    import VueConstructor from "vue";
    import VueRouter from "vue-router";
    import moment from "moment";
    import copy from "copy-to-clipboard"; 
    import VueI18n from 'vue-i18n';
    import { 
        ProjectCreationDTO, 
        SpeciesDTO, 
        ExperimentGetDTO, 
        ResourceTreeDTO,
        InfrastructureGetDTO,
        ExperimentsService, 
        InfrastructuresService,
        ProjectsService, 
        SpeciesService
    } from "opensilex-core/index";
    import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
    import ExperimentList from './ExperimentList.vue';

    @Component
    export default class ExperimentModalList extends ExperimentList {

        fields = [
            {
                key: "uri",
                label: "component.common.uri",
                sortable: true
            },
            {
                key: "label",
                label: "component.common.name",
                sortable: true
            },
            {
                key: "species",
                label: "component.experiment.species"
            },
            {
                key: "projects",
                label: "component.experiment.search.column.projects",
            },
            {
                key: "installations",
                label: "component.experiment.search.column.installations",
            },
            {
                key: "campaign",
                label: "component.experiment.search.column.campaign",
                sortable: true
            },
            {
                key: "sites",
                label: "component.experiment.search.column.sites",
            },
            {
                key: "startDate",
                label: "component.experiment.startDate",
                sortable: true
            },
            {
                key: "state",
                label: "component.experiment.search.column.state"
            }
        ];

        show() {
            let modalRef: any = this.$refs.modalRef;
            modalRef.show();
        }

        hide(validate: boolean) {
            let modalRef: any = this.$refs.modalRef;
            modalRef.hide();

            if(validate) {
                this.$emit("onValidate", this.tableRef.getSelected());
            }
        }

    }

</script>

<style scoped lang="scss">

</style>
