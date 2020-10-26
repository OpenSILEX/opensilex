<template>
  <div>
    <opensilex-ExperimentForm
      v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
      ref="experimentForm"
      @onUpdate="loadExperiment()"
    ></opensilex-ExperimentForm>

    <div v-if="experiment" class="row">
      <div class="col col-xl-6" style="min-width: 400px">
        <opensilex-Card icon="ik#ik-clipboard" :label="$t('component.experiment.description')">
          <template v-slot:rightHeader>
            <b-button-group
              v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
            >
              <opensilex-EditButton
                v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
                @click="showEditForm()"
                label="component.experiment.update"
              ></opensilex-EditButton>

              <opensilex-DeleteButton
                v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_DELETE_ID)"
                @click="deleteExperiment(experiment.uri)"
                label="component.experiment.delete"
              ></opensilex-DeleteButton>
            </b-button-group>
          </template>

          <template v-slot:body>
            <opensilex-StringView label="component.common.name" :value="experiment.label"></opensilex-StringView>
            <div class="static-field">
              <span
                class="field-view-title"
              >{{ $t('component.common.state') }}{{ $t('component.common.colon') }}</span>
              <span class="static-field-line">
                <span
                  v-if="!isEnded(experiment)"
                  class="badge badge-pill badge-info-phis"
                  :title="$t('component.experiment.view.status.in-progress')"
                >
                  <i class="ik ik-activity mr-1"></i>
                  {{ $t("component.experiment.common.status.in-progress") }}
                </span>
                <span
                  v-else
                  class="badge badge-pill badge-light"
                  :title="$t('component.experiment.view.status.finished')"
                >
                  <i class="ik ik-archive"></i>
                  {{ $t("component.experiment.common.status.finished") }}
                </span>

                <span
                  v-if="experiment.isPublic"
                  class="badge badge-pill badge-info"
                  :title="$t('component.experiment.view.status.public')"
                >
                  <i class="ik ik-users mr-1"></i>
                  {{ $t("component.experiment.common.status.public") }}
                </span>
              </span>
            </div>
            <opensilex-StringView label="component.common.period" :value="period"></opensilex-StringView>
            <opensilex-UriView :uri="experiment.uri"></opensilex-UriView>
            <opensilex-TextView
              label="component.experiment.objective"
              :value="experiment.objective"
            ></opensilex-TextView>
            <opensilex-TextView label="component.experiment.comment" :value="experiment.comment"></opensilex-TextView>
          </template>
        </opensilex-Card>
      </div>

      <div class="col col-xl-6">
        <opensilex-Card icon="ik#ik-box" :label="$t('component.experiment.context')">
          <template v-slot:body>
            <opensilex-UriListView label="component.experiment.projects" :list="projectsList"></opensilex-UriListView>
            <opensilex-UriListView
              label="component.experiment.infrastructures"
              :list="infrastructuresList"
            ></opensilex-UriListView>
            <opensilex-UriListView label="component.experiment.species" :list="speciesList"></opensilex-UriListView>
            <opensilex-UriListView
              label="component.menu.experimentalDesign.factors"
              :list="factorsList"
            ></opensilex-UriListView>
            <opensilex-UriListView label="component.experiment.groups" :list="groupsList"></opensilex-UriListView>
          </template>
        </opensilex-Card>

        <opensilex-Card icon="ik#ik-users" :label="$t('component.experiment.contacts')">
          <template v-slot:body>
            <opensilex-UriListView
              label="component.experiment.scientificSupervisors"
              :list="scientificSupervisorsList"
            ></opensilex-UriListView>
            <opensilex-UriListView
              label="component.experiment.technicalSupervisors"
              :list="technicalSupervisorsList"
            ></opensilex-UriListView>
          </template>
        </opensilex-Card>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import {
  ExperimentCreationDTO,
  ExperimentGetDTO,
  ExperimentsService,
  ProjectCreationDTO,
  InfrastructureGetDTO,
  ProjectsService,
  InfrastructuresService,
  SpeciesDTO,
  SpeciesService,
  FactorsService,
  FactorGetDTO
} from "opensilex-core/index";
import {
  SecurityService,
  GroupDTO,
  UserGetDTO
} from "opensilex-security/index";
import VueI18n from "vue-i18n";
import moment from "moment";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
@Component
export default class ExperimentDetail extends Vue {
  $opensilex: any;
  $t: any;
  $route: any;
  $store: any;

  uri: string = "";
  period: string = "";

  @Ref("experimentForm") readonly experimentForm!: any;
  experiment: any = null;
  service: ExperimentsService;

  speciesList = [];
  factorsList = [];
  groupsList = [];
  projectsList = [];
  scientificSupervisorsList = [];
  technicalSupervisorsList = [];
  installationsList = [];
  infrastructuresList = [];

  created() {
    this.service = this.$opensilex.getService("opensilex.ExperimentsService");
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.loadExperiment();
  }

  showEditForm() {
    this.convertDtoBeforeEditForm();
    this.experimentForm.showEditForm(this.experiment);
  }

  convertDtoBeforeEditForm() {
    if (
      this.experiment.projects &&
      this.experiment.projects.length &&
      this.experiment.projects[0].uri
    ) {
      this.experiment.projects = this.experiment.projects.map(project => {
        return project.uri;
      });
    }
    if (
      this.experiment.infrastructures &&
      this.experiment.infrastructures.length &&
      this.experiment.infrastructures[0].uri
    ) {
      this.experiment.infrastructures = this.experiment.infrastructures.map(
        infrastructure => {
          return infrastructure.uri;
        }
      );
    }
  }

  deleteExperiment(uri: string) {
    this.service
      .deleteExperiment(uri)
      .then(() => {
        this.$router.go(-1);
      })
      .catch(this.$opensilex.errorHandler);
  }

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  loadExperiment() {
    if (this.uri) {
      this.service
        .getExperiment(this.uri)
        .then((http: HttpResponse<OpenSilexResponse<ExperimentGetDTO>>) => {
          this.experiment = http.response.result;
          this.loadExperimentDetails();
        })
        .catch(error => {
          this.$opensilex.errorHandler(error);
        });
    }
  }

  loadExperimentDetails() {
    this.loadProjects();
    this.loadInfrastructures();
    this.loadUsers();
    this.loadGroups();
    this.loadFactors();
    this.loadSpecies();

    this.period = this.formatPeriod(
      this.experiment.startDate,
      this.experiment.endDate
    );
  }

  loadInfrastructures() {
    let service: InfrastructuresService = this.$opensilex.getService(
      "opensilex.InfrastructuresService"
    );
    this.infrastructuresList = [];

    if (
      this.experiment.infrastructures &&
      this.experiment.infrastructures.length > 0
    ) {
      this.experiment.infrastructures.forEach(infrastructure => {
        this.infrastructuresList.push({
          uri: infrastructure.uri,
          value: infrastructure.name
        });
      });
    }
  }

  loadGroups() {
    let service: SecurityService = this.$opensilex.getService(
      "opensilex.SecurityService"
    );
    this.groupsList = [];
    if (this.experiment.groups && this.experiment.groups.length > 0) {
      service
        .getGroupsByURI(this.experiment.groups)
        .then((http: HttpResponse<OpenSilexResponse<GroupDTO[]>>) => {
          this.groupsList = http.response.result.map(group => {
            return {
              uri: group.uri,
              value: group.name
            };
          });
        })
        .catch(this.$opensilex.errorHandler);
    }
  }

  loadUsers() {
    let service: SecurityService = this.$opensilex.getService(
      "opensilex.SecurityService"
    );
    this.scientificSupervisorsList = [];
    if (
      this.experiment.scientificSupervisors &&
      this.experiment.scientificSupervisors.length > 0
    ) {
      service
        .getUsersByURI(this.experiment.scientificSupervisors)
        .then((http: HttpResponse<OpenSilexResponse<UserGetDTO[]>>) => {
          this.scientificSupervisorsList = http.response.result.map(item => {
            return {
              uri: item.email,
              url: "mailto:" + item.email,
              value: item.firstName + " " + item.lastName
            };
          });
        })
        .catch(this.$opensilex.errorHandler);
    }
    this.technicalSupervisorsList = [];
    if (
      this.experiment.technicalSupervisors &&
      this.experiment.technicalSupervisors.length > 0
    ) {
      service
        .getUsersByURI(this.experiment.technicalSupervisors)
        .then((http: HttpResponse<OpenSilexResponse<UserGetDTO[]>>) => {
          this.technicalSupervisorsList = http.response.result.map(item => {
            return {
              uri: item.email,
              url: "mailto:" + item.email,
              value: item.firstName + " " + item.lastName
            };
          });
        })
        .catch(this.$opensilex.errorHandler);
    }
  }

  loadSpecies() {
    let service: SpeciesService = this.$opensilex.getService(
      "opensilex.SpeciesService"
    );
    this.speciesList = [];
    if (this.experiment.species && this.experiment.species.length > 0) {
      service
        .getAllSpecies()
        .then((http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>>) => {
          for (let i = 0; i < http.response.result.length; i++) {
            if (
              this.experiment.species.find(
                species => species == http.response.result[i].uri
              )
            ) {
              this.speciesList.push(http.response.result[i]);
            }
          }

          this.speciesList = this.speciesList.map(item => {
            return {
              uri: item.uri,
              value: item.label
            };
          });
        })
        .catch(this.$opensilex.errorHandler);
    }
  }

  loadFactors() {
    let service: FactorsService = this.$opensilex.getService(
      "opensilex.FactorsService"
    );
    this.factorsList = [];
    if (this.experiment.factors && this.experiment.factors.length > 0) {
      service
        .getAllFactors()
        .then((http: HttpResponse<OpenSilexResponse<Array<FactorGetDTO>>>) => {
          for (let i = 0; i < http.response.result.length; i++) {
            if (
              this.experiment.factors.find(
                factors => factors == http.response.result[i].uri
              )
            ) {
              this.factorsList.push(http.response.result[i]);
            }
          }

          this.factorsList = this.factorsList.map(item => {
            return {
              uri: item.uri,
              value: item.name,
              to: {
                path: "/factor/details/" + encodeURIComponent(item.uri)
              }
            };
          });
        })
        .catch(this.$opensilex.errorHandler);
    }
  }

  loadProjects() {
    let service: ProjectsService = this.$opensilex.getService(
      "opensilex.ProjectsService"
    );
    this.projectsList = [];
    if (this.experiment.projects) {
      this.experiment.projects.forEach(project => {
        this.projectsList.push({
          uri: project.uri,
          value: project.name,
          to: {
            path: "/project/details/" + encodeURIComponent(project.uri)
          }
        });
      });
    }
  }

  isEnded(experiment) {
    if (experiment.endDate) {
      return moment(experiment.endDate, "YYYY-MM-DD").diff(moment()) < 0;
    }
    return false;
  }

  formatPeriod(startDateValue: string, endDateValue: string) {
    let startDate = moment(startDateValue, "YYYY-MM-DD");
    let endDate;
    let result = this.$opensilex.formatDate(startDateValue);

    if (endDateValue) {
      endDate = moment(endDateValue, "YYYY-MM-DD");
      result += " - " + this.$opensilex.formatDate(endDateValue);
    } else {
      endDate = moment();
    }

    let period = endDate.diff(startDate);
    let duration = Math.floor(moment.duration(period).asMonths());

    result +=
      " (" +
      duration +
      " " +
      this.$t("component.common.months").toString() +
      ")";

    return result;
  }
}
</script>

<style scoped lang="scss">
</style>
