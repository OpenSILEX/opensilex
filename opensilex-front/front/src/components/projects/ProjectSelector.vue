<template>
  <div>
    <opensilex-FormSelector
      :label="label"
      v-model:selected="projectsProxy"
      :multiple="multiple"
      :itemLoadingMethod="loadProjects"
      :searchMethod="searchProjects"
      :conversionMethod="projectToSelectNode"
      :placeholder="t('component.project.selector-placeholder')"
      noResultsText="component.project.selector-search-no-result"
      @select="onSelect"
      @deselect="onDeselect"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import { ProjectsService } from 'opensilex-core/index'
import type { ProjectGetDTO } from 'opensilex-core/index'
import type HttpResponse from 'opensilex-security/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'
import { useI18n } from 'vue-i18n'

type SelectNode = { label: string; id: string }

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
  (e: 'select', v: any): void
  (e: 'deselect', v: any): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

/**
 * Proxy v-model pour opensilex-FormSelector
 * - si multiple: on veut un tableau
 * - sinon: une valeur unique (string | null)
 */
const projectsProxy = computed<any>({
  get: () => props.projects,
  set: (v) => emit('update:projects', v)
})

/** Service search */
function searchProjects(searchQuery: string, page: number, pageSize: number) {
  const service = $opensilex.getService<ProjectsService>('opensilex.ProjectsService')
  return service.searchProjects(
    searchQuery, // name
    undefined,   // year
    undefined,   // keyword
    undefined,   // financial
    undefined,   // orderBy
    page,
    pageSize
  )
}

/** Load selected items by URI(s) */
function loadProjects(projectsURI: any) {
  const service = $opensilex.getService<ProjectsService>('opensilex.ProjectsService')
  return service.getProjectsByURI(projectsURI).then(
    (http: HttpResponse<OpenSilexResponse<Array<ProjectGetDTO>>>) => http.response.result
  )
}

/** Convert DTO -> node for selector */
function projectToSelectNode(dto: ProjectGetDTO): SelectNode {
  return {
    label: dto.shortname || dto.name,
    id: dto.uri
  }
}

function onSelect(value: any) {
  emit('select', value)
}
function onDeselect(value: any) {
  emit('deselect', value)
}
</script>

<style scoped lang="scss">
</style>
