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
        :end_required="Boolean(position.geojson) || Boolean(position.startDate)"
        v-model:isInstant="currentLocationIsInstant"
        :canBeInstant="true"
      />

      <div class="row">
        <!-- Geometry -->
        <div class="col-8">
          <!-- <opensilex-GeometryForm
            v-model:value="position.geojson"
            label="component.common.geometry.geometry-title"
            :required="Boolean(position.endDate)"
            placeholder="LocationsForm.geometry"
            @onUpdate="checkGeometryNotSaved"
          /> -->
        </div>

        <!-- Add position -->
        <div class="col-4" style="padding-top: 25px">
          <opensilex-AddChildButton
            @click="addPosition"
            label="LocationsForm.add-position"
            :small="true"
          />
          <span> {{ t('LocationsForm.add-position') }}</span>
        </div>
      </div>
    </div>

    <!-- Position list -->
    <opensilex-TableView :fields="fields" :items="facility.locations">
      <template #cell(startDate)="{ data }">
        <opensilex-DateView :value="data.item.startDate" />
      </template>

      <template #cell(endDate)="{ data }">
        <opensilex-DateView :value="data.item.endDate" />

        <!-- Warning iff endDate == 1970 (migration default) -->
        <div
          v-if="data.item.endDate === DEFAULT_DATE"
          class="alert alert-warning mt-2"
          role="alert"
        >
          {{ t('component.facility.warning.facility-default-date') }}
        </div>
      </template>

      <!-- <template #cell(geometry)="{ data }"> -->
        <!-- <opensilex-GeometryCopy label="" :value="data.item.geojson" /> -->
      <!-- </template> -->

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
          />
        </n-button-group>
      </template>
    </opensilex-TableView>

    <!-- Wizard édition position -->
    <opensilex-WizardForm
      ref="locationFormRef"
      :steps="locationSteps"
      editTitle="LocationsForm.update"
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

// v-model replacement of PropSync("form")
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

// Table fields
const fields = computed(() => [
  { key: 'startDate', label: 'component.common.begin', sortable: true },
  { key: 'endDate', label: 'component.common.end', sortable: true },
  { key: 'geometry', label: 'component.common.geometry.geometry-title' },
  { key: 'actions', label: 'component.common.actions' }
])

const locationSteps = computed(() => [
  {
    component: 'opensilex-LocationForm',
    title: 'LocationsForm.update' // nécessaire pour WizardForm (sinon t(undefined))
  }
])

const locationFormRef = ref<any>(null)
const index = ref<number>(-1)

/**
 * Check if endDate & geometry are filled but not "saved" via addPosition.
 */
function checkGeometryNotSaved() {
  const hasEndDate = !!position.value.endDate
  const hasGeometry = !!position.value.geojson

  if (hasEndDate && hasGeometry && !positionIsValid()) {
    emit('geometryIsNotSaved')
  }
}

/**
 * Simulate validation of addPosition: is the position already in the list.
 */
function positionIsValid(): boolean {
  const locs = facility.value.locations ?? []
  return locs.some(
    (l: any) => l?.endDate === position.value.endDate && l?.geojson === position.value.geojson
  )
}

function addPosition() {
  if (position.value.geojson && position.value.endDate) {
    if (!Array.isArray(facility.value.locations)) facility.value.locations = [] as any
    facility.value.locations.push({ ...position.value } as any)
    position.value = getPositionEmpty()
    emit('positionIsValid')
  }
}

function updatePosition(data: any) {
  index.value = data.index
  // copie pour éviter la mutation directe
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

// Wizard compatibility
function reset() {
  position.value = getPositionEmpty()
}

async function validate() {
  // À voir si on veut une validation plus stricte : return Boolean(position.value.geojson || position.value.text ...)
  return true
}

defineExpose({
  reset,
  validate
})
</script>

<style scoped lang="scss"></style>

<i18n>
en:
  LocationsForm:
    positions-geospatial: Geospatial positions
    add-position: Add position
    geometry:  POINT (10 10)
    update: Update the geospatial position
fr:
  LocationsForm:
    positions-geospatial: Positions géospatiales
    add-position: Ajouter une position
    geometry: POINT (10 10)
    update: Mettre à jour la position géospatiale
</i18n>
