<template>
  <div
    v-if="uri"
    class="container-fluid"
  >
    <PageHeader
      icon="bi#bi-layers"
      has-icon
      :title="name"
      description="component.experiment.view.title"
      class="detail-element-header"
    ></PageHeader>

    <PageActions
      :tabs="true"
      :returnButton="true"
    >
      <template v-slot>
        <router-link
            class="nav-link ml-3, tab"
          :active="isDetailsTab"
          :to="{ path: '/experiment/details/' + encodeURIComponent(uri) }"
          >{{ $t('component.common.description') }}
        </router-link>
        <router-link
            class="nav-link ml-3, tab"
          :active="isFactorsTab"
          :to="{ path: '/experiment/factors/' + encodeURIComponent(uri) }"
          >{{ $t('component.menu.experimentalDesign.factors') }}
          <span
            v-if="!factorsCountIsLoading && factors > 0"
            class="tabWithElements"
          >
            {{ opensilex.$numberFormatter.formateResponse(factors) }}
          </span>
        </router-link>
        <router-link
            class="nav-link ml-3, tab"
          :active="isScientificObjectsTab"
          :to="{ path: '/experiment/scientific-objects/' + encodeURIComponent(uri) }"
          >{{ $t('component.menu.scientificObjectTypes') }}
          <span
            v-if="!scientificObjectsCountIsLoading && scientificObjects > 0"
            class="tabWithElements"
          >
            {{ opensilex.$numberFormatter.formateResponse(scientificObjects) }}
          </span>
        </router-link>
        <router-link
            class="nav-link ml-3, tab"
          :active="isDataTab"
          :to="{ path: '/experiment/data/' + encodeURIComponent(uri) }"
          >{{ $t('component.menu.data.label') }}
          <span
            v-if="!dataCountIsLoading && dataCount > 0"
            class="tabWithElements"
          >
            {{ opensilex.$numberFormatter.formateResponse(dataCount) }}
          </span>
        </router-link>

        <router-link
            class="nav-link ml-3, tab"
          :active="isDatafilesTab"
          :to="{ path: '/experiment/datafiles/' + encodeURIComponent(uri) }"
          >{{ $t('component.menu.data.datafiles') }}
          <span
            v-if="!datafilesCountIsLoading && datafiles > 0"
            class="tabWithElements"
          >
            {{ opensilex.$numberFormatter.formateResponse(datafiles) }}
          </span>
        </router-link>

        <router-link
            class="nav-link ml-3, tab"
          :active="isDataVisualisation"
          :to="{ path: '/experiment/data-visualisation/' + encodeURIComponent(uri) }"
          >{{ $t('component.menu.data.visualization') }}
        </router-link>
        <router-link
            class="nav-link ml-3, tab"
          :active="isMap"
          :to="{ path: '/experiment/map/' + encodeURIComponent(uri) }"
          >{{ $t('component.menu.spatial.map') }}
        </router-link>

        <router-link
            class="nav-link ml-3, tab"
          :active="isAnnotationTab"PageHeader
          :to="{ path: '/experiment/annotations/' + encodeURIComponent(uri) }"
          >{{ $t('component.annotation.list-title') }}
          <span
            v-if="!annotationsCountIsLoading && annotations > 0"
            class="tabWithElements"
          >
            {{ opensilex.$numberFormatter.formateResponse(annotations) }}
          </span>
        </router-link>

        <router-link
            class="nav-link ml-3, tab"
          :active="isDocumentTab"
          :to="{ path: '/experiment/document/' + encodeURIComponent(uri) }"
          >{{ $t('component.project.documents') }}
          <span
            v-if="!documentsCountIsLoading && documents > 0"
            class="tabWithElements"
          >
            {{ opensilex.$numberFormatter.formateResponse(documents) }}
          </span>
        </router-link>
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
import { ExperimentsService } from 'opensilex-core';
import type { ExperimentGetDTO } from 'opensilex-core';
import ExperimentDetail from "@/components/experiments/views/ExperimentDetail.vue";
import PageContent from "@/components/layout/PageContent.vue";
import ExperimentDataVisualisation from "@/components/experiments/ExperimentDataVisualisation.vue";
import DocumentTabList from "@/components/documents/DocumentTabList.vue";
import PageActions from "@/components/layout/PageActions.vue";
import PageHeader from "@/components/layout/PageHeader.vue";
import ExperimentFactors from "@/components/experiments/views/ExperimentFactors.vue";

const route = useRoute();
const store = useStore();
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;

const service = opensilex.getService<ExperimentsService>('opensilex.ExperimentsService');

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

</script>

<style scoped lang="scss"></style>

