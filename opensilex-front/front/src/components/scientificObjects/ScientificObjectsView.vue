<template>
  <div class="container-fluid">

    <opensilex-PageActions
    v-if="
    user.hasCredential(credentials.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID)"
    >

      <opensilex-CreateButton
        @click="soForm.createScientificObject()"
        label="ExperimentScientificObjects.create-scientific-object"
        class="createButton greenThemeColor"
      ></opensilex-CreateButton>
      <opensilex-ScientificObjectForm
        ref="soForm"
        @onUpdate="redirectToDetail"
        @onCreate="redirectToDetail"
      ></opensilex-ScientificObjectForm>

      <opensilex-CreateButton
        @click="importForm.show()"
        label="OntologyCsvImporter.import"
        class="createButton greenThemeColor"
      ></opensilex-CreateButton>


      <opensilex-ScientificObjectCSVImporter
        ref="importForm"
        @csvImported="refresh()"
      ></opensilex-ScientificObjectCSVImporter>
    </opensilex-PageActions>


    <opensilex-PageContent
      class="pagecontent"
    >

      <!-- Toggle Sidebar--> 
      <div
        class="searchMenuContainer"
        v-on:click="SearchFiltersToggle = !SearchFiltersToggle"
        :title="searchFiltersPannel()"
      >
        <div class="searchMenuIcon">
          <i class="icon ik ik-search"></i>
        </div>
      </div>

      <!-- FILTERS -->
      <Transition>
        <div v-show="SearchFiltersToggle">

          <opensilex-SearchFilterField
            @search="soList.refresh()"
            @clear="reset()"
            searchButtonLabel="component.common.search.search-button"
            :showAdvancedSearch="true"
            class="searchFilterField"
          >
            <template v-slot:filters>

              <!-- Name -->
              <div>
              <opensilex-FilterField>
                <label for="name">{{ $t("component.common.name") }}</label>
                <opensilex-StringFilter
                  id="name"
                  :filter.sync="filter.name"
                  placeholder="ScientificObjectList.name-placeholder"
                  class="searchFilter"
                  @handlingEnterKey="soList.refresh()"
                ></opensilex-StringFilter>
                <br>
              </opensilex-FilterField>
              </div>
        
              <!-- Experiments --> 
              <div>   
              <opensilex-FilterField>
                <opensilex-ExperimentSelector
                  label="GermplasmList.filter.experiment"
                  :multiple="false"
                  :experiments.sync="filter.experiment"
                  class="searchFilter"
                  @handlingEnterKey="soList.refresh()"
                ></opensilex-ExperimentSelector>
              </opensilex-FilterField>
              </div>

              <!-- Types --> 
              <div>
              <opensilex-FilterField>
                <label for="type">{{ $t("component.common.type") }}</label>
                <opensilex-ScientificObjectTypeSelector
                  id="type"
                  :types.sync="filter.types"
                  :multiple="true"
                  class="searchFilter"
                ></opensilex-ScientificObjectTypeSelector>
              </opensilex-FilterField>
              </div>
            </template>

            <template v-slot:advancedSearch>

              <!-- Germplasm -->
              <div>
                <opensilex-FilterField quarterWidth="false">
                  <opensilex-GermplasmSelectorWithFilter
                      :germplasmsUris.sync="filter.germplasm"
                  ></opensilex-GermplasmSelectorWithFilter>
                </opensilex-FilterField>
              </div>

              <!-- Factors levels -->
              <div>
              <opensilex-FilterField>
                <b-form-group>
                  <label for="factorLevels">
                    {{ $t("FactorLevelSelector.label") }}
                  </label>
                  <opensilex-FactorLevelSelector
                    id="factorLevels"
                    :factorLevels.sync="filter.factorLevels"
                    :multiple="true"
                    :required="false"
                    class="searchFilter"
                  ></opensilex-FactorLevelSelector>
                </b-form-group>
              </opensilex-FilterField>
              </div>

              <!-- Exists -->
              <div>
              <opensilex-FilterField>
                <opensilex-DateForm
                  :value.sync="filter.existenceDate"
                  label="ScientificObjectList.existenceDate"
                  class="searchFilter"
                ></opensilex-DateForm>
              </opensilex-FilterField>
              </div>

              <!-- Created -->
              <div>
              <opensilex-FilterField>
                <opensilex-DateForm
                  :value.sync="filter.creationDate"
                  label="ScientificObjectList.creationDate"
                  class="searchFilter"
                ></opensilex-DateForm>
              </opensilex-FilterField>
              </div>

              <!-- Criteria search -->
              <div>
                <opensilex-FilterField quarterWidth="false">
                  <opensilex-CriteriaSearchModalCreator
                      class="searchFilter"
                      ref="criteriaSearchCreateModal"
                      :criteria_dto.sync="filter.criteriaDto"
                      :required="false"
                      :requiredBlue="false"
                  ></opensilex-CriteriaSearchModalCreator>
                </opensilex-FilterField>
              </div>
            </template>
          </opensilex-SearchFilterField>
        </div>
      </Transition>

        <opensilex-ScientificObjectList
          ref="soList"
          :searchFilter="filter"
          @update="soForm.editScientificObject($event)"
          @createDocument="createDocument"
          @createEvents="createEvents"
          @createMoves="createMoves"
          class="scientificObjectList"
        ></opensilex-ScientificObjectList>  

    </opensilex-PageContent>

    <opensilex-ModalForm
      ref="documentForm"
      component="opensilex-DocumentForm"
      createTitle="component.common.addDocument"
      modalSize="lg"
      :initForm="initForm"
      icon="ik#ik-file-text"
    ></opensilex-ModalForm>

    <opensilex-EventCsvForm
      ref="eventCsvForm"
      :targets="selectedUris"
    ></opensilex-EventCsvForm>

    <opensilex-EventCsvForm
      ref="moveCsvForm"
      :targets="selectedUris"
      :isMove="true"
    ></opensilex-EventCsvForm>

    <opensilex-ScientificObjectForm
      ref="soForm"
      @onUpdate="redirectToDetail"
    ></opensilex-ScientificObjectForm>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import EventCsvForm from "../events/form/csv/EventCsvForm.vue";
import CriteriaSearchModalCreator from "./CriteriaSearchModalCreator.vue";

@Component
export default class ScientificObjectsView extends Vue {
  $opensilex: any;
  $store: any;

  @Ref("soForm") readonly soForm!: any;
  @Ref("importForm") readonly importForm!: any;
  @Ref("documentForm") readonly documentForm!: any;
  @Ref("eventCsvForm") readonly eventCsvForm!: EventCsvForm;
  @Ref("moveCsvForm") readonly moveCsvForm!: EventCsvForm;
  @Ref("soList") readonly soList!: any;
  @Ref("criteriaSearchCreateModal") readonly criteriaSearchCreateModal!: CriteriaSearchModalCreator;

  selectedUris: Array<string> = [];

  get user() {
    return this.$store.state.user;
  }
  get credentials() {
    return this.$store.state.credentials;
  }
  
  filter = {
    name: "",
    experiment: undefined,
    germplasm: [],
    factorLevels: [],
    types: [],
    existenceDate: undefined,
    creationDate: undefined,
    criteriaDto: undefined
  };

  data(){
    return {
      SearchFiltersToggle : false,
    }
  }
  
  searchFiltersPannel() {
    return  this.$t("searchfilter.label")
  }



  redirectToDetail(http) {
    this.$router.push({
      path:
        "/scientific-objects/details/" +
        encodeURIComponent(http.response.result),
    });
  }

    createDocument() {
    this.documentForm.showCreateForm();
  }

  createEvents() {
    this.updateSelectedUris();
    this.eventCsvForm.show();
  }

  createMoves() {
    this.updateSelectedUris();
    this.moveCsvForm.show();
  }

  updateSelectedUris() {
    this.selectedUris = [];
    for (let select of this.soList.getSelected()) {
      this.selectedUris.push(select.uri);
    }
  }

  initForm() {
    let targetURI = [];
    for (let select of this.soList.getSelected()) {
      targetURI.push(select.uri);
    }

    return {
      description: {
        uri: undefined,
        identifier: undefined,
        rdf_type: undefined,
        title: undefined,
        date: undefined,
        description: undefined,
        targets: targetURI,
        authors: undefined,
        language: undefined,
        deprecated: undefined,
        keywords: undefined,
      },
      file: undefined,
    };
  }

  reset() {
    this.filter = {
      name: "",
      experiment: undefined,
      germplasm: [],
      factorLevels: [],
      types: [],
      existenceDate: undefined,
      creationDate: undefined,
      criteriaDto: undefined
    };
    this.criteriaSearchCreateModal.resetCriteriaListAndSave();
    this.soList.refresh();
  }
}
</script>

<style scoped lang="scss">

.createButton{
  margin-bottom: 10px;
  margin-top: -15px;

}
</style>