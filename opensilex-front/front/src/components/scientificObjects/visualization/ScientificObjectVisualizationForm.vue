<template>
  <div>
    <div class="card">
      <opensilex-SearchFilterField :withButton="false" :showAdvancedSearch="true">
        <template v-slot:filters>
          <!-- Type -->
          <opensilex-FilterField :halfWidth="true">
            <opensilex-VariableSelector
              label="ScientificObjectVisualizationForm.variable.label"
              :variables.sync="filter.variable"
              :multiple="false"
              :required="true"
              :scientificObjects="scientificObject"
              :clearable="true"
              :defaultSelectedValue="true"
              @select="onSearch"
            ></opensilex-VariableSelector>
          </opensilex-FilterField>
          <opensilex-FilterField :halfWidth="true">
            <div class="row">
              <div class="col col-xl-6 col-md-6 col-sm-6 col-12">
                <opensilex-DateTimeForm
                  :value.sync="filter.startDate"
                  label="component.common.begin"
                  @input="onUpdate"
                  @clear="onUpdate"
                ></opensilex-DateTimeForm>
              </div>
              <div class="col col-xl-6 col-md-6 col-sm-6 col-12">
                <opensilex-DateTimeForm
                  :value.sync="filter.endDate"
                  label="component.common.end"
                  @input="onUpdate"
                  @clear="onUpdate"
                ></opensilex-DateTimeForm>
              </div>
            </div>
          </opensilex-FilterField>
        </template>

        <template v-slot:advancedSearch>
          <opensilex-FilterField :halfWidth="true">
            <opensilex-ProvenanceSelector
              ref="provSelector"
              :provenances.sync="filter.provenance"
              :filterLabel="filterProvenanceLabel"
              :scientificObject="scientificObject"
              label="Provenance"
              @select="loadProvenance"
              @clear="clearProvenance"
              :multiple="false"
              :viewHandler="showProvenanceDetails"
              :viewHandlerDetailsVisible="visibleDetails"
              :showURI="false"
            ></opensilex-ProvenanceSelector>
          </opensilex-FilterField>

          <!-- <opensilex-FilterField :halfWidth="true">
            <div class="row">
              <div class="col col-xl-6 col-md-6 col-sm-6 col-12">
                <label for="metadataKey">{{ $t("DataVisuForm.search.metadataKey") }}</label>
                <opensilex-StringFilter
                  id="metadataKey"
                  :filter.sync="filter.metadataKey"
                  @update="onUpdate"
                ></opensilex-StringFilter>
              </div>
              <div class="col col-xl-6 col-md-6 col-sm-6 col-12">
                <label for="metadataValue">{{ $t("DataVisuForm.search.metadataValue") }}</label>
                <opensilex-StringFilter
                  id="metadataValue"
                  :filter.sync="filter.metadataValue"
                  @update="onUpdate"
                ></opensilex-StringFilter>
              </div>
            </div>
          </opensilex-FilterField> -->

          <opensilex-FilterField>
            <b-collapse
              v-if="selectedProvenance"
              id="collapse-4"
              v-model="visibleDetails"
              class="mt-2"
            >
              <opensilex-ProvenanceDetails :provenance="getSelectedProv"></opensilex-ProvenanceDetails>
            </b-collapse>
          </opensilex-FilterField>
        </template>
      </opensilex-SearchFilterField>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueConstructor from "vue";
import { ProvenanceGetDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class ScientificObjectVisualizationForm extends Vue {
  $opensilex: any;

  filterProvenanceLabel: string = null;
  selectedProvenance: any = null;
  visibleDetails: boolean = false;
  filter = {
    variable: null,
    startDate: undefined,
    endDate: undefined,
    provenance: undefined,
    // metadataKey: undefined,
    // metadataValue: undefined
  };

  resetFilters() {
    this.filter.variable = null;
    this.filter.startDate = undefined;
    this.filter.endDate = undefined;
    this.filter.provenance = undefined;
    // this.filter.metadataKey = undefined;
    // this.filter.metadataValue = undefined;

    this.filterProvenanceLabel = null;
  }

  @Prop()
  scientificObject;

  onUpdate() {
    this.$emit("update", this.filter);
  }

  onSearch() {
    this.$emit("search", this.filter);
  }

  getProvenance(uri) {
    if (uri != undefined && uri != null) {
      return this.$opensilex
        .getService("opensilex.DataService")
        .getProvenance(uri)
        .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
          return http.response.result;
        });
    }
  }

  loadProvenance(selectedValue) {
    if (selectedValue != undefined && selectedValue != null) {
      this.getProvenance(selectedValue.id).then(prov => {
        this.selectedProvenance = prov;
        this.onUpdate();
      });
    }
  }

  clearProvenance() {
    this.filterProvenanceLabel = null;
    this.onUpdate();
  }

  showProvenanceDetails() {
    if (this.selectedProvenance != null) {
      this.visibleDetails = !this.visibleDetails;
    }
  }

  get getSelectedProv() {
    return this.selectedProvenance;
  }
}
</script>

<style scoped lang="scss">
.card-vertical-group {
  margin-bottom: 0px;
}
</style>

<i18n>
en:
  ScientificObjectVisualizationForm:
    variable:
      label: Variable 
      placeholder: Search for a variable
fr:
  ScientificObjectVisualizationForm:
    variable:
      label: Variable 
      placeholder: Saisir une variable

</i18n>