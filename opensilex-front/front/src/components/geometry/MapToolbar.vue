<template>
  <div class="map-toolbar">
    <NSpace vertical :size="8">
      <NButton
        v-if="showHelp"
        quaternary
        circle
        size="small"
        @click="emit('toggleHelp')"
        title="Help"
      >
        <template #icon>
          <span class="help-icon">?</span>
        </template>
      </NButton>

      <NButton
        v-if="canCreateArea"
        type="primary"
        size="small"
        @click="emit('toggle-editing-mode')"
      >
        <template #icon>
          <span class="icon">+</span>
        </template>
        {{ t('MapView.add-area-button') }}
      </NButton>

      <NButton size="small" @click="emit('toggle-map-panel')">
        {{ t('MapView.mapPanel') }}
      </NButton>

      <NButton size="small" @click="emit('center-map')">
        <template #icon>
          <span class="icon">◎</span>
        </template>
        {{ t('MapView.center') }}
      </NButton>

      <NButton size="small" @click="emit('print-map')">
        <template #icon>
          <span class="icon">🖨</span>
        </template>
        {{ t('MapView.print') }}
      </NButton>

      <NButton size="small" @click="emit('export-map')">
        <template #icon>
          <span class="icon">⬇</span>
        </template>
        {{ t('MapView.export') }}
      </NButton>

      <NButton size="small" @click="emit('toggle-event-panel')">
        <template #icon>
          <span class="icon">⏱</span>
        </template>
        {{ t('MapView.time') }}
      </NButton>

      <NButton
        size="small"
        :disabled="selectedFeaturesCount === 0 || selectedFeaturesCount > 15"
        @click="emit('show-chart')"
      >
        <template #icon>
          <span class="icon">📊</span>
        </template>
        {{ t('MapView.chart') }}
      </NButton>
    </NSpace>
  </div>
</template>

<script setup lang="ts">
import { NButton, NSpace } from 'naive-ui'
import { useI18n } from 'vue-i18n'

//#region Public
const props = withDefaults(
  defineProps<{
    canCreateArea?: boolean
    selectedFeaturesCount?: number
    showHelp?: boolean
  }>(),
  {
    canCreateArea: true,
    selectedFeaturesCount: 0,
    showHelp: false,
  }
)

const emit = defineEmits<{
  'toggle-map-panel': []
  'toggle-event-panel': []
  'center-map': []
  'print-map': []
  'export-map': []
  'show-chart': []
  'toggle-editing-mode': []
  'toggleHelp': []
}>()

//#endregion

//#region Private
const { t } = useI18n()
//#endregion
</script>

<style scoped lang="scss">
.map-toolbar {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 100;
  background: white;
  padding: 10px;
  border-radius: 5px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);

  .help-icon {
    font-weight: bold;
    font-size: 14px;
  }

  .icon {
    font-size: 12px;
  }
}
</style>