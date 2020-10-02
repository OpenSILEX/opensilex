<template>
  <div>
    <div v-if="experiment" class="row">
      <div class="col col-xl-6" style="min-width: 400px">
        <opensilex-Card
          icon="ik#ik-clipboard"
          :label="$t('component.experiment.description')"
        >
          <template v-slot:rightHeader>
            <span
              v-if="!experiment.isEnded"
              class="badge badge-pill badge-info-phis"
              :title="$t('component.experiment.view.status.finished')"
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
          </template>
          <template v-slot:body>
            <opensilex-StringView
              label="component.common.name"
              :value="experiment.label"
            ></opensilex-StringView>
            <opensilex-UriView :uri="experiment.uri"></opensilex-UriView>

            <opensilex-StringView
              label="component.common.period"
              :value="period"
            ></opensilex-StringView>
            <opensilex-TextView
              label="component.experiment.objective"
              :value="experiment.objective"
            ></opensilex-TextView>
            <opensilex-TextView
              label="component.experiment.comment"
              :value="experiment.comment"
            ></opensilex-TextView>
          </template>
        </opensilex-Card>
      </div>
      <div class="col col-xl-6">
        <opensilex-Card
          icon="ik#ik-box"
          :label="$t('component.experiment.context')"
        >
          <template v-slot:body>
            <!--   <div class="static-field">
              <span
                class="static-field-key"
              >{{ $t('component.experiment.installations') }}{{ $t('component.common.colon') }}</span>
              <span class="static-field-line"></span>
            </div>-->

            <opensilex-UriListView
              label="component.experiment.projects"
              :list="projectsList"
              to="project"
            ></opensilex-UriListView>

            <!--     <div class="static-field">
              <span
                class="static-field-key"
              >{{ $t('component.experiment.campaign') }}{{ $t('component.common.colon') }}</span>
              <span class="static-field-line">{{ experiment.campaign }}</span>
            </div>-->

            <opensilex-UriListView
              label="component.experiment.infrastructures"
              :list="infrastructuresList"
            ></opensilex-UriListView>

            <opensilex-UriListView
              label="component.experiment.species"
              :list="speciesList"
            ></opensilex-UriListView>

            <opensilex-UriListView
              label="component.menu.experimentalDesign.factors"
              :list="factorsList"
              to="factor/details"
            ></opensilex-UriListView>

            <opensilex-UriListView
              label="component.experiment.groups"
              :list="groupsList"
            ></opensilex-UriListView>
          </template>
        </opensilex-Card>
        <opensilex-Card
          icon="ik#ik-users"
          :label="$t('component.experiment.contacts')"
        >
          <template v-slot:body>
            <opensilex-UriListView
              label="component.experiment.scientificSupervisors"
              :list="scientificSupervisorsList"
              to="contact"
            ></opensilex-UriListView>

            <opensilex-UriListView
              label="component.experiment.technicalSupervisors"
              :list="technicalSupervisorsList"
              to="contact"
            ></opensilex-UriListView>
          </template>
        </opensilex-Card>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
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
  FactorGetDTO,
} from "opensilex-core/index";
import {
  SecurityService,
  GroupDTO,
  UserGetDTO,
} from "opensilex-security/index";
import VueI18n from "vue-i18n";
import moment from "moment";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
@Component
export default class ExperimentDetail extends Vue {
  $opensilex: any;
  $t: any;

  uri: string = "";
  period: string = "";

  experiment: ExperimentGetDTO = null;

  speciesList = [];
  factorsList = [];
  groupsList = [];
  projectsList = [];
  scientificSupervisorsList = [];
  technicalSupervisorsList = [];
  installationsList = [];
  infrastructuresList = [];

  previousExperiment: string = null;
  nextExperiment: string = null;

  created() {
    this.uri = this.$route.params.uri;
    this.loadExperiment();
  }

  get user() {
    return this.$store.state.user;
  }

  loadExperiment() {
    let service: ExperimentsService = this.$opensilex.getService(
      "opensilex.ExperimentsService"
    );

    if (this.uri) {
      service
        .getExperiment(this.uri)
        .then((http: HttpResponse<OpenSilexResponse<ExperimentGetDTO>>) => {
          this.experiment = http.response.result;

          this.loadProjects();
          this.loadUsers();
          this.loadSpecies();
          this.loadFactors();
          this.loadGroups();
          this.loadInstallations();
          this.loadInfrastructures();

          this.period = this.formatPeriod(
            this.experiment.startDate,
            this.experiment.endDate
          );
          this.setPreviousAndNextExperiment();
        })
        .catch((error) => {
          this.$opensilex.errorHandler(error);
        });
    }
  }

  setPreviousAndNextExperiment() {
    if (this.$store.state.search.results) {
      let length = this.$store.state.search.results.length;
      let index = this.$store.state.search.results.indexOf(this.uri);

      if (index > 0) {
        this.previousExperiment = this.$store.state.search.results[index - 1];
      }
      if (index < length) {
        this.nextExperiment = this.$store.state.search.results[index + 1];
      }
    }
  }

  loadInstallations() {}

  loadInfrastructures() {
    let service: InfrastructuresService = this.$opensilex.getService(
      "opensilex.InfrastructuresService"
    );

    if (this.experiment.infrastructures) {
      this.experiment.infrastructures.forEach((infrastructure) => {
        service
          .getInfrastructure(infrastructure)
          .then(
            (http: HttpResponse<OpenSilexResponse<InfrastructureGetDTO>>) => {
              this.infrastructuresList.push(http.response.result);
            }
          )
          .catch(this.$opensilex.errorHandler);
      });
    }
  }

  loadGroups() {
    let service: SecurityService = this.$opensilex.getService(
      "opensilex.SecurityService"
    );

    if (this.experiment.groups) {
      this.experiment.groups.forEach((group) => {
        service
          .getGroup(group)
          .then((http: HttpResponse<OpenSilexResponse<GroupDTO>>) => {
            this.groupsList.push(http.response.result);
          })
          .catch(this.$opensilex.errorHandler);
      });
    }
  }

  loadUsers() {
    let service: SecurityService = this.$opensilex.getService(
      "opensilex.SecurityService"
    );
    if (this.experiment.scientificSupervisors.length) {
      service
        .getUsersByURI(this.experiment.scientificSupervisors)
        .then((http: HttpResponse<OpenSilexResponse<UserGetDTO[]>>) => {
          this.scientificSupervisorsList = http.response.result.map((item) => {
            return {
              uri: item.uri,
              name: item.firstName + " " + item.lastName,
            };
          });
        })
        .catch(this.$opensilex.errorHandler);
    }

    if (this.experiment.technicalSupervisors.length) {
      service
        .getUsersByURI(this.experiment.technicalSupervisors)
        .then((http: HttpResponse<OpenSilexResponse<UserGetDTO[]>>) => {
          this.technicalSupervisorsList = http.response.result.map((item) => {
            return {
              uri: item.uri,
              name: item.firstName + " " + item.lastName,
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

    if (this.experiment.species) {
      service
        .getAllSpecies()
        .then((http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>>) => {
          for (let i = 0; i < http.response.result.length; i++) {
            if (
              this.experiment.species.find(
                (species) => species == http.response.result[i].uri
              )
            ) {
              this.speciesList.push(http.response.result[i]);
            }
          }

          this.speciesList = this.speciesList.map((item) => {
            return {
              uri: item.uri,
              name: item.label,
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

    if (this.experiment.species) {
      service
        .getAllFactors()
        .then((http: HttpResponse<OpenSilexResponse<Array<FactorGetDTO>>>) => {
          for (let i = 0; i < http.response.result.length; i++) {
            if (
              this.experiment.factors.find(
                (factors) => factors == http.response.result[i].uri
              )
            ) {
              this.factorsList.push(http.response.result[i]);
            }
          }
        })
        .catch(this.$opensilex.errorHandler);
    }
  }

  loadProjects() {
    let service: ProjectsService = this.$opensilex.getService(
      "opensilex.ProjectsService"
    );

    if (this.experiment.projects) {
      this.experiment.projects.forEach((project) => {
        service
          .getProject(project)
          .then((http: HttpResponse<OpenSilexResponse<ProjectCreationDTO>>) => {
            this.projectsList.push(http.response.result);
          })
          .catch(this.$opensilex.errorHandler);
      });
    }
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
