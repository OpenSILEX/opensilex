<template>
  <div
    v-if="uri"
    class="container-fluid"
  >
    <PageHeader
      icon="ik#ik-layers"
      :title="name"
      description="component.experiment.view.title"
      class="detail-element-header"
    ></PageHeader>

    <PageActions
      :tabs="true"
      :returnButton="true"
    >
      <template v-slot>
        <b-nav-item
          :active="isDetailsTab"
          :to="{ path: '/experiment/details/' + encodeURIComponent(uri) }"
          >{{ $t('ExperimentView.description') }}
        </b-nav-item>
        <b-nav-item
          :active="isFactorsTab"
          :to="{ path: '/experiment/factors/' + encodeURIComponent(uri) }"
          >{{ $t('ExperimentView.factors') }}
          <span
            v-if="!factorsCountIsLoading && factors > 0"
            class="tabWithElements"
          >
            {{ opensilex.$numberFormatter.formateResponse(factors) }}
          </span>
        </b-nav-item>
        <b-nav-item
          :active="isScientificObjectsTab"
          :to="{ path: '/experiment/scientific-objects/' + encodeURIComponent(uri) }"
          >{{ $t('ExperimentView.scientific-objects') }}
          <span
            v-if="!scientificObjectsCountIsLoading && scientificObjects > 0"
            class="tabWithElements"
          >
            {{ opensilex.$numberFormatter.formateResponse(scientificObjects) }}
          </span>
        </b-nav-item>
        <b-nav-item
          :active="isDataTab"
          :to="{ path: '/experiment/data/' + encodeURIComponent(uri) }"
          >{{ $t('ExperimentView.data') }}
          <span
            v-if="!dataCountIsLoading && dataCount > 0"
            class="tabWithElements"
          >
            {{ opensilex.$numberFormatter.formateResponse(dataCount) }}
          </span>
        </b-nav-item>

        <b-nav-item
          :active="isDatafilesTab"
          :to="{ path: '/experiment/datafiles/' + encodeURIComponent(uri) }"
          >{{ $t('ScientificObjectDataFiles.datafiles') }}
          <span
            v-if="!datafilesCountIsLoading && datafiles > 0"
            class="tabWithElements"
          >
            {{ opensilex.$numberFormatter.formateResponse(datafiles) }}
          </span>
        </b-nav-item>

        <b-nav-item
          :active="isDataVisualisation"
          :to="{ path: '/experiment/data-visualisation/' + encodeURIComponent(uri) }"
          >{{ $t('ExperimentView.data-visualisation') }}
        </b-nav-item>
        <b-nav-item
          :active="isMap"
          :to="{ path: '/experiment/map/' + encodeURIComponent(uri) }"
          >{{ $t('ExperimentView.map') }}
        </b-nav-item>

        <b-nav-item
          :active="isAnnotationTab"
          :to="{ path: '/experiment/annotations/' + encodeURIComponent(uri) }"
          >{{ $t('Annotation.list-title') }}
          <span
            v-if="!annotationsCountIsLoading && annotations > 0"
            class="tabWithElements"
          >
            {{ opensilex.$numberFormatter.formateResponse(annotations) }}
          </span>
        </b-nav-item>

        <b-nav-item
          :active="isDocumentTab"
          :to="{ path: '/experiment/document/' + encodeURIComponent(uri) }"
          >{{ $t('ExperimentView.document') }}
          <span
            v-if="!documentsCountIsLoading && documents > 0"
            class="tabWithElements"
          >
            {{ opensilex.$numberFormatter.formateResponse(documents) }}
          </span>
        </b-nav-item>
      </template>
    </PageActions>

    <PageContent>
      <template v-slot>
        <ExperimentDetail
          v-if="isDetailsTab"
          :uri="uri"
        ></ExperimentDetail>
        <ExperimentFactors
          v-else-if="isFactorsTab"
          :uri="uri"
        ></ExperimentFactors>
        <ExperimentScientificObjects
          v-else-if="isScientificObjectsTab"
          :uri="uri"
        ></ExperimentScientificObjects>
        <ExperimentData
          v-else-if="isDataTab"
          :uri="uri"
        ></ExperimentData>

        <ExperimentDataFiles
          v-else-if="isDatafilesTab"
          :modificationCredentialId="credentials.CREDENTIAL_DATA_MODIFICATION_ID"
          :uri="uri"
        ></ExperimentDataFiles>

        <ExperimentDataVisualisation
          v-else-if="isDataVisualisation"
          :uri="uri"
          :elementName="name"
        ></ExperimentDataVisualisation>

        <MapView
          v-else-if="isMap"
          :uri="uri"
        ></MapView>

        <DocumentTabList
          v-else-if="isDocumentTab"
          :modificationCredentialId="credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID"
          :uri="uri"
        ></DocumentTabList>

        <AnnotationList
          v-else-if="isAnnotationTab"
          ref="annotationList"
          :target="uri"
          :displayTargetColumn="false"
          :enableActions="true"
          :modificationCredentialId="credentials.CREDENTIAL_ANNOTATION_MODIFICATION_ID"
          :deleteCredentialId="credentials.CREDENTIAL_ANNOTATION_DELETE_ID"
        ></AnnotationList>
      </template>
    </PageContent>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, inject, onMounted, useTemplateRef } from 'vue';
import { useRoute } from 'vue-router';
import { useStore } from 'vuex';
import OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin';
import HttpResponse, { OpenSilexResponse } from 'opensilex-core/HttpResponse';
import AnnotationList from '@/components/annotations/list/AnnotationList.vue';
import { AnnotationsService } from 'opensilex-core/api/annotations.service';
import { DocumentsService } from 'opensilex-core/api/documents.service';
import { FactorsService } from 'opensilex-core/api/factors.service';
import { DataService } from 'opensilex-core/api/data.service';
import { ScientificObjectsService } from 'opensilex-core/index';
import { ExperimentsService } from 'opensilex-core';
import type { ExperimentGetDTO } from 'opensilex-core';

const route = useRoute();
const store = useStore();
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const annotationList = useTemplateRef<InstanceType<typeof AnnotationList>>('annotationList');

const service = opensilex.getService<ExperimentsService>('opensilex.ExperimentsService');
const annotationsService = opensilex.getService<AnnotationsService>('opensilex.AnnotationsService');
const documentsService = opensilex.getService<DocumentsService>('opensilex.DocumentsService');
const dataService = opensilex.getService<DataService>('opensilex.DataService');
const factorsService = opensilex.getService<FactorsService>('opensilex.FactorsService');
const scientificObjectsService = opensilex.getService<ScientificObjectsService>(
  'opensilex.ScientificObjectsService'
);

const uri = ref<string | null>(null);
const name = ref('');
const annotations = ref<number>();
const documents = ref<number>();
const factors = ref<number>();
const dataCount = ref<number>();
const scientificObjects = ref<number>();
const datafiles = ref<number>();

const annotationsCountIsLoading = ref(true);
const documentsCountIsLoading = ref(true);
const factorsCountIsLoading = ref(true);
const dataCountIsLoading = ref(true);
const scientificObjectsCountIsLoading = ref(true);
const datafilesCountIsLoading = ref(true);

onMounted(() => {
  uri.value = decodeURIComponent(route.params.uri as string);
  if (uri.value) {
    service
      .getExperiment(uri.value)
      .then((http: HttpResponse<OpenSilexResponse<ExperimentGetDTO>>) => {
        name.value = http.response.result.name;
      })
      .catch((error) => {
        opensilex.errorHandler(error);
      });
  }
});

const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);

const isDetailsTab = computed(() => route.path.startsWith('/experiment/details/'));
const isMap = computed(() => route.path.startsWith('/experiment/map/'));
const isFactorsTab = computed(() => route.path.startsWith('/experiment/factors/'));
const isScientificObjectsTab = computed(() =>
  route.path.startsWith('/experiment/scientific-objects/')
);
const isDataTab = computed(() => route.path.startsWith('/experiment/data/'));
const isDatafilesTab = computed(() => route.path.startsWith('/experiment/datafiles/'));
const isDataVisualisation = computed(() =>
  route.path.startsWith('/experiment/data-visualisation/')
);
const isDocumentTab = computed(() => route.path.startsWith('/experiment/document/'));
const isAnnotationTab = computed(() => route.path.startsWith('/experiment/annotations/'));

function searchAnnotations() {
  return annotationsService
    .countAnnotations(uri.value, undefined, undefined)
    .then((http: HttpResponse<OpenSilexResponse<number>>) => {
      if (http && http.response) {
        annotations.value = http.response.result as number;
        annotationsCountIsLoading.value = false;
        return annotations.value;
      }
    })
    .catch(opensilex.errorHandler);
}

function searchDocuments() {
  return documentsService
    .countDocuments(uri.value, undefined, undefined)
    .then((http: HttpResponse<OpenSilexResponse<number>>) => {
      if (http && http.response) {
        documents.value = http.response.result as number;
        documentsCountIsLoading.value = false;
        return documents.value;
      }
    })
    .catch(opensilex.errorHandler);
}

function searchData() {
  // Limit count of data for performance reasons
  return dataService
    .countData(
      undefined,
      undefined,
      undefined,
      [uri.value],
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      1000,
      undefined,
      undefined
    )
    .then((http: HttpResponse<OpenSilexResponse<number>>) => {
      if (http && http.response) {
        dataCount.value = http.response.result as number;
        dataCountIsLoading.value = false;
        return dataCount.value;
      }
    })
    .catch(opensilex.errorHandler);
}

function searchDatafiles() {
  return dataService
    .countDatafiles(
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      [uri.value],
      undefined,
      undefined
    )
    .then((http: HttpResponse<OpenSilexResponse<number>>) => {
      if (http && http.response) {
        datafiles.value = http.response.result as number;
        datafilesCountIsLoading.value = false;
        return datafiles.value;
      }
    })
    .catch(opensilex.errorHandler);
}

function searchFactors() {
  return factorsService
    .countFactors(uri.value, undefined, undefined)
    .then((http: HttpResponse<OpenSilexResponse<number>>) => {
      if (http && http.response) {
        factors.value = http.response.result as number;
        factorsCountIsLoading.value = false;
        return factors.value;
      }
    })
    .catch(opensilex.errorHandler);
}

function searchScientificObjects() {
  return scientificObjectsService
    .countScientificObjects(uri.value)
    .then((http: HttpResponse<OpenSilexResponse<number>>) => {
      if (http && http.response) {
        scientificObjects.value = http.response.result as number;
        scientificObjectsCountIsLoading.value = false;
        return scientificObjects.value;
      }
    })
    .catch(opensilex.errorHandler);
}
</script>

<style scoped lang="scss"></style>

<i18n>
en:
    ExperimentView:
        description: Description
        scientific-objects: Scientific objects
        data: Data
        document: Documents
        factors: Factors
        map: Map
        data-visualisation: Visualization
fr:
    ExperimentView:
        description: Description
        scientific-objects: Objets scientifiques
        data: Données
        document: Documents
        factors: Facteurs
        map: Carte
        data-visualisation: Visualisation
</i18n>
