<template>
  <div class="static-field">
    <span class="field-view-title">{{ t(label) }}:</span>
    <span class="static-field-line">
      <span v-if="value">
        <Icon :icon="value"></Icon>
        &nbsp;({{ value }})
      </span>
      <br v-else />
    </span>
  </div>
</template>

<script setup lang="ts">
import {useI18n} from "vue-i18n";
import Icon from "@/components/common/views/Icon.vue";

var { t } = useI18n();

defineProps<{
  label: string;
  value?: string;
}>();
</script>

<style scoped lang="scss">
</style>