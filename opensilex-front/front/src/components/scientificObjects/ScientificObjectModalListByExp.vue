<template>
  <b-modal ref="modalRef" size="xl" :static="true" @shown="$emit('shown')">
    <template v-slot:modal-title>
      <i class="ik ik-search mr-1"></i>
      {{ $t('component.project.filter-description') }}
    </template>

    <opensilex-PageContent
      class="pagecontent"
    >

       <!-- Toggle Sidebar--> 
      <div class="searchMenuContainer"
        v-on:click="SearchFiltersToggle = !SearchFiltersToggle"
        :title="searchFiltersPannel()"
      >
        <div class="searchMenuIcon">
          <i class="ik ik-search"></i>
        </div>
      </div>

      <!-- FILTERS -->
      <Transition>
      <div v-show="SearchFiltersToggle">

      <opensilex-SearchFilterField
        @search="refresh()"
        @clear="$emit('clear')"
        label="ScientificObjectList.filter.label"
        :showAdvancedSearch="true"
        class="searchFilterField"
      >
      <template v-slot:filters>
        <!-- Name -->
        <div>
        <opensilex-FilterField>
          <label for="name">{{ $t("component.common.name") }}</label>
          <opensilex-StringFilter
            id="name"
            :filter.sync="filter.name"
            placeholder="ScientificObjectList.name-placeholder"
            class="searchFilter"
            @keyup.enter.native="refresh()"
          ></opensilex-StringFilter>
        </opensilex-FilterField>
        </div>

        <div>
        <opensilex-FilterField>
          <label for="type">{{ $t("component.common.type") }}</label>
          <opensilex-ScientificObjectTypeSelector
            id="type"
            :types.sync="filter.types"
            :multiple="true"
            :experimentURI="filter.experiment"
            class="searchFilter"
          ></opensilex-ScientificObjectTypeSelector>
        </opensilex-FilterField>
        </div>
      </template>

      <template v-slot:advancedSearch>
        <!-- Germplasm -->
        <div>
        <opensilex-FilterField>
          <opensilex-GermplasmSelector
            :multiple="false"
            :germplasm.sync="filter.germplasm"
            :experiment="filter.experiment"
            class="searchFilter"
          ></opensilex-GermplasmSelector>
        </opensilex-FilterField>
        </div>

        <!-- Factors levels -->
        <div>
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
              class="searchFilter"
            ></opensilex-FactorLevelSelector>
          </b-form-group>
        </opensilex-FilterField>
        </div>
        
        <!-- Exists -->
        <div>
        <opensilex-FilterField>
          <opensilex-DateForm
            :value.sync="filter.existenceDate"
            label="ScientificObjectList.existenceDate"
            class="searchFilter"
          ></opensilex-DateForm>
        </opensilex-FilterField>
        </div>

        <!-- Created -->
        <div>
        <opensilex-FilterField>
          <opensilex-DateForm
            :value.sync="filter.creationDate"
            label="ScientificObjectList.creationDate"
            class="searchFilter"
          ></opensilex-DateForm>
        </opensilex-FilterField>
        </div>
      </template>
    </opensilex-SearchFilterField>
      </div>
      </Transition>

      <opensilex-ScientificObjectList 
        ref="soList"
        :isSelectable="true"
        :noActions="true"
        :pageSize="5"
        :searchFilter.sync="filter"
        :variables="variables"
        :devices="devices"
        :noUpdateURL="true"
        @select="$emit('select', $event)"
        @unselect="$emit('unselect', $event)"
        @selectall="$emit('selectall', $event)"
        :maximumSelectedRows="maximumSelectedRows"
      >
      </opensilex-ScientificObjectList>
    </opensilex-PageContent>
    <template v-slot:modal-footer>
      <button
        type="button"
        class="btn btn-secondary"
        v-on:click="hide(false)"
      >{{ $t('component.common.close') }}</button>
      <button
        type="button"
        class="btn greenThemeColor"
        v-on:click="hide(true)"
      >{{ $t('component.common.validateSelection') }}</button>
    </template>
  </b-modal>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import ScientificObjectList from './ScientificObjectList.vue';
import ScientificObjectModalList from "./ScientificObjectModalList.vue";

@Component
export default class ScientificObjectModalListByExp extends ScientificObjectModalList {
  @Ref("soList") readonly soList!: ScientificObjectList;

  @Prop()
  maximumSelectedRows: number;

  @Prop()
  variables: Array<string>;

  @Prop()
  devices: Array<string>;
 
  data(){
    return {
      SearchFiltersToggle : false,
    }
  }
  searchFiltersPannel() {
    return  this.$t("searchfilter.label")
  }

  selectItem(row) {
    this.soList.onItemSelected(row);
  }
  unSelect(row) {
    this.soList.onItemUnselected(row);
  }
  setInitiallySelectedItems(initiallySelectedItems:Array<any>){
    this.soList.setInitiallySelectedItems(initiallySelectedItems);
  }
}
</script>

<style scoped lang="scss">
</style>
