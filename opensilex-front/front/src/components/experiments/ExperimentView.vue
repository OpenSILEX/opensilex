<template>
  <div> 
    <br>
    <opensilex-core-ExperimentList
      ref="experimentList"
      @onCreate="goToExperimentCreate"
      @onEdit="goToExperimentUpdate"
    ></opensilex-core-ExperimentList> 
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { ExperimentCreationDTO, ExperimentGetDTO, ExperimentsService } from "opensilex-core/index";

/**
  * Manage interaction between webservice 
  * and CRUD components
  */
@Component
export default class ExperimentView extends Vue {
  $opensilex: any;
  $store: any;
  service: ExperimentsService;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.ExperimentsService");
  }

  goToExperimentCreate(){
    this.$store.editXp = false;
    this.$router.push({ path: '/experiments/create' });
  }

  goToExperimentUpdate(experimentDto: ExperimentGetDTO){
      this.$store.xpToUpdate = experimentDto;
      this.$store.editXp = true;
      this.$router.push({  path: '/experiments/create' });
  }

  callCreateExperimentService(form: any, done) {
    done(
      this.service
        .createExperiment(this.user.getAuthorizationHeader(), form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("experiment created", uri);
          let experimentList: any = this.$refs.experimentList;
          experimentList.refresh();
        })
    );
  }

  // callUpdateExperimentService(form: ExperimentCreationDTO, done) {
  //   done(
  //     this.service
  //       .updateExperiment(this.user.getAuthorizationHeader(), form)
  //       .then((http: HttpResponse<OpenSilexResponse<any>>) => {
  //         let uri = http.response.result;
  //         console.debug("experiment updated", uri);
  //         let experimentList: any = this.$refs.experimentList;
  //         experimentList.refresh();
  //       })
  //   );
  // }

  // editExperiment(form: ExperimentGetDTO) {
  //   console.debug("editExperiment" + form.uri)
  //   let experimentForm: any = this.$refs.experimentForm;
  //   experimentForm.showEditForm(form);
  // }

  // deleteExperiment(uri: string) {
  //    console.debug("deleteExperiment " + uri)
  //   this.service
  //     .deleteExperiment(this.user.getAuthorizationHeader(), uri)
  //     .then(() => {
  //       let experimentList: any = this.$refs.experimentList;
  //       experimentList.refresh();
  //     })
  //     .catch(this.$opensilex.errorHandler);
  // }
}
</script>

<style scoped lang="scss">
</style>

