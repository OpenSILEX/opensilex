<template>
  <div>
    <opensilex-SelectForm
      ref="selectForm"
      label="InfrastructureFacilityPropertySelector.label"
      :selected.sync="internalValue"
      :multiple="property.isList"
      :required="property.isRequired"
      :optionsLoadingMethod="loadFacilities"
      placeholder="InfrastructureFacilityPropertySelector.placeholder"
    ></opensilex-SelectForm>
  </div>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  Model,
  Provide,
  PropSync
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class InfrastructureFacilityPropertySelector extends Vue {
  $opensilex: any;

  @Prop()
  property;

  @PropSync("value")
  internalValue;

  @Prop()
  context;

  loadFacilities() {
    if (this.context && this.context.experimentURI) {
      return this.$opensilex
        .getService("opensilex.ExperimentsService")
        .getAvailableFacilities(this.context.experimentURI)
        .then(http => {
          let facilityNodes = [];
          for (let i in http.response.result) {
            let facility = http.response.result[i];
            facilityNodes.push({
              id: facility.uri,
              label: facility.name
            });
          }
          return facilityNodes;
        });
    } else if (this.context && this.context.infrastructureURI) {
      return this.$opensilex
        .getService("opensilex.InfrastructuresService")
        .getInfrastructure(this.context.infrastructureURI)
        .then(http => {
          let facilityNodes = [];
          for (let facility of http.response.result.facilities) {
            facilityNodes.push({
              id: facility.uri,
              label: facility.name
            });
          }
          return facilityNodes;
        });
    } else {
      return Promise.resolve([]);
    }
  }
}
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
  InfrastructureFacilityPropertySelector:
    label: Facilities
    placeholder: Please select a facility

fr:
  InfrastructureFacilityPropertySelector:
    label: Installation technique
    placeholder: Sélectionner une installation technique
</i18n>