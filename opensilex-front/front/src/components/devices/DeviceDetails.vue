<template>
    <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-thermometer"
      title="DeviceDetails.title"
      :description="device.name"
    ></opensilex-PageHeader>

    <opensilex-PageActions :tabs="true" :returnButton="true" >       
      <template v-slot>
        <b-nav-item
          :active="isDetailsTab()"
          :to="{path: '/device/details/' + encodeURIComponent(uri)}"
        >{{ $t('DeviceDetails.details') }}
        </b-nav-item>

        <b-nav-item
          :active="isAnnotationTab()"
          :to="{ path: '/device/annotations/' + encodeURIComponent(uri) }"
        >{{ $t("DeviceDetails.annotation") }}
        </b-nav-item>

        <b-nav-item
          :active="isDocumentTab()"
          :to="{ path: '/device/documents/' + encodeURIComponent(uri) }"
        >{{ $t('DeviceDetails.documents') }}</b-nav-item> 

        </template>
    </opensilex-PageActions>

    <opensilex-PageContent>
        <template v-slot>
          <opensilex-DeviceDescription v-if="isDetailsTab()" :uri="uri"></opensilex-DeviceDescription>
          <opensilex-DocumentTabList
            v-else-if="isDocumentTab()"
            :uri="uri"
            :modificationCredentialId="credentials.CREDENTIAL_DEVICE_MODIFICATION_ID"
            :deleteCredentialId="credentials.CREDENTIAL_DEVICE_DELETE_ID"
          ></opensilex-DocumentTabList>

          <opensilex-AnnotationList
            v-else-if="isAnnotationTab()"
            ref="annotationList"
            :target="uri"
            :displayTargetColumn="false"
            :enableActions="true"
            :modificationCredentialId="credentials.CREDENTIAL_DEVICE_MODIFICATION_ID"
            :deleteCredentialId="credentials.CREDENTIAL_DEVICE_DELETE_ID"
            @onEdit="annotationModalForm.showEditForm($event)"
            ></opensilex-AnnotationList>
        </template>
    </opensilex-PageContent>
    </div>
</template>

<script lang="ts">
    import VueRouter from "vue-router";
    import {Component, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
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

        isDocumentTab() {
            return this.$route.path.startsWith("/device/documents/");
        }

        isAnnotationTab() {
            return this.$route.path.startsWith("/device/annotations/");
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

fr:
  DeviceDetails:
    title: Dispositif
    description: Description
    details: Details
    annotations: Annotations
    documents: Documents

</i18n>
