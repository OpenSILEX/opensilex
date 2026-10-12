<template>
  <span class="static-field-line">
    <UriLink
        v-if="label"
        :uri="value"
        :value="label"
        :to="{ path: '/germplasm/details/' + encodeURIComponent(value) }"
        :target="target"
    ></UriLink>
  </span>
</template>

<script setup lang="ts">
import { inject, ref, watch } from "vue";
import UriLink from "@/components/common/views/UriLink.vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import { OntologyService } from "opensilex-core/api/ontology.service";

const props = withDefaults(
    defineProps<{
      value: string,
      target?: string
    }>(),
    { target: "_self" }
)

const opensilex = inject<OpenSilexVuePlugin>("$opensilex")
const ontologyService = opensilex.getService<OntologyService>("opensilex.OntologyService")

const label = ref<string>("")

watch(
    () => props.value,
    (value) => {
      if (!value) {
        label.value = ""
        return
      }

      // The global loader would flash on every property line of the detail panel.
      opensilex.disableLoader()
      ontologyService.getURILabel(value)
          .then(http => {
            label.value = http.response.result
          })
          .catch(error => {
            if (error.status === 404) {
              label.value = value
            } else {
              opensilex.errorHandler(error)
            }
          })
          .finally(() => opensilex.enableLoader())
    },
    { immediate: true }
)

</script>

<style scoped lang="scss">
</style>
