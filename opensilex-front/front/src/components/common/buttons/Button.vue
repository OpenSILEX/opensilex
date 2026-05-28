<template>
  <button
    v-if="href"
    :title="label"
    :disabled="disabled"
    :href="href"
    class="btn btn-outline-primary"
    target="_blank"
  >
    <slot name="icon">
      <opensilex-Icon v-if="icon && icon.startsWith('fa#')" :icon="icon" />
      <i v-else-if="icon && icon.startsWith('bi-')" :class="['bi', icon]" />
    </slot>
    <span class="button-label" :title="tooltip" v-if="size === 'md'">{{ label }}</span>
  </button>

  <button
    v-else
    @click.prevent="emit('click')"
    :title="label"
    :class="['btn', variant ? `btn-${variant}` : '']"
    :disabled="disabled"
  >
    <slot name="icon">
      <opensilex-Icon v-if="icon && icon.startsWith('fa#')" :icon="icon" />
      <i v-else-if="icon && icon.startsWith('bi-')" :class="['bi', icon]" />
    </slot>
    <span class="button-label" :title="tooltip" v-if="size === 'md'">{{ label }}</span>
  </button>
</template>

<script setup lang="ts">
import { computed } from "vue"; 

const props = defineProps<{
  label?: string;
  variant?: string;
  small?: boolean;
  disabled?: boolean;
  icon?: string;
  helpMessage?: string;
  href?: string;
}>();

const emit = defineEmits<{
  (event: "click"): void;
}>();
 

const tooltip = computed(() =>
  !props.helpMessage || props.helpMessage.length === 0
    ? props.label ? props.label : undefined
    : props.helpMessage
);

const size = computed(() => (props.small ? "sm" : "md"));
</script>

<style scoped lang="scss">
.button-label {
  margin-left: 5px;
  padding-right: 13px;
}
</style>
