<template>
  
  <opensilex-TypeForm
    ref="selectForm"
    :type.sync="categoryString"
    :baseType="$opensilex.Oeso.FACTOR_CATEGORY_URI"
    :multiple="multiple" 
    :label="label"
    placeholder="component.factors.form.placeholder.factors"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
    noResultsText="component.account.filter-search-no-result"
    :helpMessage="helpMessage"
    @handlingEnterKey="onEnter"
  ></opensilex-TypeForm>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
import SelectForm from "../../common/forms/SelectForm.vue";

@Component
export default class FactorCategorySelector extends Vue {
  $opensilex: any;
  $i18n: any;

  @Ref("selectForm") readonly selectForm!: SelectForm;

  @PropSync("category")
  categoryString;

  @Prop({
    default: "component.factors.category",
  })
  label;

  @Prop()
  multiple;

  @Prop()
  helpMessage: string;


  select(value) {
    this.$emit("select", value);
  }

  deselect(value) {
    this.$emit("deselect", value);
  }

  onEnter() {
    this.$emit("handlingEnterKey")
  }

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