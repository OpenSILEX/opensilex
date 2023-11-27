<template>
  <div>

    <!-- Search bar -->
    <b-input-group>
      <b-input
          type="text"
          v-model="searchBar"
          :placeholder="$t(placeholder)"
          v-on:change="$emit('change', searchBar)"
          @keyup.enter.native="$emit('change', searchBar)">
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
      <b-row align-v="center">
        <b-col sm="8">
          <opensilex-SelectForm
              ref="soSelector"
              label="Ontologies"
              :selected.sync="ontologiesURIs"
              :multiple="true"
              :searchMethod="searchOntologies"
              :itemLoadingMethod="loadOntologies"
              :conversionMethod="ontologyToSelectNode"
              :disabled="isAllOntologiesSelected"
              @select="select"
              @deselect="deselect"
              @keyup.enter.native="onEnter"
          >
          </opensilex-SelectForm>
        </b-col>
        <b-col sm="4">
          <b-form-checkbox
              v-model="isAllOntologiesSelected"
              value="accepted"
          >
            {{$t("AgroportalSearch.all-ontologies")}}
          </b-form-checkbox>
        </b-col>
      </b-row>
    </b-collapse>

  </div>
</template>


<script lang="ts">

import {Component, Prop, PropSync, Watch} from "vue-property-decorator";
import Vue from 'vue';
import OpenSilexVuePlugin from "../../../../models/OpenSilexVuePlugin";
import {VariablesService} from "opensilex-core/api/variables.service";
import {OntologyAgroportalDTO} from "opensilex-core/model/ontologyAgroportalDTO";
import {VariablesGroupGetDTO} from "opensilex-core/model/variablesGroupGetDTO";
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import {AgroportalAPIService} from "opensilex-core/api/agroportalAPI.service";

@Component
export default class AgroportalSearch extends Vue {

  $opensilex: OpenSilexVuePlugin;

  @PropSync("selected")
  ontologiesURIs;

  @PropSync("isAllOntologies", { default: false })
  isAllOntologiesSelected: boolean;

  @Prop({
    default: "AgroportalSearch.enter-search-text"
  })
  placeholder: string;

  searchBar: string = "";
  advancedSearchOpen: boolean = false;

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
      label: `${dto.acronym} (${dto.name})`
    };
  }

  setSearchTerm(searchQuery: string) {
    this.searchBar = searchQuery;
    this.$emit("change", searchQuery);
  }

  select(value) {
    this.$emit("select", value);
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

#advanced-options {
  padding: 10px;
}

</style>


<i18n>
en:
  AgroportalSearch:
    enter-search-text: Enter a name
    all-ontologies: All ontologies

fr:
  AgroportalSearch:
    enter-search-text: Entrer un nom
    all-ontologies: Toutes les ontologies

</i18n>