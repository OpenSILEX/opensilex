<template>
  <div>

    <!-- Search bar -->
    <b-input-group>
      <b-input
          type="text"
          v-model="searchBar"
          placeholder="search"
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
              :disabled="isAllOntologiesSelected"
              ref="soSelector"
              label="Ontologies"
              :selected.sync="ontologiesURIs"
              :multiple="true"
              :searchMethod="searchOntologies"
              :itemLoadingMethod="loadOntologies"
              :conversionMethod="ontologyToSelectNode"
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
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
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

  searchBar;
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
      label: dto.acronym
    };
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
    all-ontologies: Use all ontologies

fr:
  AgroportalSearch:
    all-ontologies: Dans toutes les ontologies

</i18n>