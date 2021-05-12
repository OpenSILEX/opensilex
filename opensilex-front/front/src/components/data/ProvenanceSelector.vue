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
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-security/HttpResponse";
// @ts-ignore
import { ProvenanceGetDTO } from "opensilex-core/index";

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

  filterLabel: string;

  @Prop()
  scientificObject;

  @Prop()
  device;

  refresh() {
    this.selectForm.refresh();
  }

  loadProvenances(provenancesURI) {

    if (provenancesURI == undefined || provenancesURI === ".*") {
      if (this.experiment == null && this.scientificObject == null && this.device== null) {
        return this.$opensilex
          .getService("opensilex.ExperimentsService")
          .searchProvenance()
          .then(
            (
              http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>
            ) => {
              return [http.response.result];
            }
          );
      } else if (this.scientificObject) {
        return this.$opensilex
          .getService("opensilex.ScientificObjectsService")
          .getScientificObjectDataProvenances(this.scientificObject)
          .then(
            (
              http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>
            ) => {
              return [http.response.result];
            }
          );
      } else if (this.device) {
        return this.$opensilex
          .getService("opensilex.DevicesService")
          .getDeviceDataProvenances(this.device)
          .then(
            (
              http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>
            ) => {
              return [http.response.result];
            }
          );
      } else {
        return this.$opensilex
          .getService("opensilex.DataService")
          .searchExperimentProvenances(this.experiment)
          .then(
            (
              http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>
            ) => {
              return [http.response.result];
            }
          );
      }
    } else {
      return this.$opensilex
        .getService("opensilex.DataService")
        .getProvenance(provenancesURI)
        .then(
          (http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>) => {
            return [http.response.result];
          }
        );
    }
  }

  searchProvenances(label, page, pageSize) {
    this.filterLabel = label;

    if (this.filterLabel === ".*") {
      this.filterLabel = undefined;
    }
    if (this.experiment == null && this.scientificObject == null && this.device== null) {
      return this.$opensilex
        .getService("opensilex.DataService")
        .searchProvenance(this.filterLabel)
        .then(
          (http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>) =>
            http
        );
    } else if (this.scientificObject) {
      return this.$opensilex
        .getService("opensilex.ScientificObjectsService")
        .getScientificObjectDataProvenances(this.scientificObject)
        .then(
          (http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>) =>
            http
        );
    } else if (this.device) {
      return this.$opensilex
        .getService("opensilex.DevicesService")
        .getDeviceDataProvenances(this.device)
        .then(
          (http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>) =>
            http
        );
    } else {
      return this.$opensilex
        .getService("opensilex.ExperimentsService")
        .searchExperimentProvenances(this.experiment, this.filterLabel)
        .then(
          (http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>) =>
            http
        );
    }
  }

  loadOptions(query, page, pageSize) {
    this.filterLabel = query;

    if (this.filterLabel === ".*") {
      this.filterLabel = undefined;
    }

    if (this.experiment == null && this.scientificObject == null && this.device== null) {
      return this.$opensilex
        .getService("opensilex.DataService")
        .searchProvenance(this.filterLabel)
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
    } else if (this.scientificObject) {
      return this.$opensilex
        .getService("opensilex.ScientificObjectsService")
        .getScientificObjectDataProvenances(this.scientificObject)
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
    } else if (this.device) {
      return this.$opensilex
        .getService("opensilex.DevicesService")
        .getDeviceDataProvenances(this.device)
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
        .getService("opensilex.ExperimentsService")
        .searchExperimentProvenances(this.experiment, this.filterLabel)
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

  getAllProvenances(label) {}

  getProvenancesInExperiment(label, experimentURI) {}

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
<i18n>

en:
  component: 
    data: 
        form: 
         selector:
            placeholder  : Select one provenance
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