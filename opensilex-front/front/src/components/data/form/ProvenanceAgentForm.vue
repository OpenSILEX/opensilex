<template>
  <n-card :title="t('ProvenanceAgentForm.agents')" class="agentCard">
    <n-button
      v-if="!agents || agents.length < initialAgentTypes.length"
      class="greenThemeColor mb-3"
      @click="addAgent"
    >
      {{ t('ProvenanceAgentForm.add-agent') }}
    </n-button>

    <n-grid
      v-for="(agent, index) in agents"
      :key="agent.rdf_type || `agent-${index}-${agentSelectorRefreshKey}`"
      :cols="2"
      :x-gap="12"
      responsive="screen"
      item-responsive
      class="mb-2"
    >
      <n-grid-item span="2 m:1">
        <opensilex-AgentTypeSelector
          v-model:selected="agent.rdf_type"
          :multiple="false"
          :options="initialAgentTypes"
          :exclusions="usedAgentTypes"
          :key="`type-${index}-${agentSelectorRefreshKey}`"
          @update:selected="onSelectedAgent(agent)"
        />
      </n-grid-item>

      <n-grid-item span="2 m:1">
        <opensilex-PersonSelector
          v-if="agent.rdf_type === 'vocabulary:Operator'"
          v-model:persons="agent.uris"
          :label="t('ProvenanceAgentForm.agent')"
          :helpMessage="t('ProvenanceAgentForm.agent-help')"
          :multiple="true"
        />

        <opensilex-DeviceSelector
          v-else-if="agent.rdf_type"
          v-model:value="agent.uris"
          :label="t('ProvenanceAgentForm.agent')"
          :multiple="true"
          :type="agent.rdf_type"
          :helpMessage="t('ProvenanceAgentForm.agent-help')"
        />
      </n-grid-item>
    </n-grid>
  </n-card>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { NCard, NButton, NGrid, NGridItem } from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import Oeso from '../../../ontologies/Oeso'

type AgentValue = {
  uris: string[]
  rdf_type: string | null
}

const props = defineProps<{
  values: AgentValue[]
}>()

const emit = defineEmits<{
  (e: 'update:values', value: AgentValue[]): void
}>()

const { t } = useI18n();

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const ontologyService = $opensilex.getService<any>('opensilex.OntologyService')

const initialAgentTypes = ref<Array<{ id: string; label: string }>>([])
const agentSelectorRefreshKey = ref(0)

const agents = computed({
  get: () => props.values,
  set: (value: AgentValue[]) => emit('update:values', value)
})

const usedAgentTypes = computed(() => {
  if (!initialAgentTypes.value || initialAgentTypes.value.length === 0) {
    return new Set<string>()
  }

  return new Set(
    (agents.value ?? [])
      .filter(agent => agent && agent.rdf_type)
      .map(agent => agent.rdf_type as string)
  )
})

onMounted(async () => {
  initialAgentTypes.value = await getAgentTypes()
})

function addAgent() {
  const next = [...(agents.value ?? [])]
  next.push({ uris: [], rdf_type: null })
  agents.value = next
}

async function getAgentTypes(): Promise<Array<{ id: string; label: string }>> {
  const agentTypes: Array<{ id: string; label: string }> = []

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

function onSelectedAgent(agent: AgentValue) {
  agent.uris = []
  agentSelectorRefreshKey.value++
}
</script>

<style scoped>
.agentCard {
  margin-top: 12px;
}
</style>

<i18n>
en:
  ProvenanceAgentForm: 
    agent: Agent
    agent-help: Select agents
    agents: Provenance agents
    add-agent: Add an agent
fr:
  ProvenanceAgentForm:
    agent: Agent
    agent-help: Selectionner agents
    agents: Agents de la provenance
    add-agent: Ajouter un agent
</i18n>