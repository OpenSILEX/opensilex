<template>
  <b-modal ref="modalRef" size="lg" ok-only>
    <template v-slot:modal-ok>{{$t('component.common.cancel')}}</template>
    <template v-slot:modal-title>
      <font-awesome-icon icon="sun" />
      {{$t("component.germplasm.details.title") }} :
      <strong>{{details.label}}</strong>
    </template>
    <b-tabs content-class="mt-3" pills card>
      <b-tab :title="$t('component.germplasm.details.tab')" active>
        <div class="row">
          <div class="col-md-3">
            <label>
              <strong>{{$t('component.germplasm.details.uri')}}</strong>
            </label>
          </div>
          <div class="col-md-9">
            <p>{{this.details.uri}}</p>
          </div>
          <div class="col-md-3">
            <label>
              <strong>{{$t('component.germplasm.details.label')}}</strong>
            </label>
          </div>
          <div class="col-md-9">
            <p>{{this.details.label}}</p>
          </div>
          <div class="col-md-3">
            <label>
              <strong>{{$t('component.germplasm.details.rdfType')}}</strong>
            </label>
          </div>
          <div class="col-md-9">
            <p>{{this.details.typeLabel}}</p>
          </div>
          <div class="col-md-3">
            <label>
              <strong>{{$t('component.germplasm.details.species')}}</strong>
            </label>
          </div>
          <div class="col-md-3">
            <p>{{this.details.speciesLabel}}</p>            
          </div>
          <div class="col-md-6">
            <!-- <a :href="this.details.fromSpecies" class="uri-info primary">{{this.details.fromSpecies}}</a> -->
            <p>{{this.details.fromSpecies}}</p>    
          </div>
          <div class="col-md-3">
            <label>
              <strong>{{$t('component.germplasm.details.variety')}}</strong>
            </label>
          </div>
          <div class="col-md-3">
            <p>{{this.details.varietyLabel}}</p>
          </div>
          <div class="col-md-6">
            <p>{{this.details.fromVariety}}</p>
          </div>
          <div class="col-md-3">
            <label>
              <strong>{{$t('component.germplasm.details.accession')}}</strong>
            </label>
          </div>
          <div class="col-md-3">
            <p>{{this.details.accessionLabel}}</p>
          </div>
          <div class="col-md-6">
            <p>{{this.details.fromAccession}}</p>
          </div>
          <div class="col-md-3">
            <label>
              <strong>{{$t('component.germplasm.details.institute')}}</strong>
            </label>
          </div>
          <div class="col-md-9">
            <p>{{this.details.institute}}</p>
          </div>
          <div class="col-md-3">
            <label>
              <strong>{{$t('component.germplasm.details.year')}}</strong>
            </label>
          </div>
          <div class="col-md-9">
            <p>{{this.details.productionYear}}</p>
          </div>
          <div class="col-md-3">
            <label>
              <strong>{{$t('component.germplasm.details.comment')}}</strong>
            </label>
          </div>
          <div class="col-md-9">
            <p>{{this.details.comment}}</p>
          </div>
        </div>
      </b-tab>
      <b-tab :title="$t('component.common.tabs.document')" disabled></b-tab>
      <b-tab :title="$t('component.common.tabs.experiment')" disabled></b-tab>
    </b-tabs>
  </b-modal>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import { GermplasmGetDTO } from "opensilex-core/index";

@Component
export default class GermplasmDetails extends Vue {
  $opensilex: any;
  $store: any;
  $router: any;
  $t: any;
  $i18n: any;

  @Ref("modalRef") readonly modalRef!: any;

  get user() {
    return this.$store.state.user;
  }

  details: GermplasmGetDTO = {
    uri: null,
    label: null,
    rdfType: null,
    fromSpecies: null,
    fromVariety: null,
    fromAccession: null,
    institute: null,
    productionYear: null,
    comment: null
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
.uri-info {
  text-overflow: ellipsis;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
  max-width: 300px;
}
</style>
