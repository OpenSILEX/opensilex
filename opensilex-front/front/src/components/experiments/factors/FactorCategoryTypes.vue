<template>
  <opensilex-OntologyTypesView
    :rdfType="$opensilex.Oeso.FACTOR_CATEGORY_URI"
    icon="ik#ik-settings"
    typeTitle="FactorCategoryTypes.typeTitle"
    typeDescription="FactorCategoryTypes.typeDescription"
    typeURI="/factor-category-types"
    propertiesTitle="FactorCategoryTypes.propertiesTitle"
    propertiesDescription="FactorCategoryTypes.propertiesDescription"
    propertiesURI="/factor-category-types/properties"
    :withProperties="false"
  ></opensilex-OntologyTypesView>
</template>

<script lang="ts">
import {Component} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class FactorCategoryTypes extends Vue {
  $opensilex: any;
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  FactorCategoryTypes:
    typeTitle: Factor category
    // propertiesTitle: Factor category properties
    typeDescription: Manage and configure factor categories
fr:
  FactorCategoryTypes:
    typeTitle: Catégories de facteurs
    // propertiesTitle: Propriétés des dispositifs
    typeDescription: "Gérer et configurer les catégories de facteurs"
</i18n>


