<template>
  <div v-if="!disabled">
    <opensilex-FormSelector
      :label="label"
      v-model:selected="internalValue"
      :multiple="multiple"
      :required="required"
      :optionsLoadingMethod="loadFactorLevels"
      :searchMethod="searchFactorLevels"
      :conversionMethod="convertDetail"
      :disableBranchNodes="true"
      :placeholder="placeholder"
      @enterKey="onEnter"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type HttpResponse from 'opensilex-security/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'

type FactorLevelDTO = {
  uri: string
  name: string
}

type FactorDTO = {
  uri: string
  name: string
  levels: FactorLevelDTO[]
}

type FactorNode = {
  id: string
  label: string
  children: { id: string; label: string }[]
}

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const { t } = useI18n()

const emit = defineEmits<{
  (e: 'update:factorLevels', value: string[] | undefined): void
  (e: 'handlingEnterKey'): void
}>()

const props = withDefaults(defineProps<{
  factorLevels?:  string[]
  experimentURI?: string
  label?: string
  required?: boolean
  multiple?: boolean
  placeholder?: string
}>(), {
  label: 'FactorLevelSelector.label',
  required: false,
  multiple: false,
  placeholder: 'FactorLevelSelector.placeholder'
})

const disabled = ref(false)

const internalValue = computed({
  get: () => props.factorLevels,
  set: (value) => emit('update:factorLevels', value)
})

const placeholder = computed(() => t(props.placeholder))
const label = computed(() => t(props.label))

async function loadFactorLevels(): Promise<FactorNode[]> {
  if (!props.experimentURI) {
    return []
  }

  try {
    const http: HttpResponse<OpenSilexResponse<FactorDTO[]>> = await $opensilex
      .getService('opensilex.ExperimentsService')
      .getAvailableFactors(props.experimentURI)

    const factors = http.response.result ?? []

    return factors
      .map((factor) => ({
        id: factor.uri,
        label: factor.name,
        children: (factor.levels ?? []).map((factorLevel) => ({
          id: factorLevel.uri,
          label: factorLevel.name
        }))
      }))
      .filter((factorNode) => factorNode.children.length > 0)
  } catch (error: any) {
    if (error?.status === 404) {
      disabled.value = true
      return []
    }
    throw error
  }
}

function searchFactorLevels(name: string, page: number, pageSize: number) {
  if (props.experimentURI) {
    return $opensilex
      .getService('opensilex.ExperimentsService')
      .getAvailableFactors(props.experimentURI)
  }

  return $opensilex
    .getService('opensilex.FactorsService')
    .searchFactorLevels(name, ['name=asc'], page, pageSize)
}

function convertDetail(factor: FactorDTO): FactorNode {
  return {
    id: factor.uri,
    label: factor.name,
    children: (factor.levels ?? []).map((factorLevel) => ({
      id: factorLevel.uri,
      label: factorLevel.name
    }))
  }
}

function onEnter() {
  emit('handlingEnterKey')
}
</script>

<style scoped>
</style>

<i18n>
en:
  FactorLevelSelector:
    label: Factor levels
    placeholder: Select a factor level

fr:
  FactorLevelSelector:
    label: Niveaux de facteur
    placeholder: Sélectionner les niveaux de facteurs
</i18n>