<template>
  <b-card v-if="selected">
    <template v-slot:header>
      <h3>{{$t("OntologyPropertyDetail.title")}}</h3>
    </template>
    <div>
      <!-- URI -->
      <opensilex-UriView :uri="selected.uri"></opensilex-UriView>
      <!-- Type -->
      <opensilex-StringView
        label="OntologyPropertyDetail.type"
        :value="typeValue"
      ></opensilex-StringView>
      <!-- Value Type -->
      <opensilex-StringView
        label="OntologyPropertyForm.value-type"
        :value="rangeValue"
      ></opensilex-StringView>
      <!-- Name -->
      <opensilex-StringView label="component.common.name" :value="selected.label"></opensilex-StringView>
      <!-- Comment -->
      <opensilex-StringView label="component.common.comment" :value="selected.comment"></opensilex-StringView>
      {{selected}}
    </div>
  </b-card>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import OWL from "../../ontologies/OWL";

@Component
export default class OntologyPropertyDetail extends Vue {
  $opensilex: any;

  @Prop()
  selected;

  get typeValue() {
    if (OWL.isDatatypeProperty(this.selected.type)) {
      return this.$t("OntologyPropertyForm.dataProperty");
    } else {
      return this.$t("OntologyPropertyForm.objectProperty");
    }
  }

  get rangeValue() {
   if (OWL.isDatatypeProperty(this.selected.type)) {
      return this.$opensilex.getDatatype(this.selected.range);
    } else {
      return this.selected.rangeLabel;
    }
  }
}
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
  OntologyPropertyDetail:
    title: Property detail
    type: Type

fr:
  OntologyPropertyDetail:
    title: Détail de la propriété
    type: Type

</i18n>
