<template>
  <opensilex-Modal ref="modalRef">
    <template #header>
      <div class="flex justify-between items-center">
        <h4>
          <slot name="icon">
            <opensilex-Icon :icon="icon" class="icon-title" />
          </slot>
          {{ translatedTitle }}
        </h4>
        <opensilex-HelpButton
          v-if="tutorial && !editMode"
          label="component.tutorial.name"
          @click="getFormRef()?.tutorial?.()"
          :small="true"
        />
      </div>
    </template>

    <n-form :model="form" :rules="rules" ref="formRef">
      <component
        ref="componentRef"
        :is="component"
        v-model:form="form"
        :editMode="editMode"
        :data="data"
      >
        <slot name="customFields" :form="form" :editMode="editMode" />
      </component>
    </n-form>

    <template #footer>
      <button class="btn btn-secondary" @click="hide">{{ t('component.common.cancel') }}</button>
      <button class="btn greenThemeColor" @click="validate">{{ t('component.common.ok') }}</button>
    </template>
  </opensilex-Modal>
</template>

<script setup lang="ts">
import { ref, nextTick, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInst } from 'naive-ui'
import { NForm } from 'naive-ui'

const { t } = useI18n()

const modalRef = ref()
const formRef = ref<FormInst | null>(null)
const componentRef = ref()

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
  overrideSuccessMessage: Boolean
})

const emit = defineEmits(['hide', 'onCreate', 'onUpdate'])

const editMode = ref(false)
const form = ref<Record<string, any>>({})
const rules = ref<Record<string, any>>({})

onMounted(() => {
  console.log('ModalForm mounted')
})

const translatedTitle = computed(() => {
  const key = editMode.value ? props.editTitle : props.createTitle
  return t(key)
})

function getFormRef() {
  return componentRef.value
}

async function validate() {
  try {
    // API Promise : erreur si invalide
    // await formRef.value?.validate()
    const ok = await getFormRef()?.validate?.()
if (!ok) return
  } catch (errors) {
    console.log("error : ", errors)
    return
  }

  // si on arrive ici, formulaire valide
  let submit = props.createAction ?? getFormRef()?.create
  let event = 'onCreate'

  if (editMode.value) {
    submit = props.updateAction ?? getFormRef()?.update
    event = 'onUpdate'
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
        modalRef.value?.hide()
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
  // TODO : afficher le message dans un toast
}

function showCreateForm(passedForm?: any) {
  editMode.value = false
  nextTick(() => {
    form.value = passedForm ?? getFormRef()?.getEmptyForm?.() ?? {}
    getFormRef()?.reset?.()
    modalRef.value?.show()
  })
}

function showEditForm(editForm: any) {
  console.log('ModalForm showEditForm')
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
  hide
})
</script>
