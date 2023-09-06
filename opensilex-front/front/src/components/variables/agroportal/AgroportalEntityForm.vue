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
                      @import="importResult">
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
              <template v-slot:label>Selected entity</template>
            </b-form-group>

            <opensilex-AgroportalResultItem
                v-if="entity != null"
                :entity="entity"
            >
            </opensilex-AgroportalResultItem>

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
import AgroportalEntityCreate from "./AgroportalEntityCreate.vue";
    import {EntityAgroportalDTO} from "opensilex-core/model/entityAgroportalDTO";

@Component
export default class AgroportalEntityForm extends Vue {
    $opensilex: any;

    uriGenerated = true;
    text = "";

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

    onSearchTextChange(searchedText: string) {
      this.text = searchedText;
    }

    importResult(entity: EntityAgroportalDTO) {
      this.entity = entity;
      this.entityDto.uri = entity.id;
      this.entityDto.name = entity.name;
      this.entityDto.description = entity.definitions[0];
      this.entityDto.exact_match = [];
      this.entityDto.narrow_match = [];
      this.entityDto.broad_match = [];
      this.entityDto.close_match = [];
    }

    reset() {
        this.uriGenerated = true;
        return this.validatorRef.reset();
    }

    validate() {
        return this.validatorRef.validate();
    }
}
</script>

<style scoped lang="scss">
    a {color: #007bff;}
</style>
