<template>
  <div>
    <!-- POSITIONS -->
    <br />
    <p>
      <b>{{ t('LocationsForm.positions-geospatial') }}</b>
    </p>
    <hr />

    <!-- Form ajout position -->
    <div>
      <opensilex-DateTimeRangeForm
        v-model:startDate="position.startDate"
        v-model:endDate="position.endDate"
        :start_required="false"
        :end_required="Boolean(position.startDate || position.geojson)"
        v-model:isInstant="currentLocationIsInstant"
        :canBeInstant="true"
        :enforceInternalRequiredRules="false"
      />

      <div v-if="endDateError" class="field-error mb-2">
        {{ endDateError }}
      </div>

      <div class="row">
        <!-- Geometry -->
        <div class="col-8">
          <opensilex-GeometryForm
            v-model:value="position.geojson"
            :label="t('component.common.geometry.geometry-title')"
            :required="Boolean(position.endDate)"
            :placeholder="t('LocationsForm.geometry')"
            @onUpdate="checkGeometryNotSaved"
          />
          <div v-if="geometryError" class="field-error mt-1">
            {{ geometryError }}
          </div>
        </div>

        <!-- Add position -->
        <div class="col-4" style="margin-top: 35px">
          <opensilex-AddChildButton
            @click="addPosition"
            :label="t('LocationsForm.add-position')"
            :small="true"
          />
          <span> {{ t('LocationsForm.add-position') }}</span>
        </div>
      </div>

      <div v-if="draftPositionError" class="field-error mt-2">
        {{ draftPositionError }}
      </div>
    </div>

    <!-- Position list -->
    <opensilex-TableView :fields="fields" :items="facility.locations">
      <template #cell(startDate)="{ data }">
        <opensilex-DateView :value="data.item.startDate" />
      </template>

      <template #cell(endDate)="{ data }">
        <opensilex-DateView :value="data.item.endDate" />

        <div
          v-if="data.item.endDate === DEFAULT_DATE"
          class="alert alert-warning mt-2"
          role="alert"
        >
          {{ t('component.facility.warning.facility-default-date') }}
        </div>
      </template>

      <template #cell(geometry)="{ data }">
        <opensilex-GeometryCopy label="" :value="data.item.geojson" />
      </template>

      <template #cell(actions)="{ data }">
        <n-button-group size="small">
          <opensilex-EditButton
            @click="updatePosition(data)"
            label="component.common.list.buttons.update"
            :small="true"
          />
          <opensilex-DeleteButton
            @click="deletePosition(data)"
            label="component.common.list.buttons.delete"
            :small="true"
          />
        </n-button-group>
      </template>
    </opensilex-TableView>

    <!-- Wizard édition position -->
    <opensilex-WizardForm
      ref="locationFormRef"
      :steps="locationSteps"
      :editTitle="t('LocationsForm.update')"
      icon="ik#ik-globe"
      :static="false"
      :initForm="getEmptyLocationForm"
      :updateAction="updateLocationForm"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { NButtonGroup } from 'naive-ui'
import type { FacilityUpdateDTO, LocationObservationDTO } from 'opensilex-core/index'

const { t } = useI18n()

const emit = defineEmits<{
  (e: 'onUpdate'): void
  (e: 'geometryIsNotSaved'): void
  (e: 'positionIsValid'): void
  (e: 'update:form', v: FacilityUpdateDTO): void
}>()

const props = defineProps<{
  form: FacilityUpdateDTO
}>()

const facility = ref<FacilityUpdateDTO>(props.form)
watch(
  () => props.form,
  (v) => {
    facility.value = v
  },
  { deep: true }
)
watch(
  facility,
  (v) => emit('update:form', v),
  { deep: true }
)

const DEFAULT_DATE = '1970-01-01T00:00:00Z'

function getPositionEmpty(): LocationObservationDTO {
  return {
    geojson: undefined,
    startDate: undefined,
    endDate: undefined
  } as any
}

const position = ref<LocationObservationDTO>(getPositionEmpty())
const currentLocationIsInstant = ref(true)

const endDateError = ref('')
const geometryError = ref('')
const draftPositionError = ref('')

// Nettoyage des erreurs au fil de la saisie
watch(
  () => position.value.startDate,
  () => {
    draftPositionError.value = ''
    if (position.value.startDate && position.value.endDate) {
      endDateError.value = ''
    }
  }
)

watch(
  () => position.value.endDate,
  (v) => {
    draftPositionError.value = ''
    if (v) {
      endDateError.value = ''
    }
  }
)

watch(
  () => position.value.geojson,
  (v) => {
    draftPositionError.value = ''
    if (v) {
      geometryError.value = ''
    }
  }
)

const fields = computed(() => [
  { key: 'startDate', label: t('component.common.begin'), sortable: true },
  { key: 'endDate', label: t('component.common.end'), sortable: true },
  { key: 'geometry', label: t('component.common.geometry.geometry-title') },
  { key: 'actions', label: t('component.common.actions') }
])

const locationSteps = computed(() => [
  {
    component: 'opensilex-LocationForm',
    title: t('LocationsForm.update')
  }
])

const locationFormRef = ref<any>(null)
const index = ref<number>(-1)

function checkGeometryNotSaved() {
  const hasEndDate = !!position.value.endDate
  const hasGeometry = !!position.value.geojson

  if (hasEndDate && hasGeometry && !positionIsValid()) {
    emit('geometryIsNotSaved')
  }
}

function positionIsValid(): boolean {
  const locs = facility.value.locations ?? []
  return locs.some(
    (l: any) => l?.endDate === position.value.endDate && l?.geojson === position.value.geojson
  )
}

function addPosition() {
  endDateError.value = ''
  geometryError.value = ''
  draftPositionError.value = ''

  const hasStart = !!position.value.startDate
  const hasEnd = !!position.value.endDate
  const hasGeometry = !!position.value.geojson

  // Rien saisi : on ne fait rien
  if (!hasStart && !hasEnd && !hasGeometry) {
    return
  }

  // Si début saisi -> fin obligatoire
  if (hasStart && !hasEnd) {
    endDateError.value = t('validations.required_if', {
      _field_: t('component.common.end') as string
    }) as string
    return
  }

  // Si géométrie saisie -> fin obligatoire
  if (hasGeometry && !hasEnd) {
    endDateError.value = t('validations.required_if', {
      _field_: t('component.common.end') as string
    }) as string
    return
  }

  // Si fin saisie -> géométrie obligatoire
  if (hasEnd && !hasGeometry) {
    geometryError.value = t('validations.required_if', {
      _field_: t('component.common.geometry.geometry-title') as string
    }) as string
    return
  }

  if (!Array.isArray(facility.value.locations)) {
    facility.value.locations = [] as any
  }

  facility.value.locations.push({ ...position.value } as any)
  position.value = getPositionEmpty()
  currentLocationIsInstant.value = true
  emit('positionIsValid')
}

function updatePosition(data: any) {
  index.value = data.index
  const form = JSON.parse(JSON.stringify(data.item))
  locationFormRef.value?.showEditForm?.(form)
}

function deletePosition(data: any) {
  const locs = facility.value.locations ?? []
  const i = locs.indexOf(data.item)
  if (i >= 0) locs.splice(i, 1)
}

function getEmptyLocationForm() {
  return {
    geojson: position.value.geojson,
    startDate: position.value.startDate,
    endDate: position.value.endDate
  }
}

function updateLocationForm(form: any) {
  const i = index.value
  if (i < 0) return
  const locs = facility.value.locations ?? []
  if (!locs[i]) return

  locs[i].geojson = form.geojson
  locs[i].startDate = form.startDate
  locs[i].endDate = form.endDate

  emit('onUpdate')
}

function reset() {
  position.value = getPositionEmpty()
  currentLocationIsInstant.value = true
  endDateError.value = ''
  geometryError.value = ''
  draftPositionError.value = ''
}

async function validate() {
  endDateError.value = ''
  geometryError.value = ''
  draftPositionError.value = ''

  const hasStart = !!position.value.startDate
  const hasEnd = !!position.value.endDate
  const hasGeometry = !!position.value.geojson

  // Cas autorisé : rien n'est saisi
  if (!hasStart && !hasEnd && !hasGeometry) {
    return true
  }

  // Si début saisi -> fin obligatoire
  if (hasStart && !hasEnd) {
    endDateError.value = t('validations.required_if', {
      _field_: t('component.common.end') as string
    }) as string
    return false
  }

  // Si géométrie saisie -> fin obligatoire
  if (hasGeometry && !hasEnd) {
    endDateError.value = t('validations.required_if', {
      _field_: t('component.common.end') as string
    }) as string
    return false
  }

  // Si fin saisie -> géométrie obligatoire
  if (hasEnd && !hasGeometry) {
    geometryError.value = t('validations.required_if', {
      _field_: t('component.common.geometry.geometry-title') as string
    }) as string
    return false
  }

  // Position complète mais non ajoutée à la liste
  draftPositionError.value = t('LocationsForm.position-not-added')
  return false
}

defineExpose({
  reset,
  validate
})
</script>

<style scoped lang="scss">
.field-error {
  color: #dc3545;
  font-size: 0.875rem;
}
</style>

<i18n>
en:
  LocationsForm:
    positions-geospatial: Geospatial positions
    add-position: Add position
    geometry: POINT (10 10) or POLYGON((4 5, 0 55, 100 78, 4 5))
    update: Update the geospatial position
    position-not-added: The entered position must be added to the list.
fr:
  LocationsForm:
    positions-geospatial: Positions géospatiales
    add-position: Ajouter une position
    geometry: POINT (10 10) ou POLYGON((4 5, 0 55, 100 78, 4 5))
    update: Mettre à jour la position géospatiale
    position-not-added: La position saisie doit être ajoutée à la liste.
</i18n>