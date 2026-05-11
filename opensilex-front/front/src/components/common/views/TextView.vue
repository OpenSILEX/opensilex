<template>
  <div class="static-field">
    <span v-if="label" class="field-view-title">{{ t(label) }}</span>
    <span class="static-field-text">{{ displayValue }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  label?: string
  value?: string | number | null
}>()

const { t } = useI18n()

const displayValue = computed(() => props.value ?? '')
</script>