<template>
  <OntologyTypesView
    :rdfType="opensilex.Oeso.getShortURI(opensilex.Oeev.EVENT_TYPE_URI)"
    icon="bi#bi-gear"
    :typeTitle="t('component.ontology.typeView.event.title')"
    :typeDescription="t('component.ontology.typeView.event.description')"
    typeURI="/event-types"
    :propertiesTitle="t('EventTypes.propertiesTitle')"
    :propertiesDescription="t('EventTypes.propertiesDescription')"
    propertiesURI="/event-types/properties"
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
  EventTypes:
    typeTitle: Event types
    propertiesTitle: Event properties
    typeDescription: Manage and configure event types
    propertiesDescription: Manage and configure properties that can apply specifically to an event
fr:
  EventTypes:
    typeTitle: Types d'évenements
    propertiesTitle: Propriétés des évenements
    typeDescription: "Gérer et configurer les types d'évenements"
    propertiesDescription: "Gérer et configurer les propriétés qui peuvent s'appliquer spécifiquement à un évenement"
</i18n>


