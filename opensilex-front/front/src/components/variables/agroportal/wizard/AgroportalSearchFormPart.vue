<template>
    <ValidationObserver ref="validatorRef">

        <b-row>
            <b-col sm="12" lg="8">

              <!-- Title -->
              <b-form-group
                  label="component.skos.ontologies-references-label"
                  label-size="lg"
                  label-class="font-weight-bold pt-0"
                  class="mb-0"
              >
                <template v-slot:label>{{ $t('AgroportalSearchFormPart.search-for-ontology-term') }}</template>
              </b-form-group>

              <!-- Search bar -->
              <opensilex-AgroportalSearch
                  label="component.common.name"
                  type="text"
                  :placeholder="$t(props.searchPlaceholder)"
                  :selected.sync="ontologies"
                  :isAllOntologies.sync="isAllOntologies"
                  @change="onSearchTextChange"
              ></opensilex-AgroportalSearch>

              <!-- Search results -->
              <div class="row">
                <div class="col-lg-12">
                  <opensilex-AgroportalResults
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
                  <b-row align-h="left">
                    <b-col xs="6">
                      {{$t("AgroportalSearchFormPart.selected-term")}}
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
// @ts-ignore
import { EntityCreationDTO } from "opensilex-core";
import {EntityAgroportalDTO} from "opensilex-core/model/entityAgroportalDTO";
import AgroportalResults from "./AgroportalResults.vue";


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

    @Prop()
    props;

    selectedEntity: EntityAgroportalDTO = null;


    handleErrorMessage(errorMsg: string) {
        this.errorMsg = errorMsg;
    }

    @Ref("validatorRef") readonly validatorRef!: any;
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
}
</script>

<style scoped lang="scss">
    a {color: #007bff;}
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
</i18n>
