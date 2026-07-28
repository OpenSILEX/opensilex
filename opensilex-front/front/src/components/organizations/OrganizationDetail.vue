<template>
  <div>
    <Card
      v-if="selected"
      icon="bi-clipboard"
      :no-footer="true"
      :label="t('component.common.informations')"
    >
      <template #rightHeader>
        <EditButton
          v-if="
              withActions &&
              user?.hasCredential(
              credentials?.CREDENTIAL_ORGANIZATION_MODIFICATION_ID
              )
          "
          @click="editOrganization"
          label="component.organization.update"
          :small="true"
        />
        <DeleteButton
          v-if="
              withActions &&
              user?.hasCredential(
              credentials?.CREDENTIAL_ORGANIZATION_DELETE_ID
              )
          "
          @click="deleteOrganization"
          label="component.organization.delete"
          :small="true"
        />
      </template>

      <template #body>
        <div class="detailsCard">
          <!-- URI -->
          <UriView
              :uri="selected.uri"
              :value="selected.uri"
              :to="{
              path: '/organization/details/' + encodeURIComponent(selected.uri),
              }"
          />

          <!-- Name -->
          <StringView
              label="component.common.name"
              :value="selected.name"
          />

          <!-- Type -->
          <TypeView
              :type="selected.rdf_type"
              :typeLabel="selected.rdf_type_name"
          />

          <!-- Parents -->
          <UriListView
              v-if="hasParents"
              :label="t('OrganizationDetail.parentOrganizations')"
              :list="parentUriList"
              :inline="false"
          />

          <!-- Groups -->
          <UriListView
              v-if="hasGroups"
              :label="t('OrganizationDetail.groups.label')"
              :list="groupUriList"
              :inline="false"
          />

          <!-- Expe -->
          <UriListView
              v-if="hasExperiments"
              :label="t('OrganizationDetail.experiments.label')"
              :list="experimentUriList"
              :inline="false"
          />

          <!-- Metadata -->
          <MetadataView
              v-if="selected.publisher && selected.publisher.uri"
              :publisher="selected.publisher"
              :publicationDate="selected.publication_date"
              :lastUpdatedDate="selected.last_updated_date"
          />
        </div>
      </template>
    </Card>

    <OrganizationForm
      ref="organizationForm"
      :createTitle="t('OrganizationDetail.add')"
      :editTitle="t('component.organization.update')"
      @onCreate="emit('onCreate', $event)"
      @onUpdate="emit('onUpdate', $event)"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, useTemplateRef } from "vue";
import { useStore } from "vuex";
import { useI18n } from "vue-i18n";
import type HttpResponse from "../../lib/HttpResponse";
import type { OpenSilexResponse } from "../../lib/HttpResponse";
import type OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import { inject } from "vue";
import DTOConverter from "../../models/DTOConverter";
import { OrganizationsService } from "opensilex-core/api/organizations.service";
import type { OrganizationGetDTO } from "opensilex-core/index";
import Card from "@/components/common/views/Card.vue";
import EditButton from "@/components/common/buttons/EditButton.vue";
import DeleteButton from "@/components/common/buttons/DeleteButton.vue";
import UriView from "@/components/common/views/UriView.vue";
import StringView from "@/components/common/views/StringView.vue";
import TypeView from "@/components/common/views/TypeView.vue";
import UriListView from "@/components/common/views/UriListView.vue";
import MetadataView from "@/components/common/views/MetadataView.vue";
import OrganizationForm from "@/components/organizations/OrganizationForm.vue";

const props = withDefaults(
  defineProps<{
    selected?: OrganizationGetDTO;
    withActions?: boolean;
  }>(),
  {
    withActions: false,
  }
);

const emit = defineEmits<{
  (e: "onCreate", value: any): void;
  (e: "onUpdate", value: any): void;
  (e: "onDelete"): void;
}>();

const { t } = useI18n();
const store = useStore();
const opensilex = inject<OpenSilexVuePlugin>("$opensilex")!;
const organizationService = opensilex.getService<OrganizationsService>(
  "opensilex-core.OrganizationsService"
);

const organizationForm = useTemplateRef<InstanceType<typeof OrganizationForm>>('organizationForm');

const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);

const hasParents = computed(() => (props.selected?.parents?.length ?? 0) > 0);
const hasGroups = computed(() => (props.selected?.groups?.length ?? 0) > 0);
const hasSites = computed(() => (props.selected?.sites?.length ?? 0) > 0);
const hasExperiments = computed(
  () => (props.selected?.experiments?.length ?? 0) > 0
);

const groupUriList = computed(() => {
  return (props.selected?.groups ?? []).map((group: any) => ({
    uri: group.uri,
    value: group.name,
    to: {
      path: "/groups#" + encodeURIComponent(group.uri),
    },
  }));
});

const parentUriList = computed(() => {
  return (props.selected?.parents ?? []).map((parent: any) => ({
    uri: parent.uri,
    value: parent.name,
    to: {
      path: "/organization/details/" + encodeURIComponent(parent.uri),
    },
  }));
});

const experimentUriList = computed(() => {
  return (props.selected?.experiments ?? []).map((experiment: any) => ({
    uri: experiment.uri,
    value: experiment.name,
    to: {
      path: "/experiment/details/" + encodeURIComponent(experiment.uri),
    },
  }));
});

function editOrganization() {
  if (!props.selected?.uri) {
    return;
  }

  organizationService
    .getOrganization(props.selected.uri)
    .then((http: HttpResponse<OpenSilexResponse<OrganizationGetDTO>>) => {
      const editDto = DTOConverter.extractURIFromResourceProperties(
        http.response.result
      );
      organizationForm.value?.showEditForm(editDto);
    })
    .catch(opensilex.errorHandler);
}

function deleteOrganization() {
  emit("onDelete");
}

defineExpose({
  editOrganization,
  deleteOrganization,
});
</script>

<style scoped lang="scss">
.detailsCard {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

</style>

<i18n>
en:
  OrganizationDetail:
    add: Add organization
    parentOrganizations: Parent organizations
    groups:
      label: "Groups"
    facilities:
      label: "Facilities"
    sites:
      label: "Sites"
    experiments:
      label: "Experiments"
fr:
  OrganizationDetail:
    add: Ajouter une organisation
    parentOrganizations: Organisations parentes
    groups:
      label: "Groupes"
    facilities:
      label: "Installations environnementales"
    sites:
      label: "Sites"
    experiments:
      label: "Expérimentations"
</i18n>