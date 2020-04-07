<template>
    <form-wizard 
      @on-loading="setLoading"
      @on-validate="handleValidation"
      @on-complete="onComplete"
      @on-error="handleErrorMessage"

      ref="experimentCreateFormWizard"
      shape="square" 
      color="#00a38d">
      <div class="loader" v-if="this.loadingWizard"></div>
      <h2 slot="title">
        <i>
          <font-awesome-icon icon="vials" />
          {{ $t('component.experiment.form-wizard.label') }}
        </i>
      </h2>

      <tab-content
        v-bind:title="$t('component.experiment.form-wizard.general-informations')"
        :before-change="checkBeforeVariablesStep">
        <opensilex-core-ExperimentForm ref="experimentForm">
        </opensilex-core-ExperimentForm>
      </tab-content>

      <tab-content
        v-bind:title="$t('component.experiment.form-wizard.users-groups-projects')">
        <opensilex-core-ExperimentForm2 ref="experimentForm2">
        </opensilex-core-ExperimentForm2>
      </tab-content>

      <!-- <tab-content
        v-bind:title="$t('component.experiment.form-wizard.sensors')"
      >Done recap ?</tab-content> -->
      <div v-if="this.errorMsg">
        <span class="error">{{ $t(getErrorMsg) }}</span>
      </div>
      
    </form-wizard>
</template>

<script lang="ts">


import { Component } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { ExperimentCreationDTO, ExperimentGetDTO, ExperimentsService } from "opensilex-core/index";
import ExperimentForm from "./ExperimentForm.vue";
import ExperimentForm2 from "./ExperimentForm2.vue";


@Component
export default class ExperimentCreate extends Vue {
  $opensilex: any;
  $store: any;
  $i18n: any;
  service: ExperimentsService;

  loadingWizard: boolean = false;
  errorMsg: string = null;
  count: number = 0;

  editMode = false;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.ExperimentsService");
    if(this.$store.editXp !== undefined){
      this.editMode = this.$store.editXp;
    }
  }

  getLoadingWizard(){
    return this.loadingWizard;
  }

  getErrorMsg(){
    return this.errorMsg;
  }

  async checkBeforeVariablesStep(){
    let experimentForm: any = this.$refs.experimentForm;
    experimentForm.validateForm();
    
    return new Promise((resolve, reject) => {
      setTimeout(() => {
       this.setLoading(true);
        experimentForm.validateForm().
        then(isValid => {
          if (isValid) {
            resolve(true);
          }else{
            this.errorMsg ='component.common.errors.form-step-errors'
            reject();
          }
        });
        this.setLoading(false);
       }, 400);
    });
  }

  setLoading (value : boolean) {
    this.loadingWizard = value;
  }


  handleValidation(isValid: boolean, tabIndex :number){
    console.log('Tab: '+tabIndex+ ' valid: '+isValid)
  }

  handleErrorMessage(errorMsg : string){
    this.errorMsg = errorMsg;
  }

  fillForm(formPart1 : ExperimentForm, formPart2: ExperimentForm2): ExperimentCreationDTO {

    let dto: ExperimentCreationDTO = formPart1.getForm();

    let keywordsForm: string = formPart1.getKeywords();
    if(keywordsForm !== undefined && keywordsForm !== null){
      dto.keywords = keywordsForm.split(" ");
    }

    let dto2: ExperimentCreationDTO = formPart2.getForm();
    dto.groups = dto2.groups;
    dto.projects = dto2.projects;
    dto.scientificSupervisors = dto2.scientificSupervisors;
    dto.technicalSupervisors = dto2.technicalSupervisors;
    dto.infrastructures = dto2.infrastructures;
    return dto;
  }

  onComplete(){

    let experimentForm: any = this.$refs.experimentForm;
    let experimentForm2: any = this.$refs.experimentForm2;
    let dto: ExperimentCreationDTO = this.fillForm(experimentForm,experimentForm2);
    
    console.log("complete "+this.editMode);
    if(this.editMode){
      this.callUpdateExperimentService(dto);
    }else{
      this.callCreateExperimentService(dto);
    }
  }

  callCreateExperimentService(form: ExperimentCreationDTO) {

    this.service
      .createExperiment(this.user.getAuthorizationHeader(), form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("experiment created", uri);
        this.$router.push({ path: '/experiments' });
    }).catch(this.$opensilex.errorHandler);
  }

  callUpdateExperimentService(form: ExperimentCreationDTO) {
    this.service
      .updateExperiment(this.user.getAuthorizationHeader(), form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("experiment updated", uri);
        this.$router.push({ path: '/experiments' });
        // let experimentList: any = this.$refs.experimentList;
        // experimentList.refresh();

      }).catch(this.$opensilex.errorHandler);
  }

}
</script>

<style scoped lang="scss">
span.error{
  color:#e74c3c;
  font-size:20px;
  display:flex;
  justify-content:center;
}
/* This is a css loader. It's not related to vue-form-wizard */
.loader,
.loader:after {
  border-radius: 50%;
  width: 10em;
  height: 10em;
}
.loader {
  margin: 60px auto;
  font-size: 10px;
  position: relative;
  text-indent: -9999em;
  border-top: 1.1em solid rgba(255, 255, 255, 0.2);
  border-right: 1.1em solid rgba(255, 255, 255, 0.2);
  border-bottom: 1.1em solid rgba(255, 255, 255, 0.2);
  border-left: 1.1em solid #00a38d;
  -webkit-transform: translateZ(0);
  -ms-transform: translateZ(0);
  transform: translateZ(0);
  -webkit-animation: load8 1.1s infinite linear;
  animation: load8 1.1s infinite linear;
}
@-webkit-keyframes load8 {
  0% {
    -webkit-transform: rotate(0deg);
    transform: rotate(0deg);
  }
  100% {
    -webkit-transform: rotate(360deg);
    transform: rotate(360deg);
  }
}
@keyframes load8 {
  0% {
    -webkit-transform: rotate(0deg);
    transform: rotate(0deg);
  }
  100% {
    -webkit-transform: rotate(360deg);
    transform: rotate(360deg);
  }
}
</style>

