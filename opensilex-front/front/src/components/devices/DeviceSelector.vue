<template>
  <opensilex-SelectForm
    ref="selectForm"
    :label="label"
    :selected.sync="devicesURIs"
    :multiple="multiple"
    :searchMethod="searchDevices"
    :itemLoadingMethod="loadDevices"
    :conversionMethod="devicesToSelectNode"
    :placeholder="
      multiple
        ? 'component.device.selector.placeholder-multiple'
        : 'component.device.selector.placeholder'
    "
    noResultsText="component.device.selector.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
    :disableBranchNodes="true"
    :showCount="true"
    :required="required"
    :disabled="disabled"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue, { PropOptions } from "vue";
import HttpResponse, {
  OpenSilexResponse
} from "opensilex-security/HttpResponse";
import {
  DeviceGetDTO
} from "opensilex-core/index";

@Component
export default class DeviceSelector extends Vue {
  $opensilex: any;
  $i18n: any;

  @Ref("selectForm") readonly selectForm!: any;

  @Prop()
  actionHandler: Function;

  @PropSync("devices")
  devicesURIs;

  @Prop({
    default: "component.data.provenance.search"
  })
  label;

  @Prop({
    default: false
  })
  required;

  @Prop({
    default: false
  })
  multiple;

  @Prop({
    default: false
  })
  disabled;

  @Prop({
    default: true
  })
  showURI;

  filterLabel: string;

  refresh() {
    this.selectForm.refresh();
  }

  loadDevices(devicesURIs) {
    if (devicesURIs == undefined || devicesURIs === ".*") {
      return this.$opensilex
        .getService("opensilex.DevicesService")
        .searchDevices()
        .then(
          (
            http: HttpResponse<OpenSilexResponse<Array<DeviceGetDTO>>>
          ) => {
            return [http.response.result];
          }
        );
    } else {
      return this.$opensilex
        .getService("opensilex.DevicesService")
        .getDevice(devicesURIs)
        .then(
          (http: HttpResponse<OpenSilexResponse<Array<DeviceGetDTO>>>) => {
            return [http.response.result];
          }
        );
    }
  }

  searchDevices(label, page, pageSize) {
    this.filterLabel = label;
    if (this.filterLabel === ".*") {
      this.filterLabel = undefined;
    }

    return this.$opensilex
      .getService("opensilex.DevicesService")
      .searchDevices(this.filterLabel)
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<DeviceGetDTO>>>) =>
          http
      );
    
  }

  loadOptions(query, page, pageSize) {
    this.filterLabel = query;

    if (this.filterLabel === ".*") {
      this.filterLabel = undefined;
    }

    return this.$opensilex
      .getService("opensilex.DevicesService")
      .searchDevices(this.filterLabel)
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<DeviceGetDTO>>>) =>
          http.response.result
      )
      .then((result: any[]) => {
        let nodeList = [];

        for (let prov of result) {
          nodeList.push(this.devicesToSelectNode(prov));
        }
        return nodeList;
      });
  }

  devicesToSelectNode(dto: DeviceGetDTO) {
    return {
      id: dto.uri,
      label: this.showURI ? dto.name + " (" + dto.uri + ")" : dto.name
    };
  }
  select(value) {
    this.$emit("select", value);
  }

  deselect(value) {
    this.$emit("deselect", value);
  }
}
</script>

<style scoped lang="scss">
</style>
<i18n>

en:
  component: 
    device: 
      selector:
        placeholder  : Select one device
        placeholder-multiple  : Select one or more device(s)
        filter-search-no-result : No device found
fr:
  component: 
    device: 
      selector:
        placeholder : Sélectionner un dispositif
        placeholder-multiple : Sélectionner un ou plusieurs dispositif(s)   
        filter-search-no-result : Aucun dispositif trouvé

</i18n>