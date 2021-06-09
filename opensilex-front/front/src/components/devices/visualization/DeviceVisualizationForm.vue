<template>
  <div>
    <div class="card">
      <opensilex-SearchFilterField    :withButton="true"
         :showTitle="true"
          @search="onSearch"
          @clear="clear" :showAdvancedSearch="true">
        <template v-slot:filters>
          <!-- Type -->
          <opensilex-FilterField :halfWidth="true">
            <opensilex-DeviceVariableSelector
              label="DeviceVisualizationForm.variable.label"
              :variables.sync="filter.variable"
              :multiple="false"
              :device="device"
              :clearable="true"
              :required="true"
            ></opensilex-DeviceVariableSelector>
          </opensilex-FilterField>
          <opensilex-FilterField :halfWidth="true">
            <div class="row">
              <div class="col col-xl-6 col-md-6 col-sm-6 col-12">
                <opensilex-DateTimeForm
                  :value.sync="filter.startDate"
                  label="component.common.begin"
                  name="startDate"
                ></opensilex-DateTimeForm>
              </div>
              <div class="col col-xl-6 col-md-6 col-sm-6 col-12">
                <opensilex-DateTimeForm
                  :value.sync="filter.endDate"
                  label="component.common.end"
                  name="endDate"
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
              label="Provenance"
              :multiple="false"
              :device="device"
              :viewHandler="showProvenanceDetails"
              :viewHandlerDetailsVisible="visibleDetails"
              :showURI="false" 
              @select="loadProvenance"
              @clear="clearProvenance"

            ></opensilex-ProvenanceSelector>
          </opensilex-FilterField>

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
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { ProvenanceGetDTO } from "opensilex-core/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class DeviceVisualizationForm extends Vue {
  $opensilex: any;

  filterProvenanceLabel: string = null;
  selectedProvenance: any = null;
  visibleDetails: boolean = false;
  filter = {
    variable: null,
    startDate: undefined,
    endDate: undefined,
    provenance: undefined
  };

  resetFilters() {
    this.filter.variable = null;
    this.filter.startDate = undefined;
    this.filter.endDate = undefined;
    this.filter.provenance = undefined;

    this.filterProvenanceLabel = null;
  }

  @Prop()
  device;

  clear() {
    this.resetFilters();
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
      });
    }
  }

  clearProvenance() {
    this.filterProvenanceLabel = null;
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
  DeviceVisualizationForm:
    variable:
      label: Variable 
      placeholder: Search for a variable
fr:
  DeviceVisualizationForm:
    variable:
      label: Variable 
      placeholder: Saisir une variable

</i18n>
