<template>
  <n-modal
    v-model:show="showModal"
    :mask-closable="false"
    @after-leave="requiredField = false"
    @after-enter="requiredField = true"
  >
    <n-card
      style="width: 800px; max-width: 95vw"
      :bordered="false"
      role="dialog"
      aria-modal="true"
      :title="t('GenerateEventTemplate.title')"
    >
      <div>
        <div class="row g-3">
          <div class="col-md-8">
            <opensilex-TypeForm
              :type="types"
              :multiple="true"
              :required="false"
              :baseType="baseType"
              :ignoreRoot="false"
              :unselectableTypes="unselectableTypes"
              :tree="true"
              checkStrategy="all"
              :selectBranchNodes="true"
              :placeholder="t('GenerateEventTemplate.type-placeholder')"
              @update:type="types = $event"
            />
          </div>

          <div class="col-md">
            <opensilex-CSVSelectorInputForm
              :separator="separator"
              @update:separator="separator = $event"
            />
          </div>
        </div>

        <div class="mt-3">
          <n-button class="greenThemeColor" @click="csvExport">
            {{ t('GenerateEventTemplate.downloadTemplate') }}
          </n-button>
        </div>
      </div>

      <template #footer>
        <div class="d-flex justify-content-end">
          <button
            type="button"
            class="btn greenThemeColor"
            @click="hide"
          >
            {{ t('component.common.close') }}
          </button>
        </div>
      </template>
    </n-card>
  </n-modal>
</template>

<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { NModal, NCard, NButton } from 'naive-ui'
import Papa from 'papaparse'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type {
  VueJsOntologyExtensionService,
  VueRDFTypePropertyDTO
} from '../../../../lib'

interface DescriptionGeneratorInformation {
  propertyTranslationKey: string
  required: boolean
  example?: string
}

const props = withDefaults(defineProps<{
  isMove?: boolean
  targets?: string[]
}>(), {
  isMove: false,
  targets: () => []
})

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const { t } = useI18n()

const vueOntologyService =
  $opensilex.getService<VueJsOntologyExtensionService>(
    'opensilex-front.VueJsOntologyExtensionService'
  )

const showModal = ref(false)
const requiredField = ref(false)
const separator = ref(',')
const types = ref<string[]>([])

const MOVE_DESCRIPTION_GENERATOR_BY_HEADER = new Map<string, DescriptionGeneratorInformation>([
  ['from', { propertyTranslationKey: 'component.common.geometry.from-help', required: false, example: 'component.common.geometry.from-placeholder' }],
  ['to', { propertyTranslationKey: 'component.common.geometry.to-help', required: false, example: 'component.common.geometry.to-placeholder' }],
  ['coordinates', { propertyTranslationKey: 'component.common.geometry.geometry-help', required: false, example: 'component.common.geometry.coordinates-placeholder' }],
  ['x', { propertyTranslationKey: 'component.common.geometry.x-help', required: false, example: 'component.common.geometry.x-placeholder' }],
  ['y', { propertyTranslationKey: 'component.common.geometry.y-help', required: false, example: 'component.common.geometry.y-placeholder' }],
  ['z', { propertyTranslationKey: 'component.common.geometry.z-help', required: false, example: 'component.common.geometry.z-placeholder' }],
  ['textualPosition', { propertyTranslationKey: 'component.common.geometry.textual-position-help', required: false, example: 'component.common.geometry.textual-position-placeholder' }]
])

const orderedHeaders = ['uri', 'rdfType', 'isInstant', 'start', 'end', 'targets', 'description']

const moveFormatToEventFormatMapping = {
  uri: 'uri',
  rdfType: 'rdfType',
  isInstant: $opensilex.getShortUri($opensilex.Oeev.IS_INSTANT),
  start: $opensilex.getShortUri($opensilex.Time.HAS_BEGINNING),
  end: $opensilex.getShortUri($opensilex.Time.HAS_END),
  targets: $opensilex.getShortUri($opensilex.Oeev.CONCERNS),
  description: $opensilex.getShortUri($opensilex.Rdfs.COMMENT)
}

const baseType = computed(() =>
  props.isMove
    ? $opensilex.Oeev.MOVE_TYPE_URI
    : $opensilex.Oeev.EVENT_TYPE_URI
)

const unselectableTypes = computed(() =>
  props.isMove ? [] : [$opensilex.Oeev.MOVE_TYPE_URI]
)

function reset() {
  types.value = []
  separator.value = ','
}

function show() {
  showModal.value = true
}

function hide() {
  showModal.value = false
}

async function validateTemplate() {
  return true
}

function getCustomPropertyDescription(property: VueRDFTypePropertyDTO) {
  const parts = Array.of(
    property.name,
    '\n',
    t('GenerateEventTemplate.data-type'),
    ' : ',
    getDataTypeLabel(property.target_property),
    '\n',
    t('component.common.required'),
    ' : ',
    property.is_required ? t('component.common.yes') : t('component.common.no')
  )

  if (property.is_list) {
    parts.push('\n', t('GenerateEventTemplate.property-list'))
  }

  return parts.join('')
}

function getTypesPromises() {
  const promises = []

  for (const type of types.value) {
    const typePromise = vueOntologyService
      .getRDFTypeProperties(type, $opensilex.Oeev.EVENT_TYPE_URI)
      .then((http: any) => {
        const result = {
          uri: type,
          dataProperties: new Map<string, VueRDFTypePropertyDTO>(),
          objectProperties: new Map<string, VueRDFTypePropertyDTO>()
        }

        for (const property of http.response.result.data_properties) {
          result.dataProperties.set(property.uri, property)
        }

        for (const property of http.response.result.object_properties) {
          result.objectProperties.set(property.uri, property)
        }

        return result
      })

    promises.push(typePromise)
  }

  return promises
}

function generateCSV(typeModels: any[]) {
  const headers = generateMoveTemplate()
    ? [...orderedHeaders]
    : orderedHeaders.map((e) => moveFormatToEventFormatMapping[e as keyof typeof moveFormatToEventFormatMapping])

  const managedProperties = [
    $opensilex.Oeev.IS_INSTANT,
    $opensilex.Time.HAS_BEGINNING,
    $opensilex.Time.HAS_END,
    $opensilex.Oeev.CONCERNS,
    $opensilex.Rdfs.COMMENT
  ]

  function getPropertyDescription(
  propertyTranslationKey: string,
  required: boolean,
  example?: string
): string {
  return getPropertyDescriptionFromInfoObject({
    propertyTranslationKey,
    required,
    example
  })
}

function getPropertyDescriptionFromInfoObject(
  propertyDescriptionInfo: DescriptionGeneratorInformation
): string {
  const parts = Array.of(
    t(propertyDescriptionInfo.propertyTranslationKey),
    '\n',
    t('component.common.required'),
    ' : ',
    propertyDescriptionInfo.required
      ? t('component.common.yes')
      : t('component.common.no'),
    '. ',
    '\n',
    propertyDescriptionInfo.example && propertyDescriptionInfo.example.length > 0
      ? `${t('component.common.example')} : ${t(propertyDescriptionInfo.example)}`
      : ''
  )

  return parts.join('')
}

    const headersDescription = [
    getPropertyDescription('component.events.uri-help', false, 'component.events.uri-example'),
    getPropertyDescription('component.events.type-help', false, 'component.events.type-example'),
    getPropertyDescription('component.events.is-instant-help', true, 'component.events.is-instant-example'),
    getPropertyDescription('component.events.start-help', false, 'component.events.start-example'),
    getPropertyDescription('component.events.end-help', false, 'component.events.end-example'),
    getPropertyDescription('component.events.target-help', true, 'component.events.targets-example'),
    getPropertyDescription('component.common.description', false)
    ]

  if (generateMoveTemplate()) {
    managedProperties.push($opensilex.Oeev.FROM, $opensilex.Oeev.TO)

    headers.push(...Array.from(MOVE_DESCRIPTION_GENERATOR_BY_HEADER.keys()))

    Array.from(MOVE_DESCRIPTION_GENERATOR_BY_HEADER.values()).forEach((e) => {
    headersDescription.push(
        getPropertyDescriptionFromInfoObject(e)
    )
    })
  }

  const visitedProperties = new Set(
    managedProperties.map((property) => $opensilex.getShortUri(property))
  )

  for (const typeResult of typeModels) {
    const propertyFunction = (propURI: string, property: VueRDFTypePropertyDTO) => {
      if (!visitedProperties.has($opensilex.getShortUri(propURI))) {
        visitedProperties.add(propURI)
        headers.push(propURI)
        headersDescription.push(getCustomPropertyDescription(property))
      }
    }

    typeResult.dataProperties.forEach((property: VueRDFTypePropertyDTO, propURI: string) => {
      propertyFunction(propURI, property)
    })

    typeResult.objectProperties.forEach((property: VueRDFTypePropertyDTO, propURI: string) => {
      propertyFunction(propURI, property)
    })
  }

  const data = [headers, headersDescription]

  const typeIndex = orderedHeaders.indexOf('rdfType')
  const isInstantIndex = orderedHeaders.indexOf('isInstant')
  const endIndex = orderedHeaders.indexOf('end')
  const targetIndex = orderedHeaders.indexOf('targets')

  const generatedTargets = props.targets?.length ? props.targets : [undefined]
  const generatedTypes = typeModels?.length ? typeModels.map((type) => type.uri) : [undefined]

  generatedTargets.forEach((generatedTarget) => {
    generatedTypes.forEach((generatedType) => {
      const row = new Array(headers.length).fill('')

      row[typeIndex] = generatedType
      row[isInstantIndex] = 'true'
      row[endIndex] = new Date().toISOString()
      row[targetIndex] = generatedTarget

      data.push(row)
    })
  })

  return data
}

function downloadCsv(content: string, filename: string) {
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)

  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', `${filename}.csv`)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  URL.revokeObjectURL(url)
}

async function csvExport() {
  const isValid = await validateTemplate()
  if (!isValid) return

  const typePromises = getTypesPromises()
  const results = await Promise.all(typePromises)
  const data = generateCSV(results)

  let templateName = generateMoveTemplate() ? 'move' : 'event'
  templateName += '_template_' + new Date().getTime()

  const csvContent = Papa.unparse(data, { delimiter: separator.value })
  downloadCsv(csvContent, templateName)
}

function getDataTypeLabel(dataTypeUri: string): string | undefined {
  if (!dataTypeUri) {
    return undefined
  }

  const type = $opensilex.getDatatype(dataTypeUri)
  let label = 'URI'

  if (type) {
    label = t(type.label_key)
  }

  return label.charAt(0).toUpperCase() + label.slice(1)
}


function generateMoveTemplate(): boolean {
  if (props.isMove) {
    return true
  }

  if (types.value.length === 0) {
    return false
  }

  for (const type of types.value) {
    if ($opensilex.Oeev.checkURIs(type, $opensilex.Oeev.MOVE_TYPE_URI)) {
      return true
    }
  }

  return false
}

defineExpose({
  show,
  hide,
  reset,
  validateTemplate
})
</script>

<style scoped>
</style>

<i18n>
en:
  GenerateEventTemplate:
    data-type: Data type
    downloadTemplate: Download CSV template
    property-list: This column can be present multiple time to define multiple values
    title: Generate CSV template
    type-placeholder: Select a type of activity

fr:
  GenerateEventTemplate:
    data-type: Type de donnée
    downloadTemplate: Télécharger le modèle de fichier CSV
    property-list: Cette colonne peut être présente plusieurs fois pour définir plusieurs valeurs
    title: Générer un modèle de CSV
    type-placeholder: Selectionner un type d'activité
</i18n>