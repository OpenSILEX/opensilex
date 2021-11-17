<template>
  <opensilex-OntologyTypesView
    :rdfType="$opensilex.Oeso.FACILITY_TYPE_URI"
    icon="ik#ik-settings"
    typeTitle="InfrastructureFacilitiesTypes.typeTitle"
    typeURI="/facilities-types"
    propertiesTitle="InfrastructureFacilitiesTypes.propertiesTitle"
    propertiesURI="/facilities-types/properties"
  ></opensilex-OntologyTypesView>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class InfrastructureFacilitiesTypes extends Vue {
  $opensilex: any;
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  InfrastructureFacilitiesTypes:
    typeTitle: Facilities types
    propertiesTitle:  Facilities properties

fr:
  InfrastructureFacilitiesTypes:
    typeTitle: Types d'installations techniques
    propertiesTitle: Propriétés des installations techniques
</i18n>


