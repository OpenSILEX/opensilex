<template>
  <b-card v-if="selected">
    <template v-slot:header>
      <h3>{{$t("OntologyClassDetail.title")}}</h3>
    </template>
    <div>
      <!-- URI -->
      <opensilex-UriView :uri="selected.uri"></opensilex-UriView>
      <!-- Name -->
      <opensilex-StringView label="component.common.name" :value="selected.label"></opensilex-StringView>
      {{selected}}
      <div class="static-field">
        <span class="field-view-title">{{$t("Data properties")}}:</span>
      </div>

      <b-table striped hover small responsive :items="selected.dataProperties" :fields="fields">
        <template v-slot:head(name)="data">{{$t(data.label)}}</template>
        <template v-slot:head(property)="data">{{$t(data.label)}}</template>
        <template v-slot:head(isList)="data">{{$t(data.label)}}</template>
        <template v-slot:head(isRequired)="data">{{$t(data.label)}}</template>
        <template v-slot:head(inherited)="data">{{$t(data.label)}}</template>

        <!-- 
        <template v-slot:cell(typeLabel)="data">
          <span class="capitalize-first-letter">{{data.item.typeLabel}}</span>
        </template>-->
      </b-table>

      <div class="static-field">
        <span class="field-view-title">{{$t("Object properties")}}:</span>
      </div>

      <b-table striped hover small responsive :items="selected.objectProperties" :fields="fields">
        <template v-slot:head(name)="data">{{$t(data.label)}}</template>
        <template v-slot:head(property)="data">{{$t(data.label)}}</template>
        <template v-slot:head(isList)="data">{{$t(data.label)}}</template>
        <template v-slot:head(isRequired)="data">{{$t(data.label)}}</template>
        <template v-slot:head(inherited)="data">{{$t(data.label)}}</template>

        <!-- 
        <template v-slot:cell(typeLabel)="data">
          <span class="capitalize-first-letter">{{data.item.typeLabel}}</span>
        </template>-->
      </b-table>
    </div>
  </b-card>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class OntologyClassDetail extends Vue {
  $opensilex: any;

  @Prop()
  selected;

  fields = [
    {
      key: "name",
      label: "component.common.name"
    },
    {
      key: "property",
      label: "component.common.type"
    },
    {
      key: "isRequired",
      label: "OntologyClassDetail.required"
    },
    {
      key: "isList",
      label: "OntologyClassDetail.list"
    },
    {
      key: "inherited",
      label: "OntologyClassDetail.inherited"
    }
  ];
}
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
  OntologyClassDetail:
    title: Object type detail 
    required: Required
    list: List of values
    inherited: Inherited

fr:
  OntologyClassDetail:
    title: Détail du type d'objet
    required: Obligatoire
    list: Liste de valeurs
    inherited: Héritée

</i18n>

