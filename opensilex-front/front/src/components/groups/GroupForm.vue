<template>
  <Modal ref="modalRef">
    <template #header>
      <FormHeader :title="formTitle" icon="ik#ik-users" />
    </template>

    <n-form ref="formRef" :rules="rules" :model="form" label-placement="top" :show-require-mark="true" size="large">
      <!-- URI -->
      <n-form-item>
        <UriForm
          v-model:uri="form.uri"
          label="component.group.group-uri"
          helpMessage="component.common.uri-help-message"
          :editMode="isEditMode"
          v-model:generated="uriGenerated"
        />
      </n-form-item>

      <!-- Name -->
      <n-form-item path="name">
        <InputForm
          v-model:value="form.name"
          label="component.common.name"
          type="text"
          :required="true"
          :placeholder="t('component.group.form-name-placeholder')"
        />
      </n-form-item>

      <!-- Description -->
      <n-form-item path="description">
        <TextAreaForm
          v-model:value="form.description"
          label="component.common.description"
          :required="true"
          :placeholder="t('component.group.form-description-placeholder')"
        />
      </n-form-item>

      <!-- User profile selection -->
      <GroupUserProfileForm v-if="form.user_profiles" ref="userProfilesRef" v-model:profiles="form.user_profiles" />
    </n-form>

    <template #footer>
      <FormFooter @cancel="hide" @submit="submit" />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { computed, inject, ref, useTemplateRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { NForm, NFormItem } from 'naive-ui'
import type { FormRules } from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { SecurityService, GroupUpdateDTO } from 'opensilex-security/index'
import type HttpResponse from '@/lib/HttpResponse'
import type { OpenSilexResponse } from '@/lib/HttpResponse'
import useModalFormLogic from '@/composables/useModalFormLogic'
import type { ModalFormEmits, ModalFormProps } from '@/composables/useModalFormLogic'
import { requiredTrimmed } from '@/models/FormFieldsFormatter'
import Modal from '@/components/common/views/Modal.vue'
import FormHeader from '@/components/common/forms/FormHeader.vue'
import FormFooter from '@/components/common/forms/FormFooter.vue'
import UriForm from '@/components/common/forms/UriForm.vue'
import InputForm from '@/components/common/forms/InputForm.vue'
import TextAreaForm from '@/components/common/forms/TextAreaForm.vue'
import GroupUserProfileForm from '@/components/groups/GroupUserProfileForm.vue'

//#region Public
const emit = defineEmits<ModalFormEmits>()
const props = defineProps<ModalFormProps>()
//#endregion

//#region Private
//#region Plugins & services
const { t } = useI18n()
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = $opensilex.getService<SecurityService>('opensilex.SecurityService')
//#endregion

//#region Template refs
const modalRef = useTemplateRef<InstanceType<typeof Modal>>('modalRef')
const nFormRef = useTemplateRef<InstanceType<typeof NForm>>('formRef')
const userProfilesRef = useTemplateRef<InstanceType<typeof GroupUserProfileForm>>('userProfilesRef')
//#endregion

//#region Data & Computed
const uriGenerated = ref<boolean>(true)

const rules = computed<FormRules>(() => ({
  name: requiredTrimmed('component.common.name'),
  description: requiredTrimmed('component.common.description')
}))

const { form, formTitle, isEditMode, exposed, hide, submit } = useModalFormLogic<GroupUpdateDTO>({
  modalRef,
  nFormRef,
  getEmptyForm,
  create,
  update,
  reset,
  props,
  emit
})
//#endregion

//#region Methods
function getEmptyForm(): GroupUpdateDTO {
  return {
    uri: null,
    name: '',
    description: '',
    user_profiles: []
  }
}

function reset(): void {
  uriGenerated.value = true
  userProfilesRef.value?.clearForm()
}

/** Keeps only the fields expected by the API for each user profile (the list DTO also holds names). */
function toPayload(form: GroupUpdateDTO): GroupUpdateDTO {
  return {
    uri: form.uri,
    name: form.name,
    description: form.description,
    user_profiles: (form.user_profiles || []).map((up) => ({
      user_uri: up.user_uri,
      profile_uri: up.profile_uri
    }))
  }
}

async function create(form: GroupUpdateDTO): Promise<HttpResponse<OpenSilexResponse> | undefined> {
  try {
    return await service.createGroup(toPayload(form))
  } catch (error) {
    // A 409 has a dedicated message; returning undefined keeps the modal open
    if ((error as { status?: number }).status === 409) {
      $opensilex.errorHandler(error, t('component.group.errors.group-already-exists'))
      return undefined
    }
    throw error
  }
}

function update(form: GroupUpdateDTO) {
  return service.updateGroup(toPayload(form))
}
//#endregion
//#endregion

defineExpose(exposed)
</script>
