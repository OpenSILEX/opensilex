<template>
  <div :class="wrapperClass">
    <n-tree-select
      ref="treeref"
      v-bind="treeSelectBindings"
      @update:value="handleUpdateValue"
      @focus="handleFocus"
      @blur="emitClose"
      @search="onSearchChange"
      @keydown.enter="emitEnter"
      class="naiveTreeselect"
    >
      <!-- contenu en bas du panneau -->
      <template #action>
        <slot name="after-list"></slot>
      </template>
    </n-tree-select>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, onMounted, nextTick } from 'vue'
import { NTreeSelect } from 'naive-ui'
import { debounce } from 'lodash-es'

interface TreeOpt {
  key: string
  label: string
  disabled?: boolean
  children?: TreeOpt[]
}
interface NamedResourceDTO {
  uri: string
  name: string
  isDisabled?: boolean
}

const props = defineProps<{
  selected: string | string[] | undefined
  searchMethod?: (query: string, offset: number, limit: number) => Promise<any>
  multiple?: boolean
  checkable?: boolean
  itemLoadingMethod?: (uris: string[]) => Promise<NamedResourceDTO[]>
  options?: Array<{ id: string; label: string; isDisabled?: boolean }>
  resultLimit?: number
  limit?: number
  optionsLoadingMethod?: () => Promise<NamedResourceDTO[]>
  defaultSelectedValue?: boolean
  placeholder?: string
  actionHandler?: any
  viewHandler?: any
  viewHandlerDetailsVisible?: boolean
  showCount?: boolean
  disabled?: boolean
  conversionMethod?: (dto: NamedResourceDTO) => { id: string; label: string; isDisabled?: boolean }
  disableBranchNodes?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:selected', value: string | string[] | undefined): void
  (e: 'select', value: any): void
  (e: 'deselect', value: any): void
  (e: 'close'): void
  (e: 'clear'): void
  (e: 'totalCount', value: number): void
  (e: 'resultCount', value: number): void
  (e: 'enterKey', value: KeyboardEvent): void
}>()

// state
const treeref = ref<InstanceType<typeof NTreeSelect> | null>(null)
const options = ref<TreeOpt[]>([])          // ← options NaiveUI {label,key}
const value = ref<string | string[] | null>(null) //  keys
const totalCount = ref(-1)
const resultCount = ref(0)

// expose pour FormSelector
function refresh(newLimit?: number) {
  // relance la recherche courante (ou initiale)
  if (lastQuery.value == null) {
    runSearch('.*', newLimit)
  } else {
    runSearch(lastQuery.value, newLimit)
  }
}

function openTreeselect() {
  nextTick(() => {
    (treeref.value as any)?.focus?.()
    ;(treeref.value as any)?.open?.() // au cas où l’API existe
  })
}
defineExpose({ refresh, openTreeselect })

// bindings
const treeSelectBindings = computed(() => ({
  multiple: props.multiple === true,
  checkable: props.checkable,
  options: options.value,
  clearable: true,
  filterable: !!props.searchMethod,
  disabled: props.disabled,
  placeholder: props.placeholder,
  value: value.value,
  'check-strategy': 'child' as const
}))

const wrapperClass = computed(() => ({
  'multiselect-action': props.actionHandler,
  'multiselect-view': typeof props.viewHandler === 'function'
}))

// map helpers
function toTreeOpt(element: { id: string; label: string; isDisabled?: boolean }): TreeOpt {
  return { key: element.id, label: element.label, disabled: element.isDisabled }
}
function fromDTO(dto: NamedResourceDTO): { id: string; label: string; isDisabled?: boolean } {
  if (props.conversionMethod) return props.conversionMethod(dto)
  return { id: dto.uri, label: dto.name, isDisabled: dto.isDisabled }
}

// selection initiale
async function loadSelectedValues() {
  const sel = props.selected
  if (!sel) {
    value.value = null
    return
  }
  const ids = Array.isArray(sel) ? sel : [sel]
  // s’assurer que les options contiennent les sélectionnés pour affichage label
  if (props.itemLoadingMethod) {
    const dtos = await props.itemLoadingMethod(ids)
    const opts = dtos.map(fromDTO).map(toTreeOpt)
    // merge sans doublons
    const keys = new Set(options.value.map(o => o.key))
    opts.forEach(o => { if (!keys.has(o.key)) options.value.push(o) })
  }
  value.value = Array.isArray(sel) ? sel : sel
}

watch(() => props.selected, loadSelectedValues, { immediate: true })

watch(
  () => props.options,
  (opts) => {
    options.value = (opts ?? []).map(toTreeOpt)   // { id,label } -> { key,label }
  },
  { immediate: true } // si datatypesNode est déjà rempli au mount, synchronise, sinon se met à jour dèq que le tableau change via API
)


// recherche
const lastQuery = ref<string | null>(null)

async function runSearch(rawQuery: string, overrideLimit?: number) {
  if (!props.searchMethod) return
  const query = rawQuery === '' ? '.*' : rawQuery
  lastQuery.value = query
  const limit = overrideLimit ?? props.resultLimit ?? 10
  const resp = await props.searchMethod(query, 0, limit)
  const list = resp.response.result as NamedResourceDTO[]
  options.value = list.map(fromDTO).map(toTreeOpt)
  totalCount.value = resp.response.metadata.pagination.totalCount
  resultCount.value = options.value.length
  emit('totalCount', totalCount.value)
  emit('resultCount', resultCount.value)
}


const debounceSearch = debounce(runSearch, 250)
function onSearchChange(q: string) {
  debounceSearch(q)
}

function handleFocus() {
  // charger au focus si pas d’options
  if (!options.value.length) runSearch('.*')
}

function emitEnter(e: KeyboardEvent) {
  emit('enterKey', e)
}

// mise à jour de la valeur (keys)
function handleUpdateValue(v: string | string[] | null) {
  value.value = v
  emit('update:selected', v ?? (props.multiple ? [] : undefined))
  // event “select” conservé pour compat
  emit('select', v)
}

onMounted(async () => {
  // options côté props.options (compat)
  if (props.options?.length) {
    options.value = props.options.map(toTreeOpt)
  }
  // options via optionsLoadingMethod (static/initiale)
  if (props.optionsLoadingMethod && !props.searchMethod) {
    const dtos = await props.optionsLoadingMethod()
    options.value = dtos.map(fromDTO).map(toTreeOpt)
    if (props.defaultSelectedValue) {
      emit('select', options.value.map(o => o.key))
    }
  }
})
</script>

<style scoped>
.naiveTreeselect { width: 100%; }                 /* composant racine */
:deep(.n-base-selection) { width: 100%; }        /* l’input visible */
:deep(.n-tree-select) { width: 100%; }           /* conteneur Naive UI */

</style>
