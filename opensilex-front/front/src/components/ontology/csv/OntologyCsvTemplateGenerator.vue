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
      :title="t('OntologyCsvTemplateGenerator.title')"
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
              :placeholder="typePlaceholder"
              :tree="true"
              :selectBranchNodes="true"
              checkStrategy="all"
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
            {{ t('OntologyCsvTemplateGenerator.downloadTemplate') }}
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
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type {
  VueDataTypeDTO,
  VueJsOntologyExtensionService,
  VueObjectTypeDTO,
  VueRDFTypeDTO,
  VueRDFTypePropertyDTO
} from '../../../lib'
import type { OntologyService } from 'opensilex-core/api/ontology.service'
import type { PropertiesByDomainDTO } from 'opensilex-core/index'
import Rdfs from '../../../ontologies/Rdfs'
import { createUriListFromGetPropertiesResult, sortProperties } from '../OntologyTools'
import Papa from 'papaparse'

interface GetTypesPromisesReturnType {
  uri: string
  dataProperties: Map<string, VueRDFTypePropertyDTO>
  objectProperties: Map<string, VueRDFTypePropertyDTO>
  propertiesOrder: Array<string> | undefined
  fromTypeModel: VueRDFTypeDTO
}

interface HeadersAndDescriptions {
  headers: Array<string>
  headersDescriptions: Array<string>
}

export interface DescriptionGeneratorInformation {
  propertyTranslationKey: string
  required: boolean
  example?: string
}

const props = withDefaults(defineProps<{
  baseType: string
  typePlaceholder?: string
  templatePrefix?: string
  uriHelp?: string
  uriExample?: string
  typeHelp?: string
  typeExample?: string
  extraHeadersAndDescriptions?: Map<string, DescriptionGeneratorInformation>
}>(), {
  templatePrefix: 'csv_import_template'
})

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const { t } = useI18n()

const showModal = ref(false)
const requiredField = ref(false)
const separator = ref(',')
const types = ref<string[]>([])

const dataTypesToExampleTranslateKey = new Map<string, string>([
  ['xsd:integer', 'integer'],
  ['xsd:decimal', 'decimal'],
  ['xsd:string', 'string'],
  ['xsd:anyURI', 'uri'],
  ['xsd:date', 'date'],
  ['xsd:datetime', 'datetime'],
  ['xsd:boolean', 'boolean']
])

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

/**
 * Construit une description détaillée d’une propriété RDF.
 * Inclut nom, type, description, caractère obligatoire et exemple si applicable.
 */
function getCustomPropertyDescription(
  property: VueRDFTypePropertyDTO,
  isDataProperty: boolean
) {
  const parts = Array.of(
    t('OntologyCsvTemplateGenerator.property-name'),
    ' : ',
    property.name,

    '\n',
    t('OntologyCsvTemplateGenerator.data-type'),
    ' : ',
    getPropertyLabel(property.target_property, isDataProperty),

    '\n',
    t('OntologyCsvTemplateGenerator.property-description'),
    ' : ',
    property.comment,

    '\n',
    t('OntologyCsvTemplateGenerator.required'),
    ' : ',
    property.is_required ? t('component.common.yes') : t('component.common.no')
  )

  if (property.is_list) {
    parts.push('\n', t('OntologyCsvTemplateGenerator.property-list'))
  }

  if (isDataProperty) {
    parts.push(
      '\n',
      t('component.common.example'),
      ' : ',
      getPropertyExample(property.target_property, isDataProperty) ?? ''
    )
  }

  return parts.join('')
}

/**
 * Génère une description de propriété simple à partir de paramètres.
 */
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

/**
 * Génère le texte descriptif d’un header CSV à partir d’un objet de configuration.
 */
function getPropertyDescriptionFromInfoObject(
  propertyDescriptionInfo: DescriptionGeneratorInformation
): string {
  const parts = Array.of(
    t(propertyDescriptionInfo.propertyTranslationKey),
    '\n',
    t('OntologyCsvTemplateGenerator.required'),
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

/**
 * Récupère les propriétés RDF pour chaque type sélectionné.
 * @returns une liste de promesses contenant les métadonnées des types
 */
async function getTypesPromises(): Promise<Array<Promise<GetTypesPromisesReturnType>>> {
  const ontoService = $opensilex.getService<VueJsOntologyExtensionService>(
    'opensilex-front.VueJsOntologyExtensionService'
  )

  const promises: Array<Promise<GetTypesPromisesReturnType>> = []

  for (const type of types.value) {
    const typePromise = ontoService.getRDFTypeProperties(type, props.baseType)
      .then((http: any) => {
        const result: GetTypesPromisesReturnType = {
          uri: type,
          dataProperties: new Map<string, VueRDFTypePropertyDTO>(),
          objectProperties: new Map<string, VueRDFTypePropertyDTO>(),
          propertiesOrder: http.response.result.properties_order,
          fromTypeModel: http.response.result
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

/**
 * Génère les en-têtes et descriptions quand aucun ordre de propriétés n’est défini.
 * Fusionne les propriétés de tous les types en évitant les doublons.
 */
async function generateHeadersNoOrderFound(
  typeModels: GetTypesPromisesReturnType[],
  headers: string[],
  headersDescription: string[]
): Promise<HeadersAndDescriptions> {
  const visitedProperties = new Set<string>()

  const normalOntologyService = $opensilex.getService<OntologyService>(
    'opensilex.OntologyService'
  )

  const propertiesByDomainHttpResponse =
    await normalOntologyService.getPropertiesByDomainHierarchyUsingRestrictions(
      props.baseType,
      types.value
    )

  const propertiesByDomain: PropertiesByDomainDTO[] =
    propertiesByDomainHttpResponse.response.result

  const propertyIsDataPropMap = new Map<string, boolean>()
  const propertyVueRdfTypePropertyDtoMap = new Map<string, VueRDFTypePropertyDTO>()

  for (const typeResult of typeModels) {
    for (const typeResultPropertyUri of typeResult.dataProperties.keys()) {
      propertyIsDataPropMap.set(typeResultPropertyUri, true)
      propertyVueRdfTypePropertyDtoMap.set(
        typeResultPropertyUri,
        typeResult.dataProperties.get(typeResultPropertyUri)!
      )
    }

    for (const typeResultPropertyUri of typeResult.objectProperties.keys()) {
      propertyIsDataPropMap.set(typeResultPropertyUri, false)
      propertyVueRdfTypePropertyDtoMap.set(
        typeResultPropertyUri,
        typeResult.objectProperties.get(typeResultPropertyUri)!
      )
    }
  }

  for (const propertiesByDomainDTO of propertiesByDomain) {
    const propsOfDomainsUris: Array<string> = createUriListFromGetPropertiesResult(
      propertiesByDomainDTO.properties,
      $opensilex
    )

    propsOfDomainsUris.forEach((propertyUri) => {
      const shortPropertyUri = $opensilex.getShortUri(propertyUri)

      if (!visitedProperties.has(shortPropertyUri)) {
        visitedProperties.add(shortPropertyUri)

        if (propertyVueRdfTypePropertyDtoMap.has(shortPropertyUri)) {
          headers.push(shortPropertyUri)
          headersDescription.push(
            getCustomPropertyDescription(
              propertyVueRdfTypePropertyDtoMap.get(shortPropertyUri)!,
              propertyIsDataPropMap.get(shortPropertyUri)!
            )
          )
        }
      }
    })
  }

  for (const typeResult of typeModels) {
    for (const typeResultPropertyUri of typeResult.dataProperties.keys()) {
      const shortPropertyUri = $opensilex.getShortUri(typeResultPropertyUri)

      if (visitedProperties.has(shortPropertyUri)) {
        continue
      }

      visitedProperties.add(shortPropertyUri)

      if ($opensilex.compareUris(typeResultPropertyUri, Rdfs.LABEL)) {
        const copyOfEndHeaders = headers.slice(2)
        const copyOfEndDescriptions = headersDescription.slice(2)

        headers = headers.slice(0, 2)
        headersDescription = headersDescription.slice(0, 2)

        headers.push(typeResultPropertyUri)
        headersDescription.push(
          getCustomPropertyDescription(
            typeResult.dataProperties.get(typeResultPropertyUri)!,
            true
          )
        )

        headers.push(...copyOfEndHeaders)
        headersDescription.push(...copyOfEndDescriptions)
        continue
      }

      headers.push(typeResultPropertyUri)
      headersDescription.push(
        getCustomPropertyDescription(
          typeResult.dataProperties.get(typeResultPropertyUri)!,
          true
        )
      )
    }

    for (const typeResultPropertyUri of typeResult.objectProperties.keys()) {
      const shortPropertyUri = $opensilex.getShortUri(typeResultPropertyUri)

      if (!visitedProperties.has(shortPropertyUri)) {
        visitedProperties.add(shortPropertyUri)
        headers.push(typeResultPropertyUri)
        headersDescription.push(
          getCustomPropertyDescription(
            typeResult.objectProperties.get(typeResultPropertyUri)!,
            false
          )
        )
      }
    }
  }

  return { headers, headersDescriptions: headersDescription }
}

/**
 * Construit les données du CSV (headers, descriptions, lignes).
 * Utilise l’ordre défini par le type si disponible, sinon un ordre générique.
 */
async function generateCSV(typeModels: GetTypesPromisesReturnType[]) {
  let headers = ['uri', 'type']
  let headersDescription = [
    getPropertyDescription(props.uriHelp || '', false, props.uriExample),
    getPropertyDescription(props.typeHelp || '', false, props.typeExample)
  ]

  if (
    typeModels.length > 1 ||
    !typeModels[0]?.propertiesOrder ||
    typeModels[0].propertiesOrder.length === 0
  ) {
    const newHeadersAndDescriptions = await generateHeadersNoOrderFound(
      typeModels,
      headers,
      headersDescription
    )
    headers = newHeadersAndDescriptions.headers
    headersDescription = newHeadersAndDescriptions.headersDescriptions
  } else {
    const typeModelInfo = typeModels[0]

    let vueRDFTypePropertyDTOArray: Array<VueRDFTypePropertyDTO> =
      Array.from(typeModelInfo.objectProperties.values()).concat(
        Array.from(typeModelInfo.dataProperties.values())
      )

    vueRDFTypePropertyDTOArray = sortProperties(
      vueRDFTypePropertyDTOArray,
      typeModelInfo.fromTypeModel,
      $opensilex
    )

    vueRDFTypePropertyDTOArray.forEach((e) => {
      headers.push(e.uri)

      if (typeModelInfo.dataProperties.has(e.uri)) {
        headersDescription.push(
          getCustomPropertyDescription(
            typeModelInfo.dataProperties.get(e.uri)!,
            true
          )
        )
      } else {
        headersDescription.push(
          getCustomPropertyDescription(
            typeModelInfo.objectProperties.get(e.uri)!,
            false
          )
        )
      }
    })
  }

  if (props.extraHeadersAndDescriptions && props.extraHeadersAndDescriptions.size > 0) {
    headers.push(...Array.from(props.extraHeadersAndDescriptions.keys()))
    Array.from(props.extraHeadersAndDescriptions.values()).forEach((e) =>
      headersDescription.push(getPropertyDescriptionFromInfoObject(e))
    )
  }

  const data = [headers, headersDescription]

  if (typeModels && typeModels.length > 0) {
    const typeIdx = headers.indexOf('type')

    typeModels.forEach((typeModel) => {
      const row = new Array(headers.length).fill('')
      row[typeIdx] = typeModel.uri
      data.push(row)
    })
  } else {
    data.push(new Array(headers.length).fill(''))
  }

  return data
}

/**
 * Lance l’export du template CSV.
 * Récupère les types, génère le CSV puis déclenche le téléchargement.
 */
async function csvExport() {
  const isValid = await validateTemplate()
  if (!isValid) return

  const typePromises = await getTypesPromises()
  const results = await Promise.all(typePromises)
  const data = await generateCSV(results)

  const templateName = `${props.templatePrefix}_csv_template_${new Date().getTime()}`
  const csvContent = Papa.unparse(data, { delimiter: separator.value })

  downloadCsv(csvContent, templateName)
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

/**
 * Retourne un libellé lisible pour un type de propriété.
 * Gère les data properties et object properties.
 */
function getPropertyLabel(
  dataTypeUri: string,
  isDataProperty: boolean
): string | undefined {
  if (!dataTypeUri) {
    return undefined
  }

  let label: string | undefined

  if (isDataProperty) {
    const type: VueDataTypeDTO = $opensilex.getDatatype(dataTypeUri)
    if (type) {
      label = t(type.label_key)
    }
  } else {
    const type: VueObjectTypeDTO = $opensilex.getObjectType(dataTypeUri)
    if (type && type.name && type.name.length > 0) {
      label = `${type.name} (URI)`
    }
  }

  if (!label || label.length === 0) {
    return ''
  }

  return label.charAt(0).toUpperCase() + label.slice(1)
}

/**
 * Retourne un exemple de valeur pour un type de donnée.
 * Uniquement pour les data properties.
 */
function getPropertyExample(
  dataTypeUri: string,
  isDataProperty: boolean
): string | undefined {
  if (!dataTypeUri) {
    return undefined
  }

  if (isDataProperty) {
    const key = dataTypesToExampleTranslateKey.get(dataTypeUri)
    const translateKey = `datatype-example.${key}`
    return t(translateKey)
  }

  return undefined
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
  OntologyCsvTemplateGenerator:
    title: Generate CSV template
    downloadTemplate: Download CSV template
    property-name: Property name
    property-description: Property description
    property-list: This column can be present multiple time to define multiple values
    data-type: Data type
    required: Required
fr:
  OntologyCsvTemplateGenerator:
    title: Générer un modèle de CSV
    downloadTemplate: Télécharger le modèle de fichier CSV
    property-name: Nom de la propriété
    property-description: Description de la propriété
    property-list: Cette colonne peut être présente plusieurs fois pour définir plusieurs valeurs
    data-type: Type de donnée
    required: Obligatoire
</i18n>