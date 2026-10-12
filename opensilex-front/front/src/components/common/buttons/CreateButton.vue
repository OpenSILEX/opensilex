<template>
  <OVLButton
    @click="$emit('click')"
    class="greenThemeColor"
    icon="bi-plus-lg"
    :small="small"
    :label="t(label)"
    :disabled="disabled"
  />
</template>

<script setup lang="ts">

import { useI18n } from "vue-i18n";
import { OVLButton } from "opensilex-vue-library";

const { t } = useI18n();

const props = defineProps<{
  label: string;
  small?: boolean;
  disabled?: boolean;
}>();

const emit = defineEmits<{
  (event: 'click'): void;
}>();
</script>

<style scoped lang="scss">
</style>
