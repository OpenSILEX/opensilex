<template>
  <div>
    <opensilex-FormSelector
      ref="formSelector"
      :label="property.name"
      :selected.sync="internalValue"
      :multiple="property.is_list"
      :required="property.is_required"
      :optionsLoadingMethod="loadFacilities"
      placeholder="FacilityPropertySelector.placeholder"
    ></opensilex-FormSelector>
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
export default class FacilityPropertySelector extends Vue {
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
    } else if (this.context && this.context.organizationURI) {
      return this.$opensilex
        .getService("opensilex.OrganizationsService")
        .getOrganization(this.context.organizationURI)
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
      return this.$opensilex
        .getService("opensilex.OrganizationsService")
        .getAllFacilities()
        .then(http => {
          let facilityNodes = [];
          for (let facility of http.response.result) {
            facilityNodes.push({
              id: facility.uri,
              label: facility.name || facility.uri
            });
          }
          return facilityNodes;
        });
    }
  }
}
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
  FacilityPropertySelector:
    label: Facilities
    placeholder: Select a facility

fr:
  FacilityPropertySelector:
    label: Installation environnementale
    placeholder: Sélectionner une installation environnementale
</i18n>