<template>
  <b-card v-if="dto">
    <template v-slot:header>
      <h3>{{$t("OntologyPropertyDetail.title")}}</h3>
    </template>
    <div>
      <!-- URI -->
      <opensilex-UriView :uri="dto.uri"></opensilex-UriView>
      <!-- Type -->
      <opensilex-StringView label="OntologyPropertyDetail.type" :value="typeValue"></opensilex-StringView>
      <!-- Value Type -->
      <opensilex-StringView label="OntologyPropertyDetail.value-type" :value="rangeValue"></opensilex-StringView>

      <!-- Domain -->
      <opensilex-StringView label="OntologyPropertyDetail.domain" :value="dto.domain_label"></opensilex-StringView>

      <!-- Name -->
      <opensilex-StringView label="component.common.name" :value="dto.name"></opensilex-StringView>
      <!-- Comment -->
      <opensilex-StringView label="component.common.comment" :value="dto.comment"></opensilex-StringView>
    </div>
  </b-card>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import OWL from "../../ontologies/OWL";
import {RDFPropertyGetDTO} from "opensilex-core/model/rDFPropertyGetDTO";

@Component
export default class OntologyPropertyDetail extends Vue {
  $opensilex: any;

  @PropSync("selected")
  dto: RDFPropertyGetDTO;

  get typeValue() {
    if (OWL.isDatatypeProperty(this.dto.rdf_type)) {
      return this.$t("OntologyPropertyForm.dataProperty");
    } else {
      return this.$t("OntologyPropertyForm.objectProperty");
    }
  }

  get rangeValue() {
    if (OWL.isDatatypeProperty(this.dto.rdf_type)) {
      let dt = this.$opensilex.getDatatype(this.dto.range);
      if (dt) {
        return this.$t(dt.label_key);
      } else {
        return this.dto.range;
      }
    } else {
      return this.dto.range_label;
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
    domain: Domain

fr:
  OntologyPropertyDetail:
    title: Détail de la propriété
    type: Type
    value-type: Type de valeur
    domain: Domaine

</i18n>
