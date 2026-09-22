<template>
  <span class="static-field-line">
    <UriLink v-if="label" :uri="value" :value="label"></UriLink>
  </span>
</template>

<script setup lang="ts">
import { inject, ref, watch } from "vue";
import UriLink from "@/components/common/views/UriLink.vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import { OntologyService } from "opensilex-core/api/ontology.service";

const props = defineProps<{
  value: string
}>()

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

      ontologyService.getURILabel(value)
          .then(http => {
            label.value = http.response.result
          })
          .catch(error => {
            // An object without a label is still worth linking to, so fall back on its URI.
            if (error.status === 404) {
              label.value = value
            } else {
              opensilex.errorHandler(error)
            }
          })
    },
    { immediate: true }
)

</script>

<style scoped lang="scss">
</style>
