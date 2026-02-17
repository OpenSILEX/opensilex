<template>
  <opensilex-Button
    @click="emit('click')"
    variant="outline-success"
    icon="fa#plus"
    :small="small"
    :label="label"
    :disabled="disabled"
  />
</template>

<script setup lang="ts">
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
