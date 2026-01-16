<template>
  <div class="container-fluid py-3">
    <!-- Header -->
    <opensilex-PageHeader
      :title="name"
      class="detail-element-header"
      description="component.project.project"
    />

    <!-- Actions principales -->
    <opensilex-PageActions :returnButton="true">

    <!-- Onglets -->
    <nav class="tabs mb-3">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="['tab', { active: currentTab === tab.key }]"
        @click="goToTab(tab.key)"
        type="button"
      >
        {{ tab.label }}

        <!-- Badges -->
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

    <!-- Composant dynamique selon l'onglet -->
    <opensilex-PageContent>
      <component
        v-if="currentTabComponent"
        :is="currentTabComponent"
        :ref="currentTabRef"
        v-bind="currentTabProps"
        @changed="onTabChanged"
        v-on="currentTabListeners"
      />
    </opensilex-PageContent>
  </div>
</template>

<script setup lang="ts">
import { ref, type Ref, computed, inject, defineAsyncComponent, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'

import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { ProjectsService, ProjectGetDetailDTO } from 'opensilex-core/index'
import type { AnnotationsService } from 'opensilex-core/api/annotations.service'
import type { DocumentsService } from 'opensilex-core/api/documents.service'
import type HttpResponse from 'opensilex-security/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const store = useStore()
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

const credentials = computed(() => store.state.credentials)

const uri = ref<string>('')
const name = ref<string>('')

const annotations = ref<number>(0)
const documents = ref<number>(0)
const annotationsCountIsLoading = ref(true)
const documentsCountIsLoading = ref(true)

// services
const projectsService = opensilex.getService<ProjectsService>('opensilex.ProjectsService')
const annotationsService = opensilex.getService<AnnotationsService>('opensilex.AnnotationsService')
const documentsService = opensilex.getService<DocumentsService>('opensilex.DocumentsService')

// Onglets
const tabDefinitions = [
  { key: 'details',     labelKey: 'component.project.details',   component: () => import('./details/ProjectDescription.vue'),            refKey: 'projectDescription' },
  { key: 'annotations', labelKey: 'Annotations',                 component: () => import('../annotations/list/AnnotationList.vue'), refKey: 'annotationList' },
  { key: 'documents',   labelKey: 'component.project.documents', component: () => import('../documents/DocumentTabList.vue'),    refKey: 'documentTabList' }
] as const

type TabKey = typeof tabDefinitions[number]['key']

const tabs = computed(() =>
  tabDefinitions.map(({ key, labelKey }) => ({ key, label: t(labelKey) }))
)

const tabComponents = Object.fromEntries(
  tabDefinitions.map(tab => [tab.key, defineAsyncComponent(tab.component)])
) as Record<TabKey, any>

const formRefs: Record<string, Ref<any>> = {}
tabDefinitions.forEach(tab => { formRefs[tab.refKey] = ref(null) })

const currentTabComponent = computed(() => tabComponents[currentTab.value])

const currentTabRef = computed(() => {
  const def = tabDefinitions.find(t => t.key === currentTab.value)
  return def ? formRefs[def.refKey] : undefined
})

// Tab courant = URL (route.path)
function getTabFromPath(): TabKey {
  const tabPath = route.path
  if (tabPath.startsWith('/project/annotations/')) return 'annotations'
  if (tabPath.startsWith('/project/documents/')) return 'documents'
  return 'details'
}
const currentTab = ref<TabKey>(getTabFromPath())

watch(
  () => route.path,
  () => { currentTab.value = getTabFromPath() }
)

// Click onglet => navigation URL
function goToTab(key: TabKey) {
  if (!uri.value) return
  const base =
    key === 'details'
      ? '/project/details/'
      : key === 'annotations'
        ? '/project/annotations/'
        : '/project/documents/'
  router.push({ path: base + encodeURIComponent(uri.value) })
}

// Props envoyées au composant courant
const currentTabProps = computed(() => {
  if (!uri.value) return {}

  if (currentTab.value === 'details') {
    return { uri: uri.value }
  }

  if (currentTab.value === 'documents') {
    return {
      uri: uri.value,
      debounce: 300,
      lazy: false,
      modificationCredentialId: credentials.value.CREDENTIAL_DOCUMENT_MODIFICATION_ID
    }
  }

  // annotations
  return {
    target: uri.value,
    displayTargetColumn: false,
    enableActions: true,
    modificationCredentialId: credentials.value.CREDENTIAL_ANNOTATION_MODIFICATION_ID,
    deleteCredentialId: credentials.value.CREDENTIAL_ANNOTATION_DELETE_ID
  }
})

function onTabChanged(payload?: any) {
  // si l'event vient de DocumentTabList ou AnnotationList
  // on rafraîchit uniquement ce qui correspond
  if (currentTab.value === 'documents') {
    searchDocuments()     // ou refreshDocumentsCount()
  } else if (currentTab.value === 'annotations') {
    searchAnnotations()
  }
}

const currentTabListeners = computed(() => {
  if (currentTab.value === 'annotations') {
    return { changed: searchAnnotations }
  }
  if (currentTab.value === 'documents') {
    return { changed: searchDocuments }
  }
  return {}
})

// Data loading
function syncUriFromRoute() {
  const raw = route.params.uri
  const param = Array.isArray(raw) ? raw[0] : (raw as string | undefined)
  uri.value = param ? decodeURIComponent(param) : ''
}

function formatCount(v: number) {
  const nf = (opensilex as any).$numberFormatter
  return nf?.formateResponse ? nf.formateResponse(v) : String(v)
}

async function loadProject() {
  if (!uri.value) return
  try {
    const http: HttpResponse<OpenSilexResponse<ProjectGetDetailDTO>> =
      await projectsService.getProject(uri.value)
    name.value = http.response.result?.name ?? ''
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
  await Promise.all([loadProject(), searchAnnotations(), searchDocuments()])
}

onMounted(() => {
  syncUriFromRoute()
  refreshAll()
})

watch(
  () => route.params.uri,
  () => {
    syncUriFromRoute()
    refreshAll()
  }
)
</script>

<style scoped>
</style>
