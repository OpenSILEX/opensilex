<template>
  <span>
    <label class="form-label" :for="labelFor">
      {{ t(label) }}
      &nbsp;
      <i
        v-if="helpMessage"
        class="bi bi-question-circle-fill text-secondary"
        tabindex="0"
        role="button"
        :data-bs-toggle="'tooltip'"
        :data-bs-placement="'top'"
        :title="t(helpMessage)"
        ref="tooltipIcon"
      />
    </label>
  </span>
</template>

<script lang="ts" setup>
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { Tooltip } from 'bootstrap';

// Props
const props = defineProps<{
  helpMessage?: string;
  label: string;
  labelFor?: string;
}>();

// i18n
const { t } = useI18n();

// Tooltip init
const tooltipIcon = ref<HTMLElement | null>(null);

onMounted(() => {
  if (tooltipIcon.value && props.helpMessage) {
    new Tooltip(tooltipIcon.value);
  }
});
</script>

<style scoped lang="scss">
.form-label {
  display: inline-block;
  min-width: 100%;
  margin-bottom: 5px;
  cursor: default;
}
</style>
