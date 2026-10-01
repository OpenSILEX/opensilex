<template>
  <div v-if="event" class="card-body">
    <br>

    <p class="h5">
      {{ t('component.experiment.moveView.location') }}
    </p>
    <hr>

    <StringView
      :label="t('MoveView.from')"
      :value="fromLabel"
    />

    <StringView
      :label="t('component.experiment.moveView.to')"
      :value="toLabel"
    />

    <div v-if="hasPosition">
      <br>

      <p class="h5">
        {{ t('component.experiment.moveView.positionTitle') }}
      </p>
      <hr>

      <PositionView
        :positionObject="positionObjectFromLocation"
        :targetUris="event.targets"
        :targetLabelsByUri="targetLabelsByUri"
        :targetUriPathsByUri="targetUriPathsByUri"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

import type { MoveDetailsDTO } from 'opensilex-core/index'
import type { PositionFormObject } from '../../positions/view/PositionView.vue'
import StringView from "@/components/common/views/StringView.vue";
import PositionView from "@/components/positions/list/view/PositionView.vue";

const props = withDefaults(defineProps<{
  event?: MoveDetailsDTO
  targetLabelsByUri?: Record<string, string>
  targetUriPathsByUri?: Record<string, string>
  facilitiesUriLabels?: Record<string, string>
}>(), {
  event: undefined,
  targetLabelsByUri: () => ({}),
  targetUriPathsByUri: () => ({}),
  facilitiesUriLabels: () => ({})
})

const { t } = useI18n()

const fromLabel = computed(() => {
  const from = props.event?.location?.from
  return from ? props.facilitiesUriLabels[from] ?? from : ''
})

const toLabel = computed(() => {
  const to = props.event?.location?.to
  return to ? props.facilitiesUriLabels[to] ?? to : ''
})

const hasPosition = computed(() => {
  const location = props.event?.location

  if (!location) {
    return false
  }
  //   n'affiche le bloc Position que si location est vide
  return Boolean(
    location.geojson ||
    location.x ||
    location.y ||
    location.z ||
    location.text
  )
})

const positionObjectFromLocation = computed<PositionFormObject>(() => {
  const location = props.event?.location ?? {}

  return {
    point: location.geojson,
    x: location.x,
    y: location.y,
    z: location.z,
    text: location.text
  }
})
</script>