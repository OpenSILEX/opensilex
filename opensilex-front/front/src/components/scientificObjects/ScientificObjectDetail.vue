<template>
  <div v-if="selected && selected.uri" :class="{'ok embed-tabs': !globalView}">
    <PageActions :returnButton="withReturnButton">
      <nav class="tabs mb-3">
        <router-link
          v-for="tab in tabs"
          :key="tab.key"
          :to="tab.to"
          :replace="true"
          class="tab"
          exact-active-class="active"
        >
          <!-- label -->
          {{ tab.label }}

          <!-- Count for the tabs that have that information -->
          <span v-if="tab.key === 'datafiles' && !datafilesCountIsLoading && datafileQuantity > 0" class="tabWithElements">
            {{ $opensilex.$numberFormatter.formateResponse(datafileQuantity) }}
          </span>

          <span v-if="tab.key === 'events' && !eventsCountIsLoading && eventQuantity > 0" class="tabWithElements">
            {{ $opensilex.$numberFormatter.formateResponse(eventQuantity) }}
          </span>

          <span v-if="tab.key === 'positions' && !positionsCountIsLoading && positionQuantity > 0" class="tabWithElements">
            {{ $opensilex.$numberFormatter.formateResponse(positionQuantity) }}
          </span>

          <span v-if="tab.key === 'annotations' && !annotationsCountIsLoading && annotationQuantity > 0" class="tabWithElements">
            {{ $opensilex.$numberFormatter.formateResponse(annotationQuantity) }}
          </span>

          <span v-if="tab.key === 'documents' && !documentsCountIsLoading && documentQuantity > 0" class="tabWithElements">
            {{ $opensilex.$numberFormatter.formateResponse(documentQuantity) }}
          </span>

        </router-link>
      </nav>

    </PageActions>

    <!--This is the content produced by each tab, display handled by router-view,
    To see which components get built per path, look in opensilex.front.yml. To pass different non-path related props
    we use the Component slot.
    -->
    <router-view v-slot="{ Component }">
      <component
        :is="Component"
        :selected="selected"
        :globalView="globalView"
        :objectByContext="objectByContext"
        :experiment="experiment"
        :uri="selected.uri"
        :eventColumnsToDisplay="eventColumnsToDisplay"
        :positionColumnsToDisplay="positionColumnsToDisplay"
        :deleteCredentialId="deleteCredentialId"
        :enableActions="true"
        :modificationCredentialId="modificationCredentialId"
        :target="selected.uri"
        :scientificObject="selected"
        :elementName="selected.name"
        :displayTargetFilter="false"
        :columnsToDisplay="columnsToDisplay"
        :maximizeFilterSize="true"
        :context="{experimentURI: uri}"
        @onUpdate="onUpdate"
        @redirectToDetail="emit('forceTabChange', 'ScientificObjectDetailProperties')"
      />
    </router-view>

  </div>
</template>

<script setup lang="ts">
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {ScientificObjectsService} from "opensilex-core/index";
import {EventsService} from "opensilex-core/api/events.service";
import {AnnotationsService} from "opensilex-core/api/annotations.service";
import {DocumentsService} from "opensilex-core/api/documents.service";
import {PositionsService} from "opensilex-core/api/positions.service";
import {DataService} from "opensilex-core/api/data.service";
import {computed, inject, onMounted, ref} from "vue";
import {RouteLocationRaw, useRoute} from "vue-router";
import {ScientificObjectDetailByExperimentsDTO} from "opensilex-core/model/scientificObjectDetailByExperimentsDTO";
import {useStore} from "vuex";
import {useI18n} from "vue-i18n";
import {SCIENTIFIC_OBJECT_DATAFILES_PATHNAME} from "@/components/scientificObjects/ScientificObjectUtils";

export interface Tab {
  key: string;
  label: string;
  to: RouteLocationRaw;
}

//#region Props
interface Props{
  selected: ScientificObjectDetailByExperimentsDTO,
  objectByContext: ScientificObjectDetailByExperimentsDTO[],
  withReturnButton: boolean,
  globalView: boolean,
  experiment?: string,
  tabs?: (uri: string, experiment?: string) => Tab[]
}

const props = withDefaults(
  defineProps<Props>(),
  {
    objectByContext: (() => []),
    withReturnButton: false,
    experiment: null
  }
)
//#endregion

//#region Emit Definitions
const emit = defineEmits<{
  (eventName: 'forceTabChange', routeName: string): void;
  (eventName: 'onUpdate', uri: string): void;
}>();

//#endregion

//#region Constants
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const service : ScientificObjectsService = $opensilex.getService("opensilex.ScientificObjectsService");
const $EventsService: EventsService = $opensilex.getService("opensilex.EventsService");
const $AnnotationsService: AnnotationsService = $opensilex.getService("opensilex.AnnotationsService");
const $DocumentsService: DocumentsService = $opensilex.getService("opensilex.DocumentsService");
const $PositionsService: PositionsService = $opensilex.getService("opensilex.PositionsService");
const $DataService: DataService = $opensilex.getService("opensilex.DataService");
const $route = useRoute();
const $store = useStore();
const { t } = useI18n();
const eventColumnsToDisplay : Set<string> = new Set(['type', 'start', 'end', 'description']);
const positionColumnsToDisplay : Set<string> = new Set(['end']);

const tabs = computed<Tab[]>(() => {
  if (!uri.value) {
    return []
  }

  if(props.tabs){
    return props.tabs(uri.value, props.experiment);
  }

  return [
    {
      key: 'details',
      label: t('component.common.details-label'),
      to: {
        name: 'ScientificObjectDetailProperties',
        params: {
          uri: uri.value,
          experiment: props.experiment
        }
      }
    },
    {
      key: 'visualization',
      label: t('ScientificObjectVisualizationTab.visualization'),
      to: {
        name: 'ScientificObjectVisualization',
        params: {
          uri: uri.value,
          experiment: props.experiment
        }
      }
    },
    {
      key: 'documents',
      label: t('DocumentTabList.documents'),
      to: {
        name: 'ScientificObjectDocuments',
        params: {
          uri: uri.value,
          experiment: props.experiment
        }
      }
    },
    {
      key: 'annotations',
      label: t('Annotation.list-title'),
      to: {
        name: 'ScientificObjectAnnotations',
        params: {
          uri: uri.value,
          experiment: props.experiment
        }
      }
    },
    {
      key: 'events',
      label: t('Event.list-title'),
      to: {
        name: 'ScientificObjectEvents',
        params: {
          uri: uri.value,
          experiment: props.experiment
        }
      }
    },
    {
      key: 'positions',
      label: t('component.common.geometry.positions'),
      to: {
        name: 'ScientificObjectPositions',
        params: {
          uri: uri.value,
          experiment: props.experiment
        }
      }
    },
    {
      key: 'datafiles',
      label: t('ScientificObjectDataFiles.datafiles'),
      to: {
        name: SCIENTIFIC_OBJECT_DATAFILES_PATHNAME,
        params: {
          uri: uri.value,
          experiment: props.experiment
        }
      }
    },
  ]
});
//#endregion

//#region Reactive properties
const uri = ref<string>(null);
const positionQuantity = ref<number>(null);
const datafileQuantity = ref<number>(null);
const annotationQuantity = ref<number>(null);
const documentQuantity = ref<number>(null);
const eventQuantity = ref<number>(null);

//booleans
const eventsCountIsLoading = ref<boolean>(true);
const annotationsCountIsLoading = ref<boolean>(true);
const documentsCountIsLoading = ref<boolean>(true);
const positionsCountIsLoading = ref<boolean>(true);
const datafilesCountIsLoading = ref<boolean>(true);
//#endregion

//#region Event Handlers
function onUpdate(uri: string){
  emit('onUpdate', uri);
}
//#endregion

//#region Computed
const credentials = computed(() => {
  return $store.state.credentials;
});

const modificationCredentialId = computed(() => {
  switch ($route.name) {
    case "ScientificObjectAnnotations":
      return credentials.value.CREDENTIAL_ANNOTATION_MODIFICATION_ID
    case "ScientificObjectDocuments":
      return credentials.value.CREDENTIAL_DOCUMENT_MODIFICATION_ID;
    case "ScientificObjectEvents":
      return credentials.value.CREDENTIAL_EVENT_MODIFICATION_ID;
    case "ScientificObjectPositions":
      return credentials.value.CREDENTIAL_EVENT_MODIFICATION_ID;
    default:
      return undefined;
  }
});

const deleteCredentialId = computed(() => {
  switch ($route.name) {
    case "ScientificObjectAnnotations":
      return credentials.value.CREDENTIAL_ANNOTATION_DELETE_ID
    case "ScientificObjectEvents":
      return credentials.value.CREDENTIAL_EVENT_DELETE_ID;
    case "ScientificObjectPositions":
      return credentials.value.CREDENTIAL_EVENT_DELETE_ID;
    default:
      return undefined;
  }
});

const columnsToDisplay = computed(() => {
  switch ($route.name) {
    case "ScientificObjectEvents":
      return eventColumnsToDisplay;
    case "ScientificObjectPositions":
      return positionColumnsToDisplay;
    default:
      return undefined;
  }
});

//#endregion

//#region Hooks
onMounted(() => {
  // at start default tab is detail tab
  uri.value = decodeURIComponent($route.params.uri as string);
  searchEvents();
  searchAnnotations();
  searchDocuments();
  searchPositions();
  searchDatafiles(uri.value);
})

//#endregion

//#region Functions
function searchEvents() {
  return $EventsService
    .countEvents(
      [props.selected.uri],
      undefined,
      undefined
    ).then((http: HttpResponse<OpenSilexResponse<number>>) => {
      if(http && http.response){
        eventQuantity.value = http.response.result as number;
        eventsCountIsLoading.value = false;
        return eventQuantity.value;
      }
    }).catch($opensilex.errorHandler);
}

function searchAnnotations() {
  return $AnnotationsService
    .countAnnotations(
      props.selected.uri,
      undefined,
      undefined
    ).then((http: HttpResponse<OpenSilexResponse<number>>) => {
        if(http && http.response){
          annotationQuantity.value = http.response.result as number;
          annotationsCountIsLoading.value = false;
          return annotationQuantity.value;
        }
      }
    ).catch($opensilex.errorHandler);
}

function searchDocuments(){
  return $DocumentsService
    .countDocuments(
      props.selected.uri,
      undefined,
      undefined
    ).then((http: HttpResponse<OpenSilexResponse<number>>) => {
      if(http && http.response){
        documentQuantity.value = http.response.result as number;
        documentsCountIsLoading.value = false;
        return documentQuantity.value;
      }
    }).catch($opensilex.errorHandler);
}

function searchPositions(){
  return $PositionsService
    .countMoves(
      props.selected.uri,
      undefined,
      undefined
    ).then((http: HttpResponse<OpenSilexResponse<number>>) => {
      if(http && http.response){
        positionQuantity.value = http.response.result as number;
        positionsCountIsLoading.value = false;
        return positionQuantity.value;
      }
    }).catch($opensilex.errorHandler);
}

function searchDatafiles(uri){
  return $DataService
    .countDatafiles(
      [uri],
      undefined
    ).then((http: HttpResponse<OpenSilexResponse<number>>) => {
      if(http && http.response){
        datafileQuantity.value = http.response.result as number;
        datafilesCountIsLoading.value = false;
        return datafileQuantity.value;
      }
    }).catch($opensilex.errorHandler);
}
//#endregion

</script>

<style lang="scss">

.tab {
  text-decoration: none;
  color: var(--color-dark);
}

.withReturnButton {
  margin-left: 65px;
}

.back-button {
  float: left;
}

.embed-tabs > .card {
  margin-bottom: 0!important;
}

.embed-tabs .row .card:first-child  > .card-header {
  display: none
}

.embed-tabs .row .card:first-child  {
  border-top: none;
  margin-top: -5px;
}
</style>


<i18n>
en:
  ScientificObjectDetail:
    title: Detail
    generalInformation: Global information

fr:
  ScientificObjectDetail:
    title: Détail
    generalInformation: Informations globales

</i18n>