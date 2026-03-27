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

      </n-data-table>

<!-- <pre style="font-size: 10px; color: red">
  Slots disponibles : {{ Object.keys($slots) }}
</pre> -->


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
import { ref, computed, watch, h } from 'vue';
import { useI18n } from 'vue-i18n';
import { NDataTable, NPagination } from 'naive-ui';
import { onMounted, useSlots } from 'vue';

const slots = useSlots();

onMounted(() => {
  console.log('🧪 Slots disponibles dans TableView:', Object.keys(slots));
});


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
  selectable: { type: Boolean, default: false },
  customRenderers: { type: Object, default: () => ({}) }
});



const emit = defineEmits(['row-selected']);
const { t } = useI18n();

const currentPage = ref(1);
const filter = ref<string | null>(null);
const pageSize = ref(10);

const pageSizeOptions = [
  { label: '10 / page', value: 10 },
  { label: '20 / page', value: 20 },
  { label: '50 / page', value: 50 },
  { label: '100 / page', value: 100 },
];

// Tri
const sortKey = ref<string | null>(props.sortBy || props.defaultSortBy || null);
const sortOrder = ref<'ascend' | 'descend' | null>(
  sortKey.value ? (props.sortDesc ? 'descend' : 'ascend') : null
);

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

    let comparison = 0;

    if (typeof aVal === "string" && typeof bVal === "string") {
      // permet d'éviter la sensibilité à la casse et d'avoir par exemple ("Zorga" avant "aorga" dans les listes)
      comparison = aVal.localeCompare(bVal, undefined, {
        sensitivity: "base",
      });
    } else {
      if (aVal < bVal) comparison = -1;
      else if (aVal > bVal) comparison = 1;
    }

    return sortOrder.value === "ascend" ? comparison : -comparison;
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
  (props.fields || []).map((field: any) => {
    const key = field.key
    const slotName = `cell(${key})`

    return {
      key,
      title: field.label,
      isSelect: field.isSelect,
      sorter: field.sortable || false,

      // priorité :
      // 1) customRenderers prop
      // 2) slot cell(key)
      // 3) fallback: affichage brut de row[key]
      render:
        props.customRenderers?.[key] ||
        (slots[slotName]
          ? (row: any) => slots[slotName]?.({ data: { item: row } })
          : (row: any) => row?.[key])
    }
  })
)



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
