<template>
  <div v-if="uri" class="container-fluid">
    <PageHeader
      icon="bi#bi-globe"
      :hasIcon="true"
      :title="name"
      description="component.facility.view.title"
      class= "detail-element-header"
    ></PageHeader>

    <PageActions :returnButton="true" class="navigationTabs">

      <nav class="tabs mb-3">
        <router-link
          v-for="tab in tabs"
          :key="tab.key"
          :to="tab.to"
          :replace="true"
          class="tab"
          active-class="active"
        >
          {{ tab.label }}
        </router-link>
      </nav>
    </PageActions>

    <PageContent v-slot>

        <FacilityDetails
          v-if="currentTab === 'details'"
          :uri="uri"
        ></FacilityDetails>

        <FacilityMonitoringView
          v-else-if="currentTab === 'overview'"
          :uri="uri"
        ></FacilityMonitoringView>

        <LocationList
          v-else-if="currentTab === 'positions'"
          :target="uri"
        ></LocationList>

        <DocumentTabList
          v-else-if="currentTab === 'document'"
          :modificationCredentialId="credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID"
          :uri="uri"
        ></DocumentTabList>

        <AnnotationList
          v-else-if="currentTab === 'annotations'"
          ref="annotationList"
          :target="uri"
          :displayTargetColumn="false"
          :enableActions="true"
          :modificationCredentialId="credentials.CREDENTIAL_ANNOTATION_MODIFICATION_ID"
          :deleteCredentialId="credentials.CREDENTIAL_ANNOTATION_DELETE_ID"
        ></AnnotationList>

    </PageContent>
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
  import PageHeader from "@/components/layout/PageHeader.vue";
  import PageActions from "@/components/layout/PageActions.vue";
  import PageContent from "@/components/layout/PageContent.vue";
  import FacilityDetails from "@/components/facilities/views/FacilityDetails.vue";
  import FacilityMonitoringView from "@/components/facilities/views/FacilityMonitoringView.vue";
  import LocationList from "@/components/location/list/LocationList.vue";
  import DocumentTabList from "@/components/documents/DocumentTabList.vue";

  //#region: Constant values & Services
  const $route = useRoute();
  const $store = useStore();
  const $opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>('$opensilex');
  const { t } = useI18n();
  const service: OrganizationsService = $opensilex.getService<OrganizationsService>('opensilex.OrganizationsService');
  const locationsService: LocationsService = $opensilex.getService<LocationsService>('opensilex.LocationsService');

  const tabLabels = [
    {label: t('FacilityView.details')},
    {label: t('FacilityView.overview')},
    {label: t('component.annotation.list-title')},
    {label: t('FacilityView.document')},
    {label: t('component.common.geometry.positions')}
  ];
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
        label: t('component.common.geometry.positions'),
        to: `/facility/positions/${encodeURIComponent(uri.value)}`
      }
    ]
  });

  const currentTab = computed(() => {
    const tab = $route.path.split('/')[2];

    if (
      tab === 'details' ||
      tab === 'overview' ||
      tab === 'annotations' ||
      tab === 'document' ||
      tab === 'positions'
    ) {
      return tab;
    }

    return 'details';
  })
  //#endregion

</script>

<style scoped lang="scss">

.tab {
  text-decoration: none;
  color: inherit;
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
