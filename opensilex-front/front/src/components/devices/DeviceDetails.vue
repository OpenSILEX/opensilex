<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-thermometer"
      :title="device.name"
      description="DeviceDetails.title"
      class="detail-element-header"
    ></opensilex-PageHeader>

    <opensilex-PageActions :tabs="true" :returnButton="true">
      <template v-slot>
        <b-nav-item
          :active="isDetailsTab()"
          :to="{path: '/device/details/' + encodeURIComponent(uri)}"
        >{{ $t('DeviceDetails.description') }}</b-nav-item>

        <b-nav-item
          :active="isVisualizationTab()"
          :to="{ path: '/device/visualization/' + encodeURIComponent(uri) }"
        >{{ $t("DeviceDetails.visualization") }}</b-nav-item>

        <b-nav-item
          :active="isDatafilesTab()"
          :to="{ path: '/device/datafiles/' + encodeURIComponent(uri) }"
        >{{ $t("ScientificObjectDataFiles.datafiles") }}
          <span
            v-if="!datafilesCountIsLoading && datafiles > 0"
            class ="tabWithElements"
          >
            {{$opensilex.$numberFormatter.formateResponse(datafiles)}}
          </span>
        </b-nav-item>

        <b-nav-item
            :active="isEventTab()"
            :to="{ path: '/device/events/' + encodeURIComponent(uri) }"
        >{{ $t('Event.list-title') }}
          <span
            v-if="!eventsCountIsLoading && events > 0"
            class ="tabWithElements"
          >
            {{$opensilex.$numberFormatter.formateResponse(events)}}
          </span>
        </b-nav-item>

        <b-nav-item
            :active="isPositionTab()"
            :to="{ path: '/device/positions/' + encodeURIComponent(uri) }"
        >{{ $t('Position.list-title') }}
        <span
          v-if="!positionsCountIsLoading && positions > 0"
          class ="tabWithElements"
        >
          {{$opensilex.$numberFormatter.formateResponse(positions)}}
        </span>
        </b-nav-item>
        
        <b-nav-item
          :active="isAnnotationTab()"
          :to="{ path: '/device/annotations/' + encodeURIComponent(uri) }"
        >{{ $t("DeviceDetails.annotation") }}
          <span
            v-if="!annotationsCountIsLoading && annotations > 0"
            class ="tabWithElements"
          >
            {{$opensilex.$numberFormatter.formateResponse(annotations)}}
          </span>
        </b-nav-item>

        <b-nav-item
          :active="isDocumentTab()"
          :to="{ path: '/device/documents/' + encodeURIComponent(uri) }"
        >{{ $t('DeviceDetails.documents') }}
          <span
            v-if="!documentsCountIsLoading && documents > 0"
            class ="tabWithElements"
          >
            {{$opensilex.$numberFormatter.formateResponse(documents)}}
          </span>
        </b-nav-item>

        </template>
    </opensilex-PageActions>

    <opensilex-PageContent>
        <template v-slot>
          <opensilex-DeviceDescription
              v-if="isDetailsTab()"
              :key="lang">
          </opensilex-DeviceDescription>

         <opensilex-DeviceVisualizationTab
          v-else-if="isVisualizationTab()"
          :device="uri"
          ></opensilex-DeviceVisualizationTab>

         <opensilex-DeviceDataFiles
          v-else-if="isDatafilesTab()"
          :device="uri"
        ></opensilex-DeviceDataFiles>
        
          <opensilex-DocumentTabList
            v-else-if="isDocumentTab()"
            :uri="uri"
            :modificationCredentialId="credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID"
          ></opensilex-DocumentTabList>

          <opensilex-AnnotationList
            v-else-if="isAnnotationTab()"
            ref="annotationList"
            :target="uri"
            :displayTargetColumn="false"
            :enableActions="true"
            :modificationCredentialId="credentials.CREDENTIAL_ANNOTATION_MODIFICATION_ID"
            :deleteCredentialId="credentials.CREDENTIAL_ANNOTATION_DELETE_ID"
            ></opensilex-AnnotationList>

          <opensilex-EventList
              v-if="isEventTab()"
              ref="eventList"
              :target="uri"
              :columnsToDisplay="new Set(['type','start','end','description'])"
              :modificationCredentialId="credentials.CREDENTIAL_EVENT_MODIFICATION_ID"
              :deleteCredentialId="credentials.CREDENTIAL_EVENT_DELETE_ID"
              :displayTargetFilter="false"
          ></opensilex-EventList>

          <opensilex-PositionList
              v-if="isPositionTab()"
              ref="positionList"
              :target="uri"
              :modificationCredentialId="credentials.CREDENTIAL_EVENT_MODIFICATION_ID"
              :deleteCredentialId="credentials.CREDENTIAL_EVENT_DELETE_ID"
          ></opensilex-PositionList>

        </template>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
    import VueRouter from "vue-router";
    import {Component, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
    import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
    // @ts-ignore
    import {DeviceGetDTO, DevicesService} from "opensilex-core/index";
    import AnnotationList from "../annotations/list/AnnotationList.vue";

    import {EventsService} from "opensilex-core/api/events.service";
    import {AnnotationsService} from "opensilex-core/api/annotations.service";
    import {DocumentsService} from "opensilex-core/api/documents.service";
    import {PositionsService} from "opensilex-core/api/positions.service";
    import {DataService} from "opensilex-core/api/data.service";

    @Component
    export default class DeviceDetails extends Vue {
        $opensilex: OpenSilexVuePlugin;
        $route: any;

        service: DevicesService;
        $EventsService: EventsService
        $AnnotationsService: AnnotationsService
        $DocumentsService: DocumentsService
        $PositionsService: PositionsService
        $DataService: DataService

        events: number;
        annotations: number;
        documents: number;
        positions: number;
        datafiles: number;

        eventsCountIsLoading: boolean = true;
        annotationsCountIsLoading: boolean = true;
        documentsCountIsLoading: boolean = true;
        positionsCountIsLoading: boolean = true;
        datafilesCountIsLoading: boolean = true;

        uri = null;
        name: string = "";

        @Ref("annotationList") readonly annotationList!: AnnotationList;

        get user() {
            return this.$store.state.user;
        }

        get credentials() {
            return this.$store.state.credentials;
        }

        get lang() {
            return this.$store.getters.language;
        }

        device: DeviceGetDTO = {
              uri: null,
              rdf_type: null,
              name: null,
              brand: null,
              constructor_model: null,
              serial_number: null,
              person_in_charge: null,
              start_up: null,
              removal: null,
              relations: null,
              description: null
        };

        created() {
          this.service = this.$opensilex.getService<DevicesService>("opensilex.DevicesService");
          this.$EventsService = this.$opensilex.getService<EventsService>("opensilex.EventsService");
          this.$AnnotationsService = this.$opensilex.getService<AnnotationsService>("opensilex.AnnotationsService");
          this.$DocumentsService = this.$opensilex.getService<DocumentsService>("opensilex.DocumentsService");
          this.$PositionsService = this.$opensilex.getService<PositionsService>("opensilex.PositionsService");
          this.$DataService = this.$opensilex.getService<DataService>("opensilex.DataService");
          this.uri = decodeURIComponent(this.$route.params.uri);
          this.loadDevice(this.uri);
          this.searchEvents();
          this.searchAnnotations();
          this.searchDocuments();
          this.searchPositions();
          this.searchDatafiles(this.uri);
        }

        loadDevice(uri: string) {
          this.service
            .getDevice(uri)
            .then((http: HttpResponse<OpenSilexResponse<DeviceGetDTO>>) => {
              this.device = http.response.result;
            })
            .catch(this.$opensilex.errorHandler);
        }

        isDetailsTab() {
            return this.$route.path.startsWith("/device/details/");
        }

        isVisualizationTab() {
            return this.$route.path.startsWith("/device/visualization/");
        }

        isDatafilesTab() {
          return this.$route.path.startsWith("/device/datafiles/");
        }
      
        isDocumentTab() {
            return this.$route.path.startsWith("/device/documents/");
        }

        isAnnotationTab() {
            return this.$route.path.startsWith("/device/annotations/");
        }

        isEventTab(): boolean {
            return this.$route.path.startsWith("/device/events/");
        }

        isPositionTab(): boolean {
              return this.$route.path.startsWith("/device/positions/");
          }

      searchEvents() {
        return this.$EventsService
          .countEvents(
            [this.uri],
            undefined,
            undefined
          ).then((http: HttpResponse<OpenSilexResponse<number>>) => {
            if (http && http.response){
              this.events = http.response.result as number;
              this.eventsCountIsLoading = false;
              return this.events
            }
          }).catch(this.$opensilex.errorHandler);
      }

    searchAnnotations() {
      return this.$AnnotationsService
        .countAnnotations(
          this.uri,
          undefined,
          undefined
        ).then((http: HttpResponse<OpenSilexResponse<number>>) => {
          if (http && http.response){
            this.annotations = http.response.result as number;
            this.annotationsCountIsLoading = false;
            return this.annotations
          }
        }
      ).catch(this.$opensilex.errorHandler);
    }


  searchDocuments(){
    return this.$DocumentsService
      .countDocuments(
        this.uri,
        undefined,
        undefined
      ).then((http: HttpResponse<OpenSilexResponse<number>>) => {
        if (http && http.response){
          this.documents = http.response.result as number;
          this.documentsCountIsLoading = false;
          return this.documents
        }
      }).catch(this.$opensilex.errorHandler);
  }
  
  searchPositions(){
    return this.$PositionsService
      .countMoves(
        this.uri,
        undefined,
        undefined
      ).then((http: HttpResponse<OpenSilexResponse<number>>) => {
        if (http && http.response){
          this.positions = http.response.result as number;
          this.positionsCountIsLoading = false;
          return this.positions
        }
      }).catch(this.$opensilex.errorHandler);
  }

  searchDatafiles(uri){
    return this.$DataService
    .countDatafiles(
      undefined,
      [uri]
    ).then((http: HttpResponse<OpenSilexResponse<number>>) => {
      if (http && http.response){
        this.datafiles = http.response.result as number;
        this.datafilesCountIsLoading = false;
        return this.datafiles
      }
    }).catch(this.$opensilex.errorHandler);
  }
}
</script>

<style lang="scss">
</style>

<i18n>
en:
  DeviceDetails:
    title: Device
    description: Description
    details: Details
    annotation: Annotation
    documents: Documents
    visualization: Visualization

fr:
  DeviceDetails:
    title: Appareil
    description: Description
    details: Details
    annotations: Annotations
    documents: Documents
    visualization: Visualisation

</i18n>
