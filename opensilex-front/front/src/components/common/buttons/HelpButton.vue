<template>
  <opensilex-Button
    @click=" () => emit('click')"
    :label="label"
    class="helpButton"
    :variant="variant"
    :disabled="disabled"
    :small="small"
    icon="fa#question"
  />
</template>

<script setup lang="ts">
import { defineProps, defineEmits } from "vue";

defineProps<{
  label?: string;
  small?: boolean;
  disabled?: boolean;
  variant: { type: String, default: "outline-info" }
}>();

const emit = defineEmits(["click"]);

</script>

<style scoped lang="scss">
</style>
