<template>
  <!-- Bar placed above lists: display (selection) dropdown, actions dropdown, extra buttons, pagination and selection counts -->
  <n-space class="listActionButtons">
    <!-- Display / selection dropdown -->
    <n-dropdown
      v-if="displayOptions"
      :options="displayOptions"
      :placement="placement"
      trigger="hover"
      @select="onDisplaySelect"
    >
      <n-button size="small" class="greenThemeColor">
        {{ displayLabel }}
      </n-button>
    </n-dropdown>

    <!-- Actions on selected items dropdown, disabled while nothing is selected -->
    <n-dropdown
      v-if="actionsOptions && !noActions"
      :options="actionsOptions"
      :placement="placement"
      trigger="hover"
      :disabled="selectedCount === 0"
      @select="onActionSelect"
    >
      <n-button
        size="small"
        :disabled="selectedCount === 0"
        :class="selectedCount === 0 ? 'btn-disabled' : 'greenThemeColor'"
      >
        {{ t('component.common.actions') }}
      </n-button>
    </n-dropdown>

    <!-- Extra buttons specific to the list (export all, create, ...) -->
    <slot></slot>

    <div class="displayAndListSelectionCount">
      <!-- Pagination count, can be replaced via the paginationCount slot -->
      <template v-if="showCount">
        <slot v-if="$slots.paginationCount" name="paginationCount"></slot>
        <div v-else-if="paginationInfo.hasResults">
          <strong>
            <span class="ml-1">
              {{ t('component.common.list.pagination.nbEntries', {
                limit: paginationInfo.start,
                offset: paginationInfo.end,
                totalRow: n(paginationInfo.total)
              }) }}
            </span>
          </strong>
        </div>
        <div v-else>
          <strong>
            <span class="ml-1">{{ t('component.common.list.pagination.noEntries') }}</span>
          </strong>
        </div>
      </template>

      <!-- Selection count -->
      <template v-if="selectedLabel !== undefined && selectedCount !== undefined">
        <span> | </span>
        <span>
          {{ selectedLabel }} :
          <span class="badge badge-pill greenThemeColor">{{ selectedCount }}</span>
        </span>
      </template>
    </div>
  </n-space>
</template>

<script setup lang="ts">
import {NButton, NDropdown, NSpace, type DropdownProps} from "naive-ui";
import {useI18n} from "vue-i18n";

//#region Public
/** Position of the displayed page inside the whole list */
export interface ListPaginationInfo {
  start: number,
  end: number,
  total: number,
  hasResults: boolean
}

const emit = defineEmits<{
  /** Key of the option chosen in the display dropdown */
  displaySelect: [key: string],
  /** Key of the option chosen in the actions dropdown */
  actionSelect: [key: string]
}>();

interface Props {
  paginationInfo: ListPaginationInfo,
  /** Display dropdown is hidden if not given */
  displayOptions?: DropdownProps['options'],
  /** Already translated label of the display dropdown button */
  displayLabel?: string,
  /** Actions dropdown is hidden if not given */
  actionsOptions?: DropdownProps['options'],
  noActions?: boolean,
  /** Selection count is hidden if not given */
  selectedCount?: number,
  /** Already translated label of the selection count, e.g. t('ScientificObjectList.selected'). Selection count is hidden if not given */
  selectedLabel?: string,
  showCount?: boolean,
  /** Placement of both dropdown menus, naive-ui default if not given */
  placement?: DropdownProps['placement']
}

withDefaults(defineProps<Props>(), {
  noActions: false,
  showCount: true
});
//#endregion

//#region Private
//#region Plugins & services
const { t, n } = useI18n();
//#endregion

//#region Event handlers
function onDisplaySelect(key: string | number) {
  emit('displaySelect', String(key));
}

function onActionSelect(key: string | number) {
  emit('actionSelect', String(key));
}
//#endregion
//#endregion
</script>

<style scoped>
.btn-disabled {
  background-color: #e0e0e0 !important;
  color: #2e2e2e !important;
  border: none !important;
  cursor: not-allowed;
}

.listActionButtons {
  display: flex;
  gap: 8px;
  align-items: center;
}

.displayAndListSelectionCount {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
