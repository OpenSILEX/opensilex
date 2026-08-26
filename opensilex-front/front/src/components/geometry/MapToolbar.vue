<template>
  <div class="map-toolbar" :class="{ collapsed }">
    <button
      class="collapse-toggle"
      :title="collapsed ? t('MapView.expand') : t('MapView.collapse')"
      @click="toggleCollapsed"
    >
      <span :class="collapseIconClass">▶</span>
    </button>
    <div class="toolbar-content" :class="{ collapsed }">
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
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
const collapsed = ref<boolean>(false)

function toggleCollapsed() {
  collapsed.value = !collapsed.value
}

const collapseIconClass = computed(() => `collapse-icon ${collapsed.value ? 'expanded' : 'collapsed'}`)
//#endregion
</script>

<style scoped lang="scss">
.map-toolbar {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 100;
  display: flex;
  align-items: flex-start;
  gap: 0;

  &.collapsed {
    .toolbar-content {
      opacity: 0;
      pointer-events: none;
      transform: translateX(10px);
    }
  }
}

.collapse-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 4px 0 0 4px;
  cursor: pointer;
  flex-shrink: 0;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
  transition: background-color 0.2s;

  &:hover {
    background-color: #f5f5f5;
  }

  .collapse-icon {
    font-size: 10px;
    transition: transform 0.3s ease;

    &.expanded {
      transform: rotate(180deg);
    }
  }
}

.toolbar-content {
  background: white;
  padding: 10px;
  border-radius: 0 5px 5px 0;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
  transition: opacity 0.3s ease, transform 0.3s ease;

  .help-icon {
    font-weight: bold;
    font-size: 14px;
  }

  .icon {
    font-size: 12px;
  }
}
</style>