<template>
  <opensilex-WizardForm
    ref="wizardRef"
    :steps="steps"
    :createTitle="t('FacilityModalForm.add')"
    :editTitle="t('FacilityModalForm.update')"
    icon="bi#bi-map"
    :createAction="callOrganizationFacilityCreation"
    :updateAction="callOrganizationFacilityUpdate"
    @onCreate="(e) => emit('onCreate', e)"
    @onUpdate="(e) => emit('onUpdate', e)"
    :initForm="getEmptyForm"
    modalSize="lg"
  />
</template>

<script setup lang="ts">
import { inject, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import DTOConverter from './../../models/DTOConverter'

import type { OrganizationsService } from 'opensilex-core/api/organizations.service'
import type { LocationsService } from 'opensilex-core/api/locations.service'
import type {
  LocationObservationDTO,
  FacilityCreationDTO,
  FacilityGetDTO,
  FacilityUpdateDTO,
  UserGetDTO
} from 'opensilex-core/index'

const { t } = useI18n()

const emit = defineEmits<{
  (e: 'onCreate', v: any): void
  (e: 'onUpdate', v: any): void
}>()

const props = withDefaults(defineProps<{
  initForm?: (dto: FacilityCreationDTO) => void
}>(), {
  initForm: () => {}
})

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

const organizationsService = $opensilex.getService<OrganizationsService>('opensilex-core.OrganizationsService')
const locationsService = $opensilex.getService<LocationsService>('opensilex-core.LocationsService')

const wizardRef = ref<any>(null)

const steps = [
  {
    component: 'opensilex-FacilityForm',
    title: t('FacilityModalForm.stepOne')
  },
  {
    component: 'opensilex-LocationsForm',
    title: t('FacilityModalForm.stepTwo')
  }
]

function getEmptyForm() {
  const empty: any = {
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
  props.initForm?.(empty)
  return empty
}

function flattenRelations(relations: any[]) {
  const flattenedRelations: any[] = []

  for (const relation of relations || []) {
    if (relation?.value == null) continue

    if (Array.isArray(relation.value)) {
      for (const singleValue of relation.value) {
        flattenedRelations.push({
          property: relation.property,
          value: singleValue
        })
      }
    } else {
      flattenedRelations.push(relation)
    }
  }
  return flattenedRelations
}


function showCreateForm() {
  wizardRef.value?.showCreateForm?.()
}

async function showEditForm(uri: string) {
  // fallback pour ouvrir
  const fallback: any = getEmptyForm()
  fallback.uri = uri
  fallback.locations = []

  try {
    const http = await organizationsService.getFacility(uri) as any
    const dto: FacilityGetDTO = http.response.result
    const publisher: UserGetDTO = (dto as any).publisher

    const editDto = DTOConverter.extractURIFromResourceProperties<FacilityGetDTO, FacilityUpdateDTO>(dto)
    ;(editDto as any).publisher = publisher

    try {
      const httpLoc = await locationsService.searchLocationHistory(
        uri, undefined, undefined, [], 0, 0
      ) as any
      ;(editDto as any).locations = (httpLoc.response.result as LocationObservationDTO[]) ?? []
    } catch (e) {
      ;(editDto as any).locations = []
      $opensilex.errorHandler(e)
    }

    wizardRef.value?.showEditForm?.(editDto)
  } catch (e) {
    // on ouvre quand même
    $opensilex.errorHandler(e)
    wizardRef.value?.showEditForm?.(fallback)
  }
}

async function callOrganizationFacilityCreation(form: FacilityCreationDTO) {
  ;(form as any).relations = flattenRelations((form as any).relations)
  try {
    await organizationsService.createFacility(form as any)
    $opensilex.showSuccessToast(
      `${t('OrganizationFacilityForm.name')} ${form.name} ${t('component.common.success.creation-success-message')}`
    )
  } catch (error: any) {
    if (error?.status === 409) {
      $opensilex.errorHandler(error, t('OrganizationFacilityForm.organization-facility-already-exists'))
    } else {
      $opensilex.errorHandler(error)
    }
    throw error
  }
}

async function callOrganizationFacilityUpdate(form: FacilityUpdateDTO) {
  ;(form as any).relations = flattenRelations((form as any).relations)
  try {
    await organizationsService.updateFacility(form as any)
    $opensilex.showSuccessToast(
      `${t('OrganizationFacilityForm.name')} ${form.name} ${t('component.common.success.update-success-message')}`
    )
  } catch (e) {
    $opensilex.errorHandler(e)
    throw e
  }
}

defineExpose({ showCreateForm, showEditForm })
</script>

<i18n>
en:
  FacilityModalForm:
    update: Update facility
    add: Add facility
    stepOne: Informations
    stepTwo: Positions

fr:
  FacilityModalForm:
    update: Modifier l'installation environnementale
    add: Ajouter une installation environnementale
    stepOne: Informations
    stepTwo: Positions
</i18n>

