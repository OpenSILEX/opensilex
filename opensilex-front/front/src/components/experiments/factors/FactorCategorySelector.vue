<template>

  <TypeForm
    ref="formSelector"
    v-model:type="category"
    :baseType="opensilex.Oeso.FACTOR_CATEGORY_URI"
    :multiple="multiple"
    :label="label"
    placeholder="component.factors.form.placeholder.factors"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
    noResultsText="component.account.filter-search-no-result"
    :helpMessage="helpMessage"
    @handlingEnterKey="onEnter"
  ></TypeForm>
</template>

<script setup lang="ts">
import Vue, {inject, useTemplateRef} from "vue";
import TypeForm from "@/components/common/forms/TypeForm.vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {I18nN, useI18n} from "vue-i18n";
import FormSelector from "@/components/common/forms/FormSelector.vue";

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const { t } = useI18n()

const formSelector = useTemplateRef<InstanceType<typeof TypeForm>>('formSelector')

const category = defineModel('category')
interface Props {
  label?: string;
  multiple?: boolean;
  helpMessage?: string;
}

const props = withDefaults(defineProps<Props>(), {
  label: "component.factors.category",
});

const emit = defineEmits(['select', 'deselect', 'handlingEnterKey', 'clear'])

  function select(value) {
    emit("select", value);
  }

  function deselect(value) {
    emit("deselect", value);
  }

  function onEnter() {
    emit("handlingEnterKey")
  }


</script>

<style scoped lang="scss">
</style>
<i18n>

en:
  component: 
    factors:
        category : Category
        factor : Factors
        form: 
            placeholder:
                factors : Select one category
                filter-search-no-result : No category found
    factor:
      select:
        other: Other
        fieldManagement: Field management
        lightManagement: Light management
        waterManagement: Water management
        chemical: Chemical
        bioticStress: Biotic stress
        abioticStress: Abiotic stress
        soil: Soil
        nutrient: Nutrient
        atmospheric: Atmospheric
        temperature: Temperature 
fr:
  component: 
    factors:
        category : Category
        factor : Facteurs
        form: 
            placeholder:
                factors : Sélectionner une catégorie
                filter-search-no-result : Aucun facteur trouvé
    factor:
      select:
        other: Autre
        fieldManagement: Conduite culturale
        lightManagement: Gestion de la lumière
        waterManagement: Gestion de l'eau
        chemical: Chimique
        bioticStress: Stress biotique
        abioticStress: Stress abiotique
        soil: Sol
        nutrient: Nutriments
        atmospheric: Atmosphérique
        temperature: Température
</i18n>