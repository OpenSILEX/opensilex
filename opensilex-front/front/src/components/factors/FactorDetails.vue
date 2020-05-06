<template>
  <b-modal ref="modalRef" size="lg" ok-only>
    <template v-slot:modal-ok>{{$t('component.common.cancel')}}</template>
    <template v-slot:modal-title>
      <font-awesome-icon icon="sun" />
      {{$t("component.factor.details") }} :
      <strong>{{details.uri}}</strong>
    </template>
    <b-tabs content-class="mt-3" pills card>
      <b-tab :title="$t('component.factor.tabs.details')" active>
        <div class="row">
          <div class="col-md-6">
            <label>
              <strong>{{$t('component.factor.uri')}}</strong>
            </label>
          </div>
          <div class="col-md-6">
            <p>{{this.details.uri}}</p>
          </div>
          <div class="col-md-6">
            <label>
              <strong>{{$t('component.factor.names.en')}}</strong>
            </label>
          </div>
          <div class="col-md-6">
            <p>{{this.details.names.en}}</p>
          </div>
          <div class="col-md-6">
            <label>
              <strong>{{$t('component.factor.names.fr')}}</strong>
            </label>
          </div>
          <div class="col-md-6">
            <p>{{this.details.names.fr}}</p>
          </div>
          <div class="col-md-6">
            <label>
              <strong>{{$t('component.factor.comment')}}</strong>
            </label>
          </div>
          <div class="col-md-6">
            <p>{{this.details.comment}}</p>
          </div>
        </div>
      </b-tab>
      <b-tab :title="$t('component.common.tabs.factorLevels')">
      <div>
        <b-table
          v-if="this.details.factorLevels.length != 0"
          striped
          bordered
          :items="this.details.factorLevels"
          :fields="this.factorLevelFields"
        >
          <template v-slot:head(uri)="data">{{$t(data.label)}}</template>
          <template v-slot:head(name)="data">{{$t(data.label)}}</template>
          <template v-slot:head(comment)="data">{{$t(data.label)}}</template>
        </b-table>
        <p v-else>
          <strong>{{$t('component.skos.no-external-links-provided')}}</strong>
        </p>
      </div>
      </b-tab>
      <b-tab :title="$t('component.skos.ontologies-references-label')">
        <opensilex-ExternalReferencesDetails :skosReferences="this.details"></opensilex-ExternalReferencesDetails>
      </b-tab>
      <b-tab :title="$t('component.common.tabs.document')" disabled></b-tab>
      <b-tab :title="$t('component.common.tabs.experiment')" disabled></b-tab>
    </b-tabs>
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

  details: any = {
    uri: null,
    names: { en: null, fr: null },
    comment: null,
    exactMatch: [],
    closeMatch: [],
    broader: [],
    narrower: [],
    factorLevels: []
  };

  factorLevelFields: any[] = [
    {
      key: "uri",
      label: "component.factorLevel.uri",
      sortable: false
    },
    {
      key: "name",
      label: "component.factorLevel.name",
      sortable: true,
    },
    {
      key: "comment",
      label: "component.factorLevel.comment",
      sortable: false,
    }
  ];

  showDetails(details: any) {
    this.details = details;
    this.modalRef.show();
  }

  hideForm() {
    this.modalRef.hide();
  }

  async update() {
    return new Promise((resolve, reject) => {
      this.$emit("onUpdate", this.details, result => {
        if (result instanceof Promise) {
          result.then(resolve).catch(reject);
        } else {
          resolve(result);
        }
      });
    });
  }
}
</script>

<style scoped lang="scss">
</style>
