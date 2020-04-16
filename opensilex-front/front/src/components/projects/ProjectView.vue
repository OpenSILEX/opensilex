<template>
  <div>
     <div class="page-header">
        <div class="row align-items-end">
            <div class="col-lg-8">
                <div class="page-header-title">
                    <i class="ik ik-layers bg-phis-green"></i>
                    <div class="d-inline">
                        <h5>{{ $t('component.menu.projects') }}</h5>
                        <span>{{ $t('component.project.search-description') }}</span>
                    </div>
                </div>
            </div>
            <div class="col-lg-4">
                <nav class="breadcrumb-container" aria-label="breadcrumb">
                    <ol class="breadcrumb">
                        <li class="breadcrumb-item">
                            <a href="#" title="Revenir au tableau de bord"><i class="ik ik-grid mr-1"></i>{{ $t('component.menu.dashboard') }}</a>
                        </li>
                        <li class="breadcrumb-item active"><i class="ik ik-layers mr-1"></i>{{ $t('component.menu.projects') }}</li>
                    </ol>
                </nav>
            </div>
        </div>
    </div>  

    <b-button @click="showCreateForm" variant="phis">{{$t('component.project.add')}}</b-button>

    <opensilex-core-ProjectForm
      ref="projectForm"
      @onCreate="callCreateProjectService"
      @onUpdate="callUpdateProjectService"
    ></opensilex-core-ProjectForm>
<br><br>
    <opensilex-core-ProjectTable ref="projectTable" @onEdit="editProject" @onDelete="deleteProject"></opensilex-core-ProjectTable>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import {ProjectGetDTO,ProjectsService} from "opensilex-core/index";
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
        .createProject( form)
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
    this.service.deleteProject( uri)
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
