<template>
  <b-form id="geneticResource" >
    <!-- GeneticResource -->
    <opensilex-GeneticResourceSelector
        :experiment="experiment"
        :geneticResource.sync="filter.geneticResource"
        :multiple="false"
        :label="$t('FilterMap.filter.geneticResource')"
    ></opensilex-GeneticResourceSelector>
        <!-- Factor -->
    <opensilex-FactorLevelSelector
        id="factorLevels"
        :experimentURI="experiment"
        :factorLevels.sync="filter.factorLevels"
        :multiple="false"
        :required="false"
        :label="$t('FilterMap.filter.factor-level')"
    ></opensilex-FactorLevelSelector>
    <div class="row mx-md">
      <div class="col px-md-5">
        <opensilex-InputForm
            :value.sync="color"
            type="color"
            :label="$t('FilterMap.filter.color')"
        ></opensilex-InputForm>
      </div>

      <div class="col px-md-5">
        <opensilex-FormInputLabelHelper
          v-if="$t('FilterMap.filter.styleFilter')"
          :label="$t('FilterMap.filter.styleFilter')"
          :helpMessage="''"
          :labelFor="''"
        ></opensilex-FormInputLabelHelper>
        <toggle-button
            v-model="isStrikeColor"
            :labels="{
              checked: $t('FilterMap.filter.strokeColor'),
              unchecked: $t('FilterMap.filter.fillColor'),
            }"
            :sync="true"
            :width="68"
        />
      </div>
    </div>
    <div class="col-3 ">
    </div>
  </b-form>
</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {FactorDetailsGetDTO, GeneticResourceGetSingleDTO, ScientificObjectNodeDTO} from "opensilex-core/index";
import {ScientificObjectsService} from "opensilex-core/api/scientificObjects.service";

@Component
export default class FilterMap extends Vue {
  featureOS: any[];

  scientificObjectsService:ScientificObjectsService;

  experiment: string;

  @Prop()
  editMode;

  @Prop()
  title: string;

  @Prop({
    default: () => {
      return {
        ref: null,
        tabFeatures: [],
        vlStyleStrokeColor: null,
        vlStyleFillColor: null,
        display: "false",
        titleDisplay: "unknow",
      };
    },
  })
  form;

  color: string = this.getRandomColor();
  isStrikeColor: boolean = false;

  feature: any[] = [];

  $opensilex: any;
  $store: any;

  filter = {
    geneticResource: undefined,
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
      geneticResource: undefined,
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

  created() {
    this.$opensilex.showLoader();

    this.experiment = decodeURIComponent(this.$route.params.uri);
    this.scientificObjectsService=this.$opensilex.getService("opensilex.ScientificObjectsService")
    this.recoveryScientificObjects();
  }

  searchScientificObject() {
    return new Promise((resolve, reject) => {
      this.formatFactors();

      const {geneticResource, factorLevels} = this.filter;
  
      this.feature.splice(0, this.feature.length);
  
      let pageSize = 0;
      for (const feature of this.featureOS) {
        pageSize += feature.length;
      }

      this.scientificObjectsService
          .searchScientificObjects(
              this.experiment, // experiment uri?: string,
              [], // rdfTypes?: Array<string>,
              "", // pattern?: string,
              undefined, // parentURI?: string,
              geneticResource ?  [geneticResource] : undefined,
              factorLevels, // factorLevels?: Array<string>,
              undefined, // facility?: string,
              undefined,
              undefined,
              undefined,
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
            this.getNameDisplay().then(() => {
              resolve("");
            });
          });
    })
  }

  create(form) {
    return this.searchScientificObject().then(() => { 
      let ref = this.ReferenceDefinition();
      let strokeColor = "";
      let fillColor = "";
  
      if (this.isStrikeColor) {
        strokeColor = this.color;
      } else {
        fillColor = this.color;
      }

      if (ref == null) {
        this.$opensilex.showErrorToast(this.$i18n.t("FilterMap.filter.empty-filter-error"))
        return false;
      }

      let newFilter = {
        ref: ref,
        tabFeatures: this.feature,
        vlStyleStrokeColor: strokeColor, // outline color
        vlStyleFillColor: fillColor,
        display: "true",
        titleDisplay: this.titleDisplay
      };
      return newFilter;
    })
  }

  update(form) {
    return null;
  }

  getEmptyForm() {
    return {
      ref: null,
      tabFeatures: [],
      vlStyleStrokeColor: null,
      vlStyleFillColor: null,
      display: "false",
      titleDisplay: "unknow",
    };
  }

  getNameDisplay() {
    return new Promise((resolve, reject) => {
      if (this.filter.geneticResource !== undefined) {
        // GeneticResource
        this.$opensilex
            .getService("opensilex.GeneticResourceService")
            .getGeneticResource(this.filter.geneticResource)
            .then((http: HttpResponse<OpenSilexResponse<GeneticResourceGetSingleDTO>>) => {
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
                    resolve("");
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
          resolve("");
        });
      }
    })
  }

  private ReferenceDefinition() {
    let ref = "";
    ref = this.filter.geneticResource;
    if (this.filter.factorLevels) {
      this.filter.factorLevels.forEach((item) => (ref += item));
    }

    return ref;
  }

  /**
   * format factorsLevels to match the type expected by the Scientific Objects search service -> array
   * @private
   */
  private formatFactors() {
    if (typeof this.filter.factorLevels != "object") {
      this.filter.factorLevels
          ? (this.filter.factorLevels = [this.filter.factorLevels])
          : (this.filter.factorLevels = []);
    }
  }

  private recoveryScientificObjects() {
    this.featureOS = [];
    const scientificObjectsService = "opensilex.ScientificObjectsService";

    this.$opensilex
        .getService(scientificObjectsService)
        .searchScientificObjectsWithGeometryListByUris(this.experiment)
        .then((http: HttpResponse<OpenSilexResponse<Array<ScientificObjectNodeDTO>>>) => {
              const res = http.response.result as any;
              res.forEach(element => {
                if (element.geometry != null) {
                  element.geometry.properties = {
                    uri: element.uri,
                    name: element.name,
                    type: element.rdf_type,
                    nature: "ScientificObjects",
                    display: "true"
                  };

                  let inserted = false;
                  this.featureOS.forEach(item => {
                    if (item[0].properties.type == element.rdf_type) {
                      item.push(element.geometry);
                      inserted = true;
                    }
                  });
                  if (!inserted) {
                    this.featureOS.push([element.geometry]);
                  }
                }
              });
            }
        )
        .catch(this.$opensilex.errorHandler)
        .finally(() => {
          this.$opensilex.hideLoader();
        });
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
      empty-filter-error: Filter must have at least one criterion.
      geneticResource: GeneticResource
      factor-level: Factor level
      styleFilter: Filter style
      color: Selected color
fr:
  FilterMap:
    filter:
      filter: Filtre
      delete-button: Supprimer la couche
      strokeColor: Trait
      fillColor: Remplir
      crossFilter: Filtre croisé
      empty-filter-error: Le filtre doit avoir au moins un critère.
      geneticResource: Matériel génétique
      factor-level: Niveau de facteur
      styleFilter: Style de filtre
      color: Couleur sélectionnée
</i18n>
