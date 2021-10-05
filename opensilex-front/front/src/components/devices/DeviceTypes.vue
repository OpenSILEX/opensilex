<template>
  <opensilex-OntologyTypesView
    :rdfType="$opensilex.Oeso.DEVICE_TYPE_URI"
    icon="ik#ik-settings"
    typeTitle="DeviceTypes.typeTitle"
    typeURI="/device-types"
    propertiesTitle="DeviceTypes.propertiesTitle"
    propertiesURI="/device-types/properties"
    :withProperties="true"
  ></opensilex-OntologyTypesView>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
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

fr:
  DeviceTypes:
    typeTitle: Types d'équipement
    propertiesTitle: Propriétés des équipements
</i18n>


