<template>
  <ValidationObserver ref="validatorRef">
    <b-form>
      <div class="card-vertical-group">
        <div class="card">
          <div class="card-header">
            <h3 class="mr-3">
              <opensilex-Icon icon="fa#bars"/>
            </h3>
          </div>

          <div class="card-body row">
            <div class="filter-group col col-xl-6 col-sm-6 col-12">
              <opensilex-TagInputForm
                  :value.sync="filter.concernedItems"
                  label="visuForm.search.scientificObject.label"
                  :required="true"
              ></opensilex-TagInputForm>
              <!--  Waiting the new IMAGES access by provenances and the new EVENTS service-->
              <!--    <b-form-checkbox v-model="filter.showImages" switch>Images</b-form-checkbox>
              <b-form-checkbox v-model="filter.showEvents" switch>Evénements</b-form-checkbox>-->
            </div>
            <div class="filter-group col col-xl-6 col-sm-6 col-12">
              <opensilex-SelectForm
                  ref="varSelector"
                  label="visuForm.search.variable.label"
                  placeholder="visuForm.search.variable.placeholder"
                  :selected.sync="filter.variable"
                  :conversionMethod="variablesGetListDTOToSelectNode"
                  modalComponent="opensilex-VariableModalList"
                  :isModalSearch="true"
                  :required="true"
                  :multiple="true"
                  :clearable="false"
                  :maximumSelectedItems="2"
                  @onValidate="refreshComponent"
                  @onClose="refreshComponent"
                  @select="refreshComponent"
              ></opensilex-SelectForm>
            </div>
            <div class="filter-group col col-xl-6 col-sm-6 col-12">
              <!-- Default language -->
              <opensilex-InputForm
                  :value.sync="filter.startDate"
                  label="component.common.startDate"
                  type="date"
              ></opensilex-InputForm>
            </div>
            <div class="filter-group col col-xl-6 col-sm-6 col-12">
              <!-- Default language -->
              <opensilex-InputForm
                  :value.sync="filter.endDate"
                  label="component.common.endDate"
                  type="date"
              ></opensilex-InputForm>
            </div>
          </div>
        </div>
        <div class="card">
          <div class="card-header sub-header" v-if="filter.showImages">
            <h3 class="mr-3">{{ $t('visuForm.search.title-images') }}</h3>
          </div>

          <div v-if="filter.showImages" class="card-body" style=" background-color: #f6f8fb;">
            <div class="row">
              <div class="filter-group col col-xl-6 col-sm-6 col-12">
                <!-- Default language -->
                <opensilex-SelectForm
                    :selected.sync="filter.type"
                    :options="imageTypes"
                    label="visuForm.search.image-type.label"
                    placeholder="visuForm.search.image-type.placeholder"
                ></opensilex-SelectForm>
              </div>
              <div class="filter-group col col-xl-6 col-sm-6 col-12">
                <!-- Default language -->
                <opensilex-SelectForm
                    :selected.sync="filter.imagePosition"
                    :options="positions"
                    label="visuForm.search.image-position.label"
                    placeholder="visuForm.search.image-position.placeholder"
                ></opensilex-SelectForm>
              </div>
            </div>
          </div>
          <div class="card-footer text-right">
            <b-button @click="$emit('clear')" class="btn btn-light mr-3">
              <opensilex-Icon icon="ik#ik-x"/>
              {{ $t('component.common.search.clear-button') }}
            </b-button>
            <b-button @click="validate()" class="btn btn-primary">
              <opensilex-Icon icon="ik#ik-search"/>
              {{ $t('component.common.search.search-button') }}
            </b-button>
          </div>
        </div>
      </div>
    </b-form>
  </ValidationObserver>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";

import Vue from "vue";
// @ts-ignore
import {VariablesService, VariableGetDTO} from "opensilex-core/index";

class DataFilter {
  concernedItems = [];
  variable = [];
  showEvents = false;
  imageType;
  showImages = false;
  startDate;
  endDate;

  constructor() {
    this.reset();
  }

  reset() {
    this.concernedItems = [];
    this.variable = [];
    this.showEvents = false;
    this.imageType = undefined;
    this.showImages = false;
    this.startDate = undefined;
    this.endDate = undefined;
  }
}

@Component
export default class VisuForm extends Vue {
  $opensilex: any;
  variablesService: VariablesService;

  showSearchComponent: boolean = false;
  @Ref("validatorRef") readonly validatorRef!: any;
  @Ref("varSelector") readonly varSelector!: any;

  filter = new DataFilter();
  imageTypes: any = [];
  positions: any = [];
  refreshKey = 0;

  @Prop()
  selectedExperiment;

  @Prop()
  selectedScientificObjects;

  created() {
    this.variablesService = this.$opensilex.getService(
        "opensilex.VariablesService"
    );
    this.filter.concernedItems = this.selectedScientificObjects;
  }

  refreshComponent(){
    this.refreshKey += 1
  }

  scientificObjectsGetListDTOToSelectNode(dto: any) {
    if (dto) {
      return {
        id: dto.uri,
        label: dto.name
      };
    }
    return null;
  }

  variablesGetListDTOToSelectNode(dto: VariableGetDTO) {
    if (dto) {
      return {
        id: dto.uri,
        label: dto.name
      };
    }
    return null;
  }

  validate() {
    this.validatorRef.validate().then(isValid => {
      if (isValid) {
        this.$emit("search", this.filter);
      }
    });
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  visuForm:
    search:
      title: Search for data
      variable:
        label: Variable (Max=2)
        placeholder: Search for a variable
      scientificObject:
        label: Scientific objects (URI)
        placeholder: Enter scientific objects
      show-images: Show images
      show-events: Show events
      title-images: images
      image-type:
        label: Image type
        placeholder: Search for an image type
      image-position:
        label: Image position
        placeholder: Search for an image position
fr:
  visuForm:
    search:
      title: Recherche de données
      variable:
        label: Variable (Max=2)
        placeholder: Saisir une variable
      scientificObject:
        label: Objets scientifiques (URI)
        placeholder: Saisir des objets scientifiques
      show-images: Afficher les images
      show-events: Afficher les événements
      title-images: images
      image-type:
        label: Type d'images
        placeholder: Saisir le type d'images
      image-position:
        label: Position de l'image
        placeholder: Saisir une position

</i18n>
