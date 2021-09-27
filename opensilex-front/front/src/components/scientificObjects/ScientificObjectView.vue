<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-target"
      title="component.menu.scientificObjects"
      description="ScientificObjectList.description"
    ></opensilex-PageHeader>

    <opensilex-PageActions
      v-if="
        user.hasCredential(
          credentials.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID
        )
      "
    >
      <opensilex-CreateButton
        @click="soForm.createScientificObject()"
        label="ExperimentScientificObjects.create-scientific-object"
      ></opensilex-CreateButton>
      <opensilex-ScientificObjectForm
        ref="soForm"
        @onUpdate="redirectToDetail"
        @onCreate="redirectToDetail"
      ></opensilex-ScientificObjectForm>
      &nbsp;
      <opensilex-CreateButton
        @click="importForm.show()"
        label="OntologyCsvImporter.import"
      ></opensilex-CreateButton>
      <opensilex-ScientificObjectCSVImporter
        ref="importForm"
        @csvImported="refresh()"
      ></opensilex-ScientificObjectCSVImporter>
    </opensilex-PageActions>
    
    <opensilex-PageContent>
      <opensilex-SearchFilterField
        @search="soList.refresh()"
        @clear="reset()"
        searchButtonLabel="component.common.search.visualize-button"
        :showAdvancedSearch="true"
      >
      <template v-slot:filters>
        <!-- Name -->
        <opensilex-FilterField>
          <label for="name">{{ $t("component.common.name") }}</label>
          <opensilex-StringFilter
            id="name"
            :filter.sync="filter.name"
            placeholder="ScientificObjectList.name-placeholder"
          ></opensilex-StringFilter>
        </opensilex-FilterField>
        <!-- Experiments -->
        <opensilex-FilterField>
          <opensilex-ExperimentSelector
            label="GermplasmList.filter.experiment"
            :multiple="false"
            :experiments.sync="filter.experiment"
          ></opensilex-ExperimentSelector>
        </opensilex-FilterField>

        <opensilex-FilterField>
          <label for="type">{{ $t("component.common.type") }}</label>
          <opensilex-ScientificObjectTypeSelector
            id="type"
            :types.sync="filter.types"
            :multiple="true"
          ></opensilex-ScientificObjectTypeSelector>
        </opensilex-FilterField>
      </template>

      <template v-slot:advancedSearch>
        <!-- Germplasm -->
        <opensilex-FilterField>
          <opensilex-GermplasmSelector
            :multiple="false"
            :germplasm.sync="filter.germplasm"
          ></opensilex-GermplasmSelector>
        </opensilex-FilterField>
        <!-- Factors levels -->
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
            ></opensilex-FactorLevelSelector>
          </b-form-group>
        </opensilex-FilterField>
        <!-- Exists -->
        <opensilex-FilterField>
          <opensilex-DateForm
            :value.sync="filter.existenceDate"
            label="ScientificObjectList.existenceDate"
          ></opensilex-DateForm>
        </opensilex-FilterField>
        <!-- Created -->
        <opensilex-FilterField>
          <opensilex-DateForm
            :value.sync="filter.creationDate"
            label="ScientificObjectList.creationDate"
          ></opensilex-DateForm>
        </opensilex-FilterField>
      </template>
    </opensilex-SearchFilterField>

    <opensilex-ScientificObjectList
      ref="soList"
      :searchFilter="filter"
      @update="soForm.editScientificObject($event)"
      @createDocument="createDocument"
      @createEvents="createEvents"
      @createMoves="createMoves"
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

@Component
export default class ScientificObjectView extends Vue {
  $opensilex: any;
  $store: any;

  @Ref("soForm") readonly soForm!: any;
  @Ref("importForm") readonly importForm!: any;
  @Ref("documentForm") readonly documentForm!: any;
  @Ref("eventCsvForm") readonly eventCsvForm!: EventCsvForm;
  @Ref("moveCsvForm") readonly moveCsvForm!: EventCsvForm;
  @Ref("soList") readonly soList!: any;

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
    germplasm: undefined,
    factorLevels: [],
    types: [],
    existenceDate: undefined,
    creationDate: undefined,
  };

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
      germplasm: undefined,
      factorLevels: [],
      types: [],
      existenceDate: undefined,
      creationDate: undefined,
    };
    this.soList.refresh();
  }
}
</script>

<style scoped lang="scss">
</style>
