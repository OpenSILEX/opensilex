<template>
  <div>
    <div v-if="experiment" class="row">
      <div class="col col-xl-6" style="min-width: 400px">
        <div class="card">
          <div class="card-header">
            <h3>
              <i class="ik ik-clipboard"></i>
              {{ $t('component.experiment.description') }}
            </h3>
            <div class="card-header-right">
              <span
                v-if="!experiment.isEnded"
                class="badge badge-pill badge-info-phis"
                :title="$t('component.experiment.view.status.finished')"
              >
                <i class="ik ik-activity mr-1"></i>
                {{ $t('component.experiment.common.status.in-progress') }}
              </span>
              <span
                v-else
                class="badge badge-pill badge-light"
                :title="$t('component.experiment.view.status.finished')"
              >
                <i class="ik ik-archive"></i>
                {{ $t('component.experiment.common.status.finished') }}
              </span>

              <span
                v-if="experiment.isPublic"
                class="badge badge-pill badge-info"
                :title="$t('component.experiment.view.status.public')"
              >
                <i class="ik ik-users mr-1"></i>
                {{ $t('component.experiment.common.status.public') }}
              </span>
            </div>
          </div>
          <div class="card-body">
            <div class="static-field">
              <span
                class="static-field-key"
              >{{ $t('component.experiment.alias') }}{{ $t('component.common.colon') }}</span>
              <span class="static-field-line">{{ experiment.label }}</span>
            </div>
            <div class="static-field">
              <span
                class="static-field-key"
              >{{ $t('component.experiment.uri') }}{{ $t('component.common.colon') }}</span>
              <span class="inline-action static-field-line">
                {{ experiment.uri }}
                <a
                  href="#"
                  class="inline-action-btn"
                  v-on:click="copyUri(experiment.uri, $event)"
                  :title="$t('component.copyToClipboard.copyUri')"
                >
                  <i class="ik ik-copy"></i>
                </a>
              </span>
            </div>
            <div class="static-field">
              <span
                class="static-field-key"
              >{{ $t('component.experiment.period') }}{{ $t('component.common.colon') }}</span>
              <span class="static-field-line">{{ period }}</span>
            </div>
            <div class="static-field">
              <span
                class="static-field-key"
              >{{ $t('component.experiment.objective') }}{{ $t('component.common.colon') }}</span>
              <div class="static-field-text">{{ experiment.objective }}</div>
            </div>
            <div class="static-field">
              <span
                class="static-field-key"
              >{{ $t('component.experiment.comment') }}{{ $t('component.common.colon') }}</span>
              <div class="static-field-text">{{ experiment.comment }}</div>
            </div>
          </div>
          <div class="card-footer text-center">
            <h6>{{ $t('component.experiment.keywords') }}</h6>
            <span :key="index" v-for="(uri, index) in experiment.keywords">
              <span class="keyword badge badge-pill badge-dark">{{ uri }}</span>
            </span>
          </div>
        </div>
      </div>
      <div class="col col-xl-6">
        <div class="card">
          <div class="card-header">
            <h3>
              <i class="ik ik-box"></i>
              {{ $t('component.experiment.context') }}
            </h3>
          </div>
          <div class="card-body">
            <div class="static-field">
              <span
                class="static-field-key"
              >{{ $t('component.experiment.installations') }}{{ $t('component.common.colon') }}</span>
              <span class="static-field-line"></span>
            </div>
            <div class="static-field">
              <span
                class="static-field-key"
              >{{ $t('component.experiment.projects') }}{{ $t('component.common.colon') }}</span>
              <span class="static-field-line">
                <span :key="index" v-for="(project, index) in projectsList">
                  <span :title="uri">{{ project.label }}</span>
                  <span v-if="index + 1 < projectsList.length">,</span>
                </span>
              </span>
            </div>
            <div class="static-field">
              <span
                class="static-field-key"
              >{{ $t('component.experiment.campaign') }}{{ $t('component.common.colon') }}</span>
              <span class="static-field-line">{{ experiment.campaign }}</span>
            </div>
            <div class="static-field">
              <span
                class="static-field-key"
              >{{ $t('component.experiment.places') }}{{ $t('component.common.colon') }}</span>
              <span class="static-field-line">
                <span :key="index" v-for="(infrastructure, index) in infrastructuresList">
                  <span :title="infrastructure.uri">{{ infrastructure.name }}</span>
                  <span v-if="index + 1 < infrastructuresList.length">,</span>
                </span>
              </span>
            </div>
            <div class="static-field">
              <span
                class="static-field-key"
              >{{ $t('component.experiment.species') }}{{ $t('component.common.colon') }}</span>
              <span class="static-field-line">
                <span :key="index" v-for="(species, index) in speciesList">
                  <span :title="species.uri">{{ species.label }}</span>
                  <span v-if="index + 1 < speciesList.length">,</span>
                </span>
              </span>
            </div>
            <div class="static-field">
              <span
                class="static-field-key"
              >{{ $t('component.menu.experimentalDesign.factors') }}{{ $t('component.common.colon') }}</span>
              <span class="static-field-line">
                <span :key="index" v-for="(factor, index) in factorsList">
                  <span :title="factor.name">
                    <opensilex-UriLink
                      :uri="factor.uri"
                      :value="factor.name"
                      :to="{path: '/factor/details/'+ encodeURIComponent(factor.uri)}"
                    ></opensilex-UriLink>
                  </span>
                  <span v-if="index + 1 < factor.length">,</span>
                </span>
              </span>
            </div>
            <div class="static-field">
              <span
                class="static-field-key"
              >{{ $t('component.experiment.groups') }}{{ $t('component.common.colon') }}</span>
              <ul class="static-field-list" :key="index" v-for="(group, index) in groupsList">
                <li class="inline-action">
                  <span :title="group.uri">{{ group.name }}</span>
                </li>
              </ul>
            </div>
          </div>
        </div>
        <div class="card">
          <div class="card-header">
            <h3>
              <i class="ik ik-users"></i>
              {{ $t('component.experiment.contacts') }}
            </h3>
          </div>
          <div class="card-body">
            <div class="static-field">
              <span
                class="static-field-key"
              >{{ $t('component.experiment.scientificSupervisors') }}{{ $t('component.common.colon') }}</span>
              <ul
                class="static-field-list"
                :key="index"
                v-for="(scientificSupervisor, index) in scientificSupervisorsList"
              >
                <li class="inline-action">
                  <span class="uri inline-action">
                    {{ scientificSupervisor.firstName }} {{ scientificSupervisor.lastName }}
                    <a
                      href="#"
                      v-on:click="copyUri(scientificSupervisor.email, $event)"
                      class="inline-action-btn"
                      :title="$t('component.copyToClipboard.copyEmail')"
                    >
                      <i class="ik ik-copy"></i>
                    </a>
                  </span>
                </li>
              </ul>
            </div>
            <div class="static-field">
              <span
                class="static-field-key"
              >{{ $t('component.experiment.technicalSupervisors') }}{{ $t('component.common.colon') }}</span>
              <ul
                class="static-field-list"
                :key="index"
                v-for="(technicalSupervisor, index) in technicalSupervisorsList"
              >
                <li class="inline-action">
                  <span class="uri inline-action">
                    {{ technicalSupervisor.firstName }} {{ technicalSupervisor.lastName }}
                    <a
                      href="#"
                      v-on:click="copyUri(technicalSupervisor.email, $event)"
                      class="inline-action-btn"
                      :title="$t('component.copyToClipboard.copyEmail')"
                    >
                      <i class="ik ik-copy"></i>
                    </a>
                  </span>
                </li>
              </ul>
            </div>
          </div>
        </div>
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
  FactorGetDTO
} from "opensilex-core/index";
import {
  SecurityService,
  GroupDTO,
  UserGetDTO
} from "opensilex-security/index";
import VueI18n from "vue-i18n";
import moment from "moment";
import copy from "copy-to-clipboard";
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
        .catch(error => {
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
      this.experiment.infrastructures.forEach(infrastructure => {
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
      this.experiment.groups.forEach(group => {
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

    if (this.experiment.scientificSupervisors) {
      this.experiment.scientificSupervisors.forEach(scientificSupervisor => {
        service
          .getUser(scientificSupervisor)
          .then((http: HttpResponse<OpenSilexResponse<UserGetDTO>>) => {
            this.scientificSupervisorsList.push(http.response.result);
          })
          .catch(this.$opensilex.errorHandler);
      });
    }

    if (this.experiment.technicalSupervisors) {
      this.experiment.technicalSupervisors.forEach(technicalSupervisor => {
        service
          .getUser(technicalSupervisor)
          .then((http: HttpResponse<OpenSilexResponse<UserGetDTO>>) => {
            this.technicalSupervisorsList.push(http.response.result);
          })
          .catch(this.$opensilex.errorHandler);
      });
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
                species => species == http.response.result[i].uri
              )
            ) {
              this.speciesList.push(http.response.result[i]);
            }
          }
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
                factors => factors == http.response.result[i].uri
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
      this.experiment.projects.forEach(project => {
        service
          .getProject(project)
          .then((http: HttpResponse<OpenSilexResponse<ProjectCreationDTO>>) => {
            this.projectsList.push(http.response.result);
          })
          .catch(this.$opensilex.errorHandler);
      });
    }
  }

  copyUri(uri: string, event) {
    copy(uri);
    if (event) {
      event.preventDefault();
    }
  }

  formatPeriod(startDateValue: string, endDateValue: string) {
    let statDate = moment(startDateValue, "YYYY-MM-DD");
    let endDate;
    let result = this.$opensilex.formatDate(startDateValue);

    if (endDateValue) {
      endDate = moment(endDateValue, "YYYY-MM-DD");
      result += " - " + this.$opensilex.formatDate(endDateValue);
    } else {
      endDate = moment();
    }

    let period = endDate.diff(statDate);
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
