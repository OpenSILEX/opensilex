<template>
  <div>
    <div>
      <b-table v-if="relations.length != 0"
              striped
              hover
              small
              responsive
              sort-icon-left
              bordered 
              :items="relations" 
              :fields="fields">
        <template v-slot:head(relation)="data">{{$t(data.label)}}</template>
        <template v-slot:cell(relation)="data">{{$t(data.value)}}</template>
        <template v-slot:head(relationURI)="data">{{$t(data.label)}}</template>
        <template v-slot:cell(relationURI)="data">
          <a :href="data.value" target="_blank">{{data.value}}</a>
        </template>
      </b-table>
      <p v-else>
        <strong> {{$t('component.skos.no-external-links-provided')}}</strong> 
      </p>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import SUPPORTED_SKOS_RELATIONS from "../../../models/SkosRelations";

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
    for (let skosRelation of SUPPORTED_SKOS_RELATIONS) {
      this.updateRelations(skosRelation.dtoKey, this.skosReferences[skosRelation.dtoKey]);
    }
    return this.relationsInternal;
  }

  updateRelations(relation: string, references: string[]) {
    if(references != undefined){
      for (let index = 0; index < references.length; index++) {
        const element = references[index];
        this.addRelation(relation, element);
      }
    }
  }

  addRelation(relation: string, externalUri: string) {
    this.$set(this.relationsInternal, this.relationsInternal.length, {
      relation: relation,
      relationURI: externalUri
    });
  }
}
</script>

<style scoped lang="scss">
a {
  color : #007bff
}
</style>