<template>
  <opensilex-Card
      v-if="selected?.uri"
      :label="t('OntologyPropertyDetail.title')"
      noFooter
  >
    <template #body>
      <!-- URI -->
      <opensilex-UriView :uri="selected.uri"></opensilex-UriView>
      <!-- Type -->
      <opensilex-StringView :label="t('OntologyPropertyDetail.type')" :value="typeValue"></opensilex-StringView>
      <!-- Value Type -->
      <opensilex-StringView :label="t('OntologyPropertyDetail.value-type')" :value="rangeValue"></opensilex-StringView>

      <!-- Domain -->
      <opensilex-StringView :label="t('OntologyPropertyDetail.domain')" :value="selected.domain_label"></opensilex-StringView>

      <!-- Name -->
      <opensilex-StringView :label="t('component.common.name')" :value="selected.name"></opensilex-StringView>
      <!-- Comment -->
      <opensilex-StringView :label="t('component.common.comment')" :value="selected.comment"></opensilex-StringView>
      <!-- Metadata -->
      <opensilex-MetadataView
          v-if="selected.publisher && selected.publisher.uri"
          :publisher="selected.publisher"
          :publicationDate="selected.publication_date"
          :lastUpdatedDate="selected.last_updated_date"
      ></opensilex-MetadataView>
    </template>
  </opensilex-Card>
</template>

<script setup lang="ts">
import {computed, inject} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {RDFPropertyGetDTO} from "opensilex-core/model/rDFPropertyGetDTO";
import OWL from "@/ontologies/OWL";
import {useI18n} from "vue-i18n";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const {t} = useI18n();

const props = defineProps<{
  selected: RDFPropertyGetDTO
}>();

const typeValue = computed(() => {
  if (OWL.isDatatypeProperty(props.selected.rdf_type)) {
    return t("OntologyPropertyForm.dataProperty");
  } else {
    return t("OntologyPropertyForm.objectProperty");
  }
});

const rangeValue = computed(() => {
  if (OWL.isDatatypeProperty(props.selected.rdf_type)) {
    let dt = opensilex.getDatatype(props.selected.range);
    if (dt) {
      return t(dt.label_key);
    } else {
      return props.selected.range;
    }
  } else {
    return props.selected.range_label;
  }
});
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
