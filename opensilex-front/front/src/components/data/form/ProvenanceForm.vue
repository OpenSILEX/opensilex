<template>
  <Modal ref="modalRef">
    <template #header>
      <FormHeader :title="modalFormLogic.formTitle.value" icon="fa#seedling" />
    </template>

    <n-form
      ref="formRef"
      :model="modalFormLogic.form.value"
      :rules="rules"
      label-placement="top"
      :show-require-mark="true"
      size="large"
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
      <n-form-item>
        <UriForm
          :uri.sync="modalFormLogic.form.value.uri"
          label="component.common.uri-or-url"
          :editMode="modalFormLogic.editMode.value"
          :generated.sync="uriGenerated"
        />
      </n-form-item>

      <!-- Name -->
      <n-form-item path="name">
        <InputForm
          v-model:value="modalFormLogic.form.value.name"
          :label="t('component.common.name')"
          :helpMessage="t('ProvenanceForm.name-help')"
          type="text"
          :placeholder="t('ProvenanceForm.name-placeholder')"
          :required="true"
        />
      </n-form-item>

      <!-- Description -->
      <n-form-item>
        <TextAreaForm
          v-model:value="modalFormLogic.form.value.description"
          :helpMessage="t('ProvenanceForm.description-help')"
          :label="t('component.common.description')"
          :placeholder="t('ProvenanceForm.description-placeholder')"
          @keydown.enter.stop
        />
      </n-form-item>

      <!-- Activity -->
      <n-card :title="t('ProvenanceForm.activity')" class="activityCard">
        <!-- Type -->
        <n-form-item path="activity_type">
          <TypeForm
            v-model:type="modalFormLogic.form.value.activity_type"
            :baseType="Prov.ACTIVITY_TYPE_URI"
            :required="true"
            :helpMessage="t('ProvenanceForm.type-help')"
            :placeholder="t('ProvenanceForm.type-placeholder')"
          />
        </n-form-item>

        <!-- Start / End -->
        <n-grid :cols="2" :x-gap="12" responsive="screen" item-responsive>
          <n-grid-item span="2 m:1">
            <n-form-item>
              <DateTimeForm
                v-model:value="modalFormLogic.form.value.activity_start_date"
                :label="t('ProvenanceForm.start')"
                :helpMessage="t('ProvenanceForm.start-help')"
              />
            </n-form-item>
          </n-grid-item>

          <n-grid-item span="2 m:1">
            <n-form-item>
              <DateTimeForm
                v-model:value="modalFormLogic.form.value.activity_end_date"
                :label="t('ProvenanceForm.end')"
                :helpMessage="t('ProvenanceForm.end-help')"
              />
            </n-form-item>
          </n-grid-item>
        </n-grid>

        <!-- Activity URI -->
        <n-form-item>
          <InputForm
            v-model:value="modalFormLogic.form.value.activity_uri"
            label="url"
            type="url"
            rules="url"
            :helpMessage="t('ProvenanceForm.url-help')"
          />
        </n-form-item>
      </n-card>

      <!-- Agents -->
      <n-form-item>
        <ProvenanceAgentForm
          v-model:values="modalFormLogic.form.value.agents"
          :key="lang"
        />
      </n-form-item>
    </n-form>

    <template #footer>
      <FormFooter @cancel="modalFormLogic.hide" @submit="modalFormLogic.submit" />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { computed, inject, ref, useTemplateRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { useStore } from 'vuex'
import { NForm, NFormItem, NCard, NGrid, NGridItem } from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import {requiredObjectOrLists, requiredTrimmed} from '../../../models/FormFieldsFormatter'
import Prov from '../../../ontologies/Prov'
import UriForm from '@/components/common/forms/UriForm.vue'
import InputForm from '@/components/common/forms/InputForm.vue'
import TextAreaForm from '@/components/common/forms/TextAreaForm.vue'
import TypeForm from '@/components/common/forms/TypeForm.vue'
import DateTimeForm from '@/components/common/forms/DateTimeForm.vue'
import ProvenanceAgentForm from '@/components/data/form/ProvenanceAgentForm.vue'
import Modal from '@/components/common/views/Modal.vue'
import FormHeader from '@/components/common/forms/FormHeader.vue'
import FormFooter from '@/components/common/forms/FormFooter.vue'
import useModalFormLogic from '@/composables/useModalFormLogic'

//#region Public
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

const emit = defineEmits<{
  (e: 'onUpdate', payload: any): void
  (e: 'onCreate', payload: any): void
  (e: 'onSuccess'): void
}>()

const props = defineProps<{
  createTitle: string,
  editTitle: string
}>()
//#endregion

//#region Private

//#region Plugin and services
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const dataService = opensilex.getService<any>('opensilex.DataService')
const store = useStore()
const { t } = useI18n()
//#endregion

const modalRef = useTemplateRef<InstanceType<typeof Modal>>('modalRef')
const formRef = useTemplateRef<InstanceType<typeof NForm>>('formRef')

//#region Datas & computed
let uriGenerated = ref<boolean>(true)

const lang = computed(() => store.getters.language)

const rules = computed(() => ({
  name: requiredTrimmed('component.common.name'),
  activity_type: requiredObjectOrLists('ProvenanceForm.activity-type-label')
}))
//#endregion

//#region modalFormLogic composable
const modalFormLogic = useModalFormLogic<ProvenanceFormModel>({
  modalRef,
  nFormRef: formRef,
  getEmptyForm,
  create,
  update,
  reset,
  addTitle: props.createTitle,
  editTitle: props.editTitle,
  onCreate: (res) => emit('onCreate', res),
  onUpdate: (res) => emit('onUpdate', res),
  onSuccess: () => emit('onSuccess'),
})
//#endregion

//#region Methods
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

async function reset(): Promise<void> {
  uriGenerated.value = true
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

async function create(sourceForm: ProvenanceFormModel) {
  const provenance = buildPayload(sourceForm, false)
  return  await dataService.createProvenance(provenance)
}

async function update(sourceForm: ProvenanceFormModel) {
  const provenance = buildPayload(sourceForm, true)
  return  await dataService.updateProvenance(provenance)
}
//#endregion

//#endregion
defineExpose({
  showCreateForm: modalFormLogic.showCreateForm,
  showEditForm: modalFormLogic.showEditForm
})
</script>

<style scoped lang="scss">
.activityCard {
  margin-top: 12px;
  margin-bottom: 12px;
}
</style>

<i18n>
en:
  ProvenanceForm:
    name-help: Enter a name
    name-placeholder: Enter a name
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
    name-help: Entrer un nom
    name-placeholder: Entrer un nom
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