<template>
  <!-- Barre Actions / Counts / Selection -->
  <n-space
    class="listActionButtons"
    :class="[filtersCollapsed ? 'filtersNotCollapsed' : 'filtersCollapsed']"
  >
      <!-- Bouton Create Document -->
    <CreateButton
      v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
      size="small"
      label="component.common.addDocument"
      @click="documentForm.showCreateForm(initForm())"
      class="createButton"
    />
  </n-space>

  <!-- Layout -->
  <n-layout has-sider class="document-layout">
    <!-- Bouton loupe -->
    <n-space class="mb-2 me-1" align="top">
      <n-button
        quaternary
        circle
        @click="filtersCollapsed = !filtersCollapsed"
        :title="t('DocumentList.label-filter')"
        :class="{ greenThemeColor: filtersCollapsed }"
        class="globalFiltersSearchButton"
      >
        <i class="bi bi-search filtersGlobalSearchIcon"></i>
        <div v-show="filtersCollapsed && activeFiltersCount > 0" class="filters-count-badge">
          ( {{ activeFiltersCount }} )
        </div>
      </n-button>
    </n-space>

    <!-- Sidebar / Filtres -->
    <n-layout-sider
      v-model:collapsed="filtersCollapsed"
      :collapsed-width="0"
      :width="360"
      collapse-mode="width"
      show-trigger
      bordered
      class="document-sider"
    >
      <n-space class="p-3" vertical>
        <n-form label-placement="top" size="small" @submit.prevent="applyFilters">
          <n-form-item :label="t('component.common.title')">
            <n-input
              v-model:value="filter.title"
              clearable
              :placeholder="t('DocumentList.filter-title-placeholder')"
              @keydown.enter.prevent.stop="applyFilters"
            />
          </n-form-item>

          <n-form-item :label="t('DocumentList.filter-date')">
            <n-input
              v-model:value="filter.date"
              :placeholder="t('DocumentList.filter-date-placeholder')"
              @handlingEnterKey="applyFilters"
            />
          </n-form-item>

          <n-form-item :label="t('DocumentList.filter-author')">
            <n-input
              v-model:value="filter.authors"
              :placeholder="t('DocumentList.filter-author-placeholder')"
              @handlingEnterKey="applyFilters"
            />
          </n-form-item>

          <n-form-item :label="t('DocumentList.filter-keywords')">
            <n-input
              v-model:value="filter.keywords"
              :placeholder="t('DocumentList.filter-keywords-placeholder')"
              @handlingEnterKey="applyFilters"
            />
          </n-form-item>

          <n-form-item>
            <n-checkbox v-model:value="filter.deprecated">
              {{ t('DocumentList.filter-deprecated') }}
            </n-checkbox>
          </n-form-item>

          <n-space justify="end" class="mt-2">
            <Button
              class="resetButton"
              :label="t('component.common.search.clear-button')"
              icon="bi-x-lg"
              @click="resetFilters"
            />
            <Button
              class="greenThemeColor"
              :label="t('component.common.search.search-button')"
              icon="bi-search"
              @click="applyFilters"
            />
          </n-space>
        </n-form>
      </n-space>
    </n-layout-sider>

    <!-- Contenu Liste -->
    <n-layout-content class="document-content">
      <TableAsyncView
        ref="tableRef"
        :searchMethod="loadData"
        :fields="fields"
        defaultSortBy="title"
        :defaultPageSize="pageSize"
        :showHeaderCount="false"
        labelNumberOfSelectedRow="component.document.selectedLabel"
        @select="emit('select', $event)"
        @unselect="emit('unselect', $event)"
        @selectall="emit('selectall', $event)"
      >
        <template #cell(title)="{ data }">
          <UriLink
            :uri="data.item.uri"
            :value="data.item.title"
            :to="{ path: '/document/details/' + encodeURIComponent(data.item.uri) }"
            :allowCopy="true"
          />
        </template>

        <template #cell(date)="{ data }">
          <DateView :value="data.item.date" />
        </template>

        <template #cell(authors)="{ data }">
          <span v-for="(author, index) in data.item.authors || []" :key="index">
            <span :title="author">{{ author }}</span>
            <span v-if="index + 1 < (data.item.authors || []).length"> - </span>
          </span>
        </template>

        <template #cell(actions)="{ data }">
          <n-button-group size="small" class="btn-group btn-group-sm">
            <EditButton
              v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
              @click="editDocument(data.item.uri)"
              label="component.document.update"
              :small="true"
            />
            <DeprecatedButton
              v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
              @click="toggleDeprecated(data.item.uri)"
              :small="true"
              :deprecated="data.item.deprecated"
            />
          </n-button-group>
        </template>
      </TableAsyncView>

      <!-- Formulaire Creation Document -->
      <ModalForm
        v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
        ref="documentForm"
        component="opensilex-DocumentForm"
        createTitle="component.common.addDocument"
        modalSize="lg"
        :initForm="initForm"
        icon="bi#bi-file-text"
      />
    </n-layout-content>
  </n-layout>
</template>

<script setup lang="ts">
import { computed, inject, nextTick, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useStore } from 'vuex'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import { DocumentsService } from 'opensilex-core/index'
import CreateButton from "@/components/common/buttons/CreateButton.vue";
import ModalForm from "@/components/common/forms/ModalForm.vue";
import DeprecatedButton from "@/components/common/buttons/DeprecatedButton.vue";
import EditButton from "@/components/common/buttons/EditButton.vue";
import DateView from "@/components/common/views/DateView.vue";
import UriLink from "@/components/common/views/UriLink.vue";
import TableAsyncView from "@/components/common/views/TableAsyncView.vue";
import Button from "@/components/common/buttons/Button.vue";
import {TableField} from "@/components/common/views/TableField";

const emit = defineEmits<{
  (e: 'onEdit', document: any): void
  (e: 'select', v: any): void
  (e: 'unselect', v: any): void
  (e: 'selectall', v: any): void
}>()

const props = withDefaults(defineProps<{
  noActions?: boolean
  pageSize?: number
  showCount?: boolean
}>(), {
  noActions: false,
  pageSize: 20,
  showCount: true
})

const { t, n } = useI18n()
const route = useRoute()
const store = useStore()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = $opensilex.getService<DocumentsService>('opensilex.DocumentsService')

const tableRef = ref<any>(null)
const documentForm = ref<any>(null)

const filtersCollapsed = ref(true)

const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)

const filter = ref({
  title: '',
  date: '',
  authors: '',
  keywords: '',
  deprecated: false
})

onMounted(() => {
  $opensilex.updateFiltersFromURL(route.query, filter.value)
})

const activeFiltersCount = computed(() =>
  Object.values(filter.value).filter(v => v !== undefined && v !== '').length
)

const fields = computed(() => {
  const tableFields: TableField[] = [
    { key: 'title', label: 'component.common.title', sortable: true },
    { key: 'date', label: 'component.common.date', sortable: true },
    { key: 'authors', label: 'component.common.authors' },
  ]
  if (!props.noActions) tableFields.push({ key: 'actions', label: 'component.common.actions', resizable: false, naiveProps: {width: 100} })
  return tableFields
})


const paginationInfo = computed(() => {
  return tableRef.value?.getPaginationInfo?.() ?? { start: 0, end: 0, total: 0, hasResults: false }
})

function applyFilters() {
  tableRef.value?.setPage?.(1)
  nextTick(() => tableRef.value?.refresh?.())
  $opensilex.updateURLParameters(filter.value)
  filtersCollapsed.value = true
}

function resetFilters() {
  for (const key of Object.keys(filter.value)) {
    filter.value[key] = key === 'deprecated' ? false : ''
  }
  tableRef.value?.setPage?.(1)
  nextTick(() => tableRef.value?.refresh?.())
}

async function loadData(options: any) {
  return await service.searchDocuments(
    undefined, // rdf_type (tu ne l’utilises plus)
    filter.value.title,
    filter.value.date,
    undefined, // targets
    filter.value.authors,
    filter.value.keywords,
    undefined, // multiple
    String(filter.value.deprecated),
    options.orderBy,
    options.currentPage,
    options.pageSize
  )
}

function getSelected() {
  return tableRef.value?.getSelected?.() ?? []
}

function initForm() {
  const targetURI: string[] = getSelected().map(s => s.uri)
  return {
    description: { targets: targetURI },
    file: undefined
  }
}


async function toggleDeprecated(uri: string) {
  try {
    const http = await service.getDocumentMetadata(uri)
    const document = http.response?.result || http.result

    const form = {
      description: {
        uri: document.uri,
        identifier: document.identifier,
        rdf_type: document.rdf_type,
        title: document.title,
        date: document.date,
        description: document.description,
        targets: document.targets,
        authors: document.authors,
        language: document.language,
        format: document.format,
        deprecated: !document.deprecated,
        keywords: document.keywords,
        source: document.source
      }
    }

    await $opensilex.uploadFileToService(
      "/core/documents",
      form,
      null,
      true
    )

    tableRef.value?.refresh?.()
  } catch (err) {
    console.error(err)
  }
}

defineExpose({ refresh: () => tableRef.value?.refresh?.() })

async function editDocument(uri: string) {
  try {
    const http = await service.getDocumentMetadata(uri)
    const document = http.result || http.response?.result

    const form = {
      description: {
        uri: document.uri,
        identifier: document.identifier,
        rdf_type: document.rdf_type,
        title: document.title,
        date: document.date,
        description: document.description,
        targets: document.targets,
        authors: document.authors,
        language: document.language,
        format: document.format,
        deprecated: document.deprecated,
        keywords: document.keywords,
        source: document.source
      }
    }

    documentForm.value?.showEditForm(form)
  } catch (err) {
    console.error(err)
  }
}
</script>

<style>
.document-layout { 
  background: transparent; 
}

.document-sider {
  background: #fff;
}

.document-content { 
  padding-left: 12px; 
}

.listActionButtons { 
  position: relative; 
  display: flex; 
  gap: 0 !important; 
}

.displayAndListSelectionCount { 
  display: flex; 
  align-items: center; 
  gap: 8px; 
  }

.filtersNotCollapsed { 
  margin-left: 55px; 
  }

.filtersGlobalSearchIcon { 
  font-size: 1.2em; 
  }

.globalFiltersSearchButton { 
  width: 40px; 
  height: 55px; 
  }

.fade-enter-active, 
.fade-leave-active { 
  transition: all 0.3s ease; 
  }

.fade-enter-from, 
.fade-leave-to { 
  opacity: 0; 
  transform: translateX(-10px); 
  }

.fade-enter-to, 
.fade-leave-from { 
  opacity: 1; 
  transform: translateX(0); }

.btn-disabled { 
  background-color: #e0e0e0 !important; 
  color: #2e2e2e !important; 
  border: none !important; 
  cursor: not-allowed; 
  }
</style>

<i18n>
en:
  DocumentList:
    selected: Selected Document(s)
    selected-all: All documents
    display: Display
    label-filter: Search documents
    filter-title-placeholder: Filter by title
    filter-date-placeholder: Filter by date
    filter-author-placeholder: Filter by author
    filter-keywords-placeholder: Filter by keywords
    filter-deprecated: Deprecated
fr:
  DocumentList:
    selected: Document(s) sélectionné(s)
    selected-all: Tous les documents
    display: Affichage
    label-filter: Rechercher des documents
    filter-title-placeholder: Filtrer par titre
    filter-date-placeholder: Filtrer par date
    filter-author-placeholder: Filtrer par auteur
    filter-keywords-placeholder: Filtrer par mots-clés
    filter-deprecated: Obsolète
</i18n>