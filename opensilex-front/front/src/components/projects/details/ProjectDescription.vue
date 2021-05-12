<template>
  <div>
    <opensilex-ProjectForm
      v-if="user.hasCredential(credentials.CREDENTIAL_PROJECT_MODIFICATION_ID)"
      ref="projectForm"
      @onUpdate="loadProject()"
    ></opensilex-ProjectForm>

    <div v-if="project" class="row">
      <div class="col col-xl-5" style="min-width: 400px">
        <opensilex-Card icon="ik#ik-clipboard" :label="$t('component.common.description')">
          <template v-slot:rightHeader>
            <b-button-group>
              <opensilex-EditButton
                v-if="user.hasCredential(credentials.CREDENTIAL_PROJECT_MODIFICATION_ID)"
                @click="showEditForm"
                label="component.project.update"
              ></opensilex-EditButton>

              <opensilex-DeleteButton
                v-if="user.hasCredential(credentials.CREDENTIAL_PROJECT_DELETE_ID)"
                @click="deleteProject(project.uri)"
                label="component.project.delete"
              ></opensilex-DeleteButton>
            </b-button-group>
          </template>
          <template v-slot:body>
            <opensilex-StringView label="component.project.name" :value="project.name"></opensilex-StringView>
            <div class="static-field">
              <span
                class="field-view-title"
              >{{ $t('component.common.state') }}</span>
              <span class="static-field-line">
                <span
                  v-if="!isEnded(project)"
                  class="badge badge-pill badge-info-phis"
                  :title="$t('component.project.view.status.in-progress')"
                >
                  <i class="ik ik-activity mr-1"></i>
                  {{ $t('component.project.common.status.in-progress') }}
                </span>
                <span
                  v-else
                  class="badge badge-pill badge-light"
                  :title="$t('component.project.view.status.finished')"
                >
                  <i class="ik ik-archive"></i>
                  {{ $t('component.project.common.status.finished') }}
                </span>
              </span>
            </div>
            <opensilex-StringView label="component.common.period" :value="period"></opensilex-StringView>
            <opensilex-UriView :uri="project.uri"></opensilex-UriView>
            <opensilex-StringView label="component.project.shortname" :value="project.shortname"></opensilex-StringView>
            <opensilex-TextView
              label="component.project.financialFunding"
              :value="project.financial_funding"
            ></opensilex-TextView>
            <opensilex-UriView
              title="component.project.website"
              :value="project.website"
              :uri="project.website"
            ></opensilex-UriView>
            <opensilex-UriListView
              label="component.project.relatedProjects"
              :list="relatedProjectsList"
            ></opensilex-UriListView>
            <opensilex-TextView label="component.project.objective" :value="project.objective"></opensilex-TextView>
            <opensilex-TextView label="component.project.description" :value="project.description"></opensilex-TextView>
          </template>

          <!--   <template v-slot:footer>
            <h6>{{ $t('component.common.keywords') }}</h6>
            <span :key="index" v-for="(uri, index) in project.keywords">
              <span class="keyword badge badge-pill badge-dark">{{ uri }}</span>
            </span>
          </template>-->
        </opensilex-Card>
      </div>

      <div class="col col-xl-7">
        <opensilex-AssociatedExperimentsList
          :searchMethod="loadExperiments"
        ></opensilex-AssociatedExperimentsList>

        <opensilex-Card label="component.common.contacts" icon="ik#ik-users">
          <template v-slot:body>
            <opensilex-UriListView
              label="component.project.scientificContacts"
              :list="scientificContactsList"
            ></opensilex-UriListView>
            <opensilex-UriListView label="component.project.coordinators" :list="coordinatorsList"></opensilex-UriListView>
            <opensilex-UriListView
              label="component.project.administrativeContacts"
              :list="administrativeContactsList"
            ></opensilex-UriListView>
          </template>
        </opensilex-Card>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import moment from "moment";
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";
// @ts-ignore
import { ProjectGetDetailDTO, ProjectsService } from "opensilex-core/index";
// @ts-ignore
import { SecurityService, UserGetDTO } from "opensilex-security/index";

@Component
export default class ProjectDescription extends Vue {
  $opensilex: any;
  $route: any;
  $store: any;
  service: ProjectsService;

  get user() {
    return this.$store.state.user;
  }
  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("projectForm") readonly projectForm!: any;
  @Ref("tableRef") readonly tableRef!: any;

  project: ProjectGetDetailDTO = null;

  period: string = "";
  uri = null;
  experiments = [];
  scientificContactsList = [];
  coordinatorsList = [];
  administrativeContactsList = [];
  relatedProjectsList = [];
  fields = [
    {
      key: "name",
      label: "component.common.name"
    },
    {
      key: "description",
      label: "component.common.description"
    },
    {
      key: "start_date",
      label: "component.common.startDate",
      sortable: true
    },
    {
      key: "end_date",
      label: "component.common.endDate",
      sortable: true
    },
    {
      key: "state",
      label: "component.common.state"
    }
  ];
  created() {
    this.service = this.$opensilex.getService("opensilex.ProjectsService");
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.loadProject();
  }

  showEditForm() {
    // make a deep copy of the variable in order to not change the current dto
    // In case a field has been updated into the form without confirmation (by sending update to the server)
    let projectDtoCopy = JSON.parse(JSON.stringify(this.project));
    this.projectForm.showEditForm(projectDtoCopy);
  }

  deleteProject(uri: string) {
    this.service
      .deleteProject(uri)
      .then(() => {
        this.$router.push({
          path: "/projects"
        });
      })
      .catch(this.$opensilex.errorHandler);
  }

  loadProject() {
    this.service
      .getProject(this.uri)
      .then((http: HttpResponse<OpenSilexResponse<ProjectGetDetailDTO>>) => {
        this.project = http.response.result;
        this.period = this.formatPeriod(
          this.project.start_date,
          this.project.end_date
        );
        this.loadUsers();
        this.loadRelatedProject();
      })
      .catch(this.$opensilex.errorHandler);
  }

  loadExperiments(options) {
    return this.$opensilex
      .getService("opensilex.ExperimentsService")
      .searchExperiments(
        undefined, // name
        undefined, // year
        undefined, // isEnded,
        undefined, // species,
        undefined, // factors
        [this.uri], // projects
        undefined, // isPublic
        options.orderBy,
        options.currentPage,
        options.pageSize
      );
  }
  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        this.period = this.formatPeriod(
          this.project.start_date,
          this.project.end_date
        );
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }    

  loadRelatedProject() {
    if (this.project.related_projects) {
      this.relatedProjectsList = [];
      this.project.related_projects.forEach(relatedProject => {
        this.service
          .getProject(relatedProject)
          .then(
            (http: HttpResponse<OpenSilexResponse<ProjectGetDetailDTO>>) => {
              let projectDetail = http.response.result;
              this.relatedProjectsList.push({
                uri: projectDetail.uri,
                value: projectDetail.shortname || projectDetail.name,
                to: {
                  path:
                    "/project/details/" + encodeURIComponent(projectDetail.uri)
                }
              });
            }
          )
          .catch(this.$opensilex.errorHandler);
      });
    }
  }
  loadUsers() {
    let service: SecurityService = this.$opensilex.getService(
      "opensilex.SecurityService"
    );
    this.scientificContactsList = [];
    if (this.project.scientific_contacts.length) {
      service
        .getUsersByURI(this.project.scientific_contacts)
        .then((http: HttpResponse<OpenSilexResponse<UserGetDTO[]>>) => {
          this.scientificContactsList = http.response.result.map(item => {
            return {
              uri: item.email,
              url: "mailto:" + item.email,
              value: item.first_name + " " + item.last_name
            };
          });
        })
        .catch(this.$opensilex.errorHandler);
    }
    this.coordinatorsList = [];
    if (this.project.coordinators.length) {
      service
        .getUsersByURI(this.project.coordinators)
        .then((http: HttpResponse<OpenSilexResponse<UserGetDTO[]>>) => {
          this.coordinatorsList = http.response.result.map(item => {
            return {
              uri: item.email,
              url: "mailto:" + item.email,
              value: item.first_name + " " + item.last_name
            };
          });
        })
        .catch(this.$opensilex.errorHandler);
    }
    this.administrativeContactsList = [];
    if (this.project.administrative_contacts.length) {
      service
        .getUsersByURI(this.project.administrative_contacts)
        .then((http: HttpResponse<OpenSilexResponse<UserGetDTO[]>>) => {
          this.administrativeContactsList = http.response.result.map(item => {
            return {
              uri: item.email,
              url: "mailto:" + item.email,
              value: item.first_name + " " + item.last_name
            };
          });
        })
        .catch(this.$opensilex.errorHandler);
    }
  }

  isEnded(project) {
    if (project.end_date) {
      return moment(project.end_date, "YYYY-MM-DD").diff(moment()) < 0;
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

    result += " (" + yearsString + " " + monthsString + " " + daysString + " )";

    return result;
  }


}
</script>

<style  lang="scss">
</style>

<i18n>
en:
    ProjectDetails:
        title: Detailled project view
        advanced: Advanced informations
fr:
    ProjectDetails:
        title: Vue détaillée du projet
        advanced: Informations avancées
</i18n>
