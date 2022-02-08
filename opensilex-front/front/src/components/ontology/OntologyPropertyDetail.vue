<template>
  <b-card v-if="selected">
    <template v-slot:header>
      <h3>{{$t("OntologyPropertyDetail.title")}}</h3>
    </template>
    <div>
      <!-- URI -->
      <opensilex-UriView :uri="selected.uri"></opensilex-UriView>
      <!-- Type -->
      <opensilex-StringView label="OntologyPropertyDetail.type" :value="typeValue"></opensilex-StringView>
      <!-- Value Type -->
      <opensilex-StringView label="OntologyPropertyDetail.value-type" :value="rangeValue"></opensilex-StringView>

      <!-- Domain -->
      <opensilex-StringView label="OntologyPropertyDetail.domain" :value="selected.domain_label"></opensilex-StringView>

      <!-- Name -->
      <opensilex-StringView label="component.common.name" :value="selected.name"></opensilex-StringView>
      <!-- Comment -->
      <opensilex-StringView label="component.common.comment" :value="selected.comment"></opensilex-StringView>
    </div>
  </b-card>
</template>

<script lang="ts">
import { Component, Prop, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";
import OWL from "../../ontologies/OWL";
import {RDFPropertyGetDTO} from "opensilex-core/model/rDFPropertyGetDTO";

@Component
export default class OntologyPropertyDetail extends Vue {
  $opensilex: any;

  @Prop()
  selected: RDFPropertyGetDTO;

  get typeValue() {
    if (OWL.isDatatypeProperty(this.selected.rdf_type)) {
      return this.$t("OntologyPropertyForm.dataProperty");
    } else {
      return this.$t("OntologyPropertyForm.objectProperty");
    }
  }

  get rangeValue() {
    if (OWL.isDatatypeProperty(this.selected.rdf_type)) {
      let dt = this.$opensilex.getDatatype(this.selected.range);
      if (dt) {
        return this.$t(dt.label_key);
      } else {
        return this.selected.range;
      }
    } else {
      return this.selected.range_label;
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
    value-type: Value type
    domain: Concerns

fr:
  OntologyPropertyDetail:
    title: Détail de la propriété
    type: Type
    value-type: Type de valeur
    domain: Concerne

</i18n>
