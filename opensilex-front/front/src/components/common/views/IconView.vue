<template>
  <div class="static-field">
    <span class="field-view-title">{{ t(label) }}:</span>
    <span class="static-field-line">
      <span v-if="value">
        <OVLIcon :icon="value"></OVLIcon>
        &nbsp;({{ value }})
      </span>
      <br v-else />
    </span>
  </div>
</template>

<script setup lang="ts">
import {useI18n} from "vue-i18n";
import { OVLIcon } from "opensilex-vue-library";

var { t } = useI18n();

defineProps<{
  label: string;
  value?: string;
}>();
</script>

<style scoped lang="scss">
</style>