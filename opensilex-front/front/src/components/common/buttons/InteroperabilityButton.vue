<template>
  <Button
    @click="$emit('click')"
    variant="outline-info"
    :small="small"
    :label="label"
    :disabled="disabled"
    icon="fa#globe-americas"
  />
</template>

<script setup lang="ts">
import Button from "@/components/common/buttons/Button.vue";

defineProps<{
  label: string;
  small?: boolean;
  disabled?: boolean;
}>();

defineEmits<{
  (e: 'click'): void;
}>();
</script>
