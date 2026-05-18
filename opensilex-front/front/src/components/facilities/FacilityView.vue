<template>
  <div v-if="uri" class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-globe"
      :title="name"
      description="component.facility.view.title"
      class= "detail-element-header"
    ></opensilex-PageHeader>

    <opensilex-PageActions :tabs="true" :returnButton="true" class="navigationTabs">

      <nav class="tabs mb-3">
        <router-link
          v-for="tab in tabs"
          :key="tab.key"
          :to="tab.to"
          class="tab"
          active-class="active"
        >
          {{ tab.label }}
        </router-link>
      </nav>
    </opensilex-PageActions>

    <opensilex-PageContent v-slot>

        <opensilex-FacilityDetails
          v-if="currentTab === 'details'"
          :uri="uri"
        ></opensilex-FacilityDetails>

        <opensilex-FacilityMonitoringView
          v-if="currentTab === 'overview'"
          :uri="uri"
        ></opensilex-FacilityMonitoringView>

        <opensilex-LocationList
          v-if="currentTab === 'positions'"
          :target="uri"
        ></opensilex-LocationList>

        <opensilex-DocumentTabList
          v-else-if="currentTab === 'document'"
          :modificationCredentialId="credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID"
          :uri="uri"
        ></opensilex-DocumentTabList>

        <opensilex-AnnotationList
          v-else-if="currentTab === 'annotations'"
          ref="annotationList"
          :target="uri"
          :displayTargetColumn="false"
          :enableActions="true"
          :modificationCredentialId="credentials.CREDENTIAL_ANNOTATION_MODIFICATION_ID"
          :deleteCredentialId="credentials.CREDENTIAL_ANNOTATION_DELETE_ID"
        ></opensilex-AnnotationList>

    </opensilex-PageContent>
  </div>
</template>

<script setup lang="ts">


  import {ref, computed, inject, onMounted} from "vue";
  import {OrganizationsService, FacilityGetDTO} from "opensilex-core/index";
  import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
  import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
  import {useRoute} from "vue-router";
  import {LocationsService} from "opensilex-core/api/locations.service";
  import { useI18n } from 'vue-i18n'
  import {useStore} from "vuex";
  import AnnotationList from "@/components/annotations/list/AnnotationList.vue";

  //#region: Constant values & Services
  const $route = useRoute();
  const $store = useStore();
  const $opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>('$opensilex');
  const { t } = useI18n();
  const service: OrganizationsService = $opensilex.getService<OrganizationsService>('opensilex.OrganizationsService');
  const locationsService: LocationsService = $opensilex.getService<LocationsService>('opensilex.LocationsService');
  //#endregion

  //#region: Value refs

  const name = ref("");
  const positionsCountIsLoading = ref(true);
  const positions = ref<number | null>(null);
  //#endregion

  //#region: Component Refs
  const annotationList = ref<InstanceType<typeof AnnotationList> | null>(null);
  //#endregion

  //#region: Hooks
  onMounted(() => {
    if (uri.value) {
      service
        .getFacility(uri.value)
        .then((http: HttpResponse<OpenSilexResponse<FacilityGetDTO>>) => {
          name.value = http.response.result.name;
        })
        .catch((error) => {
          $opensilex.errorHandler(error);
        });
    }

   searchCountPositions();
  });
  //#endregion

  //#region: Functions
  function searchCountPositions(){
    return locationsService
      .countLocations(uri.value)
      .then((http: HttpResponse<OpenSilexResponse<number>>) => {
        if (http && http.response){
          positions.value = http.response.result as number;
          positionsCountIsLoading.value = false;
          return positions.value;
        }
      }).catch($opensilex.errorHandler);
  }
  //#endregion

  //#region: Computed

  const uri = computed(() => {
    return $route.params.uri
      ? decodeURIComponent($route.params.uri as string)
      : null
  });

  const credentials = computed(() => {
    return $store.state.credentials;
  });

  const tabs = computed(() => {
    if (!uri.value) return []

    return [
      {
        key: 'details',
        label: t('FacilityView.details'),
        to: `/facility/details/${encodeURIComponent(uri.value)}`
      },
      {
        key: 'overview',
        label: t('FacilityView.overview'),
        to: `/facility/overview/${encodeURIComponent(uri.value)}`
      },
      {
        key: 'annotations',
        label: t('component.annotation.list-title'),
        to: `/facility/annotations/${encodeURIComponent(uri.value)}`
      },
      {
        key: 'document',
        label: t('FacilityView.document'),
        to: `/facility/document/${encodeURIComponent(uri.value)}`
      },
      {
        key: 'positions',
        //TODO MAX when the correct component is present , move translation to global like for Annotations
        label: t('Position.list-title'),
        to: `/facility/positions/${encodeURIComponent(uri.value)}`
      }
    ]
  });

  const currentTab = computed(() => {
    const path = $route.path

    if (path.includes('/details/')) return 'details'
    if (path.includes('/overview/')) return 'overview'
    if (path.includes('/annotations/')) return 'annotations'
    if (path.includes('/document/')) return 'document'
    if (path.includes('/positions/')) return 'positions'

    return null
  })
  //#endregion

</script>

<style scoped lang="scss">

.tab {
  text-decoration: none;
  color: inherit;
}

.wip-icon {
  float: right;
  padding-left: 5px;
  width: 28px;
}

</style>

<i18n>
en:
  FacilityView:
    details: Details
    overview: Monitoring
    document: Documents
fr:
  FacilityView:
    details: Détail
    overview: Supervision
    document: Documents
</i18n>
