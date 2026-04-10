<template>
  <div :class="wrapperClass">
    <n-tree-select
      ref="treeref"
      v-bind="treeSelectBindings"
      @update:value="handleUpdateValue"
      @focus="handleFocus"
      @blur="emitClose"
      @update:pattern="onSearchChange"
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
import { ref, watch, computed, onMounted, nextTick, useAttrs } from 'vue'
import { NTreeSelect } from 'naive-ui'
import { debounce } from 'lodash-es'

interface TreeOpt {
  key: string
  label: string
  disabled?: boolean
  children?: TreeOpt[]
}

type InputOpt = {
  id: string
  label: string
  isDisabled?: boolean
  children?: InputOpt[]
  isDefaultExpanded?: boolean
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
  checkStrategy?: 'all' | 'parent' | 'child'
  itemLoadingMethod?: (uris: string[]) => Promise<NamedResourceDTO[]>
  options?: InputOpt[]  
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
  checkStrategy?: 'all' | 'parent' | 'child'
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

const attrs = useAttrs()

// filterable: si l’appelant le force via $attrs, il gagne.
// sinon: filterable seulement si searchMethod
const isFilterable = computed(() => {
  const attrVal = (attrs as any).filterable
  if (typeof attrVal === 'boolean') return attrVal
  if (attrVal === '' || attrVal === 'true') return true
  if (attrVal === 'false') return false
  return !!props.searchMethod
})

// state
const treeref = ref<InstanceType<typeof NTreeSelect> | null>(null)
const options = ref<TreeOpt[]>([])                 // options Naive {key,label}
const value = ref<string | string[] | null>(null)  // keys
const totalCount = ref(-1)
const resultCount = ref(0)

const selectedCache = new Map<string, TreeOpt>()  // key -> option (label)

// expose pour FormSelector
function refresh(newLimit?: number) {
  if (lastQuery.value == null) {
    runSearch('.*', newLimit)
  } else {
    runSearch(lastQuery.value, newLimit)
  }
}
function openTreeselect() {
  nextTick(() => {
    (treeref.value as any)?.focus?.();
    (treeref.value as any)?.open?.()
  })
}
defineExpose({ refresh, openTreeselect })

// bindings
const treeSelectBindings = computed(() => ({
  multiple: props.multiple === true,
  checkable: props.checkable,
  options: options.value,
  clearable: true,
  filterable: isFilterable.value,
  disabled: props.disabled,
  placeholder: props.placeholder,
  value: value.value,
  'check-strategy': props.checkStrategy ?? 'child',
  'default-expanded-keys': defaultExpandedKeys.value,   // <— optionnel
  'default-expand-all': true
}))

const wrapperClass = computed(() => ({
  'multiselect-action': props.actionHandler,
  'multiselect-view': typeof props.viewHandler === 'function'
}))

// map helpers
// --- helpers : conversion récursive -> TreeOpt (on garde l’arbre !)
function toTreeOpt(el: InputOpt): TreeOpt {
  const node: TreeOpt = {
    key: el.id,
    label: el.label,
    disabled: el.isDisabled
  }
  if (Array.isArray(el.children) && el.children.length) {
    node.children = el.children.map(toTreeOpt)
    // si on veut empêcher la sélection des parents :
    if (props.disableBranchNodes) node.disabled = true
  }
  return node
}

function fromDTO(dto: NamedResourceDTO): { id: string; label: string; isDisabled?: boolean } {
  if (props.conversionMethod) return props.conversionMethod(dto)
  return { id: dto.uri, label: dto.name, isDisabled: dto.isDisabled }
}

// retrouver un TreeOpt par key
function findOptionByKey(key: string, list: TreeOpt[] = options.value): TreeOpt | undefined {
  for (const n of list) {
    if (n.key === key) return n
    if (n.children?.length) {
      const hit = findOptionByKey(key, n.children)
      if (hit) return hit
    }
  }
  return undefined
}

 function cacheSelectedOption(opt: TreeOpt | undefined) {
   if (opt) selectedCache.set(opt.key, opt)
 }
 function cacheSelectedKeys(keys: string | string[] | null) {
   const arr = Array.isArray(keys) ? keys : (keys ? [keys] : [])
   for (const k of arr) {
     const opt = findOptionByKey(k)
     if (opt) cacheSelectedOption(opt)
   }
 }

// conversion key(s) -> objet(s) { id, label }
function keysToObjects(keys: string | string[] | null): any {
  if (keys == null) return null
  if (Array.isArray(keys)) {
    return keys.map(key => {
      const object = findOptionByKey(key)
      return object ? { id: object.key, label: object.label } : { id: key, label: String(key) }
    })
  } else {
    const object = findOptionByKey(keys)
    return object ? { id: object.key, label: object.label } : { id: keys, label: String(keys) }
  }
}

function normalizeSelectedToIds(selectedElements: string | string[] | undefined): string[] {
  if (selectedElements == null) return []
  const selectionArray = Array.isArray(selectedElements) ? selectedElements : [selectedElements]
  return selectionArray
    .map(s => (typeof s === 'string' ? s.trim() : ''))
    .filter(s => s.length > 0)
}

// sélection initiale
async function loadSelectedValues() {
const sel = props.selected
const ids = normalizeSelectedToIds(sel)
// si aucune sélection réelle, on ne call pas itemLoadingMethod
if (ids.length === 0) {
    value.value = null
    return
  }
  if (props.itemLoadingMethod) {
    const dtos = await props.itemLoadingMethod(ids)
  //  const opts = dtos
  //    .map(fromDTO)
  //    .filter((object): object is { id: string; label: string; isDisabled?: boolean } => !!object)
  //    .map(toTreeOpt)


const opts = dtos
     .map((dto, i) => {
       const o = fromDTO(dto)
       o.id = ids[i] ?? o.id
       return o
     })
     .filter(Boolean)
     .map(toTreeOpt)

      opts.forEach(object => {
        const exists = !!findOptionByKey(object.key, options.value)
        if (!exists) options.value.push(object)
        cacheSelectedOption(object)
      })
  }
   // on répercute la sélection normalisée
  value.value = props.multiple ? ids : ids[0]
}

watch(() => props.selected, loadSelectedValues, { immediate: true })

// sync options depuis props.options
// --- watcher options : NE PAS aplatir
watch(
  () => props.options,
  (opts) => {
    options.value = (opts ?? []).map(toTreeOpt)
  },
  { immediate: true }
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
  const newOptions = list.map(fromDTO).map(toTreeOpt)

  // Conserver les options actuellement sélectionnées si elles ne sont pas dans les résultats
 const selectedKeys = new Set(
   Array.isArray(value.value) ? value.value : (value.value ? [value.value] : [])
 )
 const keysInNew = new Set(newOptions.map(o => o.key))
 for (const k of selectedKeys) {
  if (!keysInNew.has(k)) {
     const cached = selectedCache.get(k) || findOptionByKey(k) || { key: k, label: String(k) }
     newOptions.push(cached)
   }
 }


  // options.value = list.map(fromDTO).map(toTreeOpt)
  options.value = newOptions
  totalCount.value = resp.response.metadata.pagination.totalCount

  // await ensureSelectedVisible()


  resultCount.value = options.value.length
  emit('totalCount', totalCount.value)
  emit('resultCount', resultCount.value)
}
const debounceSearch = debounce(runSearch, 250)

// si pas de searchMethod: Naive filtre localement -> ne rien faire
function onSearchChange(q: string) {
  if (props.searchMethod) debounceSearch(q)
}

function handleFocus() {
  // if (!options.value.length) runSearch('.*')
    runSearch(lastQuery.value ?? '.*')
}
function emitEnter(e: KeyboardEvent) { emit('enterKey', e) }

// mise à jour de la valeur (keys) -> émettre objets cohérents pour select/deselect
function handleUpdateValue(v: string | string[] | null) {
  value.value = v
  cacheSelectedKeys(v)
  console.log('[CTS] update value ->', v)

  // 1) Mettre à jour le v-model du parent avec les ids
  if (props.multiple) {
    const ids = Array.isArray(v) ? v : []
    emit('update:selected', ids)
  } else {
    const id = typeof v === 'string' ? v : undefined
    emit('update:selected', id)
  }

  // 2) Émettre des objets { id, label } pour les handlers
  if (props.multiple) {
    const objects = Array.isArray(v) ? keysToObjects(v) : []
    emit('select', objects)
  } else {
    if (v == null) {
      emit('deselect', null)
    } else {
      const obj = keysToObjects(v)      // { id, label }
      emit('select', obj)
    }
  }
}


// --- bindings NTreeSelect : on peut fournir des clés à étendre par défaut (si on veut étendre que certaines sous parties)
const defaultExpandedKeys = computed(() => {
  // collecte les ids marqués isDefaultExpanded dans les options d’entrée
  const keys: string[] = []
  function walk(input?: InputOpt[]) {
    if (!input) return
    for (const it of input) {
      if (it.isDefaultExpanded) keys.push(it.id)
      if (it.children?.length) walk(it.children)
    }
  }
  walk(props.options)
  return keys
})

// const defaultExpandedKeys = computed(() => {
//   const keys: string[] = []
//   function collectKeys(list?: InputOpt[]) {
//     if (!list) return
//     for (const item of list) {
//       keys.push(item.id)
//       if (item.children?.length) collectKeys(item.children)
//     }
//   }
//   collectKeys(props.options)
//   return keys
// })



function emitClose() { emit('close') }

onMounted(async () => {
  if (props.options?.length) {
    options.value = props.options.map(toTreeOpt)
  }
  if (props.optionsLoadingMethod && !props.searchMethod) {
    const dtos = await props.optionsLoadingMethod()
    options.value = dtos.map(fromDTO).map(toTreeOpt)
    if (props.defaultSelectedValue) {
      emit('select', options.value.map(object => ({ id: object.key, label: object.label })))
    }
  }
})
</script>

<style scoped>
.naiveTreeselect { width: 100%; }                 /* composant racine */
:deep(.n-base-selection) { width: 100%; }        /* l’input visible */
:deep(.n-tree-select) { width: 100%; }           /* conteneur Naive UI */

</style>
