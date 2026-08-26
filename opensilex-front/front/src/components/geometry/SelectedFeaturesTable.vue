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
import { computed, h } from 'vue'
import { NDataTable, NButton, NTooltip } from 'naive-ui'

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
  delete: [feature: Feature]
  edit: [feature: Feature]
  details: [feature: Feature]
}>()

//#endregion

//#region Private
const iconEye = 'M12,9A3,3 0 0,0 9,12A3,3 0 0,0 12,15A3,3 0 0,0 15,12A3,3 0 0,0 12,9M12,17A5,5 0 0,1 7,12A5,5 0 0,1 12,7A5,5 0 0,1 17,12A5,5 0 0,1 12,17M12,4A16,16 0 0,0 6,12A16,16 0 0,0 12,20A16,16 0 0,0 18,12A16,16 0 0,0 12,4Z'
const iconEdit = 'M20.71,7.04C21.1,6.65 21.1,6 20.71,5.63L18.37,3.29C18,2.9 17.35,2.9 16.96,3.29L15.12,5.12L18.87,8.87M3,17.25V21H6.75L17.81,9.93L14.06,6.18L3,17.25Z'
const iconDelete = 'M19,4H15.5L14.5,3H9.5L8.5,4H5V6H19M6,19A2,2 0 0,0 8,21H16A2,2 0 0,0 18,19V7H6V19Z'

const columns = [
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
    width: 120,
    render(row: any) {
      const feature = row.actions
      return h('div', { class: 'action-buttons' }, [
        h(NTooltip, { trigger: 'hover' }, {
          default: () => 'Details',
          trigger: () => h(NButton, {
            quaternary: true,
            size: 'small',
            circle: true,
            onClick: () => emit('details', feature),
          }, {
            icon: () => h('svg', { width: '14', height: '14', viewBox: '0 0 24 24' }, [
              h('path', { fill: 'currentColor', d: iconEye })
            ])
          })
        }),
        h(NTooltip, { trigger: 'hover' }, {
          default: () => 'Edit',
          trigger: () => h(NButton, {
            quaternary: true,
            size: 'small',
            circle: true,
            onClick: () => emit('edit', feature),
          }, {
            icon: () => h('svg', { width: '14', height: '14', viewBox: '0 0 24 24' }, [
              h('path', { fill: 'currentColor', d: iconEdit })
            ])
          })
        }),
        h(NTooltip, { trigger: 'hover' }, {
          default: () => 'Delete',
          trigger: () => h(NButton, {
            quaternary: true,
            size: 'small',
            circle: true,
            type: 'error',
            onClick: () => emit('delete', feature),
          }, {
            icon: () => h('svg', { width: '14', height: '14', viewBox: '0 0 24 24' }, [
              h('path', { fill: 'currentColor', d: iconDelete })
            ])
          })
        })
      ])
    },
  },
]

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

.action-buttons {
  display: flex;
  gap: 4px;
  justify-content: center;
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