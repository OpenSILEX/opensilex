<template>
  <div class="selected-features-table">
    <NDataTable
      :columns="columns"
      :data="tableData"
      :pagination="false"
      :bordered="false"
      size="small"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NDataTable } from 'naive-ui'
import { useI18n } from 'vue-i18n'

//#region Public
const props = withDefaults(
  defineProps<{
    features: Feature[]
    experimentUri: string
  }>(),
  {}
)

interface Feature {
  properties: {
    uri?: string
    name?: string
    type?: string
    nature?: string
    [key: string]: unknown
  }
  geometry?: {
    coordinates?: unknown
    type?: string
  }
}

const emit = defineEmits<{
  delete: [feature: Feature]
  edit: [feature: Feature]
  details: [feature: Feature]
}>()

//#endregion

//#region Private
const { t } = useI18n()

const columns = computed(() => [
  {
    title: 'Name',
    key: 'name',
    ellipsis: {
      tooltip: true,
    },
  },
  {
    title: 'Type',
    key: 'type',
    ellipsis: {
      tooltip: true,
    },
  },
  {
    title: 'Actions',
    key: 'actions',
    width: 200,
  },
])

const tableData = computed(() =>
  props.features.map((feature) => ({
    key: feature.properties?.uri || Math.random().toString(),
    name: feature.properties?.name || 'Unknown',
    type: feature.properties?.type || 'Unknown',
    uri: feature.properties?.uri,
    nature: feature.properties?.nature,
    actions: feature,
  }))
)

const $t = (key: string) => key
//#endregion

//#region methods
function customURIPath(feature: Feature) {
  switch (feature.properties?.nature) {
    case 'Areas':
      return `/area/details/${encodeURIComponent(feature.properties?.uri as string)}`
    case 'Devices':
      return `/device/details/${encodeURIComponent(feature.properties?.uri as string)}`
    default:
      return `/scientific-objects/details/${encodeURIComponent(feature.properties?.uri as string)}`
  }
}

//#endregion
</script>

<style scoped lang="scss">
.selected-features-table {
  position: absolute;
  bottom: 50px;
  left: 10px;
  right: 10px;
  z-index: 100;
  background: white;
  border-radius: 5px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  animation: slide-up 0.6s ease-out;
  max-height: 300px;
  overflow: auto;
}

@keyframes slide-up {
  0% {
    opacity: 0;
    transform: translateY(20px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>