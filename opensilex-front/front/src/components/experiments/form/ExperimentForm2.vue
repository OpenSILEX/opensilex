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
          <opensilex-GroupsListSelector ref="groupListSelector" 
            :selectedTableData="form.groups" 
          ></opensilex-GroupsListSelector>

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
          <opensilex-ProjectsListSelector ref="projectListSelector" 
            :selectedTableData="form.projects" 
          >
          </opensilex-ProjectsListSelector>
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
          
          <opensilex-InfrastructuresListSelector ref="infrastructuresListSelector" 
            :selectedTableData="form.infrastructures" 
          >
          </opensilex-InfrastructuresListSelector>
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
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>