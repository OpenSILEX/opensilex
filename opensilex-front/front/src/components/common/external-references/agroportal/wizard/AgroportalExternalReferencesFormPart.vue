<template>
  <div class="v-step-agroportal-references">
    <opensilex-Tutorial
        ref="tutorial"
        :steps="tutorialSteps"
        @onFinish="onTutorialFinishOrSkip"
        @onSkip="onTutorialFinishOrSkip"
    >
    </opensilex-Tutorial>
    <ValidationObserver ref="validatorRef">
      <b-form>
        <b-row>
          <b-col md="6">
            <b-form-group
                label-size="lg"
                label-class="font-weight-bold pt-0"
                class="mb-0"
            >
              <template v-slot:label>
                {{ $t('AgroportalExternalReferencesFormPart.search-mapping-title') }}
              </template>

              <opensilex-AgroportalTermSelector
                  v-if="isAgroportalReachable"
                  ref="agroportalTermSelector"
                  :placeholder="$t(props.searchPlaceholder)"
                  :ontologies="ontologies"
                  :isMappingMode="true"
                  @importMapping="onImportMapping"
              ></opensilex-AgroportalTermSelector>
            </b-form-group>

            <b-form-group
                label-size="lg"
                label-class="font-weight-bold pt-0"
                class="mb-0"
            >
              <template v-slot:label>
                <span id="manual-mapping">
                  {{ $t('AgroportalExternalReferencesFormPart.map-manually-title') }}
                </span>
              </template>

              <!-- URI -->
              <opensilex-FilterField
                  :fullWidth="true"
              >
                <b-form-group>
                  <div class="helperAndBlueStar">
                    <opensilex-FormInputLabelHelper
                        label="AgroportalExternalReferencesFormPart.manual-mapping"
                        helpMessage="AgroportalExternalReferencesFormPart.ontologies-help"
                    ></opensilex-FormInputLabelHelper>
                  </div>
                  <span
                      class="error-message alert alert-danger"
                      v-if="isIncludedInRelations()"
                  >{{ $t('component.skos.external-already-existing') }}</span>
                  <opensilex-SkosRelationInput
                      @input="addRelationToTerm"
                  ></opensilex-SkosRelationInput>
                </b-form-group>
              </opensilex-FilterField>
            </b-form-group>
          </b-col>

          <b-col md="6">
            <b-form-group
                label-size="lg"
                label-class="font-weight-bold pt-0"
                class="mb-0"
            >
              <template v-slot:label>
                {{ $t("AgroportalSearchFormPart.selected-term") }}
              </template>

              <b-container class="result">
                <b-row class="mx-0 jqx-max-size">
                  <b-col col lg="12" class="result-name">
                      {{ formDto.name }}
                  </b-col>
                </b-row>
                <b-row>
                  <b-col col lg="12">
                    <a v-bind:href="formDto.uri" target="_blank" rel="noopener noreferrer">{{ formDto.uri }}</a>
                  </b-col>
                </b-row>
                <b-row class="mx-0 jqx-max-size">
                  <b-col col lg="12">
                    {{ formDto.description }}
                  </b-col>
                </b-row>
              </b-container>
            </b-form-group>

            <opensilex-SkosRelationTable
              :uriRelations.sync="uriRelations"
            ></opensilex-SkosRelationTable>
          </b-col>
        </b-row>
      </b-form>
    </ValidationObserver>
  </div>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../../../../models/OpenSilexVuePlugin";
import {AgroportalAPIService} from "opensilex-core/api/agroportalAPI.service";
import SUPPORTED_SKOS_RELATIONS, {BROAD_MATCH, UriSkosRelation} from "../../../../../models/SkosRelations";
import {BaseExternalReferencesDTO} from "../../ExternalReferencesTypes";
import {Tour} from "vue-tour";
import {AgroportalTermDTO} from "opensilex-core/model/agroportalTermDTO";
import AgroportalTermSelector from "../AgroportalTermSelector.vue";
import {SelectableItem} from "../../../forms/SelectForm.vue";

@Component
export default class AgroportalExternalReferencesFormPart extends Vue {
  //region Plugins
  private readonly $opensilex: OpenSilexVuePlugin;
  //endregion

  //region Props
  @Prop()
  private readonly props: {
    ontologiesConfig: string,
    searchPlaceholder: string
  };

  @Prop({default: true})
  private displayInsertButton: boolean;

  @PropSync("form")
  private formDto: BaseExternalReferencesDTO;

  //endregion

  //region Refs
  @Ref("validatorRef")
  private readonly validatorRef!: any;

  @Ref("agroportalTermSelector")
  private readonly agroportalTermSelector: AgroportalTermSelector;

  @Ref("tutorial")
  private tutorial: Tour;
  //endregion

  //region Data
  private agroportalAPIService: AgroportalAPIService;
  private ontologies: string[] = [];
  private isAgroportalReachable: boolean = false;

  private readonly fields = [
    {
      key: "relation",
      label: "component.skos.relation",
      sortable: true
    },
    {
      key: "relationURI",
      label: "component.skos.uri",
      sortable: false
    },
    {
      key: "actions",
      label: "component.common.actions"
    }
  ];

  //region Tutorial data
  private savedStateBeforeTutorial: {
    formDto: BaseExternalReferencesDTO
  }

  private readonly tutorialSteps = [
    {
      target: ".v-step-agroportal-references .v-step-agroportal-search",
      header: {title: this.$t("AgroportalExternalReferencesFormPart.tutorial.step-search.title")},
      content: this.$t("AgroportalExternalReferencesFormPart.tutorial.step-search.content"),
      params: {placement: "left"}
    },
    {
      target: ".v-step-agroportal-references .v-step-agroportal-results",
      header: {title: this.$t("AgroportalExternalReferencesFormPart.tutorial.step-results.title")},
      content: this.$t("AgroportalExternalReferencesFormPart.tutorial.step-results.content"),
      params: {placement: "left"}
    },
    {
      target: ".v-step-agroportal-references .v-step-agroportal-results .v-step-result-mapping-button",
      header: {title: this.$t("AgroportalExternalReferencesFormPart.tutorial.step-result-mapping.title")},
      content: this.$t("AgroportalExternalReferencesFormPart.tutorial.step-result-mapping.content"),
      params: {placement: "right", enableScrolling: false},
      before: this.beforeImportMappingStep
    },
    {
      target: ".v-step-agroportal-references .v-step-table",
      header: {title: this.$t("AgroportalExternalReferencesFormPart.tutorial.step-table.title")},
      content: this.$t("AgroportalExternalReferencesFormPart.tutorial.step-table.content"),
      params: {placement: "left"},
      before: this.beforeMappingOverviewStep
    },
    {
      target: ".v-step-agroportal-references .v-step-change-mapping",
      header: {title: this.$t("AgroportalExternalReferencesFormPart.tutorial.step-change-mapping.title")},
      content: this.$t("AgroportalExternalReferencesFormPart.tutorial.step-change-mapping.content"),
      params: {placement: "left"}
    },
    {
      target: ".v-step-agroportal-references .v-step-manual-uri",
      header: {title: this.$t("AgroportalExternalReferencesFormPart.tutorial.step-manual-uri.title")},
      content: this.$t("AgroportalExternalReferencesFormPart.tutorial.step-manual-uri.content"),
      params: {placement: "top"},
      before: this.beforeManualMappingStep
    },
    {
      target: ".v-step-agroportal-references .v-step-manual-mapping",
      header: {title: this.$t("AgroportalExternalReferencesFormPart.tutorial.step-manual-mapping.title")},
      content: this.$t("AgroportalExternalReferencesFormPart.tutorial.step-manual-mapping.content"),
      params: {placement: "top"}
    },
    {
      target: ".v-step-agroportal-references .v-step-table",
      header: {title: this.$t("AgroportalExternalReferencesFormPart.tutorial.step-table-bis.title")},
      content: this.$t("AgroportalExternalReferencesFormPart.tutorial.step-table-bis.content"),
      params: {placement: "left"},
      before: this.beforeMappingOverviewAgainStep
    },
    {
      target: "#v-step-wizard-buttons",
      header: {title: this.$t("AgroportalExternalReferencesFormPart.tutorial.step-validation.title")},
      content: this.$t("AgroportalExternalReferencesFormPart.tutorial.step-validation.content"),
      params: {placement: "top"}
    },
  ];
  //endregion
  //endregion

  //region Computed
  /**
   * Computed getter and setter which translates the weird relation maps in the DTO to a list of URI<->Relation pairs
   * for the table to display.
   */
  get uriRelations(): Array<UriSkosRelation> {
    const uriRelationList: Array<UriSkosRelation> = [];
    for (const skosRelation of SUPPORTED_SKOS_RELATIONS) {
      for (const relationUri of this.formDto[skosRelation.dtoKey]) {
        uriRelationList.push({
          relationDtoKey: skosRelation.dtoKey,
          uri: relationUri as string
        });
      }
    }
    return uriRelationList;
  }

  set uriRelations(newUriRelations: Array<UriSkosRelation>) {
    this.formDto.exact_match = [];
    this.formDto.close_match = [];
    this.formDto.broad_match = [];
    this.formDto.narrow_match = [];
    for (const uriRelation of newUriRelations) {
      this.formDto[uriRelation.relationDtoKey].push(uriRelation.uri);
    }
  }
  //endregion

  //region Hooks
  created() {
    this.agroportalAPIService = this.$opensilex.getService<AgroportalAPIService>("opensilex.AgroportalAPIService");
    this.checkAgroportalReachable();
    this.ontologies = this.$opensilex.getConfig().agroportal[this.props.ontologiesConfig];
  }
  //endregion

  //region Private methods
  private checkAgroportalReachable() {
    this.agroportalAPIService.pingAgroportal(1000).then((http) => {
      if (http && http.response) {
        let isReachable = http.response.result;
        this.isAgroportalReachable = isReachable;
      }
    });
  }

  private resetExternalUriForm() {
    this.$nextTick(() => this.validatorRef.reset());
  }


  private addRelationToTerm(uriRelation: UriSkosRelation) {
    let isIncludedInRelations = this.isIncludedInRelations();
    if (!isIncludedInRelations) {
      this.formDto[uriRelation.relationDtoKey].push(uriRelation.uri);
      this.resetExternalUriForm();
    }
  }

  private isIncludedInRelations(): boolean {
    //@todo
    return false;
  }
  //endregion

  //region Event Handlers
  private onImportMapping(uriRelation: UriSkosRelation) {
    console.debug("Import mapping", uriRelation);
    this.addRelationToTerm(uriRelation);
  }
  //endregion

  //region Tutorial
  private saveStateBeforeTutorial() {
    this.savedStateBeforeTutorial = {
      formDto: JSON.parse(JSON.stringify(this.formDto))
    };
  }

  private clearCurrentState() {
    this.formDto.exact_match = [];
    this.formDto.close_match = [];
    this.formDto.broad_match = [];
    this.formDto.narrow_match = [];
  }

  private restoreStateAfterTutorial() {
    this.formDto = JSON.parse(JSON.stringify(this.savedStateBeforeTutorial.formDto));
  }

  private async beforeImportMappingStep() {
    this.agroportalTermSelector.selectFirstItem();
  }

  private async beforeMappingOverviewStep() {
    this.agroportalTermSelector.selectAndMapFirstItem();
  }

  private async beforeManualMappingStep() {
    this.agroportalTermSelector.setSearchText("");
  }

  private async beforeMappingOverviewAgainStep() {
    this.addRelationToTerm({
      uri: "http://www.w3.org/2002/07/owl#Thing",
      relationDtoKey: BROAD_MATCH.dtoKey
    });
  }

  public startTutorial() {
    this.saveStateBeforeTutorial();
    this.clearCurrentState();
    this.agroportalTermSelector.setSearchText(this.$t(this.props.searchPlaceholder).toString());
    this.tutorial.start();
  }

  private onTutorialFinishOrSkip() {
    this.restoreStateAfterTutorial();
  }
  //endregion
}

</script>

<style scoped>

a {
  color: #007bff;
}

.helperAndBlueStar {
  display: flex;
}

.result-name {
  font-weight: bold;
  font-size: large;
  margin-bottom: 5px;
}

#manual-mapping {
  padding-top: 10px;
}

.result {
  font-size: medium;
  margin-bottom: 10px;
  padding: 10px;
  margin-right: 1px;
}

ul {
  list-style-type: none;
}

</style>

<i18n>
en:
  AgroportalExternalReferencesFormPart:
    uri-help: "Uncheck this checkbox if you want to insert a concept from an existing ontology or if want to set a particular URI. Let it checked if you want to create a new entity with an auto-generated URI"
    ontologies-help: "You can find URIs in this locations:
      <li>
        <ul style=\"list-style-type: none;\"><a target=\"_blank\" rel=\"noopener noreferrer\" href=\"https://agroportal.lirmm.fr/\">AgroPortal</a></ul>
        <ul style=\"list-style-type: none;\"><a target=\"_blank\" rel=\"noopener noreferrer\" href=\"https://agroportal.lirmm.fr/\">BioPortal</a></ul>
      </li>"
    search-mapping-title: Search for mapping...
    map-manually-title: ...Or map manually
    manual-mapping: "URI"
    tutorial:
      step-search:
        title: Search
        content: >
          Search for a term on Agroportal that you might want to link to your concept.
      step-results:
        title: Results
        content: >
          Browse the results from Agroportal and select one that you want to link as a reference to your concept.
      step-result-mapping:
        title: Result mapping
        content: >
          Click on the button to select the type of mapping that describes the relation between your concept and this
          one.
      step-table:
        title: References
        content: >
          The currently defined references are shown in this table. If your concept was created from an Agroportal
          term selected in the first step, a 'close match' relation is already defined for you. You can modify or
          delete it as any other reference.
      step-change-mapping:
        title: Change reference
        content: >
          You can change the type of relation for a reference using this button.
      step-manual-uri:
        title: Manual URI
        content: >
          You can also define an external reference by specifying its URI in this field.
      step-manual-mapping:
        title: Manual URI mapping
        content: >
          Select the type of relation for this external reference.
      step-table-bis:
        title: References
        content: >
          You can have as many references as your want for your concept.
      step-validation:
        title: Validation
        content: >
          Once you have linked your concept to external references, click the '@:component.common.form-wizard.done'
          button.
fr:
  AgroportalExternalReferencesFormPart:
    uri-help: "Décocher si vous souhaitez ajouter une entité à partir d'une ontologie existante ou si vous souhaitez spécifier une URI particulière. Laisser coché si vous souhaitez ajouter une entité avec une URI auto-générée"
    ontologies-help: "Vous pouvez chercher des URIs via ces portails:
      <li>
        <ul style=\"list-style-type: none;\"><a target=\"_blank\" rel=\"noopener noreferrer\" href=\"https://agroportal.lirmm.fr/\">AgroPortal</a></ul>
        <ul style=\"list-style-type: none;\"><a target=\"_blank\" rel=\"noopener noreferrer\" href=\"https://agroportal.lirmm.fr/\">BioPortal</a></ul>
      </li>"
    search-mapping-title: Rechercher des mapping...
    map-manually-title: ...Ou mapper manuellement
    manual-mapping: "URI"
    tutorial:
      step-search:
        title: Recherche de terme
        content: >
          Cherchez un terme sur Agroportal que vous voudriez ajouter comme référence pour votre concept.
      step-results:
        title: Résultats
        content: >
          Parcourez les résultats de la recherche et sélectionnez le concept que vous souhaitez ajouter comme référence.
      step-result-mapping:
        title: Ajout de la relation
        content: >
          Cliquez sur ce bouton pour sélectionner le type de relation qui lie votre concept à celui-ci.
      step-table:
        title: Références
        content: >
          Les références actuellement définies pour votre concept sont montrées dans ce tableau. Si vous avez créé
          votre concept à partir d'un terme d'Agroportal à l'étape 1, alors une relation de type 'similaire' a été
          ajoutée automatiquement. Vous pouvez modifier ou supprimer cette relation, tout comme n'importe quelle
          autre référence.
      step-change-mapping:
        title: Modifier une référence
        content: >
          Vous pouvez changer le type de relation d'une référence en utilisant ce bouton.
      step-manual-uri:
        title: Référence arbitraire
        content: >
          Vous pouvez également définir une référence vers un concept en spécifiant son URI dans ce champ, par
          exemple dans le cas où vous ne le trouvez pas dans Agroportal.
      step-manual-mapping:
        title: Ajout de la relation
        content: >
          Sélectionnez le type de la relation qui lie votre concept à celui-ci.
      step-table-bis:
        title: Nombre de références
        content: >
          Vous pouvez avoir autant de références que vous le souhaitez pour votre concept.
      step-validation:
        title: Validation
        content: >
          Une fois votre concept lié à des références externes, cliquez sur le bouton
          '@:component.common.form-wizard.done' pour créer votre concept.
</i18n>