<template>
  <DataFilesView :withPageActions="false" :withFileNameFilter="false" :withImagesViewOption="false" :enforcedScientificObjectUri="uri" />
</template>

<script setup lang="ts">

import { onMounted, ref } from "vue";
import {useRoute} from "vue-router";
import DataFilesView from "@/components/data/DataFilesView.vue";

//#region Constants & Services
const $route = useRoute();
//#endregion

//#region Reactive Data
const uri = ref<string>(null);
//#endregion

//#region Hooks
onMounted(() => {
  uri.value = decodeURIComponent($route.params.uri as string);
});
//#endregion

</script>
