<template>
  <OntologyTypesView
    :rdfType="opensilex.Oeso.getShortURI(opensilex.Oeso.DEVICE_TYPE_URI)"
    icon="bi#bi-gear"
    :typeTitle="t('component.ontology.typeView.device.title')"
    :typeDescription="t('component.ontology.typeView.device.description')"
    typeURI="/device-types"
    :propertiesTitle="t('DeviceTypes.propertiesTitle')"
    :propertiesDescription="t('DeviceTypes.propertiesDescription')"
    propertiesURI="/device-types/properties"
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
  DeviceTypes:
    typeTitle: Devices types
    propertiesTitle: Devices properties
    typeDescription: Manage and configure devices types
    propertiesDescription: Manage and configure properties that can apply specifically to a device
fr:
  DeviceTypes:
    typeTitle: Types d'appareils
    propertiesTitle: Propriétés des appareils
    typeDescription: "Gérer et configurer les types d'appareil"
    propertiesDescription: "Gérer et configurer les propriétés qui peuvent s'appliquer spécifiquement à un appareil"
</i18n>


