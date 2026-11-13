<template>
  <InfiniteScrollDropdown
    v-model:selected="projectsProxy"
    :fetchPage="searchProjects"
    :itemLoadingMethod="loadProjects"
    :conversionMethod="projectToSelectOption"
    :label="label"
    :multiple="multiple"
    :placeholder="t('component.project.selector-placeholder')"
    @selectionChange="(option) => emit('selectionChange', option)"
    @clear="emit('clear')"
  />
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import type { SelectOption } from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import { ProjectsService } from 'opensilex-core/index'
import type { ProjectGetDTO } from 'opensilex-core/index'
import type HttpResponse from 'opensilex-security/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'
import { useI18n } from 'vue-i18n'
import InfiniteScrollDropdown from '@/components/common/forms/InfiniteScrollDropdown.vue'

const props = withDefaults(defineProps<{
  projects?: string[] | string | null
  label?: string
  multiple?: boolean
}>(), {
  multiple: false,
  projects: null
})

const { t } = useI18n()

const emit = defineEmits<{
  (e: 'update:projects', v: string[] | string | null): void
  (e: 'selectionChange', option: SelectOption | SelectOption[] | undefined): void
  (e: 'clear'): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

/**
 * v-model proxy
 * - multiple: an array is expected
 * - otherwise: a single value (string | null)
 */
const projectsProxy = computed<any>({
  get: () => props.projects,
  set: (v) => emit('update:projects', v)
})

/** Loads one page of results. `page` is zero-based. */
function searchProjects(searchQuery: string, page: number, pageSize: number) {
  const service = $opensilex.getService<ProjectsService>('opensilex.ProjectsService')
  return service.searchProjects(
    searchQuery,     // name
    undefined,       // year
    undefined,       // keyword
    undefined,       // financial_funding
    ['name=asc'],    // order_by
    page,
    pageSize
  )
}

/** Loads the already selected elements (update form), so that their name can be displayed. */
function loadProjects(uris: string[]) {
  const service = $opensilex.getService<ProjectsService>('opensilex.ProjectsService')
  return service.getProjectsByURI(uris).then(
    (http: HttpResponse<OpenSilexResponse<Array<ProjectGetDTO>>>) => http.response.result
  )
}

const projectToSelectOption = (dto: ProjectGetDTO): SelectOption => ({
  label: dto.shortname || dto.name,
  value: dto.uri
})
</script>

<style scoped lang="scss">
</style>
