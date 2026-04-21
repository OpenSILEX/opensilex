<template>
  <opensilex-FormSelector
    ref="formSelector"
    :label="t('OperatorSelector.label')"
    :selected="operatorId"
    :multiple="false"
    :options="criteriaOperators"
    :clearable="false"
    :rules="rules"
    :required="required"
    :placeholder="t('OperatorSelector.placeholder')"
    @update:selected="operatorId = $event"
    @clear="emit('clear')"
    @select="select"
    @deselect="deselect"
  />
</template>

<script setup lang="ts">
import { computed, inject, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { DataService } from 'opensilex-core/api/data.service'
import type HttpResponse from 'opensilex-security/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'

interface SelectableItem {
  id: string
  label: string
}

const props = withDefaults(defineProps<{
  operator?: string
  rules?: string
  defaultSelectedValue?: string
  required?: boolean
}>(), {
  required: false
})

const emit = defineEmits<{
  (e: 'update:operator', value: string | undefined): void
  (e: 'clear'): void
  (e: 'select', value: any): void
  (e: 'deselect', value: any): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const { t } = useI18n()

const formSelector = ref<any>(null)
const criteriaOperators = ref<SelectableItem[]>([])

const operatorId = computed({
  get: () => props.operator,
  set: (value) => emit('update:operator', value)
})

async function searchOperators() {
  try {
    const service = $opensilex.getService<DataService>('opensilex.DataService')
    return await service.getMathematicalOperators()
  } catch (error) {
    $opensilex.errorHandler(error)
    return undefined
  }
}

function select(value: any) {
  emit('select', value)
}

function deselect(value: any) {
  emit('deselect', value)
}

onMounted(async () => {
  const http = await searchOperators()
  const result = http?.response?.result ?? []

  criteriaOperators.value = result.map((operator: string) => ({
    label: t(`OperatorSelector.${operator}`) as string,
    id: operator
  }))
})
</script>

<style scoped>
</style>

<i18n>
en:
  OperatorSelector:
    placeholder: Select an operator
    label: Operator
    LessThan: '<'
    LessOrEqualThan: '<='
    MoreThan: '>'
    MoreOrEqualThan: '>='
    EqualToo: =
    NotMeasured: Is not measured

fr:
  OperatorSelector:
    placeholder: Sélectionner un opérateur
    label: Opérateur
    LessThan: '<'
    LessOrEqualThan: '<='
    MoreThan: '>'
    MoreOrEqualThan: '>='
    EqualToo: =
    NotMeasured: N'est pas mesuré
</i18n>