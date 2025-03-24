<template>
  <button
    v-if="href"
    :title="t(label)"
    :disabled="disabled"
    :href="href"
    class="btn btn-outline-primary"
    target="_blank"
  >
    <slot name="icon">
      <opensilex-Icon v-if="icon && icon.startsWith('fa#')" :icon="icon" />
      <i v-else-if="icon && icon.startsWith('bi-')" :class="['bi', icon]" />
    </slot>
    <span class="button-label" :title="tooltip" v-if="size === 'md'">{{ t(label) }}</span>
  </button>

  <button
    v-else
    @click.prevent="emit('click')"
    :title="t(label)"
    :class="['btn', variant ? `btn-${variant}` : '']"
    :disabled="disabled"
  >
    <slot name="icon">
      <opensilex-Icon v-if="icon && icon.startsWith('fa#')" :icon="icon" />
      <i v-else-if="icon && icon.startsWith('bi-')" :class="['bi', icon]" />
    </slot>
    <span class="button-label" :title="tooltip" v-if="size === 'md'">{{ t(label) }}</span>
  </button>
</template>

<script setup lang="ts">
import { defineProps, defineEmits, computed } from "vue";
import { useI18n } from "vue-i18n";

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

const { t } = useI18n();

const tooltip = computed(() =>
  !props.helpMessage || props.helpMessage.length === 0
    ? props.label ? t(props.label) : undefined
    : t(props.helpMessage)
);

const size = computed(() => (props.small ? "sm" : "md"));
</script>

<style scoped lang="scss">
.button-label {
  margin-left: 5px;
  padding-right: 13px;
}
</style>
