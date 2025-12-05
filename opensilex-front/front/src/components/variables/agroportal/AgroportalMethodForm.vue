<template>
  <opensilex-AgroportalCreateForm
    ref="createForm"
    @onCreate="(e) => emit('onCreate', e)"
    @onUpdate="(e) => emit('onUpdate', e)"
    ontologiesConfig="entityOntologies"
    :searchPlaceholder="t('AgroportalMethodForm.name-placeholder')"
    :createTitle="t('AgroportalMethodForm.add')"
    :editTitle="t('AgroportalMethodForm.edit')"
    :createMethod="createMethod"
    :updateMethod="updateMethod"
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
const createMethod = (payload: any) => service.createMethod(payload)
const updateMethod = (payload: any) => service.updateMethod(payload)

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
  AgroportalMethodForm:
    add: Add method
    edit: Edit method
    name-placeholder: Image analysis
fr:
  AgroportalMethodForm:
    add: Ajouter une méthode
    edit: Modifier une méthode
    name-placeholder: Analyse d'image
</i18n>
