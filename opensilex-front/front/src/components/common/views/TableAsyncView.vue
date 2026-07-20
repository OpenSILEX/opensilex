<template>
  <opensilex-Overlay :show="isSearching">
    <div class="card">
  

      <!-- Header si table sélectionnable -->
      <div v-if="isSelectable && showHeaderCount && tableRef" class="card-header d-flex justify-content-between align-items-center">
        <div>
          <h3 class="d-inline me-2">
            <opensilex-Icon :icon="iconNumberOfSelectedRow" class="title-icon" />
            {{ t(labelNumberOfSelectedRow) }}
          </h3>
          <span v-if="!maximumSelectedRows && selectMode !== 'single'" class="badge rounded-pill greenThemeColor">
            {{ numberOfSelectedRows }}
          </span>
          <span v-else-if="selectMode !== 'single'" class="badge rounded-pill greenThemeColor" :title="t(badgeHelpMessage)">
            {{ numberOfSelectedRows }}/{{ maximumSelectedRows }}
          </span>
          <slot name="selectableTableButtons" :numberOfSelectedRows="numberOfSelectedRows"></slot>
        </div>
      </div>

      <!-- Header si non sélectionnable -->
      <div v-if="!isSelectable && tableRef" class="d-flex justify-content-end">
        <!-- <opensilex-NbElementPerPageSelector @change="onNbElementPerPageChange" /> -->
      </div>

      <!-- Export slot -->
      <div v-if="totalRow > 0">
        <slot name="export"></slot>
      </div>


      <div v-if="showCount">
        <div v-if="totalRow > 0">
          <strong>
            <span class="ml-1">
              {{ t('component.common.list.pagination.nbEntries', {
                limit: start,
                offset: end,
                totalRow: n(total)  
              }) }}
            </span>
          </strong>
        </div>
        <div v-else>
          <strong>
            <span class="ml-1">{{ t('component.common.list.pagination.noEntries') }}</span>
          </strong>
        </div>
      </div>


      <!-- <n-p>
        {{ t('TableAsyncView.selected')}} : <span class="badge badge-pill greenThemeColor">{{ checkedRowKeys.length }} </span>
      </n-p> -->
      


      <!-- Table with Naive UI -->
      <n-data-table
        ref="tableRef"
        :columns="naiveColumns"
        :data="tableData"
        :loading="isSearching"
        :pagination="pagination"
        :remote="true"
        :row-key="row => row.uri"
        @row-click="onRowClickedSafe"
        :row-props="(row) => ({
          style: row.__isDetailsRow ? '' : 'cursor: pointer'
        })"
        :checked-row-keys="checkedRowKeys"
        @update:checked-row-keys="onCheckedRowKeysChange"
        @update:page="onPageChange"
        @update:page-size="onPageSizeChange"
        :sorter="defaultSorter"
        @update:sorter="onSortChange"
      />


    </div>
  </opensilex-Overlay>
</template>


<script setup lang="ts">
import { ref, computed, watch, onMounted, inject, nextTick, useSlots} from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import type { OrderBy } from "opensilex-core/index";
import type { NamedResourceDTO } from "opensilex-core/model/namedResourceDTO";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import type HttpResponse from "../../../lib/HttpResponse";
import { OpenSilexResponse } from "opensilex-core/HttpResponse";
import { NButton, NTag, NDataTable, DataTableRowKey } from 'naive-ui';

export type RowWithData<T> = {
  item: T & {
    _showDetails?: boolean;
  };
};

// Props
const props = defineProps<{
  fields: any,
  fieldKeyToSortableModelLabelMap?: Record<string, string>,
  searchMethod: Function,
  useQueryParams?: boolean,
  defaultSortBy?: string,
  defaultSortDesc?: boolean,
  selectMode?: "multi" | "single",
  labelNumberOfSelectedRow?: string,
  iconNumberOfSelectedRow?: string,
  maximumSelectedRows?: number,
  selectAllLimit?: number,
  defaultPageSize?: { type: number, default: 20 },
  showCount?: { type: Boolean, default: true },
  isSelectable?: { type: Boolean, default: false },
  showHeaderCount?: { type: Boolean, default: true },
  allowOnlySelected?: { type: Boolean, default: false }, // optionnel, par défaut false
  showActions?: { type: Boolean, default: false }, // pour bouton dropdown actions groupées
}>();

//  Injections
const route = useRoute();
const { t, n } = useI18n();
const $opensilex = inject<OpenSilexVuePlugin>("$opensilex")!;
const tableRef = ref(); // lié avec ref="tableRef" dans le template

// States
const currentPage = ref(1);
const tabPage = ref(1);
const pageSize = ref(20);
const totalRow = ref(0);
const sortBy = ref(props.defaultSortBy ?? "name");
const sortDesc = ref(props.defaultSortDesc ?? false);
const isSearching = ref(false);
const selectAll = ref(false);
const onlySelected = ref(false);
const numberOfSelectedRows = ref(0);
const selectedItems = ref<Array<NamedResourceDTO>>([]);
const selectedItem = ref();
const badgeHelpMessage = "component.common.search.badgeHelpMessage";
const currentStartPath = ref("");
const currentTabPath = ref("");
const selectedRowIndex = ref<number | null>(null);
const checkedRowKeys = ref<DataTableRowKey[]>([]);
const selectedUriSet = ref<Set<string>>(new Set())
const selectedCache = ref<Map<string, any>>(new Map()) // uri -> objet complet


const emit = defineEmits<{
  (e: 'select', item: NamedResourceDTO): void;
  (e: 'unselect', item: NamedResourceDTO): void;
  (e: 'row-selected', count: number): void;
  (e: 'refreshed'): void;
  (e: 'selectall', selected: NamedResourceDTO[]): void;
}>();

const defaultSorter = ref({
  columnKey: props.defaultSortBy || 'name',
  order: props.defaultSortDesc ? 'descend' : 'ascend'
});


// Extracted from $route
const routeArr = computed(() => route.path.split("/"));

onMounted(() => {
  if (props.isSelectable && props.selectMode !== "single") {
    props.fields.unshift({ key: "select", isSelect: true });
  }

  // attendre que la table soit générée pour appeler et remplir de données
  nextTick(() => {
    if (tableRef.value) {
      refresh();
    }
  });
});


function onCheckedRowKeysChange(newKeys: DataTableRowKey[]) {
  checkedRowKeys.value = newKeys

  // URIs présentes sur la page courante
  const pageUris = dataList.value.map((it: any) => it.uri)
  const newSet = new Set(newKeys as string[])

  // 1) Retirer les désélections sur la page courante
  for (const uri of pageUris) {
    if (selectedUriSet.value.has(uri) && !newSet.has(uri)) {
      selectedUriSet.value.delete(uri)
      selectedCache.value.delete(uri)
      emit('unselect', dataList.value.find((x: any) => x.uri === uri))
    }
  }

  // 2) Ajouter les nouvelles sélections sur la page courante
  for (const uri of newSet) {
    if (!selectedUriSet.value.has(uri)) {
      selectedUriSet.value.add(uri)
      const item = dataList.value.find((x: any) => x.uri === uri)
      if (item) {
        selectedCache.value.set(uri, item)
        emit('select', item)
      }
    }
  }

  numberOfSelectedRows.value = selectedUriSet.value.size
  emit('row-selected', numberOfSelectedRows.value)
}

 /* Sécurise le clic sur une ligne de tableau.
 *
 * Ignore les lignes techniques utilisées pour afficher les détails,
 * afin d'éviter qu'un clic sur la zone de détails déclenche la sélection
 * ou la désélection d'un élément.
 *
 * Pour les lignes normales, délègue le traitement à `onRowClicked`.
 */
function onRowClickedSafe(row: any) {
  if (row?.__isDetailsRow) {
    return;
  }
  onRowClicked(row);
}

function onRowClicked(item: NamedResourceDTO) {
  const idx = selectedItems.value.findIndex((it) => item.uri === it.uri);

  if (idx >= 0) {
    selectedItems.value.splice(idx, 1);
    emit("unselect", item);
  } else {
    selectedItems.value.push(item);
    if (!props.maximumSelectedRows || numberOfSelectedRows.value < props.maximumSelectedRows) {
      emit("select", item);
    }
  }

  numberOfSelectedRows.value = selectedItems.value.length;

  if (
    props.maximumSelectedRows &&
    numberOfSelectedRows.value > props.maximumSelectedRows
  ) {
    selectedRowIndex.value = tableRef.value?.sortedItems.findIndex((it) => item === it) ?? null;
    selectedItem.value = item;
  }
}

const pagination = ref({
  page: 1,
  pageSize: 20,
  pageSizes: [10, 20, 50, 100],
  showSizePicker: true,
  itemCount: 0
});

const paginationInfo = computed(() => {
  const total = totalRow.value;
  const page = pagination.value.page;
  const pageSize = pagination.value.pageSize;

  const start = total === 0 ? 0 : (page - 1) * pageSize + 1;
  const end = Math.min(page * pageSize, total);

  return {
    start,
    end,
    total,
    hasResults: total > 0
  };
});

const start = computed(() => paginationInfo.value.start);
const end = computed(() => paginationInfo.value.end);
const total = computed(() => paginationInfo.value.total);
// const hasResults = computed(() => paginationInfo.value.hasResults);
const hasResults = computed(() => totalRow.value > 0);


async function refresh() {
  isSearching.value = true;
  try {
    const results = await loadData(); //  wrapper de recherche
    dataList.value = results;
    syncCheckedForCurrentPage()
  } catch (e) {
    if ($opensilex) {
      $opensilex.errorHandler(e);
    }
  } finally {
    isSearching.value = false;
  }
}

function onItemUnselected(item: any) {
  const idx = tableRef.value?.sortedItems.findIndex(it => item.id === it.uri);
  if (idx !== undefined && idx >= 0) {
    tableRef.value?.unselectRow(idx);
  }

  const selectedIdx = selectedItems.value.findIndex(it => item.id === it.uri);
  if (selectedIdx !== -1) {
    selectedItems.value.splice(selectedIdx, 1);
  }
  numberOfSelectedRows.value = selectedItems.value.length;
}

function onItemSelected(item: any) {
  const idx = tableRef.value?.sortedItems.findIndex(it => item.id === it.uri);
  if (idx !== undefined && idx >= 0) {
    tableRef.value?.selectRow(idx);
    selectedItems.value.push(tableRef.value.sortedItems[idx]);
    numberOfSelectedRows.value = selectedItems.value.length;
  }
}

function syncCheckedForCurrentPage() {
  const keysOnPage = dataList.value.map((it: any) => it.uri)
  checkedRowKeys.value = keysOnPage.filter((uri: string) => selectedUriSet.value.has(uri))
}


function getSelected(): any[] {
  return Array.from(selectedCache.value.values())
}

function getOrderBy(): string[] {
  const orderBy: string[] = [];
  if (sortBy.value) {
    let orderByText = sortBy.value + "=";
    if (
      props.fieldKeyToSortableModelLabelMap &&
      props.fieldKeyToSortableModelLabelMap[sortBy.value]
    ) {
      orderByText = props.fieldKeyToSortableModelLabelMap[sortBy.value] + "=";
    }
    orderBy.push(orderByText + (sortDesc.value ? "desc" : "asc"));
  }
  return orderBy;
}

function clickOnlySelected() {
  onlySelected.value = !onlySelected.value;
  currentPage.value = 1;
  tableRef.value?.refresh();
}

function toggleOnlySelected() {
  onlySelected.value = !onlySelected.value
  pagination.value.page = 1
  currentPage.value = 1
  refresh()
}

function resetSelection() {
  selectedUriSet.value.clear()
  selectedCache.value.clear()
  checkedRowKeys.value = []
  numberOfSelectedRows.value = 0

  if (onlySelected.value) {
    onlySelected.value = false
  }

  pagination.value.page = 1
  currentPage.value = 1
  refresh()
}

function loadData() {
  const orderBy = getOrderBy();

  $opensilex.disableLoader();
  isSearching.value = true;

  if (onlySelected.value) {
  const all = Array.from(selectedCache.value.values())
  totalRow.value = all.length

  pagination.value.itemCount = totalRow.value

  const startIdx = (currentPage.value - 1) * pagination.value.pageSize
  const endIdx = currentPage.value * pagination.value.pageSize

  $opensilex.enableLoader()
  isSearching.value = false
  return Promise.resolve(all.slice(startIdx, endIdx))
} else {
    return props.searchMethod({
      orderBy,
      currentPage: currentPage.value - 1,
      pageSize: pagination.value.pageSize,
    })
      .then((http: HttpResponse<OpenSilexResponse<any[]>>) => {
        totalRow.value = http.response.metadata.pagination.totalCount;
        pageSize.value = pagination.value.pageSize;
        pagination.value.itemCount = totalRow.value;
        isSearching.value = false;
        $opensilex.enableLoader();
        return http.response.result;
      })
      .catch((error) => {
        isSearching.value = false;
        $opensilex.errorHandler(error);
      });
  }
}

function getCurrentItemLimit(): number {
  return Math.max(0, pageSize.value * (currentPage.value - 1));
}

function getCurrentItemOffset(): number {
  const offset = pageSize.value * currentPage.value;
  return Math.min(offset, totalRow.value);
}

function getPaginationInfo() {
  const total = totalRow.value
  const page = pagination.value.page
  const size = pagination.value.pageSize

  const start = total === 0 ? 0 : (page - 1) * size + 1
  const end = Math.min(page * size, total)

  return { start, end, total, hasResults: total > 0 }
}

function getCurrentPage() {
  return pagination.value.page
}
function getPageSize() {
  return pagination.value.pageSize
}
function getTotalRow() {
  return totalRow.value
}

function onPageChange(page: number) {
  pagination.value.page = page
  currentPage.value = page
  refresh()
}

function onPageSizeChange(size: number) {
  pagination.value.pageSize = size
  pageSize.value = size
  pagination.value.page = 1
  currentPage.value = 1
  refresh()
}


const slots = useSlots();

const dataList = ref<any[]>([]);

/**
 * Prépare les données envoyées à `n-data-table`.
 *
 * Part de la liste de données principale (`dataList`) et ajoute, juste après
 * chaque ligne qui demande l'affichage de détails (`_showDetails`), une ligne
 * technique supplémentaire.
 *
 * Cette ligne technique est identifiée par `__isDetailsRow` et sert uniquement
 * à afficher le slot `row-details` sous la ligne principale.
 */
const tableData = computed(() => {
  const rows: any[] = [];

  for (const item of dataList.value) {
    rows.push(item);

    if (item._showDetails) {
      rows.push({
        __isDetailsRow: true,
        __parent: item,
        uri: `${item.uri}__details`
      });
    }
  }

  return rows;
});

/**
 * Transforme la configuration générique des colonnes (`props.fields`)
 * en colonnes compatibles avec le composant `n-data-table` de Naive UI.
 *
 * Pour chaque champ, définit :
 * - le titre traduit,
 * - la clé de colonne,
 * - le tri,
 * - le rendu via slot personnalisé si disponible,
 * - la gestion spéciale des lignes de détails.
 *
 * Si la table est sélectionnable, ajoute aussi la colonne de sélection fournie par Naive UI.
 */
const naiveColumns = computed(() => {
  const dynamicCols = props.fields.map((field: any, colIndex: number) => ({
    title: t(field.label),
    key: field.key,
    resizable: true,
    sortable: field.sortable ?? false,
    sorter: (a, b) => {
      if (a.__isDetailsRow || b.__isDetailsRow) return 0;

      const valA = a[field.key];
      const valB = b[field.key];

      if (typeof valA === 'string' && typeof valB === 'string') {
        return valA.localeCompare(valB);
      }

      return (valA ?? 0) - (valB ?? 0);
    },
    render: (row: any, index: number) => {
      if (row.__isDetailsRow) {
        if (colIndex !== 0) {
          return null;
        }

        return slots['row-details']
          ? slots['row-details']!({
              data: {
                item: row.__parent,
                index
              }
            })
          : null;
      }

      return slots[`cell(${field.key})`]
        ? slots[`cell(${field.key})`]!({
            data: {
              item: row,
              index
            }
          })
        : row[field.key];
    },
    colSpan: (row: any) => {
      if (row.__isDetailsRow) {
        return colIndex === 0 ? props.fields.length + (props.isSelectable ? 1 : 0) : 0;
      }
      return 1;
    }
  }));

  if (props.isSelectable) {
    return [
      { type: 'selection' },
      ...dynamicCols
    ];
  }

  return dynamicCols;
});

function onSortChange(sorter) {
  if (!sorter) return;

  sortBy.value = sorter.columnKey;
  sortDesc.value = sorter.order === 'descend';

  // Relance la récupération ou le tri des données
  refresh();
}

function setPage(page: number) {
  pagination.value.page = page
  currentPage.value = page
}

defineExpose({
  refresh,
  getSelected,
  onItemSelected,
  onItemUnselected,
  getPaginationInfo,
  getCurrentPage,
  getPageSize,
  getTotalRow,
  toggleOnlySelected,
  resetSelection,
  setPage
});


</script>

<style scoped>
.table-selectable tbody tr {
  cursor: pointer;
}

.table-active {
  background-color: rgba(0, 163, 141, 0.1);
}

.title-icon {
  position: relative;
}
</style>

<i18n>

en:
    TableAsyncView:
        selected: Selected Elements
fr:
    TableAsyncView:
        selected: Élements Sélectionnés
       
</i18n>