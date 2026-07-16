<template>
  <FormField
    :rules="rules"
    :required="isRequired"
    :label="label"
    :helpMessage="helpMessage"
  >
    <template #field="{ id }">
      <n-input
        :id="id"
        ref="inputRef"
        v-model:value="stringValue"
        :type="type"
        :disabled="disabled"
        :required="isRequired"
        :placeholder="placeholder"
        :autocomplete="autocomplete"
        @input="onInput"
        @change="onChange"
        @blur="onBlur"
        @keyup.enter="onEnter"
      />
    </template>
  </FormField>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { NInput } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import FormField from "@/components/common/forms/FormField.vue";

const { t } = useI18n()

// Props
const props = defineProps<{
  value: string | null
  type?: 'text' | 'password' | 'textarea' | 'email' | 'number' | 'date'
  label?: string
  helpMessage?: string
  placeholder?: string
  required?: boolean
  disabled?: boolean
  rules?: string | Function
  autocomplete?: string
}>()

// Emits
const emit = defineEmits<{
  (e: 'update:value', value: string | null): void
  (e: 'input', value: string): void
  (e: 'change', value: string): void
  (e: 'blur'): void
  (e: 'handlingEnterKey'): void
}>()

// Refs
const inputRef = ref<HTMLInputElement | null>(null)
const stringValue = ref(props.value ?? '')

// Sync with external value
watch(
  () => props.value,
  (newVal) => {
    stringValue.value = newVal ?? ''
  }
)

// Handlers
function onInput(value: string) {
  emit('update:value', value)
  emit('input', value)
}

function onChange(value: string) {
  emit('change', value)
}

function onBlur() {
  emit('blur')
}

function onEnter() {
  emit('handlingEnterKey')
}

// Defaults
const isRequired = props.required ?? false
const type = props.type ?? 'text'
</script>


<style scoped lang="scss">
// Remove arrows on number input
input::-webkit-outer-spin-button,
input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

input[type='number'] {
  appearance: textfield;
  -moz-appearance: textfield; /* Firefox */
}
</style>
