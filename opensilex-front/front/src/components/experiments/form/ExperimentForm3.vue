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
          <opensilex-UserListSelector ref="scientificSupervisorsListSelector" 
            :selectedTableData="form.scientificSupervisors" 
          > </opensilex-UserListSelector>       

          <div class="error-message alert alert-danger">{{ errors[0] }}</div>
          </ValidationProvider>
        </b-form-group>

      <!-- technicalSupervisors -->
        <b-form-group  required  >
          <opensilex-FormInputLabelHelper label=component.experiment.technicalSupervisors   helpMessage="component.experiment.technicalSupervisors-help" >
          </opensilex-FormInputLabelHelper>
          <ValidationProvider :name="$t('component.experiment.technicalSupervisors')" v-slot="{ errors }">
           <opensilex-UserListSelector ref="technicalSupervisorsListSelector" 
              :selectedTableData="form.technicalSupervisors" 
          > </opensilex-UserListSelector>       
          <div class="error-message alert alert-danger">{{ errors[0] }}</div>
          </ValidationProvider>
        </b-form-group>
      </b-form>

    </ValidationObserver>
  </div>
</template>


<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";

import ExperimentForm from "./ExperimentForm.vue";

import ListSelector from "opensilex-core/index";

@Component
export default class ExperimentForm3 extends ExperimentForm {

}
</script>

<style scoped lang="scss">
</style>