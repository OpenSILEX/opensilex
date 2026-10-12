<template>
  <OVLButton
    @click="emit('click')"
    variant="outline-success"
    icon="fa#plus"
    :small="small"
    :label="label && t(label)"
    :disabled="disabled"
  />
</template>

<script setup lang="ts">
import { useI18n } from "vue-i18n";
import { OVLButton } from "opensilex-vue-library";

const { t } = useI18n();
const props = withDefaults(
  defineProps<{
    label?: string
    small?: boolean
    disabled?: boolean
  }>(),
  {
    label: undefined,
    small: false,
    disabled: false
  }
)

const emit = defineEmits<{
  (e: 'click'): void
}>()

const { label, small, disabled } = props
</script>

<style scoped lang="scss"></style>
