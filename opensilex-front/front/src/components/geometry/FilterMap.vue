<template>
  <div class="row">
    <div class="col-8">
      <div id="germplasm" class="row">
        <div class="col-5">
          <!-- Germplasm -->
          <opensilex-GermplasmSelector
              :experiment="experiment"
              :germplasm.sync="filter.germplasm"
              :multiple="false"
              label=""
          ></opensilex-GermplasmSelector>
        </div>
        <div class="col-2">
          <opensilex-InputForm
              :value.sync="colorGermplasm"
              type="color"
          ></opensilex-InputForm>
        </div>
        <toggle-button
            v-model="isStrikeColorGermplasm"
            :labels="{checked: $t('FilterMap.filter.strokeColor'), unchecked: $t('FilterMap.filter.fillColor')}"
            :sync="true"
            :width="68"
        />
        <opensilex-CheckboxForm
            :disabled="true"
            :value.sync="isCrossFilter"
            class="col"
            title="FilterMap.filter.crossFilter"
        ></opensilex-CheckboxForm>
        <div class="col">
          <opensilex-CreateButton
              :disabled="filter.germplasm === undefined"
              label="FilterMap.filter.filter"
              @click="searchScientificObject('germplasm')"
          ></opensilex-CreateButton>
        </div>
      </div>
      <div id="factor" class="row">
        <div class="col-5">
          <!-- Factor -->
          <opensilex-FactorLevelSelector
              id="factorLevels"
              :experimentURI="experiment"
              :factorLevels.sync="filter.factorLevels"
              :multiple="true"
              :required="false"
          ></opensilex-FactorLevelSelector>
        </div>
        <div class="col-2">
          <opensilex-InputForm
              :value.sync="colorFactor"
              type="color"
          ></opensilex-InputForm>
        </div>
        <toggle-button
            v-model="isStrikeColorFactor"
            :labels="{checked: $t('FilterMap.filter.strokeColor'), unchecked: $t('FilterMap.filter.fillColor')}"
            :sync="true"
            :width="68"
        />
        <opensilex-CheckboxForm
            :disabled="true"
            :value.sync="isCrossFilter"
            class="col"
            title="FilterMap.filter.crossFilter"
        ></opensilex-CheckboxForm>
        <div class="col">
          <opensilex-CreateButton
              :disabled="filter.factorLevels.length === 0"
              label="FilterMap.filter.filter"
              @click="searchScientificObject('factor')"
          ></opensilex-CreateButton>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {GermplasmGetSingleDTO} from "opensilex-core/model/germplasmGetSingleDTO";
import {FactorDetailsGetDTO} from "opensilex-core/model/factorDetailsGetDTO";

@Component
export default class FilterMap extends Vue {
  @Prop()
  featureOS: any[];

  @Prop()
  tabLayer: any[];

  @Prop()
  experiment: string;

  colorGermplasm: string = this.getRandomColor();
  colorFactor: string = this.getRandomColor();
  isStrikeColorGermplasm: boolean = false;
  isStrikeColorFactor: boolean = false;

  isCrossFilter: string = "false";

  feature: any[] = [];

  $opensilex: any;
  $store: any;

  filter = {
    germplasm: undefined,
    factorLevels: [],
  };

  private titleDisplay = "";

  get user() {
    return this.$store.state.user;
  }

  get lang() {
    return this.$store.state.lang;
  }

  reset(source) {
    if (this.isCrossFilter === "true") {
      this.filter = {
        germplasm: undefined,
        factorLevels: [],
      };
    } else {
      if (source === "germplasm") {
        this.filter.germplasm = undefined;
        this.colorGermplasm = this.getRandomColor();
      } else {
        this.filter.factorLevels = [];
        this.colorFactor = this.getRandomColor();
      }
    }
    this.titleDisplay = "";
  }

  getRandomColor() {
    const letters = "0123456789ABCDEF";
    let color = "#";
    for (let i = 0; i < 6; i++) {
      color += letters[Math.floor(Math.random() * 16)];
    }
    return color;
  }

  searchScientificObject(source) {
    const {germplasm, factorLevels} = this.filter;
    let ref = this.ReferenceDefinition(source);

    for (let tabLayerElement of this.tabLayer) {
      if (tabLayerElement.ref == ref) return;
    }
    this.feature.splice(0, this.feature.length);
    this.$opensilex
        .getService("opensilex.ScientificObjectsService")
        .searchScientificObjects(
            this.experiment, // experiment uri?: string,
            [], // rdfTypes?: Array<string>,
            "", // pattern?: string,
            undefined, // parentURI?: string,
            germplasm ? (this.isCrossFilter ? germplasm : (source === "germplasm" ? germplasm : undefined)) : undefined,
            this.isCrossFilter ? factorLevels : (source === "factor" ? factorLevels : []), // factorLevels?: Array<string>,
            undefined, // facility?: string,
            undefined,
            undefined,
            [],
            0,
            this.featureOS.length
        )
        .then((http) => {
          http.response.result.forEach(({uri}) => {
            this.featureOS.forEach((item) => {
              if (uri === item.properties.uri) {
                this.feature.push(item);
              }
            });
          });
          this.getNameDisplay(source);
        });
  }

  addFilter(source) {
    let ref = this.ReferenceDefinition(source);
    let strokeColor = "";
    let fillColor = "";

    if (source === "germplasm") {
      if (this.isStrikeColorGermplasm) {
        strokeColor = this.colorGermplasm;
      } else {
        fillColor = this.colorGermplasm;
      }
    } else {
      if (this.isStrikeColorFactor) {
        strokeColor = this.colorFactor;
      } else {
        fillColor = this.colorFactor;
      }
    }
    this.tabLayer.push({
      ref: ref,
      tabFeatures: this.feature,
      vlStyleStrokeColor: strokeColor, // outline color
      vlStyleFillColor: fillColor,
      display: "true",
      titleDisplay: this.titleDisplay,
    });
    this.reset(source);
  }

  getNameDisplay(source) {
    if (this.isCrossFilter === "true") {
      // Germplasm
      this.$opensilex
          .getService("opensilex.GermplasmService")
          .getGermplasm(this.filter.germplasm)
          .then((http: HttpResponse<OpenSilexResponse<GermplasmGetSingleDTO>>) => {
                this.titleDisplay = http.response.result.name;
                {
                  // Factor
                  let promiseName = [];
                  for (let i = 0; i < this.filter.factorLevels.length; i++) {
                    const factorLevel = this.filter.factorLevels[i];
                    promiseName.push(
                        this.$opensilex
                            .getService("opensilex.FactorsService")
                            .getFactorLevel(factorLevel)
                            .then((http: HttpResponse<OpenSilexResponse<FactorDetailsGetDTO>>) => {
                                  return http.response.result.name;
                                }
                            )
                    );
                  }
                  Promise.all(promiseName).then((items) => {
                    items.forEach((item) => {
                      this.titleDisplay.length == 0
                          ? (this.titleDisplay = item)
                          : (this.titleDisplay += " - " + item);
                    });
                    this.addFilter(source);
                  });
                }
              }
          );
    } else if (source == "germplasm") {
      // Germplasm
      this.$opensilex
          .getService("opensilex.GermplasmService")
          .getGermplasm(this.filter.germplasm)
          .then((http: HttpResponse<OpenSilexResponse<GermplasmGetSingleDTO>>) => {
                this.titleDisplay = http.response.result.name;
                this.addFilter(source);
              }
          );
    } else {
      // Factor
      let promiseName = [];
      for (let i = 0; i < this.filter.factorLevels.length; i++) {
        const factorLevel = this.filter.factorLevels[i];
        promiseName.push(
            this.$opensilex
                .getService("opensilex.FactorsService")
                .getFactorLevel(factorLevel)
                .then((http: HttpResponse<OpenSilexResponse<FactorDetailsGetDTO>>) => {
                      return http.response.result.name;
                    }
                )
        );
      }
      Promise.all(promiseName).then((items) => {
        items.forEach((item) => {
          this.titleDisplay.length == 0
              ? (this.titleDisplay = item)
              : (this.titleDisplay += " - " + item);
        });
        this.addFilter(source);
      });
    }
  }

  private ReferenceDefinition(source) {
    let ref = "";
    if (this.isCrossFilter === "true") {
      ref = this.filter.germplasm;
      this.filter.factorLevels.forEach((item) => (ref += item));
    } else if (source == "germplasm") {
      // Germplasm
      ref = this.filter.germplasm;
    } else {
      // Factor
      this.filter.factorLevels.forEach((item) => (ref += item));
    }
    return ref;
  }
}
</script>

<style lang="scss" scoped>
div.col {
  display: inline;
}
</style>

<i18n>
en:
  FilterMap:
    filter:
      filter: Filter
      delete-button: Delete layer
      strokeColor: Stroke
      fillColor: Fill
      crossFilter: Cross filter
fr:
  FilterMap:
    filter:
      filter: Filtre
      delete-button: Supprimer la couche
      strokeColor: Trait
      fillColor: Remplir
      crossFilter: Filtre croisé
</i18n>
