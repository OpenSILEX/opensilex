<template>
  <div class="container-fluid" v-if="selected">
    <PageHeader
      icon="bi#bi-geo-alt"
      :hasIcon="true"
      :title="selected.name"
      :description="selected.rdf_type_name"
      class="detail-element-header"
    />

    <PageActions :tabs="false" :returnButton="true" />

    <div id="detail-content">
      <div id="left-side">
        <SiteDetail
          :selected="selected"
          :withActions="true"
          @onUpdate="refresh"
          @onDelete="redirectToSites"
        />
      </div>

      <div id="right-side">
        <FacilitiesView
          :withActions="true"
          :site="selected"
          :isSelectable="false"
          :facilities="selected.facilities"
          :fetchAndShowCurrentExperiments="true"
          :createButtonLabel="t('SiteDetailView.create-facility')"
          @onUpdate="refresh"
          @onCreate="refresh"
          @onDelete="refresh"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, inject } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useI18n } from 'vue-i18n'

import type HttpResponse from "../../../lib/HttpResponse";
import type { OpenSilexResponse } from "../../../lib/HttpResponse";
import type { OrganizationsService } from "opensilex-core/api/organizations.service";
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { SiteGetDTO } from "opensilex-core/index";
import PageHeader from "@/components/layout/PageHeader.vue";
import PageActions from "@/components/layout/PageActions.vue";
import SiteDetail from "@/components/organizations/site/SiteDetail.vue";
import FacilitiesView from "@/components/facilities/FacilitiesView.vue";

const route = useRoute();
const router = useRouter();
const { t } = useI18n()

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

const selected = ref<SiteGetDTO | null>(null);
const uri = ref<string>("");

let service: OrganizationsService;

function redirectToSites() {
  router.push({ path: "/sites" });
}

function refresh() {
  if (!service || !uri.value) return;

  service
    .getSite(uri.value)
    .then((http: HttpResponse<OpenSilexResponse<SiteGetDTO>>) => {
      selected.value = http.response.result;
    });
}

onMounted(() => {
  uri.value = decodeURIComponent(String(route.params.uri ?? ""));
  service = opensilex.getService("opensilex-core.OrganizationsService") as OrganizationsService;
  refresh();
});
</script>

<style scoped lang="scss">
#right-side {
  margin-top: 1.2vh;
}

#detail-content {
  display: flex;
  justify-content: space-between;
}

#detail-content > * {
  width: 48%;
}
</style>

<i18n>
en:
  SiteDetailView:
    create-facility: "Create facility"
fr:
  SiteDetailView:
    create-facility: "Créer une installation environnementale"
</i18n>
