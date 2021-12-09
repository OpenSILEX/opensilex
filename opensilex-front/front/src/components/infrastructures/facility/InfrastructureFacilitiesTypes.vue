<template>
  <opensilex-OntologyTypesView
    :rdfType="$opensilex.Oeso.FACILITY_TYPE_URI"
    icon="ik#ik-settings"
    typeTitle="InfrastructureFacilitiesTypes.typeTitle"
    typeDescription="InfrastructureFacilitiesTypes.typeDescription"
    typeURI="/facilities-types"
    propertiesTitle="InfrastructureFacilitiesTypes.propertiesTitle"
    propertiesDescription="InfrastructureFacilitiesTypes.propertiesDescription"
    propertiesURI="/facilities-types/properties"
  ></opensilex-OntologyTypesView>
</template>

<script lang="ts">
import {Component} from "vue-property-decorator";
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
    typeDescription: Manage and configure facilities types
    propertiesDescription: Manage and configure properties that apply specifically to a facility
fr:
  InfrastructureFacilitiesTypes:
    typeTitle: Types d'installations techniques
    propertiesTitle: Propriétés des installations techniques
    typeDescription: "Gérer et configurer les types d'installations techniques"
    propertiesDescription: "Gérer et configurer les propriétés qui s'appliquent spécifiquement à une installation technique"
</i18n>

