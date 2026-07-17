<template>
  <div>
    <!-- URI -->
    <UriForm
      v-model:uri="formState.uri"
      label="component.common.uri"
      helpMessage="component.common.uri-help-message"
      :editMode="editMode"
      v-model:generated="uriGenerated"
    />

    <!-- Name -->
    <InputForm
      v-model:value="formState.name"
      label="component.common.name"
      type="text"
      :required="true"
      :placeholder="t('SiteForm.form-name-placeholder')"
    />

    <!-- Description -->
    <InputForm
      :value="formState.description"
      label="component.common.description"
      type="text"
      :placeholder="t('component.common.description')"
    />

    <!-- Organizations -->
    <OrganizationSelector
      ref="organizationSelectorRef"
      :label="t('SiteForm.organizations')"
      v-model:organizations="formState.organizations"
      :multiple="true"
      :required="true"
      checkStrategy="all"
    />

    <!-- Facilities -->
    <FacilitySelector
      :label="t('SiteForm.facilities')"
      v-model:facilities="formState.facilities"
      :multiple="true"
    />

    <!-- Groups -->
    <GroupSelector
      :label="t('SiteForm.groups')"
      v-model:groups="formState.groups"
      :multiple="true"
      :helpMessage="t('SiteForm.groups-help-message')"
    />

    <!-- Address toggle (Bootstrap switch) -->
    <div class="form-check form-switch my-2">
  <input
    class="form-check-input"
    type="checkbox"
    role="switch"
    id="site-address-toggle"
    :checked="hasAddress"
    @change="toggleAddress"
  />
  <label class="form-check-label" for="site-address-toggle">
    {{ t('SiteForm.toggleAddress') }}
  </label>
</div>

    <!-- Address -->
    <AddressForm v-if="hasAddress" v-model:address="formState.address" />
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { OrganizationsService } from 'opensilex-core/api/organizations.service'
import type { SiteCreationDTO } from 'opensilex-core/index'
import type { SiteUpdateDTO } from 'opensilex-core/model/siteUpdateDTO'
import type HttpResponse from '@/lib/HttpResponse'
import type { OpenSilexResponse } from '@/lib/HttpResponse'
import UriForm from "@/components/common/forms/UriForm.vue";
import InputForm from "@/components/common/forms/InputForm.vue";
import OrganizationSelector from "@/components/organizations/OrganizationSelector.vue";
import FacilitySelector from "@/components/facilities/FacilitySelector.vue";
import GroupSelector from "@/components/groups/GroupSelector.vue";
import AddressForm from "@/components/common/forms/AddressForm.vue";

const { t } = useI18n()
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

function emptySiteForm(): SiteCreationDTO {
  return {
    uri: undefined,
    rdf_type: undefined,
    name: "undefined",
    description: undefined,
    address: undefined,
    organizations: [],
    facilities: [],
    groups: []
  } as any
}

const props = defineProps<{
  editMode?: boolean
  form?: SiteCreationDTO
}>()

const formState = ref<SiteCreationDTO>(props.form ?? emptySiteForm())

// quand le Wizard remplace form (showCreateForm/showEditForm), on resynchronise la référence
watch(
  () => props.form,
  (v) => {
    formState.value = v ?? emptySiteForm()
  },
  { deep: false }
)

const editMode = computed(() => !!props.editMode)
const uriGenerated = ref(true)

// toggle dérivé du form
const hasAddress = computed(() => !!formState.value.address)

function toggleAddress(event: Event) {
  const checked = (event.target as HTMLInputElement).checked
  formState.value.address = checked
    ? (formState.value.address ?? {})
    : undefined
}


// refs
const organizationSelectorRef = ref<any>(null)

// Attendue par WizardForm/parents
function reset() {
  organizationSelectorRef.value?.reset?.()
}

function getEmptyForm(): SiteCreationDTO {
  return emptySiteForm()
}

async function create(form: SiteCreationDTO) {
  $opensilex.showLoader()
  form.organizations = null;
  try {
    const service = $opensilex.getService<OrganizationsService>('opensilex.OrganizationsService')
    const http = await service.createSit(form) as unknown as HttpResponse<OpenSilexResponse<string>>
    form.uri = (http as any).response.result
    return form
  } catch (error: any) {
    if (error?.status === 409) {
      $opensilex.errorHandler(error, t('SiteForm.siteAlreadyExists'))
    } else {
      $opensilex.errorHandler(error)
    }
    throw error
  } finally {
    $opensilex.hideLoader()
  }
}

async function update(form: SiteUpdateDTO) {
  $opensilex.enableLoader?.()
  $opensilex.showLoader()
  try {
    delete (form as any).rdf_type_name
    const service = $opensilex.getService<OrganizationsService>('opensilex.OrganizationsService')
    return await service.updateSite(form)
  } catch (e) {
    $opensilex.errorHandler(e as any)
    throw e
  } finally {
    $opensilex.hideLoader()
  }
}

defineExpose({ reset, create, update, getEmptyForm })
</script>


<style scoped></style>

<i18n>
en:
  SiteForm:
    organizations: Organizations
    facilities: Facilities
    groups: Groups
    groups-help-message: "Selected groups will have access to this site"
    toggleAddress: "Address"
    siteAlreadyExists: Site already exists
    form-name-placeholder: Enter object name
    form-type-placeholder: Select object type

fr:
  SiteForm:
    organizations: Organisations
    facilities: Installations environnementales
    groups: Groupes
    groups-help-message: "Les groupes sélectionnés auront accès à ce site"
    toggleAddress: "Adresse"
    siteAlreadyExists: Ce site existe déjà
    form-name-placeholder: Saisir le nom de l'objet
    form-type-placeholder: Sélectionner le type de l'objet
</i18n>
