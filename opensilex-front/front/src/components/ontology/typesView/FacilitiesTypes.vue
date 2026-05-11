<template>
  <OntologyTypesView
    :rdfType="opensilex.Oeso.getShortURI(opensilex.Oeso.FACILITY_TYPE_URI)"
    icon="bi#bi-gear"
    :typeTitle="t('component.ontology.typeView.facility.title')"
    :typeDescription="t('component.ontology.typeView.facility.description')"
    typeURI="/facilities-types"
    :propertiesTitle="t('FacilitiesTypes.propertiesTitle')"
    :propertiesDescription="t('FacilitiesTypes.propertiesDescription')"
    propertiesURI="/facilities-types/properties"
  ></OntologyTypesView>
</template>

<script setup lang="ts">
import {inject} from "vue";
import type OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useI18n} from "vue-i18n";
import OntologyTypesView from "@/components/ontology/OntologyTypesView.vue";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const { t } = useI18n();
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  FacilitiesTypes:
    typeTitle: Facilities types
    propertiesTitle:  Facilities properties
    typeDescription: Manage and configure facilities types
    propertiesDescription: Manage and configure properties that can apply specifically to a facility
fr:
  FacilitiesTypes:
    typeTitle: Types d'installations environnementales
    propertiesTitle: Propriétés des installations environnementales
    typeDescription: "Gérer et configurer les types d'installations environnementales"
    propertiesDescription: "Gérer et configurer les propriétés qui peuvent s'appliquer spécifiquement à une installation environnementale"
</i18n>


