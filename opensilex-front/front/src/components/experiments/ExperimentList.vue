<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-layers"
      title="component.menu.experiments"
      description="component.experiment.search.description"
    ></opensilex-PageHeader>

    <opensilex-PageActions>
      <template v-slot>
        <opensilex-CreateButton
          v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
          @click="experimentForm.showCreateForm()"
          label="component.experiment.search.buttons.create-experiment"
        ></opensilex-CreateButton>
      </template>
    </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-TableAsyncView ref="tableRef" :searchMethod="searchExperiments" :fields="fields">
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

          <template v-slot:cell(startDate)="{data}">
            <opensilex-DateView :value="data.item.startDate"></opensilex-DateView>
          </template>
          <template v-slot:cell(endDate)="{data}">
            <opensilex-DateView :value="data.item.endDate"></opensilex-DateView>
          </template>

          <template v-slot:cell(state)="{data}">
            <i
              v-if="!isEnded(data.item)"
              class="ik ik-activity badge-icon badge-info-opensilex"
              :title="$t('component.experiment.common.status.in-progress')"
            ></i>
            <i
              v-else
              class="ik ik-archive badge-icon badge-light"
              :title="$t('component.experiment.common.status.finished')"
            ></i>
            <i
              v-if="data.item.isPublic"
              class="ik ik-users badge-icon badge-info"
              :title="$t('component.experiment.common.status.public')"
            ></i>
          </template>

          <template v-slot:cell(actions)="{data}">
            <b-button-group size="sm">
              <opensilex-EditButton
                v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
                @click="experimentForm.showEditForm(data.item)"
                label="component.experiment.update"
                :small="true"
              ></opensilex-EditButton>
              <opensilex-DeleteButton
                v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_DELETE_ID)"
                @click="deleteExperiment(data.item.uri)"
                label="component.experiment.delete"
                :small="true"
              ></opensilex-DeleteButton>
            </b-button-group>
          </template>
        </opensilex-TableAsyncView>
      </template>
    </opensilex-PageContent>
    <opensilex-ExperimentForm
      v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
      ref="experimentForm"
      @onCreate="refresh()"
      @onUpdate="refresh()"
    ></opensilex-ExperimentForm>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueConstructor from "vue";
import copy from "copy-to-clipboard";
import VueI18n from "vue-i18n";
import moment from 'moment';
import {
  ProjectGetDTO,
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
import { UserGetDTO } from "opensilex-security/index";

export class ExperimentState {
  code: String;
  label: String;

  constructor(code: String, label: String) {
    this.code = code;
    this.label = label;
  }
}

@Component
export default class ExperimentList extends Vue {
  $opensilex: any;

  @Ref("experimentForm") readonly experimentForm!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  speciesByUri: Map<String, SpeciesDTO> = new Map<String, SpeciesDTO>();

  @Ref("tableRef") readonly tableRef!: any;

  refresh() {
    this.tableRef.refresh();
  }

  searchExperiments(options) {
    return this.$opensilex
      .getService("opensilex.ExperimentsService")
      .searchExperiments(
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        0,
        1000
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<ProjectGetDTO>>>) => {
        let results: Map<String, ProjectGetDTO> = new Map<
          String,
          ProjectGetDTO
        >();
        let resultsList = [];
        for (let i = 0; i < http.response.result.length; i++) {
          results.set(http.response.result[i].uri, http.response.result[i]);
          resultsList.push(http.response.result[i]);
        }
        this.projectsList = resultsList;
        this.projectsByUri = results;
      })
      .catch(this.$opensilex.errorHandler);
  }

  loadInfrastructures() {
    let service: InfrastructuresService = this.$opensilex.getService(
      "opensilex.InfrastructuresService"
    );

    service
      .searchInfrastructuresTree(this.user.getAuthorizationHeader(), undefined)
      .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
        let results: Map<String, ResourceTreeDTO> = new Map<
          String,
          ResourceTreeDTO
        >();
        let resultsList = [];
        for (let i = 0; i < http.response.result.length; i++) {
          results.set(http.response.result[i].uri, http.response.result[i]);
          resultsList.push(http.response.result[i]);
        }
        this.infrastructuresList = resultsList;
        this.infrastructuresByUri = results;
      })
      .catch(this.$opensilex.errorHandler);
  }

  created() {
    let service: SpeciesService = this.$opensilex.getService(
      "opensilex.SpeciesService"
    );

    this.$opensilex.disableLoader();
    service
      .getAllSpecies()
      .then((http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>>) => {
        for (let i = 0; i < http.response.result.length; i++) {
          this.speciesByUri.set(
            http.response.result[i].uri,
            http.response.result[i]
          );
        }
        this.$opensilex.enableLoader();
      })
      .catch(this.$opensilex.errorHandler);
  }

  getSpeciesName(uri: String): String {
    if (this.speciesByUri.has(uri)) {
      return this.speciesByUri.get(uri).label;
    }
    return null;
  }

  isEnded(experiment) {
    if (experiment.endDate) {
      return moment(experiment.endDate, "YYYY-MM-DD").diff(moment()) < 0;
    } 

    return false;
  }

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
      key: "startDate",
      label: "component.experiment.startDate",
      sortable: true
    },
    {
      key: "endDate",
      label: "component.experiment.endDate",
      sortable: true
    },
    {
      key: "state",
      label: "component.experiment.search.column.state"
    },
    {
      label: "component.common.actions",
      key: "actions"
    }
  ];

  deleteExperiment(uri: string) {
    this.$opensilex
      .getService("opensilex.ExperimentsService")
      .deleteExperiment(uri)
      .then(() => {
        this.refresh();
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>


<style scoped lang="scss">
</style>