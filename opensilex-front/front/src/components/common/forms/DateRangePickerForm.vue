<template>
  <div class="row">
    <!-- Start Date -->
    <div class="col-lg-6">
      <opensilex-DateForm
        :helpMessage="helpMessageStart"
        :label="labelStart"
        :max-date="endValue ?? undefined"
        :placeholder="placeholderStart"
        :required="requiredStart"
        :disabled="disabled"
        v-model:value="startValue"
      />
    </div>

    <!-- End Date -->
    <div class="col-lg-6">
      <opensilex-DateForm
        :helpMessage="helpMessageEnd"
        :label="labelEnd"
        :min-date="startValue ?? undefined"
        :placeholder="placeholderEnd"
        :required="requiredEnd"
        :disabled="disabled"
        v-model:value="endValue"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  labelStart?: string
  labelEnd?: string
  helpMessageStart?: string
  helpMessageEnd?: string
  placeholderStart?: string
  placeholderEnd?: string
  requiredStart?: boolean
  requiredEnd?: boolean
  disabled?: boolean
  start?: string | undefined
  end?: string | undefined
}>(), {
  requiredStart: false,
  requiredEnd: false,
  disabled: false
})

const emit = defineEmits<{
  (e: 'update:start', v: string | undefined): void
  (e: 'update:end', v: string | undefined): void
}>()

function normalizeDateInput(v: unknown): string | undefined {
  // Convertit '' / null en undefined (évite Naive "Invalid time value")
  if (v === '' || v === null || v === undefined) return undefined
  const s = String(v).trim()
  return s.length ? s : undefined
}

const startValue = computed<string | undefined>({
  get: () => normalizeDateInput(props.start),
  set: (v) => emit('update:start', normalizeDateInput(v))
})

const endValue = computed<string | undefined>({
  get: () => normalizeDateInput(props.end),
  set: (v) => emit('update:end', normalizeDateInput(v))
})
</script>