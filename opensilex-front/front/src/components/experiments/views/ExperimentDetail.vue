<template>
  <div>
    <opensilex-ExperimentForm
      v-if="
        user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)
      "
      ref="experimentForm"
      @onUpdate="loadExperiment()"
    ></opensilex-ExperimentForm>

    <div v-if="experiment" class="row">
      <div class="col col-xl-6" style="min-width: 400px">
        <opensilex-Card
          icon="ik#ik-clipboard"
          :label="$t('component.experiment.description')"
        >
          <template v-slot:rightHeader>
            <b-button-group
              v-if="
                user.hasCredential(
                  credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID
                )
              "
            >
              <opensilex-EditButton
                v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID
                  )
                "
                @click="showEditForm()"
                label="component.experiment.update"
              ></opensilex-EditButton>

              <opensilex-DeleteButton
                v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_EXPERIMENT_DELETE_ID
                  )
                "
                @click="deleteExperiment(experiment.uri)"
                label="component.experiment.delete"
              ></opensilex-DeleteButton>
            </b-button-group>
          </template>

          <template v-slot:body>
            <opensilex-StringView
              label="component.common.name"
              :value="experiment.name"
            ></opensilex-StringView>
            <div class="static-field">
              <span class="field-view-title">{{
                $t("component.common.state")
              }}</span>
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
                  v-if="experiment.is_public"
                  class="badge badge-pill badge-info"
                  :title="$t('component.experiment.view.status.public')"
                >
                  <i class="ik ik-users mr-1"></i>
                  {{ $t("component.experiment.common.status.public") }}
                </span>
              </span>
            </div>
            <opensilex-StringView
              label="component.common.period"
              :value="period"
            ></opensilex-StringView>
            <opensilex-UriView :uri="experiment.uri"></opensilex-UriView>
            <opensilex-TextView
              label="component.experiment.objective"
              :value="experiment.objective"
            ></opensilex-TextView>
            <opensilex-TextView
              label="component.experiment.comment"
              :value="experiment.description"
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
            <opensilex-UriListView
              label="component.experiment.projects"
              :list="projectsList"
            ></opensilex-UriListView>
            <opensilex-UriListView
              label="component.experiment.infrastructures"
              :list="infrastructuresListURIs"
            ></opensilex-UriListView>
            <opensilex-UriListView
              label="component.experiment.facilities"
              :list="facilityListUris"
            ></opensilex-UriListView>
            <opensilex-UriListView
              label="component.experiment.species"
              :list="speciesList"
            ></opensilex-UriListView>
            <opensilex-UriListView
              label="component.menu.experimentalDesign.factors"
              :list="factorsList"
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
// @ts-ignore
import { ExperimentGetDTO, ExperimentsService, ProjectsService, OrganizationsService, SpeciesDTO, SpeciesService, FactorsService, FactorGetDTO } from "opensilex-core/index";
// @ts-ignore
import { SecurityService, GroupDTO, UserGetDTO } from "opensilex-security/index";
import moment from "moment";
// @ts-ignore
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

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      (lang) => {
        this.loadSpecies();
        this.period = this.formatPeriod(
          this.experiment.start_date,
          this.experiment.end_date
        );
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  showEditForm() {
    this.convertDtoBeforeEditForm();
    // make a deep copy of the experiment in order to not change the current dto
    // In case a field has been updated into the form without confirmation (by sending update to the server)
    let experimentDtoCopy = JSON.parse(JSON.stringify(this.experiment));

    this.experimentForm.showEditForm(experimentDtoCopy);
  }

  convertDtoBeforeEditForm() {
    if (
      this.experiment.projects &&
      this.experiment.projects.length > 0 &&
      this.experiment.projects[0].uri //convert experiment 's project list only one time on open form
    ) {
      this.experiment.projects = this.experiment.projects.map((project) => {
        return project.uri;
      });
    }
    if (
      this.experiment.organisations &&
      this.experiment.organisations.length > 0 &&
      this.experiment.organisations[0].uri
    ) {
      this.experiment.organisations = this.experiment.organisations.map(
        (organisation) => {
          return organisation.uri;
        }
      );
    }
  }

  get infrastructuresListURIs() {
    let infraUris = [];
    for (let infra of this.infrastructuresList) {
      infra.to = {
        path: "/infrastructure/details/" + encodeURIComponent(infra.uri),
      };
      infraUris.push(infra);
    }
    return infraUris;
  }

  get facilityListUris() {
    return this.experiment.facilities.map(facility => {
      return {
        uri: facility.uri,
        value: facility.name
      }
    });
  }

  deleteExperiment(uri: string) {
    this.service
      .deleteExperiment(uri)
      .then(() => {
        this.$router.push({
          path: "/experiments",
        });
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
        .catch((error) => {
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
      this.experiment.start_date,
      this.experiment.end_date
    );
  }

  loadInfrastructures() {
    let service: OrganizationsService = this.$opensilex.getService(
      "opensilex.OrganizationsService"
    );
    this.infrastructuresList = [];

    if (
      this.experiment.organisations &&
      this.experiment.organisations.length > 0
    ) {
      this.experiment.organisations.forEach((organisation) => {
        this.infrastructuresList.push({
          uri: organisation.uri,
          value: organisation.name,
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
          this.groupsList = http.response.result.map((group) => {
            return {
              uri: group.uri,
              value: group.name,
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
      this.experiment.scientific_supervisors &&
      this.experiment.scientific_supervisors.length > 0
    ) {
      service
        .getUsersByURI(this.experiment.scientific_supervisors)
        .then((http: HttpResponse<OpenSilexResponse<UserGetDTO[]>>) => {
          this.scientificSupervisorsList = http.response.result.map((item) => {
            return {
              uri: item.email,
              url: "mailto:" + item.email,
              value: item.first_name + " " + item.last_name,
            };
          });
        })
        .catch(this.$opensilex.errorHandler);
    }
    this.technicalSupervisorsList = [];
    if (
      this.experiment.technical_supervisors &&
      this.experiment.technical_supervisors.length > 0
    ) {
      service
        .getUsersByURI(this.experiment.technical_supervisors)
        .then((http: HttpResponse<OpenSilexResponse<UserGetDTO[]>>) => {
          this.technicalSupervisorsList = http.response.result.map((item) => {
            return {
              uri: item.email,
              url: "mailto:" + item.email,
              value: item.first_name + " " + item.last_name,
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
                (species) => species == http.response.result[i].uri
              )
            ) {
              this.speciesList.push(http.response.result[i]);
            }
          }

          this.speciesList = this.speciesList.map((item) => {
            return {
              uri: item.uri,
              value: item.name,
              to: {
                path: "/germplasm/details/" + encodeURIComponent(item.uri),
              },
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
        .searchFactors(
          undefined, // name
          undefined, // description
          undefined, // category
          undefined, // experiment
          undefined, // orderBy
          0, // page
          0 // pageSize
        )
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

          this.factorsList = this.factorsList.map((item) => {
            return {
              uri: item.uri,
              value: item.name,
              to: {
                path:
                  "/" +
                  encodeURIComponent(this.uri) +
                  "/factor/details/" +
                  encodeURIComponent(item.uri),
              },
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
      this.experiment.projects.forEach((project) => {
        this.projectsList.push({
          uri: project.uri,
          value: project.name,
          to: {
            path: "/project/details/" + encodeURIComponent(project.uri),
          },
        });
      });
    }
  }

  isEnded(experiment) {
    if (experiment.end_date) {
      return moment(experiment.end_date, "YYYY-MM-DD").diff(moment()) < 0;
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

    endDate.add(1, "days");
    let years = endDate.diff(startDate, "year");
    startDate.add(years, "years");
    let months = endDate.diff(startDate, "months");
    startDate.add(months, "months");
    let days = endDate.diff(startDate, "days");

    let yearsString = "";
    let monthsString = "";
    let daysString = "";
    if (years > 0) {
      if (years == 1) {
        yearsString = years + " " + this.$t("component.common.year").toString();
      }
      if (years > 1) {
        yearsString =
          years + " " + this.$t("component.common.years").toString();
      }
    }

    if (months > 0) {
      if (months == 1) {
        monthsString =
          months + " " + this.$t("component.common.month").toString();
      }
      if (months > 1) {
        monthsString =
          months + " " + this.$t("component.common.months").toString();
      }
    }
    if (days > 0) {
      if (days == 1) {
        daysString = days + " " + this.$t("component.common.day").toString();
      }
      if (days > 1) {
        daysString = days + " " + this.$t("component.common.days").toString();
      }
    }
    result +=
      " ( " + yearsString + " " + monthsString + " " + daysString + " )";

    return result;
  }
}
</script>

<style scoped lang="scss">
</style>
