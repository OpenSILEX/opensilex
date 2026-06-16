<template>
  <div class="v-step-project-1">
    <!-- URI -->
    <div class="v-step-uri mb-3" :class="{ 'has-error': !!uriError }">
      <opensilex-UriForm
        v-model:uri="formDto.uri"
        v-model:generated="uriGenerated"
        label="component.project.project-uri"
        helpMessage="component.common.uri-help-message"
        :editMode="editMode"
        :required="true"
      />
    </div>

    <!-- Name -->
    <div class="v-step-name mb-3" :class="{ 'has-error': !!nameError }">
      <opensilex-InputForm
        v-model:value="formDto.name"
        label="component.project.name"
        type="text"
        :required="true"
        :placeholder="t('component.project.form-name-placeholder')"
      />
      <div v-if="nameError" class="field-error">
        {{ nameError }}
      </div>
    </div>

    <!-- Short name -->
    <opensilex-InputForm
      class="mb-3"
      v-model:value="formDto.shortname"
      label="component.project.shortname"
      type="text"
      :placeholder="t('component.project.form-shortname-placeholder')"
    />

    <!-- Period -->
    <div class="v-step-dates mb-3" :class="{ 'has-error': !!startDateError }">
      <opensilex-DateRangePickerForm
        v-model:start="formDto.start_date"
        v-model:end="formDto.end_date"
        labelStart="component.common.date-time.startDate"
        labelEnd="component.common.date-time.endDate"
        type="date"
        :requiredStart="true"
      />
      <div v-if="startDateError" class="field-error">
        {{ startDateError }}
      </div>
    </div>

    <!-- Financial funding -->
    <opensilex-InputForm
      class="mb-3"
      v-model:value="formDto.financial_funding"
      label="component.project.financialFunding"
      type="text"
      :placeholder="t('component.project.form-financialFunding-placeholder')"
    />

    <!-- Website -->
    <div class="v-step-website mb-3" :class="{ 'has-error': !!websiteError }">
      <opensilex-InputForm
        v-model:value="formDto.website"
        label="component.project.website"
        type="url"
        rules="url"
        :placeholder="t('component.project.form-website-placeholder')"
      />
      <div v-if="websiteError" class="field-error">
        {{ websiteError }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ProjectCreationDTO } from 'opensilex-core/index'

const props = defineProps<{
  form: ProjectCreationDTO
  editMode?: boolean
}>()

const { t } = useI18n({ useScope: 'local' })

const formDto = computed<ProjectCreationDTO>({
  get: () => props.form,
  set: (v) => Object.assign(props.form, v)
})

/** Etat local */
const uriGenerated = ref(true)

/** Erreurs */
const uriError = ref('')
const nameError = ref('')
const startDateError = ref('')
const websiteError = ref('')

/** Clear erreurs à la saisie*/
watch(
  () => formDto.value.uri,
  (v) => {
    if (uriError.value && (v ?? '').toString().trim()) uriError.value = ''
  }
)

watch(
  () => formDto.value.name,
  (v) => {
    if (nameError.value && (v ?? '').toString().trim()) nameError.value = ''
  }
)

watch(
  () => formDto.value.start_date,
  (v) => {
    if (startDateError.value && (v ?? '').toString().trim()) startDateError.value = ''
  }
)

watch(
  () => formDto.value.website,
  (v) => {
    // si l'utilisateur vide le champ, c'est OK
    if (websiteError.value) {
      const val = (v ?? '').toString().trim()
      if (!val) websiteError.value = ''
      else if (isValidUrl(val)) websiteError.value = ''
    }
  }
)

/** Helpers */
function isValidUrl(value: string): boolean {
  try {
    // accepte http(s)://...
    new URL(value)
    return true
  } catch {
    return false
  }
}

function reset() {
  uriGenerated.value = true
  uriError.value = ''
  nameError.value = ''
  startDateError.value = ''
  websiteError.value = ''
}

function validate(): boolean {
  uriError.value = ''
  nameError.value = ''
  startDateError.value = ''
  websiteError.value = ''

  // URI requise seulement si PAS générée
  const uri = (formDto.value.uri ?? '').toString().trim()
  if (!uri && !uriGenerated.value) {
    uriError.value = t('validations.required_if', {
      _field_: t('component.common.uri') as string
    }) as string
  }

  const name = (formDto.value.name ?? '').toString().trim()
  if (!name) {
    nameError.value = t('validations.required_if', {
      _field_: t('component.common.name') as string
    }) as string
  }

  const startDate = (formDto.value.start_date ?? '').toString().trim()
  if (!startDate) {
    startDateError.value = t('validations.required_if', {
      _field_: t('component.common.date-time.startDate') as string
    }) as string
  }

  const website = (formDto.value.website ?? '').toString().trim()
  if (website && !isValidUrl(website)) {
    websiteError.value = t('validations.url', {
      _field_: t('component.project.website') as string
    }) as string
  }

  return !uriError.value && !nameError.value && !startDateError.value && !websiteError.value
}


defineExpose({
  reset,
  validate
})
</script>

<style scoped>
.has-error .field-error {
  margin-top: 6px;
}
.field-error {
  font-size: 0.875rem;
  color: #d03050;
}
</style>
