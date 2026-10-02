<template>
  <Button
    @click="$emit('click')"
    class="greenThemeColor"
    icon="bi-plus-lg"
    :small="small"
    :label="label"
    :disabled="disabled"
  />
</template>

<script setup lang="ts">

import Button from "@/components/common/buttons/Button.vue";

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
