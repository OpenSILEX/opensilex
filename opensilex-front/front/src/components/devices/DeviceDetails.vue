<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-thermometer"
      title="DeviceDetails.title"
      :description="device.name"
      class="detail-element-header"
    ></opensilex-PageHeader>

    <opensilex-PageActions :tabs="true" :returnButton="true" >
      <template v-slot>
        <b-nav-item
          :active="isDetailsTab()"
          :to="{path: '/device/details/' + encodeURIComponent(uri)}"
        >{{ $t('DeviceDetails.details') }}</b-nav-item>

        <b-nav-item
          :active="isVisualizationTab()"
          :to="{ path: '/device/visualization/' + encodeURIComponent(uri) }"
        >{{ $t("DeviceDetails.visualization") }}</b-nav-item>

        <b-nav-item
          :active="isDatafilesTab()"
          :to="{ path: '/device/datafiles/' + encodeURIComponent(uri) }"
        >{{ $t("ScientificObjectDataFiles.datafiles") }}</b-nav-item>

        <b-nav-item
            :active="isEventTab()"
            :to="{ path: '/device/events/' + encodeURIComponent(uri) }"
        >{{ $t('Event.list-title') }}
        </b-nav-item>

        <b-nav-item
            :active="isPositionTab()"
            :to="{ path: '/device/positions/' + encodeURIComponent(uri) }"
        >{{ $t('Position.list-title') }}
        </b-nav-item>
        
        <b-nav-item
          :active="isAnnotationTab()"
          :to="{ path: '/device/annotations/' + encodeURIComponent(uri) }"
        >{{ $t("DeviceDetails.annotation") }}</b-nav-item>

        <b-nav-item
          :active="isDocumentTab()"
          :to="{ path: '/device/documents/' + encodeURIComponent(uri) }"
        >{{ $t('DeviceDetails.documents') }}</b-nav-item>

        </template>
    </opensilex-PageActions>

    <opensilex-PageContent>
        <template v-slot>
          <opensilex-DeviceDescription v-if="isDetailsTab()" :uri="uri"></opensilex-DeviceDescription>

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
    import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
    // @ts-ignore
    import {DeviceGetDTO, DevicesService} from "opensilex-core/index";
    import AnnotationList from "../annotations/list/AnnotationList.vue";

    @Component
    export default class DeviceDetails extends Vue {
        $opensilex: any;
        $route: any;

        service: DevicesService;
        uri = null;
        name: string = "";

        @Ref("annotationList") readonly annotationList!: AnnotationList;

        get user() {
            return this.$store.state.user;
        }

        get credentials() {
            return this.$store.state.credentials;
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
          this.service = this.$opensilex.getService("opensilex.DevicesService");
          this.uri = decodeURIComponent(this.$route.params.uri);
          this.loadDevice(this.uri);
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
    title: Dispositif
    description: Description
    details: Details
    annotations: Annotations
    documents: Documents
    visualization: Visualisation

</i18n>
