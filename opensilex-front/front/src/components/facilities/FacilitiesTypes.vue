<template>
  <opensilex-OntologyTypesView
    :rdfType="$opensilex.Oeso.FACILITY_TYPE_URI"
    icon="ik#ik-settings"
    typeTitle="FacilitiesTypes.typeTitle"
    typeDescription="FacilitiesTypes.typeDescription"
    typeURI="/facilities-types"
    propertiesTitle="FacilitiesTypes.propertiesTitle"
    propertiesDescription="FacilitiesTypes.propertiesDescription"
    propertiesURI="/facilities-types/properties"
  ></opensilex-OntologyTypesView>
</template>

<script lang="ts">
import {Component} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class FacilitiesTypes extends Vue {
  $opensilex: any;
}
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

