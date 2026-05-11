<template>
  <div>
    <div v-for="(relation, index) in typeRelations" :key="index">
      <component
        v-if="getInputComponent(relation.property)"
        :is="getInputComponent(relation.property)"
        :property="relation.property"
        :label="relation.property.name"
        :required="relation.property.is_required"
        :multiple="relation.property.is_list"
        :value="relation.value"
        @update:value="onRelationValueUpdate($event, relation.property, relation)"
        :context="context"
        v-bind="getCustomPropsForComponent(relation.property.uri)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref, withDefaults } from 'vue'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { OntologyService } from 'opensilex-core/api/ontology.service'
import type {
  PropertiesByDomainDTO,
  RDFObjectRelationDTO
} from 'opensilex-core/index'
import type {
  VueJsOntologyExtensionService,
  VueRDFTypeDTO,
  VueRDFTypePropertyDTO
} from '@/lib'
import type { MultiValuedRDFObjectRelation } from './models/MultiValuedRDFObjectRelation'
import { createUriListFromGetPropertiesResult, sortProperties } from './OntologyTools'

const props = withDefaults(defineProps<{
  relations?: Array<RDFObjectRelationDTO>
  rdfType?: string
  baseType?: string
  excludedProperties?: Set<string>
  context?: { experimentURI: string } | string
  initHandler?: (relation: MultiValuedRDFObjectRelation) => void
  customComponentProps?: Map<string, Map<string, any>>
}>(), {
  relations: () => [],
  excludedProperties: () => new Set<string>(),
  customComponentProps: () => new Map<string, Map<string, any>>(),
  initHandler: () => {}
})

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const vueOntologyService = opensilex.getService<VueJsOntologyExtensionService>('opensilex.VueJsOntologyExtensionService')
const ontologyService = opensilex.getService<OntologyService>('opensilex.OntologyService')

const vueOntologyService = opensilex.getService<VueJsOntologyExtensionService>(
  'opensilex-front.VueJsOntologyExtensionService'
)

const ontologyService = opensilex.getService<OntologyService>(
  'opensilex-core.OntologyService'
)

const internalRelations = ref<Array<MultiValuedRDFObjectRelation>>([])
const typeModel = ref<VueRDFTypeDTO | null>(null)
const propertiesByDomainHierarchy = ref<PropertiesByDomainDTO[]>([])

const typeRelations = computed<Array<MultiValuedRDFObjectRelation>>(() => {
  // Retourne les relations internes uniquement lorsque le modèle RDF du type est chargé
  if (!typeModel.value) {
    return []
  }

  return internalRelations.value
})

    /**
  * Détermine le composant Vue à utiliser pour saisir la valeur d'une propriété RDF
  * Donne la priorité au composant spécifique défini pour cette propriété, sinon composant par défaut
  */
function getInputComponentName(property: VueRDFTypePropertyDTO): string | null {
  if (
    property.input_components_by_property &&
    property.input_components_by_property[property.uri]
  ) {
    return property.input_components_by_property[property.uri]
  }

  return property.input_component ?? null
}

function getInputComponent(property: VueRDFTypePropertyDTO): string | null {
  const componentName = getInputComponentName(property)

  if (!componentName) {
    return null
  }

  return componentName
}

function getHandledProperties(): Array<VueRDFTypePropertyDTO> {
  /**
  * Récupère les propriétés RDF à afficher, en excluant celles passées via excludedProperties,
  * puis les trie selon l'ordre défini par le modèle ou par la hiérarchie des domaines
  */
  if (!typeModel.value) {
    return []
  }

  const shortExcludedProperties = new Set<string>()

  props.excludedProperties.forEach(property => {
    shortExcludedProperties.add(opensilex.getShortUri(property))
  })

  const properties = typeModel.value.data_properties
    .concat(typeModel.value.object_properties)
    .filter(property => property.inherited === false)
    .filter(property => !shortExcludedProperties.has(opensilex.getShortUri(property.uri)))
    .filter(property => !!getInputComponent(property))

  if (!typeModel.value.properties_order || typeModel.value.properties_order.length === 0) {
    const newPropertyOrder: Array<VueRDFTypePropertyDTO> = []

    for (const propertiesByDomainDTO of propertiesByDomainHierarchy.value) {
      const propsOfDomainsUris: Array<string> = createUriListFromGetPropertiesResult(
        propertiesByDomainDTO.properties,
        opensilex
      )

      propsOfDomainsUris.forEach(propertyUri => {
        const filteredByCurrentProperty = properties.filter(property =>
          opensilex.compareUris(property.uri, propertyUri)
        )

        if (filteredByCurrentProperty.length === 1) {
          newPropertyOrder.push(filteredByCurrentProperty[0])
        }
      })
    }

    const visitedPropsUris = newPropertyOrder.map(property =>
      opensilex.getShortUri(property.uri)
    )

    newPropertyOrder.push(
      ...properties.filter(
        property => !visitedPropsUris.includes(opensilex.getShortUri(property.uri))
      )
    )

    return newPropertyOrder
  }

  return sortProperties(properties, typeModel.value, opensilex)
}

function getCustomPropsForComponent(property: string): any {
  /**
  * Retourne les props personnalisées associées à une propriété RDF,
  * afin de les transmettre dynamiquement au composant d'input
  */
  if (!props.customComponentProps || !props.customComponentProps.has(property)) {
    return {}
  }

  const customAttributes = props.customComponentProps.get(property)

  if (!customAttributes || customAttributes.size === 0) {
    return {}
  }

  return Object.fromEntries(customAttributes)
}

async function typeSwitch(type: string, initialLoad: boolean) {
  /**
  * Charge les propriétés du type RDF sélectionné, initialise le modèle interne,
  * puis prépare les relations affichées dans le formulaire
  */
  if (!type || type.length === 0 || !props.baseType) {
    typeModel.value = null
    internalRelations.value = []
    propertiesByDomainHierarchy.value = []
    return
  }

  try {
    const propertiesByDomainHttpResponse =
      await ontologyService.getPropertiesByDomainHierarchyUsingRestrictions(
        props.baseType,
        [type]
      )
    propertiesByDomainHierarchy.value = propertiesByDomainHttpResponse.response.result

    propertiesByDomainHierarchy.value =
      propertiesByDomainHttpResponse.response.result

    const vueRdfTypeResponse =
      await vueOntologyService.getRDFTypeProperties(type, props.baseType)

    typeModel.value = vueRdfTypeResponse.response.result

    internalRelations.value.splice(0)

    if (initialLoad) {
      internalRelations.value.push(...toMultiValuedRelations(props.relations))
    } else {
      internalRelations.value.push(
        ...getHandledProperties().map(property => ({
          property,
          value: property.is_list ? [] : undefined
        }))
      )
    }

    if (props.initHandler) {
      internalRelations.value.forEach(relation => {
        props.initHandler?.(relation)
      })
    }
  } catch (error) {
    opensilex.errorHandler(error)
  }
}

function getInputComponent(property: VueRDFTypePropertyDTO) {
  /**
  * Détermine le composant Vue à utiliser pour saisir la valeur d'une propriété RDF
  * Donne la priorité au composant spécifique défini pour cette propriété, sinon composant par défaut
  */
  if (property.input_components_by_property && property.input_components_by_property[property.uri]) {
    return property.input_components_by_property[property.uri]
  }
  return property.input_component
}

function updateRelation(_newValue: string | Array<string>, _property: VueRDFTypePropertyDTO) {
  /**
  * Synchronise la prop relations avec les relations internes,
  * en reconvertissant les valeurs multiples en relations mono-valuées.
  */
function updateRelation(
  _newValue: string | Array<string>,
  _property: VueRDFTypePropertyDTO
) {
  props.relations.splice(0)
  props.relations.push(...toMultipleMonoValuedRelations(internalRelations.value))
}

function onRelationValueUpdate(
  /**
  * Met à jour la valeur d'une relation après modification dans un champ,
  * puis synchronise la liste des relations exposée au parent
  */
function onRelationValueUpdate(
  newValue: string | Array<string>,
  property: VueRDFTypePropertyDTO,
  relation: MultiValuedRDFObjectRelation
) {
  relation.value = newValue
  updateRelation(newValue, property)
}

function toMultipleMonoValuedRelations(
  /**
  * Convertit les relations internes, qui peuvent contenir plusieurs valeurs,
  * en liste de relations RDF mono-valuées attendue par l'API ou le parent
  */
function toMultipleMonoValuedRelations(
  relations: Array<MultiValuedRDFObjectRelation>
): Array<RDFObjectRelationDTO> {
  const newRelations: Array<RDFObjectRelationDTO> = []

  relations
    .filter(relation =>
      Array.isArray(relation.value)
        ? relation.value.length > 0
        : relation.value !== undefined &&
          relation.value !== null &&
          relation.value !== ''
    )
    .forEach(relation => {
      if (Array.isArray(relation.value)) {
        relation.value.forEach(value => {
          newRelations.push({
            property: relation.property.uri,
            value
          })
        })
      } else {
        newRelations.push({
          property: relation.property.uri,
          value: relation.value
        })
      }
    })

  return newRelations
}

function toMultiValuedRelations(
  /**
  * Regroupe les relations RDF mono-valuées par propriété,
  * afin de reconstruire le modèle utilisé par les composants du formulaire
  */
function toMultiValuedRelations(
  relations: Array<RDFObjectRelationDTO>
): Array<MultiValuedRDFObjectRelation> {
  if (!typeModel.value) {
    return []
  }

  const valueByProperties = new Map<string, MultiValuedRDFObjectRelation>()

  relations
    .filter(relation => !relation.inverse)
    .forEach(relation => {
      let propertyDto =
        typeModel.value?.object_properties.find(propertyModel =>
          opensilex.compareUris(propertyModel.uri, relation.property)
        )

      if (!propertyDto) {
        propertyDto =
          typeModel.value?.data_properties.find(propertyModel =>
            opensilex.compareUris(propertyModel.uri, relation.property)
          )
      }

      if (!propertyDto) {
        return
      }

      if (propertyDto.is_list) {
        if (!valueByProperties.has(propertyDto.uri)) {
          valueByProperties.set(propertyDto.uri, {
            property: propertyDto,
            value: [relation.value]
          })
        } else {
          const values = valueByProperties.get(propertyDto.uri)?.value as Array<string>
          values.push(relation.value)
        }
      } else {
        valueByProperties.set(propertyDto.uri, {
          property: propertyDto,
          value: relation.value
        })
      }
    })

  const emptyRelations: Array<MultiValuedRDFObjectRelation> = getHandledProperties()
    .filter(property => !valueByProperties.has(property.uri))
    .map(property => ({
      property,
      value: property.is_list ? [] : undefined
    }))

  return Array.from(valueByProperties.values()).concat(emptyRelations)
}

watch(
  () => props.rdfType,
  async rdfType => {
    if (!rdfType) {
      typeModel.value = null
      internalRelations.value = []
      propertiesByDomainHierarchy.value = []
      return
    }

    const initialLoad = props.relations.length > 0
    await typeSwitch(rdfType, initialLoad)
  },
  { immediate: true }
)

defineExpose({
  typeSwitch
})
</script>

<style scoped>
</style>