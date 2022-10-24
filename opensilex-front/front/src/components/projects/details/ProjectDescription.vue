<template>
  <div>
    <opensilex-ProjectForm
      v-if="user.hasCredential(credentials.CREDENTIAL_PROJECT_MODIFICATION_ID)"
      ref="projectForm"
      class="projectDescription"
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
        <opensilex-AssociatedExperimentsList
            :searchMethod="loadExperiments"
            :nameFilter.sync="experimentName"
        ></opensilex-AssociatedExperimentsList>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";
import {ProjectGetDetailDTO, ProjectsService} from "opensilex-core/index";
import {SecurityService, UserGetDTO} from "opensilex-security/index";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {ExperimentsService} from "opensilex-core/api/experiments.service";

@Component
export default class ProjectDescription extends Vue {
  $opensilex: OpenSilexVuePlugin;
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
  experimentName = "";
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
        this.period = this.$opensilex.$dateTimeFormatter.formatPeriod(
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
      .getService<ExperimentsService>("opensilex.ExperimentsService")
      .searchExperiments(
        this.experimentName, // name
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
        this.period = this.$opensilex.$dateTimeFormatter.formatPeriod(
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
      return new Date(project.end_date).getTime() < new Date().getTime();
    }

    return false;
  }


}
</script>

<style  lang="scss">
</style>
