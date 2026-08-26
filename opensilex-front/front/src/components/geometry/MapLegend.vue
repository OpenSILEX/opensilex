<template>
  <div class="map-legend">
    <span class="legend-title">{{ t('MapView.Legend') }}:</span>
    <NSpace :size="16">
      <NTag type="info" size="small">
        {{ t('MapView.LegendSO') }}
      </NTag>
      <NTag type="success" size="small">
        {{ t('MapView.LegendStructuralArea') }}
      </NTag>
      <NTag type="error" size="small">
        {{ t('MapView.LegendTemporalArea') }}
      </NTag>
    </NSpace>
  </div>
</template>

<script setup lang="ts">
import { NTag, NSpace } from 'naive-ui'
import { useI18n } from 'vue-i18n'

//#region Public
const props = withDefaults(
  defineProps<{
    showSO?: boolean
    showStructuralArea?: boolean
    showTemporalArea?: boolean
  }>(),
  {
    showSO: true,
    showStructuralArea: true,
    showTemporalArea: true,
  }
)

//#endregion

//#region Private
const { t } = useI18n()
//#endregion
</script>

<style scoped lang="scss">
.map-legend {
  position: absolute;
  bottom: 10px;
  left: 10px;
  z-index: 100;
  background: white;
  padding: 8px 12px;
  border-radius: 5px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
  font-size: 12px;

  .legend-title {
    font-weight: bold;
    margin-right: 8px;
  }
}
</style>