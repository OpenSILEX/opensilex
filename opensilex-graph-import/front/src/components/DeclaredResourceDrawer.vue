<template>
  <n-drawer v-model:show="shown" :width="520" placement="right">
    <n-drawer-content :title="title" closable>
      <template v-if="resource">
        <p class="small text-body-secondary">{{ $t('GraphImport.declared.help') }}</p>
        <div v-for="property in fields" :key="property.uri" class="mb-3">
          <div class="d-flex align-items-center gap-2 mb-1">
            <span class="small fw-semibold">{{ property.label }}</span>
            <n-tag v-if="(property.min_values ?? 0) > 0" size="tiny" type="warning" :bordered="false">
              {{ $t('GraphImport.missing.required') }}
            </n-tag>
            <span class="small text-body-secondary">{{ property.range_label }}</span>
          </div>
          <opensilex-FactorCategorySelector
            v-if="property.range === FACTOR_CATEGORY"
            :category="values[property.uri!]?.[0] || undefined"
            label="GraphImport.declared.category"
            @update:category="(chosen: string | undefined) => onValueInput(property.uri!, 0, chosen ?? '')"
          />
          <div v-for="(value, index) in property.range === FACTOR_CATEGORY ? [] : values[property.uri!]" :key="index"
               class="d-flex gap-2 mb-1">
            <n-input
              :value="value"
              size="small"
              :placeholder="$t(property.kind === 'OBJECT' ? 'GraphImport.missing.linkPlaceholder' : 'GraphImport.declared.valuePlaceholder')"
              @update:value="(typed: string) => onValueInput(property.uri!, index, typed)"
            />
          </div>
          <n-button
            v-if="property.range !== FACTOR_CATEGORY && acceptsMore(property)"
            size="tiny"
            tertiary
            @click="onAddValueClick(property.uri!)"
          >
            {{ $t('GraphImport.declared.addValue') }}
          </n-button>
        </div>
        <p v-if="refusal" class="small text-danger">{{ refusal }}</p>
      </template>
      <template #footer>
        <n-space justify="space-between" class="w-100">
          <n-button type="error" tertiary :disabled="busy || !resource" @click="onForgetClick">
            {{ $t('GraphImport.declared.forget') }}
          </n-button>
          <n-space>
            <n-button :disabled="busy" @click="shown = false">{{ $t('GraphImport.understandGraph.cancel') }}</n-button>
            <n-button type="primary" :disabled="busy || !resource" @click="onSaveClick">{{ $t('GraphImport.declared.save') }}</n-button>
          </n-space>
        </n-space>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import type { DeclaredResourceDTO, InterpretationDTO, PropertyOptionDTO, SheetInterpretationDTO } from '../lib'
import type { OpenSilexPlugin } from '../opensilexPlugin'
import { declaredName, LABEL, PLATFORM_KEPT_PREFIX, valuesByProperty } from '../declared'

const EXPERIMENT = 'http://www.opensilex.org/vocabulary/oeso#Experiment'
/** Its values are subclasses of the category, chosen in the platform's list rather than typed. */
const FACTOR_CATEGORY = 'http://www.opensilex.org/vocabulary/oeso#FactorCategory'

//#region Public
const emit = defineEmits<{
  /** The resource as the user left it, to keep in place of the one declared. */
  save: [resource: DeclaredResourceDTO]
  /** Forget the resource: rows that pointed to it point to nothing again. */
  forget: [id: string]
}>()

const props = defineProps<{
  interpretation?: InterpretationDTO
  /** The resource edited; undefined while the drawer is closed. */
  resourceId?: string
  busy: boolean
}>()

/** Whether the drawer is open. */
const shown = defineModel<boolean>('show', { required: true })
//#endregion

//#region Private
//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
//#endregion

//#region Data & Computed
/** What the user typed, by property, one entry per value. */
const values = ref<Record<string, string[]>>({})

const resource = computed<DeclaredResourceDTO | undefined>(() =>
  (props.interpretation?.declared ?? []).find((declared) => declared.id === props.resourceId)
)

/** The sheet the assistant added for the resource's class, which knows what the class accepts. */
const sheet = computed<SheetInterpretationDTO | undefined>(() =>
  (props.interpretation?.sheets ?? []).find((candidate) => (candidate.declared_ids ?? []).includes(props.resourceId ?? ''))
)

/** The properties the user may fill: the required first, then by label; the platform's own left out. */
const fields = computed<PropertyOptionDTO[]>(() =>
  (sheet.value?.properties ?? [])
    .filter((property) => property.uri && !property.uri.startsWith(PLATFORM_KEPT_PREFIX))
    .sort((a, b) => Number((b.min_values ?? 0) > 0) - Number((a.min_values ?? 0) > 0))
)

const title = computed(() =>
  $opensilex.$i18n.t('GraphImport.declared.title', { class: sheet.value?.name ?? '', name: name.value })
)

const name = computed(() => values.value[LABEL]?.[0] ?? '')

/**
 * The experiment the declared resource belongs to by default: the one chosen for the import, else the one
 * declared in the assistant when there is exactly one, by its name. An experiment is declared first, then
 * its factor, then the factor's levels.
 */
const experimentOfImport = computed<string | undefined>(() => {
  if (props.interpretation?.experiment) {
    return props.interpretation.experiment
  }
  const experiments = (props.interpretation?.declared ?? []).filter((declared) => declared.class_uri === EXPERIMENT)
  return experiments.length === 1 ? declaredName(experiments[0]) || undefined : undefined
})

const refusal = computed(() => (name.value.trim() ? '' : $opensilex.$i18n.t('GraphImport.declared.nameRequired')))
//#endregion

//#region Hooks & watchers
watch([resource, fields], () => {
  const known = valuesByProperty(resource.value)
  const filled: Record<string, string[]> = {}
  fields.value.forEach((property) => {
    const uri = property.uri!
    const own = known[uri] ?? []
    const preset = property.range === EXPERIMENT && experimentOfImport.value ? [experimentOfImport.value] : ['']
    filled[uri] = own.length ? [...own] : preset
  })
  values.value = filled
}, { immediate: true })
//#endregion

//#region Event handlers
function onValueInput(property: string, index: number, typed: string) {
  const list = [...(values.value[property] ?? [])]
  list[index] = typed
  values.value = { ...values.value, [property]: list }
}

function onAddValueClick(property: string) {
  values.value = { ...values.value, [property]: [...(values.value[property] ?? []), ''] }
}

function onSaveClick() {
  if (!resource.value || refusal.value) {
    return
  }
  const kept = Object.entries(values.value).flatMap(([property, list]) =>
    list.map((value) => value.trim()).filter((value) => value).map((value) => ({ property, value }))
  )
  emit('save', { id: resource.value.id, class_uri: resource.value.class_uri, values: kept })
}

function onForgetClick() {
  if (resource.value?.id) {
    emit('forget', resource.value.id)
  }
}
//#endregion

//#region Methods
function acceptsMore(property: PropertyOptionDTO): boolean {
  const max = property.max_values
  return max === undefined || max === null || (values.value[property.uri!]?.length ?? 0) < max
}
//#endregion
//#endregion
</script>
