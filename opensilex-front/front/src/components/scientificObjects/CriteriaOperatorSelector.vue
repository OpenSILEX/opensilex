<template>
  <opensilex-FormSelector
      ref="formSelector"
      label="OperatorSelector.label"
      :selected.sync="operatorId"
      :multiple="false"
      :options="criteriaOperators"
      :clearable="false"
      :rules=rules
      :required="required"
      placeholder="OperatorSelector.placeholder"
      @clear="$emit('clear')"
      @select="select"
      @deselect="deselect"
  ></opensilex-FormSelector>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {Prop, PropSync } from 'vue-property-decorator';
import {DataService} from "opensilex-core/api/data.service";
import { SelectableItem } from '../common/forms/FormSelector.vue';

@Component({})
export default class CriteriaOperatorSelector extends Vue {
  $opensilex: OpenSilexVuePlugin;

  service: DataService;

  @PropSync("operator")
  operatorId: string;

  @Prop()
  rules: string;

  @Prop()
  defaultSelectedValue;

  @Prop({default: false})
  required;

  criteriaOperators: Array<SelectableItem> = [];

  async created() {
    this.service = this.$opensilex.getService("opensilex.DataService");
    this.criteriaOperators = (await this.searchOperators()).response.result.map(e=> {
      return {label: this.$t("OperatorSelector."+e) as string, id: e};
    });
  }

  searchOperators() {

    return this.service.getMathematicalOperators().then(http => {
      return http;
    }).catch(this.$opensilex.errorHandler);

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
  OperatorSelector:
    placeholder: Select an operator
    label: Operator
    LessThan: '<'
    LessOrEqualThan: '<='
    MoreThan: '>'
    MoreOrEqualThan: '>='
    EqualToo: =
    NotMeasured: Is not measured

fr:
  OperatorSelector:
    placeholder: Sélectionner un opérateur
    label: Opérateur
    LessThan: '<'
    LessOrEqualThan: '<='
    MoreThan: '>'
    MoreOrEqualThan: '>='
    EqualToo: =
    NotMeasured: N'est pas mesuré
</i18n>