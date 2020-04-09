<template>
  <div>
    <div>
      <b-table striped hover :items="relations" :fields="fields">
        <template v-slot:head(relation)="data">{{$t(data.label)}}</template>
        <template v-slot:head(uri)="data">{{$t(data.label)}}</template>
        <template v-slot:cell(uri)="data">{{$t(data.value)}}</template>
      </b-table>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Multiselect from "vue-multiselect";
import Vue from "vue";

@Component
export default class ExternalReferencesDetails extends Vue {
  $opensilex: any;
  $store: any;

  @Prop()
  skosReferences: any;

  relationsInternal: any[] = [];

  fields = [
    {
      key: "relation",
      label: "component.skos.relation",
      sortable: true
    },
    {
      key: "relationURI",
      label: "component.skos.uri",
      sortable: false
    }
  ];

  get relations() {
    this.relationsInternal = [];
    this.updateRelations("narrower", this.skosReferences.narrower);
    this.updateRelations("broader", this.skosReferences.broader);
    this.updateRelations("closeMatch", this.skosReferences.closeMatch);
    this.updateRelations("exactMatch", this.skosReferences.exactMatch);
    console.debug(this.relationsInternal);
    return this.relationsInternal;
  }

  updateRelations(relation: string, references: string[]) {
    for (let index = 0; index < references.length; index++) {
      const element = references[index];
      this.addRelation(relation, element);
    }
  }

  addRelation(currentRelation: string, currentExternalUri: string) {
    this.$set(this.relationsInternal, this.relationsInternal.length, {
      relation: currentRelation,
      relationURI: currentExternalUri
    });
  }
}
</script>

<style scoped lang="scss">
</style>