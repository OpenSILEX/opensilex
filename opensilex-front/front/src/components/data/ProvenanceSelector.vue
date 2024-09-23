<template>
  <opensilex-FormSelector
    ref="formSelector"
    :label="label"
    :selected.sync="provenancesURI"
    :multiple="multiple"
    :searchMethod="searchProvenances"
    :itemLoadingMethod="loadProvenances"
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
    @loadMoreItems="loadMoreItems"
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
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import {ProvenanceGetDTO} from "opensilex-core/index";
import FormSelector from "../common/forms/FormSelector.vue";


@Component
export default class ProvenanceSelector extends Vue {
  $opensilex: any;
  $i18n: any;
  pageSize = 10;

  @Ref("formSelector") readonly formSelector!: FormSelector;

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


  @Prop()
  viewHandler: Function;

  @Prop({
    default: false
  })
  viewHandlerDetailsVisible: boolean;

  filterLabel: string;

  @Prop()
  scientificObject;

  @Prop()
  device;

  refresh() {
    this.formSelector.refresh();
  }

  loadProvenances(provenancesURI) {
    return this.$opensilex
      .getService("opensilex.DataService")
      .getProvenancesByURIs(provenancesURI)
      .then(
      (http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>) =>
        http.response.result
    );
  }

  searchProvenances(label, page, pageSize) {
    this.filterLabel = label;

    if (this.filterLabel === ".*") {
      this.filterLabel = undefined;
    }
    
    return this.$opensilex
      .getService("opensilex.DataService")
      .searchProvenance(this.filterLabel, undefined, undefined, undefined, undefined, undefined, undefined, undefined, this.pageSize)
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>) =>
          http
          // trouver la bone place pour cibler pageSize et le remplacer par this.pageSize 
      );
  }

  getAllProvenances(label) {}

  getProvenancesInExperiment(label, experimentURI) {}

  provenancesToSelectNode(dto: ProvenanceGetDTO) {
    return {
      id: dto.uri,
      label: dto.name
    };
  }
  select(value) {
    this.$emit("select", value);
  }

  deselect(value) {
    this.$emit("deselect", value);
  }

  loadMoreItems(){
    this.pageSize = 0;
    this.formSelector.refresh();
    this.$nextTick(() => {
      this.formSelector.openTreeselect();
    })
  }
}
</script>

<style scoped lang="scss">
</style>
<i18n>

en:
  component: 
    data: 
        form: 
          selector:
            placeholder  : Select a provenance
            placeholder-multiple  : Select one or more provenance(s)
            filter-search-no-result : No provenance found
fr:
  component: 
    data: 
        form:
          selector:
            placeholder : Sélectionner une provenance
            placeholder-multiple : Sélectionner une ou plusieurs provenance(s)   
            filter-search-no-result : Aucune provenance trouvée

</i18n>