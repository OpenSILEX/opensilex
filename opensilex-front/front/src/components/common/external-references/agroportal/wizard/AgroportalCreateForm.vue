<template>
  <opensilex-WizardForm
    ref="wizardRef"
    :steps="steps"
    :createTitle="createTitle"
    :editTitle="editTitle"
    :icon="icon"
    :modalWidth='1200'
    :initForm="getEmptyForm"
    :createAction="create"
    :updateAction="update"
    :convertAction="convert"
    :static="false"
    :nextStepAction="nextStep"
    :validateAction="validateCustom"
    :isBlockingStep="false"
    @agroportalTermSelected="handleTermSelected"
    @agroportalTermUnselected="handleTermUnselected"
  >
    <template #createAdditionalFields="scope">
      <slot name="createAdditionalFields" v-bind="scope"></slot>
    </template>
    <template #icon></template>
  </opensilex-WizardForm>
</template>

<script setup lang="ts">
import { ref, inject, computed } from 'vue'
import type { AgroportalAPIService } from 'opensilex-core/api/agroportalAPI.service'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { BaseExternalReferencesDTO } from '../../ExternalReferencesTypes'
import type { AgroportalTermDTO } from 'opensilex-core/model/agroportalTermDTO'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'
import type HttpResponse from 'opensilex-security/HttpResponse'
import { useI18n } from 'vue-i18n'

/* ----------------------------
   PROPS
---------------------------- */
const props = defineProps<{
  ontologiesConfig: string
  requireCreate?: boolean
  searchPlaceholder?: string
  descriptionPlaceholder?: string
  createTitle?: string
  editTitle?: string
  icon: { type: string, default: "fa#vials"}
  createMethod: (form: any) => Promise<HttpResponse<OpenSilexResponse<string>>>
  updateMethod: (form: any) => Promise<HttpResponse<OpenSilexResponse<string>>>
  emptyForm?: BaseExternalReferencesDTO
}>()

const emit = defineEmits<{
  (e: 'onCreate', v: any): void
  (e: 'onUpdate', v: any): void
}>()

/* ----------------------------
   SERVICES
---------------------------- */
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const agroportalService = opensilex.getService<AgroportalAPIService>('opensilex.AgroportalAPIService')
const { t } = useI18n({ useScope: 'local' })

/* ----------------------------
   REFS / STATE
---------------------------- */
const wizardRef = ref<any>(null)
const editMode = ref(false)
const termIsSelected = ref(false)

/* ----------------------------
   METHODS - Handlers
---------------------------- */
function handleTermSelected() {
    console.log("event import reçu dans AgroportalResults > AgroportalTermSelector > AgroportalSearchFormPart > AgroportalCreateForm")
  termIsSelected.value = true
}
function handleTermUnselected() {
  termIsSelected.value = false
}

/* ----------------------------
   WIZARD STEPS
---------------------------- */
const steps = computed(() => [
  // Step1
  {
    component: 'opensilex-AgroportalSearchFormPart',
    title: t('AgroportalCreateForm.step1-title'),
    finish: () =>
  !props.requireCreate && termIsSelected.value
    ? t('AgroportalCreateForm.reuse')
    : '',
    next: () =>
      termIsSelected.value
        ? t('AgroportalCreateForm.next')  // "Suivant"
        : t('AgroportalCreateForm.skip'), // "Passer"
    props: {
      ontologiesConfig: props.ontologiesConfig,
      searchPlaceholder: props.searchPlaceholder
    }
  },
  // Step2
  {
    component: 'opensilex-AgroportalCreateFormPart',
    title: t('AgroportalCreateForm.step2-title'),
    finish: t('AgroportalCreateForm.save'),
    next: t('AgroportalCreateForm.next'),
    props: {
      namePlaceholder: props.searchPlaceholder,
      descriptionPlaceholder: props.descriptionPlaceholder
    },
    slots: ['createAdditionalFields']
  },
  // Step 3
  {
    component: 'opensilex-AgroportalMappingFormPart',
    title: t('AgroportalCreateForm.step3-title'),
    props: {
      ontologiesConfig: props.ontologiesConfig,
      searchPlaceholder: props.searchPlaceholder
    }
  }
])


/* ----------------------------
   PUBLIC METHODS
---------------------------- */
function showCreateForm() {
  checkAgroportalReachable()
  wizardRef.value?.showCreateForm?.()
}

function showEditForm(form: BaseExternalReferencesDTO) {
  editMode.value = true
  wizardRef.value?.showEditForm?.(form)
}

defineExpose({ showCreateForm, showEditForm })

/* ----------------------------
   PRIVATE HELPERS
---------------------------- */
async function checkAgroportalReachable() {
  try {
    const http = await agroportalService.pingAgroportal()
    if (http && http.response && !http.response.result) {
      wizardRef.value?.skipStep?.()
    }
  } catch (e: any) {
    agroportalErrorHandler(e)
  }
}

function agroportalErrorHandler(error: HttpResponse) {
  if (error.status === 503) {
    wizardRef.value?.skipStep?.()
    return
  }
  opensilex.errorHandler(error)
}

/* ----------------------------
   FORM LOGIC
---------------------------- */
function getEmptyForm(): BaseExternalReferencesDTO {
  return JSON.parse(
    JSON.stringify(
      props.emptyForm ?? {
        uri: null,
        name: null,
        description: null,
        exact_match: [],
        close_match: [],
        broad_match: [],
        narrow_match: []
      }
    )
  )
}

async function create(form: BaseExternalReferencesDTO) {
  try {
    const http = await props.createMethod(form)
    form.uri = http.response.result
    opensilex.showSuccessToast(
      (opensilex as any).$t?.('component.common.success.creation-success-with-template', { uri: form.uri }) ??
      `Created ${form.uri}`
    )
    emit('onCreate', form)
  } catch (e) {
    opensilex.errorHandler(e)
  }
}

async function update(form: BaseExternalReferencesDTO) {
  try {
    const http = await props.updateMethod(form)
    form.uri = http.response.result
    opensilex.showSuccessToast(
      (opensilex as any).$t?.('component.common.success.update-success-with-template', { uri: form.uri }) ??
      `Updated ${form.uri}`
    )
    emit('onUpdate', form)
  } catch (e) {
    opensilex.errorHandler(e)
  }
}

function convert(form: BaseExternalReferencesDTO, searchDTO: AgroportalTermDTO) {
  if (!editMode.value) {
    form.uri = searchDTO.id
  }
  form.name = searchDTO.name
  form.description = searchDTO.definitions?.[0] ?? null
  form.exact_match = []
  form.narrow_match = []
  form.broad_match = []
  form.close_match = []
  return form
}

function validateCustom(form: BaseExternalReferencesDTO) {
  return Boolean(form.name)
}

function nextStep(stepIndex: number, form: BaseExternalReferencesDTO) {
  if (stepIndex === 0 && form.uri) {
    if (editMode.value) return true
    form.close_match.push(form.uri)
    form.uri = null
  }
  return true
}
</script>

<style scoped></style>

<i18n>
en:
  AgroportalCreateForm:
    step1-title: Search
    step2-title: Create
    step3-title: Mapping
    reuse: Reuse
    save: Save
    createNew: Create New
    create: Create
    map: Map
    skip: Skip
    next: Next
    search-for-ontology-term: Search for ontology term
    selected-term: Selected term
    no-selected-item: No selected term
    tutorial:
      step-search:
        title: Search a term
        content: Look for a term in Agroportal. You can change the selected ontologies using the filter button.
      step-result:
        title: Select a concept
        content: >
          Select the concept that you want to reuse. If no concept exactly matches yours, you can select the
          closest and create a new one. You can enrich it on later steps.
      step-selected:
        title: Selected concept
        content: >
          The selected concept appears here. If you want to deselect it, you can click on the trash button above.
      step-validation:
        title: Validation
        content: >
          If you want to import the concept as-is, click the '@:AgroportalSearchFormPart.reuse' button. If
          you want to use it as a basis for creating your own concept, click the '@:AgroportalSearchFormPart.next'
          button. Please note that for Units, you are required to enrich the concept.
      step-no-concept:
        title: No concept
        content: >
          If you didn't find any concept in Agroportal that matches yours, you can create your own by clicking the
          '@:AgroportalSearchFormPart.skip' button when no concept is selected.
fr:
  AgroportalCreateForm:
    step1-title: Chercher
    step2-title: Créer
    step3-title: Aligner
    reuse: Réutiliser
    save: Enregistrer
    createNew: Créer Nouveau
    create: Créer
    map: Mapper
    skip: Passer
    next: Suivant
    search-for-ontology-term: Rechercher un terme
    selected-term: Terme sélectionné
    no-selected-item: Aucun terme sélectionné
    tutorial:
      step-search:
        title: Cherchez un terme
        content: >
          Recherchez un terme dans Agroportal. Vous pouvez changer les ontologies parcourues en cliquant sur le
          bouton de filtre.
      step-result:
        title: Sélectionnez un concept
        content: >
          Sélectionnez le concept à réutiliser. Si aucun concept ne correspond à celui que vous cherchez à définir, vous
          pouvez choisir le concept qui s'en rapproche le plus et à partir de ce dernier en créer un nouveau. Vous pourrez l'enrichir aux étapes suivantes.
      step-selected:
        title: Concept sélectionné
        content: >
          Le concept sélectionné apparaît ici. Pour le retirer, cliquez sur le bouton de suppression ci-dessus.
      step-validation:
        title: Validation
        content: >
          Si vous souhaitez réutiliser le concept tel quel, cliquez sur le bouton
          '@:AgroportalSearchFormPart.reuse'. Si vous souhaitez l'utiliser comme base pour définir votre
          propre concept, cliquez sur le bouton '@:AgroportalSearchFormPart.create'. Veuillez noter que vous
          devez impérativement enrichir le concept dans le cas d'une unité.
      step-no-concept:
        title: Pas de concept
        content: >
          Si vous ne trouvez pas de concept dans Agroportal qui correspond à celui que vous cherchez à définir, vous
          pouvez créer le vôtre en cliquant sur le bouton '@:AgroportalSearchFormPart.create' lorsqu'aucun concept
          n'est sélectionné.
</i18n>