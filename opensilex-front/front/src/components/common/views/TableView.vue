<template>
  <div>
    <div class="input-group input-group-sm mb-2" v-if="computedTotalRows > 0">
      <slot name="export" />
    </div>

    <div class="card p-3">
      <!-- Filtre global -->
      <div v-if="globalFilterField" class="mb-3">
        <opensilex-StringFilter
          v-model:filter="filter"
          :placeholder="$t(filterPlaceholder)"
          :debounce="300"
          :lazy="false"
        />
      </div>

      <!-- Info sur le nombre de résultats -->
      <div v-if="showCount" class="mb-2">
        <strong>
          <span v-if="computedTotalRows > 0">
            {{ $t('component.common.list.pagination.nbEntries', {
              limit: getCurrentItemLimit(),
              offset: getCurrentItemOffset(),
              totalRow: computedTotalRows
            }) }}
          </span>
          <span v-else>
            {{ $t('component.common.list.pagination.noEntries') }}
          </span>
        </strong>
      </div>

      <!-- Tableau -->
      <n-data-table
        :columns="normalizedFields"
        :data="pagedItems"
        :pagination="false"
        :bordered="false"
        :striped="true"
        :scroll-x="true"
        :row-key="row => row.uri"
        @update:checked-row-keys="onRowSelected"
        @update:sorter="handleSort"
      >

      
        <template #header="{ column }">
          <template v-if="!column.isSelect">
            {{ $t(column.title) }}
          </template>
        </template>

        <template #body-cell="{ column, row }">
          <template v-if="!column.isSelect">
            <template v-if="$slots[`cell(${column.key})`]">
                <component
                :is="$slots[`cell(${column.key})`]"
                :data='{ item: row }'
                />
            </template>
        <template v-else>
            {{ row[column.key] }}
        </template>
          </template>
          <template v-else>
            <span class="checkbox" />
          </template>
        </template>

      </n-data-table>

      <!-- Pagination -->
      <div class="pagination-wrapper mt-3">
        <n-pagination
          v-if="withPagination"
          v-model:page="currentPage"
          :page-size="pageSize"
          :page-sizes="pageSizeOptions"
          :item-count="computedTotalRows"
          show-size-picker
          @update:page-size="onNbElementPerPageChange"
        />
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { NDataTable, NPagination } from 'naive-ui';

const props = defineProps({
  items: { type: Array, default: () => [] },
  fields: Array,
  defaultSortBy: { type: String, default: 'name' },
  filterPlaceholder: { type: String, default: 'TableView.filter.placeholder' },
  globalFilterField: { type: Boolean, default: false },
  showCount: { type: Boolean, default: true },
  withPagination: { type: Boolean, default: true },
  sortBy: String,
  sortDesc: { type: Boolean, default: false },
  selectable: { type: Boolean, default: false }
});

const emit = defineEmits(['row-selected']);
const { t } = useI18n();

const currentPage = ref(1);
const filter = ref<string | null>(null);
const pageSize = ref(20);

const pageSizeOptions = [
  { label: '10 / page', value: 10 },
  { label: '20 / page', value: 20 },
  { label: '50 / page', value: 50 },
  { label: '100 / page', value: 100 },
];

// Tri
const sortKey = ref<string | null>(props.defaultSortBy || null);
const sortOrder = ref<'ascend' | 'descend' | null>('ascend');

function handleSort({ columnKey, order }: { columnKey: string; order: 'ascend' | 'descend' | false }) {
  sortKey.value = order ? columnKey : null;
  sortOrder.value = order || null;
}

// Appliquer filtre et tri
const filteredItems = computed(() => {
  let items = [...props.items];

  if (filter.value) {
    items = items.filter((item) =>
      Object.values(item).some(val =>
        String(val).toLowerCase().includes(filter.value!.toLowerCase())
      )
    );
  }

  if (sortKey.value && sortOrder.value) {
    items.sort((a, b) => {
      const aVal = a[sortKey.value!];
      const bVal = b[sortKey.value!];

      if (aVal == null) return 1;
      if (bVal == null) return -1;
      if (aVal < bVal) return sortOrder.value === 'ascend' ? -1 : 1;
      if (aVal > bVal) return sortOrder.value === 'ascend' ? 1 : -1;
      return 0;
    });
  }

  return items;
});

// Pagination
const computedTotalRows = computed(() => filteredItems.value.length);

const pagedItems = computed(() => {
  if (!props.withPagination) return filteredItems.value;
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredItems.value.slice(start, start + pageSize.value);
});

function onRowSelected(keys: any[]) {
  emit('row-selected', keys[0]);
}

function onNbElementPerPageChange(value: number) {
  pageSize.value = value;
  currentPage.value = 1;
}

function getCurrentItemOffset(): number {
  return Math.min(currentPage.value * pageSize.value, computedTotalRows.value);
}

function getCurrentItemLimit(): number {
  return computedTotalRows.value === 0 ? 0 : (currentPage.value - 1) * pageSize.value + 1;
}

function getCellTemplateName(key: string) {
  return `cell(${key})`;
}

// Colonnes avec option de tri
const normalizedFields = computed(() =>
  props.fields.map((field: any) => ({
    key: field.key,
    title: field.label,
    isSelect: field.isSelect,
    sorter: field.sortable || false,
  }))
);

watch(() => props.items, () => {
  currentPage.value = 1;
}, { deep: true });

watch(filter, () => {
  currentPage.value = 1;
});
</script>

<style scoped>
.checkbox {
  display: inline-block;
  width: 1rem;
  height: 1rem;
  background-color: #fff;
  border: 1px solid #adb5bd;
  border-radius: 4px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
}
</style>
