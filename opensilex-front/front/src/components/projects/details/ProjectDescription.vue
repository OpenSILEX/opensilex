<template>
  <div>
    <div v-if="project" class="row">
      <div class="col col-xl-5" style="min-width: 400px">
        <opensilex-Card icon="ik#ik-clipboard">
          <template v-slot:rightHeader>
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
            <!--       

            <opensilex-Icon icon="fa#edit"></opensilex-Icon>-->
          </template>
          <template v-slot:body>
            <opensilex-StringView label="component.project.name" :value="project.name"></opensilex-StringView>
            <!--  <div class="static-field">
              <span
                class="field-view-title"
              >{{ $t('component.common.state') }}{{ $t('component.common.colon') }}</span>
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
            </div>-->
            <opensilex-StringView label="component.common.period" :value="period"></opensilex-StringView>
            <opensilex-UriView :uri="project.uri"></opensilex-UriView>
            <opensilex-StringView label="component.project.shortname" :value="project.shortname"></opensilex-StringView>
            <opensilex-TextView
              label="component.project.financialFunding"
              :value="project.financialFunding"
            ></opensilex-TextView>
            <opensilex-UriView
              title="component.project.website"
              :value="project.homePage"
              :uri="project.homePage"
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
        <opensilex-Card label="component.project.experiments" icon="ik#ik-layers">
          <template v-slot:body>
            <opensilex-TableAsyncView
              ref="tableRef"
              :searchMethod="loadExperiments"
              :fields="fields"
              defaultSortBy="name"
               :defaultPageSize="6"
            >
              <template v-slot:cell(label)="{data}">
                <opensilex-UriLink
                  :uri="data.item.uri"
                  :value="data.item.label"
                  :to="{path: '/experiment/details/'+ encodeURIComponent(data.item.uri)}"
                ></opensilex-UriLink>
              </template>
              <template v-slot:cell(startDate)="{data}">
                <opensilex-DateView :value="data.item.startDate"></opensilex-DateView>
              </template>
              <template v-slot:cell(endDate)="{data}">
                <opensilex-DateView :value="data.item.endDate"></opensilex-DateView>
              </template>

              <template v-slot:cell(comment)="{data}">
                <span>{{textReduce(data.item.comment)}}</span>
              </template>
              <template v-slot:cell(state)="{data}">
                <i
                  v-if="!isEnded(data.item)"
                  class="ik ik-activity badge-icon badge-info-opensilex"
                  :title="$t('component.project.common.status.in-progress')"
                ></i>
                <i
                  v-else
                  class="ik ik-archive badge-icon badge-light"
                  :title="$t('component.project.common.status.finished')"
                ></i>
              </template>
            </opensilex-TableAsyncView>
          </template>
        </opensilex-Card>

        <opensilex-Card label="component.common.contacts" icon="ik#ik-users">
          <!--    <template v-slot:rightHeader>
            <opensilex-Icon icon="fa#edit"></opensilex-Icon>
          </template>-->
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
import {
  ProjectGetDetailDTO,
  ExperimentGetListDTO,
  ExperimentsService,
  ProjectsService
} from "opensilex-core/index";
import { SecurityService, UserGetDTO } from "opensilex-security/index";

@Component
export default class ProjectDescription extends Vue {
  $opensilex: any;
  $route: any;
  service: ProjectsService;
  get user() {
    return this.$store.state.user;
  }
  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("tableRef") readonly tableRef!: any;

  project: ProjectGetDetailDTO = null;

  period: string = "";

  experiments = [];
  scientificContactsList = [];
  coordinatorsList = [];
  administrativeContactsList = [];
  relatedProjectsList = [];
  fields = [
    {
      key: "label",
      label: "component.common.name"
    },
    {
      key: "comment",
      label: "component.common.description"
    },
    {
      key: "startDate",
      label: "component.common.startDate",
      sortable: true
    },
    {
      key: "endDate",
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
    this.loadProject(this.$route.params.uri);
  }

  textReduce(text) {
    if (text.length > 60) {
      var shortname = text.substring(0, 60) + " ...";
      return text.substring(0, 60) + " ...";
    } else {
      return text;
    }
  }

  loadProject(uri: string) {
    this.service
      .getProject(uri)
      .then((http: HttpResponse<OpenSilexResponse<ProjectGetDetailDTO>>) => {
        this.project = http.response.result;
        this.period = this.formatPeriod(
          this.project.startDate,
          this.project.endDate
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
        undefined, //year
        undefined, // label
        undefined, // species,
        undefined, // factors,
        [this.$route.params.uri], // projects
        undefined, // isPublic
        undefined, // isEnded
        options.orderBy,
        options.currentPage,
        options.pageSize
      );
  }

  loadRelatedProject() {
    if (this.project.relatedProjects) {
      this.relatedProjectsList = [];
      this.project.relatedProjects.forEach(relatedProject => {
        this.service
          .getProject(relatedProject)
          .then(
            (http: HttpResponse<OpenSilexResponse<ProjectGetDetailDTO>>) => {
              let projectDetail = http.response.result;
              this.relatedProjectsList.push({
                uri: projectDetail.uri,
                value:  projectDetail.shortname || projectDetail.name,
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
    let that = this;
    if (this.project.scientificContacts.length) {
      service
        .getUsersByURI(this.project.scientificContacts)
        .then((http: HttpResponse<OpenSilexResponse<UserGetDTO[]>>) => {
          this.scientificContactsList = http.response.result.map(item => {
            return {
              uri: item.email,
              url: "mailto:" + item.email,
              value: item.firstName + " " + item.lastName
            };
          });
        })
        .catch(this.$opensilex.errorHandler);
    }

    if (this.project.coordinators.length) {
      service
        .getUsersByURI(this.project.coordinators)
        .then((http: HttpResponse<OpenSilexResponse<UserGetDTO[]>>) => {
          this.coordinatorsList = http.response.result.map(item => {
            return {
              uri: item.email,
              url: "mailto:" + item.email,
              value: item.firstName + " " + item.lastName
            };
          });
        })
        .catch(this.$opensilex.errorHandler);
    }

    if (this.project.administrativeContacts.length) {
      service
        .getUsersByURI(this.project.administrativeContacts)
        .then((http: HttpResponse<OpenSilexResponse<UserGetDTO[]>>) => {
          this.administrativeContactsList = http.response.result.map(item => {
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

  isEnded(project) {
    if (project.endDate) {
      return moment(project.endDate, "YYYY-MM-DD").diff(moment()) < 0;
    }

    return false;
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
