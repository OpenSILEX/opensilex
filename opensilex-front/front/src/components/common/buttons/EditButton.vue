<template>
  <Button
    @click="$emit('click')"
    variant="outline-primary"
    :small="small"
    :label="label"
    :disabled="disabled"
    icon="fa#pencil-alt"
  />
</template>

<script setup lang="ts">
import Button from "@/components/common/buttons/Button.vue";

const props = defineProps<{
  label: string;
  small?: boolean;
  disabled?: boolean;
}>();

const emit = defineEmits(['click']);
</script>
