<template>
  <Card
    icon="bi-clipboard"
    :label="t('component.common.informations')"
    class="facilityDetailComponent"
    v-if="selected"
  >
    <template v-if="withActions" #rightHeader>
      <EditButton
        v-if="user.hasCredential(credentials.CREDENTIAL_FACILITY_MODIFICATION_ID)"
        @click="onClickEditButton()"
        label="component.facility.buttons.update"
        :small="true"
      ></EditButton>
      <DeleteButton
        v-if=" user.hasCredential(credentials.CREDENTIAL_FACILITY_DELETE_ID)"
        @click="onClickDeleteButton()"
        label="component.facility.buttons.delete"
        :small="true"
      ></DeleteButton>
      <FacilityModalForm
        v-if="user.hasCredential(credentials.CREDENTIAL_FACILITY_MODIFICATION_ID)"
        ref="organizationFacilityForm"
        @onUpdate="emit('update')"
      ></FacilityModalForm>
    </template>

    <template v-slot:body>
      <!-- URI -->
      <UriView
        :uri="selected.uri"
        :value="selected.uri"
      >
      </UriView>
      <!-- Name -->
      <StringView
        :value="selected.name"
        label="component.common.name"
      ></StringView>
      <!-- Type -->
      <TypeView
        :type="selected.rdf_type"
        :typeLabel="selected.rdf_type_name"
      ></TypeView>
      <!-- Description -->
      <StringView
        :value="selected.description"
        label="component.common.description"
      ></StringView>
      <!-- Organisations -->
      <UriListView
        v-if="hasOrganizations"
        label="credential-groups.organizations"
        :list="organizationUriList"
        :inline="false"
        :allowCopy="true"
      >
      </UriListView>

      <!-- Site -->
      <UriListView
        v-if="hasSites"
        label="component.common.organization.site"
        :list="siteUriList"
        :inline="false"
        :allowCopy="true"
      >
      </UriListView>

      <!-- Experiments -->
      <UriListView
        v-if="hasExperiments"
        label="component.experiment.expsInProgress"
        :list="experimentUriList"
        :inline="false"
        :allowCopy="true"
      >
      </UriListView>

      <!-- VariableGroups -->
      <UriListView
        v-if="hasVariableGroups"
        label="component.variable.groupVariable.groupVariable"
        :list="variableGroupUriList"
        :inline="true"
        :allowCopy="true"
      >
      </UriListView>

      <!-- Devices -->
      <UriListView
        v-if="hasDevices"
        label="component.menu.devices"
        :list="deviceUriList"
        :inline="true"
        :allowCopy="true"
      >
      </UriListView>

      <!-- Address -->
      <AddressView
        v-if="selected.address"
        :address="addressViewFormattedAddress"
      >
      </AddressView>

      <!--Last Position-->
      <div v-if="selected.lastPosition">
        <span :class="['field-view-title']">{{ t("component.common.geometry.lastPosition") }}</span>
        <div v-if="selected.lastPosition.endDate">
          <span>{{ new Date(selected.lastPosition.endDate).toLocaleString() }}</span>
        </div>
        <ul>
          <li>
            <GeometryCopy
              label=""
              :value="selected.lastPosition.geojson">
            </GeometryCopy>
          </li>
        </ul>
      </div>

      <OntologyObjectProperties
        :selected="selected"
        :parentType="oeso.FACILITY_TYPE_URI"
        :relations="selected.relations"
      >
      </OntologyObjectProperties>

      <!-- Metadata -->
      <MetadataView
        v-if="selected.publisher && selected.publisher.uri"
        :publisher="selected.publisher"
        :publicationDate="selected.publication_date"
        :lastUpdatedDate="selected.last_updated_date"
      ></MetadataView>
    </template>
  </Card>
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
import Card from "@/components/common/views/Card.vue";
import EditButton from "@/components/common/buttons/EditButton.vue";
import DeleteButton from "@/components/common/buttons/DeleteButton.vue";
import FacilityModalForm from "@/components/facilities/FacilityModalForm.vue";
import UriView from "@/components/common/views/UriView.vue";
import StringView from "@/components/common/views/StringView.vue";
import TypeView from "@/components/common/views/TypeView.vue";
import UriListView from "@/components/common/views/UriListView.vue";
import GeometryCopy from "@/components/common/views/GeometryCopy.vue";
import AddressView from "@/components/common/views/AddressView.vue";
import MetadataView from "@/components/common/views/MetadataView.vue";

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

const addressViewFormattedAddress = computed(() => {return {"readableAddress": props.selected.address.readableAddress}})

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

