<template>
  <OVLButton
    @click="$emit('click')"
    variant="outline-primary"
    :small="small"
    :label="t(label)"
    :disabled="disabled"
    icon="fa#pencil-alt"
  />
</template>

<script setup lang="ts">
import { useI18n } from "vue-i18n";
import OVLButton from "opensilex-vue-library/components/OVLButton.vue";

const { t } = useI18n();

const props = defineProps<{
  label: string;
  small?: boolean;
  disabled?: boolean;
}>();

const emit = defineEmits(['click']);
</script>
