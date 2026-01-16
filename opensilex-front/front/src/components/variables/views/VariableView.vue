<template>
  <div class="container-fluid py-3">
    <!-- Header -->
    <opensilex-PageHeader
      icon="fa#sun"
      :hasIcon="true"
      :title="variable?.name"
      description="component.variable.type"
      class="detail-element-header"
    />

    <!-- Onglets -->
    <opensilex-PageActions :tabs="true" :returnButton="true" class="navigationTabs">
      <template v-slot>
        <nav class="tabs mb-3">
          <button
            :class="['tab', { active: isDetailsTab }]"
            @click="go('details')"
          >
            {{ t('component.common.details-label') }}
          </button>

          <button
            :class="['tab', { active: isAnnotationTab }]"
            @click="go('annotations')"
          >
            {{ t('Annotations') }}
            <span v-if="!annotationsCountIsLoading && annotations > 0" class="tabBadge">
              {{ opensilex?.$numberFormatter?.formateResponse(annotations) ?? annotations }}
            </span>
          </button>

          <button
            v-if="onLocalInstance"
            :class="['tab', { active: isVisualizationTab }]"
            @click="go('visualization')"
          >
            {{ t('component.variable.visualization') }}
          </button>

          <button
            :class="['tab', { active: isDocumentTab }]"
            @click="go('documents')"
          >
            {{ t('component.project.documents') }}
            <span v-if="!documentsCountIsLoading && documents > 0" class="tabBadge">
              {{ opensilex?.$numberFormatter?.formateResponse(documents) ?? documents }}
            </span>
          </button>
        </nav>
      </template>
    </opensilex-PageActions>

    <!-- Contenu -->
    <opensilex-PageContent>
      <template #default>
        <opensilex-VariableDetails
          v-if="isDetailsTab"
          :variable="variable"
          :displayLocalActions="onLocalInstance"
          @onUpdate="updateVariable"
        />

        <opensilex-AnnotationList
          v-else-if="isAnnotationTab"
          class="projectAnnotations"
          ref="annotationList"
          :target="uri"
          :displayTargetColumn="false"
          :enableActions="true"
          :modificationCredentialId="credentials.CREDENTIAL_ANNOTATION_MODIFICATION_ID"
          :deleteCredentialId="credentials.CREDENTIAL_ANNOTATION_DELETE_ID"
        />

        <opensilex-VariableVisualizationTab
          v-else-if="isVisualizationTab"
          :variable="uri"
          :elementName="variable?.name"
          :modificationCredentialId="credentials.CREDENTIAL_DEVICE_MODIFICATION_ID"
        />

        <opensilex-DocumentTabList
          v-else-if="isDocumentTab"
          :uri="uri"
          :modificationCredentialId="credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID"
        />
      </template>
    </opensilex-PageContent>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useStore } from 'vuex';
import { useRoute, useRouter } from 'vue-router';

import type { OpenSilexVuePlugin } from '@/models/OpenSilexVuePlugin';
import  HttpResponse, { OpenSilexResponse } from 'opensilex-core/HttpResponse';
import type { VariablesService, AnnotationsService, DocumentsService, VariableDetailsDTO } from 'opensilex-core/index';

const { t } = useI18n();
const store = useStore();
const route = useRoute();
const router = useRouter();

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const variablesService = opensilex.getService<VariablesService>('opensilex.VariablesService');
const annotationsService = opensilex.getService<AnnotationsService>('opensilex.AnnotationsService');
const documentsService = opensilex.getService<DocumentsService>('opensilex.DocumentsService');

const credentials = computed(() => store.state.credentials);

// État principal
const variable = ref<VariableDetailsDTO | null>(null);
const uri = ref<string>('');
const onLocalInstance = ref(true);

// Badges
const annotations = ref<number>(0);
const documents = ref<number>(0);
const annotationsCountIsLoading = ref(true);
const documentsCountIsLoading = ref(true);

// Onglet actif (via route)
const isDetailsTab = computed(() => route.path.startsWith('/variable/details/'));
const isAnnotationTab = computed(() => route.path.startsWith('/variable/annotations/'));
const isVisualizationTab = computed(() => route.path.startsWith('/variable/visualization/'));
const isDocumentTab = computed(() => route.path.startsWith('/variable/documents/'));

// DTO vide
function emptyDetails(): VariableDetailsDTO {
  return {
    uri: undefined,
    alternative_name: undefined,
    name: undefined,
    entity: undefined,
    entity_of_interest: undefined,
    characteristic: undefined,
    description: undefined,
    time_interval: undefined,
    sampling_interval: undefined,
    datatype: undefined,
    trait: undefined,
    trait_name: undefined,
    method: undefined,
    unit: undefined,
    exact_match: [],
    close_match: [],
    broad_match: [],
    narrow_match: [],
    species: undefined,
    publisher: undefined,
    publication_date: undefined,
    last_updated_date: undefined
  };
}

// Navigation par bouton
function getPath(tab: 'details'|'annotations'|'visualization'|'documents'): string {
  const base = `/variable/${tab}/${encodeURIComponent(uri.value)}`;
  const sri = route.query.sharedResourceInstance as string | undefined;
  return sri ? `${base}?sharedResourceInstance=${sri}` : base;
}
function go(tab: 'details'|'annotations'|'visualization'|'documents') {
  router.push({ path: getPath(tab) });
}

// Chargements
async function loadVariable(targetUri: string, sharedResourceInstance?: string) {
  try {
    const http = await variablesService.getVariable(targetUri, sharedResourceInstance);
    variable.value = http.response.result;
  } catch (e) {
    opensilex.errorHandler(e);
  }
}
async function searchAnnotations() {
  try {
    const http = await annotationsService.countAnnotations(uri.value, undefined, undefined);
    if (http?.response) {
      annotations.value = (http.response as OpenSilexResponse<number>).result as number;
    }
  } catch (e) {
    opensilex.errorHandler(e);
  } finally {
    annotationsCountIsLoading.value = false;
  }
}
async function searchDocuments() {
  try {
    const http = await documentsService.countDocuments(uri.value, undefined, undefined);
    if (http?.response) {
      documents.value = (http.response as OpenSilexResponse<number>).result as number;
    }
  } catch (e) {
    opensilex.errorHandler(e);
  } finally {
    documentsCountIsLoading.value = false;
  }
}

// Appelé quand VariableDetails émet onUpdate
function updateVariable(updated: VariableDetailsDTO) {
  uri.value = updated.uri as string;
  loadVariable(uri.value, undefined);
  // rafraîchir badges
  annotationsCountIsLoading.value = true;
  documentsCountIsLoading.value = true;
  searchAnnotations();
  searchDocuments();
}

// Init
onMounted(() => {
  uri.value = decodeURIComponent(route.params.uri as string);

  const encodedSri = route.query['sharedResourceInstance'] as string | undefined;
  const sriUrl = encodedSri ? decodeURIComponent(encodedSri) : undefined;
  onLocalInstance.value = sriUrl === undefined;

  variable.value = emptyDetails();
  loadVariable(uri.value, sriUrl);
  searchAnnotations();
  searchDocuments();
});

// Si l’URL change (navigation entre onglets / autre variable)
watch(
  () => route.fullPath,
  () => {
    uri.value = decodeURIComponent(route.params.uri as string);

    const encodedSri = route.query['sharedResourceInstance'] as string | undefined;
    const sriUrl = encodedSri ? decodeURIComponent(encodedSri) : undefined;
    onLocalInstance.value = sriUrl === undefined;

    loadVariable(uri.value, sriUrl);
    annotationsCountIsLoading.value = true;
    documentsCountIsLoading.value = true;
    searchAnnotations();
    searchDocuments();
  }
);
</script>

<style scoped>
.projectAnnotations{
  margin-top: 18px;
}
.navigationTabs {
  margin-bottom: -9px;
}
</style>
