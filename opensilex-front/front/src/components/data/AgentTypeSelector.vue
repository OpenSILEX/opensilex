<template>
  <opensilex-FormSelector
    :label="t('ProvenanceView.agent_type')"
    v-model:selected="agentTypeURI"
    :options="internalOptions"
    :multiple="multiple"
    :required="required"
    :placeholder="t('ProvenanceView.agent_type-placeholder')"
    @clear="emit('clear')"
    @select="emit('select')"
    @deselect="emit('deselect')"
    @keyup.enter="onEnter"
  />
</template>

<script setup lang="ts">
import { computed, inject, onMounted, ref, watch } from 'vue'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import Oeso from "../../ontologies/Oeso"
import { useI18n } from 'vue-i18n'

type SelectorOption = {
  id: string
  label: string
}

const props = withDefaults(defineProps<{
  selected?: string | string[] | null
  multiple?: boolean
  required?: boolean
  exclusions?: Set<string>
  options?: SelectorOption[]
}>(), {
  multiple: false,
  required: false,
  exclusions: () => new Set<string>()
})

const emit = defineEmits<{
  (e: 'update:selected', value: string | string[] | null): void
  (e: 'clear'): void
  (e: 'select', value?: any): void
  (e: 'deselect', value?: any): void
  (e: 'handlingEnterKey'): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const ontologyService = $opensilex.getService<any>('opensilex.OntologyService')
const { t } = useI18n()

const internalOptions = ref<SelectorOption[]>([])

const agentTypeURI = computed({
  get: () => props.selected ?? (props.multiple ? [] : null),
  set: (value) => emit('update:selected', value)
})

onMounted(async () => {
  await refreshOptions()
})

watch(
  () => [props.options, props.selected, props.exclusions],
  async () => {
    await refreshOptions()
  },
  { deep: true }
)

async function refreshOptions() {
  if (!props.options) {
    internalOptions.value = await getAgentTypes()
    return
  }

  internalOptions.value = props.options.filter(option =>
    option.id === props.selected || !props.exclusions.has(option.id)
  )
}

async function getAgentTypes(): Promise<SelectorOption[]> {
  const agentTypes: SelectorOption[] = []

  try {
    const operatorHttp = await ontologyService.getRDFType(Oeso.OPERATOR_TYPE_URI, undefined)
    agentTypes.push({
      id: operatorHttp.response.result.uri,
      label: operatorHttp.response.result.name
    })
  } catch (error) {
    $opensilex.errorHandler(error)
  }

  try {
    const devicesHttp = await ontologyService.getSubClassesOf(Oeso.DEVICE_TYPE_URI, true)
    for (const item of devicesHttp.response.result ?? []) {
      agentTypes.push({
        id: item.uri,
        label: item.name
      })
    }
  } catch (error) {
    $opensilex.errorHandler(error)
  }

  return agentTypes
}

function onEnter() {
  emit('handlingEnterKey')
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  ProvenanceView:
    add: Add provenance
    description: Describe data provenance
    name: Name
    activity_type: Activity type
    agent_type: Agent type
    agent: Agent
    activity_start_date: Start date
    activity_end_date: End date
    name-placeholder: Enter provenance name
    activity_type-placeholder: Select a type of activity
    agent_type-placeholder: Select a type of agent

fr:
  ProvenanceView:
    add: Ajouter une provenance
    description: Décrire la provenance de données
    name: Nom
    activity_type: Type d'activité
    agent_type: Type d'agent
    agent: Agent
    activity_start_date: Date de début
    activity_end_date: Date de fin
    name-placeholder: Entrer un nom de provenance
    activity_type-placeholder: Selectionner un type d'activité
    agent_type-placeholder: Selectionner un type d'agent
</i18n>