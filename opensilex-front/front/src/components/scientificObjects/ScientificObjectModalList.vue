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
        label="ScientificObjectList.filter.label"
        :showAdvancedSearch="true"
      >
      <template v-slot:filters>
        <!-- Name -->
        <opensilex-FilterField>
          <label for="name">{{ $t("component.common.name") }}</label>
          <opensilex-StringFilter
            id="name"
            :filter.sync="filter.name"
            placeholder="ScientificObjectList.name-placeholder"
          ></opensilex-StringFilter>
        </opensilex-FilterField>
        <!-- Experiments -->
        <opensilex-FilterField>
          <opensilex-ExperimentSelector
            label="GermplasmList.filter.experiment"
            :multiple="false"
            :experiments.sync="filter.experiment"
          ></opensilex-ExperimentSelector>
        </opensilex-FilterField>

        <opensilex-FilterField>
          <label for="type">{{ $t("component.common.type") }}</label>
          <opensilex-ScientificObjectTypeSelector
            id="type"
            :types.sync="filter.types"
            :multiple="true"
          ></opensilex-ScientificObjectTypeSelector>
        </opensilex-FilterField>
      </template>

      <template v-slot:advancedSearch>
        <!-- Germplasm -->
        <opensilex-FilterField>
          <opensilex-GermplasmSelector
            :multiple="false"
            :germplasm.sync="filter.germplasm"
            :experiment="filter.experiment"
          ></opensilex-GermplasmSelector>
        </opensilex-FilterField>
        <!-- Factors levels -->
        <opensilex-FilterField>
          <b-form-group>
            <label for="factorLevels">
              {{ $t("FactorLevelSelector.label") }}
            </label>
            <opensilex-FactorLevelSelector
              id="factorLevels"
              :factorLevels.sync="filter.factorLevels"
              :multiple="true"
              :required="false"
            ></opensilex-FactorLevelSelector>
          </b-form-group>
        </opensilex-FilterField>
        <!-- Exists -->
        <opensilex-FilterField>
          <opensilex-DateForm
            :value.sync="filter.existenceDate"
            label="ScientificObjectList.existenceDate"
          ></opensilex-DateForm>
        </opensilex-FilterField>
        <!-- Created -->
        <opensilex-FilterField>
          <opensilex-DateForm
            :value.sync="filter.creationDate"
            label="ScientificObjectList.creationDate"
          ></opensilex-DateForm>
        </opensilex-FilterField>
      </template>
    </opensilex-SearchFilterField>
      <opensilex-ScientificObjectList 
        ref="soList"
        :isSelectable="true"
        :noActions="true"
        :pageSize="10"
        :searchFilter.sync="filter"
        :noUpdateURL="true"
      >
      </opensilex-ScientificObjectList>
    </div>
  </b-modal>
</template>

<script lang="ts">
import Vue from "vue";
import { Component, Ref, Prop } from "vue-property-decorator";

@Component
export default class ScientificObjectModalList extends Vue {

  filter = {
    name: "",
    experiment: undefined,
    germplasm: undefined,
    factorLevels: [],
    types: [],
    existenceDate: undefined,
    creationDate: undefined,
  };

  @Ref("soList") readonly soList!: any;
  @Ref("modalRef") readonly modalRef!: any;

  unSelect(row) {
    this.soList.onItemUnselected(row);
  }

  show() {
    this.modalRef.show();
  }

  hide(validate: boolean) {
    this.modalRef.hide();

    if (validate) {
      this.$emit("onValidate", this.soList.getSelected());
    }
  }

  refresh() {
    this.soList.refresh();
  }

  reset() {
    this.filter = {
      name: "",
      experiment: undefined,
      germplasm: undefined,
      factorLevels: [],
      types: [],
      existenceDate: undefined,
      creationDate: undefined,
    };
    this.refresh();
  }

  refreshWithKeepingSelection() {
    this.soList.refreshWithKeepingSelection();
  }

}
</script>

<style scoped lang="scss">
</style>
