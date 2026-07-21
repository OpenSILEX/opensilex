<template>
  <Modal ref="modalRef">
    <template #header>
      <div class="flex justify-between items-center">
        <h4>
          <slot name="icon">
            <Icon :icon="icon" class="icon-title" />
          </slot>
          {{ translatedTitle }}
        </h4>
        <HelpButton
          v-if="tutorial && !editMode"
          label="component.tutorial.name"
          class="modalFormTutorial"
          @click="getFormRef()?.tutorial?.()"
          :small="true"
        />
      </div>
    </template>

    <component
      ref="componentRef"
      :is="component"
      v-model:form="form"
      :key="componentRefreshKey"
      :editMode="editMode"
      :data="data"
      @validationChanged="isValid = $event"
    >
      <slot name="customFields" :form="form" :editMode="editMode" />
    </component>

    <template #footer>
      <button class="btn btn-secondary" @click="hide">{{ t('component.common.cancel') }}</button>
      <button
          class="btn greenThemeColor"
          @click="submit"
          :disabled="!isValid"
      >{{ t('component.common.ok') }}</button>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import {computed, inject, nextTick, ref, useTemplateRef} from 'vue'
import {useI18n} from 'vue-i18n'
import type {FormInst} from 'naive-ui'
import {NForm} from 'naive-ui'
import OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import Modal from "@/components/common/views/Modal.vue";
import Icon from "@/components/common/views/Icon.vue";
import HelpButton from "@/components/common/buttons/HelpButton.vue";

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const { t } = useI18n()

const modalRef = ref()
const componentRef = useTemplateRef<any>('componentRef')
const isValid = ref<boolean>(true);

const props = defineProps({
  component: { type: [String, Object], required: true },
  icon: String,
  createTitle: { type: String, required: true },
  editTitle: { type: String, required: true },
  tutorial: Boolean,
  editMode: Boolean,
  data: Object,
  createAction: Function,
  updateAction: Function,
  successMessage: [String, Function],
  overrideSuccessMessage: Boolean,
  initForm: Function
})

const emit = defineEmits(['hide', 'onCreate', 'onUpdate', 'onSuccess'])

const editMode = ref(false)
const form = ref<Record<string, any>>()
const componentRefreshKey = ref(0)

const translatedTitle = computed(() => {
  const key = editMode.value ? props.editTitle : props.createTitle
  return t(key)
})

function getFormRef() {
  return componentRef.value
}

async function submit() {
  try {
    const child = getFormRef()
    if (child?.validate) {
      const ok = await child.validate()
      if (ok === false) {
        // Le composant enfant décide d’annuler la soumission
        return
      }
    }
  } catch (errors) {
    console.log('ModalForm validation error : ', errors)
    return
  }

  // si on arrive ici, le formulaire est considéré valide
  let submit = props.createAction ?? getFormRef()?.create

  if (editMode.value) {
    submit = props.updateAction ?? getFormRef()?.update
  }

  const result = submit?.(form.value)

  Promise.resolve(result)
    .then((res) => {
      if (res !== false) {
        creationOrUpdateMessage()

        if (editMode.value) {
          emit('onUpdate', res)
        } else {
          emit('onCreate', res)
        }

        hide();
        emit('hide')
        emit('onSuccess') // rafraîchir les listes
        console.log('[ModalForm] submit done, res = ', res)
      }
    })
    .catch((err) => {
      getFormRef()?.handleSubmitError?.(err)
    })
}

function creationOrUpdateMessage() {
  let msg =
    typeof props.successMessage === 'function'
      ? props.successMessage(form.value)
      : t(props.successMessage ?? 'component.common.element')

  if (!props.overrideSuccessMessage) {
    msg += t(
      editMode.value
        ? 'component.common.success.update-success-message'
        : 'component.common.success.creation-success-message'
    )
  }
  opensilex?.showSuccessToast?.(msg)
}

function showCreateForm(passedForm?: any) {
  editMode.value = false
  nextTick(() => {
    form.value = passedForm ?? getFormRef()?.getEmptyForm?.() ?? {}
    if (props.initForm) {
      form.value = props.initForm(form.value)
    }
    getFormRef()?.reset?.()
    componentRefreshKey.value++
    modalRef.value?.show()
  })
}

function showEditForm(editForm: any) {
  editMode.value = true
  nextTick(() => {
    form.value = editForm
    getFormRef()?.reset?.()
    getFormRef()?.onShowEditForm?.()
    modalRef.value?.show()
  })
}

function hide() {
  modalRef.value?.hide()
}

defineExpose({
  showCreateForm,
  showEditForm,
  getFormRef,
  hide
})
</script>
<style lang="scss">
  .modalFormTutorial {
  position: absolute;
  top: 19px;
  right: 50px;
  }
</style>
