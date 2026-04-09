<template>
  <opensilex-OntologyTypesView
    :rdfType="opensilex.Oeso.getShortURI(opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI)"
    icon="bi#bi-gear"
    :typeTitle="t('component.ontology.typeView.scientificObject.title')"
    :typeDescription="t('component.ontology.typeView.scientificObject.description')"
    typeURI="/scientific-object-types"
    :propertiesTitle="t('ScientificObjectTypes.propertiesTitle')"
    :propertiesDescription="t('ScientificObjectTypes.propertiesDescription')"
    propertiesURI="/scientific-object-types/properties"
  ></opensilex-OntologyTypesView>
</template>

<script setup lang="ts">
import {inject} from "vue";
import type OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useI18n} from "vue-i18n";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const { t } = useI18n();
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  ScientificObjectTypes:
    propertiesTitle: Scientific objects properties
    propertiesDescription: Manage and configure properties that can apply specifically to a scientific object
fr:
  ScientificObjectTypes:
    propertiesTitle: Propriétés des objets scientifiques
    propertiesDescription: "Gérer et configurer les propriétés qui peuvent s'appliquer spécifiquement à un objet scientifique"
</i18n>


