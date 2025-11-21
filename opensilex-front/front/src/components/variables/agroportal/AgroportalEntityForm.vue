<template>
  <opensilex-AgroportalCreateForm
    ref="createForm"
    @onCreate="(e) => emit('onCreate', e)"
    @onUpdate="(e) => emit('onUpdate', e)"
    ontologiesConfig="entityOntologies"
    :searchPlaceholder="t('AgroportalEntityForm.name-placeholder')"
    :createTitle="t('AgroportalEntityForm.add')"
    :editTitle="t('AgroportalEntityForm.edit')"
    :createMethod="createEntity"
    :updateMethod="updateEntity"
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
const createEntity = (payload: any) => service.createEntity(payload)
const updateEntity = (payload: any) => service.updateEntity(payload)

// API exposée pour le parent
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
  AgroportalEntityForm:
    add: Add entity
    edit: Edit entity
    name-placeholder: Plant
fr:
  AgroportalEntityForm:
    add: Ajouter une entité
    edit: Modifier une entité
    name-placeholder: Plant
</i18n>
