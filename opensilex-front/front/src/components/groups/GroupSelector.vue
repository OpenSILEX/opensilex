<template>
  <opensilex-FormSelector
    :label="label"
    v-model:selected="groupsURI"
    :multiple="multiple"
    :itemLoadingMethod="loadGroups"
    :searchMethod="searchGroups"
    :conversionMethod="groupToSelectNode"
    :placeholder="t(placeholder)"
    :noResultsText="noResultsText"
    :helpMessage="helpMessage"
    :disabled="disabled"
    @select="(v) => emit('select', v)"
    @deselect="(v) => emit('deselect', v)"
  />
</template>

<script setup lang="ts">
import { inject, ref, watch } from 'vue'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import { useI18n } from 'vue-i18n'
import type { SecurityService, GroupDTO } from 'opensilex-security/index'
import type HttpResponse from 'opensilex-security/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'

const { t } = useI18n()

const props = withDefaults(
  defineProps<{
    groups?: any // string | string[] selon multiple
    label?: string
    placeholder?: string
    noResultsText?: string
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
  (e: 'select', v: any): void
  (e: 'deselect', v: any): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = $opensilex.getService<SecurityService>('opensilex.SecurityService')

// v-model replacement of PropSync("groups")
const groupsURI = ref<any>(props.groups)

watch(
  () => props.groups,
  (v) => {
    groupsURI.value = v
  },
  { deep: true }
)

watch(
  groupsURI,
  (v) => emit('update:groups', v),
  { deep: true }
)

function searchGroups(searchQuery: string, page?: number, pageSize?: number) {
  // on garde  la signature searchMethod attendue par FormSelector
  return service.searchGroups(searchQuery, undefined, page, pageSize)
}

async function loadGroups(groupsUris: string[]) {
  if (!groupsUris || groupsUris.length === 0) return undefined
  const http = await service.getGroupsByURI(groupsUris) as unknown as HttpResponse<
    OpenSilexResponse<GroupDTO[]>
  >
  return (http as any).response.result
}

function groupToSelectNode(dto: GroupDTO) {
  if (!dto) return undefined
  return {
    label: (dto as any).name,
    // shortUri needed to avoid auto deselection problem on selectors with both shorts and long URIs
    id: $opensilex.getShortUri((dto as any).uri)
  }
}
</script>

<style scoped lang="scss"></style>
