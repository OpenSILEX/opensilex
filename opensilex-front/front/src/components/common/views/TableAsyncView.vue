<template>
  <opensilex-Overlay :show="isSearching && !isGlobalLoaderVisible">
    <div class="card">
  

      <!-- Header si table sélectionnable -->
      <div v-if="isSelectable && tableRef" class="card-header d-flex justify-content-between align-items-center">
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
        <!-- <opensilex-NbêtreElementPerPageSelector @change="onNbElementPerPageChange" /> -->
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
        :row-key="row => row.uri"
        @row-click="onRowClicked"
        :row-props="(row) => ({ style: 'cursor: pointer' })"
        :checked-row-keys="checkedRowKeys"
        @update:checked-row-keys="onCheckedRowKeysChange"
        @update:page="(page) => pagination.page = page"
        @update:page-size="(size) => pagination.pageSize = size"
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

// Props
const props = defineProps<{
  fields: any;
  fieldKeyToSortableModelLabelMap?: Record<string, string>;
  searchMethod: Function;
  useQueryParams?: boolean;
  defaultSortBy?: string;
  defaultSortDesc?: boolean;
  selectMode?: "multi" | "single";
  labelNumberOfSelectedRow?: string;
  iconNumberOfSelectedRow?: string;
  maximumSelectedRows?: number;
  selectAllLimit?: number;
  defaultPageSize: number;
  showCount: { type: Boolean, default: true },
  isSelectable: { type: Boolean, default: false },
  allowOnlySelected: Boolean, // optionnel, par défaut false
  showActions: Boolean, // pour bouton dropdown actions groupées
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


// watch(currentPage, () => {
//   const startPath = localStorage.getItem("startPath");
//   const tabPath = localStorage.getItem("tabPath");

//   if (routeArr.value[1] === startPath || startPath === routeArr.value[1] + "s") {
//     if (routeArr.value[2]) {
//       if (routeArr.value[2] === tabPath) {
//         currentPage.value = parseInt(localStorage.getItem("tabPage") || "1", 10);
//         $opensilex.updateURLParameter("tabPage", currentPage.value, "");
//       } else {
//         localStorage.setItem("tabPath", routeArr.value[2]);
//         localStorage.setItem("tabPage", "1");
//         currentPage.value = 1;
//         tableRef.value?.refresh();
//       }
//     } else {
//       currentPage.value = parseInt(localStorage.getItem("page") || "1", 10);
//       $opensilex.updateURLParameter("page", currentPage.value, "");
//     }
//   } else {
//     localStorage.setItem("startPath", routeArr.value[1]);
//     localStorage.setItem("page", "1");
//     localStorage.setItem("tabPath", routeArr.value[2] || "");
//     localStorage.setItem("tabPage", "1");
//     currentPage.value = 1;
//     tableRef.value?.refresh();
//   }
// });

onMounted(() => {
  // if (routeArr.value[2]) {
  //   currentPage.value = parseInt(localStorage.getItem("tabPage") || "1", 10);
  //   currentStartPath.value = localStorage.getItem("startPath") || "";
  //   currentTabPath.value = localStorage.getItem("tabPath") || "";
  // } else {
  //   currentPage.value = parseInt(localStorage.getItem("page") || "1", 10);
  //   currentStartPath.value = localStorage.getItem("startPath") || "";
  // }

  if (props.isSelectable && props.selectMode !== "single") {
    props.fields.unshift({ key: "select", isSelect: true });
  }

  // if (props.useQueryParams) {
  //   const query = route.query;

  //   pageSize.value = parseInt(query.pageSize as string) || props.defaultPageSize || 20;
  //   sortBy.value = decodeURIComponent(query.sortBy as string || "") || props.defaultSortBy || "name";
  //   sortDesc.value = query.sortDesc === "true" || props.defaultSortDesc || false;
  // }

  // attendre que la table soit générée pour appeler et remplir de données
  nextTick(() => {
    if (tableRef.value) {
      refresh();
    }
  });
});



function onCheckedRowKeysChange(newKeys: DataTableRowKey[]) {
  checkedRowKeys.value = newKeys;

  // Synchroniser selectedItems avec les objets complets correspondants
  selectedItems.value = dataList.value.filter(item => newKeys.includes(item.uri));

  numberOfSelectedRows.value = selectedItems.value.length;

  // Émettre un événement de selection
  emit('row-selected', numberOfSelectedRows.value);
}

// function pageChange(newPage: number) {
//   if (routeArr.value[2]) {
//     localStorage.setItem("tabPage", newPage.toString());
//   } else {
//     localStorage.setItem("page", newPage.toString());
//   }
//   currentPage.value = newPage;
//   tableRef.value?.refresh();
// }

// function setInitiallySelectedItems(initiallySelectedItems: any[]) {
//   if (Array.isArray(initiallySelectedItems) && initiallySelectedItems.length !== 0) {
//     selectedItems.value = [];
//     initiallySelectedItems.forEach((e) => {
//       selectedItems.value.push(e);
//     });
//     update();
//   }
// }

// function getHeadTemplateName(key: string): string {
//   return `head(${key})`;
// }

// function getCellTemplateName(key: string): string {
//   return `cell(${key})`;
// }

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

// function onRowSelected(items: any[]) {
//   if (
//     props.maximumSelectedRows &&
//     numberOfSelectedRows.value > props.maximumSelectedRows
//   ) {
//     if (selectedRowIndex.value !== null) {
//       tableRef.value?.unselectRow(selectedRowIndex.value);
//       const idx = selectedItems.value.findIndex((it) => selectedItem.value === it);
//       selectedItems.value.splice(idx, 1);
//     }
//   }

//   numberOfSelectedRows.value = selectedItems.value.length;
//   emit("row-selected", numberOfSelectedRows.value);
// }


// function pagination() {
//   tableRef.value?.refresh();
// }

const pagination = ref({
  page: 1,
  pageSize: 10,
  pageSizes: [10, 20, 50, 100],
  showSizePicker: true
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




// function refresh() {
//   currentPage.value = 1;
//   pageSize.value = props.defaultPageSize || 20;
//   tableRef.value?.refresh();
// }

async function refresh() {
  isSearching.value = true;
  try {
    const results = await loadData(); // ton wrapper de recherche (appel API ou slice selected)
    dataList.value = results;
    console.log("datalist.value ", dataList.value)
  } catch (e) {
    if ($opensilex) {
      $opensilex.errorHandler(e);
    }
  } finally {
    isSearching.value = false;
  }
}


// function changeCurrentPage(page: number) {
//   currentPage.value = page;
//   if (routeArr.value[2]) {
//     localStorage.setItem("tabPage", page.toString());
//   } else {
//     localStorage.setItem("page", page.toString());
//   }
//   refresh();
// }

// function resetSelected() {
//   onlySelected.value = false;
//   selectAll.value = false;
//   onSelectAll(); // à définir quelque part
//   refresh();
// }

// function update() {
//   tableRef.value?.refresh();
// }

// function onRefreshed() {
//   emit("refreshed");
//   numberOfSelectedRows.value = selectedItems.value.length;

//   setTimeout(() => {
//     afterRefreshedItemsSelection();
//   }, 1);
// }

// function afterRefreshedItemsSelection() {
//   selectedItems.value.forEach((element) => {
//     const index = tableRef.value?.sortedItems.findIndex((it) => element.uri === it.uri);
//     if (index !== undefined && index >= 0) {
//       tableRef.value?.selectRow(index);
//     }
//   });
// }

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

function getSelected(): any[] {
  return selectedItems.value;
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

// function clickOnlySelected() {
//   onlySelected.value = !onlySelected.value;
//   currentPage.value = 1;
//   tableRef.value?.refresh();
// }

function loadData() {
  console.log("fonction load data")
  const orderBy = getOrderBy();

  // if (props.useQueryParams) {
  //   $opensilex.updateURLParameter("sortBy", sortBy.value, props.defaultSortBy);
  //   $opensilex.updateURLParameter("sortDesc", sortDesc.value, props.defaultSortDesc);
  //   $opensilex.updateURLParameter("pageSize", pageSize.value.toString(), pageSize.value.toString());
  // }

  $opensilex.disableLoader();
  isSearching.value = true;

  if (onlySelected.value) {
    totalRow.value = selectedItems.value.length;
    pageSize.value = selectedItems.value.length <= props.defaultPageSize ? selectedItems.value.length : props.defaultPageSize;
    isSearching.value = false;

    if (pageSize.value > 0) {
      selectAll.value = true;
    }

    $opensilex.enableLoader();
    return Promise.resolve(
      selectedItems.value.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value)
    );
  } else {
    return props.searchMethod({
      orderBy,
      currentPage: currentPage.value - 1,
      pageSize: parseInt(route.query.pageSize as string) || props.defaultPageSize,
    })
      .then((http: HttpResponse<OpenSilexResponse<any[]>>) => {
        totalRow.value = http.response.metadata.pagination.totalCount;
        pageSize.value = http.response.metadata.pagination.pageSize;
        isSearching.value = false;
        $opensilex.enableLoader();
        console.log("loadData response ", http.response)
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


function getTotalRow(): number {
  return totalRow.value;
}

// function onSelectAll() {
//   if (selectAll.value) {
//     if (totalRow.value > props.selectAllLimit) {
//       alert(t("TableAsyncView.alertSelectAllLimitSize") + props.selectAllLimit);
//       nextTick(() => {
//         selectAll.value = false;
//       });
//     } else {
//       if (!selectedItems.value) selectedItems.value = [];

//       const orderBy = getOrderBy();

//       props.searchMethod({
//         orderBy,
//         currentPage: 0,
//         pageSize: props.selectAllLimit,
//       }).then((http: HttpResponse<OpenSilexResponse<any[]>>) => {
//         totalRow.value = http.response.metadata.pagination.totalCount;
//         const results = http.response.result;
//         results.forEach((elem) => {
//           if (!selectedItems.value.find(obj => obj.uri === elem.uri)) {
//             selectedItems.value.push(elem);
//           }
//         });
//         numberOfSelectedRows.value = selectedItems.value.length;
//         tableRef.value?.selectAllRows();
//         emit("selectall", selectedItems.value);
//       });
//     }
//   } else {
//     selectedItems.value = [];
//     numberOfSelectedRows.value = 0;
//     tableRef.value?.clearSelected();
//     emit("selectall", selectedItems.value);
//   }
// }


// function checkSelectedItems(uri: string) {
//   const idx = selectedItems.value.findIndex(it => it.uri === uri);
//   if (idx !== -1) {
//     selectedItems.value.splice(idx, 1);
//   }
// }

const slots = useSlots();

const dataList = ref<any[]>([]);
const tableData = computed(() => dataList.value);

const naiveColumns = computed(() => {
  const dynamicCols = props.fields.map((field: any) => ({
    title: t(field.label),
    key: field.key,
    resizable: true,
    sortable: field.sortable ?? false,
    sorter: (a, b) => {
  const valA = a[field.key];
  const valB = b[field.key];

  // Si les valeurs sont des chaînes, utiliser localeCompare
  if (typeof valA === 'string' && typeof valB === 'string') {
    return valA.localeCompare(valB);
  }
  // Sinon, supposer qu'elles sont numériques
  return (valA ?? 0) - (valB ?? 0);
}
,
    render: (row: any, index: number) => {
      return slots[`cell(${field.key})`]
        ? slots[`cell(${field.key})`]!({ data: { item: row, index } })
        : row[field.key];
    },
  }));
      console.log("field : ", dynamicCols)

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


defineExpose({
  refresh,
  getSelected,
  onItemSelected,
  onItemUnselected,
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
  top: -5px;
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