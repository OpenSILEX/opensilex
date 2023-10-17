<template>
  <div>

    <!-- Search bar -->
    <b-input-group>
      <b-input
          type="text"
          v-model="searchBar"
          placeholder="search"
          v-on:change="$emit('change', searchBar)">
      </b-input>
      <template #append>
        <opensilex-Button
            icon="ik#ik-search"
            :small="true"
            variant="outline-secondary"
            @click="$emit('change', searchBar)"
        >
        </opensilex-Button>
        <opensilex-Button
            v-b-toggle.advanced-options
            icon="ik#ik-filter"
            :small="true"
            variant="outline-secondary"
        >
        </opensilex-Button>
      </template>
    </b-input-group>

    <!-- Advanced options -->
    <b-collapse id="advanced-options">
      <opensilex-SelectForm
          ref="soSelector"
          label="Ontologies"
          :selected.sync="ontologiesURIs"
          :multiple="true"
          :searchMethod="searchOntologies"
          :itemLoadingMethod="loadOntologies"
          :conversionMethod="ontologyToSelectNode"
          @reset="reset"
          @select="select"
          @deselect="deselect"
          @keyup.enter.native="onEnter"
      >
      </opensilex-SelectForm>
    </b-collapse>

  </div>
</template>


<script lang="ts">

import {Component, Prop, PropSync} from "vue-property-decorator";
import Vue from 'vue';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {VariablesService} from "opensilex-core/api/variables.service";
import {OntologyAgroportalDTO} from "opensilex-core/model/ontologyAgroportalDTO";
import {VariablesGroupGetDTO} from "opensilex-core/model/variablesGroupGetDTO";
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import {AgroportalAPIService} from "opensilex-core/api/agroportalAPI.service";

@Component
export default class AgroportalSearch extends Vue {

  $opensilex: OpenSilexVuePlugin;

  searchBar;
  advancedSearchOpen: boolean = false;

  @PropSync("selected")
  ontologiesURIs;

  created() {
    this.searchBar = "";
  }

  loadOntologies(ontologieAcronyms): Promise<Array<OntologyAgroportalDTO>> {
    return this.$opensilex
        .getService<AgroportalAPIService>("opensilex.AgroportalAPIService")
        .getAgroportalOntologies("", ontologieAcronyms, undefined, 0, 0)
        .then((http: HttpResponse<OpenSilexResponse<Array<OntologyAgroportalDTO>>>) => {
          return http.response.result;
        })
        .catch(this.$opensilex.errorHandler);
  }

  searchOntologies(searchQuery, page, pageSize) {
    console.debug(this.ontologiesURIs);
    return this.$opensilex
        .getService<AgroportalAPIService>("opensilex.AgroportalAPIService")
        .getAgroportalOntologies(searchQuery, undefined, undefined, 0, 0)
        .then((http: HttpResponse<OpenSilexResponse<Array<OntologyAgroportalDTO>>>) => {
          return http;
        })
        .catch(this.$opensilex.errorHandler);
  }

  ontologyToSelectNode(dto: OntologyAgroportalDTO) {
    return {
      id: dto.acronym,
      label: dto.acronym
    };
  }

  reset() {
    console.debug("selected" + this.ontologiesURIs);
  }

  select(value) {
    this.$emit("select", value);
    console.debug(value);
  }

  deselect(value) {
    this.$emit("deselect", value);
  }

  onEnter() {
    this.$emit("handlingEnterKey");
  }

}
</script>


<style scoped>

</style>


<i18n>

</i18n>