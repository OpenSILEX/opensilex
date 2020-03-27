<template>
  <div>
    <b-button
      @click="showCreateForm"
      variant="success"
    >{{$t('component.factorLevel.add')}}</b-button>
    <opensilex-FactorLevelForm
      ref="factorLevelForm"
      @onCreate="callCreateFactorLevelService"
      @onUpdate="callUpdateFactorLevelService"
    ></opensilex-FactorLevelForm>
    <opensilex-FactorLevelList
      ref="factorLevelList"
      @onEdit="editFactorLevel"
      @onDelete="deleteFactorLevel"
    ></opensilex-FactorLevelList>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { 
  FactorLevelCreationDTO,
  FactorLevelGetDTO,
  FactorGetDTO,
  FactorLevelsService,
  FactorsService
} 
from "opensilex-core/index";

@Component
export default class FactorLevelView extends Vue {
  $opensilex: any;
  $store: any;
  service: FactorLevelsService;
  factors: Array<FactorGetDTO> = [];


  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }


  // TODO :
  // add factors list in form selector
  // add async factors list loading
  // static credentialsGroups = [];
  // static async asyncInit($opensilex) {
  //   console.debug("Loading factors list...");
  //   let security: FactorsService = await $opensilex.loadService(
  //     "opensilex-core.FactorsService"
  //   );
  
  // }

  async created() {
    console.debug("Loading form view...");
    this.service = this.$opensilex.getService("opensilex.FactorLevelsService");

    console.debug("Loading factors list...");
    let factorsService: FactorsService = await this.$opensilex.loadService(
      "opensilex-rest.FactorsService"
    );
    let http: HttpResponse<OpenSilexResponse<
      Array<FactorGetDTO>
    >> = await factorsService.searchFactors(
      this.$opensilex.getUser().getAuthorizationHeader(),
      [],
      200,
      0
    );
    this.factors = http.response.result;
    console.debug("Factors list loaded !", this.factors);
  }

  showCreateForm() {
    console.log(this.$refs) 
    let factorLevelForm: any = this.$refs.factorLevelForm;
    factorLevelForm.showCreateForm();
  }

  callCreateFactorLevelService(form: FactorLevelCreationDTO, done) {
    done(
      this.service
        .createFactorLevel(this.user.getAuthorizationHeader(), form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("factorLevel created", uri);
          let factorLevelList: any = this.$refs.factorLevelList;
          factorLevelList.refresh();
        })
    );
  }

  callUpdateFactorLevelService(form: FactorLevelCreationDTO, done) {
    done(
      this.service
        .updateFactorLevel(this.user.getAuthorizationHeader(), form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("factorLevel updated", uri);
          let factorLevelList: any = this.$refs.factorLevelList;
          factorLevelList.refresh();
        })
    );
  }

  editFactorLevel(form: FactorLevelGetDTO) {
    console.log("edit")
    let factorLevelForm: any = this.$refs.factorLevelForm;
    factorLevelForm.showEditForm(form);
  }

  deleteFactorLevel(uri: string) {
     console.log("delete" + uri)
    this.service
      .deleteFactorLevel(this.user.getAuthorizationHeader(), uri)
      .then(() => {
        let factorLevelList: any = this.$refs.factorLevelList;
        factorLevelList.refresh();
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>

