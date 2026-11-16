<template>
  <div>
    <n-layout has-sider>
      <SearchFiltersSidebar
        v-model:filters-collapsed="filtersCollapsed"
        :active-filters-count="activeFiltersCount"
        @refresh="refresh"
        @reset="reset"
      >
        <n-form-item :label="t('component.germplasms.list.filter.label')">
          <StringFilter
            v-model:filter="filter.name"
            placeholder="component.germplasms.list.filter.label-placeholder"
            @handlingEnterKey="refresh"
          />
        </n-form-item>

        <GermplasmSelector
          v-model:germplasm="filter.germplasm"
          :multiple="true"
          @handlingEnterKey="refresh"
        />
      </SearchFiltersSidebar>

      <n-layout-content>
        <TableAsyncView
          ref="tableRef"
          :search-method="searchGermplasmGroups"
          :fields="fields"
          :is-selectable="true"
          select-mode="single"
          label-number-of-selected-row="component.germplasms.list.selected"
          icon-number-of-selected-row="fa#seedling"
          default-sort-by="name"
          @select="emit('select', $event)"
        >
          <template #cell(actions)="{ data }">
            <n-button-group size="small">
              <EditButton
                v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID)"
                label="component.common.edit"
                :small="true"
                @click="emit('edit', data.item)"
              />
              <DeleteButton
                v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_DELETE_ID)"
                label="component.common.delete"
                :small="true"
                @click="emit('delete', data.item)"
              />
            </n-button-group>
          </template>
        </TableAsyncView>
      </n-layout-content>
    </n-layout>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref, useTemplateRef } from 'vue';
import { useI18n } from 'vue-i18n';
import { useStore } from 'vuex';
import { NButtonGroup, NFormItem, NLayout, NLayoutContent } from 'naive-ui';
import { GermplasmService } from 'opensilex-core/lib';
import type { GermplasmGroupGetDTO } from 'opensilex-core/index';
import OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin';
import EditButton from '@/components/common/buttons/EditButton.vue';
import DeleteButton from '@/components/common/buttons/DeleteButton.vue';
import StringFilter from '@/components/common/filters/StringFilter.vue';
import SearchFiltersSidebar from '@/components/common/filters/SearchFiltersSidebar.vue';
import GermplasmSelector from '@/components/germplasm/GermplasmSelector.vue';
import TableAsyncView from '@/components/common/views/TableAsyncView.vue';

interface TableSearchOptions {
  orderBy?: string[];
  currentPage: number;
  pageSize: number;
}

//#region Public
const { t } = useI18n();
const emit = defineEmits<{
  (event: 'select', group: GermplasmGroupGetDTO): void;
  (event: 'edit', group: GermplasmGroupGetDTO): void;
  (event: 'delete', group: GermplasmGroupGetDTO): void;
}>();
//#endregion

//#region Private
const store = useStore();
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const service = opensilex.getService<GermplasmService>('opensilex.GermplasmService');
const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);
const tableRef = useTemplateRef<InstanceType<typeof TableAsyncView>>('tableRef');
const filtersCollapsed = ref(true);
const filter = ref({
  name: '',
  germplasm: [] as string[],
});

const activeFiltersCount = computed(() => {
  let count = 0;

  if (filter.value.name.trim()) {
    count += 1;
  }

  if (filter.value.germplasm.length > 0) {
    count += 1;
  }

  return count;
});

const fields = computed(() => [
  {
    key: 'name',
    label: t('component.common.name'),
    sortable: false,
  },
  {
    key: 'actions',
    label: t('component.common.actions'),
    sortable: false,
  },
]);

function searchGermplasmGroups(options: TableSearchOptions) {
  return service.searchGermplasmGroups(
    filter.value.name || undefined,
    filter.value.germplasm.length > 0 ? filter.value.germplasm : undefined,
    options.orderBy,
    options.currentPage,
    options.pageSize,
  );
}

function refresh() {
  tableRef.value?.setPage(1);
  tableRef.value?.refresh();
}

function reset() {
  filter.value = {
    name: '',
    germplasm: [],
  };
  refresh();
}
//#endregion

defineExpose({
  refresh,
});
</script>
