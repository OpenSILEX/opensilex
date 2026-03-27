<template>
  <div v-if="selected" class="container-fluid">
    <opensilex-PageHeader
      icon="bi#bi-globe"
      :hasIcon="true"
      :title="selected.name"
      :description="selected.rdf_type_name"
      class="detail-element-header"
    />

    <opensilex-PageActions :tabs="false" :returnButton="true" />

    <div class="detail-content">
      <div class="left-side">
        <!-- Organization detail -->
        <opensilex-OrganizationDetail
          :selected="selected"
          :withActions="true"
          @onDelete="deleteOrganization"
          @onUpdate="refresh"
        />

        <!-- Site list -->
        <n-card class="mt-3">
          <opensilex-SiteView
            :organizationsForFilter="[selected.uri]"
          />
        </n-card>
      </div>

      <div class="right-side">
        <!-- Organization facilities -->
        <opensilex-FacilitiesView
          :withActions="true"
          :organization="selected"
          :isSelectable="false"
          :facilities="selected.facilities"
          :fetchAndShowCurrentExperiments="true"
          :createButtonLabel="t('OrganizationDetailView.create-facility')"
          @onUpdate="refresh"
          @onCreate="refresh"
          @onDelete="refresh"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { inject, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useI18n } from 'vue-i18n';
import { NCard } from "naive-ui";
import type HttpResponse from "../../lib/HttpResponse";
import type { OpenSilexResponse } from "../../lib/HttpResponse";
import type OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
// @ts-ignore
import { OrganizationsService } from "opensilex-core/api/organizations.service";
import type { OrganizationGetDTO } from "opensilex-core/index";

const $opensilex = inject<OpenSilexVuePlugin>("$opensilex")!;
const route = useRoute();
const router = useRouter();
const { t } = useI18n();

const selected = ref<OrganizationGetDTO | null>(null);
const uri = ref<string>("");
const service = $opensilex.getService<OrganizationsService>("opensilex-core.OrganizationsService")

watch(
  () => route.params.uri,
  (newUri) => {
    if (!newUri || typeof newUri !== "string") {
      return;
    }

    uri.value = decodeURIComponent(newUri);
    refresh();
  },
  { immediate: true }
);

function refresh() {
  service
    .getOrganization(uri.value)
    .then((http: HttpResponse<OpenSilexResponse<OrganizationGetDTO>>) => {
      selected.value = http.response.result;
    })
    .catch($opensilex.errorHandler);
}

function deleteOrganization() {
  service
    .deleteOrganization(uri.value)
    .then(() => {
      router.push({
        path: "/organizations",
      });
    })
    .catch($opensilex.errorHandler);
}
</script>

<style scoped lang="scss">
.detail-content {
  display: flex;
  justify-content: space-between;
}

.detail-content > * {
  width: 49%;
}

.left-side {
  margin-top: 2.4vh;
}
</style>

<i18n>
en:
  OrganizationDetailView:
    create-facility: "Create facility"
fr:
  OrganizationDetailView:
    create-facility: "Créer une installation environnementale"
</i18n>
