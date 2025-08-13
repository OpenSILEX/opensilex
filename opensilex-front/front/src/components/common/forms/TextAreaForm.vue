<template>
  <opensilex-FormField
    :rules="rules"
    :required="required"
    :label="label"
    :helpMessage="helpMessage"
    :vid="vid"
  >
    <template #field="{ id }">
      <n-input
        v-bind="$attrs"
        v-model:value="stringValue"
        :id="id"
        type="textarea"
        :disabled="disabled"
        :required="required"
        :placeholder="placeholder"
        :autocomplete="autocomplete"
        @input="onInput"
        @change="onChange"
        @blur="onBlur"
        @keypress.enter="onEnter"
      />
    </template>
  </opensilex-FormField>
</template>

<script setup lang="ts">
import { ref, defineProps, defineEmits, watch} from 'vue'
import { useI18n } from 'vue-i18n'
import { NInput } from 'naive-ui'

const { t } = useI18n()

// Props
const props = defineProps<{
  value: string
  label?: string
  helpMessage?: string
  placeholder?: string
  required?: boolean
  disabled?: boolean
  rules?: string | Function
  autocomplete?: string
  vid?: string
}>()

// Emits
const emit = defineEmits<{
  (e: 'update:value', value: string): void
  (e: 'input', value: string): void
  (e: 'change', value: string): void
  (e: 'blur'): void
  (e: 'handlingEnterKey'): void
}>()

// Model
const stringValue = ref(props.value)
watch(() => props.value, v => stringValue.value = v)

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
</script>

<style scoped lang="scss">
</style>
