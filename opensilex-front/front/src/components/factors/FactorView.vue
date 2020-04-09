<template>
  <div>
    <b-button @click="showCreateForm" variant="success">{{$t('component.factor.add')}}</b-button>
    <opensilex-FactorForm
      ref="factorForm"
      @onCreate="callCreateFactorService"
      @onUpdate="callUpdateFactorService"
    ></opensilex-FactorForm>
    <opensilex-FactorList 
    ref="factorList" 
    @onEdit="editFactor" 
    @onDelete="deleteFactor" 
    @onDetails="showFactorDetails"
    ></opensilex-FactorList>
    <opensilex-FactorDetails
     ref="factorDetails">
    </opensilex-FactorDetails>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import {
  FactorCreationDTO,
  FactorsService,
  FactorGetDTO,
  FactorDetailsGetDTO,
  FactorSearchDTO
} from "opensilex-core/index";

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

  @Ref("modalRef") readonly modalRef!: any;

  @Ref("factorForm") readonly factorForm!: any;

  @Ref("factorList") readonly factorList!: any;

  @Ref("factorDetails") readonly factorDetails!: any;

  created() {
    console.debug("Loading FactorView view...");
    this.service = this.$opensilex.getService("opensilex.FactorsService");
  }

  showCreateForm() {
    this.factorForm.showCreateForm();
  }

  callCreateFactorService(form: FactorCreationDTO, done) {
    done(
      this.service
        .createFactor(this.user.getAuthorizationHeader(), form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("factor created", uri);
          this.factorList.refresh();
          return uri;
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
          this.factorList.refresh();
        })
    );
  }
  showFactorDetails(uri: string){
    console.debug("showFactorDetails" + uri);
    this.service
      .getFactor(this.user.getAuthorizationHeader(), uri)
      .then((http: HttpResponse<OpenSilexResponse<FactorDetailsGetDTO>>) => {
        this.factorDetails.showDetails(http.response.result);
      })
      .catch(this.$opensilex.errorHandler);
  }

  editFactor(uri: string) {
    console.debug("editFactor" + uri);
    this.service
      .getFactor(this.user.getAuthorizationHeader(), uri)
      .then((http: HttpResponse<OpenSilexResponse<FactorDetailsGetDTO>>) => {
        this.factorForm.showEditForm(http.response.result);
      })
      .catch(this.$opensilex.errorHandler);
  }

  deleteFactor(uri: string) {
    console.debug("deleteFactor " + uri);
    this.service
      .deleteFactor(this.user.getAuthorizationHeader(), uri)
      .then(() => {
        this.factorList.refresh();
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>

