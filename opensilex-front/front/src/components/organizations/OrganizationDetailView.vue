<template>
  <div v-if="selected" class="container-fluid">
    <PageHeader
      icon="bi#bi-globe"
      :hasIcon="true"
      :title="selected.name"
      :description="selected.rdf_type_name"
      class="detail-element-header"
    />

    <PageActions :tabs="false" :returnButton="true" />

    <div class="detail-content">
      <div class="left-side">
        <!-- Organization detail -->
        <OrganizationDetail
          :selected="selected"
          :withActions="true"
          @onDelete="deleteOrganization"
          @onUpdate="refresh"
        />

        <!-- Site list -->
        <n-card class="mt-3">
          <SiteView
            :organizationsForFilter="[selected.uri]"
          />
        </n-card>
      </div>

      <div class="right-side">
        <!-- Organization facilities -->
        <FacilitiesView
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
import OrganizationDetail from "@/components/organizations/OrganizationDetail.vue";
import PageActions from "@/components/layout/PageActions.vue";
import PageHeader from "@/components/layout/PageHeader.vue";
import SiteView from "@/components/organizations/site/SiteView.vue";
import FacilitiesView from "@/components/facilities/FacilitiesView.vue";
import {useStore} from "vuex";

const $opensilex = inject<OpenSilexVuePlugin>("$opensilex")!;
const route = useRoute();
const router = useRouter();
const { t } = useI18n();

const selected = ref<OrganizationGetDTO | null>(null);
const uri = ref<string>("");
const service = $opensilex.getService<OrganizationsService>("opensilex-core.OrganizationsService")
const store = useStore()

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

watch(() => store.getters.language, () => refresh())


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
