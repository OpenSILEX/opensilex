<template>
  <div>
    <div class="pageActionsBtns">
      <opensilex-CreateButton
        v-if="user.hasCredential(props.modificationCredentialId)"
        @click="createDocument"
        :label="t('DocumentTabList.add')"
        class="createButton"
      />
    </div>

    <opensilex-StringFilter
      v-if="props.search"
      v-model:filter="filter.title"
      @update="updateFilters"
      :debounce="300"
      :lazy="false"
      :placeholder="t('DocumentTabList.title-placeholder')"
    />
      
      <div>
        <opensilex-PageContent v-if="renderComponent">
          <template #default>
            <opensilex-TableAsyncView
              ref="tableRef"
              :searchMethod="searchDocuments"
              :fields="fields"
              defaultSortBy="name"
            >
              <template #cell(uri)="{ data }">
                <opensilex-UriLink
                  :uri="data.item.uri"
                  :value="data.item.title"
                  :to="{ path: '/document/details/' + encodeURIComponent(data.item.uri) }"
                />
              </template>

              <template #cell(authors)="{ data }">
                <span v-if="data.item.authors">
                  <span v-for="(author, index) in data.item.authors" :key="index">
                    <span :title="author">{{ author }}</span>
                    <span v-if="index + 1 < data.item.authors.length"> - </span>
                  </span>
                </span>
              </template>

              <template #cell(actions)="{ data }">
                <n-button-group size="small" class="btn-group btn-group-sm">
                  <opensilex-EditButton
                    v-if="user.hasCredential(props.modificationCredentialId)"
                    @click="() => editDocument(data.item.uri)"
                    :label="t('DocumentTabList.update')"
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
                    :label="t('DocumentTabList.download')"
                    :small="true"
                    icon="bi-download"
                    variant="outline-info"
                  />
                  <opensilex-Button
                    v-if="data.item.source"
                    @click="() => browseSource(data.item.source)"
                    :label="t('DocumentTabList.browseSource')"
                    :small="true"
                    icon="bi-link-45deg"
                    variant="outline-info"
                  />
                </div>
              </template>
            </opensilex-TableAsyncView>
          </template>
        </opensilex-PageContent>
      </div>

    <opensilex-ModalForm
      v-if="user.hasCredential(props.modificationCredentialId)"
      ref="documentForm"
      component="opensilex-DocumentForm"
      :createTitle="t('DocumentTabList.add')"
      :editTitle="t('DocumentTabList.update')"
      modalSize="lg"
      :data="{ initialTargets}"
      icon="bi#bi-file-text"
      @onCreate="onCreated"
      @onUpdate="onUpdated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, defineProps, defineEmits, onMounted, nextTick, inject} from 'vue';
import { useStore } from 'vuex';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import { DocumentsService } from '../../../../../opensilex-core/front/src/lib';

const props = defineProps({
  uri: [String, Array] as unknown as () => string | string[] | undefined,
  modificationCredentialId: String,
  search: {
    type: Boolean,
    default: true
  }
});

 const initialTargets = computed<string[]>(() => {
   const elementUri = props.uri
   if (!elementUri) return []
   return Array.isArray(elementUri) ? elementUri.filter(Boolean) as string[] : [elementUri]
 })

const emit = defineEmits<{
  (e: 'onUpdate', payload?: any): void
  (e: 'changed', payload?: { reason: 'create' | 'update' | 'deprecated' | 'delete', uri?: string }): void
}>()

const store = useStore();
const route = useRoute();
const { t } = useI18n();
const opensilex = inject<OpenSilexVuePlugin>("$opensilex");

const tableRef = ref();
const documentForm = ref();
const renderComponent = ref(true);

const user = computed(() => store.state.user);

let service: DocumentsService;

onMounted(() => {
  service = opensilex.getService('opensilex.DocumentsService');
});

const fields = computed(() => [
  { key: 'uri', label: t('DocumentTabList.title'), sortable: true },
  { key: 'authors', label: t('DocumentTabList.author'), sortable: true },
  { key: 'date', label: t('DocumentTabList.date'), sortable: true },
  { key: 'rdf_type_name', label: t('DocumentTabList.type'), sortable: true },
  { key: 'actions', label: t('component.common.actions'), resizable:false, sortable: false, naiveProps: {width: 100}}
]);


const filter = ref({
  title: undefined,
  deprecated: 'false',
  date: undefined,
  rdf_type: undefined,
  authors: undefined,
  keywords: undefined,
  targets: undefined,
  multiple: undefined
});

watch(() => props.uri, () => {
  renderComponent.value = false;
  nextTick(() => {
    renderComponent.value = true;
  });
});

function searchDocuments(options) {
  const target: string | undefined = Array.isArray(props.uri) ? props.uri[0] : props.uri;

  return service.searchDocuments(
    undefined,
    filter.value.title,
    undefined,
    target,
    undefined,
    undefined,
    undefined,
    'false',
    options.orderBy,
    options.currentPage,
    options.pageSize
  );
}

function onCreated(payload: any) {
  refresh()
  emit('changed', { reason: 'create' })
  emit('onUpdate', payload)
}

function onUpdated(payload: any) {
  refresh()
  emit('changed', { reason: 'update' })
  emit('onUpdate', payload)
}


function refresh() {
  tableRef.value?.refresh();
}

function createDocument() {
  documentForm.value?.showCreateForm();
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
    .catch(opensilex.errorHandler);
}

function updateForDeprecated(form) {
  return opensilex
    .uploadFileToService('/core/documents', form, null, true)
    .then((http) => {
      emit('onUpdate', form);
      emit('changed', { reason: 'deprecated', uri: form?.description?.uri })
      refresh();
    })
    .catch(opensilex.errorHandler);
}

function editDocument(uri: string) {
 service
    .getDocumentMetadata(uri)
    .then((http) => {
      const document = http.response.result;
      const form = {
        description: { ...document }
      };
      documentForm.value?.showEditForm(form);
    })
    .catch(opensilex.errorHandler);
}

function loadFile(uri: string, title: string, format: string) {
  const path = `/core/documents/${encodeURIComponent(uri)}`;
  opensilex.downloadFilefromService(path, title, format, {});
}

function updateFilters() {
  opensilex.updateURLParameter('name', filter.value.title, '');
  refresh();
}

function resetSearch() {
  filter.value.title = '';
  opensilex.updateURLParameter('name', undefined, undefined);
  refresh();
}

function browseSource(source: string) {
  window.open(source);
}
</script>

<style scoped lang="scss">
.pageActionsBtns {
  margin-left: 10px;
  margin-bottom: 10px;
}
</style>

<i18n>
en:
    DocumentTabList:
        documents: Documents
        uri: URI
        title: Title
        author: Author
        date: Date
        add: Add document
        type: Type
        update: Update document
        delete: Delete Document
        deprecated: Deprecated
        download: Download file
        browseSource: Browse source
        title-placeholder: Enter title

fr:
    DocumentTabList:
        documents: Documents
        uri: URI
        title: Titre
        author: Auteur
        date: Date
        add: Ajouter un document
        type: Type
        update: Modifier le document
        delete: Supprimer le Document
        deprecated: Obsolète
        download: Télécharger le fichier
        browseSource: Naviguer à la source
        title-placeholder: Saisir un titre
</i18n>

