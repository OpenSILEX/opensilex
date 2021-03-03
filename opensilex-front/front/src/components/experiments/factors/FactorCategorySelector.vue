<template>
  <opensilex-SelectForm
    ref="selectForm"
    :label="label"
    :selected.sync="categoryString"
    :multiple="multiple"
    :options="categories"
    placeholder="component.factors.form.placeholder.factors"
    noResultsText="component.user.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
    :helpMessage="helpMessage"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {
  OpenSilexResponse,
} from "opensilex-security/HttpResponse";
import { FactorsService, FactorCategoryGetDTO } from "opensilex-core/index";

@Component
export default class FactorCategorySelector extends Vue {
  $opensilex: any;
  $i18n: any;
  service: FactorsService;

  @Ref("selectForm") readonly selectForm!: any;

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

  categories: any[] = [];

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      () => {
        this.searchCategories();
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.FactorsService");
    this.searchCategories();
  }

  searchCategories() {
    this.service
      .searchCategories(undefined, ["name=asc"], undefined, undefined)
      .then(
        (
          http: HttpResponse<OpenSilexResponse<Array<FactorCategoryGetDTO>>>
        ) => {
          if (http && http.response) {
            this.categories = [];
            http.response.result.forEach((categoryDto) => {
              this.categories.push({
                label: categoryDto.name,
                id: categoryDto.uri,
              });
            });
          }
        }
      )
      .catch(this.$opensilex.errorHandler);
  }

  select(value) {
    this.$emit("select", value);
  }

  deselect(value) {
    this.$emit("deselect", value);
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
                factors : Selectionner une catégorie
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