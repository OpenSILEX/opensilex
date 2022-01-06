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
        @clear="$emit('clear')"
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

        <opensilex-FilterField>
          <label for="type">{{ $t("component.common.type") }}</label>
          <opensilex-ScientificObjectTypeSelector
            id="type"
            :types.sync="filter.types"
            :multiple="true"
            :experimentURI="filter.experiment"
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
              :experimentURI="filter.experiment"
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
        :filter.sync="filter"
        :noUpdateURL="true"
      >
      </opensilex-ScientificObjectList>
    </div>
  </b-modal>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import ScientificObjectModalList from "./ScientificObjectModalList.vue";

@Component
export default class ScientificObjectModalListByExp extends ScientificObjectModalList {

}
</script>

<style scoped lang="scss">
</style>
