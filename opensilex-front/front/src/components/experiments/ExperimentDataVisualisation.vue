<template>
  <ExperimentDataVisualisationView
      :soFilter="soFilter"
      :elementName="elementName"
  ></ExperimentDataVisualisationView>
</template>

<script setup lang="ts">
import {computed, ComputedRef} from "vue";
import ExperimentDataVisualisationView from "@/components/experiments/views/ExperimentDataVisualisationView.vue";
import {ScientificObjectFilter} from "@/components/scientificObjects/ScientificObjectList.vue";

/**
 * "Visualization" tab of an experiment, see ExperimentDataVisualisationView.
 */

//#region Public
const props = defineProps<{
  /** URI of the experiment */
  uri: string
  /** Name of the experiment, written in the title of the downloaded image */
  elementName?: string
}>();
//#endregion

//#region Private
//#region Data & Computed
/** Initial filter of the scientific objects selection modal: the scientific objects of the experiment */
const soFilter: ComputedRef<ScientificObjectFilter> = computed<ScientificObjectFilter>(() => ({
  name: "",
  experiment: props.uri,
  germplasm: undefined,
  factorLevels: [],
  types: [],
  existenceDate: undefined,
  creationDate: undefined
}));
//#endregion
//#endregion
</script>
