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
        <!-- Each tab is the child route of its experiment route, see opensilex.front.yml -->
        <router-view v-slot="{ Component }">
          <component
              v-if="Component"
              :is="Component"
              v-bind="currentTabProps"
          />
        </router-view>
      </template>
    </PageContent>
  </div>
</template>

<script setup lang="ts">
import {computed, ref, inject, onMounted} from 'vue';
import {useRoute} from 'vue-router';
import {useStore} from 'vuex';
import OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin';
import HttpResponse, {OpenSilexResponse} from 'opensilex-core/HttpResponse';
import {ExperimentsService, ScientificObjectsService} from 'opensilex-core';
import type {ExperimentGetDTO} from 'opensilex-core';
import PageContent from "@/components/layout/PageContent.vue";
import PageActions from "@/components/layout/PageActions.vue";
import PageHeader from "@/components/layout/PageHeader.vue";

const route = useRoute();
const store = useStore();
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;

const service = opensilex.getService<ExperimentsService>('opensilex.ExperimentsService');
const scientificObjectsService = opensilex.getService<ScientificObjectsService>('opensilex.ScientificObjectsService');

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

/**
 * Props given to the component of the current tab, rendered by the router-view.
 */
const currentTabProps = computed(() => {
  switch (Object.values(ExperimentTab).find(isExperimentTab)) {
    case ExperimentTab.DETAILS:
    case ExperimentTab.FACTORS:
    case ExperimentTab.SCIENTIFIC_OBJECTS:
    case ExperimentTab.DATA:
    case ExperimentTab.MAP:
      return {uri: uri.value};
    case ExperimentTab.DATAFILES:
      return {
        uri: uri.value,
        modificationCredentialId: credentials.value.CREDENTIAL_DATA_MODIFICATION_ID
      };
    case ExperimentTab.DATA_VISUALISATION:
      return {uri: uri.value, elementName: name.value};
    case ExperimentTab.DOCUMENT:
      return {
        uri: uri.value,
        modificationCredentialId: credentials.value.CREDENTIAL_DOCUMENT_MODIFICATION_ID
      };
    case ExperimentTab.ANNOTATIONS:
      return {
        target: uri.value,
        displayTargetColumn: false,
        enableActions: true,
        modificationCredentialId: credentials.value.CREDENTIAL_ANNOTATION_MODIFICATION_ID,
        deleteCredentialId: credentials.value.CREDENTIAL_ANNOTATION_DELETE_ID
      };
    default:
      return {};
  }
})

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

    scientificObjectsService
        .countScientificObjects(uri.value)
        .then((http: HttpResponse<OpenSilexResponse<number>>) => {
          scientificObjects.value = http.response.result;
          scientificObjectsCountIsLoading.value = false;
        })
        .catch((error) => {
          opensilex.errorHandler(error);
        });
  }
});


</script>


