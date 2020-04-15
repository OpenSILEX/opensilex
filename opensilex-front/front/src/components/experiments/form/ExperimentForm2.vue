<template>
  <div ref="modalRef" @ok.prevent="validate">
    <ValidationObserver ref="validatorRef">
      <b-form>
        
      <!-- groups -->
        <b-form-group  required  >
          <opensilex-FormInputLabelHelper 
          label=component.experiment.groups 
          helpMessage="component.experiment.groups-help" >
          </opensilex-FormInputLabelHelper>

          <ValidationProvider :name="$t('component.experiment.groups')" v-slot="{ errors }">

          <opensilex-ListSelector ref="groupListSelector" 
            :selectionTableData = "groupList"
            :selectedTableData="experimentGroups" 
            selectionListTitle="Group list" selectedListTitle="Experiment groups"
          ></opensilex-ListSelector>

          <div class="error-message alert alert-danger">{{ errors[0] }}</div>
          </ValidationProvider>
        </b-form-group>

         <!-- projects -->
        <b-form-group  required  >
          <opensilex-FormInputLabelHelper 
          label=component.experiment.projects 
          helpMessage="component.experiment.projects-help" >
          </opensilex-FormInputLabelHelper>
          <ValidationProvider :name="$t('component.experiment.projects')" v-slot="{ errors }">
          <opensilex-ListSelector ref="projectListSelector" 
            :selectionTableData = "projectList"
            :selectedTableData="experimentProjects" 
            selectionListTitle="Projects list" selectedListTitle="Experiment projects"
          >
          </opensilex-ListSelector>
            <div class="error-message alert alert-danger">{{ errors[0] }}</div>
          </ValidationProvider>
        </b-form-group>

         <!-- infrastructures -->
        <b-form-group  required  >
          <opensilex-FormInputLabelHelper 
          label=component.experiment.infrastructures 
          helpMessage="component.experiment.infrastructures-help" >
          </opensilex-FormInputLabelHelper>
          <ValidationProvider :name="$t('component.experiment.infrastructures')" v-slot="{ errors }">
          
           <opensilex-ListSelector ref="infrastructureListSelector" 
            :selectionTableData = "infrastructureList"
            :selectedTableData="experimentInfrastructures" 
            selectionListTitle="Infrastructure list" selectedListTitle="Experiment infrastructures"
          >
          </opensilex-ListSelector>       
          <!-- <b-form-select id="infrastructures" v-model="form.infrastructures" :options="infrastructureList" multiple :select-size="3"> </b-form-select> -->
          <div class="error-message alert alert-danger">{{ errors[0] }}</div>
          </ValidationProvider>
        </b-form-group>

      
      </b-form>

    </ValidationObserver>
  </div>
</template>


<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";

import ExperimentForm from "./ExperimentForm.vue";

import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import ListSelector from "opensilex-core/index";

import {
  InfrastructuresService,
  InfrastructureGetDTO,
  ProjectsService,
  ProjectCreationDTO,
  ResourceTreeDTO
} from "opensilex-core/index";
import {
  SecurityService,
  GroupGetDTO,
} from "opensilex-security/index";

@Component
export default class ExperimentForm2 extends ExperimentForm {

  projectList: any = [];
  groupList: any = [];
  infrastructureList: any = [];

  experimentProjects: any = [];
  experimentGroups: any = [];
  experimentInfrastructures: any = [];

  getProjects(){
    return this.experimentProjects;
  }

  getGroups(){
    return this.experimentGroups;
  }

  getInfrastructures(){
    return this.experimentInfrastructures;
  }

  created() {
    this.loadProjects();
    this.loadGroups();
    this.loadInfrastructures();
  }

  loadProjects() {
    let service: ProjectsService = this.$opensilex.getService(
      "opensilex.ProjectsService"
    );
    service
      .searchProjects(null, 0, 1000)
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<ProjectCreationDTO>>>) => {
          for (let i = 0; i < http.response.result.length; i++) {
            let dto = http.response.result[i];
            this.projectList.push({ value: dto.uri, text: dto.label });
          }

          for(let i=0; i<this.form.projects.length; i++){
            let uri = this.form.projects[i];
            let label = this.projectList.find(item => item.value == uri).text;
            this.experimentProjects.push({ value: uri, text : label});
          }
        }
      )
      .catch(this.$opensilex.errorHandler);
  }

  loadGroups() {
    let service: SecurityService = this.$opensilex.getService(
      "opensilex.SecurityService"
    );
    service
      .searchGroups(undefined, null, 0, 100)
      .then((http: HttpResponse<OpenSilexResponse<Array<GroupGetDTO>>>) => {
        for (let i = 0; i < http.response.result.length; i++) {
          let dto = http.response.result[i];
          this.groupList.push({ value: dto.uri, text: dto.name });
        }
        for(let i=0; i<this.form.groups.length; i++){
          let uri = this.form.groups[i];
          let label = this.groupList.find(item => item.value == uri).text;
          this.experimentGroups.push({ value: uri, text : label});
        }
      })
      .catch(this.$opensilex.errorHandler);
  }

  loadInfrastructures() {
    let service: InfrastructuresService = this.$opensilex.getService(
      "opensilex.InfrastructuresService"
    );
    service
      .searchInfrastructuresTree(undefined)
      .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
        for (let i = 0; i < http.response.result.length; i++) {
          let dto = http.response.result[i];
          this.infrastructureList.push({ value: dto.uri, text: dto.name });
        }
        for(let i=0; i<this.form.infrastructures.length; i++){
          let uri = this.form.infrastructures[i];
          let label = this.infrastructureList.find(item => item.value == uri).text;
          this.experimentInfrastructures.push({ value: uri, text : label});
        }
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>