<template>
  <div class="feature-details-drawer">
    <div v-if="feature" class="details-content">
      <div class="feature-header">
        <h3 class="feature-name">{{ feature.properties?.name || 'Unknown' }}</h3>
        <NTag type="info" size="small">{{ feature.properties?.type || 'Unknown' }}</NTag>
        <NTag v-if="feature.properties?.nature" :type="natureTagType" size="small">{{ feature.properties?.nature }}</NTag>
      </div>

      <div class="details-section">
        <h4>Properties</h4>
        <div class="property-row" v-if="feature.properties?.uri">
          <span class="property-label">URI</span>
          <span class="property-value">{{ feature.properties.uri }}</span>
        </div>
        <div class="property-row" v-if="feature.properties?.rdf_type_name">
          <span class="property-label">Type Name</span>
          <span class="property-value">{{ feature.properties.rdf_type_name }}</span>
        </div>
        <div class="property-row" v-if="feature.properties?.creation_date">
          <span class="property-label">Creation Date</span>
          <span class="property-value">{{ formatDate(feature.properties.creation_date) }}</span>
        </div>
        <div class="property-row" v-if="feature.properties?.destruction_date">
          <span class="property-label">Destruction Date</span>
          <span class="property-value">{{ formatDate(feature.properties.destruction_date) }}</span>
        </div>
      </div>

      <div class="details-section" v-if="feature.geometry?.type">
        <h4>Geometry</h4>
        <div class="property-row">
          <span class="property-label">Type</span>
          <span class="property-value">{{ feature.geometry.type }}</span>
        </div>
        <div class="property-row" v-if="feature.geometry.type === 'Point' && feature.geometry.coordinates">
          <span class="property-label">Coordinates</span>
          <span class="property-value">{{ formatCoordinates(feature.geometry.coordinates as number[]) }}</span>
        </div>
      </div>

      <div class="details-actions">
        <NButton type="primary" size="small" @click="emit('close')">Close</NButton>
      </div>
    </div>

    <div v-else class="no-feature">
      <p>No feature selected</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { NTag, NButton } from 'naive-ui'

//#region Public
const props = withDefaults(
  defineProps<{
    feature: Feature | null
  }>(),
  {}
)

interface Feature {
  properties: {
    uri?: string
    name?: string
    type?: string
    nature?: string
    creation_date?: string
    destruction_date?: string
    rdf_type_name?: string
    [key: string]: unknown
  }
  geometry?: {
    coordinates?: unknown
    type?: string
  }
}

const emit = defineEmits<{
  close: []
}>()

//#endregion

//#region Private
const natureTagType = (nature: string) => {
  switch (nature) {
    case 'Structural':
      return 'success'
    case 'Temporal':
      return 'warning'
    case 'Device':
      return 'info'
    default:
      return 'default'
  }
}

function formatDate(dateString: string): string {
  if (!dateString) return ''
  try {
    const date = new Date(dateString)
    return date.toLocaleDateString()
  }
  catch {
    return dateString
  }
}

function formatCoordinates(coords: number[]): string {
  if (!coords || coords.length === 0) return ''
  return `Lng: ${coords[0]?.toFixed(6)}, Lat: ${coords[1]?.toFixed(6)}`
}
//#endregion
</script>

<style scoped lang="scss">
.feature-details-drawer {
  padding: 16px;
}

.details-content {
  .feature-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;
    flex-wrap: wrap;

    .feature-name {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      flex: 1;
    }
  }

  .details-section {
    margin-bottom: 16px;

    h4 {
      margin: 0 0 8px 0;
      font-size: 14px;
      font-weight: 600;
      color: #333;
    }

    .property-row {
      display: flex;
      justify-content: space-between;
      padding: 4px 0;
      border-bottom: 1px solid #f0f0f0;

      .property-label {
        font-weight: 500;
        color: #666;
        font-size: 13px;
      }

      .property-value {
        color: #333;
        font-size: 13px;
        text-align: right;
        max-width: 60%;
        word-break: break-word;
      }
    }
  }

  .details-actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}

.no-feature {
  text-align: center;
  color: #999;
  padding: 40px 0;
}
</style>