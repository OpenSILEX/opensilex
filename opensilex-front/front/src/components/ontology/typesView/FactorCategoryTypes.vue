<template>
  <OntologyTypesView
    :rdfType="opensilex.Oeso.getShortURI(opensilex.Oeso.FACTOR_CATEGORY_URI)"
    icon="bi#bi-gear"
    :typeTitle="t('component.ontology.typeView.factorCategory.title')"
    :typeDescription="t('component.ontology.typeView.factorCategory.description')"
    typeURI="/factor-category-types"
    :withProperties="false"
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


