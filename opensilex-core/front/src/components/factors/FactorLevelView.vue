<template>
  <div>
    <opensilex-core-FactorLevelList
      ref="factorLevelList"
    ></opensilex-core-FactorLevelList>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { FactorLevelCreationDTO } from "../../lib/model/factorLevelCreationDTO";
import { FactorLevelGetDTO } from "../../lib/model/factorLevelGetDTO";
import { FactorLevelsService } from "../../lib/api/factorLevels.service";

@Component
export default class factorLevelView extends Vue {
  $opensilex: any;
  $store: any;
  service: FactorLevelsService;

  get credentials() {
    return this.$store.state.credentials;
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.FactorLevelsService");
  }

  // showCreateForm() {
  //   let factorLevelForm: any = this.$refs.factorLevelForm;
  //   factorLevelForm.showCreateForm();
  // }

  // callCreatefactorLevelService(form: FactorLevelCreationDTO, done) {
  //   done(
  //     this.service
  //       .createFactorLevel(this.FactorLevel.getAuthorizationHeader(), form)
  //       .then((http: HttpResponse<OpenSilexResponse<any>>) => {
  //         let uri = http.response.result;
  //         console.debug("factorLevel created", uri);
  //         let factorLevelList: any = this.$refs.factorLevelList;
  //         factorLevelList.refresh();
  //       })
  //   );
  // }

  // callUpdatefactorLevelService(form: factorLevelUpdateDTO, done) {
  //   done(
  //     this.service
  //       .updatefactorLevel(this.factorLevel.getAuthorizationHeader(), form)
  //       .then((http: HttpResponse<OpenSilexResponse<any>>) => {
  //         let uri = http.response.result;
  //         console.debug("factorLevel updated", uri);
  //         let factorLevelList: any = this.$refs.factorLevelList;
  //         factorLevelList.refresh();
  //       })
  //   );
  // }

  // editfactorLevel(form: factorLevelGetDTO) {
  //   let factorLevelForm: any = this.$refs.factorLevelForm;
  //   factorLevelForm.showEditForm(form);
  // }

  // deletefactorLevel(uri: string) {
  //   this.service
  //     .deletefactorLevel(this.factorLevel.getAuthorizationHeader(), uri)
  //     .then(() => {
  //       let factorLevelList: any = this.$refs.factorLevelList;
  //       factorLevelList.refresh();
  //     }) 
  //     .catch(this.$opensilex.errorHandler);
  // }
}
</script>

<style scoped lang="scss">
</style>

