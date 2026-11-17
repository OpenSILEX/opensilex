<template>
  <!--
    Declares the factors of the files in an experiment with the platform's own factor form: a
    factor the experiment lacks is created with its levels, one that lacks levels is edited with
    them added, since the platform adds a level by updating its factor. An experiment has several
    factors, so each is its own turn of the form.
  -->
  <opensilex-FactorForm
    ref="factorForm"
    :experiment="experiment"
    modalSize="lg"
    createTitle="component.experiment.add-factor"
    editTitle="component.experiment.update-factor"
    @onCreate="onFactorDone"
    @onUpdate="onFactorDone"
  />
</template>

<script setup lang="ts">
import { inject, nextTick, ref, useTemplateRef } from 'vue'
import type { FactorPlanDTO } from '../lib'
import type { OpenSilexPlugin } from '../opensilexPlugin'
import { useCreationDraft } from '../useCreationDraft'

/**
 * What the platform's factor form is opened with: the same shape for a factor to create and for
 * one to complete, a level carrying its URI only when it already exists.
 */
interface FactorFormValues {
  uri: string | null
  name: string | null
  category: string | null
  description: string | null
  experiment: string
  levels: Array<{ uri: string | null; name: string; description: string | null }>
}

interface FactorFormApi {
  showCreateForm(form: FactorFormValues): void
  showEditForm(form: FactorFormValues): void
}

//#region Public
const emit = defineEmits<{
  /** A factor was created or completed: the instance changed, so the report is to be read again. */
  declared: []
  /** Every factor the files name is declared: the form opened is for one of the user's own. */
  complete: []
}>()

const props = defineProps<{
  sessionId: string
}>()
//#endregion

//#region Private

//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
const { factorPlans } = useCreationDraft($opensilex, () => props.sessionId)
//#endregion

//#region Template refs
const factorForm = useTemplateRef<FactorFormApi>('factorForm')
//#endregion

//#region Data & Computed
const experiment = ref('')
//#endregion

//#region Event handlers
function onFactorDone() {
  emit('declared')
}
//#endregion

//#region Methods
function emptyValues(experimentUri: string): FactorFormValues {
  return {
    uri: null,
    name: null,
    category: null,
    description: null,
    experiment: experimentUri,
    levels: [{ uri: null, name: '', description: null }]
  }
}

function valuesOf(plan: FactorPlanDTO, experimentUri: string): FactorFormValues {
  return {
    uri: plan.uri ?? null,
    name: plan.name ?? null,
    category: null,
    description: plan.description ?? null,
    experiment: experimentUri,
    levels: (plan.levels ?? []).map((level) => ({
      uri: level.exists ? level.uri ?? null : null,
      name: level.name ?? '',
      description: level.description ?? null
    }))
  }
}

/**
 * Opens the form for the first factor that still has something to declare.
 *
 * @param experimentUri the experiment the factors belong to
 */
async function open(experimentUri: string) {
  const plans = await factorPlans(experimentUri)
  const next = plans.find((plan) => plan.status !== 'COMPLETE')
  experiment.value = experimentUri
  // The form takes its experiment from the prop, which has to be set before it is shown.
  await nextTick()
  if (!next) {
    // Everything the files name is declared, or they name nothing: a factor of the user's own.
    factorForm.value?.showCreateForm(emptyValues(experimentUri))
    emit('complete')
    return
  }
  const values = valuesOf(next, experiment.value)
  if (next.status === 'INCOMPLETE') {
    factorForm.value?.showEditForm(values)
  } else {
    factorForm.value?.showCreateForm(values)
  }
}
//#endregion

//#endregion

defineExpose({ open })
</script>
