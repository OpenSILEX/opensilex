<template>
    <ValidationObserver ref="validatorRef">

        <div class="row">
            <div class="col-lg-8">

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
                  @change="onSearchTextChange"
              ></opensilex-AgroportalSearch>

              <!-- Search results -->
              <div class="row">
                <div class="col-lg-12">
                  <opensilex-AgroportalResults
                      ref="searchResults"
                      :text.sync="text"
                      :ontologies="ontologies"
                      @import="selectItem">
                  </opensilex-AgroportalResults>
                </div>
              </div>

            </div>

          <div class="col-lg-4">

            <b-form-group
                label="component.skos.ontologies-references-label"
                label-size="lg"
                label-class="font-weight-bold pt-0"
                class="mb-0"
            >
              <template v-slot:label>
                <b-row>
                  <b-col>
                    Selected entity
                  </b-col>
                  <b-col v-if="!!entity">
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

          </div>
        </div>

        <div class="row">

        </div>
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

@Component
export default class AgroportalEntityForm extends Vue {
    $opensilex: any;

    uriGenerated = true;
    text = "";
    ontologies: string[] = [];

    @Prop()
    editMode;

    errorMsg: String = "";

    @PropSync("form")
    entityDto: EntityCreationDTO;
    entity: EntityAgroportalDTO = null;

    externalOntologiesRefs: any[] = ExternalOntologies.getExternalOntologiesReferences(EntityCreate.selectedOntologies);

    handleErrorMessage(errorMsg: string) {
        this.errorMsg = errorMsg;
    }

    @Ref("validatorRef") readonly validatorRef!: any;

    created() {
      this.ontologies = this.$opensilex.getConfig().agroportal.entityOntologies;
    }

    onSearchTextChange(searchedText: string) {
      this.text = searchedText;
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
      for(var member in this.entityDto) {
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
