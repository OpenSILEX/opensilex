<template>
  <opensilex-WizardForm
    ref="wizardRef"
    :steps="steps"
    :createTitle="t('ProjectForm.create')"
    :editTitle="t('ProjectForm.update')"
    icon="ik#ik-folder"
    modalSize="lg"
    :initForm="getEmptyForm"
    :createAction="create"
    :updateAction="update"
  >
  <template #icon />
  </opensilex-WizardForm>
</template>

<script setup lang="ts">
import { ref, inject } from 'vue'
import { useI18n } from 'vue-i18n'

import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import { ProjectsService } from 'opensilex-core/index'

import type HttpResponse from 'opensilex-security/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'
import type { ProjectCreationDTO } from 'opensilex-core/model/projectCreationDTO'

const emit = defineEmits<{
  (e: 'onCreate', form: ProjectCreationDTO): void
  (e: 'onUpdate', form: ProjectCreationDTO): void
}>()

const { t } = useI18n()
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const wizardRef = ref<any>(null)

const steps = [
  { component: 'opensilex-ProjectForm1', title: t('ProjectForm.step1-title') },
  { component: 'opensilex-ProjectForm2', title: t('ProjectForm.step2-title') }
]

function getEmptyForm(): ProjectCreationDTO {
  return {
    uri: null,
    name: '',
    shortname: undefined,
    financial_funding: undefined,
    description: undefined,
    objective: undefined,
    start_date: '',
    end_date: undefined,
    website: undefined,
    experiments: undefined,
    administrative_contacts: [],
    coordinators: [],
    scientific_contacts: [],
    related_projects: []
  }
}

function showCreateForm() {
  wizardRef.value?.showCreateForm?.()
}

function showEditForm(form: ProjectCreationDTO) {
  // copie pour éviter d’éditer la source
  const copy: ProjectCreationDTO = JSON.parse(JSON.stringify(form))
  wizardRef.value?.showEditForm?.(copy)
}

/** Create */
async function create(form: ProjectCreationDTO) {
  try {
    const service = $opensilex.getService<ProjectsService>('opensilex.ProjectsService')
    const http: HttpResponse<OpenSilexResponse<string>> =
      await service.createProject(form)

    const uri = http.response.result
    form.uri = uri

    emit('onCreate', form)

    const message =
      `${t('ProjectList.name')} ${form.name} ${t('component.common.success.creation-success-message')}`
    $opensilex.showSuccessToast(message)
    return true
  } catch (error: any) {
    if (error?.status === 409) {
      $opensilex.errorHandler(error, t('ProjectForm.project-already-exists'))
    } else {
      $opensilex.errorHandler(error)
    }
    return false
  }
}

/** Update */
async function update(form: ProjectCreationDTO) {
  try {
    if (form.website === '') form.website = undefined

    const service = $opensilex.getService<ProjectsService>('opensilex.ProjectsService')
    const http: HttpResponse<OpenSilexResponse<string>> =
      await service.updateProject(form)

    const uri = http.response.result
    console.debug('project updated', uri)

    emit('onUpdate', form)

    const message =
      `${t('ProjectList.name')} ${form.name} ${t('component.common.success.update-success-message')}`
    $opensilex.showSuccessToast(message)
    return true
  } catch (error: any) {
    $opensilex.errorHandler(error)
    return false
  }
}

defineExpose({
  showCreateForm,
  showEditForm,
  getEmptyForm
})
</script>

<i18n>
en:
  ProjectForm:
    create: Add project
    update: Update the project
    project-already-exists: Project already exists
    step1-title: Informations
    step2-title: Détails

fr:
  ProjectForm:
    create: Ajouter un projet
    update: Modifier le projet
    project-already-exists: Le projet existe déjà
    step1-title: Informations
    step2-title: Details
</i18n>
