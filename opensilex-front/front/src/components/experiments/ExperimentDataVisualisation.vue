<template>
  <div>
    <div>
      <ExperimentDataVisualisationView
        :soFilter="soFilter" 
        @graphicCreated="onGraphicCreated"
        :selectedScientificObjects="selectedScientificObjects"
        :elementName="elementName"
      ></ExperimentDataVisualisationView> 
    </div>     
  </div>
</template>

<script setup lang="ts">
import {computed, inject, ref} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useRoute} from "vue-router";

defineProps<{
  elementName?: string
}>()

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const route = useRoute()
const selectedObjects = ref<string[]>([]);
const showDataVisuView = ref(false);
const soService = ref<any>()
const varService = ref<any>()
const namedObjectsArray = ref<any>({});

 const soFilter = ref({
    name: "",
    experiment: decodeURIComponent(route.params.uri as string),
    germplasm: undefined,
    factorLevels: [],
    types: [],
    existenceDate: undefined,
    creationDate: undefined,
  });

  const selectedScientificObjects = computed(() => {
    showDataVisuView.value = true;
    return selectedObjects.value.map(objectUri => {
      return {
        uri: objectUri,
        name: namedObjectsArray.value[objectUri]
      }
    });
  })

soService.value = opensilex.getService(
    "opensilex.ScientificObjectsService"
);

varService.value = opensilex.getService(
    "opensilex.VariablesService"
);

  function onGraphicCreated() {
    let that = this;
    setTimeout(function() {
      that.page.scrollIntoView({
        behavior: "smooth",
        block: "end",
        inline: "nearest"
      });
    }, 500);
  }

</script>

<style scoped lang="scss">
.selection-box {
  margin-top: 1px;
  margin-left: 24px;
}

.async-tree-action {
  font-style: italic;
}

.async-tree-action a:hover {
  text-decoration: underline;
  cursor: pointer;
}

.card-header {
  padding-top: 0 !important;
  padding-left: 0 !important;
  padding-right: 0 !important;
}

.card-header .badge {
  margin-left: 5px;
}

.scientificObjectsCards {
  display:contents
}
</style>
