<template>
  <b-modal ref="modalRef" size="xl" :static="true" @shown="$emit('shown')">
    <template v-slot:modal-title>
      <i class="ik ik-search mr-1"></i>
      {{ $t('component.project.filter-description') }}
    </template>

    <template v-slot:modal-footer>
        <button
            type="button"
            class="btn btn-secondary"
            v-on:click="hide(false)"
        >{{ $t('component.common.close') }}</button>
        <button
            type="button"
            class="btn btn-primary"
            v-on:click="hide(true)"
        >{{ $t('component.common.validateSelection') }}</button>
    </template>

    <div class="card">
        <opensilex-SearchFilterField
            @search="refresh()"
            @clear="reset()"
            :showAdvancedSearch="true"
        >
            <template v-slot:filters>
                <!-- Name -->
                <opensilex-FilterField>
                    <label for="name">{{ $t("component.common.name") }}</label>
                    <opensilex-StringFilter
                        :filter.sync="filter.name"
                        placeholder="VariableList.name-placeholder"
                    ></opensilex-StringFilter>
                </opensilex-FilterField>

                <!-- Entity -->
                <opensilex-FilterField>
                    <opensilex-EntitySelector
                        label="VariableView.entity"
                        :entity.sync="filter.entity"
                    ></opensilex-EntitySelector>
                </opensilex-FilterField>

                <!-- Characteristic -->
                <opensilex-FilterField>
                    <opensilex-CharacteristicSelector
                        label="VariableView.characteristic"
                        :characteristic.sync="filter.characteristic"
                    ></opensilex-CharacteristicSelector>
                </opensilex-FilterField>

                <!-- GroupVariables -->
                <opensilex-FilterField>
                    <opensilex-GroupVariablesSelector
                        label="VariableView.groupVariable"
                        :variableGroup.sync="filter.group"
                    ></opensilex-GroupVariablesSelector>
                </opensilex-FilterField>
            </template>

            <template v-slot:advancedSearch>
                <!-- InterestEntity -->
                <opensilex-FilterField>
                    <opensilex-InterestEntitySelector
                        label="VariableForm.interestEntity-label"
                        :interestEntity.sync="filter.entityOfInterest"
                    ></opensilex-InterestEntitySelector>
                </opensilex-FilterField>

                <!-- Method -->
                <opensilex-FilterField>
                    <opensilex-MethodSelector
                        label="VariableView.method"
                        :method.sync="filter.method"
                    ></opensilex-MethodSelector>
                </opensilex-FilterField>

                <!-- Unit -->
                <opensilex-FilterField>
                    <opensilex-UnitSelector
                        label="VariableView.unit"
                        :unit.sync="filter.unit"
                    ></opensilex-UnitSelector>
                </opensilex-FilterField>
            </template>
        </opensilex-SearchFilterField> 

        <opensilex-VariableListWithoutFilter 
        ref="varList"
        :isSelectable="true"
        :noActions="true"
        :experiment="filter.experiment"
        :pageSize="5"
        >
        </opensilex-VariableListWithoutFilter>
    </div>
  </b-modal>
</template>

<script lang="ts">
import Vue from "vue";
import { Component, Ref, PropSync } from "vue-property-decorator";
import VariableListWithoutFilter from "./VariableListWithoutFilter.vue";

@Component
export default class VariablesModalListByExp extends Vue {

  @PropSync("searchFilter", {
    default: () => {
      return {
        name: undefined,
        entity: undefined,
        entityOfInterest: undefined,
        characteristic: undefined,
        method: undefined,
        unit: undefined,
        group: undefined,
        experiment: undefined
      };
    },
  })
  filter;


  @Ref("varList") readonly varList!: VariableListWithoutFilter;
  @Ref("modalRef") readonly modalRef!: any;

  unSelect(row) {
    this.varList.onItemUnselected(row);
  }

  show() {
    this.modalRef.show();
  }

  hide(validate: boolean) {
    this.modalRef.hide();

    if (validate) {
      this.$emit("onValidate", this.varList.getSelected());
    }
  }

  refresh() {
    this.varList.filter = this.filter;
    this.varList.refresh();
  }

  reset(){
    this.filter.name = undefined;
    this.filter.entity = undefined;
    this.filter.entityOfInterest = undefined;
    this.filter.characteristic = undefined;
    this.filter.method = undefined;
    this.filter.unit = undefined;
    this.filter.group = undefined;
    this.refresh();
  }

  refreshWithKeepingSelection() {
    this.varList.refreshWithKeepingSelection();
  }

}
</script>

<style scoped lang="scss">
</style>
