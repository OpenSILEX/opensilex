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
    </ValidationObserver>
</template>

<script lang="ts">

import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import {EntityCreationDTO} from "opensilex-core/index";
import EntityCreate from "./AgroportalEntityCreate.vue";
import AgroportalEntityCreate from "./AgroportalEntityCreate.vue";
import {EntityAgroportalDTO} from "opensilex-core/model/entityAgroportalDTO";

@Component
export default class AgroportalEntityExternalReferencesForm extends Vue {

    selectedOntologies: string[] = EntityCreate.selectedOntologies;

    @PropSync("form")
    entityDto: EntityAgroportalDTO;

    text = "";


    onSearchTextChange(searchedText: string) {
      this.text = searchedText;
    }

    onImportMapping(entity: EntityAgroportalDTO, relation) {

    }

}
</script>

<style scoped>

</style>