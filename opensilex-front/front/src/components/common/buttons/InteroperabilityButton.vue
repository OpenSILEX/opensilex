<template>
  <opensilex-Button
    @click="$emit('click')"
    variant="outline-info"
    :small="small"
    :label="label"
    :disabled="disabled"
    icon="fa#globe-americas"
  />
</template>

<script setup lang="ts">
defineProps<{
  label: string;
  small: boolean;
  disabled: boolean;
}>()

defineEmits<{
  (e: 'click'): void;
}>()
</script>

<style scoped lang="scss">
</style>
