<template>
  <opensilex-OntologyTypesView
    :rdfType="$opensilex.Oeso.DEVICE_TYPE_URI"
    icon="ik#ik-settings"
    typeTitle="DeviceTypes.typeTitle"
    typeDescription="DeviceTypes.typeDescription"
    typeURI="/device-types"
    propertiesTitle="DeviceTypes.propertiesTitle"
    propertiesDescription="DeviceTypes.propertiesDescription"
    propertiesURI="/device-types/properties"
    :withProperties="true"
  ></opensilex-OntologyTypesView>
</template>

<script lang="ts">
import {Component} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class DeviceTypes extends Vue {
  $opensilex: any;
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  DeviceTypes:
    typeTitle: Devices types
    propertiesTitle: Devices properties
    typeDescription: Manage and configure devices types
    propertiesDescription: Manage and configure properties that apply specifically to a device
fr:
  DeviceTypes:
    typeTitle: Types de dispositif
    propertiesTitle: Propriétés des dispositifs
    typeDescription: "Gérer et configurer les types de dispositif"
    propertiesDescription: "Gérer et configurer les propriétés qui s'appliquent spécifiquement à un dispositif"
</i18n>


