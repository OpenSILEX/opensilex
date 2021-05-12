<template>
  <opensilex-SelectForm
    ref="selectForm"
    :label="label"
    :selected.sync="provenancesURI"
    :multiple="multiple"
    :optionsLoadingMethod="getProvenances"
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
// @ts-ignore
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
// @ts-ignore
import { ProvenanceGetDTO } from "opensilex-core/index";


@Component
export default class DatafileProvenanceSelector extends Vue {
  $opensilex: any;
  $i18n: any;

  @Ref("selectForm") readonly selectForm!: any;

  @Prop()
  actionHandler: Function;

  @PropSync("provenances")
  provenancesURI;

  @Prop({
    default: false,
  })
  required;

  @Prop()
  scientificObject

  @Prop()
  device

  @Prop({
    default: false,
  })
  multiple;

  @Prop({
    default: true,
  })
  showURI;

  @Prop()
  viewHandler: Function;

  @Prop({
    default: false,
  })
  viewHandlerDetailsVisible: boolean;

  refresh() {
    this.selectForm.refresh();
  }

  provenancesToSelectNode(dto: ProvenanceGetDTO) {
    return {
      id: dto.uri,
      label: dto.name,
    };
  }
  select(value) {
    this.$emit("select", value);
  }

  deselect(value) {
    this.$emit("deselect", value);
  }

  getProvenances() {
    if (this.scientificObject) {
      return this.$opensilex
      .getService("opensilex.ScientificObjectsService")
      .getScientificObjectDataFilesProvenances(this.scientificObject)
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>) =>
          http.response.result
      );

    } else if (this.device) {
      return this.$opensilex
      .getService("opensilex.DevicesService")
      .getDeviceDataFilesProvenances(this.device)
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>) =>
          http.response.result
      );
    }
    
  }

}
</script>

<style scoped lang="scss">
</style>