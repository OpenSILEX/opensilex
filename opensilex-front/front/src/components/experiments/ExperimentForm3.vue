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
          <opensilex-DualList ref="scientificSupervisorsDualList" 
            :leftTableData = "scientificSupervisors"
            :rightTableData="experimentScientificSupervisors" 
            leftListTitle="User list" rightListTitle="Experiment scientific supervisors"
          > </opensilex-DualList>       

          <div class="error-message alert alert-danger">{{ errors[0] }}</div>
          </ValidationProvider>
        </b-form-group>

      <!-- technicalSupervisors -->
        <b-form-group  required  >
          <opensilex-FormInputLabelHelper label=component.experiment.technicalSupervisors   helpMessage="component.experiment.technicalSupervisors-help" >
          </opensilex-FormInputLabelHelper>
          <ValidationProvider :name="$t('component.experiment.technicalSupervisors')" v-slot="{ errors }">
           <opensilex-DualList ref="technicalSupervisorsDualList" 
            :leftTableData = "technicalSupervisors"
            :rightTableData="experimentTechnicalSupervisors" 
            leftListTitle="User list" rightListTitle="Experiment technical supervisors"
          > </opensilex-DualList>       
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
import DualList from "./DualList.vue";

import {
  SecurityService,
  GroupGetDTO,
  UserGetDTO
} from "opensilex-security/index";

@Component
export default class ExperimentForm3 extends ExperimentForm {

  technicalSupervisors: any = [];
  scientificSupervisors: any = [];

  experimentScientificSupervisors: any = {};
  experimentTechnicalSupervisors: any = {};

  async created() {
    this.loadUsers();
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
          this.scientificSupervisors.push({ value: dto.uri, text: displayName });
        }

        for(let i=0; i<this.form.technicalSupervisors.length; i++){
            let uri = this.form.technicalSupervisors[i];
            let label = this.technicalSupervisors.find(item => item.value == uri).text;
            this.experimentTechnicalSupervisors.push({ value: uri, text : label});         
        }
        for(let i=0; i<this.form.scientificSupervisors.length; i++){
            let uri = this.form.scientificSupervisors[i];
            let label = this.scientificSupervisors.find(item => item.value == uri).text;
            this.experimentScientificSupervisors.push({ value: uri, text : label});         
        }
      }).catch(this.$opensilex.errorHandler);
  }

}
</script>

<style scoped lang="scss">
</style>