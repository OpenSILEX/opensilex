<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-folder"
      title="component.menu.projects"
      description="component.project.search-description"
    ></opensilex-PageHeader>
    <div class="card">
      <div class="card-header row clearfix">
        <div class="col col-sm-3">
          <div class="card-options d-inline-block">
            
            <b-button @click="showCreateForm" variant="primary">
              <i class="ik ik-plus"></i>
              {{$t('component.project.add')}}
            </b-button>
          </div>
        </div>
      </div>
    </div>
    <opensilex-ProjectTable ref="projectTable" @onEdit="editProject" @onDelete="deleteProject"></opensilex-ProjectTable>
    <opensilex-ProjectForm
      ref="projectForm"
      @onCreate="callCreateProjectService"
      @onUpdate="callUpdateProjectService"
    ></opensilex-ProjectForm>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import { ProjectGetDTO, ProjectsService } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib//HttpResponse";

@Component
export default class ProjectView extends Vue {
  $opensilex: any;
  $store: any;
  service: ProjectsService;

  get user() {
    return this.$store.state.user;
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.ProjectsService");
  }

  showCreateForm() {
    let projectForm: any = this.$refs.projectForm;
    projectForm.showCreateForm();
  }

  editProject(form: ProjectGetDTO) {
    let projectForm: any = this.$refs.projectForm;
    projectForm.showEditForm(form);
  }

  callCreateProjectService(form: ProjectGetDTO, done) {
    done(
      this.service
        .createProject(form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("Project created", uri);

          let projectTable: any = this.$refs.projectTable;
          projectTable.refresh();
        })
    );
  }

  callUpdateProjectService(form: ProjectGetDTO, done) {
    done(
      this.service
        .updateProject(form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("Project updated", uri);
          let projectTable: any = this.$refs.projectTable;
          projectTable.refresh();
        })
    );
  }

  deleteProject(uri: string) {
    this.service
      .deleteProject(uri)
      .then(() => {
        let projectTable: any = this.$refs.projectTable;
        projectTable.refresh();
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
.uri-info {
  text-overflow: ellipsis;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
  max-width: 300px;
}

.btn-phis {
  background-color: #00a38d;
  border: 1px solid #00a38d;
  color: #ffffff !important;
}
.btn-phis:hover,
.btn-phis:focus,
.btn-phis.active {
  background-color: #00a38d;
  border: 1px solid #00a38d;
  color: #ffffff !important;
}
.btn-phis:focus {
  outline: 0;
  -webkit-box-shadow: none;
  box-shadow: none;
}
</style>
