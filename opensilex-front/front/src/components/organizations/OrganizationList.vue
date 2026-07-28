<template>
  <n-layout has-sider class="organization-layout">
    <!-- Bouton loupe -->
    <n-space class="mb-2 me-1" align="top">
      <n-button
        quaternary
        circle
        @click="filtersCollapsed = !filtersCollapsed"
        :title="t('searchfilter.label')"
        :class="{ greenThemeColor: filtersCollapsed }"
        class="globalFiltersSearchButton"
      >
        <i class="bi bi-search filtersGlobalSearchIcon"></i>

        <div
          v-show="filtersCollapsed && activeFiltersCount > 0"
          class="filters-count-badge"
        >
          ( {{ activeFiltersCount }} )
        </div>
      </n-button>
    </n-space>

    <!-- Sidebar / Filtres -->
    <n-layout-sider
      v-model:collapsed="filtersCollapsed"
      :collapsed-width="0"
      :width="360"
      collapse-mode="width"
      show-trigger
      bordered
    >
      <n-space class="p-3" vertical>
        <n-form
          label-placement="top"
          size="small"
          @submit.prevent.stop="applyFilters"
        >
          <!-- Name -->
          <n-form-item :label="t('component.common.name')">
            <n-input
              v-model:value="filter.name"
              clearable
              :placeholder="t('OrganizationList.filter.name-placeholder')"
              @keydown.enter.prevent.stop="applyFilters"
            />
          </n-form-item>

          <!-- Type -->
          <opensilex-TypeForm
            v-model:type="filter.type_uri"
            :baseType="$opensilex.Foaf.ORGANIZATION_TYPE_URI"
            :ignoreRoot="false"
            :placeholder="t('OrganizationList.filter.type-placeholder')"
            class="searchFilter typeFilter"
            @handlingEnterKey="applyFilters"
          />

          <!-- Parents of -->
          <opensilex-FormSelector
            v-model:selected="filter.direct_child_uri"
            :options="parentOptions"
            :multiple="false"
            :label="t('OrganizationList.filter.parent-organizations')"
            :helpMessage="t('OrganizationList.filter.parent-organizations-help')"
            :placeholder="t('OrganizationList.parent-placeholder') "
            class="searchFilter"
          />

          <!-- Children of -->
          <opensilex-FormSelector
            v-model:selected="filter.direct_parent_uri"
            :options="parentOptions"
            :multiple="false"
            :label="t('OrganizationList.filter.child-organizations')"
            :helpMessage="t('OrganizationList.filter.child-organizations-help')"
            :placeholder="t('OrganizationList.child-placeholder')"
            class="searchFilter"
          />

          <!-- Facility -->
          <n-form-item :label="t('OrganizationList.facilities-label')">
            <opensilex-FacilitySelector
              v-model:facilities="filter.facility"
              :multiple="false"
              class="searchFilter"
            />
          </n-form-item>

          <n-space justify="end" class="mt-2">
            <opensilex-Button
              class="resetButton"
              :label="t('component.common.search.clear-button')"
              icon="bi-x-lg"
              @click="resetFilters"
            />
            <opensilex-Button
              class="greenThemeColor"
              :label="t('component.common.search.search-button')"
              icon="bi-search"
              @click="applyFilters"
            />
          </n-space>
        </n-form>
      </n-space>
    </n-layout-sider>

    <!-- Contenu liste -->
    <n-layout-content class="organization-content">
      <TableView
        :items="organizations"
        :fields="fields"
        sortBy="name"
      >
        <template #cell(name)="{ data }">
          <UriLink
            :uri="data.item.uri"
            :value="data.item.name"
            :to="{ path: '/organization/details/' + encodeURIComponent(data.item.uri) }"
          />
        </template>

        <template #cell(rdf_type_name)="{ data }">
          {{ data.item.rdf_type_name }}
        </template>

        <template #cell(facilities)="{ data }">
          <FacilitiesModalList
            :facilities="data.item.facilities"
            :hostNameForTitle="data.item.name"
            @onCRUD="refresh"
          />
        </template>

        <template #cell(actions)="{ data }" v-if="!props.disableActions">
          <n-button-group size="small" class="btn-group btn-group-sm">
            <EditButton
              v-if="user?.hasCredential(credentials?.CREDENTIAL_ORGANIZATION_MODIFICATION_ID)"
              @click="emitOnEdit(data.item)"
              label="component.organization.update"
              :small="true"
            />
            <DeleteButton
              v-if="user?.hasCredential(credentials?.CREDENTIAL_ORGANIZATION_DELETE_ID)"
              @click="onDeleteClick(data.item)"
              label="component.organization.delete"
              :small="true"
            />
          </n-button-group>
        </template>
      </TableView>
    </n-layout-content>
  </n-layout>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useStore } from "vuex";
import {
  NLayout,
  NLayoutSider,
  NLayoutContent,
  NForm,
  NFormItem,
  NInput,
  NButton,
  NSpace,
  NButtonGroup
} from "naive-ui";
import type OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import DTOConverter from "../../models/DTOConverter";
import type HttpResponse from "../../lib/HttpResponse";
import type { OpenSilexResponse } from "../../lib/HttpResponse";
import type { OpenSilexStore } from "../../models/Store";
import type { OrganizationGetDTO } from "opensilex-core/model/organizationGetDTO";
import type { OrganizationDagDTO } from "opensilex-core/model/organizationDagDTO";
import type { OrganizationUpdateDTO } from "opensilex-core/model/organizationUpdateDTO";
import { OrganizationsService } from "opensilex-core/api/organizations.service";
import TableView from "@/components/common/views/TableView.vue";
import UriLink from "@/components/common/views/UriLink.vue";
import FacilitiesModalList from "@/components/facilities/FacilitiesModalList.vue";
import EditButton from "@/components/common/buttons/EditButton.vue";
import DeleteButton from "@/components/common/buttons/DeleteButton.vue";
import {TableField} from "@/components/common/views/TableField";

interface OrganizationListFilter {
  name: string | undefined;
  type_uri: string | undefined;
  direct_child_uri: string | undefined;
  direct_parent_uri: string | undefined;
  facility: string | undefined;
}

const emit = defineEmits<{
  (e: "onEdit", dto: OrganizationUpdateDTO): void;
}>();

const props = withDefaults(
  defineProps<{
    organizationsToFetch?: string[] | null;
    disableActions?: boolean;
  }>(),
  {
    organizationsToFetch: null,
    disableActions: false,
  }
);

const { t } = useI18n();
const store = useStore() as OpenSilexStore;
const $opensilex = inject<OpenSilexVuePlugin>("$opensilex")!;
const service = $opensilex.getService<OrganizationsService>(
  "opensilex-core.OrganizationsService"
);

const organizations = ref<OrganizationDagDTO[]>([]);
const allOrganizations = ref<OrganizationDagDTO[]>([]);
const filtersCollapsed = ref(true);

function getEmptyFilter(): OrganizationListFilter {
  return {
    name: undefined,
    type_uri: undefined,
    direct_child_uri: undefined,
    direct_parent_uri: undefined,
    facility: undefined,
  };
}

const filter = ref<OrganizationListFilter>(getEmptyFilter());

const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);

const activeFiltersCount = computed(() => {
  return [
    filter.value.name,
    filter.value.type_uri,
    filter.value.direct_child_uri,
    filter.value.direct_parent_uri,
    filter.value.facility,
  ].filter((v) => v !== undefined && String(v).trim() !== "").length;
});

const fields = computed(() => {
  const tableFields: TableField[] = [
    { key: "name", label: t("component.common.name"), sortable: true },
    { key: "rdf_type_name", label: "Type", sortable: true },
    { key: "facilities", label: t("OrganizationList.facilities") },
  ];

  if (!props.disableActions) {
    tableFields.push({ key: "actions", label: t("component.common.actions"), resizable: false, naiveProps: {width: 100} });
  }

  return tableFields;
});

const parentOptions = computed(() => {
  return $opensilex.buildTreeFromDag(allOrganizations.value);
});

watch(filtersCollapsed, (collapsed) => {
  if (!collapsed) {
    refreshOrganizationsForParentFilter();
  }
});

onMounted(() => {
  fetchOrganization();
});

async function fetchOrganization() {
  $opensilex.enableLoader();
  $opensilex.showLoader();

  try {
    const httpResult = await service.searchOrganizations(
      filter.value.name,
      props.organizationsToFetch ?? undefined,
      filter.value.type_uri,
      filter.value.direct_child_uri,
      filter.value.facility
    );

    let result = httpResult.response.result;

    if (filter.value.direct_parent_uri != null) {
      const parentOrganization = allOrganizations.value.find(
        (organization) => organization.uri === filter.value.direct_parent_uri
      );

      result = result.filter((organization) =>
        parentOrganization?.children?.includes(organization.uri)
      );
    }

    organizations.value = result.map((organization: any) => {
      const { children, _children, ...flatOrganization } = organization;
      return flatOrganization;
    });
  } catch (error) {
    $opensilex.errorHandler(error);
  } finally {
    $opensilex.hideLoader();
    $opensilex.disableLoader();
  }
}

function applyFilters() {
  refresh();
  filtersCollapsed.value = true;
}

function refresh() {
  fetchOrganization();

  if (!filtersCollapsed.value) {
    refreshOrganizationsForParentFilter();
  }
}

function resetFilters() {
  filter.value = getEmptyFilter();
  refresh();
}

function refreshOrganizationsForParentFilter() {
  service
    .searchOrganizations()
    .then((http: HttpResponse<OpenSilexResponse<OrganizationDagDTO[]>>) => {
      allOrganizations.value = http.response.result;
    })
    .catch($opensilex.errorHandler);
}

function emitOnEdit(organization: OrganizationDagDTO) {
  service
    .getOrganization(organization.uri)
    .then((http: HttpResponse<OpenSilexResponse<OrganizationGetDTO>>) => {
      const editDto: OrganizationUpdateDTO =
        DTOConverter.extractURIFromResourceProperties(http.response.result);
      emit("onEdit", editDto);
    })
    .catch($opensilex.errorHandler);
}

function onDeleteClick(dto: OrganizationDagDTO) {
  service
    .deleteOrganization(dto.uri)
    .then(() => {
      refresh();
      $opensilex.showSuccessToast(
        `${dto.name} ${t("component.common.success.delete-success-message")}`
      );
    })
    .catch($opensilex.errorHandler);
}

defineExpose({
  refresh,
  resetFilters,
});
</script>

<style scoped lang="scss">
.organization-layout {
  background: transparent;
}

.organization-content {
  padding-left: 12px;
}

.filtersGlobalSearchIcon {
  font-size: 1.2em;
}

.globalFiltersSearchButton {
  width: 40px;
  height: 55px;
}

.globalFiltersSearchButton span {
  display: block !important;
}

.globalFiltersSearchButton div {
  margin-top: 5px;
}

.typeFilter {
  margin-bottom: 15px;
}
</style>

<i18n>
en:
  OrganizationList:
    filter:
        name-placeholder: "Filter by name"
        type-placeholder: "Filter by type"
        parent-organizations: "Parents of"
        parent-organizations-help: "Only direct parents of this organization will be displayed"
        parent-organizations-placeholder: "Filter by child organization"
        child-organizations: "Children of"
        child-organizations-help: "Only direct children of this organization will be displayed"
        child-organizations-placeholder: "Filter by parent organization"
    facilities: "Facilities"
    facilities-label: Facilities
    parent-placeholder: Select parent organization
    child-placeholder: Select child organization
fr:
  OrganizationList:
    filter:
        name-placeholder: "Filtrer par nom"
        type-placeholder: "Filtrer par type"
        parent-organizations: "Parents de"
        parent-organizations-help: "Seuls les parents directs de cette organisation seront affichés"
        parent-organizations-placeholder: "Filtrer par organisation enfant"
        child-organizations: "Enfants de"
        child-organizations-help: "Seuls les enfants directs de cette organisation seront affichés"
        child-organizations-placeholder: "Filtrer par organisation parent"
    facilities: "Installations"
    facilities-label: Installations environnementales
    parent-placeholder: Sélectionner l'organisation parente
    child-placeholder: Sélectionner l'organisation enfant
</i18n>
