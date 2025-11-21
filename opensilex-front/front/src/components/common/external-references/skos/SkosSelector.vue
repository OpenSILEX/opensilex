<template>
  <n-dropdown
    class="v-step-skos-selector"
    trigger="click"
    :options="dropdownOptions"
    :placement="right ? 'bottom-end' : 'bottom-start'"
    @select="onSelect"
  >
    <n-button quaternary size="small" class="greenThemeColor">
      {{ dropdownLabel }}
    </n-button>
  </n-dropdown>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { NDropdown, NButton, NTooltip } from 'naive-ui'
import type { DropdownOption } from 'naive-ui'
import SUPPORTED_SKOS_RELATIONS from './../../../../models/SkosRelations'

type SkosRelationDefinition = {
  dtoKey: string
  label: string
  description: string
}

// ---- Props / Emits
const props = withDefaults(defineProps<{
  labelAsCurrentSelectedRelation?: boolean
  right?: boolean
  selectedRelation?: string
}>(), {
  labelAsCurrentSelectedRelation: false,
  right: false,
  selectedRelation: undefined
})

const emit = defineEmits<{
  (e: 'update:selectedRelation', v?: string): void
}>()

const { t } = useI18n()

const relations = computed<SkosRelationDefinition[]>(() =>
  Array.from(SUPPORTED_SKOS_RELATIONS as Set<SkosRelationDefinition>)
)

// Utility: translate if it's a key, otherwise return as-is
function maybeT(keyOrText: string): string {
  // If the key exists in i18n, t() will return a different string;
  // if not, return the key itself.
  return String(t(keyOrText))
}

// Options dropdown (with tooltip per option)
const dropdownOptions = computed<DropdownOption[]> (() =>
  relations.value.map(rel => ({
      key: rel.dtoKey,
      label: () =>
        h(
            NTooltip,
            { placement: 'right', trigger: 'hover' },
            {
            default: () => maybeT(rel.description), // texte du tooltip
            trigger: () => h('span', maybeT(rel.label)) // texte visible dans le menu
            }
        )
    }))
)

// Button label
const dropdownLabel = computed(() => {
  if (props.labelAsCurrentSelectedRelation && props.selectedRelation) {
    const found = relations.value.find(r => r.dtoKey === props.selectedRelation)
    if (found) return maybeT(found.label)
  }
  return String(t('SkosSelector.label'))
})

// ---- Handlers
function onSelect(key: string) {
  emit('update:selectedRelation', key)
}
</script>

<style scoped>
</style>

<i18n>
en:
  SkosSelector:
    label: Map term as
fr:
  SkosSelector:
    label: Choisir une relation
</i18n>
