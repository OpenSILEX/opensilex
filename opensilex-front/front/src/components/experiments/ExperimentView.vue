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
            class="nav-link ml-3 tab"
            :class="{ active: isExperimentTab(ExperimentTab.DETAILS) }"
            :to="experimentTabPath(ExperimentTab.DETAILS)"> {{ $t('component.common.description') }}
        </router-link>
        <router-link
            class="nav-link ml-3 tab"
            :class="{ active: isExperimentTab(ExperimentTab.FACTORS) }"
            :to="experimentTabPath(ExperimentTab.FACTORS)"> {{
            $t('component.menu.experimentalDesign.factors')
          }} <span v-if="!factorsCountIsLoading && factors > 0"
                   class="tabWithElements"> {{ opensilex.$numberFormatter.formateResponse(factors) }} </span>
        </router-link>
        <router-link
            class="nav-link ml-3 tab"
            :class="{ active: isExperimentTab(ExperimentTab.SCIENTIFIC_OBJECTS) }"
            :to="experimentTabPath(ExperimentTab.SCIENTIFIC_OBJECTS)">
          {{ $t('component.menu.scientificObjectTypes') }} <span
            v-if="!scientificObjectsCountIsLoading && scientificObjects > 0"
            class="tabWithElements"> {{ opensilex.$numberFormatter.formateResponse(scientificObjects) }} </span>
        </router-link>
        <router-link
            class="nav-link ml-3 tab"
            :class="{ active: isExperimentTab(ExperimentTab.DATA) }"
            :to="experimentTabPath(ExperimentTab.DATA)"> {{ $t('component.menu.data.label') }} <span
            v-if="!dataCountIsLoading && dataCount > 0"
            class="tabWithElements"> {{ opensilex.$numberFormatter.formateResponse(dataCount) }} </span>
        </router-link>
        <router-link
            class="nav-link ml-3 tab" :class="{ active: isExperimentTab(ExperimentTab.DATAFILES) }"
            :to="experimentTabPath(ExperimentTab.DATAFILES)"> {{ $t('component.menu.data.datafiles') }} <span
            v-if="!datafilesCountIsLoading && datafiles > 0"
            class="tabWithElements"> {{ opensilex.$numberFormatter.formateResponse(datafiles) }} </span>
        </router-link>
        <router-link
            class="nav-link ml-3 tab"
            :class="{ active: isExperimentTab(ExperimentTab.DATA_VISUALISATION) }"
            :to="experimentTabPath(ExperimentTab.DATA_VISUALISATION)">
          {{ $t('component.menu.data.visualization') }}
        </router-link>
        <router-link
            class="nav-link ml-3 tab"
            :class="{ active: isExperimentTab(ExperimentTab.MAP) }"
            :to="experimentTabPath(ExperimentTab.MAP)"> {{ $t('component.menu.spatial.map') }}
        </router-link>
        <router-link
            class="nav-link ml-3 tab"
            :class="{ active: isExperimentTab(ExperimentTab.ANNOTATIONS) }"
            :to="experimentTabPath(ExperimentTab.ANNOTATIONS)"> {{ $t('component.annotation.list-title') }}
          <span v-if="!annotationsCountIsLoading && annotations > 0"
                class="tabWithElements"> {{ opensilex.$numberFormatter.formateResponse(annotations) }} </span>
        </router-link>
        <router-link
            class="nav-link ml-3 tab"
            :class="{ active: isExperimentTab(ExperimentTab.DOCUMENT) }"
            :to="experimentTabPath(ExperimentTab.DOCUMENT)"> {{ $t('component.project.documents') }} <span
            v-if="!documentsCountIsLoading && documents > 0"
            class="tabWithElements"> {{ opensilex.$numberFormatter.formateResponse(documents) }} </span>
        </router-link>
      </template>
    </PageActions>

    <PageContent>
      <template v-slot>
        <ExperimentDetail
            v-if="isExperimentTab(ExperimentTab.DETAILS)"
            :uri="uri"
        />

        <ExperimentFactors
            v-else-if="isExperimentTab(ExperimentTab.FACTORS)"
            :uri="uri"
        />

        <ExperimentScientificObjects
            v-else-if="isExperimentTab(ExperimentTab.SCIENTIFIC_OBJECTS)"
            :uri="uri"
        />

        <ExperimentData
            v-else-if="isExperimentTab(ExperimentTab.DATA)"
            :uri="uri"
        />

        <ExperimentDataFiles
            v-else-if="isExperimentTab(ExperimentTab.DATAFILES)"
            :modificationCredentialId="credentials.CREDENTIAL_DATA_MODIFICATION_ID"
            :uri="uri"
        />

        <ExperimentDataVisualisation
            v-else-if="isExperimentTab(ExperimentTab.DATA_VISUALISATION)"
            :uri="uri"
            :elementName="name"
        />

        <MapView
            v-else-if="isExperimentTab(ExperimentTab.MAP)"
            :uri="uri"
        />

        <DocumentTabList
            v-else-if="isExperimentTab(ExperimentTab.DOCUMENT)"
            :modificationCredentialId="credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID"
            :uri="uri"
        />

        <AnnotationList
            v-else-if="isExperimentTab(ExperimentTab.ANNOTATIONS)"
            ref="annotationList"
            :target="uri"
            :displayTargetColumn="false"
            :enableActions="true"
            :modificationCredentialId="credentials.CREDENTIAL_ANNOTATION_MODIFICATION_ID"
            :deleteCredentialId="credentials.CREDENTIAL_ANNOTATION_DELETE_ID"
        />
      </template>
    </PageContent>
  </div>
</template>

<script setup lang="ts">
import {computed, ref, inject, onMounted, useTemplateRef} from 'vue';
import {useRoute} from 'vue-router';
import {useStore} from 'vuex';
import OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin';
import HttpResponse, {OpenSilexResponse} from 'opensilex-core/HttpResponse';
import AnnotationList from '@/components/annotations/list/AnnotationList.vue';
import {ExperimentsService} from 'opensilex-core';
import type {ExperimentGetDTO} from 'opensilex-core';
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

const credentials = computed(() => store.state.credentials);

enum ExperimentTab {
  DETAILS = 'details',
  FACTORS = 'factors',
  SCIENTIFIC_OBJECTS = 'scientific-objects',
  DATA = 'data',
  DATAFILES = 'datafiles',
  DATA_VISUALISATION = 'data-visualisation',
  MAP = 'map',
  ANNOTATIONS = 'annotations',
  DOCUMENT = 'document',
}

function isExperimentTab(tab: ExperimentTab): boolean {
  return route.path.startsWith(`/experiment/${tab}/`);
}

function experimentTabPath(tab: ExperimentTab): string {
  return `/experiment/${tab}/${encodeURIComponent(uri.value!)}`;
}

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


</script>


