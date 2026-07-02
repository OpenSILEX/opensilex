<template>
  <div class="container-fluid py-3">
    <opensilex-PageHeader
      icon="fa#seedling"
      :hasIcon="true"
      :title="name"
      :description="t('ProvenanceDetailsPage.title')"
      class="detail-element-header"
    />

    <opensilex-PageActions :returnButton="true">
      <nav class="tabs mb-3">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="['tab', { active: currentTab === tab.key }]"
          type="button"
          @click="goToTab(tab.key)"
        >
          {{ tab.label }}

          <span
            v-if="tab.key === 'annotations' && !annotationsCountIsLoading && annotations > 0"
            class="tabBadge"
          >
            {{ formatCount(annotations) }}
          </span>

          <span
            v-if="tab.key === 'documents' && !documentsCountIsLoading && documents > 0"
            class="tabBadge"
          >
            {{ formatCount(documents) }}
          </span>
        </button>
      </nav>
    </opensilex-PageActions>

    <opensilex-PageContent>
    <component
      v-if="currentTabComponent && uri"
      :is="currentTabComponent"
      :ref="currentTabRef"
      v-bind="currentTabProps"
      v-on="currentTabListeners"
    />
    </opensilex-PageContent>

    <opensilex-ModalForm
      ref="provenanceForm"
      component="opensilex-ProvenanceForm"
      :createTitle="t('ProvenanceDetailsPage.add')"
      :editTitle="t('ProvenanceDetailsPage.update')"
      icon="fa#seedling"
      modalSize="lg"
      @onUpdate="loadProvenance"
      :successMessage="successMessage"
    />
  </div>
</template>

<script setup lang="ts">
import {
  ref,
  computed,
  inject,
  defineAsyncComponent,
  watch,
  onMounted,
  type Ref
} from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { useI18n } from 'vue-i18n'

import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type HttpResponse from 'opensilex-security/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'
import type {
  ProvenanceGetDTO,
  DataService
} from 'opensilex-core/index'
import type { AnnotationsService } from 'opensilex-core/api/annotations.service'
import type { DocumentsService } from 'opensilex-core/api/documents.service'

const route = useRoute()
const router = useRouter()
const store = useStore()
const { t } = useI18n()
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

const credentials = computed(() => store.state.credentials)

const name = ref<string>('')

const annotations = ref<number>(0)
const documents = ref<number>(0)
const annotationsCountIsLoading = ref(true)
const documentsCountIsLoading = ref(true)

const provenance = ref<ProvenanceGetDTO>({
  uri: null,
  name: null,
  description: null,
  prov_activity: [],
  prov_agent: []
} as ProvenanceGetDTO)

const provenanceForm = ref<any>(null)

// Services
const dataService = opensilex.getService<DataService>('opensilex.DataService')
const annotationsService = opensilex.getService<AnnotationsService>('opensilex.AnnotationsService')
const documentsService = opensilex.getService<DocumentsService>('opensilex.DocumentsService')

// Tabs
const tabDefinitions = [
  {
    key: 'details',
    labelKey: 'component.common.details-label',
    component: () => import('./ProvenanceDescription.vue'),
    refKey: 'provenanceDescription'
  },
  {
    key: 'annotations',
    labelKey: 'ProvenanceDetailsPage.annotations',
    component: () => import('../annotations/list/AnnotationList.vue'),
    refKey: 'annotationList'
  },
  {
    key: 'documents',
    labelKey: 'ProvenanceDetailsPage.documents',
    component: () => import('../documents/DocumentTabList.vue'),
    refKey: 'documentTabList'
  }
] as const

type TabKey = typeof tabDefinitions[number]['key']

const tabs = computed(() =>
  tabDefinitions.map(({ key, labelKey }) => ({
    key,
    label: t(labelKey)
  }))
)

const tabComponents = Object.fromEntries(
  tabDefinitions.map(tab => [tab.key, defineAsyncComponent(tab.component)])
) as Record<TabKey, any>

const formRefs: Record<string, Ref<any>> = {}
tabDefinitions.forEach(tab => {
  formRefs[tab.refKey] = ref(null)
})

const currentTabComponent = computed(() => tabComponents[currentTab.value])

const currentTabRef = computed(() => {
  const def = tabDefinitions.find(t => t.key === currentTab.value)
  return def ? formRefs[def.refKey] : undefined
})

function getTabFromPath(): TabKey {
  const tabPath = route.path
  if (tabPath.startsWith('/provenances/annotations/')) return 'annotations'
  if (tabPath.startsWith('/provenances/documents/')) return 'documents'
  return 'details'
}

const currentTab = ref<TabKey>(getTabFromPath())

watch(
  () => route.path,
  () => {
    currentTab.value = getTabFromPath()
  }
)

function goToTab(key: TabKey) {
  if (!uri.value) return

  const base =
    key === 'details'
      ? '/provenances/details/'
      : key === 'annotations'
        ? '/provenances/annotations/'
        : '/provenances/documents/'

  router.replace({ path: base + encodeURIComponent(uri.value) })
}


const currentTabProps = computed(() => {
  if (!uri.value) return {}

  if (currentTab.value === 'details') {
    return {
      provenance: provenance.value,
      credentials: credentials.value,
      onEdit: showEditForm,
      onDelete: deleteProvenance
    }
  }

  if (currentTab.value === 'documents') {
    return {
      uri: uri.value,
      debounce: 300,
      lazy: false,
      modificationCredentialId: credentials.value.CREDENTIAL_DOCUMENT_MODIFICATION_ID
    }
  }

  return {
    target: uri.value,
    displayTargetColumn: false,
    enableActions: true,
    modificationCredentialId: credentials.value.CREDENTIAL_ANNOTATION_MODIFICATION_ID,
    deleteCredentialId: credentials.value.CREDENTIAL_ANNOTATION_DELETE_ID
  }
})

const currentTabListeners = computed(() => {
  if (currentTab.value === 'annotations') {
    return { changed: searchAnnotations }
  }
  if (currentTab.value === 'documents') {
    return { changed: searchDocuments }
  }
  return {}
})

function getUriFromRoute(): string {
  const raw = route.params.uri
  const param = Array.isArray(raw) ? raw[0] : (raw as string | undefined)
  return param ? decodeURIComponent(param) : ''
}

const uri = ref<string>(getUriFromRoute())


function formatCount(v: number) {
  const nf = (opensilex as any).$numberFormatter
  return nf?.formateResponse ? nf.formateResponse(v) : String(v)
}

async function loadProvenance() {
  if (!uri.value) return

  try {
    const http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>> =
      await dataService.getProvenance(uri.value)

    const prov = http.response.result
    const promiseArray: Promise<any>[] = []

    if (prov?.prov_activity != null && prov.prov_activity.length > 0) {
      const promiseActivity = opensilex
        .getService<any>('opensilex.OntologyService')
        .getURILabel(prov.prov_activity[0].rdf_type)
        .then((http: HttpResponse<OpenSilexResponse<string>>) => {
          prov.prov_activity[0]['name'] = http.response.result
        })
        .catch((http: HttpResponse<OpenSilexResponse<string>>) => {
          if (http.status === 404) {
            prov.prov_activity[0]['name'] = prov.prov_activity[0].rdf_type
          } else {
            opensilex.errorHandler(http)
          }
        })

      promiseArray.push(promiseActivity)
    }

    if (prov?.prov_agent != null) {
      for (let i = 0; i < prov.prov_agent.length; i++) {
        let promiseAgent: Promise<any>

        if (
          opensilex.Oeso.checkURIs(
            prov.prov_agent[i].rdf_type,
            opensilex.Oeso.OPERATOR_TYPE_URI
          )
        ) {
          promiseAgent = opensilex
            .getService<any>('opensilex.SecurityService')
            .getPerson(prov.prov_agent[i].uri)
            .then((http: HttpResponse<OpenSilexResponse<any>>) => {
              const userDto = http.response.result
              prov.prov_agent[i]['operator'] = userDto
              prov.prov_agent[i]['name'] = `${userDto.first_name}${userDto.last_name}`
            })
            .catch(opensilex.errorHandler)
        } else {
          promiseAgent = opensilex
            .getService<any>('opensilex.OntologyService')
            .getURILabel(prov.prov_agent[i].uri)
            .then((http: HttpResponse<OpenSilexResponse<string>>) => {
              prov.prov_agent[i]['name'] = http.response.result
            })
            .catch((http: HttpResponse<OpenSilexResponse<string>>) => {
              if (http.status === 404) {
                prov.prov_agent[i]['name'] = prov.prov_agent[i].uri
              } else {
                opensilex.errorHandler(http)
              }
            })
        }

        promiseArray.push(promiseAgent)
      }
    }

    await Promise.all(promiseArray)

    provenance.value = prov
    name.value = prov?.name ?? ''
  } catch (e) {
    opensilex.errorHandler(e)
  }
}

async function searchAnnotations() {
  if (!uri.value) return

  annotationsCountIsLoading.value = true
  try {
    const http: HttpResponse<OpenSilexResponse<number>> =
      await annotationsService.countAnnotations(uri.value, undefined, undefined)
    annotations.value = http.response.result ?? 0
  } catch (e) {
    opensilex.errorHandler(e)
  } finally {
    annotationsCountIsLoading.value = false
  }
}

async function searchDocuments() {
  if (!uri.value) return

  documentsCountIsLoading.value = true
  try {
    const http: HttpResponse<OpenSilexResponse<number>> =
      await documentsService.countDocuments(uri.value, undefined, undefined)
    documents.value = http.response.result ?? 0
  } catch (e) {
    opensilex.errorHandler(e)
  } finally {
    documentsCountIsLoading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadProvenance(), searchAnnotations(), searchDocuments()])
}

function convertDtoBeforeEditForm() {
  const form: any = {
    uri: provenance.value.uri,
    name: provenance.value.name,
    description: provenance.value.description,
    activity_type: null,
    activity_start_date: null,
    activity_end_date: null,
    activity_uri: null,
    agents: [],
    publisher: provenance.value.publisher,
    publication_date: provenance.value.issued,
    last_updated_date: provenance.value.modified
  }

  if (
    provenance.value.prov_activity != null &&
    provenance.value.prov_activity.length > 0
  ) {
    form.activity_type = provenance.value.prov_activity[0].rdf_type
    form.activity_start_date = provenance.value.prov_activity[0].start_date
    form.activity_end_date = provenance.value.prov_activity[0].end_date
    form.activity_uri = provenance.value.prov_activity[0].uri
  }

  const uniqueTypes = new Set<string>()
  if (provenance.value.prov_agent != null) {
    provenance.value.prov_agent.forEach((agent: any) => {
      uniqueTypes.add(agent.rdf_type)
    })
  }

  for (const type of uniqueTypes) {
    const agentsByType: string[] = []

    for (const agent of provenance.value.prov_agent as any[]) {
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

function showEditForm() {
  const updateForm = convertDtoBeforeEditForm()
  provenanceForm.value?.showEditForm(updateForm)
}

async function deleteProvenance() {
  try {
    await dataService.deleteProvenance(provenance.value.uri)

    const message =
      t('ProvenanceDetailsPage.title') +
      ' ' +
      provenance.value.uri +
      ' ' +
      t('component.common.success.delete-success-message')

    opensilex.showSuccessToast(message)
    router.push({ path: '/provenances' })
  } catch (error: any) {
    if (error?.response?.result?.message) {
      opensilex.errorHandler(error, error.response.result.message)
    } else {
      opensilex.errorHandler(error)
    }
  }
}

function successMessage(form: any) {
  return t('ProvenanceDetailsPage.success-message') + ' ' + form.name
}

onMounted(() => {
  refreshAll()
})

watch(
  () => route.params.uri,
  () => {
    uri.value = getUriFromRoute()
    refreshAll()
  }
)
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  ProvenanceDetailsPage:
    add: Add provenance
    annotations: Annotations
    documents: Documents
    agents: Provenance agents
    activity: Activity type
    activity_start_date: Start Date
    activity_end_date: End Date
    activity_external_link: External link
    agent-settings: Settings
    update: Update provenance
    title: Provenance
    success-message: Provenance
    delete-message: Provenance


fr:
  ProvenanceDetailsPage:
    add: Ajouter une provenance
    agents: Agents de la provenance
    activity: Type d'activité
    activity_start_date: Date de début
    activity_end_date: Date de fin
    activity_external_link: Lien externe
    agent-settings: Paramètres
    update: Modifier une provenance
    title: Provenance
    success-message: La provenance
    delete-message: La provenance

</i18n>