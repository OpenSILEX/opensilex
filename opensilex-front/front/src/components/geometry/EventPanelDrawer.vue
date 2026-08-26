<template>
  <div class="event-panel-drawer-content">
    <EventTimeline
      :experiment-uri="experimentUri"
      @select-feature="onSelectFeature"
    />
  </div>
</template>

<script setup lang="ts">
import EventTimeline from './EventTimeline.vue'

//#region Public
const props = withDefaults(
  defineProps<{
    experimentUri: string
  }>(),
  {}
)

const emit = defineEmits<{
  selectFeature: [uri: string]
}>()

//#endregion

//#region Private
function onSelectFeature(uri: string) {
  emit('selectFeature', uri)
}
//#endregion
</script>

<style scoped lang="scss">
.event-panel-drawer-content {
  padding: 10px;
}
</style>