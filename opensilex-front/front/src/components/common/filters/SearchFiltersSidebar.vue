<template>
  <!-- This contains the search button and the sidebar for filters, this component is to be placed inside a  <n-layout has-sider> -->

  <!-- Search Button -->
  <n-space class="mb-2 me-1" align="start">
    <n-button
      quaternary
      circle
      @click="filtersCollapsed = !filtersCollapsed"
      :title="t('searchfilter.label')"
      :class="{ greenThemeColor: filtersCollapsed }"
      class="globalFiltersSearchButton"
    >
      <i class="bi bi-search filtersGlobalSearchIcon"></i>

      <div
        v-show="filtersCollapsed && activeFiltersCount > 0"
        class="filters-count-badge"
      >
        ( {{ activeFiltersCount }} )
      </div>
    </n-button>
  </n-space>

  <!-- The sidebar (box that pops out of left side of screen and contains our filters -->
  <n-layout-sider
    v-model:collapsed="filtersCollapsed"
    :collapsed-width="0"
    :width="360"
    collapse-mode="width"
    show-trigger
    bordered
  >
    <n-space class="p-3" vertical>
      <n-form label-placement="top" size="small" @submit.prevent.stop="refresh">
        <!-- Name -->
        <n-form-item :label="t('DeviceList.filter.namePattern')"  class="compact-form-item">
          <StringFilter
            v-model:filter="filter.name"
            :placeholder="t('DeviceList.filter.namePattern-placeholder')"
            class="searchFilter"
            @handlingEnterKey="refresh"
          />
        </n-form-item>

        <!-- Type -->
        <n-form-item  class="compact-form-item">
          <TypeForm
            v-model:type="filter.rdf_type"
            :baseType="$opensilex.Oeso.DEVICE_TYPE_URI"
            :placeholder="t('DeviceList.filter.rdfTypes-placeholder')"
            class="searchFilter"
            @handlingEnterKey="refresh"
          />
        </n-form-item>

        <!-- Dynamic selected type properties -->
        <n-collapse
          :accordion="false"
          class="advancedFiltersSearch"
          v-if="dynamicTypeProperties.length"
        >
          <n-collapse-item :title="$t('component.common.type-properties')" name="adv">
            <n-form-item
              v-for="property in dynamicTypeProperties"
              :key="property.uri"
              :label="property.name ?? property.uri"
              class="compact-form-item"
            >
              <StringFilter
                v-model:filter="dynamicPropertyFilters[property.uri]"
                :placeholder="property.name ?? property.uri"
                class="searchFilter"
                @handlingEnterKey="refresh"
              />
            </n-form-item>
          </n-collapse-item>
        </n-collapse>

        <!-- Variables -->
        <n-form-item :label="t('DeviceList.filter.variable')">
          <VariableSelectorWithFilter
            v-model:variables="filter.variable"
            :placeholder="t('DeviceList.filter.variable-placeholder')"
            maximumSelectedRows="1"
            class="searchFilter"
          />
        </n-form-item>

        <!-- Start up -->
        <n-form-item :label="t('DeviceList.filter.start_up')" class="compact-form-item">
          <StringFilter
            v-model:filter="filter.start_up"
            :placeholder="t('DeviceList.filter.start_up-placeholder')"
            type="number"
            class="searchFilter"
            @handlingEnterKey="refresh"
          />
        </n-form-item>

        <!-- Facility -->
        <n-form-item  class="compact-form-item">
          <FormSelector
            :label="t('DeviceList.filter.facility')"
            :placeholder="t('DeviceList.filter.facility-placeholder')"
            :multiple="false"
            v-model:selected="filter.facility"
            :options="facilities"
            class="searchFilter"
            @handlingEnterKey="refresh"
          />
        </n-form-item>

        <!-- Brand -->
        <n-form-item :label="t('DeviceList.filter.brand')"  class="compact-form-item">
          <StringFilter
            v-model:filter="filter.brand"
            :placeholder="t('DeviceList.filter.brand-placeholder')"
            class="searchFilter"
            @handlingEnterKey="refresh"
          />
        </n-form-item>

        <!-- Model -->
        <n-form-item :label="t('DeviceList.filter.model')"  class="compact-form-item">
          <StringFilter
            v-model:filter="filter.model"
            :placeholder="t('DeviceList.filter.model-placeholder')"
            class="searchFilter"
            @handlingEnterKey="refresh"
          />
        </n-form-item>

        <!-- Metadata key -->
        <n-form-item :label="t('DeviceList.filter.metadataKey')"  class="compact-form-item">
          <StringFilter
            v-model:filter="filter.metadataKey"
            :placeholder="t('DeviceList.filter.metadataKey-placeholder')"
            class="searchFilter"
            @handlingEnterKey="refresh"
          />
        </n-form-item>

        <!-- Metadata value -->
        <n-form-item :label="t('DeviceList.filter.metadataValue')"  class="compact-form-item">
          <StringFilter
            v-model:filter="filter.metadataValue"
            :placeholder="t('DeviceList.filter.metadataValue-placeholder')"
            class="searchFilter"
            @handlingEnterKey="refresh"
          />
        </n-form-item>

        <n-space justify="end" class="mt-2">
          <Button
            class="resetButton"
            :label="t('component.common.search.clear-button')"
            icon="bi-x-lg"
            @click="reset"
          />
          <Button
            class="greenThemeColor"
            :label="t('component.common.search.search-button')"
            icon="bi-search"
            @click="refresh"
          />
        </n-space>
      </n-form>
    </n-space>
  </n-layout-sider>
</template>

<script setup lang="ts">

import Button from "@/components/common/buttons/Button.vue";
import {NButton, NCollapse, NCollapseItem, NForm, NFormItem, NLayout, NLayoutSider, NSpace} from "naive-ui";
import TypeForm from "@/components/common/forms/TypeForm.vue";
import FormSelector from "@/components/common/forms/FormSelector.vue";
import StringFilter from "@/components/common/filters/StringFilter.vue";
import VariableSelectorWithFilter from "@/components/variables/views/VariableSelectorWithFilter.vue";
import {useI18n} from "vue-i18n";

//TODO MAX delete this todo, note for after holiday, this is a cool shorthand to make v-models (2 way data binding)
//BUT WARING , you can normaly have a default value but there is some warning about that in the doc so i'm not doing that
//#region Props & V-models
const filtersCollapsed = defineModel<boolean>("filtersCollapsed");

//Applied Filter quantity depends on parent filter object so we just pass as a prop
//TODO MAX make some typescript fucntion somewhere for the part of that calculation in parents that always looks the same
interface Props{
  activeFiltersCount: number
}

const props = defineProps<Props>();
//#endregion

//#region Constant values & Composables
const { t } = useI18n();
//#endregion

</script>

<style scoped lang="scss">

</style>