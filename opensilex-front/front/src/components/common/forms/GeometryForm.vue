<template>
  <opensilex-FormField
    :rules="isMove ? 'containsPoint|wkt' : 'wkt'"
    :label="label"
    :helpMessage="helpMessage"
    :vid="vid"
    :required="isRequired"
  >
    <template #field="field">
      <n-input
        :id="field.id"
        :value="stringValue"
        @update:value="updateValue"
        :disabled="disabled"
        type="text"
        :placeholder="t(placeholder)"
      />
    </template>
  </opensilex-FormField>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { NInput } from 'naive-ui'
import { wktToGeoJSON, geojsonToWKT } from '@terraformer/wkt'

const { t } = useI18n()

const props = withDefaults(
  defineProps<{
    value?: any
    label?: string
    helpMessage?: string
    placeholder?: string
    required?: boolean
    disabled?: boolean
    isMove?: boolean
    vid?: any
  }>(),
  {
    required: false,
    disabled: false,
    isMove: false
  }
)

const emit = defineEmits<{
  (e: 'update:value', v: any): void
  (e: 'update:required', v: boolean): void
  (e: 'onUpdate'): void
}>()

const isRequired = computed(() => !!props.required)

// local string representation of WKT
const stringValue = ref('')

// keep stringValue in sync with incoming geojson
watch(
  () => props.value,
  (v) => {
    if (!v) {
      stringValue.value = ''
      return
    }
    try {
      stringValue.value = geojsonToWKT(v)
    } catch {
      stringValue.value = ''
    }
  },
  { immediate: true, deep: true }
)

function updateValue(newValue: string) {
  stringValue.value = newValue

  // parse WKT -> GeoJSON
  try {
    const geojson = newValue?.trim() ? wktToGeoJSON(newValue) : undefined
    emit('update:value', geojson)
  } catch {
    // si le WKT est invalide, on n’écrase pas la valeur par un parse
    // on laisse la validation (rules wkt) gérer l'erreur
    emit('update:value', undefined)
  }

  emit('onUpdate')
}
</script>

<style scoped lang="scss"></style>
