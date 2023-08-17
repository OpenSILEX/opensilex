<template>
    <ValidationObserver ref="entityValidatorRef">
      <div class="row">
        <opensilex-AgroportalSearch
            label="component.common.name"
            type="text"
            placeholder="search"
            @change="onSearchTextChange"
        ></opensilex-AgroportalSearch>

        <opensilex-AgroportalResults
            ref="searchResults"
            :text.sync="text"
            :isMappingMode="true"
            @importMapping="onImportMapping">
        </opensilex-AgroportalResults>
      </div>

      <div class="row">
        <opensilex-ExternalReferencesForm
          :references.sync="form"
          :displayInsertButton="false"
          :ontologiesToSelect="selectedOntologies">
        </opensilex-ExternalReferencesForm>
      </div>
    </ValidationObserver>
</template>

<script lang="ts">

import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import {EntityCreationDTO} from "opensilex-core/index";
import {EntityAgroportalDTO} from "opensilex-core/model/entityAgroportalDTO";
import AgroportalEntityCreate from "./AgroportalEntityCreate.vue";


@Component
export default class AgroportalEntityExternalReferencesForm extends Vue {

    selectedOntologies: string[] = AgroportalEntityCreate.selectedOntologies;

    @PropSync("form")
    entityDto: EntityCreationDTO;

    text = "";


    onSearchTextChange(searchedText: string) {
      this.text = searchedText;
    }

    onImportMapping(entity: EntityAgroportalDTO, relation) {
      if(relation == "exact-match") {
        this.entityDto.exact_match.push(entity.id);
      }
    }

}
</script>

<style scoped>

</style>