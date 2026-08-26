<template>
  <div class="event-timeline">
    <NTimeline v-if="events.length > 0">
      <NTimelineItem
        v-for="event in events"
        :key="event.uri"
        :title="event.name"
        :time="formatDate(event.start)"
        @click="onSelect(event)"
      >
        <template #dot>
          <span class="event-dot"></span>
        </template>
      </NTimelineItem>
    </NTimeline>

    <div v-else class="no-events">
      No temporal areas displayed on the map.
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { NTimeline, NTimelineItem } from 'naive-ui'

//#region Public
const props = withDefaults(
  defineProps<{
    experimentUri: string
  }>(),
  {}
)

interface TimelineEvent {
  uri: string
  name: string
  start: string | Date
}

const emit = defineEmits<{
  selectFeature: [uri: string]
}>()

//#endregion

//#region Private
const events = ref<TimelineEvent[]>([])

onMounted(() => {
  loadEvents()
})

function loadEvents() {
  events.value = []
}

function formatDate(date: string | Date) {
  if (!date) {
    return ''
  }

  const d = new Date(date)
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const year = d.getFullYear()

  return `${year}-${month}-${day}`
}

function onSelect(event: TimelineEvent) {
  emit('selectFeature', event.uri)
}
//#endregion
</script>

<style scoped lang="scss">
.event-timeline {
  max-height: 500px;
  overflow: auto;

  .event-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background-color: #00a38d;
  }

  .no-events {
    text-align: center;
    color: #999;
    padding: 20px;
    font-size: 12px;
  }
}
</style>