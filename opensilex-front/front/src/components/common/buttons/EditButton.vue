<template>
  <opensilex-Button
    @click="$emit('click')"
    variant="outline-primary"
    :small="small"
    :label="label"
    :disabled="disabled"
    icon="fa#pencil-alt"
  />
</template>

<script setup lang="ts">
const props = defineProps<{
  label: string;
  small?: boolean;
  disabled?: boolean;
}>();

const emit = defineEmits(['click']);
</script>
