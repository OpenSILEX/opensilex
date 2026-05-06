<template>
  <div class="static-field">
    <span class="field-view-title">{{ t(label) }}</span>
    <span class="static-field-text">{{ value }}</span>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';

const props = defineProps<{
  label: string;
  value?: string;
}>();

const { t } = useI18n();
</script>

<style scoped lang="scss">
</style>
