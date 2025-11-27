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
    target: '.v-step-agroportal-references .v-step-agroportal-results .n-base-wave',
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
    target: '.v-step-agroportal-references .v-step-skos-relation-table .n-base-wave',
    header: { title: t('AgroportalMappingFormPart.tutorial.step-change-mapping.title') },
    content: t('AgroportalMappingFormPart.tutorial.step-change-mapping.content'),
    params: { placement: 'left' }
  },
  {
    target: '.v-step-agroportal-references .v-step-skos-relation-input .uri-input',
    header: { title: t('AgroportalMappingFormPart.tutorial.step-manual-uri.title') },
    content: t('AgroportalMappingFormPart.tutorial.step-manual-uri.content'),
    params: { placement: 'top' },
    before: beforeManualMappingStep
  },
  {
    target: '.v-step-agroportal-references .v-step-skos-relation-input .n-button',
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
    tutorial:
      step-search:
        title: Search
        content: >
          Search for a term on Agroportal that you might want to link to your concept.
      step-results:
        title: Results
        content: >
          Browse the results from Agroportal and select one that you want to link as a reference to your concept.
      step-result-mapping:
        title: Result mapping
        content: >
          Click on the button to select the type of mapping that describes the relation between your concept and this
          one.
      step-table:
        title: References
        content: >
          The currently defined references are shown in this table. If your concept was created from an Agroportal
          term selected in the first step, a 'close match' relation is already defined for you. You can modify or
          delete it as any other reference.
      step-change-mapping:
        title: Change reference
        content: >
          You can change the type of relation for a reference using this button.
      step-manual-uri:
        title: Manual URI
        content: >
          You can also define an external reference by specifying its URI in this field.
      step-manual-mapping:
        title: Manual URI mapping
        content: >
          Select the type of relation for this external reference.
      step-table-bis:
        title: References
        content: >
          You can have as many references as your want for your concept.
      step-validation:
        title: Validation
        content: >
          Once you have linked your concept to external references, click the 'Save'
          button.


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
    tutorial:
      step-search:
        title: Recherche de terme
        content: >
          Cherchez un terme sur Agroportal que vous voudriez ajouter comme référence pour votre concept.
      step-results:
        title: Résultats
        content: >
          Parcourez les résultats de la recherche et sélectionnez le concept que vous souhaitez ajouter comme référence.
      step-result-mapping:
        title: Ajout de la relation
        content: >
          Cliquez sur ce bouton pour sélectionner le type de relation qui lie votre concept à celui-ci.
      step-table:
        title: Références
        content: >
          Les références actuellement définies pour votre concept sont montrées dans ce tableau. Si vous avez créé
          votre concept à partir d'un terme d'Agroportal à l'étape 1, alors une relation de type 'similaire' a été
          ajoutée automatiquement. Vous pouvez modifier ou supprimer cette relation, tout comme n'importe quelle
          autre référence.
      step-change-mapping:
        title: Modifier une référence
        content: >
          Vous pouvez changer le type de relation d'une référence en utilisant ce bouton.
      step-manual-uri:
        title: Référence arbitraire
        content: >
          Vous pouvez également définir une référence vers un concept en spécifiant son URI dans ce champ, par
          exemple dans le cas où vous ne le trouvez pas dans Agroportal.
      step-manual-mapping:
        title: Ajout de la relation
        content: >
          Sélectionnez le type de la relation qui lie votre concept à celui-ci.
      step-table-bis:
        title: Nombre de références
        content: >
          Vous pouvez avoir autant de références que vous le souhaitez pour votre concept.
      step-validation:
        title: Validation
        content: >
          Une fois votre concept lié à des références externes, cliquez sur le bouton
          'Terminer' pour créer votre concept.

</i18n>

