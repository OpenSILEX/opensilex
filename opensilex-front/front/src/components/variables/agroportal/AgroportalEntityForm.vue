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
                <template v-slot:label>{{ $t('AgroportalEntityForm.search-for-ontology-term') }}</template>
              </b-form-group>

              <!-- Search bar -->
              <opensilex-AgroportalSearch
                  label="component.common.name"
                  type="text"
                  placeholder="search"
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
                      Selected entity
                    </b-col>
                    <b-col xs="2" v-if="!!entity">
                      <opensilex-Button
                          @click="removeSelected"
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
                  v-if="!!entity"
                  :entity="entity"
              >
              </opensilex-AgroportalResultItem>
              <div v-else>
                {{ $t('AgroportalEntityForm.no-selected-item') }}
              </div>

            </b-col>
        </b-row>
    </ValidationObserver>
</template>

<script lang="ts">
    import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
import {ExternalOntologies} from "../../../models/ExternalOntologies";
import EntityCreate from "./AgroportalEntityCreate.vue";
// @ts-ignore
import { EntityCreationDTO } from "opensilex-core/index";
import {EntityAgroportalDTO} from "opensilex-core/model/entityAgroportalDTO";
    import AgroportalResults from "../AgroportalResults.vue";

@Component
export default class AgroportalEntityForm extends Vue {
    $opensilex: any;

    uriGenerated = true;
    text = "";
    ontologies: string[] = [];
    isAllOntologies: boolean = false;

    @Prop()
    editMode: boolean;

    errorMsg: String = "";

    @PropSync("form")
    entityDto: EntityCreationDTO;
    entity: EntityAgroportalDTO = null;

    externalOntologiesRefs: any[] = ExternalOntologies.getExternalOntologiesReferences(EntityCreate.selectedOntologies);

    handleErrorMessage(errorMsg: string) {
        this.errorMsg = errorMsg;
    }

    @Ref("validatorRef") readonly validatorRef!: any;
    @Ref("searchResults") readonly searchResults!: AgroportalResults;

    created() {
      this.ontologies = this.$opensilex.getConfig().agroportal.entityOntologies;
    }

    onSearchTextChange(searchedText: string) {
      this.text = searchedText;
      this.searchResults.updateResults(searchedText, this.isAllOntologies);
    }

    importResult(entity: EntityAgroportalDTO) {
      if (!entity) return;
      this.entityDto.uri = entity.id;
      this.entityDto.name = entity.name;
      this.entityDto.description = entity.definitions[0];
      this.entityDto.exact_match = [];
      this.entityDto.narrow_match = [];
      this.entityDto.broad_match = [];
      this.entityDto.close_match = [];
    }

    removeSelected() {
      this.entity = null;
      this.clear();
    }

    selectItem(entity: EntityAgroportalDTO) {
      this.entity = entity;
      this.importResult(this.entity);
    }

    reset() {
        this.uriGenerated = true;
        return this.validatorRef.reset();
    }

    clear() {
      this.entityDto.name = "";
      for(let member in this.entityDto) {
        this.entityDto[member] = null;
      }
      console.debug(this.entityDto);
    }

    validate() {
        this.importResult(this.entity);
        return this.validatorRef.validate();
    }
}
</script>

<style scoped lang="scss">
    a {color: #007bff;}
</style>
