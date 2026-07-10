<template>
  <n-layout has-sider class="document-layout">
    
    <n-space class="mb-2 me-1" align="top">
      <n-button
        quaternary
        circle
        @click="filtersCollapsed = !filtersCollapsed"
        :title="t('searchfilter.label')"
        :class="{ greenThemeColor: filtersCollapsed }"
        class="globalFiltersSearchButton"
      >
        <i class="bi bi-search filtersGlobalSearchIcon"></i>

        <div
          v-show="filtersCollapsed && activeFiltersCount > 0"
          class="filters-count-badge"
        >
          ( {{ activeFiltersCount }} )
        </div>
      </n-button>
    </n-space>

    <n-layout-sider
      v-model:collapsed="filtersCollapsed"
      :collapsed-width="0"
      :width="360"
      collapse-mode="width"
      show-trigger
      bordered
    >
      <n-space class="p-3" vertical>
        <n-form
          label-placement="top"
          size="small"
          @submit.prevent.stop="applyFilters"
        >

          <!-- Title -->
          <n-form-item class="compact-form-item">
            <n-input
              v-model:value="filter.multiple"
              clearable
              :placeholder="t('component.document.searchAll-placeholder')"
              @keydown.enter.prevent.stop="applyFilters"
              class="searchFilter"
            />
          </n-form-item>

           <!-- Advanced -->
          <n-collapse
            v-model:expanded-names="expandedNames"
            :accordion="false"
            @update:expanded-names="onCollapseUpdate"
            class="advancedFiltersSearch"
          >
          <n-collapse-item :title="t('component.common.advanced-search-title')" name="adv">

            <!-- Title -->
            <n-form-item class="compact-form-item" :label="t('component.common.title')">
              <n-input
                v-model:value="filter.title"
                :placeholder="t('component.document.filter-title-placeholder')"
                @keydown.enter.prevent.stop="applyFilters"
                class="searchFilter"
              />
            </n-form-item>

            <!-- Type -->
            <n-form-item  class="compact-form-item">
              <opensilex-TypeForm
                v-model:type="filter.rdf_type"
                :baseType="$opensilex.Oeso.DOCUMENT_TYPE_URI"
                @keydown.enter.prevent.stop="applyFilters"
                class="searchFilter"
              />
            </n-form-item> 

            <!-- date -->   
            <n-form-item class="compact-form-item" :label="t('component.document.date')">
              <n-input
                v-model:value="filter.date"
                :placeholder="t('component.document.filter-date-placeholder')"
                @keydown.enter.prevent.stop="applyFilters"
                class="searchFilter"
              />
            </n-form-item>

            <!-- targets -->
            <n-form-item class="compact-form-item" :label="t('component.document.targets')">
              <n-input
                v-model:value="filter.targets"
                :placeholder="t('component.document.title-placeholder')"
                @keydown.enter.prevent.stop="applyFilters"
                class="searchFilter"
              />
            </n-form-item>

            <!-- author -->
            <n-form-item class="compact-form-item" :label="t('component.document.author')">
              <n-input
                v-model:value="filter.authors"
                :placeholder="t('component.document.filter-author-placeholder')"
                @handlingEnterKey="applyFilters"
                class="searchFilter"
              />
            </n-form-item>

            <!-- keywords -->
            <n-form-item class="compact-form-item" :label="t('component.document.keywords')">
              <n-input
                v-model:value="filter.keywords"
                :placeholder="t('component.document.filter-keywords-placeholder')"
                @keydown.enter.prevent.stop="applyFilters"
                class="searchFilter"
              />
            </n-form-item>

            <!-- Deprecated -->
            <n-form-item class="compact-form-item" :label="t('component.document.filter-deprecated')">
              <n-switch
                v-model:value="filter.deprecated" 
              ></n-switch>
            </n-form-item> 

          </n-collapse-item>
        </n-collapse>


          <n-space justify="end" class="mt-2">
            <opensilex-Button
              class="resetButton"
              :label="t('component.common.search.clear-button')"
              icon="bi-x-lg"
              @click="resetFilters"
            />
            <opensilex-Button
              class="greenThemeColor"
              :label="t('component.common.search.search-button')"
              icon="bi-search"
              @click="applyFilters"
            />
          </n-space>
        </n-form>
      </n-space>
    </n-layout-sider>

    <n-layout-content class="document-content">
      <div class="pageActionsBtns">
        <opensilex-CreateButton
          v-if="user.hasCredential(props.modificationCredentialId)"
          @click="createDocument"
          :label="t('component.common.addDocument')"
          class="createButton"
        />
      </div>

      <div class="docCountRight">
        <div v-if="paginationInfo.hasResults">
          <strong>
            {{ t('component.common.list.pagination.nbEntries', {
              limit: paginationInfo.start,
              offset: paginationInfo.end,
              totalRow: n(paginationInfo.total)
            }) }}
          </strong>
        </div>
        <div v-else>
          <strong>{{ t('component.common.list.pagination.noEntries') }}</strong>
        </div>
      </div>

      <opensilex-TableAsyncView
        ref="tableRef"
        :searchMethod="loadData"
        :fields="fields"
        defaultSortBy="title"
        :defaultPageSize="pageSize"
      >
        <template #cell(uri)="{ data }">
          <opensilex-UriLink
            :uri="data.item.uri"
            :value="data.item.title"
            :to="{ path: '/document/details/' + encodeURIComponent(data.item.uri) }"
            :allowCopy="true"
          />
        </template>

        <template #cell(authors)="{ data }">
          <span v-for="(author, index) in data.item.authors || []" :key="index">
            <span :title="author">{{ author }}</span>
            <span v-if="index + 1 < (data.item.authors || []).length"> - </span>
          </span>
        </template>

        <template #cell(actions)="{ data }">
          <div class="btn-group btn-group-sm">
            <opensilex-EditButton
              v-if="user.hasCredential(props.modificationCredentialId)"
              @click="() => editDocument(data.item.uri)"
              label="component.document.update"
              :small="true"
            />

            <opensilex-DeprecatedButton
              v-if="user.hasCredential(props.modificationCredentialId)"
              :deprecated="data.item.deprecated"
              @click="() => deprecatedDocument(data.item.uri)"
              :small="true"
            />

            <opensilex-Button
              v-if="!data.item.source"
              component="opensilex-DocumentDetails"
              @click="() => loadFile(data.item.uri, data.item.title, data.item.format)"
              label="component.document.download"
              :small="true"
              icon="bi-download"
              variant="outline-info"
            />
            <opensilex-Button
              v-if="data.item.source"
              @click="() => browseSource(data.item.source)"
              label="component.document.browseSource"
              :small="true"
              icon="bi-link-45deg"
              variant="outline-info"
            />

          </div>
        </template>
      </opensilex-TableAsyncView>

      <opensilex-ModalForm
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
import {
  NLayout,
  NLayoutSider,
  NLayoutContent,
  NForm,
  NFormItem,
  NInput,
  NButton,
  NSpace,
  NButtonGroup,
  NCollapse, 
  NCollapseItem,
  NSwitch
} from "naive-ui";

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
const expandedNames = ref<string[]>([])
const loadAdvancedSearchFilters = ref(false)

const filter = ref({
  rdf_type:'',
  title: '',
  date: '',
  targets: '',
  authors: '',
  keywords: '',
  multiple: '',
  deprecated: false
})

onMounted(() => {
  $opensilex.updateFiltersFromURL(route.query, filter.value)
})

const activeFiltersCount = computed(() =>
  Object.values(filter.value).filter(v => v !== undefined && v !== '').length
)

const fields = computed(() => [
  { key: 'uri', label: 'component.common.title', sortable: true },
  { key: 'date', label: 'component.common.date', sortable: true },
  { key: 'rdf_type_name', label: 'component.common.type', sortable: true },
  { key: 'actions', label: 'component.common.actions', sortable: false }
]);

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

function onCollapseUpdate(names: string[]) {
  expandedNames.value = names
  if (names.includes('adv')) {
    loadAdvancedSearchFilters.value = true
  }
}

async function loadData(options: any) {
  return await service.searchDocuments(
    filter.value.rdf_type, 
    filter.value.title,
    filter.value.date,
    filter.value.targets,
    filter.value.authors,
    filter.value.keywords,
    filter.value.multiple,
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


function deprecatedDocument(uri: string) {
  service
    .getDocumentMetadata(uri)
    .then((http) => {
      const document = http.response.result;
      const form = {
        description: { ...document, deprecated: true }
      };
      updateForDeprecated(form);
    })
    .catch($opensilex.errorHandler);
}

function updateForDeprecated(form) {
  return $opensilex
    .uploadFileToService('/core/documents', form, null, true)
    .then((http) => {
      emit('onUpdate', form);
      emit('changed', { reason: 'deprecated', uri: form?.description?.uri })
      refresh();
    })
    .catch($opensilex.errorHandler);
}

function refresh() {
  tableRef.value?.refresh();
}
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

function loadFile(u: string, title?: string | null, format?: string | null) {
  const path = '/core/documents/' + encodeURIComponent(u)
  $opensilex.downloadFilefromService(path, title ?? '', format ?? '')
}

function browseSource(source: string) {
    window.open(source);
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

.filtersGlobalSearchIcon {
  font-size: 1.2em;
}

.globalFiltersSearchButton {
  width: 40px;
  height: 55px;
}

.globalFiltersSearchButton span {
  display: block !important;
}

.globalFiltersSearchButton div {
  margin-top: 5px;
}

.typeFilter {
  margin-bottom: 15px;
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

.document-layout {
  background: transparent;
}

.docCountRight{
  text-align: right;
  white-space: nowrap;
}

.advancedFiltersSearch {
  margin-top: 10px;
}
</style>
