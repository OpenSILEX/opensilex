<template>
  <opensilex-Card
    icon="bi-clipboard"
    :label="t('component.common.informations')"
    class="facilityDetailComponent"
    v-if="selected"
  >
    <template v-if="withActions" #rightHeader>
      <opensilex-EditButton
        v-if="user.hasCredential(credentials.CREDENTIAL_FACILITY_MODIFICATION_ID)"
        @click="onClickEditButton()"
        label="component.facility.buttons.update"
        :small="true"
      ></opensilex-EditButton>
      <opensilex-DeleteButton
        v-if=" user.hasCredential(credentials.CREDENTIAL_FACILITY_DELETE_ID)"
        @click="onClickDeleteButton()"
        label="component.facility.buttons.delete"
        :small="true"
      ></opensilex-DeleteButton>
      <opensilex-FacilityModalForm
        v-if="user.hasCredential(credentials.CREDENTIAL_FACILITY_MODIFICATION_ID)"
        ref="organizationFacilityForm"
        @onUpdate="emit('update')"
      ></opensilex-FacilityModalForm>
    </template>

    <template v-slot:body>
      <!-- URI -->
      <opensilex-UriView
        :uri="selected.uri"
        :value="selected.uri"
      >
      </opensilex-UriView>
      <!-- Name -->
      <opensilex-StringView
        :value="selected.name"
        label="component.common.name"
      ></opensilex-StringView>
      <!-- Type -->
      <opensilex-TypeView
        :type="selected.rdf_type"
        :typeLabel="selected.rdf_type_name"
      ></opensilex-TypeView>
      <!-- Description -->
      <opensilex-StringView
        :value="selected.description"
        label="component.common.description"
      ></opensilex-StringView>
      <!-- Organisations -->
      <opensilex-UriListView
        v-if="hasOrganizations"
        label="credential-groups.organizations"
        :list="organizationUriList"
        :inline="false"
      >
      </opensilex-UriListView>

      <!-- Site -->
      <opensilex-UriListView
        v-if="hasSites"
        label="component.common.organization.site"
        :list="siteUriList"
        :inline="false"
      >
      </opensilex-UriListView>

      <!-- Experiments -->
      <opensilex-UriListView
        v-if="hasExperiments"
        label="component.experiment.expsInProgress"
        :list="experimentUriList"
        :inline="false"
      >
      </opensilex-UriListView>

      <!-- VariableGroups -->
      <opensilex-UriListView
        v-if="hasVariableGroups"
        label="component.variable.groupVariable.groupVariable"
        :list="variableGroupUriList"
        :inline="true"
      >
      </opensilex-UriListView>

      <!-- Devices -->
      <opensilex-UriListView
        v-if="hasDevices"
        label="component.menu.devices"
        :list="deviceUriList"
        :inline="true"
      >
      </opensilex-UriListView>

      <!-- Address -->
      <opensilex-AddressView
        v-if="selected.address"
        :address="selected.address"
      >
      </opensilex-AddressView>

      <!--Last Position-->
      <opensilex-StringView v-if="selected.lastPosition" label="component.common.geometry.lastPosition">
        <!-- Position detail -->
        <div v-if="selected.lastPosition.endDate">
          <span>{{ new Date(selected.lastPosition.endDate).toLocaleString() }}</span>
        </div>
        <ul>
          <li>
            <opensilex-GeometryCopy
              label=""
              :value="selected.lastPosition.geojson">
            </opensilex-GeometryCopy>
          </li>
        </ul>
      </opensilex-StringView>

      <opensilex-OntologyObjectProperties
        :selected="selected"
        :parentType="oeso.FACILITY_TYPE_URI"
        :relations="selected.relations"
      >
      </opensilex-OntologyObjectProperties>

      <!-- Metadata -->
      <opensilex-MetadataView
        v-if="selected.publisher && selected.publisher.uri"
        :publisher="selected.publisher"
        :publicationDate="selected.publication_date"
        :lastUpdatedDate="selected.last_updated_date"
      ></opensilex-MetadataView>
    </template>
  </opensilex-Card>
</template>

<script setup lang="ts">
import {computed, inject, ref} from "vue";
import type {FacilityGetDTO} from 'opensilex-core/index';
import {ExperimentGetListDTO} from "opensilex-core/model/experimentGetListDTO";
import {DeviceGetDTO} from "opensilex-core/model/deviceGetDTO";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {useRouter} from "vue-router";
import {useStore} from "vuex";
import {useI18n} from "vue-i18n";

//#region: Constants
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex');
const $store = useStore();
const router = useRouter();
const { t } = useI18n()
//#endregion

//#region: Props and Emits
interface Props {
  selected?: FacilityGetDTO;
  experiments?: ExperimentGetListDTO[];
  devices?: DeviceGetDTO[];
  withActions?: boolean;
}
const props = withDefaults(defineProps<Props>(), {
  withActions: false
});
const emit = defineEmits<{
  (e: 'update'): void;
}>();
//#endregion

//#region: Component Refs
const organizationFacilityForm = ref();
//#endregion

//#region: Computed
const oeso = computed(() => $opensilex.Oeso);
const user = computed(() => $store.state.user);
const credentials = computed(() => $store.state.credentials);

const hasOrganizations = computed(() =>
  props.selected?.organizations?.length > 0
);

const hasExperiments = computed(() =>
  props.experiments?.length > 0
);

const hasVariableGroups = computed(() =>
  props.selected?.variableGroups?.length > 0
);

const hasDevices = computed(() =>
  props.devices?.length > 0
);

const hasSites = computed(() =>
  props.selected?.sites?.length > 0
);

const organizationUriList = computed(() => {
  return props.selected?.organizations?.map(org => ({
    uri: org.uri,
    value: org.name,
    to: {
      path: "/organization/details/" + encodeURIComponent(org.uri),
    },
  })) ?? [];
});

const experimentUriList = computed(() => {
  return props.experiments?.map(exp => ({
    uri: exp.uri,
    value: exp.name,
    to: {
      path: "/experiment/details/" + encodeURIComponent(exp.uri),
    },
  })) ?? [];
});

const deviceUriList = computed(() => {
  return props.devices?.map(device => ({
    uri: device.uri,
    value: device.name,
    to: {
      path: "/device/details/" + encodeURIComponent(device.uri),
    },
  })) ?? [];
});

const siteUriList = computed(() => {
  return props.selected?.sites?.map(site => ({
    uri: site.uri,
    value: site.name,
    to: {
      path: "/organization/site/details/" + encodeURIComponent(site.uri),
    },
  })) ?? [];
});

const variableGroupUriList = computed(() => {
  return props.selected?.variableGroups?.map(varGroup => ({
    uri: varGroup.uri,
    value: varGroup.name,
    to: {
      path:
        "/variables?elementType=VariableGroup&selected=" +
        encodeURIComponent(varGroup.uri),
    },
  })) ?? [];
});
//#endregion

//#region: Event handlers
function onClickEditButton() {
  organizationFacilityForm.value?.showEditForm(props.selected.uri);
}
function onClickDeleteButton() {
  $opensilex
    .getService<OrganizationsService>("opensilex.OrganizationsService")
    .deleteFacility(props.selected.uri)
    .then(() => {
      router.push({
        path: "/organizations",
      });
    })
    .catch($opensilex.errorHandler);
}
//#endregion

</script>

<style scoped>
@media only screen and (min-width: 768px) {
  .facilityDetailComponent {
    margin-top: 30px;
  }
}

</style>

