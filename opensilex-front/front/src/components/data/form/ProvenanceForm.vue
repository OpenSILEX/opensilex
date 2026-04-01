<template>
  <n-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-placement="top"
    :show-require-mark="true"
  >
    <!-- Help message -->
    <div class="divHelpMsg">
      <p>
        {{ t('ProvenanceForm.help-msg') }}
        <router-link target="_blank" :to="{ path: '/devices' }">
          <span class="helpMsg">{{ t('component.menu.devices') }}</span>
        </router-link>
      </p>
    </div>

    <!-- URI -->
    <opensilex-UriForm
      v-model:uri="form.uri"
      label="component.common.uri-or-url"
      :editMode="editMode"
      v-model:generated="uriGeneratedModel"
    />

    <!-- Name -->
    <n-form-item path="name" class="compact-form-item">
      <opensilex-InputForm
        v-model:value="form.name"
        :label="t('ProvenanceForm.name')"
        :helpMessage="t('ProvenanceForm.name-help')"
        type="text"
        :placeholder="t('ProvenanceForm.name-placeholder')"
        :required="!disableValidation"
      />
    </n-form-item>

    <!-- Description -->
    <opensilex-TextAreaForm
      v-model:value="form.description"
      :helpMessage="t('ProvenanceForm.description-help')"
      :label="t('ProvenanceForm.description')"
      :placeholder="t('ProvenanceForm.description-placeholder')"
      @keydown.enter.stop
    />

    <!-- Activity -->
    <n-card :title="t('ProvenanceForm.activity')" class="activityCard">
      <!-- Type -->
      <n-form-item path="activity_type" class="compact-form-item">
        <opensilex-TypeForm
          v-model:type="form.activity_type"
          :baseType="Prov.ACTIVITY_TYPE_URI"
          :required="!disableValidation"
          :helpMessage="t('ProvenanceForm.type-help')"
          :placeholder="t('ProvenanceForm.type-placeholder')"
        />
      </n-form-item>

      <!-- Start / End -->
      <n-grid :cols="2" :x-gap="12" responsive="screen" item-responsive>
        <n-grid-item span="2 m:1">
          <opensilex-DateTimeForm
            v-model:value="form.activity_start_date"
            :label="t('ProvenanceForm.start')"
            :helpMessage="t('ProvenanceForm.start-help')"
          />
        </n-grid-item>

        <n-grid-item span="2 m:1">
          <opensilex-DateTimeForm
            v-model:value="form.activity_end_date"
            :label="t('ProvenanceForm.end')"
            :helpMessage="t('ProvenanceForm.end-help')"
          />
        </n-grid-item>
      </n-grid>

      <!-- Activity URI -->
      <opensilex-InputForm
        v-model:value="form.activity_uri"
        label="url"
        type="url"
        rules="url"
        :helpMessage="t('ProvenanceForm.url-help')"
      />
    </n-card>

    <!-- Agents -->
    <opensilex-ProvenanceAgentForm
      v-model:values="form.agents"
      :key="lang"
    />
  </n-form>
</template>

<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useStore } from 'vuex'
import { NForm, NFormItem, NCard, NGrid, NGridItem } from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import { requiredTrimmed } from '../../../models/FormFieldsFormatter'
import Prov from '../../../ontologies/Prov'

type ProvenanceAgentGroup = {
  uris: string[]
  rdf_type: string | null
}

type ProvenanceFormModel = {
  uri: string | null
  name: string | null
  description: string | null
  experiments?: any[]
  activity_type: string | null
  activity_start_date: string | null
  activity_end_date: string | null
  activity_uri: string | null
  agentTypes?: any[]
  agents: ProvenanceAgentGroup[]
  publisher?: any
  publication_date?: any
  last_updated_date?: any
}

const props = withDefaults(
  defineProps<{
    form: ProvenanceFormModel
    editMode?: boolean
    uriGenerated?: boolean
    disableValidation?: boolean
  }>(),
  {
    editMode: false,
    uriGenerated: true,
    disableValidation: false
  }
)

const emit = defineEmits<{
  (e: 'update:form', value: ProvenanceFormModel): void
  (e: 'update:uriGenerated', value: boolean): void
}>()

const store = useStore()
const { t } = useI18n()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = $opensilex.getService<any>('opensilex.DataService')

const formRef = ref<any>(null)
const lang = computed(() => store.getters.language)

const form = computed({
  get: () => props.form,
  set: (value: ProvenanceFormModel) => emit('update:form', value)
})

const uriGeneratedModel = computed({
  get: () => props.uriGenerated,
  set: (value: boolean) => emit('update:uriGenerated', value)
})

const rules = computed(() => {
  if (props.disableValidation) {
    return {}
  }

  return {
    name: requiredTrimmed('ProvenanceForm.name'),
    activity_type: {
      required: true,
      message: t('validations.required_if', {
        _field_: t('ProvenanceForm.activity-type-label')
      }),
      trigger: ['change', 'blur']
    }
  }
})

function getEmptyForm(): ProvenanceFormModel {
  return {
    uri: null,
    name: null,
    description: null,
    experiments: [],
    activity_type: null,
    activity_start_date: null,
    activity_end_date: null,
    activity_uri: null,
    agentTypes: [],
    agents: [
      {
        uris: [],
        rdf_type: null
      }
    ]
  }
}

function flattenAgents(sourceForm: ProvenanceFormModel) {
  const agents: Array<{ uri: string; rdf_type: string | null }> = []

  for (const group of sourceForm.agents ?? []) {
    for (const uri of group.uris ?? []) {
      agents.push({
        uri,
        rdf_type: group.rdf_type
      })
    }
  }

  return agents
}

function buildPayload(sourceForm: ProvenanceFormModel, includeMetadata = false) {
  const agents = flattenAgents(sourceForm)

  const provenance: any = {
    uri: sourceForm.uri,
    name: sourceForm.name,
    description: sourceForm.description,
    prov_activity: [
      {
        rdf_type: sourceForm.activity_type,
        start_date: sourceForm.activity_start_date,
        end_date: sourceForm.activity_end_date,
        uri: sourceForm.activity_uri
      }
    ],
    prov_agent: agents
  }

  if (includeMetadata) {
    provenance.publisher = sourceForm.publisher
    provenance.publication_date = sourceForm.publication_date
    provenance.last_updated_date = sourceForm.last_updated_date
  }

  return provenance
}

async function validateForm() {
  try {
    await formRef.value?.validate()
    return true
  } catch {
    return false
  }
}

async function create(sourceForm: ProvenanceFormModel) {
  const isValid = await validateForm()
  if (!isValid) {
    return Promise.reject(new Error('Form validation failed'))
  }

  const provenance = buildPayload(sourceForm, false)
  const http = await service.createProvenance(provenance)
  const uri = http.response.result
  provenance.uri = uri
  return provenance
}

async function update(sourceForm: ProvenanceFormModel) {
  const isValid = await validateForm()
  if (!isValid) {
    return Promise.reject(new Error('Form validation failed'))
  }

  const provenance = buildPayload(sourceForm, true)
  const http = await service.updateProvenance(provenance)
  const uri = http.response.result
  provenance.uri = uri
  return provenance
}

defineExpose({
  getEmptyForm,
  create,
  update,
  validate: validateForm
})
</script>

<style scoped lang="scss">
.helpMsg {
  color: #007bff;
  margin: 0;
  padding: 0;
}

.divHelpMsg {
  margin-bottom: 12px;
}

.activityCard {
  margin-top: 12px;
  margin-bottom: 12px;
}

:deep(.compact-form-item) {
  --n-label-height: 0px !important;
  --n-label-padding: 0 !important;
}
</style>

<i18n>
en:
  ProvenanceForm:
    name: Name
    name-help: Enter a name
    name-placeholder: Enter a name
    description: Description
    description-help: Describe provenance
    description-placeholder: Describe provenance
    agent: Agent
    agent-help: Select agents
    agent-placeholder: Select agents
    activity: Activity
    activity-type-label: Activity type
    start: Start Date
    start-help: Start Date
    end: End Date
    end-date: End Date
    end-help: End Date
    agents: Provenance agents
    add-agent: Add an agent
    type-placeholder: Select a type of activity
    type-help: Select a type of activity
    url-help: External link describing the activity
    help-msg: Before starting, verify that the related devices have been already created. Check the section

fr:
  ProvenanceForm:
    name: Nom
    name-help: Entrer un nom
    name-placeholder: Entrer un nom
    description: Description
    description-help: Décrire la provenance
    description-placeholder: Décrire la provenance
    agent: Agent
    agent-help: Selectionner agents
    agent-placeholder: Selectionner agents
    activity: Activité
    activity-type-label: Type d'activité
    start: Date de début
    start-help: Date de début
    end: Date de fin
    end-date: Date de fin
    end-help: Date de fin
    agents: Agents de la provenance
    add-agent: Ajouter un agent
    type-placeholder: Selectionner un type d'activité
    type-help: Selectionner un type d'activité
    url-help: Lien externe décrivant l'activity
    help-msg: Avant de commencer, vérifiez d'abord que les appareils associés ont déjà été créés. Regardez sur la section
</i18n>