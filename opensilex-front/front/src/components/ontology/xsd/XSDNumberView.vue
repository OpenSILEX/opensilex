<template>
  <span class="static-field-line">{{ formattedValue }}</span>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";

const props = defineProps<{
  value: string | number
}>()

const { n } = useI18n()

const formattedValue = computed(() => {
  const parsed = Number(props.value)
  // Keep the raw value visible rather than "NaN" when the property does not hold a number.
  return Number.isNaN(parsed) ? props.value : n(parsed, "decimal")
})

</script>

<style scoped lang="scss">
</style>
