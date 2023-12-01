<template>
  <ValidationObserver ref="validatorRef">
    <opensilex-Tutorial
        ref="tutorial"
        :steps="tutorialSteps"
        @onFinish="onTutorialFinishOrSkip"
        @onSkip="onTutorialFinishOrSkip"
    >
    </opensilex-Tutorial>
    <b-row>
      <b-col sm="12" lg="8">

        <!-- Title -->
        <b-form-group
            label="component.skos.ontologies-references-label"
            label-size="lg"
            label-class="font-weight-bold pt-0"
            class="mb-0"
        >
          <template v-slot:label>
            {{ $t('AgroportalSearchFormPart.search-for-ontology-term') }}
          </template>
        </b-form-group>

        <!-- Search bar -->
        <opensilex-AgroportalSearch
            id="v-step-search"
            label="component.common.name"
            type="text"
            ref="searchComponent"
            :placeholder="$t(props.searchPlaceholder)"
            :selected.sync="ontologies"
            :isAllOntologies.sync="isAllOntologies"
            @change="onSearchTextChange"
        ></opensilex-AgroportalSearch>

        <!-- Search results -->
        <div class="row">
          <div class="col-lg-12">
            <opensilex-AgroportalResults
                id="v-step-result"
                ref="searchResults"
                :text.sync="text"
                :ontologies.sync="ontologies"
                @import="selectItem">
            </opensilex-AgroportalResults>
          </div>
        </div>

      </b-col>

      <b-col lg="4" id="selected-term-panel">

        <b-form-group
            label-size="lg"
            label-class="font-weight-bold pt-0"
            class="mb-0"
        >
          <template v-slot:label>
            <b-row>
              <b-col xs="6">
                {{ $t("AgroportalSearchFormPart.selected-term") }}
              </b-col>
              <b-col xs="2" v-if="!!selectedEntity">
                <opensilex-Button
                    @click="clear"
                    variant="outline-danger"
                    :small="true"
                    icon="fa#trash-alt"
                >
                </opensilex-Button>
              </b-col>
            </b-row>
          </template>

        </b-form-group>

        <opensilex-AgroportalResultItem
            v-if="!!selectedEntity"
            :entity="selectedEntity"
            id="v-step-selected"
        >
        </opensilex-AgroportalResultItem>
        <div v-else>
          {{ $t('AgroportalSearchFormPart.no-selected-item') }}
        </div>

      </b-col>
    </b-row>
  </ValidationObserver>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
import {EntityAgroportalDTO} from "opensilex-core/model/entityAgroportalDTO";
import AgroportalResults from "./AgroportalResults.vue";
import { Tour } from "vue-tour";
import AgroportalSearch from "./AgroportalSearch.vue";


@Component
export default class AgroportalSearchFormPart extends Vue {
  $opensilex: any;

  uriGenerated = true;
  text = "";
  ontologies: string[] = [];
  isAllOntologies: boolean = false;

  @Prop()
  editMode: boolean;

  errorMsg: String = "";

  @PropSync("form")
  formDto: any;

  selectedEntity: EntityAgroportalDTO = null;

  @Ref("tutorial")
  private tutorial: Tour;

  @Prop()
  private props: {
    searchPlaceholder: string,
    ontologiesConfig: string
  };

  private tutorialSteps = [
    {
      target: "#v-step-search",
      header: {title: this.$t("AgroportalSearchFormPart.tutorial.step-search.title")},
      content: this.$t("AgroportalSearchFormPart.tutorial.step-search.content"),
      params: {placement: "bottom"}
    },
    {
      target: "#v-step-result",
      header: {title: this.$t("AgroportalSearchFormPart.tutorial.step-result.title")},
      content: this.$t("AgroportalSearchFormPart.tutorial.step-result.content"),
      params: {placement: "left"},
      before: this.beforeImportStep
    },
    {
      target: "#v-step-selected",
      header: {title: this.$t("AgroportalSearchFormPart.tutorial.step-selected.title")},
      content: this.$t("AgroportalSearchFormPart.tutorial.step-selected.content"),
      params: {placement: "right"}
    },
    {
      target: "#v-step-wizard-buttons",
      header: {title: this.$t("AgroportalSearchFormPart.tutorial.step-validation.title")},
      content: this.$t("AgroportalSearchFormPart.tutorial.step-validation.content"),
      params: {placement: "top"}
    },
    {
      header: {title: this.$t("AgroportalSearchFormPart.tutorial.step-no-concept.title")},
      content: this.$t("AgroportalSearchFormPart.tutorial.step-no-concept.content"),
      before: this.beforeNoSearchStep
    }
  ]

  private savedSearchTerm?: string;


  handleErrorMessage(errorMsg: string) {
    this.errorMsg = errorMsg;
  }

  @Ref("validatorRef") readonly validatorRef!: any;
  @Ref("searchComponent") readonly searchComponent!: AgroportalSearch;
  @Ref("searchResults") readonly searchResults!: AgroportalResults;

  created() {
    this.ontologies = this.$opensilex.getConfig().agroportal[this.props.ontologiesConfig];
  }

  onSearchTextChange(searchedText: string) {
    this.text = searchedText;
    this.searchResults.updateResults(searchedText, this.isAllOntologies);
  }

  importResult(entity: EntityAgroportalDTO) {
    if (!entity) return;
    this.selectedEntity = entity;
    this.$emit("fill", this.selectedEntity);
  }

  clear() {
    this.selectedEntity = null;
    //this.$emit("clear");
  }

  selectItem(entity: EntityAgroportalDTO) {
    this.selectedEntity = entity;
    this.importResult(this.selectedEntity);
  }

  reset() {
    this.uriGenerated = true;
    return this.validatorRef.reset();
  }

  validate() {
    this.importResult(this.selectedEntity);
    return this.validatorRef.validate();
  }

  startTutorial() {
    this.savedSearchTerm = this.text;
    this.searchComponent.setSearchTerm(this.$t(this.props.searchPlaceholder).toString());
    this.tutorial.start();
  }

  async beforeImportStep() {
    this.searchResults.selectAndImportItem(0);
  }

  async beforeNoSearchStep() {
    this.clearForTutorial();
  }

  onTutorialFinishOrSkip() {
    this.clearForTutorial();
  }

  clearForTutorial() {
    this.searchComponent.setSearchTerm(this.savedSearchTerm);
    this.selectedEntity = undefined;
  }
}
</script>

<style scoped lang="scss">
a {
  color: #007bff;
}
</style>

<i18n>
en:
  AgroportalSearchFormPart:
    step1-title: Search
    step2-title: Enrich
    step3-title: Mapping
    import-and-save: Import & Save
    save: Save
    enrich: Enrich
    skip: Skip
    search-for-ontology-term: Search for ontology term
    selected-term: Selected term
    no-selected-item: No selected term
    tutorial:
      step-search:
        title: Search a term
        content: Look for a term in Agroportal. You can change the searched ontologies using the filter button.
      step-result:
        title: Select a concept
        content: >
          Select the concept that you want to import. If no concept exactly matches yours, you can select the
          closest one and enrich it on later steps.
      step-selected:
        title: Selected concept
        content: >
          The selected concept appears here. If you want to deselect it, you can click on the trash button above.
      step-validation:
        title: Validation
        content: >
          If you want to import the concept as-is, click the '@:AgroportalSearchFormPart.import-and-save' button. If
          you want to use it as a basis for creating your own concept, click the '@:AgroportalSearchFormPart.enrich'
          button.
      step-no-concept:
        title: No concept
        content: >
          If you didn't find any concept in Agroportal that matches yours, you can create your own by clicking the
          '@:AgroportalSearchFormPart.enrich' button when no concept is selected.
fr:
  AgroportalSearchFormPart:
    step1-title: Chercher
    step2-title: Enrichir
    step3-title: Mapper
    import-and-save: Importer & Enregistrer
    save: Enregistrer
    enrich: Enrichir
    skip: Passer
    search-for-ontology-term: Rechercher un terme
    selected-term: Terme sélectionné
    no-selected-item: Aucun terme sélectionné
    tutorial:
      step-search:
        title: Cherchez un terme
        content: >
          Recherchez un terme dans Agroportal. Vous pouvez changer les ontologies parcourues en cliquant sur le
          bouton de filtre.
      step-result:
        title: Sélectionnez un concept
        content: >
          Sélectionnez le concept à importer. Si aucun concept ne correspond à celui que vous cherchez à définir, vous
          pouvez choisir le concept qui s'en rapproche le plus et l'enrichir aux étapes suivantes.
      step-selected:
        title: Concept sélectionné
        content: >
          Le concept sélectionné apparaît ici. Pour le retirer, cliquez sur le bouton de suppression ci-dessus.
      step-validation:
        title: Validation
        content: >
          Si vous souhaitez réutiliser le concept tel quel, cliquez sur le bouton
          '@:AgroportalSearchFormPart.import-and-save'. Si vous souhaitez l'utiliser comme base pour définir votre
          propre concept, cliquez sur le bouton '@:AgroportalSearchFormPart.enrich'.
      step-no-concept:
        title: Pas de concept
        content: >
          Si vous ne trouvez pas de concept dans Agroportal qui correspond à celui que vous cherchez à définir, vous
          pouvez créer le vôtre en cliquant sur le bouton '@:AgroportalSearchFormPart.enrich' lorsqu'aucun concept
          n'est sélectionné.
</i18n>
