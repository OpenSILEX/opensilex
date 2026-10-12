<template>
  <span class="static-field-line">
    <UriLink
        v-if="label"
        :uri="value"
        :value="label"
        :to="{ path: '/facility/details/' + encodeURIComponent(value) }"
        :target="target"
    ></UriLink>
  </span>
</template>

<script setup lang="ts">
import { inject, ref, watch } from "vue";
import UriLink from "@/components/common/views/UriLink.vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import { OrganizationsService } from "opensilex-core/api/organizations.service";

const props = withDefaults(
    defineProps<{
      value: string,
      target?: string
    }>(),
    { target: "_self" }
)

const opensilex = inject<OpenSilexVuePlugin>("$opensilex")
const organizationsService = opensilex.getService<OrganizationsService>("opensilex.OrganizationsService")

const label = ref<string>("")

watch(
    () => props.value,
    (value) => {
      if (!value) {
        label.value = ""
        return
      }

      opensilex.disableLoader()
      organizationsService.getFacility(value)
          .then(http => {
            label.value = http.response.result.name
          })
          .catch(() => {
            label.value = value
          })
          .finally(() => opensilex.enableLoader())
    },
    { immediate: true }
)

</script>

<style scoped lang="scss">
</style>
