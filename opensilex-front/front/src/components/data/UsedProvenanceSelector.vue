<template>
  <opensilex-SelectForm
    ref="selectForm"
    :label="label"
    :selected.sync="provenancesURI"
    :multiple="multiple"
    :optionsLoadingMethod="loadProvenances"
    :conversionMethod="provenancesToSelectNode"
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
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
import { ProvenanceGetDTO } from "opensilex-core/index";

@Component
export default class UsedProvenanceSelector extends Vue {
  $opensilex: any;
  $i18n: any;

  @Ref("selectForm") readonly selectForm!: any;

  @Prop()
  actionHandler: Function;

  @PropSync("provenances")
  provenancesURI;

  @Prop({
    default: "component.data.provenance.search"
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
  device;

  refresh() {
    this.selectForm.refresh();
  }

  loadProvenances() {
    let experiments = null;    
    if (this.experiment != null) {
      experiments = [this.experiment]
    }

    let objects = null;
    if (this.scientificObject != null) {
      objects = [this.scientificObject]
    }

    let devices = null;
    if (this.device != null) {
      devices = [this.device]
    }

    return this.$opensilex
      .getService("opensilex.DataService")
      .getUsedProvenances(experiments, objects, null, devices)
      .then(http => {
        return http.response.result;
      });
  }

  provenancesToSelectNode(dto: ProvenanceGetDTO) {
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