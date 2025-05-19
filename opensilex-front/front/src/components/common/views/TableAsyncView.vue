<template>
  <div class="card">
    <!-- Selectable tables header -->
    <div v-if="isSelectable && tableRef" class="card-header clearfix">
      <div>
        <h3 class="d-inline me-1">
          <opensilex-Icon :icon="iconNumberOfSelectedRow" class="title-icon" />
          {{ $t(labelNumberOfSelectedRow) }}
        </h3>
        <span 
          v-if="!maximumSelectedRows && selectMode !== 'single'" 
          class="badge rounded-pill bg-success"
        >
          {{ numberOfSelectedRows }}
        </span>
        <span 
          v-else-if="selectMode !== 'single'" 
          class="badge rounded-pill bg-success" 
          v-tooltip="$t(badgeHelpMessage)"
        >
          {{ numberOfSelectedRows }}/{{ maximumSelectedRows }}
        </span>
        <slot 
          name="selectableTableButtons" 
          :numberOfSelectedRows="numberOfSelectedRows"
        ></slot>
      </div>
      <opensilex-NbElementPerPageSelector
        @change="onNbElementPerPageChange"
      />
    </div>

    <!-- Non-selectable tables header -->
    <div 
      v-if="!isSelectable && tableRef" 
      class="d-flex justify-content-end mt-2"
    >
      <opensilex-NbElementPerPageSelector
        @change="onNbElementPerPageChange"
      />
    </div>

    <!-- Export section -->
    <b-input-group size="sm">
      <slot name="export" v-if="totalRow > 0"></slot>
    </b-input-group>

    <!-- Entries count -->
    <div v-if="showCount">
      <div v-if="totalRow > 0">
        <strong>
          <span class="ms-1">
            {{ $t('component.common.list.pagination.nbEntries', { 
              limit: getCurrentItemLimit(),
              offset: getCurrentItemOffset(), 
              totalRow: $n(totalRow) 
            }) }}
          </span>
        </strong>
      </div>
      <div v-else>
        <strong>
          <span class="ms-1">
            {{ $t('component.common.list.pagination.noEntries') }}
          </span>
        </strong>
      </div>
    </div>

    <!-- Table -->
    <div class="table-responsive">
      <table 
        ref="tableRef"
        class="table table-striped table-hover table-sm"
        :class="{ 'table-selectable': selectMode === 'single' }"
      >
        <thead>
          <tr>
            <th 
              v-for="(field, index) in fields" 
              :key="index"
              @click="updateSort(field.key)"
            >
              <template v-if="!field.isSelect">
                {{ $t(field.label || field.key) }}
                <span v-if="sortBy === field.key">
                  {{ sortDesc ? '▼' : '▲' }}
                </span>
              </template>
              <template v-else>
                <input 
                  v-if="!maximumSelectedRows"
                  type="checkbox"
                  v-model="selectAll"
                  @change="onSelectAll"
                />
              </template>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr 
            v-for="(item, index) in tableData" 
            :key="item.uri || index"
            @click="onRowClicked(item)"
            :class="{ 'table-active': isSelected(item) }"
          >
            <td 
              v-for="(field, fieldIndex) in fields" 
              :key="fieldIndex"
            >
              <template v-if="!field.isSelect">
                <slot 
                  :name="`cell(${field.key})`" 
                  :data="{ item, value: item[field.key] }"
                >
                  {{ item[field.key] }}
                </slot>
              </template>
              <template v-else>
                <input 
                  type="checkbox"
                  :checked="isSelected(item)"
                />
              </template>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <nav v-if="totalRow > 0" aria-label="Page navigation">
      <ul class="pagination justify-content-center">
        <li 
          class="page-item" 
          :class="{ disabled: currentPage === 1 }"
        >
          <button 
            class="page-link" 
            @click="changeCurrentPage(currentPage - 1)"
            :disabled="currentPage === 1"
          >
            Previous
          </button>
        </li>
        <li 
          v-for="page in totalPages" 
          :key="page"
          class="page-item" 
          :class="{ active: currentPage === page }"
        >
          <button 
            class="page-link" 
            @click="changeCurrentPage(page)"
          >
            {{ page }}
          </button>
        </li>
        <li 
          class="page-item" 
          :class="{ disabled: currentPage === totalPages }"
        >
          <button 
            class="page-link" 
            @click="changeCurrentPage(currentPage + 1)"
            :disabled="currentPage === totalPages"
          >
            Next
          </button>
        </li>
      </ul>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { 
  ref, 
  reactive, 
  computed, 
  onMounted, 
  watch, 
  PropType 
} from 'vue'
import { useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { useI18n } from 'vue-i18n'

// Interfaces
interface NamedResourceDTO {
  uri: string
  [key: string]: any
}

interface TableField {
  key: string
  label?: string
  isSelect?: boolean
}

interface SearchParams {
  orderBy?: string[]
  currentPage: number
  pageSize: number
}

// Props
const props = defineProps({
  fields: {
    type: Array as PropType<TableField[]>,
    required: true
  },
  fieldKeyToSortableModelLabelMap: {
    type: Object as PropType<Record<string, string>>,
    default: () => ({})
  },
  searchMethod: {
    type: Function,
    required: true
  },
  useQueryParams: {
    type: Boolean,
    default: true
  },
  defaultSortBy: {
    type: String,
    default: 'name'
  },
  defaultSortDesc: {
    type: Boolean,
    default: false
  },
  isSelectable: {
    type: Boolean,
    default: false
  },
  selectMode: {
    type: String as PropType<'multi' | 'single'>,
    default: 'multi'
  },
  labelNumberOfSelectedRow: String,
  iconNumberOfSelectedRow: String,
  showCount: {
    type: Boolean,
    default: true
  },
  maximumSelectedRows: Number,
  selectAllLimit: {
    type: Number,
    default: 10000
  }
})

// Emits
const emit = defineEmits([
  'select', 
  'unselect', 
  'row-selected', 
  'selectall', 
  'refreshed'
])

// Composables
const route = useRoute()
const store = useStore()
const { t, n } = useI18n()

// Refs and Reactive State
const tableRef = ref<HTMLTableElement | null>(null)
const currentPage = ref(1)
const pageSize = ref(20)
const totalRow = ref(0)
const sortBy = ref(props.defaultSortBy)
const sortDesc = ref(props.defaultSortDesc)
const isSearching = ref(false)
const selectAll = ref(false)
const onlySelected = ref(false)

const selectedItems = ref<NamedResourceDTO[]>([])
const tableData = ref<NamedResourceDTO[]>([])

// Computed Properties
const numberOfSelectedRows = computed(() => selectedItems.value.length)
const totalPages = computed(() => Math.ceil(totalRow.value / pageSize.value))

const badgeHelpMessage = computed(() => 
  'component.common.search.badgeHelpMessage'
)

// Methods
function updateSort(fieldKey: string) {
  if (sortBy.value === fieldKey) {
    sortDesc.value = !sortDesc.value
  } else {
    sortBy.value = fieldKey
    sortDesc.value = false
  }
  loadData()
}

function isSelected(item: NamedResourceDTO) {
  return selectedItems.value.some(selectedItem => selectedItem.uri === item.uri)
}

function onRowClicked(item: NamedResourceDTO) {
  const index = selectedItems.value.findIndex(it => item.uri === it.uri)
  
  if (index >= 0) {
    selectedItems.value.splice(index, 1)
    emit('unselect', item)
  } else {
    if (!props.maximumSelectedRows || numberOfSelectedRows.value < props.maximumSelectedRows) {
      selectedItems.value.push(item)
      emit('select', item)
    }
  }

  emit('row-selected', numberOfSelectedRows.value)
}

async function loadData() {
  isSearching.value = true

  try {
    if (onlySelected.value) {
      // Handle only selected items logic
      totalRow.value = selectedItems.value.length
      tableData.value = selectedItems.value.slice(
        (currentPage.value - 1) * pageSize.value, 
        pageSize.value * currentPage.value
      )
    } else {
      // Fetch data from search method
      const orderBy = getOrderBy()
      const response = await props.searchMethod({
        orderBy,
        currentPage: currentPage.value - 1,
        pageSize: pageSize.value
      })

      totalRow.value = response.response.metadata.pagination.totalCount
      tableData.value = response.response.result
    }
  } catch (error) {
    console.error('Error loading data:', error)
  } finally {
    isSearching.value = false
  }
}

function getOrderBy(): string[] {
  if (!sortBy.value) return []

  const orderByText = props.fieldKeyToSortableModelLabelMap[sortBy.value] || sortBy.value
  return [`${orderByText}=${sortDesc.value ? 'desc' : 'asc'}`]
}

function onSelectAll() {
  if (selectAll.value) {
    if (totalRow.value > props.selectAllLimit) {
      alert(t('TableAsyncView.alertSelectAllLimitSize') + props.selectAllLimit)
      selectAll.value = false
      return
    }

    // Fetch and select all items
    props.searchMethod({
      orderBy: getOrderBy(),
      currentPage: 0,
      pageSize: props.selectAllLimit
    }).then(response => {
      const newItems = response.response.result
      newItems.forEach(item => {
        if (!selectedItems.value.some(selected => selected.uri === item.uri)) {
          selectedItems.value.push(item)
        }
      })
      emit('selectall', selectedItems.value)
    })
  } else {
    selectedItems.value = []
    emit('selectall', selectedItems.value)
  }
}

function changeCurrentPage(page: number) {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    loadData()
  }
}

function onNbElementPerPageChange(newPageSize: number) {
  pageSize.value = newPageSize
  currentPage.value = 1
  loadData()
}

function getCurrentItemLimit(): number {
  return n((pageSize.value * (currentPage.value - 1)) < 0 ? 0 : pageSize.value * (currentPage.value - 1))
}

function getCurrentItemOffset(): number {
  return n(pageSize.value * currentPage.value < totalRow.value 
    ? pageSize.value * currentPage.value 
    : totalRow.value)
}

// Lifecycle Hooks
onMounted(() => {
  // Initial data load and setup
  loadData()
})

// Expose methods for external use
defineExpose({
  loadData,
  getSelected: () => selectedItems.value,
  resetSelected: () => {
    selectedItems.value = []
    selectAll.value = false
  }
})
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