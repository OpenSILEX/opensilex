<template>
  <opensilex-SelectForm
      ref="selectForm"
      :label="label"
      :selected.sync="devicesURI"
      :multiple="multiple"
      :optionsLoadingMethod="loadDevices"
      :conversionMethod="devicesToSelectNode"
      :placeholder="
      multiple
        ? 'component.data.form.selector.placeholder-multiple'
        : 'component.data.form.selector.placeholder'
    "
      noResultsText="component.data.form.selector.filter-search-no-result"
      @clear="$emit('clear')"
      @select="select"
      @deselect="deselect"
      :disableBranchNodes="true"
      :showCount="true"
      :actionHandler="actionHandler"
      :viewHandler="viewHandler"
      :required="required"
      :viewHandlerDetailsVisible="viewHandlerDetailsVisible"
      :defaultSelectedValue="true"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
import {DeviceGetDTO} from "opensilex-core/index";
import {DevicesService} from "opensilex-core/index";

@Component
export default class VariableDevicesSelector extends Vue {
  $opensilex: any;
  $i18n: any;
  $service: DevicesService;

  @Ref("selectForm") readonly selectForm!: any;

  @Prop()
  actionHandler: Function;

  @PropSync("devices")
  devicesURI;


  @Prop({
    default: "component.data.device.search"
  })
  label;

  @Prop({
    default: false
  })
  required;

  @Prop({
    default: undefined
  })
  experiment;

  @Prop({
    default: false
  })
  multiple;

  @Prop({
    default: true
  })
  showURI;

  @Prop()
  viewHandler: Function;

  @Prop({
    default: false
  })
  viewHandlerDetailsVisible: boolean;

  @Prop()
  scientificObject;


  @Prop()
  variable;


  loadDevices() {
    return this.$opensilex
        .getService("opensilex.DevicesService")
        .searchDevices(
            undefined,
            false,
            undefined,
            this.variable,
            undefined,
            undefined,
            undefined,
            undefined,
            undefined,
            undefined,
            ["name=asc"],
            0,
            200
        )
        .then(http => {
          return http.response.result;
        }).catch(this.$opensilex.errorHandler);
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