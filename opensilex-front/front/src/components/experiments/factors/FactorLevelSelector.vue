<template>
  <div v-if="!disabled">
    <opensilex-SelectForm
      v-if="experimentURI"
      :label="label"
      :selected.sync="internalValue"
      :multiple="multiple"
      :required="required"
      :optionsLoadingMethod="loadFactorLevels"
      placeholder="FactorLevelSelector.placeholder"
    ></opensilex-SelectForm>
    <opensilex-SelectForm
      v-else
      :label="label"
      :selected.sync="internalValue"
      :multiple="multiple"
      :required="required"
      :searchMethod="searchFactorLevels"
      :conversionMethod="convertDetail"
      placeholder="FactorLevelSelector.placeholder"
    ></opensilex-SelectForm>
  </div>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  Model,
  Provide,
  PropSync,
} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class FactorLevelSelector extends Vue {
  $opensilex: any;

  @Prop()
  label;

  @Prop({
    default: false,
  })
  required;

  @Prop({
    default: false,
  })
  multiple;

  @PropSync("factorLevels")
  internalValue;

  @Prop()
  experimentURI;

  disabled = false;

  loadFactorLevels() {
    if (this.experimentURI) {
      return new Promise((resolve, reject) => {
        this.$opensilex
          .getService("opensilex.ExperimentsService")
          .getAvailableFactors(this.experimentURI)
          .then((http) => {
            let factorNodes = [];
            for (let i in http.response.result) {
              let factor = http.response.result[i];
              let factorNode = {
                id: factor.uri,
                label: factor.name,
                isDisabled: true,
                children: [],
              };
              for (let j in factor.levels) {
                let factorLevel = factor.levels[j];
                factorNode.children.push({
                  id: factorLevel.uri,
                  label: factorLevel.name,
                });
              }

              if (factorNode.children.length > 0) {
                factorNodes.push(factorNode);
              }
            }
            resolve(factorNodes);
          })
          .catch((error) => {
            if (error.status == 404) {
              this.disabled = true;
            } else {
              reject(error);
            }
          });
      });
    } else {
      return Promise.resolve([]);
    }
  }

  searchFactorLevels(name: string, page, pageSize) {
    return this.$opensilex
      .getService("opensilex.FactorsService")
      .searchFactorLevels(name, ["name=asc"], page, pageSize);
  }

  convertDetail(factor) {
    let factorNode = {
      id: factor.uri,
      label: factor.name,
      isDisabled: true,
      children: [],
    };
    for (let j in factor.levels) {
      let factorLevel = factor.levels[j];
      factorNode.children.push({
        id: factorLevel.uri,
        label: factorLevel.name,
      });
    }
    return factorNode;
  }
}
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
  FactorLevelSelector:
    label: Factor levels
    placeholder: Select a factor level

fr:
  FactorLevelSelector:
    label: Niveaux de facteur
    placeholder: Sélectionner les niveaux de facteurs
</i18n>