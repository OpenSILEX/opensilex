<template>
  <DataFilesView :withPageActions="false" :withFileNameFilter="false" :withImagesViewOption="false" :passedDeviceUri="uri" />
</template>

<script setup lang="ts">

import { useRoute } from 'vue-router'
import DataFilesView from "@/components/data/DataFilesView.vue";

const route = useRoute()
const uri = decodeURIComponent(route.params.uri as string)

</script>
