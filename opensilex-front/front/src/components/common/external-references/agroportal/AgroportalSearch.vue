<template>
  <div>
    <!-- Search bar -->
    <b-input-group>
      <b-input
          type="text"
          v-model="searchText"
          :placeholder="$t(placeholder)"
          @keyup.enter.native="emitChange"
          @input="onInputValueChange">
      </b-input>
      <template #append>
        <opensilex-Button
            icon="ik#ik-x"
            :small="true"
            variant="outline-secondary"
            class="agroportalCleanSearchBtn"
            @click="cleanSearchField"
        >
        </opensilex-Button>

        <opensilex-Button
            v-b-toggle.advanced-options
            icon="ik#ik-filter"
            :small="true"
            variant="outline-secondary"
            class="agroportalSearchBarBtn"
        >
        </opensilex-Button>

        <opensilex-Button
            icon="ik#ik-search"
            :small="true"
            variant="outline-secondary"
            class="agroportalSearchBarBtn"
            @click="emitChange"
        >
        </opensilex-Button>
      </template>
    </b-input-group>

    <!-- Advanced options -->
    <b-collapse id="advanced-options">
      <b-row align-v="center">
        <b-col sm="8">
          <opensilex-FormSelector
              ref="soSelector"
              label="Ontologies"
              :selected.sync="ontologiesURIs"
              :multiple="true"
              :searchMethod="searchOntologies"
              :itemLoadingMethod="loadOntologies"
              :conversionMethod="ontologyToSelectNode"
              :disabled="isAllOntologiesSelected"
          >
          </opensilex-FormSelector>
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

import {Component, Prop, PropSync} from "vue-property-decorator";
import Vue from 'vue';
import OpenSilexVuePlugin from "../../../../models/OpenSilexVuePlugin";
import {OntologyAgroportalDTO} from "opensilex-core/model/ontologyAgroportalDTO";
import HttpResponse, {OpenSilexResponse} from "../../../../lib/HttpResponse";
import {AgroportalAPIService} from "opensilex-core/api/agroportalAPI.service";
import {SelectableItem} from "../../forms/FormSelector.vue";

@Component
export default class AgroportalSearch extends Vue {
  //#region Plugin
  private readonly $opensilex: OpenSilexVuePlugin;
  //#endregion

  //#region Props
  @Prop({
    default: "AgroportalSearch.enter-search-text"
  })
  private readonly placeholder: string;

  @PropSync("selected")
  private ontologiesURIs;

  @PropSync("isAllOntologies", { default: false })
  private isAllOntologiesSelected: boolean;
  //#endregion

  //#region Data
  private searchText: string = "";
  private isAgroportalReachable: boolean = true;
  //#endregion

  //#region Private methods
  private loadOntologies(ontologieAcronyms: Array<string>): Promise<Array<OntologyAgroportalDTO>> {
    return this.$opensilex
        .getService<AgroportalAPIService>("opensilex.AgroportalAPIService")
        .getAgroportalOntologies("", ontologieAcronyms)
        .then((http: HttpResponse<OpenSilexResponse<Array<OntologyAgroportalDTO>>>) => {
          return http.response.result;
        })
        .catch(this.agroportalErrorHandler);
  }

  private searchOntologies(searchQuery, _page, _pageSize):
      Promise<HttpResponse<OpenSilexResponse<Array<OntologyAgroportalDTO>>>> {
    return this.$opensilex
        .getService<AgroportalAPIService>("opensilex.AgroportalAPIService")
        .getAgroportalOntologies(searchQuery, undefined)
        .then((http: HttpResponse<OpenSilexResponse<Array<OntologyAgroportalDTO>>>) => {
          return http;
        });
  }

  private ontologyToSelectNode(dto: OntologyAgroportalDTO): SelectableItem {
    return {
      id: dto.acronym,
      label: `${dto.acronym} (${dto.name})`
    };
  }

  private agroportalErrorHandler(error: HttpResponse): Array<OntologyAgroportalDTO> {
    if (error.status === 503) {
      return [];
    }
    return this.$opensilex.errorHandler(error);
  }
  //#endregion

  //#region Public methods
  public setSearchText(text: string) {
    this.searchText = text;
    this.emitChange();
  }
  //#endregion

  //#region Events
  private emitChange() {
    if (this.searchText !== "") {
      this.$emit("change", this.searchText);
    }
  }

  private cleanSearchField(){
    this.searchText = "";
  }

  private onInputValueChange(text: string){
    this.searchText = text
    if (this.searchText !== "") {
      this.$emit("inputValueHasChanged", this.searchText);
    }
  }
  //#endregion
}
</script>

<style scoped>
#advanced-options {
  padding: 10px;
}

.agroportalSearchBarBtn, .agroportalCleanSearchBtn {
  color: #00A38D;
  border-color: #00A38D;
}

.agroportalSearchBarBtn:hover {
  background: #00A38D;
  color: white;
}

.agroportalCleanSearchBtn:hover {
  color: white;
  background: red;
  border-color: red;
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