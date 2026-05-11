<template>
  <div class="container-fluid">
    <opensilex-PageContent>
      <div class="row">
        <div class="col-lg-6">
          <opensilex-Card
            label="component.common.informations"
            icon="bi-clipboard"
          >
            <template #rightHeader>
              <opensilex-FavoriteButton :uri="device.uri" />

              <opensilex-EditButton
                v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_MODIFICATION_ID)"
                :label="t('DeviceDescription.update')"
                @click="editForm"
                :small="true"
              />

              <opensilex-DeleteButton
                v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_DELETE_ID)"
                label="DeviceDescription.delete"
                :small="true"
                @click="deleteDevice(device.uri)"
              />
            </template>

            <template #body>
                <div class="detailsCard">
                    <!-- URI -->
                    <opensilex-UriView :uri="device.uri" />

                    <!-- Type -->
                    <opensilex-StringView
                        :label="t('DeviceDescription.type')"
                        :value="device.rdf_type_name"
                    />

                    <!-- Nom -->
                    <opensilex-StringView
                        :label="t('DeviceDescription.name')"
                        :value="device.name"
                    />

                    <!-- Marque -->
                    <opensilex-StringView
                        :label="t('DeviceDescription.brand')"
                        :value="device.brand"
                    />

                    <!-- Modèle -->
                    <opensilex-StringView
                        :label="t('DeviceDescription.constructorModel')"
                        :value="device.constructor_model"
                    />

                    <!-- Numéro de serie -->
                    <opensilex-StringView
                        :label="t('DeviceDescription.serialNumber')"
                        :value="device.serial_number"
                    />

                    <!-- Personne responsable -->
                    <opensilex-ContactsList
                        :label="t('DeviceDescription.personInCharge')"
                        :list="loadedPersonInCharge ? [loadedPersonInCharge] : []"
                    />

                    <!-- Mise en service -->
                    <opensilex-StringView
                        :label="t('DeviceDescription.start_up')"
                        :value="device.start_up"
                    />

                    <!-- Mise hors service -->
                    <opensilex-StringView
                        :label="t('DeviceDescription.removal')"
                        :value="device.removal"
                    />

                    <!-- Description -->
                    <opensilex-StringView
                        :label="t('DeviceDescription.description')"
                        :value="device.description"
                    />

                    <!-- Dernière calibration -->
                    <opensilex-StringView
                        :label="t('Event.calibration')"
                        :value="lastCalibration"
                    />

                    <!-- Derniere position -->
                    <opensilex-StringView :label="t('Event.lastPosition')">
                        <span v-if="lastPosition?.location?.endDate">
                        {{ new Date(lastPosition.location.endDate).toLocaleString() }}
                        </span>

                        <ul>
                        <li v-if="lastPosition?.location?.to">
                            {{ lastPosition.location.to }}
                        </li>

                        <li
                            v-if="
                            lastPosition?.location &&
                            (lastPosition.location.x || lastPosition.location.y || lastPosition.location.z)
                            "
                        >
                            {{ customCoordinatesText(lastPosition.location) }}
                        </li>

                        <li v-if="lastPosition?.location?.text">
                            {{ lastPosition.location.text }}
                        </li>

                        <li v-if="lastPosition?.location?.geojson">
                            <opensilex-GeometryCopy
                            label=""
                            :value="lastPosition.location.geojson"
                            />
                        </li>
                        </ul>
                    </opensilex-StringView>

                    <!-- <opensilex-StringView :label="t('Event.lastPosition')">
                      <pre pre>{{ lastPosition }}</pre>
                      <br>
                      <span> position </span>
                      <pre>{{ JSON.stringify(lastPosition, null, 2) }}</pre>
                    </opensilex-StringView> -->

                    <!-- Relations -->
                    <div
                        v-for="(relation, index) in device.relations || []"
                        :key="index"
                    >
                        <opensilex-StringView
                        v-if="!relation.inverse && !isIsPartOfRelation(relation) && !isVariableRelation(relation)"
                        :label="getPropertyName(relation.property)"
                        :value="relation.value"
                        />

                        <opensilex-UriView
                        v-else-if="!relation.inverse && isIsPartOfRelation(relation)"
                        :title="getPropertyName(relation.property)"
                        :uri="relation.value"
                        :value="relation.value"
                        :to="{ path: '/device/details/' + encodeURIComponent(relation.value) }"
                        />
                    </div>

                    <opensilex-MetadataView
                        v-if="device.publisher && device.publisher.uri"
                        :publisher="device.publisher"
                        :publicationDate="device.publication_date"
                        :lastUpdatedDate="device.last_updated_date"
                    />
                </div>
            </template>
          </opensilex-Card>
        </div>

        <div class="col-lg-6">
          <div class="row">
            <div class="col-12 mb-3">
              <!-- Variables associées -->
              <opensilex-Card
                :label="t('DeviceDescription.variables')"
                icon="bi-clipboard"
              >
                <template #body>
                  <opensilex-TableView
                    v-if="(device.relations?.length ?? 0) !== 0 && linkedVariables.length > 0"
                    :items="linkedVariables"
                    :fields="relationsFields"
                    :globalFilterField="true"
                  >
                    <template #cell(name)="{ data }">
                      <opensilex-UriLink
                        :uri="data.item.uri"
                        :value="data.item.name"
                        :to="{ path: '/variable/details/' + encodeURIComponent(data.item.uri) }"
                      />
                    </template>
                  </opensilex-TableView>

                  <p v-else>
                    <strong>{{ t('DeviceDescription.no-var-provided') }}</strong>
                  </p>
                </template>
              </opensilex-Card>
            </div>

            <!-- Informations additionnelles  -->
            <div
              v-if="addInfo.length !== 0"
              class="col-12"
            >
              <opensilex-Card
                :label="t('DeviceDescription.additionalInfo')"
                icon="bi-clipboard"
              >
                <template #body>
                  <opensilex-TableView
                    :items="addInfo"
                    :fields="attributeFields"
                    :globalFilterField="false"
                  />
                </template>
              </opensilex-Card>
            </div>
          </div>
        </div>
      </div>
    </opensilex-PageContent>

    <!-- Formulaire Modal -->
    <opensilex-DeviceModalForm
      ref="deviceForm"
      @onUpdate="refresh"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, ref, watch } from 'vue'
import { useStore } from 'vuex'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'

import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { VueJsOntologyExtensionService, VueRDFTypeDTO } from '@/lib'
import type HttpResponse from '@/lib/HttpResponse'
import type { OpenSilexResponse } from '@/lib/HttpResponse'

import Oeev from "../../ontologies/Oeev";

import type {
  DeviceGetDetailsDTO,
  DevicesService,
  RDFObjectRelationDTO,
  VariableDetailsDTO
} from 'opensilex-core/index'

import type { EventGetDTO } from 'opensilex-core/model/eventGetDTO'
import type { PositionGetDTO } from 'opensilex-core/model/positionGetDTO'
import type { LocationObservationDTO } from 'opensilex-core/model/locationObservationDTO'
import type { EventsService } from 'opensilex-core/api/events.service'
import type { PositionsService } from 'opensilex-core/api/positions.service'
import type { VariablesService } from 'opensilex-core/api/variables.service'

import type { SecurityService, PersonDTO } from 'opensilex-security/index'

import DeviceModalForm from '../form/DeviceModalForm.vue'

const store = useStore()
const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

const service = $opensilex.getService<DevicesService>('opensilex.DevicesService')
const variablesService = $opensilex.getService<VariablesService>('opensilex.VariablesService')
const vueJsOntologyService = $opensilex.getService<VueJsOntologyExtensionService>('opensilex.VueJsOntologyExtensionService')
const eventsService = $opensilex.getService<EventsService>('opensilex.EventsService')
const positionService = $opensilex.getService<PositionsService>('opensilex.PositionsService')
const securityService = $opensilex.getService<SecurityService>('opensilex.SecurityService')

const deviceForm = ref<any>(null)

const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)

const uri = ref<string>('')
const addInfo = ref<Array<{ attribute: string; value: string }>>([])
const baseType = ref<string>($opensilex.Oeso.DEVICE_TYPE_URI)
const propertyByUri = ref<Map<string, VueRDFTypeDTO>>(new Map())

const linkedVariablesURIs = ref<string[]>([])
const linkedVariables = ref<VariableDetailsDTO[]>([])
const loadedPersonInCharge = ref<PersonDTO | null>(null)

const relationsFields = computed(() => [
  {
    key: 'name',
    label: t('DeviceDescription.name'),
    sortable: true
  }
])

const attributeFields = computed(() => [
  {
    key: 'attribute',
    label: t('DeviceDescription.attribute')
  },
  {
    key: 'value',
    label: t('DeviceDescription.value')
  }
])

const device = ref<DeviceGetDetailsDTO>({
  uri: null as any,
  rdf_type: null as any,
  name: null as any,
  brand: null as any,
  constructor_model: null as any,
  serial_number: null as any,
  person_in_charge: null as any,
  start_up: null as any,
  removal: null as any,
  description: null as any,
  metadata: null as any,
  relations: []
})

const lastCalibration = ref<string>('')

const lastPosition = ref<PositionGetDTO>({
  event: null as any,
  location: {
    geojson: null as any,
    featureOfInterest: null as any,
    label: null as any,
    startDate: null as any,
    from: null as any,
    to: null as any,
    endDate: null as any,
    x: null as any,
    y: null as any,
    z: null as any,
    text: null as any
  }
})

function syncUriFromRoute() {
  const raw = route.params.uri
  const param = Array.isArray(raw) ? raw[0] : raw
  uri.value = param ? decodeURIComponent(param) : ''
}

async function loadDevice() {
  if (!uri.value) return

  try {
    const http: HttpResponse<OpenSilexResponse<DeviceGetDetailsDTO>> =
      await service.getDevice(uri.value)

    device.value = http.response.result
    getAddInfo()
    loadProperties()
    loadLastCalibrationEvent()
    loadLastPosition()
    loadPersonInCharge()
    getLinkedVariablesURIs(device.value)
  } catch (error) {
    $opensilex.errorHandler(error)
  }
}

function refresh() {
  loadDevice()
}

function getLinkedVariablesURIs(currentDevice: DeviceGetDetailsDTO) {
  linkedVariablesURIs.value = []

  for (const relation of currentDevice.relations || []) {
    if (isVariableRelation(relation)) {
      linkedVariablesURIs.value.push(relation.value)
    }
  }

  if (linkedVariablesURIs.value.length > 0) {
    getLinkedVariables()
  } else {
    linkedVariables.value = []
  }
}

async function getLinkedVariables() {
  try {
    const http: HttpResponse<OpenSilexResponse<VariableDetailsDTO[]>> =
      await variablesService.getVariablesByURIs(linkedVariablesURIs.value)

    linkedVariables.value = http.response.result
  } catch (error) {
    $opensilex.errorHandler(error)
  }
}

async function loadProperties() {
  try {
    const http: HttpResponse<OpenSilexResponse<VueRDFTypeDTO>> =
      await vueJsOntologyService.getRDFTypeProperties(device.value.rdf_type, baseType.value)

    const typeModel = http.response.result
    const map = new Map<string, VueRDFTypeDTO>()

    typeModel.data_properties.forEach((property: any) => {
      map.set(property.uri, property)
    })
    typeModel.object_properties.forEach((property: any) => {
      map.set(property.uri, property)
    })

    propertyByUri.value = map
  } catch (error) {
    $opensilex.errorHandler(error)
  }
}

async function loadLastCalibrationEvent() {
  if (!device.value.uri) return

  try {
    const http: HttpResponse<OpenSilexResponse<EventGetDTO[]>> =
      await eventsService.searchEvents(
        Oeev.CALIBRATION_TYPE_URI,
        undefined,
        undefined,
        device.value.uri,
        undefined,
        ['end=desc'],
        undefined,
        undefined
      )

    if (http.response.result.length === 0) {
      lastCalibration.value = ''
      return
    }

    lastCalibration.value = new Date(http.response.result[0].end).toLocaleString()
  } catch (error) {
    $opensilex.errorHandler(error)
  }
}

async function loadLastPosition() {
  if (!device.value.uri) {
    lastPosition.value = null
    return
  }

  try {
    const http = await positionService.searchPositionHistory(
      device.value.uri,
      undefined,
      undefined,
      ['end=desc'],
      0,
      1
    )
    lastPosition.value = http.response.result?.[0] ?? null
  } catch (error) {
    $opensilex.errorHandler(error)
    lastPosition.value = null
  }
}

async function loadPersonInCharge() {
  if (!device.value.person_in_charge) {
    loadedPersonInCharge.value = null
    return
  }

  try {
    const http: HttpResponse<OpenSilexResponse<PersonDTO>> =
      await securityService.getPerson(device.value.person_in_charge)

    loadedPersonInCharge.value = http.response.result
  } catch (error) {
    $opensilex.errorHandler(error)
  }
}

function getAddInfo() {
  addInfo.value = []

  if (!device.value.metadata) return

  for (const property in device.value.metadata) {
    addInfo.value.push({
      attribute: property,
      value: device.value.metadata[property]
    })
  }
}

async function deleteDevice(deviceUri: string) {
  try {
    await service.deleteDevice(deviceUri)
    router.go(-1)
    const message = `${t('DeviceForm.name')} ${deviceUri} ${t('component.common.success.delete-success-message')}`
    $opensilex.showSuccessToast(message)
  } catch (error: any) {
    if (error?.response?.result?.title === 'LINKED_DEVICE_ERROR') {
      const message = `${t('DeviceList.associated-device-error')} ${error.response.result.message}`
      $opensilex.showErrorToast(message)
    } else {
      $opensilex.errorHandler(error)
    }
  }
}

function editForm() {
  deviceForm.value?.showEditForm?.(device.value.uri)
}

function isVariableRelation(relation: RDFObjectRelationDTO): boolean {
  const measuresProp = $opensilex.Oeso.MEASURES_PROP_URI
  return (
    relation.property === measuresProp ||
    relation.property === $opensilex.Oeso.getShortURI(measuresProp)
  )
}

function isIsPartOfRelation(relation: RDFObjectRelationDTO): boolean {
  const isPartOfProp = $opensilex.Oeso.IS_PART_OF
  return (
    relation.property === isPartOfProp ||
    relation.property === $opensilex.Oeso.getShortURI(isPartOfProp)
  )
}

function getPropertyName(propertyUri: string) {
  if (!propertyUri) return undefined

  const property = propertyByUri.value.get(propertyUri)
  if (property) {
    return property.name
  }

  return propertyUri
}

function customCoordinatesText(position: LocationObservationDTO): string | undefined {
  if (!position) return undefined

  let customCoordinates = ''

  if (position.x) {
    customCoordinates += `X:${position.x}`
  }
  if (position.y) {
    if (customCoordinates.length > 0) customCoordinates += ', '
    customCoordinates += `Y:${position.y}`
  }
  if (position.z) {
    if (customCoordinates.length > 0) customCoordinates += ', '
    customCoordinates += `Z:${position.z}`
  }

  return customCoordinates.length > 0 ? customCoordinates : undefined
}

onMounted(() => {
  syncUriFromRoute()
  loadDevice()
})

watch(
  () => route.params.uri,
  () => {
    syncUriFromRoute()
    loadDevice()
  }
)
</script>

<style scoped lang="scss">
.detailsCard {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
</style>

<i18n>
en:
  DeviceDescription:
    events: Events
    title: Device
    description: Description
    uri: URI
    name: Name
    type: Type
    start_up: Start up
    brand: Brand
    constructorModel: Constructor model
    serialNumber: Serial number
    personInCharge: Person in charge
    localisation: Localisation
    variables: Variables
    position: Position
    update: Update device
    delete: Delete device
    additionalInfo: Metadata
    removal: Removal
    attribute: Attribute
    value: Value
    no-var-provided: No variable provided
  Event:
    calibration: Last calibration
    lastPosition: Last position

fr:
  DeviceDescription:
    events: Événements
    title: Appareil
    description: Description
    uri: URI
    name: Nom
    type: Type
    start_up: Date de mise en service
    brand: Marque
    constructorModel: Modèle du constructeur
    serialNumber: Numéro de série
    personInCharge: Personne responsable
    localisation: Localisation
    variables: Variables
    position: Position
    update: Modifier cet appareil
    delete: Supprimer cet appareil
    additionalInfo: Métadonnées
    removal: Date de mise hors service
    attribute: Attribut
    value: Valeur
    no-var-provided: Aucune variable associée
  Event:
    calibration: Dernière calibration
    lastPosition: Dernière position
</i18n>