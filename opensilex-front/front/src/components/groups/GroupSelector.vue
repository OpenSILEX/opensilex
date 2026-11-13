<template>
  <InfiniteScrollDropdown
    v-model:selected="groupsURI"
    :fetchPage="searchGroups"
    :itemLoadingMethod="loadGroups"
    :conversionMethod="groupToSelectOption"
    :label="label"
    :multiple="multiple"
    :placeholder="t(placeholder)"
    :helpMessage="helpMessage"
    :disabled="disabled"
    @selectionChange="(option) => emit('selectionChange', option)"
    @clear="emit('clear')"
  />
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import { useI18n } from 'vue-i18n'
import type { SelectOption } from 'naive-ui'
import type { SecurityService, GroupDTO } from 'opensilex-security/index'
import type HttpResponse from 'opensilex-security/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'
import InfiniteScrollDropdown from "@/components/common/forms/InfiniteScrollDropdown.vue";

const { t } = useI18n()

const props = withDefaults(
  defineProps<{
    groups?: any // string | string[] depending on `multiple`
    label?: string
    placeholder?: string
    multiple?: boolean
    disabled?: boolean
    helpMessage?: string
  }>(),
  {
    groups: () => [],
    placeholder: 'component.group.filter-placeholder',
    multiple: false,
    disabled: false
  }
)

const emit = defineEmits<{
  (e: 'update:groups', v: any): void
  (e: 'selectionChange', option: SelectOption | SelectOption[] | undefined): void
  (e: 'clear'): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = $opensilex.getService<SecurityService>('opensilex.SecurityService')

// v-model proxy
const groupsURI = computed({
  get: () => props.groups,
  set: (v) => emit('update:groups', v)
})

/** Loads one page of results. `page` is zero-based. */
function searchGroups(searchQuery: string, page: number, pageSize: number) {
  return service.searchGroups(searchQuery, ['name=asc'], page, pageSize)
}

/** Loads the already selected elements (update form), so that their name can be displayed. */
async function loadGroups(groupsUris: string[]) {
  if (!groupsUris || groupsUris.length === 0) return undefined
  const http = await service.getGroupsByURI(groupsUris) as unknown as HttpResponse<
    OpenSilexResponse<GroupDTO[]>
  >
  return (http as any).response.result
}

const groupToSelectOption = (dto: GroupDTO): SelectOption => ({
  label: (dto as any).name,
  // shortUri needed to avoid auto deselection problem on selectors with both shorts and long URIs
  value: $opensilex.getShortUri((dto as any).uri)
})
</script>

<style scoped lang="scss"></style>
