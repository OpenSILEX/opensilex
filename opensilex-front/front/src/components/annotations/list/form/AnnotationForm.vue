<template>
  <Modal ref="modalRef">
    <template #header>
      <FormHeader :title="modalFormLogic.formTitle.value" icon="fa#vials" />
    </template>

    <n-form
      ref="formRef"
      :model="modalFormLogic.form.value"
      :rules="rules"
      label-placement="top"
      :show-require-mark="true"
      size="large"
    >
      <!-- URI -->
      <n-form-item>
        <UriForm
          v-model:uri="modalFormLogic.form.value.uri"
          :generated="uriGenerated"
          @update:generated="val => uriGenerated = val"
          :editMode="modalFormLogic.isEditMode.value"
          :helpMessage="t('component.common.uri-help-message')"
          label="component.common.uri"
        />
      </n-form-item>

      <!-- Motivation -->
        <FormSelector
          path="motivation"
          v-model:selected="modalFormLogic.form.value.motivation"
          :required="true"
          :multiple="false"
          :options="motivations"
          :itemLoadingMethod="loadMotivation"
          :label="'component.annotation.motivation'"
          :noResultsText="t('component.annotation.no-motivation')"
          :helpMessage="t('component.annotation.motivation-help')"
          :placeholder="t('component.annotation.select-motivation')"
        />

      <!-- Description -->
      <n-form-item path="description">
        <TextAreaForm
          v-model:value="modalFormLogic.form.value.description"
          :label="t('component.annotation.description')"
          :required="true"
          @keydown.enter.stop
          :placeholder="t('component.common.set-description')"
        />
      </n-form-item>
    </n-form>

    <template #footer>
      <FormFooter @cancel="modalFormLogic.hide" @submit="modalFormLogic.submit" />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { ref, computed, inject, onMounted, onBeforeUnmount, watch, useTemplateRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { useStore } from 'vuex'
import { NForm, NFormItem } from 'naive-ui'
import {requiredObjectOrLists, requiredTrimmed} from '@/models/FormFieldsFormatter'

import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import HttpResponse, { OpenSilexResponse } from 'opensilex-core/HttpResponse'
import type { AnnotationsService } from 'opensilex-core/api/annotations.service'
import type { AnnotationCreationDTO, NamedResourceDTO } from 'opensilex-core/index'

import UriForm from '@/components/common/forms/UriForm.vue'
import FormSelector from '@/components/common/forms/FormSelector.vue'
import TextAreaForm from '@/components/common/forms/TextAreaForm.vue'
import FormHeader from '@/components/common/forms/FormHeader.vue'
import FormFooter from '@/components/common/forms/FormFooter.vue'
import Modal from '@/components/common/views/Modal.vue'
import useModalFormLogic from '@/composables/useModalFormLogic'
import {AnnotationUpdateDTO} from "opensilex-core/model/annotationUpdateDTO";

//#region Public
const emit = defineEmits<{
  (e: 'onUpdate', payload: HttpResponse<OpenSilexResponse>): void
  (e: 'onCreate', payload: HttpResponse<OpenSilexResponse>): void
  (e: 'onSuccess'): void
}>()

const props = defineProps<{
  createTitle: string
  editTitle: string
}>()
//#endregion

//#region Private

//#region Plugin and services
const { t } = useI18n()
const store = useStore()
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = opensilex.getService<AnnotationsService>('opensilex.AnnotationsService')
//#endregion

const modalRef = useTemplateRef<InstanceType<typeof Modal>>('modalRef')
const formRef = useTemplateRef<InstanceType<typeof NForm>>('formRef')

//#region Datas
const uriGenerated = ref<boolean>(true)
const motivations = ref<Array<{ label: string; id: string }>>([])
//#endregion

//#region Computed
const rules = computed(() => ({
  motivation: requiredObjectOrLists("component.annotation.motivation"),
  description: requiredTrimmed('component.common.description')
}))
//#endregion

//#region modalFormLogic composable
const modalFormLogic = useModalFormLogic<AnnotationCreationDTO>({
  modalRef,
  nFormRef: formRef,
  getEmptyForm,
  create,
  update,
  reset,
  addTitle: props.createTitle,
  editTitle: props.editTitle,
  onCreate: (res) => emit('onCreate', res),
  onUpdate: (res) => emit('onUpdate', res),
  onSuccess: () => emit('onSuccess'),
})
//#endregion

//#region Methods

function getEmptyForm(): AnnotationCreationDTO {
  return {
    uri: undefined,
    motivation: undefined,
    targets: [],
    description: undefined
  }
}

async function reset(): Promise<void> {
  uriGenerated.value = true
}

async function create(formData: AnnotationCreationDTO) {
  return await service.createAnnotation(formData)
}

async function update(formData: AnnotationUpdateDTO) {
  return await service.updateAnnotation(formData)
}

function showCreateForm(targetsArg: string[] = []) {
  const annotationForm = getEmptyForm()
  annotationForm.targets = targetsArg
  modalFormLogic.showCreateForm(annotationForm)
}

function showEditForm(form: any) {
  const normalized = JSON.parse(JSON.stringify(form))
  if (normalized?.motivation && typeof normalized.motivation === 'object') {
    normalized.motivation = normalized.motivation.uri
  }
  modalFormLogic.showEditForm(normalized)
}

function searchMotivations() {
  service.searchMotivations(undefined, ['name=asc'], undefined, undefined)
    .then((http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>) => {
      motivations.value = (http?.response?.result ?? []).map(m => ({ label: m.name, id: m.uri }))
    })
    .catch(opensilex.errorHandler)
}

function loadMotivation(selected: Array<any>): Array<any> | undefined {
  if (!selected || selected.length === 0) return undefined

  if (selected[0]?.uri) {
    const opt = { label: (modalFormLogic.form.value as any).motivation.name, id: (modalFormLogic.form.value as any).motivation.uri }
    ;(modalFormLogic.form.value as any).motivation = (modalFormLogic.form.value as any).motivation.uri
    return [opt]
  }

  const found = motivations.value.find(m => m.id === selected[0])
  return found ? [found] : undefined
}
//#endregion

//#region Watchers & lifecycle
const langUnwatch = watch(() => store.getters.language, () => searchMotivations())

onMounted(() => {
  searchMotivations()
})

onBeforeUnmount(() => {
  langUnwatch()
})
//#endregion

//#endregion

defineExpose({
  showCreateForm,
  showEditForm
})
</script>

<style scoped lang="scss">
</style>
