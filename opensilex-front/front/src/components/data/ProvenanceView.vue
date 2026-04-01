<template>
  <div class="container-fluid">
    <opensilex-PageActions
      v-if="user.hasCredential(credentials.CREDENTIAL_PROVENANCE_MODIFICATION_ID)"
    >
      <opensilex-CreateButton
        @click="provenanceFormRef?.showCreateForm?.()"
        :label="t('ProvenanceView.add')"
        class="createButton"
      />
    </opensilex-PageActions>

    <opensilex-PageContent>
      <opensilex-ProvenanceList
        ref="provListRef"
        @onEdit="editProvenance"
        @onDelete="deleteProvenance"
      />
    </opensilex-PageContent>

    <opensilex-ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_PROVENANCE_MODIFICATION_ID)"
      ref="provenanceFormRef"
      component="opensilex-ProvenanceForm"
      :createTitle="t('ProvenanceView.add')"
      :editTitle="t('ProvenanceView.update')"
      icon="fa#seedling"
      modalSize="lg"
      :successMessage="successMessage"
      :key="lang"
      @onCreate="provListRef?.refresh?.()"
      @onUpdate="provListRef?.updateSelectedProvenance?.()"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useStore } from 'vuex'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'

const store = useStore()
const { t, locale } = useI18n()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = $opensilex.getService<any>('opensilex.DataService')

const provenanceFormRef = ref<any>(null)
const provListRef = ref<any>(null)

const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)
const lang = computed(() => store.getters.language ?? locale.value)

async function editProvenance(uri: string) {
  try {
    const http = await service.getProvenance(uri)
    const updateForm = convertDtoBeforeEditForm(http.response.result)
    provenanceFormRef.value?.showEditForm?.(updateForm)
  } catch (error: any) {
    $opensilex.errorHandler(error)
  }
}

function convertDtoBeforeEditForm(dto: any) {
  const form: any = {
    uri: dto.uri,
    name: dto.name,
    description: dto.description,
    activity_type: null,
    activity_start_date: null,
    activity_end_date: null,
    activity_uri: null,
    agents: [],
    items: [0],
    publisher: dto.publisher,
    publication_date: dto.issued,
    last_updated_date: dto.modified
  }

  if (dto.prov_activity != null && dto.prov_activity.length > 0) {
    form.activity_type = dto.prov_activity[0].rdf_type
    form.activity_start_date = dto.prov_activity[0].start_date
    form.activity_end_date = dto.prov_activity[0].end_date
    form.activity_uri = dto.prov_activity[0].uri
  }

  const uniqueTypes = new Set<string>()

  if (dto.prov_agent != null) {
    dto.prov_agent.forEach((agent: any) => {
      uniqueTypes.add(agent.rdf_type)
    })
  }

  for (const type of uniqueTypes) {
    const agentsByType: string[] = []

    for (const agent of dto.prov_agent ?? []) {
      if (agent.rdf_type === type) {
        agentsByType.push(agent.uri)
      }
    }

    form.agents.push({
      rdf_type: type,
      uris: agentsByType
    })
  }

  return form
}

async function deleteProvenance(uri: string) {
  try {
    await service.deleteProvenance(uri)
    provListRef.value?.refresh?.()

    const message =
      `${t('ProvenanceView.delete-message')} ${uri} ` +
      `${t('component.common.success.delete-success-message')}`

    $opensilex.showSuccessToast(message)
  } catch (error: any) {
    if (error?.response?.result?.message) {
      $opensilex.errorHandler(error, error.response.result.message)
    } else {
      $opensilex.errorHandler(error)
    }
  }
}

function successMessage(form: any) {
  return `${t('ProvenanceView.success-message')} ${form.name}`
}
</script>

<style scoped lang="scss">
.createButton {
  margin-bottom: 10px;
  margin-top: -15px;
  margin-left: 0;
}
</style>

<i18n>
en:
  ProvenanceView:
    add: Add provenance
    update: Update provenance
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
    success-message: Provenance
    delete-message: Provenance
    associated-data-error: Provenance already associated with data

fr:
  ProvenanceView:
    add: Ajouter une provenance
    update: Modifier une provenance
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
    success-message: La provenance
    delete-message: La provenance
    associated-data-error: Provenance déjà associée à des données
</i18n>