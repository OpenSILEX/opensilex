<template>
  <div>
    <!-- URI -->
    <UriForm
      v-model:uri="facility.uri"
      :label="t('FacilityForm.uri-label')"
      helpMessage="component.common.uri-help-message"
      :editMode="editMode"
      v-model:generated="uriGenerated"
    />

    <!-- Type -->
    <TypeForm
      v-if="baseType"
      v-model:type="facility.rdf_type"
      :baseType="baseType"
      :ignoreRoot="false"
      :required="true"
      :disabled="editMode"
      :placeholder="t('FacilityForm.form-type-placeholder')"
      @select="(e) => typeSwitch(e.id, false)"
    />
    <div v-if="typeError" class="field-error">
      {{ typeError }}
    </div>

    <!-- Name -->
    <InputForm
      v-model:value="facility.name"
      label="component.common.name"
      type="text"
      :required="true"
      :placeholder="t('FacilityForm.form-name-placeholder')"
    />
    <div v-if="nameError" class="field-error">
      {{ nameError }}
    </div>

    <!-- Description -->
    <TextAreaForm
      v-model:value="facility.description"
      label="component.common.description"
      :placeholder="t('component.common.description')"
      @keydown.enter.stop
    />

    <!-- Group of variables -->
    <GroupVariablesSelector
      :label="t('component.variable.groupVariable.groupVariable')"
      v-model:variableGroup="facility.variableGroups"
      :multiple="true"
    />

    <!-- Custom properties -->
    <OntologyRelationsForm
      ref="ontologyRelationsFormRef"
      :rdfType="facility.rdf_type"
      :relations="facility.relations"
      :baseType="baseType"
      :editMode="editMode"
    />

    <slot v-if="facility.rdf_type" :form="facility" />

    <!-- Organizations -->
    <OrganizationSelector
      label="component.experiment.organizations"
      v-model:organizations="facility.organizations"
      :multiple="true"
    />

    <!-- Site -->
    <SiteSelector
      label="component.common.organization.site"
      :multiple="true"
      v-model:siteURIs="facility.sites"
    />

    <!-- Warning if more than one site -->
    <div
      v-if="Array.isArray(facility.sites) && facility.sites.length > 1"
      class="alert alert-warning"
      role="alert"
    >
      {{ t('component.facility.warning.facility-should-have-unique-site') }}
    </div>

    <!-- Address toggle -->
    <div class="form-check form-switch mt-2">
      <input
        class="form-check-input"
        type="checkbox"
        role="switch"
        id="toggleAddress"
        v-model="hasAddress"
        @change="onAddressToggled"
      />
      <label class="form-check-label" for="toggleAddress">
        {{ t('FacilityForm.toggleAddress') }}
      </label>
    </div>

    <!-- Address -->
    <AddressForm v-model:address="facility.address" />

    <br />
  </div>
</template>

<script setup lang="ts">
import { inject, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { FacilityCreationDTO } from 'opensilex-core/index'
import type OntologyRelationsForm from '@/components/ontology/OntologyRelationsForm.vue'
import UriForm from "@/components/common/forms/UriForm.vue";
import TypeForm from "@/components/common/forms/TypeForm.vue";
import InputForm from "@/components/common/forms/InputForm.vue";
import TextAreaForm from "@/components/common/forms/TextAreaForm.vue";
import GroupVariablesSelector from "@/components/groupVariable/GroupVariablesSelector.vue";
import OrganizationSelector from "@/components/organizations/OrganizationSelector.vue";
import SiteSelector from "@/components/organizations/site/SiteSelector.vue";
import AddressForm from "@/components/common/forms/AddressForm.vue";

function emptyFacilityForm(): FacilityCreationDTO {
  return {
    uri: undefined,
    rdf_type: undefined,
    name: undefined,
    description: undefined,
    address: undefined,
    organizations: [],
    sites: [],
    variableGroups: [],
    relations: [],
    locations: []
  }
}

const { t } = useI18n()

const props = withDefaults(
  defineProps<{
    editMode?: boolean
    uriGenerated?: boolean
    form?: FacilityCreationDTO
  }>(),
  {
    editMode: false,
    uriGenerated: true
  }
)

const emit = defineEmits<{
  (e: 'update:form', v: FacilityCreationDTO): void
  (e: 'update:uriGenerated', v: boolean): void
}>()

// v-model
const facility = ref<FacilityCreationDTO>(props.form ?? emptyFacilityForm())

watch(
  () => props.form,
  (v) => {
    facility.value = v ?? emptyFacilityForm()
  },
  { deep: true }
)

watch(
  facility,
  (v) => emit('update:form', v),
  { deep: true }
)

// v-model uriGenerated
const uriGenerated = ref<boolean>(props.uriGenerated)
watch(() => props.uriGenerated, (v) => (uriGenerated.value = v))
watch(uriGenerated, (v) => emit('update:uriGenerated', v))

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

const baseType = ref('')
const hasAddress = ref(false)
const ontologyRelationsFormRef = ref<InstanceType<typeof OntologyRelationsForm> | any>(null)

watch(
  facility,
  () => {
    hasAddress.value = !!facility.value.address
    resetTypeModel()
  },
  { deep: true }
)

function onAddressToggled() {
  facility.value.address = hasAddress.value ? ({} as any) : undefined
}

function reset() {}


const nameError = ref('')
const typeError = ref('')

function validate() {
  nameError.value = ''
  typeError.value = ''

  const name = (facility.value.name ?? '').toString().trim()
  if (!name) {
    nameError.value = t('validations.required_if', {
      _field_: t('component.common.name') as string
    }) as string
  }

  if (!facility.value.rdf_type) {
    typeError.value = t('validations.required_if', {
      _field_: t('FacilityForm.type-label') as string
    }) as string
  }

  return !nameError.value && !typeError.value
}

function typeSwitch(type: string, initialLoad: boolean) {
  ontologyRelationsFormRef.value?.typeSwitch?.(type, initialLoad)
}

function getEmptyForm() {
  return emptyFacilityForm()
}

function resetTypeModel() {}

onMounted(() => {
  baseType.value = $opensilex.Oeso.FACILITY_TYPE_URI
  hasAddress.value = !!facility.value.address
})

defineExpose({ reset, validate, typeSwitch, getEmptyForm })
</script>

<style scoped></style>

<i18n>
en:
  FacilityForm:
    toggleAddress: Address
    uri-label: Object URI
    type-label: Object Type
    form-name-placeholder: Enter object name
    form-type-placeholder: Select object type

fr:
  FacilityForm:
    toggleAddress: Adresse
    uri-label: URI de l'objet
    type-label: Type de l'objet
    form-name-placeholder: Saisir le nom de l'objet
    form-type-placeholder: Sélectionner le type de l'objet
</i18n>
