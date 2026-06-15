<template>
  <div class="container-fluid">
    <TypeForm
        v-model:type="selectedType"
        :baseType="opensilex.Oeso.GERMPLASM_TYPE_URI"
        :placeholder="t('form-type-placeholder')"
    ></TypeForm>
    <GermplasmTable v-if="selectedType" :germplasmType="selectedType"></GermplasmTable>
  </div>
</template>

<script setup lang="ts">
import TypeForm from "@/components/common/forms/TypeForm.vue";
import GermplasmTable from "@/components/germplasm/creation/GermplasmTable.vue";
import {inject, ref} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useI18n} from "vue-i18n";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex")
const {t} = useI18n()

const selectedType = ref<string>();

</script>

<style scoped lang="scss">

</style>

<i18n>
en:
  title: Declare Germplasm
  description: Add species, varieties, accessions ...
  backToList: Return to germplasm list
  form-type-placeholder: Please select a type
fr:
  title: Déclarer des ressources génétiques
  description: Créer des espèces, des variétiés, des accessions ...
  backToList: Revenir à la liste des germplasm
  form-type-placeholder: Veuillez sélectionner un type de germplasm
</i18n>