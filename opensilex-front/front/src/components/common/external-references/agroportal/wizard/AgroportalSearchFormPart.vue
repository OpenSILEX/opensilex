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
          <opensilex-AgroportalTermSelector
              ref="agroportalTermSelector"
              :placeholder="$t(props.searchPlaceholder)"
              :ontologies="ontologies"
              @import="onTermImported"
              @inputValueHasChanged="updateItemName"
          ></opensilex-AgroportalTermSelector>
      </b-col>

      <b-col lg="4">
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
              <b-col xs="2" v-if="!!selectedTerm">
                <opensilex-Button
                    @click="clearSelectedTerm"
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
            v-if="!!selectedTerm"
            :entity="selectedTerm"
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
import {Component, Emit, Prop, PropSync, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import {Tour} from "vue-tour";
import {AgroportalTermDTO} from "opensilex-core/model/agroportalTermDTO";
import AgroportalTermSelector from "../AgroportalTermSelector.vue";
import {BaseExternalReferencesDTO} from "../../ExternalReferencesTypes";
import OpenSilexVuePlugin from "../../../../../models/OpenSilexVuePlugin";
import {ValidationObserver} from "vee-validate";

@Component
export default class AgroportalSearchFormPart extends Vue {
  //#region Plugins
  $opensilex: OpenSilexVuePlugin;
  //#endregion

  //#region Props
  @Prop()
  editMode: boolean;
  @Prop()
  private props: {
    searchPlaceholder: string,
    ontologiesConfig: string
  };
  @PropSync("form")
  formDto: BaseExternalReferencesDTO;
  //#endregion

  //#region Ref
  @Ref("tutorial")
  private tutorial: Tour;
  @Ref("validatorRef")
  readonly validatorRef!: InstanceType<typeof ValidationObserver>;
  @Ref("agroportalTermSelector")
  private readonly agroportalTermSelector: AgroportalTermSelector;
  //#endregion

  //#region Data
  uriGenerated = true;
  text = "";
  ontologies: string[] = [];
  isAllOntologies: boolean = false;
  selectedTerm: AgroportalTermDTO = null;
  //#endregion

  //#region Tutorial data
  private savedStateBeforeTutorial?: {
    searchTerm: string,
    formDto: BaseExternalReferencesDTO
  };
  private tutorialSteps = [
    {
      target: ".v-step-agroportal-search",
      header: {title: this.$t("AgroportalSearchFormPart.tutorial.step-search.title")},
      content: this.$t("AgroportalSearchFormPart.tutorial.step-search.content"),
      params: {placement: "bottom"}
    },
    {
      target: ".v-step-agroportal-results",
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
  //#endregion

  //#region Hooks
  private created() {
    this.ontologies = this.$opensilex.getConfig().agroportal[this.props.ontologiesConfig];
  }
  //#endregion

  //#region Private methods
  private clearSelectedTerm() {
    this.selectedTerm = null;
  }
  //#endregion

  //#region Events
  private onTermImported(term: AgroportalTermDTO) {
    this.selectedTerm = term;
    this.formDto.uri = term.id;
    this.formDto.name = term.name;
    this.formDto.description = term.definitions[0];
  }

  private updateItemName (text: string){
    this.formDto.name = text
  }
  //#endregion

  //#region Public methods for wizard form
  public reset() {
    this.uriGenerated = true;
    return this.validatorRef.reset();
  }

  public validate() {
    return this.validatorRef.validate();
  }
  //#endregion

  //#region Tutorial methods
  public startTutorial() {
    this.saveStateBeforeTutorial();
    this.clearCurrentState();
    this.agroportalTermSelector.setSearchText(this.$t(this.props.searchPlaceholder).toString());
    this.tutorial.start();
  }

  private saveStateBeforeTutorial() {
    this.savedStateBeforeTutorial = {
      searchTerm: this.text,
      formDto: JSON.parse(JSON.stringify(this.formDto))
    };
  }

@Watch('selectedTerm')
asTermSelectionChange() {
  this.$emit(this.selectedTerm ? "agroportalTermSelected" : "agroportalTermUnselected");
}

  private clearCurrentState() {
    this.agroportalTermSelector.setSearchText("");
    this.selectedTerm = undefined;
  }

  private restoreStateAfterTutorial() {
    this.text = this.savedStateBeforeTutorial.searchTerm;
    this.formDto = JSON.parse(JSON.stringify(this.savedStateBeforeTutorial.formDto));
  }

  private async beforeImportStep() {
    this.agroportalTermSelector.selectAndImportFirstItem();
  }

  private async beforeNoSearchStep() {
    this.clearCurrentState();
  }

  private onTutorialFinishOrSkip() {
    this.restoreStateAfterTutorial();
  }
  //#endregion
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
    step2-title: Create
    step3-title: Mapping
    reuse: Reuse
    save: Save
    createNew: Create New
    create: Create
    map: Map
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
          If you want to import the concept as-is, click the '@:AgroportalSearchFormPart.reuse' button. If
          you want to use it as a basis for creating your own concept, click the '@:AgroportalSearchFormPart.create'
          button. Please note that for Units, you are required to enrich the concept.
      step-no-concept:
        title: No concept
        content: >
          If you didn't find any concept in Agroportal that matches yours, you can create your own by clicking the
          '@:AgroportalSearchFormPart.create' button when no concept is selected.
fr:
  AgroportalSearchFormPart:
    step1-title: Chercher
    step2-title: Créer
    step3-title: Mapper
    reuse: Réutiliser
    save: Enregistrer
    createNew: Créer Nouveau
    create: Créer
    map: Mapper
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
          '@:AgroportalSearchFormPart.reuse'. Si vous souhaitez l'utiliser comme base pour définir votre
          propre concept, cliquez sur le bouton '@:AgroportalSearchFormPart.create'. Veuillez noter que vous
          devez impérativement enrichir le concept dans le cas d'une unité.
      step-no-concept:
        title: Pas de concept
        content: >
          Si vous ne trouvez pas de concept dans Agroportal qui correspond à celui que vous cherchez à définir, vous
          pouvez créer le vôtre en cliquant sur le bouton '@:AgroportalSearchFormPart.create' lorsqu'aucun concept
          n'est sélectionné.
</i18n>
