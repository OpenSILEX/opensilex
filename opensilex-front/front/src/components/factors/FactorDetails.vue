<template>
  <b-modal ref="modalRef" size="lg" ok-only>
    <template v-slot:modal-ok>{{$t('component.common.cancel')}}</template>
    <template v-slot:modal-title>
      <font-awesome-icon icon="sun" />
      {{$t("component.factor.details")}}
    </template>
    <div class="row">
      <div class="col-md-6">
        <label><strong>{{$t('component.factor.uri')}}</strong></label>
      </div>
      <div class="col-md-6">
        <p>{{this.details.uri}}</p>
      </div>
      <div class="col-md-6">
        <label><strong>{{$t('component.factor.alias')}}</strong></label>
      </div>
      <div class="col-md-6">
        <p>{{this.details.alias}}</p>
      </div>
      <div class="col-md-6">
        <label><strong>{{$t('component.factor.comment')}}</strong></label>
      </div>
      <div class="col-md-6">
        <p>{{this.details.comment}}</p>
      </div>
    </div>
    <br>
    <h5>{{$t('component.skos.ontologies-references-label') }}</h5>
    <opensilex-ExternalReferencesDetails 
    :skosReferences="this.details">
    </opensilex-ExternalReferencesDetails>
  </b-modal>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import { FactorDetailsGetDTO } from "opensilex-core/index";

@Component
export default class FactorDetails extends Vue {
  $opensilex: any;
  $store: any;
  $router: any;
  $t: any;
  $i18n: any;

  @Ref("modalRef") readonly modalRef!: any;

  get user() {
    return this.$store.state.user;
  }

  details: FactorDetailsGetDTO = {
    uri: null,
    alias: null,
    comment: null,
    exactMatch: [],
    closeMatch: [],
    broader: [],
    narrower: []
    // lang: "en-US"
  };

  showDetails(details: any) {
    this.details = details;
    this.modalRef.show();
  }

  hideForm() {
    this.modalRef.hide();
  }
}
</script>

<style scoped lang="scss">
</style>
