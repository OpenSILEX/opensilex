<template>
  <div>DETAILS WIP</div>
<!--  <b-card v-if="dto">-->
<!--    <template v-slot:header>-->
<!--      <h3>{{$t("OntologyPropertyDetail.title")}}</h3>-->
<!--    </template>-->
<!--    <div>-->
<!--      &lt;!&ndash; URI &ndash;&gt;-->
<!--      <opensilex-UriView :uri="dto.uri"></opensilex-UriView>-->
<!--      &lt;!&ndash; Type &ndash;&gt;-->
<!--      <opensilex-StringView label="OntologyPropertyDetail.type" :value="typeValue"></opensilex-StringView>-->
<!--      &lt;!&ndash; Value Type &ndash;&gt;-->
<!--      <opensilex-StringView label="OntologyPropertyDetail.value-type" :value="rangeValue"></opensilex-StringView>-->

<!--      &lt;!&ndash; Domain &ndash;&gt;-->
<!--      <opensilex-StringView label="OntologyPropertyDetail.domain" :value="dto.domain_label"></opensilex-StringView>-->

<!--      &lt;!&ndash; Name &ndash;&gt;-->
<!--      <opensilex-StringView label="component.common.name" :value="dto.name"></opensilex-StringView>-->
<!--      &lt;!&ndash; Comment &ndash;&gt;-->
<!--      <opensilex-StringView label="component.common.comment" :value="dto.comment"></opensilex-StringView>-->
<!--      &lt;!&ndash; Metadata &ndash;&gt;-->
<!--      <opensilex-MetadataView-->
<!--        v-if="dto.publisher && dto.publisher.uri"-->
<!--        :publisher="dto.publisher"-->
<!--        :publicationDate="dto.publication_date"-->
<!--        :lastUpdatedDate="dto.last_updated_date" -->
<!--      ></opensilex-MetadataView>-->
<!--    </div>-->
<!--  </b-card>-->
</template>

<script setup lang="ts">
// @Component
// export default class OntologyPropertyDetail extends Vue {
//   $opensilex: any;
//
//   @PropSync("selected")
//   dto: RDFPropertyGetDTO;
//
//   get typeValue() {
//     if (OWL.isDatatypeProperty(this.dto.rdf_type)) {
//       return this.$t("OntologyPropertyForm.dataProperty");
//     } else {
//       return this.$t("OntologyPropertyForm.objectProperty");
//     }
//   }
//
//   get rangeValue() {
//     if (OWL.isDatatypeProperty(this.dto.rdf_type)) {
//       let dt = this.$opensilex.getDatatype(this.dto.range);
//       if (dt) {
//         return this.$t(dt.label_key);
//       } else {
//         return this.dto.range;
//       }
//     } else {
//       return this.dto.range_label;
//     }
//   }
// }
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
