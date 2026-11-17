<template>
  <!--
    The confirmation card of a creation drafted from the files, without the assistant. The values
    the files could answer are filled; the rest is asked here, and nothing is written until the
    user confirms.
  -->
  <opensilex-ai-import-CreationProposalCard
    v-if="proposal"
    :proposal="proposal"
    :session-id="sessionId"
    @applied="onCardApplied"
    @cancelled="proposal = undefined"
  />
</template>

<script setup lang="ts">
import { inject, ref } from 'vue'
import type { CreationProposalDTO } from '../lib'
import type { OpenSilexPlugin } from '../opensilexPlugin'
import { useCreationDraft } from '../useCreationDraft'

//#region Public
const emit = defineEmits<{
  /** A creation was confirmed: the report it answered with is the new one. */
  applied: [payload: { target: string; report: unknown }]
}>()

const props = defineProps<{
  sessionId: string
}>()
//#endregion

//#region Private

//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
const { draft } = useCreationDraft($opensilex, () => props.sessionId)
//#endregion

//#region Data & Computed
const proposal = ref<CreationProposalDTO>()
//#endregion

//#region Event handlers
function onCardApplied(payload: { target: string; report: unknown }) {
  proposal.value = undefined
  emit('applied', payload)
}
//#endregion

//#region Methods
/**
 * Drafts the creation of a target and shows its card.
 *
 * @param values what the user already chose, by field name
 */
async function open(target: string, values?: { [field: string]: string }) {
  proposal.value = (await draft(target, values)) ?? undefined
}

/**
 * Shows a draft that already exists, such as the one a resumed conversation was left with.
 */
function show(pending: CreationProposalDTO) {
  proposal.value = pending
}
//#endregion

//#endregion

defineExpose({ open, show })
</script>
