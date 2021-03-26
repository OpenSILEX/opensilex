<template>
  <opensilex-OntologyTypesView
    :rdfType="$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI"
    icon="ik#ik-settings"
    typeTitle="ScientificObjectTypes.typeTitle"
    typeURI="/scientific-object-types"
    propertiesTitle="ScientificObjectTypes.propertiesTitle"
    propertiesURI="/scientific-object-types/properties"
  ></opensilex-OntologyTypesView>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
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

fr:
  ScientificObjectTypes:
    typeTitle: Types d'objets scientifiques
    propertiesTitle: Propriétés des objets scientifiques
</i18n>


