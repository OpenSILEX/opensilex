<template>
  <opensilex-FormSelector
    :label="label"
    :required="required"
    v-model:selected="organizationsURI"
    :multiple="multiple"
    :options="organizationsOptions"
    :placeholder="t('OrganizationSelector.filter-placeholder')"
    filterable
    clearable
    @select="(v) => emit('select', v)"
    @deselect="(v) => emit('deselect', v)"
  />
</template>

<script setup lang="ts">
import { inject, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { OrganizationsService } from 'opensilex-core/api/organizations.service'
import type { OrganizationDagDTO } from 'opensilex-core/index'
import type HttpResponse from 'opensilex-core/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-core/HttpResponse'

const { t } = useI18n()

const props = withDefaults(
  defineProps<{
    organizations?: any
    label?: string
    multiple?: boolean
    excludeOrganizationURI?: string
    required?: boolean
  }>(),
  {
    organizations: () => [],
    multiple: false,
    required: false
  }
)

const emit = defineEmits<{
  (e: 'update:organizations', v: any): void
  (e: 'select', v: any): void
  (e: 'deselect', v: any): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = $opensilex.getService<OrganizationsService>('opensilex-core.OrganizationsService')

// v-model replacement of PropSync("organizations")
const organizationsURI = ref<any>(props.organizations)

watch(
  () => props.organizations,
  (v) => {
    organizationsURI.value = v
  },
  { deep: true }
)

watch(
  organizationsURI,
  (v) => emit('update:organizations', v),
  { deep: true }
)

const organizationsOptions = ref<any[]>([])

/**
 * Normalise un tree quelconque en format Naive UI TreeSelect:
 * { key, label, children }
 */
function toNaiveTree(nodes: any[]): any[] {
  return (nodes ?? [])
    .map((node) => {
      if (!node) return null

      const key = node.key ?? node.id ?? node.uri
      const label = node.label ?? node.name ?? ''

      const children = node.children
        ? toNaiveTree(node.children)
        : undefined

      return {
        ...node,
        key,
        label,
        children
      }
    })
    .filter(Boolean)
}

async function init() {
  try {
    const http = (await service.searchOrganizations(
      '.*',
      undefined,
      undefined,
      undefined,
      undefined
    )) as HttpResponse<OpenSilexResponse<Array<OrganizationDagDTO>>>

    let organizationGraph = http.response.result ?? []

    if (props.excludeOrganizationURI) {
      organizationGraph = organizationGraph.filter(
        (organization: any) =>
          organization?.uri !== props.excludeOrganizationURI
      )
    }

    const organizationTree = ($opensilex as any).buildTreeFromDag(organizationGraph)

    organizationsOptions.value = toNaiveTree(organizationTree)
  } catch (e) {
    $opensilex.errorHandler(e as any)
  }
}

function reset() {
  init()
}

onMounted(() => {
  init()
})

defineExpose({ reset })
</script>

<i18n>
en:
  OrganizationSelector:
    filter-placeholder: "Search organizations"
fr:
  OrganizationSelector:
    filter-placeholder: "Rechercher des organisations"
</i18n>
