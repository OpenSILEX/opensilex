<template>
  <div id="germplasm" class="row">
    <div class="col-lg-3">
      <!-- Germplasm -->
      <opensilex-GermplasmSelector
          :germplasm.sync="filter.germplasm"
          :multiple="false"
      ></opensilex-GermplasmSelector>
    </div>
    <div class="col-lg-1">
      <opensilex-InputForm
          :value.sync="color"
          type="color"
      ></opensilex-InputForm>
    </div>
    <div class="col-lg-1">
      <opensilex-CreateButton
          label="FilterMap.filter.germplasm"
          @click="searchScientificObject"
      ></opensilex-CreateButton>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {GermplasmGetSingleDTO} from "opensilex-core/model/germplasmGetSingleDTO";

@Component
export default class FilterMap extends Vue {
  @Prop()
  featureOS: any[];

  @Prop()
  tabLayer: any[];

  @Prop()
  experiment: string;

  color: string = this.getRandomColor();

  featureGermplasm: any[] = [];

  $opensilex: any;
  $store: any;

  filter = {
    germplasm: undefined,
    factorLevels: [],
  };

  private titleDisplay;

  get user() {
    return this.$store.state.user;
  }

  get lang() {
    return this.$store.state.lang;
  }

  reset() {
    this.filter = {
      germplasm: undefined,
      factorLevels: [],
    };
  }

  getRandomColor() {
    const letters = "0123456789ABCDEF";
    let color = "#";
    for (let i = 0; i < 6; i++) {
      color += letters[Math.floor(Math.random() * 16)];
    }
    return color;
  }

  searchScientificObject() {
    for (let tabLayerElement of this.tabLayer) {
      if (tabLayerElement.ref == this.filter.germplasm) return;
    }
    this.featureGermplasm.splice(0, this.featureGermplasm.length);
    this.getNameDisplay();
    this.$opensilex
        .getService("opensilex.ScientificObjectsService")
        .searchScientificObjects(
            this.experiment, // experiment uri?: string,
            [], // rdfTypes?: Array<string>,
            "", // pattern?: string,
            undefined, // parentURI?: string,
            this.filter.germplasm ? this.filter.germplasm : undefined,
            this.filter.factorLevels, // factorLevels?: Array<string>,
            undefined, // facility?: string,
            undefined,
            undefined
        )
        .then((http) => {
          http.response.result.forEach(({uri}) => {
            this.featureOS.forEach((item) => {
              let regExp = /expe:.+|experiments#.+/;
              if (uri.slice(uri.search(regExp) + 5) == item.properties.uri.slice(item.properties.uri.search(regExp) + 12)) {
                this.featureGermplasm.push(item);
              }
            });
          });
          this.addFilterGermplasm();
        });
  }

  addFilterGermplasm() {
    this.tabLayer.push({
      ref: this.filter.germplasm,
      tabFeatures: this.featureGermplasm,
      // vlStyleStrokeColor: "red",  // outline color
      vlStyleFillColor: this.color,
      display: "true",
      titleDisplay: this.titleDisplay,
    });
    this.color = this.getRandomColor();
  }

  getNameDisplay() {
    // if () { // Germplasm
    this.$opensilex
        .getService("opensilex.GermplasmService")
        .getGermplasm(this.filter.germplasm)
        .then((http: HttpResponse<OpenSilexResponse<GermplasmGetSingleDTO>>) =>
            this.titleDisplay = http.response.result.name
        );
    // } else { // Factor
    // }
  }
}
</script>

<style lang="scss" scoped>
</style>

<i18n>
en:
  FilterMap:
    placeholder:
      germplasm: All Germplasm
      factors: All Factors
    filter:
      label: Search for Scientific Objects
      germplasm: Filter by Germplasm
      factors: Filter by Factors
fr:
  FilterMap:
    placeholder:
      germplasm: Tous les Matériels Génétiques
      factors: Tous les Facteurs
    filter:
      label: Rechercher des Objets Scientifiques
      germplasm: Filtrer par Matériel Génétiques
      factors: Filtrer par Facteurs
</i18n>