<template>
  <opensilex-FormSelector
    ref="formSelector"
    :label="label"
    :selected.sync="provenancesURI"
    :multiple="multiple"
    :optionsLoadingMethod="loadProvenances"
    :placeholder="
      multiple
        ? 'component.data.form.selector.placeholder-multiple'
        : 'component.data.form.selector.placeholder'
    "
    noResultsText="component.data.form.selector.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
    @keyup.enter.native="onEnter"
    :disableBranchNodes="true"
    :showCount="true"
    :actionHandler="actionHandler"
    :viewHandler="viewHandler"
    :required="required"
    :viewHandlerDetailsVisible="viewHandlerDetailsVisible"
  ></opensilex-FormSelector>
</template>

<script lang="ts">

import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";


@Component
export default class DataProvenanceSelector extends Vue {
  $opensilex: any;
  $i18n: any;

  @Prop()
  actionHandler: Function;

  @PropSync("provenances")
  provenancesURI;

  @Prop({
    default: false,
  })
  required;

  @Prop({
    default: "component.data.provenance.search"
  })
  label;

  @Prop()
  experiments;

  @Prop()
  targets

  @Prop()
  devices

  @Prop()
  variables

  @Prop({
    default: false,
  })
  multiple;

  @Prop()
  viewHandler: Function;

  @Prop({
    default: false,
  })
  viewHandlerDetailsVisible: boolean;

  select(value) {
    this.$emit("select", value);
  }

  deselect(value) {
    this.$emit("deselect", value);
  }

  onEnter() {
    this.$emit("handlingEnterKey")
  }

  loadProvenances() {

    return this.$opensilex
      .getService("opensilex.DataService")
      .getUsedProvenancesByTargets(this.experiments, this.variables, this.devices, this.targets)
      .then(http => {
        return http.response.result;
      });
  }

}
</script>

<style scoped lang="scss">
</style>