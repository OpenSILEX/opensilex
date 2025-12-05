<template>
  <opensilex-AgroportalCreateForm
    ref="createForm"
    @onCreate="(e) => emit('onCreate', e)"
    @onUpdate="(e) => emit('onUpdate', e)"
    ontologiesConfig="entityOntologies"
    :searchPlaceholder="t('AgroportalEntityOfInterestForm.name-placeholder')"
    :createTitle="t('AgroportalEntityOfInterestForm.add')"
    :editTitle="t('AgroportalEntityOfInterestForm.edit')"
    :createMethod="createEntityOfInterest"
    :updateMethod="updateEntityOfInterest"
  />
</template>

<script setup lang="ts">
import { ref, inject } from 'vue'
import type OpenSilexVuePlugin from '../../../models/OpenSilexVuePlugin'
import { VariablesService } from 'opensilex-core/api/variables.service'
import { useI18n } from 'vue-i18n'

// events à propager vers le parent
const emit = defineEmits<{
  (e: 'onCreate', payload: any): void
  (e: 'onUpdate', payload: any): void
}>()


const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = opensilex.getService<VariablesService>('opensilex-core.VariablesService')
const { t } = useI18n({ useScope: 'local' })

// référence vers le sous-formulaire pour exposer ses méthodes
const createForm = ref<any>(null)

// méthodes passées au composant enfant
const createEntityOfInterest = (payload: any) => service.createInterestEntity(payload)
const updateEntityOfInterest = (payload: any) => service.updateInterestEntity(payload)

function showCreateForm() {
  createForm.value?.showCreateForm?.()
}
function showEditForm(dto: any) {
  createForm.value?.showEditForm?.(dto)
}
defineExpose({ showCreateForm, showEditForm })
</script>

<i18n>
en:
  AgroportalEntityOfInterestForm:
    add: Add entity of interest
    edit: Edit entity of interest
    name-placeholder: Canopy
fr:
  AgroportalEntityOfInterestForm:
    add: Ajouter une entité d'intérêt
    edit: Modifier une entité d'intérêt
    name-placeholder: Canopée
</i18n>
