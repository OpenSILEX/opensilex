<template>
  <div ref="modalRef" @ok.prevent="validate">
    <ValidationObserver ref="validatorRef">
      <b-form>
          
      <!-- scientificSupervisors -->
        <b-form-group  required  >
            <opensilex-FormInputLabelHelper 
            label=component.experiment.scientificSupervisors 
            helpMessage="component.experiment.scientificSupervisors-help" >
            </opensilex-FormInputLabelHelper>
          <ValidationProvider :name="$t('component.experiment.scientificSupervisors')" v-slot="{ errors }">
          <b-form-select id="scientificSupervisors" v-model="form.scientificSupervisors" :options="scientificSupervisors" multiple :select-size="3"> </b-form-select>
          <div class="error-message alert alert-danger">{{ errors[0] }}</div>
          </ValidationProvider>
        </b-form-group>

      <!-- technicalSupervisors -->
        <b-form-group  required  >
          <opensilex-FormInputLabelHelper l
          abel=component.experiment.technicalSupervisors 
          helpMessage="component.experiment.technicalSupervisors-help" >
          </opensilex-FormInputLabelHelper>
          <ValidationProvider :name="$t('component.experiment.technicalSupervisors')" v-slot="{ errors }">
          <b-form-select id="technicalSupervisors" v-model="form.technicalSupervisors" :options="technicalSupervisors" multiple :select-size="3"> </b-form-select>
          <div class="error-message alert alert-danger">{{ errors[0] }}</div>
          </ValidationProvider>
        </b-form-group>

      <!-- groups -->
        <b-form-group  required  >
          <opensilex-FormInputLabelHelper 
          label=component.experiment.groups 
          helpMessage="component.experiment.groups-help" >
          </opensilex-FormInputLabelHelper>

          <ValidationProvider :name="$t('component.experiment.groups')" v-slot="{ errors }">
            <b-form-select
              id="groups"
              v-model="form.groups"
              :options="groupList"
              multiple
              :select-size="3"
            ></b-form-select>
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
            <b-form-select
              id="projects"
              v-model="form.projects"
              :options="projectList"
              multiple
              :select-size="3"
            ></b-form-select>
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
          <b-form-select id="infrastructures" v-model="form.infrastructures" :options="infrastructureList" multiple :select-size="3"> </b-form-select>
          <div class="error-message alert alert-danger">{{ errors[0] }}</div>
          </ValidationProvider>
        </b-form-group>
      </b-form>
    </ValidationObserver>
  </div>
</template>


<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";

import ExperimentForm from "./ExperimentForm.vue";

import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

import {
  InfrastructuresService,
  InfrastructureGetDTO,
  ProjectsService,
  ProjectGetDTO,
  ResourceTreeDTO
} from "opensilex-core/index";
import {
  SecurityService,
  GroupGetDTO,
  UserGetDTO
} from "opensilex-security/index";

@Component
export default class ExperimentForm2 extends ExperimentForm {
  projectList: any = [];
  groupList: any = [];
  infrastructureList: any = [];
  technicalSupervisors: any = [];
  scientificSupervisors: any = [];

  // form2: ExperimentCreationDTO;

  created() {
    this.loadProjects();
    this.loadGroups();
    this.loadUsers();
    this.loadInfrastructures();
  }

  loadProjects() {
    let service: ProjectsService = this.$opensilex.getService(
      "opensilex.ProjectsService"
    );
    service
      .searchProjects(null,null,null,null,null,null,null, 0, 1000)
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<ProjectGetDTO>>>) => {
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

  loadUsers() {
    let service: SecurityService = this.$opensilex.getService(
      "opensilex.SecurityService"
    );
    service
      .searchUsers(undefined, null, 0, 100)
      .then((http: HttpResponse<OpenSilexResponse<Array<UserGetDTO>>>) => {
        for (let i = 0; i < http.response.result.length; i++) {
          let dto = http.response.result[i];
          let displayName = dto.firstName + " " + dto.lastName;

          this.technicalSupervisors.push({ value: dto.uri, text: displayName });
          this.scientificSupervisors.push({
            value: dto.uri,
            text: displayName
          });
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