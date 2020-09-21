<template>
  <div>
    <opensilex-SelectForm
      ref="selectForm"
      label="FactorLevelPropertySelector.label"
      :selected.sync="internalValue"
      :multiple="property.isList"
      :required="property.isRequired"
      :optionsLoadingMethod="loadFactorLevels"
      placeholder="FactorLevelPropertySelector.placeholder"
    ></opensilex-SelectForm>
  </div>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  Model,
  Provide,
  PropSync
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class FactorLevelPropertySelector extends Vue {
  $opensilex: any;

  @Prop()
  property;

  @PropSync("value")
  internalValue;

  @Prop()
  context;

  loadFactorLevels() {
    if (this.context && this.context.experimentURI) {
      return this.$opensilex
        .getService("opensilex.ExperimentsService")
        .getAvailableFactors(this.context.experimentURI)
        .then(http => {
          let factorNodes = [];
          for (let i in http.response.result) {
            let factor = http.response.result[i];
            let factorNode = {
              id: factor.uri,
              label: factor.name,
              isDisabled: true,
              children: []
            };
            for (let j in factor.factorLevels) {
              let factorLevel = factor.factorLevels[j];
              factorNode.children.push({
                id: factorLevel.uri,
                label: factorLevel.name
              });
            }

            if (factorNode.children.length > 0) {
              factorNodes.push(factorNode);
            }
          }
          return factorNodes;
        });
    } else {
      return Promise.resolve([]);
    }
  }
}
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
  FactorLevelPropertySelector:
    label: Factor levels
    placeholder: Please select a factor level

fr:
  FactorLevelPropertySelector:
    label: Niveaux de facteur
    placeholder: Sélectionner les niveaux de facteurs
</i18n>