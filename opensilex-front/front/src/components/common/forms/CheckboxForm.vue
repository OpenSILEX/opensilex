<template>
  <opensilex-FormField
    :rules="rules"
    :required="required"
    :label="label"
    :helpMessage="helpMessage"
  >
    <!-- le slot "field" fournit un id unique -->
    <template #field="{ id }">
      <label class="ox-checkbox" :for="id">
        <input
          :id="id"
          type="checkbox"
          :checked="booleanValue"
          @change="onChange"
          :disabled="disabled"
          :required="required"
        />
        <span class="ox-checkbox__label">
          {{ t(title) }}
          <i
            v-if="!label && title && helpMessage"
            class="bi bi-question-circle-fill text-secondary"
            :title="t(helpMessage)"
            tabindex="0"
            aria-hidden="true"
          />
        </span>
      </label>
    </template>
  </opensilex-FormField>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';

const props = defineProps<{
  value?: boolean;             // v-model:value
  label?: string;
  title?: string;              // clé i18n
  helpMessage?: string;        // clé i18n
  required?: boolean;
  disabled?: boolean;
  rules?: string | ((v:any)=>any);
}>();

const emit = defineEmits<{
  (e:'update:value', v:boolean): void;
}>();

const { t } = useI18n();

// pont v-model:value
const booleanValue = computed<boolean>({
  get: () => !!props.value,
  set: v => emit('update:value', v),
});

function onChange(e: Event) {
  const target = e.target as HTMLInputElement;
  booleanValue.value = target.checked;
}
</script>

<style scoped>
.ox-checkbox {
  display: inline-flex;
  align-items: center;
  gap: .5rem;
  cursor: pointer;
}
.ox-checkbox input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: #00A38D;
}

.ox-checkbox__label {
  display: inline-flex;
  align-items: center;
  gap: .4rem;
}
</style>
