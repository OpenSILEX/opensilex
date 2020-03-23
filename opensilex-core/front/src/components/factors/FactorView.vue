<template>
  <div>
    <b-button @click="showCreateForm" variant="success">{{$t('component.factor.add')}}</b-button>
    <opensilex-core-FactorForm
      ref="factorForm"
      @onCreate="callCreateFactorService"
      @onUpdate="callUpdateFactorService"
    ></opensilex-core-FactorForm>
    <opensilex-core-FactorList ref="factorList" @onEdit="editFactor" @onDelete="deleteFactor"></opensilex-core-FactorList>
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
export default class FactorView extends Vue {
  $opensilex: any;
  $store: any;
  service: FactorsService;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  created() {
    console.debug("Loading form view...");
    this.service = this.$opensilex.getService("opensilex.FactorsService");
  }

  showCreateForm() {
    let factorForm: any = this.$refs.factorForm;
    factorForm.showCreateForm();
  }

  callCreateFactorService(form: FactorCreationDTO, done) {
    done(
      this.service
        .createFactor(this.user.getAuthorizationHeader(), form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("factor created", uri);
          let factorList: any = this.$refs.factorList;
          factorList.refresh();
        })
    );
  }

  callUpdateFactorService(form: FactorCreationDTO, done) {
    done(
      this.service
        .updateFactor(this.user.getAuthorizationHeader(), form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("factor updated", uri);
          let factorList: any = this.$refs.factorList;
          factorList.refresh();
        })
    );
  }

  editFactor(form: FactorGetDTO) {
    console.debug("editFactor" + form.uri);
    let factorForm: any = this.$refs.factorForm;
    factorForm.showEditForm(form);
  }

  deleteFactor(uri: string) {
    console.debug("deleteFactor " + uri);
    this.service
      .deleteFactor(this.user.getAuthorizationHeader(), uri)
      .then(() => {
        let factorList: any = this.$refs.factorList;
        factorList.refresh();
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>

