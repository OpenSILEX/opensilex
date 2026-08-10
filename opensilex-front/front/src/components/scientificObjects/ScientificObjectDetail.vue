<template>
  <div v-if="selected && selected.uri" :class="{'ok embed-tabs': !globalView}">
    <opensilex-PageActions :returnButton="withReturnButton">
      <nav class="tabs mb-3">
        <router-link
          v-for="tab in tabs"
          :key="tab.key"
          :to="tab.to"
          :replace="true"
          class="tab"
          exact-active-class="active"
        >
          {{ tab.label }}
        </router-link>
      </nav>




<!--      <b-nav-item
        :active="isDetailsTab"
        @click.prevent="onTabChanged(ScientificObjectDetail.DETAILS_TAB)"
      >{{ $t("component.common.details-label") }}
      </b-nav-item>

      <b-nav-item
        v-if="includeTab(ScientificObjectDetail.VISUALIZATION_TAB)"
        @click.prevent="onTabChanged(ScientificObjectDetail.VISUALIZATION_TAB)"
        :active="isVisualizationTab"
      >{{ $t("ScientificObjectVisualizationTab.visualization") }}
      </b-nav-item>

      <b-nav-item
        v-if="includeTab(ScientificObjectDetail.DATAFILES_TAB)"
        :active="isDatafilesTab"
        @click.prevent="onTabChanged(ScientificObjectDetail.DATAFILES_TAB)"
      >{{ $t("ScientificObjectDataFiles.datafiles") }}
        <span
          v-if="!datafilesCountIsLoading && datafileQuantity > 0"
          class="tabWithElements"
        >
          {{$opensilex.$numberFormatter.formateResponse(datafileQuantity)}}
        </span>
      </b-nav-item>

      <b-nav-item
        v-if="includeTab(ScientificObjectDetail.EVENTS_TAB)"
        :active="isEventTab"
        @click.prevent="onTabChanged(ScientificObjectDetail.EVENTS_TAB)"
      >{{ $t("Event.list-title") }}
        <span
          v-if="!eventsCountIsLoading && eventQuantity > 0"
          class="tabWithElements"
        >
          {{$opensilex.$numberFormatter.formateResponse(eventQuantity)}}
        </span>
      </b-nav-item>

      <b-nav-item
        v-if="includeTab(ScientificObjectDetail.POSITIONS_TAB)"
        :active="isPositionTab"
        @click.prevent="onTabChanged(ScientificObjectDetail.POSITIONS_TAB)"
      >{{ $t("Position.list-title") }}
        <span
          v-if="!positionsCountIsLoading && positionQuantity > 0"
          class="tabWithElements"
        >
          {{$opensilex.$numberFormatter.formateResponse(positionQuantity)}}
        </span>
      </b-nav-item>

      <b-nav-item
        v-if="includeTab(ScientificObjectDetail.ANNOTATIONS_TAB)"
        :active="isAnnotationTab"
        @click.prevent="onTabChanged(ScientificObjectDetail.ANNOTATIONS_TAB)"
      >{{ $t("Annotation.list-title") }}
        <span
          v-if="!annotationsCountIsLoading && annotationQuantity > 0"
          class="tabWithElements"
        >
          {{$opensilex.$numberFormatter.formateResponse(annotationQuantity)}}
        </span>
      </b-nav-item>

      <b-nav-item
        v-if="includeTab(ScientificObjectDetail.DOCUMENTS_TAB)"
        :active="isDocumentTab"
        @click.prevent="onTabChanged(ScientificObjectDetail.DOCUMENTS_TAB)"
      >{{ $t("DocumentTabList.documents") }}
        <span
          v-if="!documentsCountIsLoading && documentQuantity > 0"
          class="tabWithElements"
        >
          {{$opensilex.$numberFormatter.formateResponse(documentQuantity)}}
        </span>
      </b-nav-item>-->
    </opensilex-PageActions>


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


<!--
    <div v-if="isDetailsTab">
      <opensilex-ScientificObjectDetailProperties
        :globalView="globalView"
        :selected="selected"
        :objectByContext="objectByContext"
        :experiment="experiment"
        @onUpdate="$emit('onUpdate', $event)"
      ></opensilex-ScientificObjectDetailProperties>
    </div>

    <opensilex-ScientificObjectDataFiles
      v-if="isDatafilesTab"
      :uri="selected.uri"
      @redirectToDetail="onTabChanged(ScientificObjectDetail.DETAILS_TAB)"
    ></opensilex-ScientificObjectDataFiles>

    <div v-if="isAnnotationTab">
      <opensilex-AnnotationList
        ref="annotationList"
        :deleteCredentialId="credentials.CREDENTIAL_EXPERIMENT_DELETE_ID"
        :enableActions="true"
        :modificationCredentialId="
          credentials.CREDENTIAL_ANNOTATION_MODIFICATION_ID
        "
        :target="selected.uri"
      ></opensilex-AnnotationList>
    </div>

    <opensilex-ScientificObjectVisualizationTab
      v-if="isVisualizationTab"
      :scientificObject="selected"
      :elementName="selected.name"
    ></opensilex-ScientificObjectVisualizationTab>

    <opensilex-DocumentTabList
      v-if="isDocumentTab"
      :modificationCredentialId="credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID"
      :uri="selected.uri"
    ></opensilex-DocumentTabList>

    <opensilex-EventList
      v-if="isEventTab"
      ref="eventList"
      :target="selected.uri"
      :displayTargetFilter="false"
      :columnsToDisplay="eventColumnsToDisplay"
      :maximizeFilterSize="true"
      :modificationCredentialId="credentials.CREDENTIAL_EVENT_MODIFICATION_ID"
      :deleteCredentialId="credentials.CREDENTIAL_EVENT_DELETE_ID"
      :context="{experimentURI: uri}"
    ></opensilex-EventList>

    <opensilex-PositionList
      v-if="isPositionTab"
      ref="positionList"
      :target="selected.uri"
      :columnsToDisplay="positionColumnsToDisplay"
      :modificationCredentialId="credentials.CREDENTIAL_EVENT_MODIFICATION_ID"
      :deleteCredentialId="credentials.CREDENTIAL_EVENT_DELETE_ID"
    ></opensilex-PositionList>-->
  </div>
</template>

<script setup lang="ts">
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import AnnotationList from "../annotations/list/AnnotationList.vue";
import {ScientificObjectsService} from "opensilex-core/index";
import PositionList from "../positions/list/PositionList.vue";
import EventList from "../events/list/EventList.vue";

import {EventsService} from "opensilex-core/api/events.service";
import {AnnotationsService} from "opensilex-core/api/annotations.service";
import {DocumentsService} from "opensilex-core/api/documents.service";
import {PositionsService} from "opensilex-core/api/positions.service";
import {DataService} from "opensilex-core/api/data.service";
import {computed, inject, onMounted, ref, useTemplateRef} from "vue";
import {RouteLocationRaw, useRoute} from "vue-router";
import {ScientificObjectDetailByExperimentsDTO} from "opensilex-core/model/scientificObjectDetailByExperimentsDTO";
import ScientificObjectDetail from "@/components/scientificObjects/ScientificObjectDetail.vue";
import {useStore} from "vuex";
import {useI18n} from "vue-i18n";

export interface Tab {
  key: string;
  label: string;
  to: RouteLocationRaw;
}

//#region Props
//TODO MAX at the time of writing this i guessed type for Experilment, not sure if its a string or some dto
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
      //to: `/scientific-objects/visualization/${encodeURIComponent(uri.value)}`
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
      //to: `/scientific-objects/documents/${encodeURIComponent(uri.value)}`
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
      //to: `/scientific-objects/annotations/${encodeURIComponent(uri.value)}`
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
      //to: `/scientific-objects/events/${encodeURIComponent(uri.value)}`
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
      //to: `/scientific-objects/positions/${encodeURIComponent(uri.value)}`
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
      //to: `/scientific-objects/datafiles/${encodeURIComponent(uri.value)}`
      to: {
        name: 'ScientificObjectDatafiles',
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
//TODO MAX in the original version these next 3 were props, think that was just an error in vue2 version
const annotationQuantity = ref<number>(null);
const documentQuantity = ref<number>(null);
const eventQuantity = ref<number>(null);

//booleans
const eventsCountIsLoading = ref<boolean>(true);
const annotationsCountIsLoading = ref<boolean>(true);
const documentsCountIsLoading = ref<boolean>(true);
const positionsCountIsLoading = ref<boolean>(true);
const datafilesCountIsLoading = ref<boolean>(true);

//TODO MAX its unclear if and what this does, it was just used once in the old create method
//routeArr : string = this.$route.path.split('/');

//#endregion

//#region Template refs
const annotationList = useTemplateRef<InstanceType<typeof AnnotationList>>("annotationList");
const eventList = useTemplateRef<InstanceType<typeof EventList>>("eventList");
const positionList = useTemplateRef<InstanceType<typeof PositionList>>("positionList");
//#endregion

//#region Event Handlers
function onUpdate(uri: string){
  emit('onUpdate', uri);
}
//#endregion

//#region Computed
const user = computed(() => {
  return $store.state.user;
});

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
  //TODO MAX i dont understand what these next to lines were for, delete at end if no problems (ainsi que the routeArr definition up above)
  //localStorage.setItem("tabPath", this.routeArr[2]);
  //localStorage.setItem("tabPage", "1");
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