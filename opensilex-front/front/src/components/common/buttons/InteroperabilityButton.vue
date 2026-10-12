<template>
  <OVLButton
    @click="$emit('click')"
    variant="outline-info"
    :small="small"
    :label="t(label)"
    :disabled="disabled"
    icon="fa#globe-americas"
  />
</template>

<script setup lang="ts">
import { useI18n } from "vue-i18n";
import OVLButton from "opensilex-vue-library/components/OVLButton.vue";

const { t } = useI18n();

defineProps<{
  label: string;
  small?: boolean;
  disabled?: boolean;
}>();

defineEmits<{
  (e: 'click'): void;
}>();
</script>
