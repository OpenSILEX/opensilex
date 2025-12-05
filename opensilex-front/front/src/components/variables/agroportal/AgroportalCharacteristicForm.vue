<template>
  <opensilex-AgroportalCreateForm
    ref="createForm"
    @onCreate="(e) => emit('onCreate', e)"
    @onUpdate="(e) => emit('onUpdate', e)"
    ontologiesConfig="entityOntologies"
    :searchPlaceholder="t('AgroportalCharacteristicForm.name-placeholder')"
    :createTitle="t('AgroportalCharacteristicForm.add')"
    :editTitle="t('AgroportalCharacteristicForm.edit')"
    :createMethod="createCharacteristic"
    :updateMethod="updateCharacteristic"
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
const createCharacteristic = (payload: any) => service.createCharacteristic(payload)
const updateCharacteristic = (payload: any) => service.updateCharacteristic(payload)

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
  AgroportalCharacteristicForm:
    add: Add characteristic
    edit: Edit characteristic
    name-placeholder: Height
fr:
  AgroportalCharacteristicForm:
    add: Ajouter une caractéristique
    edit: Modifier une caractéristique
    name-placeholder: Hauteur
</i18n>
