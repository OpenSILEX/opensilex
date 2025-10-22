<template>
  <opensilex-FormField
    :rules="rules"
    :required="required"
    :label="label || 'component.common.type'"
    :helpMessage="helpMessage || 'component.common.type'"
  >
    <template #field="{ id: fieldId, validator }">
      <div :id="fieldId" @keydown.enter.stop="$emit('handlingEnterKey')">
        <opensilex-FormSelector
          v-model:selected="localSelected"
          :options="typesOptions"
          :multiple="multiple"
          :required="required"
          :disabled="disabled"
          :placeholder="t(placeholder || 'component.common.type')"
          :itemLoadingMethod="initTypes"
          :allowSelectingDisabledDescendants="true"
          :allowClearingDisabled="true"
          @select="validator?.validate(); $emit('select', $event)"
          @open="$emit('open', $event)"
        />
      </div>
    </template>
  </opensilex-FormField>
</template>

<script setup lang="ts">
import { ref, computed, inject, onMounted, onBeforeUnmount, watch } from 'vue'
import { useStore } from 'vuex'
import { useI18n } from 'vue-i18n'

import type { OpenSilexVuePlugin } from '@/models/OpenSilexVuePlugin'
import type { OntologyService, ResourceTreeDTO } from 'opensilex-core'
import HttpResponse, { OpenSilexResponse } from 'opensilex-core/HttpResponse'

/** Props */
const props = withDefaults(defineProps<{
  type?: string | string[]          // v-model:type
  baseType: string                  // URI de base
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

/** Emits */
const emit = defineEmits<{
  (e: 'update:type', v?: string | string[]): void
  (e: 'select', payload: any): void
  (e: 'open', payload: any): void
  (e: 'handlingEnterKey'): void
}>()

/** Env / services */
const { t } = useI18n()
const store = useStore()
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = opensilex.getService<OntologyService>('opensilex.OntologyService')

/** State */
const typesOptions = ref<any[] | null>(null)

/** v-model bridge */
const localSelected = computed<string | string[] | undefined>({
  get: () => props.type,
  set: (val) => emit('update:type', val)
})

/** Charge l’arbre des sous-classes (appelée à l’ouverture du sélecteur ou au mount) */
async function loadTypes () {
  const toIgnore = props.unselectableTypes.map(u => opensilex.getLongUri(u))
  const http = await service.getSubClassesOf(props.baseType, props.ignoreRoot) as HttpResponse<OpenSilexResponse<ResourceTreeDTO[]>>

  // buildTreeListOptions -> retourne un arbre { id, label, children, disabled? } compatible avec FormSelector
  typesOptions.value = opensilex.buildTreeListOptions(http.response.result, {
    expanded: null,
    disableSubTree: null,
    nodesToIgnoreList: toIgnore
  })
}

function initTypes () {
  if (!typesOptions.value) {
    return loadTypes()
  }
}

/** Langue -> recharger les libellés */
const unwatchLang = store.watch(
  () => store.getters.language,
  () => { typesOptions.value = null; loadTypes().catch(opensilex.errorHandler) }
)

/** Lifecycle */
onMounted(() => { loadTypes().catch(opensilex.errorHandler) })
onBeforeUnmount(() => { unwatchLang && unwatchLang() })
</script>
