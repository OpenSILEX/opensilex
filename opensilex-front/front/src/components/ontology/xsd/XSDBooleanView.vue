<template>
  <span class="static-field-line">
    {{ value === "true" || value === true ? t("component.common.yes") : t("component.common.no") }}
  </span>
</template>

<script setup lang="ts">
import { useI18n } from "vue-i18n";

defineProps<{
  value: string | boolean
}>()

const { t } = useI18n()

</script>

<style scoped lang="scss">
</style>
