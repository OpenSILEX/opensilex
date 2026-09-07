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
      <n-form label-placement="top" size="small" @submit.prevent.stop="emit('refresh')">

        <!-- The actual filter fields, passed via Slot -->
        <!-- Pass a suite of n-form-items here -->
        <slot>Nothing to show here</slot>

        <n-space justify="end" class="mt-2">
          <Button
            class="resetButton"
            :label="t('component.common.search.clear-button')"
            icon="bi-x-lg"
            @click="emit('reset')"
          />
          <Button
            class="greenThemeColor"
            :label="t('component.common.search.search-button')"
            icon="bi-search"
            @click="emit('refresh')"
          />
        </n-space>
      </n-form>
    </n-space>
  </n-layout-sider>
</template>

<script setup lang="ts">

import Button from "@/components/common/buttons/Button.vue";
import {NButton, NForm, NLayoutSider, NSpace} from "naive-ui";
import {useI18n} from "vue-i18n";

//#region Props & V-models & Emits
const filtersCollapsed = defineModel<boolean>("filtersCollapsed");

//Applied Filter quantity depends on parent filter object so we just pass as a prop
interface Props{
  activeFiltersCount: number
}

const props = defineProps<Props>();

//Refresh should trigger a re-search with the current filter values, and reset should reset all the filters, handled in caller of this component
const emit = defineEmits(['refresh', 'reset']);
//#endregion

//#region Constant values & Composables
const { t } = useI18n();
//#endregion

</script>

<style scoped lang="scss">

</style>