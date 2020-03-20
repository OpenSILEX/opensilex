<template>
  <div>
    <opensilex-core-FactorList
      ref="factorList"
    ></opensilex-core-FactorList>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { FactorCreationDTO } from "../../lib/model/factorCreationDTO";
import { FactorGetDTO } from "../../lib/model/factorGetDTO";
import { FactorsService } from "../../lib/api/factors.service";

@Component
export default class factorView extends Vue {
  $opensilex: any;
  $store: any;
  service: FactorsService;

  get credentials() {
    return this.$store.state.credentials;
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.FactorsService");
    console.log(this.service);
  }

  // showCreateForm() {
  //   let factorForm: any = this.$refs.factorForm;
  //   factorForm.showCreateForm();
  // }

  // callCreatefactorService(form: FactorCreationDTO, done) {
  //   done(
  //     this.service
  //       .createFactor(this.Factor.getAuthorizationHeader(), form)
  //       .then((http: HttpResponse<OpenSilexResponse<any>>) => {
  //         let uri = http.response.result;
  //         console.debug("factor created", uri);
  //         let factorList: any = this.$refs.factorList;
  //         factorList.refresh();
  //       })
  //   );
  // }

  // callUpdatefactorService(form: factorUpdateDTO, done) {
  //   done(
  //     this.service
  //       .updatefactor(this.factor.getAuthorizationHeader(), form)
  //       .then((http: HttpResponse<OpenSilexResponse<any>>) => {
  //         let uri = http.response.result;
  //         console.debug("factor updated", uri);
  //         let factorList: any = this.$refs.factorList;
  //         factorList.refresh();
  //       })
  //   );
  // }

  // editfactor(form: factorGetDTO) {
  //   let factorForm: any = this.$refs.factorForm;
  //   factorForm.showEditForm(form);
  // }

  // deletefactor(uri: string) {
  //   this.service
  //     .deletefactor(this.factor.getAuthorizationHeader(), uri)
  //     .then(() => {
  //       let factorList: any = this.$refs.factorList;
  //       factorList.refresh();
  //     })
  //     .catch(this.$opensilex.errorHandler);
  // }
}
</script>

<style scoped lang="scss">
</style>

