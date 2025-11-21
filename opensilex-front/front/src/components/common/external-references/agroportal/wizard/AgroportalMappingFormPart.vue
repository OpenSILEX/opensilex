<template>
  <div class="v-step-agroportal-references">
    <!-- Tutoriel -->
    <opensilex-Tutorial
      ref="tutorialRef"
      :steps="tutorialSteps"
      @onFinish="onTutorialFinishOrSkip"
      @onSkip="onTutorialFinishOrSkip"
    />

    <div class="container-fluid">
      <div class="row">
        <!-- Colonne gauche -->
        <div class="col-md-6">
          <!-- Recherche Agroportal -->
          <div class="mb-3">
            <label class="form-label fw-bold">
              {{ t('AgroportalMappingFormPart.search-mapping-title') }}
            </label>

            <div v-if="isAgroportalReachable">
              <opensilex-AgroportalTermSelector
                class="v-step-agroportal-search"
                ref="agroportalTermSelector"
                :placeholder="t(props.searchPlaceholder)"
                :ontologies="ontologies"
                :isMappingMode="true"
                @importMapping="onImportMapping"
              />
            </div>

            <div v-else>
              <n-alert type="info" closable>
                <span v-html="t('AgroportalMappingFormPart.agroportal-not-reachable')"></span>
              </n-alert>
            </div>
          </div>

          <!-- Saisie manuelle d'une URI -->
          <div class="mb-3 mapByUriSection">
            <label class="form-label fw-bold d-block" id="manual-mapping">
              {{ t('AgroportalMappingFormPart.map-manually-title') }}
            </label>

            <opensilex-FilterField :fullWidth="true" class="mapUriField">
              <div class="helperAndBlueStar mb-1">
                <opensilex-FormInputLabelHelper
                  :label="t('AgroportalMappingFormPart.manual-mapping')"
                  :helpMessage="ontologiesHelpHtml"
                />
              </div>

              <div
                v-if="isIncludedInRelations()"
                class="alert alert-danger py-1 my-2"
              >
                {{ t('component.skos.mapping-already-existing') }}
              </div>

              <opensilex-SkosRelationInput
                class="v-step-skos-relation-input"
                @input="addRelationToTerm"
              />
            </opensilex-FilterField>
          </div>
        </div>

        <!-- Colonne droite -->
        <div class="col-md-6">
          <!-- Récap du concept courant -->
          <div class="selectionTitle">
            {{ t('AgroportalMappingFormPart.selected-term') }}
          </div>

          <div class="result mb-3">
            <div class="mb-2 fw-bold">
              {{ formDto.name }}
            </div>

            <div v-if="formDto.uri" class="mb-2">
              <a :href="formDto.uri" target="_blank" rel="noopener noreferrer">
                {{ formDto.uri }}
              </a>
            </div>

            <div v-if="formDto.description">
              {{ formDto.description }}
            </div>
          </div>

          <!-- Table des relations SKOS -->
          <opensilex-SkosRelationTable
            class="v-step-skos-relation-table"
            v-model:uriRelations="uriRelations"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, defineProps, defineExpose, onMounted, inject } from 'vue'
import { useI18n, createI18n } from 'vue-i18n'
import { NAlert } from 'naive-ui'
import type { OpenSilexVuePlugin } from '@/models/OpenSilexVuePlugin'
import type { AgroportalAPIService } from 'opensilex-core/api/agroportalAPI.service'
import SUPPORTED_SKOS_RELATIONS, { BROAD_MATCH, type UriSkosRelation } from './../../../../../models/SkosRelations'
import type { BaseExternalReferencesDTO } from '../ExternalReferencesTypes'

/** Props (compatibles WizardForm) */
const props = defineProps<{
    ontologiesConfig: string
    searchPlaceholder: string
  displayInsertButton?: boolean
  form: BaseExternalReferencesDTO
}>()

/** Toolbox d'aide avec lien HTML vers portails */
const ontologiesHelpHtml = computed(() => `
  <p>${t('AgroportalMappingFormPart.ontologies-help')}</p>
  <ul class="tootltipLinks">
    <li>
      <a
        target="_blank"
        rel="noopener noreferrer"
     
        href="https://agroportal.lirmm.fr/"
      >
        AgroPortal
      </a>
    </li>
    <li>
      <a
        target="_blank"
        rel="noopener noreferrer"
      
        href="https://agroportal.lirmm.fr/"
      >
        BioPortal
      </a>
    </li>
  </ul>
`);


const { t } = useI18n()
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

/** Refs services */
const agroportalAPIService = ref<AgroportalAPIService | null>(null)
const agroportalTermSelector = ref<any | null>(null)
const tutorialRef = ref<any | null>(null)

/** État */
const formDto = computed<BaseExternalReferencesDTO>({
  get: () => props.form,
  set: (v) => Object.assign(props.form, v)
})
const ontologies = ref<string[]>([])
const isAgroportalReachable = ref<boolean>(false)

/** Tutoriel */
const savedStateBeforeTutorial = ref<{ formDto: BaseExternalReferencesDTO } | null>(null)


const tutorialSteps = [
  {
    target: '.v-step-agroportal-references .v-step-agroportal-search',
    header: { title: t('AgroportalMappingFormPart.tutorial.step-search.title') },
    content: t('AgroportalMappingFormPart.tutorial.step-search.content'),
    params: { placement: 'left' }
  },
  {
    target: '.v-step-agroportal-references .v-step-agroportal-results',
    header: { title: t('AgroportalMappingFormPart.tutorial.step-results.title') },
    content: t('AgroportalMappingFormPart.tutorial.step-results.content'),
    params: { placement: 'left' }
  },
  {
    target: '.v-step-agroportal-references .v-step-agroportal-results .v-step-skos-selector',
    header: { title: t('AgroportalMappingFormPart.tutorial.step-result-mapping.title') },
    content: t('AgroportalMappingFormPart.tutorial.step-result-mapping.content'),
    params: { placement: 'right', enableScrolling: false },
    before: beforeImportMappingStep
  },
  {
    target: '.v-step-agroportal-references .v-step-skos-relation-table',
    header: { title: t('AgroportalMappingFormPart.tutorial.step-table.title') },
    content: t('AgroportalMappingFormPart.tutorial.step-table.content'),
    params: { placement: 'left' },
    before: beforeMappingOverviewStep
  },
  {
    target: '.v-step-agroportal-references .v-step-skos-relation-table .v-step-skos-selector',
    header: { title: t('AgroportalMappingFormPart.tutorial.step-change-mapping.title') },
    content: t('AgroportalMappingFormPart.tutorial.step-change-mapping.content'),
    params: { placement: 'left' }
  },
  {
    target: '.v-step-agroportal-references .v-step-skos-relation-input .v-step-skos-relation-uri-input',
    header: { title: t('AgroportalMappingFormPart.tutorial.step-manual-uri.title') },
    content: t('AgroportalMappingFormPart.tutorial.step-manual-uri.content'),
    params: { placement: 'top' },
    before: beforeManualMappingStep
  },
  {
    target: '.v-step-agroportal-references .v-step-skos-relation-input .v-step-skos-selector',
    header: { title: t('AgroportalMappingFormPart.tutorial.step-manual-mapping.title') },
    content: t('AgroportalMappingFormPart.tutorial.step-manual-mapping.content'),
    params: { placement: 'top' }
  },
  {
    target: '.v-step-agroportal-references .v-step-skos-relation-table',
    header: { title: t('AgroportalMappingFormPart.tutorial.step-table-bis.title') },
    content: t('AgroportalMappingFormPart.tutorial.step-table-bis.content'),
    params: { placement: 'left' },
    before: beforeMappingOverviewAgainStep
  },
  {
    target: '#v-step-wizard-buttons',
    header: { title: t('AgroportalMappingFormPart.tutorial.step-validation.title') },
    content: t('AgroportalMappingFormPart.tutorial.step-validation.content'),
    params: { placement: 'top' }
  }
]

/** Getter/Setter table <-> DTO (exact_match / close_match / broad_match / narrow_match) */
const uriRelations = computed<Array<UriSkosRelation>>({
  get() {
    const list: UriSkosRelation[] = []
    for (const skos of SUPPORTED_SKOS_RELATIONS) {
      const arr = (formDto.value as any)[skos.dtoKey] as string[] | undefined
      if (Array.isArray(arr)) {
        for (const uri of arr) list.push({ relationDtoKey: skos.dtoKey, uri })
      }
    }
    return list
  },
  set(newList) {
    formDto.value.exact_match = []
    formDto.value.close_match = []
    formDto.value.broad_match = []
    formDto.value.narrow_match = []
    for (const it of newList) {
      ;(formDto.value as any)[it.relationDtoKey].push(it.uri)
    }
  }
})

/** Méthodes */
function onImportMapping(relation: UriSkosRelation) {
  addRelationToTerm(relation)
}

function addRelationToTerm(relation: UriSkosRelation) {
  if (isIncludedInRelations()) return
  ;(formDto.value as any)[relation.relationDtoKey].push(relation.uri)
}

function isIncludedInRelations(): boolean {
  // TODO: implémenter une vraie détection de doublon
  return false
}

/** Ping Agroportal & chargement ontologies */
async function checkAgroportalReachable() {
  try {
    const http = await agroportalAPIService.value!.pingAgroportal()
    isAgroportalReachable.value = !!http?.response?.result
  } catch (e: any) {
    console.log("AgroMappingPart - error ", e)
    if (e?.status === 503) {
      isAgroportalReachable.value = false
      return
    }
    opensilex.errorHandler(e)
  }
}

/** Tutoriel: helpers */
async function beforeImportMappingStep() {
  agroportalTermSelector.value?.selectFirstItem?.()
}
async function beforeMappingOverviewStep() {
  agroportalTermSelector.value?.selectAndMapFirstItem?.()
}
async function beforeManualMappingStep() {
  agroportalTermSelector.value?.setSearchText?.('')
}
async function beforeMappingOverviewAgainStep() {
  addRelationToTerm({
    uri: 'http://www.w3.org/2002/07/owl#Thing',
    relationDtoKey: BROAD_MATCH.dtoKey
  })
}

/** exposée au WizardForm */
function reset() {
}
function validate() {
  return true
}
function startTutorial() {
  savedStateBeforeTutorial.value = { formDto: JSON.parse(JSON.stringify(formDto.value)) }
  // état clean
  formDto.value.exact_match = []
  formDto.value.close_match = []
  formDto.value.broad_match = []
  formDto.value.narrow_match = []

  agroportalTermSelector.value?.setSearchText?.(t(props.searchPlaceholder).toString())
  tutorialRef.value?.start?.()
}
function onTutorialFinishOrSkip() {
  if (savedStateBeforeTutorial.value) {
    formDto.value = JSON.parse(JSON.stringify(savedStateBeforeTutorial.value.formDto))
  }
}

defineExpose({ reset, validate, startTutorial })

/** Lifecycle */
onMounted(async () => {
  agroportalAPIService.value = opensilex.getService('opensilex.AgroportalAPIService')
  ontologies.value = opensilex.getConfig().agroportal[props.ontologiesConfig] || []
  await checkAgroportalReachable()
})
</script>

<style scoped>
.helperAndBlueStar { display: flex; }
.result-name { font-weight: bold; font-size: large; margin-bottom: 5px; }
#manual-mapping { padding-top: 10px; }
.result { font-size: medium; padding: 5px; margin-right: 1px; }
.result:hover { background: rgba(0,0,0,.1); }
ul { list-style-type: none; }
.mapUriField { margin-left: -15px; }
.mapByUriSection { margin-top: 15px; }
.selectionTitle {
  padding-bottom: calc(0.5rem + 1px);
  font-size: 1.25rem;
  line-height: 1.5;
  font-weight: 700 !important;
}
</style>

<i18n lang="yaml">
en:
  AgroportalMappingFormPart:
    uri-help: >
      Uncheck this checkbox if you want to insert a concept from an existing ontology
      or if want to set a particular URI. Let it checked if you want to create a new
      entity with an auto-generated URI
    ontologies-help: "You can find URIs in this locations:"
    search-mapping-title: "Search for a term to map with"
    agroportal-not-reachable: >
      OpenSILEX ne peut pas accéder à AgroPortal, cependant vous pouvez cherchez des
      termes par vous-même en allant sur https://agroportal.lirmm.fr/.
    map-manually-title: "Map a term by URI"
    manual-mapping: "URI"
    selected-term: Selected term

fr:
  AgroportalMappingFormPart:
    uri-help: >
      Décocher si vous souhaitez ajouter une entité à partir d'une ontologie existante
      ou si vous souhaitez spécifier une URI particulière. Laisser coché si vous
      souhaitez ajouter une entité avec une URI auto-générée
    ontologies-help: "Vous pouvez chercher des URIs via ces portails :"
    search-mapping-title: "Rechercher un terme à associer"
    agroportal-not-reachable: >
      OpenSILEX ne peut pas accéder à AgroPortal, cependant vous pouvez cherchez des
      termes par vous-même en allant sur https://agroportal.lirmm.fr/.
    map-manually-title: "Associer un terme par URI"
    manual-mapping: "URI"
    selected-term: Terme sélectionné
</i18n>

