<template>
  <opensilex-ModalForm
    v-if="loadForm"
    ref="groupFormRef"
    modal-size="lg"
    :component="formComponent"
    :createTitle="'component.variable.groupVariable.add-groupVariable'"
    :editTitle="'component.variable.groupVariable.edit'"
    icon="fa#layer-group"
    :create-action="create"
    :update-action="update"
    :success-message="successMessage"
    :key="key"
    @onCreate="onCreate"
    @onUpdate="(group) => emit('onUpdate', group)"
  />
</template>

<script setup lang="ts">
import { ref, computed, nextTick, inject, onMounted } from 'vue'
import { useStore } from 'vuex'
import { useI18n } from 'vue-i18n'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import ModalForm from '@/components/common/forms/ModalForm.vue'
import GroupVariablesForm from './../../groupVariable/GroupVariablesForm.vue'
import {
  VariablesService,
  VariablesGroupCreationDTO,
  VariablesGroupUpdateDTO,
  VariablesGroupGetDTO
} from 'opensilex-core'

const emit = defineEmits<{
  (e: 'ready'): void
  (e: 'onCreate', group: VariablesGroupCreationDTO): void
  (e: 'onUpdate', group: VariablesGroupUpdateDTO): void
}>()

const store = useStore()
const { t } = useI18n()
const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const variablesService = opensilex.getService<VariablesService>("opensilex.VariablesService");


const groupFormRef = ref<InstanceType<typeof ModalForm>>()
const formComponent = GroupVariablesForm

const loadForm = ref(false)
const key = ref(1)

const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)

onMounted(() => emit('ready'))

function refresh() {
  key.value++
}

function showCreateForm() {
  refresh()
  loadForm.value = true
  nextTick(() => {
    groupFormRef.value?.showCreateForm()
  })
}

async function showEditForm(uri: string) {
  refresh()
  loadForm.value = true
  try {
    const http = await variablesService?.getVariablesGroup(uri)
    const form: VariablesGroupGetDTO | undefined = http?.response?.result
    if (!form) throw new Error('Groupe introuvable')
    nextTick(() => groupFormRef.value?.showEditForm(form))
  } catch (e) {
    opensilex?.errorHandler(e)
  }
}

async function create(group: VariablesGroupCreationDTO) {
  try {
    const http = await variablesService?.createVariablesGroup(group)
    const createdUri = http?.response?.result?.toString?.() ?? http?.response?.result
    if (!createdUri) throw new Error('Réponse sans URI')

    const msg = `${group.name} ${t('component.common.success.creation-success-message')}`
    opensilex?.showSuccessToast(msg)
    emit('onCreate', group)
    return group
  } catch (error: any) {
    opensilex?.errorHandler(error)
    return false
  }
}

async function update(group: VariablesGroupUpdateDTO) {
  try {
    const http = await variablesService?.updateVariablesGroup(group)
    const updatedUri = http?.response?.result
    if (!updatedUri) throw new Error('Réponse sans URI')
    const msg = `${group.name} ${t('component.common.success.update-success-message')}`
    opensilex?.showSuccessToast(msg)
    emit('onUpdate', group)
    return group
  } catch (error: any) {
    opensilex?.errorHandler(error)
    return false
  }
}

function successMessage(group: VariablesGroupCreationDTO) {
  // texte utilisé par ModalForm pour indiquer que l’élément créé/mis à jour
  return `${t('VariableView.groupVariable')} ${group.name}`
}

defineExpose({
  showCreateForm,
  showEditForm
})
</script>
