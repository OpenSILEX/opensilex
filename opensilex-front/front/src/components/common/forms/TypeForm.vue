<template>
  <opensilex-FormField
    :rules="rules"
    :required="required"
    :label="label || 'component.common.type'"
    :helpMessage="helpMessage || 'component.common.type'"
  >
    <template #field="{ id: fieldId, validator }">
      <div :id="fieldId" @keydown.enter.stop="$emit('handlingEnterKey')">
        <!-- BYPASS FORMSELECTOR -->
        <opensilex-CustomTreeselect
          v-model:selected="selectedIds"                
          :options="typesOptions"                      
          :multiple="multiple"
          :disabled="disabled"
          :placeholder="t(placeholder || 'component.common.type')"
          :itemLoadingMethod="loadByUris"            
          :disableBranchNodes="false"
          :searchMethod="searchTypes"
          :resultLimit="20"
          @select="validator?.validate(); $emit('select',$event)"
        />
      </div>
    </template>
  </opensilex-FormField>
</template>

<script setup lang="ts">
import { ref, computed, inject, onMounted, watch } from 'vue'
import { useStore } from 'vuex'
import { useI18n } from 'vue-i18n'
import CustomTreeselect from './CustomTreeselect.vue'  // 👈 adapte le chemin

import type { OpenSilexVuePlugin } from '@/models/OpenSilexVuePlugin'
import type { OntologyService, ResourceTreeDTO } from 'opensilex-core'
import HttpResponse, { OpenSilexResponse } from 'opensilex-core/HttpResponse'

const props = withDefaults(defineProps<{
  type?: string | string[]
  baseType: string
  label?: string
  placeholder?: string
  helpMessage?: string
  required?: boolean
  disabled?: boolean
  multiple?: boolean
  rules?: string | Function
  ignoreRoot?: boolean
  unselectableTypes?: string[]
}>(), {
  type: undefined,
  required: false,
  disabled: false,
  multiple: false,
  rules: undefined,
  ignoreRoot: true,
  unselectableTypes: () => []
})

const emit = defineEmits<{
  (e: 'update:type', v?: string | string[]): void
  (e: 'select', payload: any): void
  (e: 'handlingEnterKey'): void
}>()

const { t } = useI18n()
const store = useStore()
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = opensilex.getService<OntologyService>('opensilex.OntologyService')

type InputOpt = { id: string; label: string; children?: InputOpt[] }
const typesOptions = ref<InputOpt[]>([])

const selectedIds = computed<string[] | string>({
  get: () => Array.isArray(props.type) ? props.type as string[] : (props.type ?? ''),
  set: (v) => {
    if (props.multiple) emit('update:type', Array.isArray(v) ? v : (v ? [v] : []))
    else emit('update:type', Array.isArray(v) ? v[0] : v || undefined)
  }
})

function mapTree(nodes: any[]): InputOpt[] {
  return (nodes || []).map(n => ({
    id: n.id,
    label: n.label,
    children: mapTree(n.children || [])
  }))
}

async function loadTypes () {
  const toIgnore = props.unselectableTypes.map(u => opensilex.getLongUri(u))
  const http = await service.getSubClassesOf(props.baseType, props.ignoreRoot) as HttpResponse<OpenSilexResponse<ResourceTreeDTO[]>>
  const built = opensilex.buildTreeListOptions(http.response.result, {
    expanded: null,
    disableSubTree: null,
    nodesToIgnoreList: toIgnore,
    flat: true
  })
  typesOptions.value = mapTree(built)

  // injecter la valeur si hors-arbre pour l'afficher
  const vals = Array.isArray(selectedIds.value) ? selectedIds.value : (selectedIds.value ? [selectedIds.value] : [])
  for (const id of vals) {
    if (!id) continue
    const exists = JSON.stringify(typesOptions.value).includes(`"${id}"`)
    if (!exists) typesOptions.value.unshift({ id, label: (opensilex as any)?.getShortUri?.(id) ?? id, children: [] })
  }
}

async function loadByUris(uris: string[]) {
  // DTOs attendus par CustomTreeselect: [{ uri, name }]
  return uris.map(u => ({ uri: u, name: (opensilex as any)?.getShortUri?.(u) ?? u }))
}


// Recherche 
// Aplatit l’arbre {id,label,children[]} -> [{id,label}]
function flatten(nodes: InputOpt[] = []): Array<{ id: string; label: string }> {
  const out: Array<{ id: string; label: string }> = []
  const stack = [...nodes]
  while (stack.length) {
    const n = stack.shift()!
    out.push({ id: n.id, label: n.label })
    if (n.children?.length) stack.unshift(...n.children)
  }
  return out
}

// searchMethod attend => Promise<{ response: { result: NamedResourceDTO[], metadata: { pagination: { totalCount }}}}>
// NamedResourceDTO minimal = { uri, name }
async function searchTypes(query: string, _offset = 0, limit = 20) {
  const http = await service.searchSubClasses(props.baseType, query, limit) // exemple
  const list = http.response.result as Array<{ uri: string; name: string }>
  return {
    response: {
      result: list,                         // [{ uri, name }]
      metadata: { pagination: { totalCount: http.response.metadata.pagination.totalCount } }
    }
  }
}

onMounted(() => { loadTypes().catch(opensilex.errorHandler) })
watch(() => store.getters.language, () => loadTypes().catch(opensilex.errorHandler))
</script>
