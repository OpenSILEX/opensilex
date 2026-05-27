<template>
  <opensilex-FormField
    :label="label"
    :helpMessage="helpMessage"
    :required="required"
  >
    <template #field="{ id }">
      <n-spin :show="loading">
        <input :id="id" type="hidden" :value="hiddenValue" />

        <div class="select-button-container">
          <!-- Zone d’affichage des valeurs sélectionnées -->
          <div
            class="selectedItemsArea"
            :class="{ clickable: openOnClick && !isDisabled }"
            @click="showModal"
          >
            <template v-if="displayedSelectedItems.length">
              <n-tag
                v-for="item in displayedSelectedItems"
                :key="item.id"
                closable
                size="small"
                @close.stop="onSelectedItemTagClose(item)"
              >
                <span class="label" :title="item.label">
                  {{ item.label }}
                </span>
              </n-tag>

              <n-tag v-if="remainingSelectedItemsCount > 0" size="small">
                +{{ remainingSelectedItemsCount }}
              </n-tag>
            </template>

            <span v-else class="modalFormSelectorPlaceholder">
              {{ t(placeholder) }}
            </span>
          </div>

          <n-button
            v-if="clearable && !disabled && hasSelection"
            quaternary
            size="small"
            @click.stop="clearAll"
          >
            <i class="bi bi-x-lg"></i>
          </n-button>
        </div>
      </n-spin>

      <component
        ref="searchModal"
        :is="modalComponent"
        v-bind="modalComponentProps"
        :selected="confirmedSelectedItems"
        :maximumSelectedRows="maximumSelectedItems"
        v-model:searchFilter="searchModalFilterModel"
        :withAssociatedData="withAssociatedData"
        :experiment="experiment"
        :objects="objects"
        :devices="devices"
        @onClose="$emit('onClose')"
        @close="$emit('close')"
        @onValidate="onValidate"
        @shown="onModalSearchShown"
        @clear="$emit('clear')"
        @select="onSelect(conversion($event))"
        @unselect="onModalComponentUnselect(conversion($event))"
        @selectall="onSelectAll"
        @hide="$emit('hide')"
        class="isModalSearchComponent"
      />
    </template>
  </opensilex-FormField>
</template>

<script setup lang="ts">
import { computed, inject, nextTick, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { NButton, NSpin, NTag } from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'

export interface SelectableItem {
  id: string
  label: string
  isDisabled?: boolean
}

type NamedResourceDTO = {
  uri: string
  name: string
  isDisabled?: boolean
}

type SelectedJsonItem = {
  uri?: string
  name?: string
  id?: string
  label?: string
  isDisabled?: boolean
}

const props = defineProps<{
  /** v-model:selected : tableau d’URIs (multiple=true) ou string (multiple=false) */
  selected: string[] | string | undefined
  /** liste d’objets { uri, name } (ou { id, label }) pour préremplir les libellés */
  selectedInJsonFormat?: SelectedJsonItem[] | null

  multiple?: boolean
  itemLoadingMethod?: (uris: Array<string | { uri: string }>) => Promise<any[]>
  optionsLoadingMethod?: any
  options?: any
  searchMethod?: (query: any, page: number, limit: number) => Promise<any>

  clearable?: boolean
  openOnClick?: boolean
  showCount?: boolean

  modalComponent: string | any
  modalComponentProps?: Record<string, any>

  filter?: any

  conversionMethod?: (dto: NamedResourceDTO | SelectableItem) => SelectableItem

  label?: string
  helpMessage?: string
  placeholder?: string
  noResultsText?: string

  required?: boolean
  disabled?: boolean
  rules?: string | Function

  flat?: boolean
  actionHandler?: any
  viewHandler?: any
  viewHandlerDetailsVisible?: boolean

  resultLimit?: number
  defaultSelectedValue?: any
  maximumSelectedItems?: number
  /** nb max de tags affichés dans l’input avant “+N” */
  limit?: number

  /**
   * Props spécifiques à certaines modales,
   * notamment VariablesSelectorWithFilter.
   */
  withAssociatedData?: boolean
  experiment?: any
  objects?: any
  devices?: any
}>()

const emit = defineEmits<{
  (e: 'update:selected', value: string[] | string | undefined): void
  (e: 'update:filter', value: any): void
  (e: 'update:selectedInJsonFormat', value: Array<{ uri: string; name: string; isDisabled?: boolean }> | null): void
  (e: 'select', item: SelectableItem, all?: SelectableItem[]): void
  (e: 'deselect', item: SelectableItem): void
  (e: 'onValidate', items: SelectableItem[]): void
  (e: 'handlingEnterKey'): void
  (e: 'shown'): void
  (e: 'hide'): void
  (e: 'close'): void
  (e: 'onClose'): void
  (e: 'clear'): void
}>()

const { t } = useI18n()
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')

const multiple = computed(() => props.multiple !== false)
const clearable = computed(() => props.clearable !== false)
const openOnClick = computed(() => props.openOnClick !== false)
const limit = computed(() => props.limit ?? 4)
const isDisabled = computed(() => props.disabled === true)

const searchModal = ref<any>(null)
const loading = ref(false)
const firstTimeOpening = ref(false)

/** Sélection côté form : copie (confirmée) & temporaire (dans la modale) */
const confirmedSelectedItems = ref<SelectableItem[]>([])
const temporarySelectedItems = ref<SelectableItem[]>([])

/** ----- v-model:selected (URIs) ----- */
const selectionModel = computed<string[] | string | undefined>({
  get: () => props.selected,
  set: value => emit('update:selected', value)
})

/** v-model:filter -> v-model:searchFilter (nommage côté modal) */
const searchModalFilterModel = computed({
  get: () => props.filter,
  set: value => emit('update:filter', value)
})

/** ----- Hidden input (compat) ----- */
const hiddenValue = computed(() => {
  if (multiple.value && Array.isArray(selectionModel.value)) {
    return selectionModel.value.length ? selectionModel.value.join(',') : ''
  }

  return (selectionModel.value as string) || ''
})

/**
 * Liste des éléments réellement affichés en selecteur.
 * Si la sélection contient plus d’éléments que la limite,
 * on affiche seulement les premiers.
 */
const displayedSelectedItems = computed(() => confirmedSelectedItems.value.slice(0, limit.value))

/**
 * Nombre d’éléments non affichés car excedant la limite.
 * Si 7 elements, limit = 4 et remainingSelectedItemsCount = 3
 * On affiche alors "+3".
 */
const remainingSelectedItemsCount = computed(() => Math.max(0, confirmedSelectedItems.value.length - displayedSelectedItems.value.length))

const hasSelection = computed(() => confirmedSelectedItems.value.length > 0)

function normalizeSelectableItem(item: any): SelectableItem | null {
  if (!item) {
    return null
  }

  if (item.id && item.label) {
    return {
      id: item.id,
      label: item.label,
      isDisabled: item.isDisabled ?? false
    }
  }

  if (item.uri && item.name) {
    return {
      id: item.uri,
      label: item.name,
      isDisabled: item.isDisabled ?? false
    }
  }

  if (typeof item === 'string') {
    return {
      id: item,
      label: item
    }
  }

  return null
}

function conversion(dto: any): SelectableItem {
  if (typeof props.conversionMethod === 'function') {
    return props.conversionMethod(dto as any)
  }

  const normalized = normalizeSelectableItem(dto)

  if (normalized) {
    return normalized
  }

  return dto as SelectableItem
}

/**
 * Synchronise le v-model:selected du parent à partir d’une liste d’items.
 * En mode multiple : émet un tableau d’ids.
 * En mode simple : émet le premier id trouvé.
 */
function syncSelectionModelFromItems(items: SelectableItem[]) {
  const ids = items.map(item => item.id)

  if (multiple.value) {
    emit('update:selected', ids)
  } else {
    emit('update:selected', ids[0])
  }
}

function syncSelectedJsonFormat() {
  emit(
    'update:selectedInJsonFormat',
    confirmedSelectedItems.value.length
      ? confirmedSelectedItems.value.map(item => ({
          uri: item.id,
          name: item.label,
          isDisabled: item.isDisabled
        }))
      : null
  )
}

function toModalRow(item: SelectableItem) {
  return {
    uri: item.id,
    name: item.label,
    isDisabled: item.isDisabled
  }
}

function setSelection(items: SelectableItem[]) {
  const byId = new Map<string, SelectableItem>()

  items.forEach(item => {
    if (item?.id) {
      byId.set(item.id, item)
    }
  })

  confirmedSelectedItems.value = Array.from(byId.values())
  temporarySelectedItems.value = confirmedSelectedItems.value.slice()

  syncSelectionModelFromItems(confirmedSelectedItems.value)
  syncSelectedJsonFormat()
}

function preloadFromJson() {
  const list = props.selectedInJsonFormat

  if (!list || !list.length) {
    return false
  }

  const normalized = list
    .map(normalizeSelectableItem)
    .filter((item): item is SelectableItem => !!item)

  confirmedSelectedItems.value = normalized.slice()
  temporarySelectedItems.value = normalized.slice()

  return normalized.length > 0
}

/** Optionnel : charger les labels via itemLoadingMethod si on n’a que des URIs */
async function preloadFromItemLoaderIfNeeded() {
  if (!props.itemLoadingMethod) {
    return
  }

  const current = selectionModel.value
  const uris: string[] = multiple.value
    ? Array.isArray(current) ? current : []
    : current ? [current as string] : []

  if (!uris.length) {
    return
  }

  try {
    $opensilex?.disableLoader()

    const items = await props.itemLoadingMethod(uris)
    const normalized = items
      .map(conversion)
      .filter((item): item is SelectableItem => !!item?.id)

    confirmedSelectedItems.value = normalized.slice()
    temporarySelectedItems.value = normalized.slice()
    syncSelectedJsonFormat()
  } catch (error) {
    $opensilex?.errorHandler(error)
  } finally {
    $opensilex?.enableLoader()
  }
}

onMounted(async () => {
  const hasJsonSelection = preloadFromJson()

  if (!hasJsonSelection) {
    await preloadFromItemLoaderIfNeeded()
  }
})

function setSelectorToFirstTimeOpen() {
  firstTimeOpening.value = true
}

function refreshModalSearch() {
  searchModal.value?.refresh?.()
}

function refresh() {
  searchModal.value?.refresh?.()
}

async function showModal() {
  console.log('showModal called', {
    disabled: props.disabled,
    isDisabled: isDisabled.value,
    openOnClick: openOnClick.value,
    searchModal: searchModal.value
  })

  if (isDisabled.value) {
    return
  }

  temporarySelectedItems.value = confirmedSelectedItems.value.slice()

  searchModal.value?.show?.()

  await nextTick()

  const json = confirmedSelectedItems.value.map(toModalRow)

  searchModal.value?.setInitiallySelectedItems?.(json)
  searchModal.value?.refreshWithKeepingSelection?.()

  await nextTick()
  await new Promise(resolve => setTimeout(resolve, 0))

  searchModal.value?.applySelectionToPage?.()
}

function clearSelectedModal() {
  temporarySelectedItems.value.forEach(item => {
    searchModal.value?.unSelect?.(toModalRow(item))
  })

  temporarySelectedItems.value = []
  confirmedSelectedItems.value = []
}

function clearAll() {
  clearSelectedModal()

  if (multiple.value) {
    emit('update:selected', [])
  } else {
    emit('update:selected', undefined)
  }

  emit('update:selectedInJsonFormat', null)
  emit('clear')
}

function onSelectedItemTagClose(item: SelectableItem) {
  confirmedSelectedItems.value = confirmedSelectedItems.value.filter(value => value.id !== item.id)
  temporarySelectedItems.value = temporarySelectedItems.value.filter(value => value.id !== item.id)

  syncSelectionModelFromItems(confirmedSelectedItems.value)
  syncSelectedJsonFormat()

  searchModal.value?.unSelect?.(toModalRow(item))

  emit('deselect', item)
}

function onSelect(item: SelectableItem) {
  if (!item?.id) {
    return
  }

  if (!temporarySelectedItems.value.some(value => value.id === item.id)) {
    temporarySelectedItems.value.push(item)
  }

  if (multiple.value) {
    const ids = new Set<string>(Array.isArray(selectionModel.value) ? selectionModel.value : [])
    ids.add(item.id)
    emit('update:selected', Array.from(ids))
  } else {
    emit('update:selected', item.id)
  }

  emit('select', item, temporarySelectedItems.value)
}

function onModalComponentUnselect(item: SelectableItem) {
  if (!item?.id) {
    return
  }

  temporarySelectedItems.value = temporarySelectedItems.value.filter(value => value.id !== item.id)

  if (multiple.value) {
    const ids = (Array.isArray(selectionModel.value) ? selectionModel.value : [])
      .filter(id => id !== item.id)

    emit('update:selected', ids)
  } else {
    emit('update:selected', undefined)
  }

  emit('deselect', item)
}

function onSelectAll(items: any[] | undefined) {
  if (!items) {
    temporarySelectedItems.value = []
    return
  }

  const converted = items
    .map(conversion)
    .filter((item): item is SelectableItem => !!item?.id)

  const byId = new Map(temporarySelectedItems.value.map(item => [item.id, item]))

  converted.forEach(item => {
    byId.set(item.id, item)
  })

  temporarySelectedItems.value = Array.from(byId.values())

  if (multiple.value) {
    const ids = new Set<string>(Array.isArray(selectionModel.value) ? selectionModel.value : [])

    converted.forEach(item => {
      ids.add(item.id)
    })

    emit('update:selected', Array.from(ids))
  } else if (converted[0]) {
    emit('update:selected', converted[0].id)
  }
}

function onValidate(items?: any[]) {
  loading.value = temporarySelectedItems.value.length > 0

  if (items && Array.isArray(items) && items.length) {
    const converted = items
      .map(conversion)
      .filter((item): item is SelectableItem => !!item?.id)

    const byId = new Map(temporarySelectedItems.value.map(item => [item.id, item]))

    converted.forEach(item => {
      byId.set(item.id, item)
    })

    temporarySelectedItems.value = Array.from(byId.values())
  }

  setSelection(temporarySelectedItems.value)
  // Laisse Vue propager les update:selected et update:selectedInJsonFormat
  // avant d’émettre onValidate.
  setTimeout(() => {
    emit('onValidate', confirmedSelectedItems.value)
    loading.value = false
  }, 0)
}

function updateModal() {
  const toUnselect = temporarySelectedItems.value.filter(
    item => !confirmedSelectedItems.value.some(selected => selected.id === item.id)
  )

  toUnselect.forEach(item => {
    searchModal.value?.unSelect?.(toModalRow(item))
  })

  const toReselect = confirmedSelectedItems.value.filter(
    item => !temporarySelectedItems.value.some(selected => selected.id === item.id)
  )

  toReselect.forEach(item => {
    searchModal.value?.selectItem?.(toModalRow(item))
  })

  temporarySelectedItems.value = confirmedSelectedItems.value.slice()
}

async function onModalSearchShown() {
  emit('shown')

  const json = confirmedSelectedItems.value.map(toModalRow)

  searchModal.value?.setInitiallySelectedItems?.(json)
  searchModal.value?.refreshWithKeepingSelection?.()

  await nextTick()

  temporarySelectedItems.value = confirmedSelectedItems.value.slice()
  updateModal()

  await new Promise(resolve => setTimeout(resolve, 0))

  updateModal()
  searchModal.value?.applySelectionToPage?.()
}

watch(
  () => props.selectedInJsonFormat,
  list => {
    if (!list || !list.length) {
      if (!props.selected || (Array.isArray(props.selected) && props.selected.length === 0)) {
        temporarySelectedItems.value = []
        confirmedSelectedItems.value = []
      }

      return
    }

    const normalized = list
      .map(normalizeSelectableItem)
      .filter((item): item is SelectableItem => !!item)

    const before = confirmedSelectedItems.value.map(item => item.id).join('|')
    const after = normalized.map(item => item.id).join('|')

    if (before !== after) {
      temporarySelectedItems.value = normalized.slice()
      confirmedSelectedItems.value = normalized.slice()
    }
  },
  { immediate: true, deep: true }
)


watch(
  () => props.selected,
  async value => {
    const isEmpty = multiple.value
      ? !Array.isArray(value) || value.length === 0
      : !value

    if (isEmpty) {
      temporarySelectedItems.value = []
      confirmedSelectedItems.value = []
      return
    }

    if (!props.selectedInJsonFormat?.length) {
      await preloadFromItemLoaderIfNeeded()
    }
  },
  { deep: true }
)


defineExpose({
  setSelectorToFirstTimeOpen,
  refreshModalSearch,
  refresh,
  show: showModal,
  selectItem: (item: SelectableItem) => searchModal.value?.selectItem?.(toModalRow(item)),
  unSelect: (item: SelectableItem) => searchModal.value?.unSelect?.(toModalRow(item))
})
</script>

<style scoped>
.select-button-container {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.selectedItemsArea {
  flex: 1;
  min-width: 0;
  min-height: 36px;
  padding: 6px 8px;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.clickable {
  cursor: pointer;
}

.label {
  display: inline-block;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
  max-width: 220px;
  vertical-align: bottom;
}

.modalFormSelectorPlaceholder {
  opacity: 0.5;
}
</style>