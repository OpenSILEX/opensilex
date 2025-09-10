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
            class="chip-area"
            :class="{ clickable: openOnClick && !disabled }"
            @click="showModal"
          >
            <template v-if="displayed.length">
              <n-tag
                v-for="item in displayed"
                :key="item.id"
                closable
                size="small"
                @close.stop="onChipClose(item)"
              >
                <span class="label" :title="item.label">{{ item.label }}</span>
              </n-tag>
              <n-tag v-if="extraCount > 0" size="small">+{{ extraCount }}</n-tag>
            </template>

            <span v-else class="modalFormSelectorPlaceholder">{{ t(placeholder) }}</span>
          </div>

          <n-button
            size="small"
            class="greenThemeColor"
            :disabled="disabled"
            @click="showModal"
          >
            {{ t('component.common.select') }}
          </n-button>

          <n-button
            v-if="clearable && !disabled && hasSelection"
            quaternary
            size="small"
            @click="clearAll"
          >
            {{ t('component.common.clear') }}
          </n-button>
        </div>
      </n-spin>

      <!-- Composant modal dynamique -->
      <component
        ref="searchModal"
        :is="modalComponent"
        v-bind="modalComponentProps"
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
import { computed, inject, onMounted, ref, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { NButton, NSpin, NTag } from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'

/** ----- Types ----- */
export interface SelectableItem {
  id: string
  label: string
  isDisabled?: boolean
}
type NamedResourceDTO = { uri: string; name: string; isDisabled?: boolean }

/** ----- i18n / plugin ----- */
const { t } = useI18n()
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')

/** ----- Props / Emits ----- */
const props = defineProps<{
  /** v-model:selected : tableau d’URIs (multiple=true) ou string (multiple=false) */
  selected: string[] | string | undefined
  /** liste d’objets { uri, name } (ou { id, label }) pour préremplir les libellés */
  selectedInJsonFormat?: Array<{ uri?: string; name?: string; id?: string; label?: string }> | null

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

  /** v-model:filter -> renommé v-model:searchFilter côté modal */
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

  // VariablesSelectorWithFilter props
  withAssociatedData?: boolean
  experiment?: any
  objects?: any
  devices?: any
}>()

const emit = defineEmits<{
  (e: 'update:selected', value: string[] | string | undefined): void
  (e: 'update:filter', value: any): void
  (e: 'update:selectedInJsonFormat', value: Array<{ uri: string; name: string }> | null): void
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

/** ----- Valeurs par défaut ----- */
const multiple = computed(() => props.multiple !== false)
const clearable = computed(() => props.clearable !== false)
const openOnClick = computed(() => props.openOnClick !== false)
const limit = computed(() => (props.limit ?? 4))

/** ----- Réfs internes ----- */
const searchModal = ref<any>(null)
const loading = ref(false)
const firstTimeOpening = ref(false)

/** Sélection côté form : copie (confirmée) & temporaire (dans la modale) */
const selectedCopie = ref<SelectableItem[]>([])
const selectedTmp   = ref<SelectableItem[]>([])

/** ----- v-model:selected (URIs) ----- */
const selectionModel = computed<string[] | string | undefined>({
  get: () => props.selected,
  set: (val) => emit('update:selected', val)
})

/** v-model:filter -> v-model:searchFilter (nommage côté modal) */
const searchModalFilterModel = computed({
  get: () => props.filter,
  set: (v) => emit('update:filter', v)
})

/** ----- Conversion par défaut (DTO -> SelectableItem) ----- */
const conversion = (dto: any): SelectableItem => {
  if (typeof props.conversionMethod === 'function') {
    return props.conversionMethod(dto as any)
  }
  if (dto && (dto as NamedResourceDTO).uri && (dto as NamedResourceDTO).name) {
    return { id: dto.uri, label: dto.name, isDisabled: (dto as NamedResourceDTO).isDisabled ?? false }
  }
  return dto as SelectableItem
}

/** aide : convertir vers la forme attendue par la modale */
const toModalRow = (it: SelectableItem) => ({ uri: it.id, name: it.label })

/** ----- Hidden input (compat) ----- */
const hiddenValue = computed(() => {
  if (multiple.value && Array.isArray(selectionModel.value)) {
    return selectionModel.value.length ? selectionModel.value.join(',') : ''
  } else {
    return (selectionModel.value as string) || ''
  }
})

/** ----- Affichage tags avec limite ----- */
const displayed = computed(() => selectedCopie.value.slice(0, limit.value))
const extraCount = computed(() => Math.max(0, selectedCopie.value.length - displayed.value.length))
const hasSelection = computed(() => selectedCopie.value.length > 0)

/** ----- Préremplissage libellés ----- */
function preloadFromJson() {
  if (props.selectedInJsonFormat && props.selectedInJsonFormat.length) {
    const normalized: SelectableItem[] = props.selectedInJsonFormat.map((it: any) => {
      if (it.id && it.label) return { id: it.id, label: it.label }
      return { id: it.uri, label: it.name }
    })
    selectedTmp.value = normalized.slice()
    selectedCopie.value = normalized.slice()
  }
}

/** Optionnel : charger les labels via itemLoadingMethod si on n’a que des URIs */
async function preloadFromItemLoaderIfNeeded() {
  if (!props.itemLoadingMethod) return
  const current = selectionModel.value
  const uris: string[] = multiple.value
    ? (Array.isArray(current) ? current : [])
    : (current ? [current as string] : [])

  if (!uris.length) return
  try {
    $opensilex?.disableLoader()
    const items = await props.itemLoadingMethod(uris)
    const normalized = items.map(conversion)
    selectedTmp.value = normalized.slice()
    selectedCopie.value = normalized.slice()
  } catch (e) {
    $opensilex?.errorHandler(e)
  } finally {
    $opensilex?.enableLoader()
  }
}

onMounted(async () => {
  preloadFromJson()
  if (!selectedCopie.value.length) {
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

/** ----- Sync modal <-> sélection ----- */
function updateModal() {
  // désélectionner dans la modale ce qui est dans tmp mais pas dans copie
  const toUnselect = selectedTmp.value.filter(x => !selectedCopie.value.some(el => el.id === x.id))
  toUnselect.forEach(it => searchModal.value?.unSelect?.(toModalRow(it)))

  // re-sélectionner ce qui est dans copie mais pas dans tmp
  const toReselect = selectedCopie.value.filter(x => !selectedTmp.value.some(el => el.id === x.id))
  toReselect.forEach(it => searchModal.value?.selectItem?.(toModalRow(it)))

  // réaligner tmp sur copie
  selectedTmp.value = selectedCopie.value.slice()
}

/** Ouvrir la modale */
async function showModal () {
  // 1) partir de la sélection confirmée
  selectedTmp.value = selectedCopie.value.slice()

  // 2) ouvrir la modale d’abord (pour que VariableList existe vraiment)
  searchModal.value?.show?.()
  await nextTick() // laisse NModal/VariableList se monter

  // 3) pousser la sélection initiale (format { uri, name })
  const json = selectedCopie.value.map(v => ({ uri: v.id, name: v.label }))
  searchModal.value?.setInitiallySelectedItems?.(json)

  // 4) demander un refresh conservant la sélection
  searchModal.value?.refreshWithKeepingSelection?.()

  // 5) attendre que la page ait (re)chargé, puis re-cocher les cases visibles
  await nextTick()
  // micro-delay utile si fetchVariablesPage est async
  await new Promise(r => setTimeout(r, 0))
  searchModal.value?.applySelectionToPage?.()
}



/** Clear / remove */
function clearSelectedModal() {
  selectedTmp.value.forEach((it) => searchModal.value?.unSelect?.(toModalRow(it)))
  selectedTmp.value = []
  selectedCopie.value = []
}
function clearAll() {
  if (multiple.value) {
    emit('update:selected', [])
  } else {
    emit('update:selected', undefined)
  }
  clearSelectedModal()
  emit('update:selectedInJsonFormat', null)
  emit('clear')
}

function syncSelectedJsonFormat() {
  const json = selectedCopie.value.length
    ? selectedCopie.value.map(v => ({ uri: v.id, name: v.label }))
    : null
  emit('update:selectedInJsonFormat', json)
}

/** tag “x” */
function onChipClose(item: SelectableItem) {
  // retirer de la sélection confirmée + temporaire
  selectedCopie.value = selectedCopie.value.filter(v => v.id !== item.id)
  selectedTmp.value   = selectedTmp.value.filter(v => v.id !== item.id)

  // MAJ v-model:selected
  if (multiple.value) {
    const ids = (Array.isArray(selectionModel.value) ? selectionModel.value : []).filter(id => id !== item.id)
    emit('update:selected', ids)
  } else {
    emit('update:selected', undefined)
  }

  // prévenir la modale (pour la prochaine ouverture)
  searchModal.value?.unSelect?.(toModalRow(item))

  // miroir JSON
  syncSelectedJsonFormat()

  emit('deselect', item)
}

/** Événements de la modale */
function onSelect(item: SelectableItem) {
  // éviter les doublons
  if (!selectedTmp.value.some(v => v.id === item.id)) {
    selectedTmp.value.push(item)
  }

  if (multiple.value) {
    const ids = new Set<string>(Array.isArray(selectionModel.value) ? selectionModel.value : [])
    ids.add(item.id)
    emit('update:selected', Array.from(ids))
  } else {
    emit('update:selected', item.id)
  }

  emit('select', item, selectedTmp.value)
}

function onModalComponentUnselect(item: SelectableItem) {
  // retirer de tmp
  selectedTmp.value = selectedTmp.value.filter(v => v.id !== item.id)
  // MAJ v-model:selected
  if (multiple.value) {
    const ids = (Array.isArray(selectionModel.value) ? selectionModel.value : []).filter(id => id !== item.id)
    emit('update:selected', ids)
  } else {
    emit('update:selected', undefined)
  }
  emit('deselect', item)
}

function onSelectAll(items: any[] | undefined) {
  if (!items) {
    selectedTmp.value = []
    return
  }
  const converted = items.map(conversion).filter(it => !!it?.label)
  // merge unique par id
  const byId = new Map(selectedTmp.value.map(v => [v.id, v]))
  for (const it of converted) byId.set(it.id, it)
  selectedTmp.value = Array.from(byId.values())

  if (multiple.value) {
    const ids = new Set<string>(Array.isArray(selectionModel.value) ? selectionModel.value : [])
    for (const it of converted) ids.add(it.id)
    emit('update:selected', Array.from(ids))
  } else if (converted[0]) {
    emit('update:selected', converted[0].id)
  }
}

/** Validation (fermeture modale) */
function onValidate() {
  loading.value = selectedTmp.value.length > 0

  // dédoublonner & bloquer la sélection confirmée
  const byId = new Map(selectedTmp.value.map(v => [v.id, v]))
  selectedCopie.value = Array.from(byId.values())

  // sync v-model:selected (URIs)
  const finalIds = selectedCopie.value.map(v => v.id)
  emit('update:selected', multiple.value ? finalIds : finalIds[0])

  // miroir JSON pour le parent (variablesWithLabels)
  syncSelectedJsonFormat()

  setTimeout(() => {
    emit('onValidate', selectedCopie.value)
    loading.value = false
  }, 0)
}

/** Ouverture de la modale (événement) */
function onModalSearchShown() {
  emit('shown')
  searchModal.value?.refreshWithKeepingSelection?.()
}

defineExpose({
  setSelectorToFirstTimeOpen,
  refreshModalSearch,
  refresh,
  show: showModal,
  selectItem: (it: SelectableItem) => searchModal.value?.selectItem?.(toModalRow(it)),
  unSelect: (it: SelectableItem) => searchModal.value?.unSelect?.(toModalRow(it))
})
</script>

<style scoped>
.select-button-container {
  display: flex;
  align-items: center;
  gap: .5rem;
}

.chip-area {
  flex: 1;
  min-height: 36px;
  padding: 6px 8px;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}
.clickable { cursor: pointer; }

.label {
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
  max-width: 220px;
}

.modalFormSelectorPlaceholder {
  opacity: 0.5;
}
</style>
