<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="fa#sun"
      title="component.menu.projects"
      description="ProjectDetails.title"
    ></opensilex-PageHeader>
    <opensilex-NavBar returnTo="/projects">
      <template v-slot:linksLeft>
        <li class="active">
          <b-button class="mb-2 mr-2" variant="outline-primary">{{$t('ProjectDetails.title')}}</b-button>
        </li>
      </template>
    </opensilex-NavBar>
    <div class="container-fluid">
      <b-row>
        <b-col>
          <opensilex-Card label="component.common.description" icon="ik#ik-clipboard">
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
            </template>
            <template v-slot:body>
              <opensilex-UriView :uri="project.uri" :url="project.uri"></opensilex-UriView>
              <opensilex-StringView label="component.project.shortname" :value="project.shortname"></opensilex-StringView>
              <opensilex-StringView label="component.project.name" :value="project.label"></opensilex-StringView>
              <opensilex-TextView label="component.common.description" :value="project.description"></opensilex-TextView>
            </template>
          </opensilex-Card>
        </b-col>

        <b-col>
          <opensilex-Card label="Documents" icon="ik#ik-clipboard">
            <template v-slot:body></template>
          </opensilex-Card>
        </b-col>
      </b-row>
      <b-row>
        <b-col>
          <opensilex-Card label="component.common.advanced-description" icon="ik#ik-clipboard">
            <template v-slot:body></template>
          </opensilex-Card>
        </b-col>

        <b-col>
          <opensilex-Card label="component.common.contacts" icon="ik#ik-users">
            <template v-slot:body>
              <opensilex-ContactListView
                label="component.common.scientificContacts"
                :contacts="scientificContactsList"
              ></opensilex-ContactListView>
              <opensilex-ContactListView
                label="component.common.coordinators"
                :contacts="coordinatorsList"
              ></opensilex-ContactListView>
              <opensilex-ContactListView
                label="component.common.administrativeContacts"
                :contacts="administrativeContactsList"
              ></opensilex-ContactListView>
            </template>
          </opensilex-Card>
        </b-col>
      </b-row>
    </div>
  </div>
</template>

<script lang="ts">
import moment from "moment";
import { Component, Prop, PropSync } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { ProjectsService } from "opensilex-core/api/projects.service";
import { ProjectGetDetailDTO } from "opensilex-core/model/projectGetDetailDTO";

import { SecurityService, UserGetDTO } from "opensilex-security/index";

@Component
export default class ProjectDetails extends Vue {
  $opensilex: any;
  $store: any;
  $route: any;
  $t: any;
  $i18n: any;
  service: ProjectsService;

  get user() {
    return this.$store.state.user;
  }

  project: ProjectGetDetailDTO = {
    startDate: ""
  };
  scientificContactsList = [];
  coordinatorsList = [];
  administrativeContactsList = [];

  created() {
    this.service = this.$opensilex.getService("opensilex.ProjectsService");
    this.loadProject(this.$route.params.uri);
  }

  loadProject(uri: string) {
    this.service
      .getProject(uri)
      .then((http: HttpResponse<OpenSilexResponse<ProjectGetDetailDTO>>) => {
        console.log("http.response.result");
        console.log(http.response.result);
        this.project = http.response.result;
        this.loadUsers();
      })
      .catch(this.$opensilex.errorHandler);
  }

  loadUsers() {
    let service: SecurityService = this.$opensilex.getService(
      "opensilex.SecurityService"
    );

    if (this.project.scientificContacts) {
      this.project.scientificContacts.forEach(scientificContact => {
        service
          .getUser(scientificContact)
          .then((http: HttpResponse<OpenSilexResponse<UserGetDTO>>) => {
            this.scientificContactsList.push(http.response.result);
          })
          .catch(this.$opensilex.errorHandler);
      });
    }

    if (this.project.coordinators) {
      this.project.coordinators.forEach(coordinator => {
        service
          .getUser(coordinator)
          .then((http: HttpResponse<OpenSilexResponse<UserGetDTO>>) => {
            this.coordinatorsList.push(http.response.result);
          })
          .catch(this.$opensilex.errorHandler);
      });
    }

    if (this.project.administrativeContacts) {
      this.project.administrativeContacts.forEach(administrativeContact => {
        service
          .getUser(administrativeContact)
          .then((http: HttpResponse<OpenSilexResponse<UserGetDTO>>) => {
            this.administrativeContactsList.push(http.response.result);
          })
          .catch(this.$opensilex.errorHandler);
      });
    }
  }

  isEnded(project) {
    if (project.endDate) {
      return moment(project.endDate, "YYYY-MM-DD").diff(moment()) < 0;
    }

    return false;
  }
}
</script>

<style scoped lang="scss">
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
