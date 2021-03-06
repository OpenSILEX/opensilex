<template>
  <div>
    <div id="germplasm" class="row">
      <div class="col-4">
        <!-- Germplasm -->
        <opensilex-GermplasmSelector
            :experiment="experiment"
            :germplasm.sync="filter.germplasm"
            :multiple="false"
            label=""
        ></opensilex-GermplasmSelector>
      </div>
      <div class="col-4">
        <!-- Factor -->
        <opensilex-FactorLevelSelector
            id="factorLevels"
            :experimentURI="experiment"
            :factorLevels.sync="filter.factorLevels"
            :multiple="false"
            :required="false"
        ></opensilex-FactorLevelSelector>
      </div>
      <div class="col-1">
        <opensilex-InputForm
            :value.sync="color"
            type="color"
        ></opensilex-InputForm>
      </div>
      <toggle-button
          v-model="isStrikeColor"
          :labels="{
            checked: $t('FilterMap.filter.strokeColor'),
            unchecked: $t('FilterMap.filter.fillColor'),
          }"
          :sync="true"
          :width="68"
      />
      <div class="col">
        <opensilex-CreateButton
            :disabled=" (filter.factorLevels === undefined && filter.germplasm === undefined) || IsThereReference()"
            label="FilterMap.filter.filter"
            @click="searchScientificObject()"
        ></opensilex-CreateButton>
        <slot name="clear">
          <opensilex-Button
              :small="false"
              icon="ik#ik-x"
              label="component.common.search.clear-button"
              variant="light"
              @click="reset"
          ></opensilex-Button>
        </slot>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
// @ts-ignore
import {GermplasmGetSingleDTO} from "opensilex-core/model/germplasmGetSingleDTO";
// @ts-ignore
import {FactorDetailsGetDTO} from "opensilex-core/model/factorDetailsGetDTO";

@Component
export default class FilterMap extends Vue {
  @Prop()
  featureOS: any[];

  @Prop()
  tabLayer: any[];

  @Prop()
  experiment: string;

  color: string = this.getRandomColor();
  isStrikeColor: boolean = false;

  feature: any[] = [];

  $opensilex: any;
  $store: any;

  filter = {
    germplasm: undefined,
    factorLevels: undefined,
  };

  private titleDisplay = "";

  get user() {
    return this.$store.state.user;
  }

  get lang() {
    return this.$store.state.lang;
  }

  reset() {
    this.filter = {
      germplasm: undefined,
      factorLevels: undefined,
    };
    this.color = this.getRandomColor();
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

  searchScientificObject() {
    const {germplasm, factorLevels} = this.filter;
    if (this.IsThereReference()) return;

    this.feature.splice(0, this.feature.length);

    let pageSize = 0;
    for (const feature of this.featureOS) {
      pageSize += feature.length;
    }

    this.$opensilex
        .getService("opensilex.ScientificObjectsService")
        .searchScientificObjects(
            this.experiment, // experiment uri?: string,
            [], // rdfTypes?: Array<string>,
            "", // pattern?: string,
            undefined, // parentURI?: string,
            germplasm || undefined,
            factorLevels, // factorLevels?: Array<string>,
            undefined, // facility?: string,
            undefined,
            undefined,
            [],
            0,
            pageSize
        )
        .then((http) => {
          http.response.result.forEach(({rdf_type, uri}) => {
            this.featureOS.forEach((item) => {
              if (rdf_type === item[0].properties.type) {
                for (const itemElement of item) {
                  if (uri === itemElement.properties.uri) {
                    this.feature.push(itemElement);
                  }
                }
              }
            });
          });
          this.getNameDisplay();
        });
  }

  addFilter() {
    let ref = this.ReferenceDefinition();
    let strokeColor = "";
    let fillColor = "";

    if (this.isStrikeColor) {
      strokeColor = this.color;
    } else {
      fillColor = this.color;
    }

    this.tabLayer.push({
      ref: ref,
      tabFeatures: this.feature,
      vlStyleStrokeColor: strokeColor, // outline color
      vlStyleFillColor: fillColor,
      display: "true",
      titleDisplay: this.titleDisplay,
    });
    this.reset();
  }

  getNameDisplay() {
    if (this.filter.germplasm !== undefined) {
      // Germplasm
      this.$opensilex
          .getService("opensilex.GermplasmService")
          .getGermplasm(this.filter.germplasm)
          .then((http: HttpResponse<OpenSilexResponse<GermplasmGetSingleDTO>>) => {
                this.titleDisplay = http.response.result.name;
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
                  this.addFilter();
                });
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
        this.addFilter();
      });
    }
  }

  private ReferenceDefinition() {
    let ref = "";
    ref = this.filter.germplasm;
    this.filter.factorLevels.forEach((item) => (ref += item));

    return ref;
  }

  private IsThereReference() {
    if (typeof this.filter.factorLevels != "object") {
      this.filter.factorLevels
          ? (this.filter.factorLevels = [this.filter.factorLevels])
          : (this.filter.factorLevels = []);
    }

    let ref = this.ReferenceDefinition();

    for (const tabLayerElement of this.tabLayer) {
      if (tabLayerElement.ref == ref) return true;
    }
    return false;
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
