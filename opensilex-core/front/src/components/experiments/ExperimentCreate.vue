<template>
  <div>
     <i class="ik folder-plus bg-phis-green"></i>


    <opensilex-core-ExperimentList
      ref="experimentList"
    ></opensilex-core-ExperimentList> 
  </div>
</template>

<script lang="ts">
    // <b-button
    //   @click="gotToCreateForm"
    //   variant="success"
    // >{{$t('component.experiment.add')}}</b-button>
// <opensilex-core-ExperimentList
//       ref="experimentList"
//       @onEdit="editExperiment"
//       @onDelete="deleteExperiment"
//     ></opensilex-core-ExperimentList>
// TODO experiment form
//  <opensilex-core-ExperimentForm
//       ref="experimentForm"
//       @onCreate="callCreateExperimentService"
//       @onUpdate="callUpdateExperimentService"
//     ></opensilex-core-ExperimentForm>
import { Component } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { ExperimentCreationDTO } from "../../lib/model/experimentCreationDTO";
import { ExperimentGetDTO } from "../../lib/model/experimentGetDTO";
import { ExperimentsService } from "../../lib/api/experiments.service";

@Component
export default class ExperimentCreate extends Vue {
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
    console.debug("Loading form view...");
    this.service = this.$opensilex.getService("opensilex.ExperimentsService");
  }

  gotToCreateForm() {
    this.$router.push('Home') 
  }

  // callCreateExperimentService(form: ExperimentCreationDTO, done) {
  //   done(
  //     this.service
  //       .createExperiment(this.user.getAuthorizationHeader(), form)
  //       .then((http: HttpResponse<OpenSilexResponse<any>>) => {
  //         let uri = http.response.result;
  //         console.debug("experiment created", uri);
  //         let experimentList: any = this.$refs.experimentList;
  //         experimentList.refresh();
  //       })
  //   );
  // }

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

