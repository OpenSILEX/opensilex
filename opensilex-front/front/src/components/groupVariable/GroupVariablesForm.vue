<template>
  <Modal ref="modalRef">
    <template #header>
      <FormHeader :title="modalFormLogic.formTitle.value" icon="fa#layer-group" />
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
          :editMode="modalFormLogic.editMode.value"
          :helpMessage="t('component.common.uri-help-message')"
          label="component.group.group-uri"
        />
      </n-form-item>

      <!-- Name -->
      <n-form-item path="name">
        <InputForm
          v-model:value="modalFormLogic.form.value.name"
          :label="t('component.common.name')"
          type="text"
          :required="true"
          :placeholder="t('component.group.form-name-placeholder')"
        />
      </n-form-item>

      <!-- Description -->
      <n-form-item path="description">
        <TextAreaForm
          v-model:value="modalFormLogic.form.value.description"
          label="component.common.description"
          :placeholder="t('component.group.form-description-placeholder')"
          @keydown.enter.stop
        />
      </n-form-item>

      <!-- Variables -->
      <VariableSelectorWithFilter
        ref="variablesSelectorRef"
        v-model:variables-with-labels="variablesWithLabels"
        v-model:variables="modalFormLogic.form.value.variables"
        :editMode="modalFormLogic.editMode.value"
        :label="t('component.variable.title')"
        :placeholder="t('component.variable.placeholder-multiple')"
        @hideSelector="$emit('hideSelector')"
        @shownSelector="$emit('shownSelector')"
      />
    </n-form>

    <template #footer>
      <FormFooter @cancel="modalFormLogic.hide" @submit="modalFormLogic.submit" />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { ref, computed, inject, useTemplateRef, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { NForm, NFormItem } from 'naive-ui'
import { requiredTrimmed } from '@/models/FormFieldsFormatter'

import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { VariablesService } from 'opensilex-core'
import type { VariablesGroupCreationDTO } from 'opensilex-core/model/variablesGroupCreationDTO'
import HttpResponse, { OpenSilexResponse } from '@/lib/HttpResponse'

import Modal from '@/components/common/views/Modal.vue'
import FormHeader from '@/components/common/forms/FormHeader.vue'
import FormFooter from '@/components/common/forms/FormFooter.vue'
import UriForm from '@/components/common/forms/UriForm.vue'
import InputForm from '@/components/common/forms/InputForm.vue'
import TextAreaForm from '@/components/common/forms/TextAreaForm.vue'
import VariableSelectorWithFilter from '@/components/variables/views/VariableSelectorWithFilter.vue'
import useModalFormLogic from '@/composables/useModalFormLogic'

//#region Public
const emit = defineEmits<{
  onUpdate: [payload: HttpResponse<OpenSilexResponse>],
  onCreate: [payload: HttpResponse<OpenSilexResponse>],
  onSuccess
}>()

const props = defineProps<{
  createTitle: string
  editTitle: string
}>()
//#endregion

//#region Private

//#region Plugin and services
const { t } = useI18n()
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = opensilex.getService<VariablesService>('opensilex-core.VariablesService')
//#endregion

const modalRef = useTemplateRef<InstanceType<typeof Modal>>('modalRef')
const formRef = useTemplateRef<InstanceType<typeof NForm>>('formRef')
const variablesSelectorRef = ref<InstanceType<typeof VariableSelectorWithFilter> | null>(null)

//#region Datas
const uriGenerated = ref(true)
const variablesWithLabels = ref<Array<{ id: string; label: string }>>([])
//#endregion

//#region Computed
const rules = computed(() => ({
  name: requiredTrimmed('component.common.name')
}))
//#endregion

//#region modalFormLogic composable
const modalFormLogic = useModalFormLogic<VariablesGroupCreationDTO>({
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

function getEmptyForm(): VariablesGroupCreationDTO {
  return {
    uri: null,
    name: null,
    description: null,
    variables: []
  }
}

function reset(): void {
  uriGenerated.value = true
  variablesSelectorRef.value?.setVariableSelectorToFirstTimeOpen?.()
}

async function create(formData: VariablesGroupCreationDTO) {
  return await service.createVariablesGroup(formData)
}

async function update(formData: VariablesGroupCreationDTO) {
  return await service.updateVariablesGroup(formData)
}

function showCreateForm(initialData?: VariablesGroupCreationDTO & { __variablesWithLabels?: Array<{ id: string; label: string }> }) {
  if (initialData?.__variablesWithLabels) {
    variablesWithLabels.value = initialData.__variablesWithLabels
  }
  modalFormLogic.showCreateForm(initialData)
}

function showEditForm(formData: any) {
  if (formData?.__variablesWithLabels) {
    variablesSelectorRef.value?.setVariableSelectorToFirstTimeOpen?.()
    variablesWithLabels.value = formData.__variablesWithLabels  ?? []
  }
  modalFormLogic.showEditForm(formData)
}

//#endregion

//#endregion

defineExpose({
  showCreateForm,
  showEditForm
})
</script>

<style scoped lang="scss">
</style>
