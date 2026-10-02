<template>
  <div>
    <FormSelector
        ref="formSelector"
        :label="property.name"
        v-model:selected="internalValue"
        :multiple="property.is_list"
        :required="property.is_required"
        :optionsLoadingMethod="loadFacilities"
        :placeholder="$t('FacilityPropertySelector.placeholder')"
    />
  </div>
</template>

<script setup lang="ts">
import { inject } from "vue";
import FormSelector from "@/components/common/forms/FormSelector.vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {
  ExperimentsService,
  OrganizationsService
} from "../../../../../opensilex-core/front/src/lib";
import {useI18n} from "vue-i18n";

const { t } = useI18n()
const opensilex = inject<OpenSilexVuePlugin>("$opensilex");

const props = defineProps<{
  property: {
    name: string;
    is_required: boolean;
    is_list: boolean;
    target_property: string;
    comment?: string;
  };
  context?: {
    experimentURI?: string;
    organizationURI?: string;
  };
}>();

const internalValue = defineModel<string>("value");

function loadFacilities() {
  if (props.context?.experimentURI) {
    return opensilex
        ?.getService("opensilex.ExperimentsService")
        .getAvailableFacilities<ExperimentsService>(
            props.context.experimentURI
        )
        .then(http => {
          return http.response.result.map(facility => ({
            id: facility.uri,
            label: facility.name
          }));
        });
  }

  if (props.context?.organizationURI) {
    return opensilex
        ?.getService("opensilex.OrganizationsService")
        .getOrganization<OrganizationsService>(props.context.organizationURI)
        .then(http => {
          return http.response.result.facilities.map(facility => ({
            id: facility.uri,
            label: t('component.site.facilities')
          }));
        });
  }

  return opensilex
      ?.getService("opensilex.OrganizationsService")
      .getAllFacilities<OrganizationsService>()
      .then(http => {
        return http.response.result.map(facility => ({
          id: facility.uri,
          label: facility.name || facility.uri
        }));
      });
}
</script>

<style scoped lang="scss">
</style>