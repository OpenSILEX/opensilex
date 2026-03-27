<template>
  <div style="display: contents;">
    <!-- Card -->
    <div v-if="selected" class="card">
      <div class="card-header d-flex align-items-start justify-content-between">
        <h3 class="mb-0">
          <opensilex-Icon icon="bi#bi-clipboard" />
          {{ t("component.common.details-label") }}
        </h3>

        <div v-if="withActions" class="card-header-right">
          <div class="btn-group" role="group" aria-label="actions">
            <opensilex-EditButton
              v-if="user.hasCredential(credentials.CREDENTIAL_ORGANIZATION_MODIFICATION_ID)"
              @click="editSite"
              label="component.site.update"
              :small="true"
            />
            <opensilex-DeleteButton
              v-if="user.hasCredential(credentials.CREDENTIAL_ORGANIZATION_DELETE_ID)"
              @click="deleteSite"
              label="component.site.delete"
              :small="true"
            />
          </div>
        </div>
      </div>

      <div class="card-body">
        <!-- URI -->
        <opensilex-UriView
          :uri="selected.uri"
          :value="selected.uri"
          :to="{ path: '/organization/site/details/' + encodeURIComponent(selected.uri) }"
        />

        <!-- Name -->
        <opensilex-StringView label="component.common.name" :value="selected.name" />

        <!-- Description -->
        <opensilex-StringView label="component.common.description" :value="selected.description" />

        <!-- Type -->
        <opensilex-TypeView :type="selected.rdf_type" :typeLabel="selected.rdf_type_name" />

        <!-- Organizations -->
        <opensilex-UriListView
          v-if="hasOrganizations"
          :list="organizationUriList"
          :label="t('SiteDetail.organizations')"
          :inline="true"
        />

        <!-- Groups -->
        <opensilex-UriListView
          v-if="hasGroups"
          :label="t('SiteDetail.groups')"
          :list="groupUriList"
          :inline="true"
        />

        <!-- Address -->
        <opensilex-AddressView
          v-if="selected.address"
          :address="selected.address"
          :geometry="selected.geometry"
          noGeometryLabel="SiteDetail.noGeometryWarning"
        />

        <!-- Warning geometry -->
        <div
          v-if="selected.address && !selected.geometry"
          class="alert alert-warning"
          role="alert"
        >
          {{ t("component.common.geometry-address-warning") }}
        </div>

        <!-- Metadata -->
        <opensilex-MetadataView
          v-if="selected.publisher && selected.publisher.uri"
          :publisher="selected.publisher"
          :publicationDate="selected.publication_date"
          :lastUpdatedDate="selected.last_updated_date"
        />
      </div>
    </div>

    <!-- Modal form -->
    <opensilex-ModalForm
      ref="siteForm"
      component="opensilex-SiteForm"
      createTitle="add"
      editTitle="edit"
      icon="bi#bi-geo-alt"
      @onCreate="(e) => emit('onCreate', e)"
      @onUpdate="(e) => emit('onUpdate', e)"
      :initForm="initForm"
      :lazy="true"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref } from "vue";
import { useStore } from "vuex";
import { useI18n } from 'vue-i18n'
import type HttpResponse from "../../../lib/HttpResponse";
import type { OpenSilexResponse } from "../../../lib/HttpResponse";
import type { OrganizationsService } from "opensilex-core/api/organizations.service";
import DTOConverter from "../../../models/DTOConverter";
import type OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import type { SiteGetDTO, SiteUpdateDTO } from "opensilex-core/index";

// Props
const props = withDefaults(
  defineProps<{
    selected: SiteGetDTO | null;
    withActions?: boolean;
  }>(),
  { withActions: false }
);

// Emits
const emit = defineEmits<{
  (e: "onCreate", payload: unknown): void;
  (e: "onUpdate", payload: unknown): void;
  (e: "onDelete"): void;
}>();

// Store
const store = useStore() as any;
const { t } = useI18n();
const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);
const opensilex = inject<OpenSilexVuePlugin>("$opensilex")!;

// Service
const organizationService = opensilex.getService(
  "opensilex-core.OrganizationsService"
) as OrganizationsService;

// Ref modal
const siteForm = ref<any>(null);

// Computed lists
const hasOrganizations = computed(
  () =>
    !!props.selected &&
    Array.isArray(props.selected.organizations) &&
    props.selected.organizations.length > 0
);

const hasGroups = computed(
  () =>
    !!props.selected &&
    Array.isArray(props.selected.groups) &&
    props.selected.groups.length > 0
);

const organizationUriList = computed(() => {
  if (!props.selected?.organizations) return [];
  return props.selected.organizations.map((organization: any) => ({
    uri: organization.uri,
    value: organization.name,
    to: { path: "/organization/details/" + encodeURIComponent(organization.uri) },
  }));
});

const groupUriList = computed(() => {
  if (!props.selected?.groups) return [];
  return props.selected.groups.map((group: any) => ({
    uri: group.uri,
    value: group.name,
    to: { path: "/groups#" + encodeURIComponent(group.uri) },
  }));
});

// Methods
function editSite() {
  if (!props.selected?.uri) return;

  organizationService
    .getSite(props.selected.uri)
    .then((http: HttpResponse<OpenSilexResponse<SiteGetDTO>>) => {
      const editDto: SiteUpdateDTO =
        DTOConverter.extractURIFromResourceProperties(http.response.result);
      siteForm.value?.showEditForm?.(editDto);
    })
    .catch(opensilex.errorHandler);
}

function deleteSite() {
  if (!props.selected?.uri) return;

  organizationService
    .deleteSite(props.selected.uri)
    .then(() => emit("onDelete"))
    .catch(opensilex.errorHandler);
}

function initForm(form: any) {
  form.organizations = props.selected?.organizations;
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  add: Add site
  edit: Update site
  delete: Delete site
  SiteDetail:
    organizations: Organizations
    facilities: Facilities
    groups: Groups
    noGeometryWarning: No geometry was associated with the address. Maybe the address is invalid.
fr:
  add: Ajouter un site
  edit: Modifier le site
  delete: Supprimer le site
  SiteDetail:
    facilities: Installations environnementales
    groups: Groupes
    noGeometryWarning: Aucune géométrie n'a pu être déterminée à partir de l'adresse. L'adresse est peut-être invalide.
</i18n>