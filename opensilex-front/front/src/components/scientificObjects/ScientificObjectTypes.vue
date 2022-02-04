<template>
  <opensilex-OntologyTypesView
    :rdfType="$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI"
    icon="ik#ik-settings"
    typeTitle="ScientificObjectTypes.typeTitle"
    typeDescription="ScientificObjectTypes.typeDescription"
    typeURI="/scientific-object-types"
    propertiesTitle="ScientificObjectTypes.propertiesTitle"
    propertiesDescription="ScientificObjectTypes.propertiesDescription"
    propertiesURI="/scientific-object-types/properties"
  ></opensilex-OntologyTypesView>
</template>

<script lang="ts">
import {Component} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ScientificObjectTypes extends Vue {
  $opensilex: any;
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  ScientificObjectTypes:
    typeTitle: Scientific objects types
    propertiesTitle: Scientific objects properties
    typeDescription: Manage and configure scientific objects types
    propertiesDescription: Manage and configure properties that can apply specifically to a scientific object
fr:
  ScientificObjectTypes:
    typeTitle: Types d'objets scientifiques
    propertiesTitle: Propriétés des objets scientifiques
    typeDescription: "Gérer et configurer les types d'objets scientifiques"
    propertiesDescription: "Gérer et configurer les propriétés qui peuvent s'appliquer spécifiquement à un objet scientifique"
</i18n>


