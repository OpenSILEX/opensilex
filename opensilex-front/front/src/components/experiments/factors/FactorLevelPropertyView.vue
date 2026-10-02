<template>
  <span class="static-field-line">
    <UriLink
        v-if="label"
        :uri="value"
        :value="label"
        :to="to"
        :target="target"
    ></UriLink>
  </span>
</template>

<script setup lang="ts">
import { inject, ref, watch } from "vue";
import UriLink from "@/components/common/views/UriLink.vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import { FactorsService } from "opensilex-core/api/factors.service";

const props = withDefaults(
    defineProps<{
      value: string,
      experiment?: string,
      target?: string
    }>(),
    { experiment: null, target: "_self" }
)

const opensilex = inject<OpenSilexVuePlugin>("$opensilex")
const factorsService = opensilex.getService<FactorsService>("opensilex.FactorsService")

const label = ref<string>("")
const to = ref<{ path: string } | null>(null)

watch(
    () => props.value,
    (value) => {
      if (!value) {
        label.value = ""
        to.value = null
        return
      }

      opensilex.disableLoader()
      factorsService.getFactorLevelDetail(value)
          .then(http => {
            const factorLevel = http.response.result
            label.value = factorLevel.name + " (" + factorLevel.factor_name + ")"

            // The factor detail page only exists inside an experiment.
            to.value = props.experiment
                ? {
                  path: "/" + encodeURIComponent(props.experiment)
                      + "/factor/details/" + encodeURIComponent(factorLevel.factor)
                }
                : null
          })
          .catch(() => {
            label.value = value
            to.value = null
          })
          .finally(() => opensilex.enableLoader())
    },
    { immediate: true }
)

</script>

<style scoped lang="scss">
</style>
