<template>
  <opensilex-FormSelector
    :label="label"
    v-model:selected="timeIntervalURI"
    :options="periodList"
    :placeholder="placeholder"
    @enterKey="onEnter"
  />
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'

// Props
const props = defineProps<{
  timeinterval: string
  label?: string
}>()

const emit = defineEmits<{
  (e: 'handlingEnterKey'): void
}>()

const { t, locale } = useI18n()

const timeIntervalURI = ref(props.timeinterval)
const periodList = ref<{ id: string; label: string }[]>([])

const loadTimeInterval = () => {
  const periods = ['millisecond', 'second', 'minute', 'hour', 'day', 'week', 'month', 'unique']
  periodList.value = periods.map(value => ({
    id: value.charAt(0).toUpperCase() + value.slice(1),
    label: t('component.variable.dimensionValues.' + value)
  }))
}

watch(locale, () => {
  loadTimeInterval()
})

onMounted(() => {
  loadTimeInterval()
})

const onEnter = () => {
  emit('handlingEnterKey')
}
</script>
