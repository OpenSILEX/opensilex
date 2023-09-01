<template>
  <opensilex-SelectForm
      ref="selectForm"
      label="OperatorSelector.label"
      :selected.sync="operatorURI"
      :multiple="false"
      :searchMethod="searchOperators"
      :itemLoadingMethod="load"
      :conversionMethod="operatorToSelectNode"
      :clearable="false"
      placeholder="OperatorSelector.placeholder"
      :required="required"
      :defaultSelectedValue="defaultSelectedValue"
      @clear="$emit('clear')"
      @select="select"
      @deselect="deselect"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {NamedResourceDTO} from "opensilex-core/model/namedResourceDTO";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {VariableDetailsDTO} from "opensilex-core/model/variableDetailsDTO";
import {Prop, PropSync } from 'vue-property-decorator';
import {OntologyService} from "opensilex-core/api/ontology.service";
import Oeso from "../../ontologies/Oeso";
import {RDFTypeDTO} from "opensilex-core/model/rDFTypeDTO";

@Component({})
export default class CriteriaOperatorSelector extends Vue {
  $opensilex: OpenSilexVuePlugin;

  service: OntologyService;

  @PropSync("operator")
  operatorURI: string;

  @Prop()
  defaultSelectedValue;

  @Prop({default: false})
  required;

  created() {
    this.service = this.$opensilex.getService("opensilex.OntologyService");
  }


  searchOperators() {

    return this.service.getSubClassesOf(Oeso.MATHMATICAL_OPERATORS_URI, true).then(http => {
      return http;
    }).catch(this.$opensilex.errorHandler);

  }

  operatorToSelectNode(dto: RDFTypeDTO) {
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

  load(operators: Array<string>) {

    return this.service.getURILabelsList(operators).then((http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>) => {
      return (http && http.response) ? http.response.result : undefined
    }).catch(this.$opensilex.errorHandler);

  }
}
</script>

<style scoped lang="scss">

</style>

<i18n>

en:
  OperatorSelector:
    placeholder: Select an operator
    label: Operator

fr:
  OperatorSelector:
    placeholder: Sélectionner un opérateur
    label: Opérateur

</i18n>