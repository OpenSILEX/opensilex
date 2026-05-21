<template>
  <div>
    <!-- Dates -->
    <opensilex-DateTimeRangeForm
      v-if="doShowDateFields"
      v-model:startDate="formState.startDate"
      v-model:endDate="formState.endDate"
      :start_required="false"
      :end_required="Boolean(formState.geojson) || Boolean(formState.startDate)"
      :isInstant="locationIsInstant"
      :canBeInstant="true"
    />

    <div class="row">
      <div class="col-lg-5">
        <opensilex-FacilitySelector
          :label="t('LocationForm.from')"
          v-model:facilities="formState.from"
          :multiple="false"
          :required="fromRequired"
          @select="updateRequiredProps"
          @clear="updateRequiredProps"
          :helpMessage="t('LocationForm.from-help')"
        />
      </div>

      <div class="col-lg-5">
        <opensilex-FacilitySelector
          :label="t('LocationForm.to')"
          v-model:facilities="formState.to"
          :multiple="false"
          :required="toRequired"
          @select="updateRequiredProps"
          @clear="updateRequiredProps"
          :helpMessage="t('LocationForm.to-help')"
        />
      </div>
    </div>

    <div>
      <p><b>{{ t('LocationForm.positionTitle') }}</b></p>
      <hr />

      <!-- Geometry -->
      <div class="row">
        <div class="col-lg-9">
          <opensilex-GeometryForm
            v-model:value="formState.geojson"
            label="component.common.geometry.geometry-title"
            helpMessage="component.common.geometry.geometry-help"
            :placeholder="t('LocationForm.geometry')"
            :required="false"
          />
        </div>
      </div>

      <!-- XYZ form -->
      <div class="row">
        <div class="col-lg-3">
          <opensilex-InputForm
            v-model:value="formState.x"
            label="component.common.geometry.x"
            :helpMessage="t('LocationForm.x-help')"
            :placeholder="t('LocationForm.x-placeholder')"
            type="text"
          />
        </div>

        <div class="col-lg-3">
          <opensilex-InputForm
            v-model:value="formState.y"
            label="component.common.geometry.y"
            :helpMessage="t('LocationForm.y-help')"
            :placeholder="t('LocationForm.y-placeholder')"
            type="text"
          />
        </div>

        <div class="col-lg-3">
          <opensilex-InputForm
            v-model:value="formState.z"
            label="component.common.geometry.z"
            :helpMessage="t('LocationForm.z-help')"
            :placeholder="t('LocationForm.z-placeholder')"
            type="text"
          />
        </div>
      </div>

      <!-- Textual position form -->
      <div class="row">
        <div class="col">
          <opensilex-TextAreaForm
            v-model:value="formState.text"
            label="component.common.geometry.textual-position"
            :helpMessage="t('LocationForm.textual-position-help')"
            :placeholder="t('LocationForm.textual-position-placeholder')"
            @keydown.enter.stop
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { LocationObservationDTO } from 'opensilex-core/index'

const { t } = useI18n()

function emptyLocationForm(): LocationObservationDTO {
  return {
    geojson: undefined,
    featureOfInterest: undefined,
    label: undefined,
    startDate: undefined,
    from: undefined,
    to: undefined,
    endDate: undefined,
    x: undefined,
    y: undefined,
    z: undefined,
    text: undefined
  }
}

const props = withDefaults(
  defineProps<{
    form?: LocationObservationDTO
    disableValidation?: boolean
    isMove?: boolean
    displayDateFields?: boolean
  }>(),
  {
    disableValidation: false,
    isMove: false,
    displayDateFields: true
  }
)

const emit = defineEmits<{
  (e: 'update:form', v: LocationObservationDTO): void
}>()

// v-model replacement of Vue2 default prop + direct mutation
const formState = ref<LocationObservationDTO>(props.form ?? emptyLocationForm())

watch(
  () => props.form,
  (v) => {
    formState.value = v ?? emptyLocationForm()
    updateRequiredProps()
  },
  { deep: true }
)

watch(
  formState,
  (v) => emit('update:form', v),
  { deep: true }
)

// computed : affichage des dates
const doShowDateFields = computed(() => props.displayDateFields !== false)

// state
const locationIsInstant = ref(true)
const fromRequired = ref(false)
const toRequired = ref(false)

onMounted(() => {
  locationIsInstant.value = !formState.value.startDate
  updateRequiredProps()
})

// règle "required"
function updateRequiredProps() {
  const f = formState.value

  // logique reprise du Vue2 (+ léger fix : toRequired doit pouvoir redevenir false)
  if (
    f.from == undefined &&
    f.to == undefined &&
    f.geojson !== undefined &&
    f.geojson !== ''
  ) {
    fromRequired.value = false
    toRequired.value = false
    return
  }

  // si rien n'est rempli, rien n'est requis
  if (f.from == undefined && f.to == undefined && (f.geojson == undefined || f.geojson === '')) {
    fromRequired.value = false
    toRequired.value = false
    return
  }

  // dès qu'on commence un move / relation from-to, on force le "to"
  toRequired.value = true

  // "From devient requis quand To est complété"
  fromRequired.value = f.to != undefined
}

// needed for Wizard
function getEmptyForm() {
  return emptyLocationForm()
}

function reset() {
  // if needed
}

async function validate() {
  // a retravailler apres ajout lib wkt et formulaire finalisé
  // if disableValidation -> always ok
  if (props.disableValidation) return true
  return true
}

defineExpose({
  getEmptyForm,
  reset,
  validate
})
</script>

<style scoped lang="scss"></style>

<i18n>
en:
  LocationForm:
    update: Update location,
    positionTitle: Position,
    x-help: "1th dimension of a custom coordinate system"
    x-placeholder: "A or 10 or 10.5"
    y-help: "2nd dimension of a custom coordinate system"
    y-placeholder: "A or 10 or 10.5"
    z-placeholder: "A or 10 or 10.5"
    z-help: "3rd dimension of a custom coordinate system"
    textual-position-placeholder: Beside the left door
    textual-position-help: Description of the position with text
    from: From
    from-placeholder: dev-infra:greenHouseA
    from-help: "Starting facility URI (The facility must exists)."
    to: To
    to-placeholder: dev-infra:greenHouseB
    to-help: "Arrival facility URI (The facility must exists)."
    geometry: POINT (10 10) or POLYGON((4 5, 0 55, 100 78, 4 5))
fr:
  LocationForm:
    update: Modifier la localisation
    positionTitle: Position
    x-help: "1ère dimension d'un système de coordonnées"
    x-placeholder: "A ou 10 ou 10,5"
    y-help: "2ème dimension d'un système de coordonnées"
    y-placeholder: "A ou 10 ou 10,5"
    z-placeholder: "A ou 10 ou 10,5"
    z-help: "3ème dimension d'un système de coordonnées"
    textual-position-placeholder: "à côté de la porte gauche"
    textual-position-help: Description textuelle de la position
    from: De
    from-placeholder: dev-infra:serreA
    from-help: "URI de l'installation environnementale de départ (L'installation doit exister)."
    to: Vers
    to-placeholder: dev-infra:serreB
    to-help: "URI de l'installation environnementale d'arrivée (L'installation doit exister)."
    geometry: POINT (10 10) ou POLYGON((4 5, 0 55, 100 78, 4 5))
</i18n>
