<template>
  <InfiteScrollDropdown
    :selected="selectedValue"
    :fetchPage="searchEntitiesPage"
    :placeholder="placeholder"
    :conversionMethod="mapEntityToOption"
    :resultLimit="PAGE_SIZE"
    @handlingEnterKey="emit('handlingEnterKey')"
  ></InfiteScrollDropdown>

</template>

<script setup lang="ts">
import { inject } from 'vue'
import type { SelectOption } from 'naive-ui'
import { VariablesService } from 'opensilex-core'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import InfiteScrollDropdown from "@/components/common/forms/InfiteScrollDropdown.vue";

const selectedValue = defineModel<string | null>('selected');

const props = defineProps<{
  placeholder?: string
}>()

const emit = defineEmits<{
  (e: 'handlingEnterKey'): void
}>()

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = opensilex.getService<VariablesService>('opensilex.VariablesService')

const PAGE_SIZE = 10

/**
 * Convertit une entité retournée par l’API en option compatible avec n-select.
 * Le label affiché privilégie name, puis label/title, puis l’URI en dernier recours.
 */
function mapEntityToOption(entity: any): SelectOption {
  return {
    label: entity.name ?? entity.label ?? entity.title ?? entity.uri,
    value: entity.uri
  }
}

/**
 * Charge une page d’entités depuis l’API.
 * pageIndex est indexé à partir de 0
 */
async function searchEntitiesPage(filter: string, pageIndex: number, pageSize: number) {
  const orderBy = ['name=asc']

  return await (service as any).searchEntities(
    filter,
    orderBy,
    pageIndex,
    pageSize
  )

}

</script>

<style scoped>

</style>