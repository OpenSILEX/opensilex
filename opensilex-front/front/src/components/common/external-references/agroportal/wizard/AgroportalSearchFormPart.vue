<template>
    <!-- Tutoriel -->
    <opensilex-Tutorial
      ref="tutorial"
      :steps="tutorialSteps"
      @onFinish="onTutorialFinishOrSkip"
      @onSkip="onTutorialFinishOrSkip"
    />

    <!-- Grille principale -->
    <div class="row g-4">
      <!-- Colonne gauche -->
      <div class="col-12 col-lg-8">
        <div class="mb-3">
          <label class="form-label fw-bold fs-5">
            {{ t('AgroportalSearchFormPart.search-for-ontology-term') }}
          </label>

        <opensilex-AgroportalTermSelector
            ref="agroportalTermSelector"
            :placeholder="t(searchPlaceholder)"
            :ontologies="ontologies"
            @import="onTermImported"
            @inputValueHasChanged="updateItemName"
        />
        </div>
      </div>

      <!-- Colonne droite -->
      <div class="col-12 col-lg-4">
        <div class="mb-2 fw-bold d-flex align-items-center justify-content-between">
        <span>{{ t('AgroportalSearchFormPart.selected-term') }}</span>

        <opensilex-Button
          v-if="selectedTerm"
          @click="clearSelectedTerm"
          icon="fa#trash-alt"
          :small="true"
          variant="outline-danger"
          :label="t('AgroportalSearchFormPart.clear')"
        />
      </div>

      <opensilex-AgroportalResultItem
        v-if="!!selectedTerm"
        :entity="selectedTerm"
        id="v-step-selected"
      />
      <div v-else>
        {{ t('AgroportalSearchFormPart.no-selected-item') }}
      </div>
    </div>
  </div>

</template>

<script setup lang="ts">
import { ref, computed, watch, inject, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { AgroportalTermDTO } from 'opensilex-core/model/agroportalTermDTO'
import type { BaseExternalReferencesDTO } from '../../ExternalReferencesTypes'

type AgroportalTermDTO = {
  id: string
  name: string
  definitions: string[]
}

type BaseExternalReferencesDTO = { 
    uri?: string; 
    name?: string; 
    description?: string 
}

/* Props & Emits */
const props = defineProps<{
  editMode: boolean
  form: BaseExternalReferencesDTO
  searchPlaceholder: string
  ontologiesConfig: string
}>()

const emit = defineEmits<{
  (e: 'update:form', v: BaseExternalReferencesDTO): void
  (e: 'agroportalTermSelected'): void
  (e: 'agroportalTermUnselected'): void
}>()

const { t } = useI18n()
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

/* Refs & State */
const tutorial = ref<any>(null)
const agroportalTermSelector = ref<any>(null)
const uriGenerated = ref(true)
const text = ref('')
const ontologies = ref<string[]>([])
const selectedTerm = ref<AgroportalTermDTO | null>(null)

/* v-model proxy */
const formDto = computed<BaseExternalReferencesDTO>({
  get: () => props.form,
  set: (v) => emit('update:form', v)
})

/* Lifecycle */
onMounted(() => {
  ontologies.value = opensilex.getConfig().agroportal[props.ontologiesConfig]
})

/* Handlers */
function clearSelectedTerm() {
  selectedTerm.value = null
  emit('agroportalTermUnselected')
}

function onTermImported(term: AgroportalTermDTO) {
  selectedTerm.value = term
  props.form.uri = term.id
  props.form.name = term.name
  props.form.description = term.definitions?.[0]
  console.log("props.form -> ", props.form)
  emit('agroportalTermSelected')
}

function updateItemName(textVal: string) {
  formDto.value.name = textVal
}

/* Watch: notify wizard */
watch(selectedTerm, (newValue) => {
  if (newValue) emit('agroportalTermSelected')
  else emit('agroportalTermUnselected')
})

// Point d’entrée appelé par le wizard
function validate() {
  return true
}
defineExpose({ validate, startTutorial })

/* Tutorial logic */
type SavedState = { searchTerm: string; formDto: BaseExternalReferencesDTO }
let savedStateBeforeTutorial: SavedState | undefined

const tutorialSteps = [
  {
    target: '.v-step-agroportal-search',
    header: { title: t('AgroportalSearchFormPart.tutorial.step-search.title') },
    content: t('AgroportalSearchFormPart.tutorial.step-search.content'),
    params: { placement: 'bottom' }
  },
  {
    target: '.v-step-agroportal-results',
    header: { title: t('AgroportalSearchFormPart.tutorial.step-result.title') },
    content: t('AgroportalSearchFormPart.tutorial.step-result.content'),
    params: { placement: 'left' },
    before: beforeImportStep
  },
  {
    target: '#v-step-selected',
    header: { title: t('AgroportalSearchFormPart.tutorial.step-selected.title') },
    content: t('AgroportalSearchFormPart.tutorial.step-selected.content'),
    params: { placement: 'right' }
  },
  {
    target: '#v-step-wizard-buttons',
    header: { title: t('AgroportalSearchFormPart.tutorial.step-validation.title') },
    content: t('AgroportalSearchFormPart.tutorial.step-validation.content'),
    params: { placement: 'top' }
  },
  {
    target: '#v-step-wizard-next-button',
    header: { title: t('AgroportalSearchFormPart.tutorial.step-no-concept.title') },
    content: t('AgroportalSearchFormPart.tutorial.step-no-concept.content'),
    before: beforeNoSearchStep
  }
]

function startTutorial() {
  saveStateBeforeTutorial()
  clearCurrentState()
  agroportalTermSelector.value?.setSearchText?.(t(props.searchPlaceholder).toString())
  tutorial.value?.start?.()
}

function saveStateBeforeTutorial() {
  savedStateBeforeTutorial = {
    searchTerm: text.value,
    formDto: JSON.parse(JSON.stringify(formDto.value))
  }
}

function clearCurrentState() {
  agroportalTermSelector.value?.setSearchText?.('')
  selectedTerm.value = null
}

function restoreStateAfterTutorial() {
  if (!savedStateBeforeTutorial) return
  text.value = savedStateBeforeTutorial.searchTerm
  formDto.value = JSON.parse(JSON.stringify(savedStateBeforeTutorial.formDto))
}

async function beforeImportStep() {
  agroportalTermSelector.value?.selectAndImportFirstItem?.()
}

async function beforeNoSearchStep() {
  clearCurrentState()
}

function onTutorialFinishOrSkip() {
  restoreStateAfterTutorial()
}
</script>

<style scoped lang="scss">
.text-muted {
  color: var(--bs-secondary-color);
}
</style>

<i18n>
en:
  AgroportalSearchFormPart:
    step1-title: Search
    step2-title: Create
    step3-title: Mapping
    reuse: Reuse
    save: Save
    clear: Remove selected term
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
          If you want to import the concept as-is, click the 'Reuse' button. If
          you want to use it as a basis for creating your own concept, click the 'Next'
          button. Please note that for Units, you are required to enrich the concept.
      step-no-concept:
        title: No concept
        content: >
          If you didn't find any concept in Agroportal that matches yours, you can create your own by clicking the
          'Skip button when no concept is selected.
fr:
  AgroportalSearchFormPart:
    step1-title: Chercher
    step2-title: Créer
    step3-title: Aligner
    reuse: Réutiliser
    save: Enregistrer
    clear: Supprimer le terme selectionné
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
          'Réutiliser'. Si vous souhaitez l'utiliser comme base pour définir votre
          propre concept, cliquez sur le bouton 'Suivant'. Veuillez noter que vous
          devez impérativement enrichir le concept dans le cas d'une unité.
      step-no-concept:
        title: Pas de concept
        content: >
          Si vous ne trouvez pas de concept dans Agroportal qui correspond à celui que vous cherchez à définir, vous
          pouvez créer le vôtre en cliquant sur le bouton 'Passer' lorsqu'aucun concept
          n'est sélectionné.
</i18n>
