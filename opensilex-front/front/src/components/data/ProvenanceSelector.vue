<template>
  <opensilex-SelectForm
    ref="selectForm"
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
import Vue, { PropOptions } from "vue";
import { SecurityService, UserGetDTO } from "opensilex-security/index";
import HttpResponse, {
  OpenSilexResponse,
} from "opensilex-security/HttpResponse";
import { DataService, ProvenanceGetDTO } from "opensilex-core/index";

import { ASYNC_SEARCH } from "@riophae/vue-treeselect";

@Component
export default class ProvenanceSelector extends Vue {
  $opensilex: any;
  $i18n: any;

  @Ref("selectForm") readonly selectForm!: any;

  @Prop()
  actionHandler: Function;

  @PropSync("provenances")
  provenancesURI;

  @Prop({
    default: "component.data.provenance.search",
  })
  label;

  @Prop({
    default: false,
  })
  required;

  @Prop({
    default: undefined,
  })
  experiment;

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

  filterLabel: string;

  refresh() {
    this.selectForm.refresh();
  }

  loadProvenances(provenancesURI) {
    console.debug("provenancesURI loaded", provenancesURI);

    if (provenancesURI == undefined || provenancesURI === ".*") {
      return this.$opensilex
        .getService("opensilex.DataService")
        .searchProvenance(undefined, undefined, this.experiment)
        .then(
          (http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>) => {
            return [http.response.result];
          }
        );
    } else {
      return this.$opensilex
        .getService("opensilex.DataService")
        .getProvenance(provenancesURI, this.experiment)
        .then(
          (http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>) => {
            return [http.response.result];
          }
        );
    }
  }

  searchProvenances(label, page, pageSize) {
    this.filterLabel = label;
    if (label == undefined || label === ".*") {
      return this.$opensilex
        .getService("opensilex.DataService")
        .searchProvenance(undefined, undefined, this.experiment)
        .then(
          (http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>) =>
            http
        );
    } else {
      return this.$opensilex
        .getService("opensilex.DataService")
        .searchProvenance(label, undefined, this.experiment)
        .then(
          (http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>) =>
            http
        );
    }
  }

  loadOptions(query, page, pageSize) {
    this.filterLabel = query;

    if (this.filterLabel === ".*") {
      return this.$opensilex
        .getService("opensilex.DataService")
        .searchProvenance(undefined, undefined, this.experiment)
        .then(
          (http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>) =>
            http.response.result
        )
        .then((result: any[]) => {
          let nodeList = [];
          for (let prov of result) {
            nodeList.push(this.provenancesToSelectNode(prov));
          }
          return nodeList;
        });
    } else {
      return this.$opensilex
        .getService("opensilex.DataService")
        .searchProvenance(this.filterLabel, undefined, this.experiment)
        .then(
          (http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>) =>
            http.response.result
        )
        .then((result: any[]) => {
          let nodeList = [];

          for (let prov of result) {
            nodeList.push(this.provenancesToSelectNode(prov));
          }
          return nodeList;
        });
    }
  }

  provenancesToSelectNode(dto: ProvenanceGetDTO) {
    return {
      id: dto.uri,
      label: dto.name + " (" + dto.uri + ")",
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
    data: 
        form: 
         selector:
            placeholder  : Select one data description
            placeholder-multiple  : Select one or more provenance(s)
            filter-search-no-result : No provenance found
    
            
fr:
  component: 
    data: 
        form:
          selector:
            placeholder : Selectionner une description de données
            placeholder-multiple : Selectionner un ou plusieurs provenance(s)   
            filter-search-no-result : Aucune provenance trouvée

</i18n>