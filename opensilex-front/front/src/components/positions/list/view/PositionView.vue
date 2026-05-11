<template>
  <div>
    <opensilex-UriListView
      :label="t('PositionView.targets')"
      :list="uriLinkDescriptions"
    />

    <opensilex-GeometryView
      label="component.common.geometry.geometry-title"
      :value="positionObject?.point"
    />

    <opensilex-StringView
      label="component.common.geometry.x"
      :value="positionObject?.x"
    />

    <opensilex-StringView
      label="component.common.geometry.y"
      :value="positionObject?.y"
    />

    <opensilex-StringView
      label="component.common.geometry.z"
      :value="positionObject?.z"
    />

    <opensilex-TextView
      v-if="positionObject"
      label="component.common.geometry.textual-position"
      :value="positionObject.text"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { GeoJsonObject } from 'opensilex-core/model/geoJsonObject'

export interface PositionFormObject {
  point?: GeoJsonObject
  x?: string
  y?: string
  z?: string
  text?: string
}

const { t } = useI18n();

const props = withDefaults(defineProps<{
  positionObject?: PositionFormObject
  targetUris?: string[]
  targetLabelsByUri?: Record<string, string>
  targetUriPathsByUri?: Record<string, string>
}>(), {
  positionObject: undefined,
  targetUris: () => [],
  targetLabelsByUri: () => ({}),
  targetUriPathsByUri: () => ({})
})

const uriLinkDescriptions = computed(() => {
  return props.targetUris.map((targetUri) => ({
    uri: targetUri,
    value: props.targetLabelsByUri[targetUri] ?? targetUri,
    to: props.targetUriPathsByUri[targetUri]
  }))
})
</script>

<i18n>
en:
  PositionView:
    targets: Targets

fr:
  PositionView:
    targets: Concerne
</i18n>