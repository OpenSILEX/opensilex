<template>
  <n-modal
    v-model:show="visible"
    :mask-closable="false"
    preset="card"
    :style="{ width: modalWidth }"
    :title="editMode ? t(editTitle) : t(createTitle)"
  >
    <n-steps :current="currentStepIndex" :size="'small'" class="mb-4">
      <n-step v-for="(step, index) in steps" :key="index" :title="step.title" />
    </n-steps>

    <component
      :is="steps[currentStepIndex].component"
      v-bind="steps[currentStepIndex].props"
      :form="form"
      :editMode="editMode"
      ref="stepComponent"
      @fill="fillForm"
      @clear="clearForm"
    />

    <template #footer>
      <n-space justify="space-between">
        <n-button @click="close">{{ t('component.common.form-wizard.cancel') }}</n-button>

        <div>
          <n-button
            v-if="currentStepIndex > 0"
            @click="prevStep"
          >{{ getStepBtnPreviousTitle }}</n-button>

          <n-button
            v-if="!isLastStep"
            type="primary"
            @click="nextStep"
          >{{ getStepBtnNextTitle }}</n-button>

          <n-button
            v-if="isLastStep"
            type="primary"
            @click="submitForm"
          >{{ getStepBtnDoneTitle }}</n-button>
        </div>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'

interface WizardStep {
  component: any
  title: string
  finish?: string
  next?: string
  done?: string
  previous?: string
  props?: Record<string, any>
}

const props = defineProps<{
  steps: WizardStep[]
  editTitle: string
  createTitle: string
  icon?: string
  modalWidth?: string
  isBlockingStep?: boolean
  initForm: () => any
  createAction?: (form: any) => Promise<any>
  updateAction?: (form: any) => Promise<any>
  convertAction?: (form: any, dto: any) => any
  nextStepAction?: (index: number, form: any, nextStep: any, currentStep: any) => Promise<boolean> | boolean
  validateAction?: (form: any) => boolean
}>()

const emit = defineEmits<{
  (e: 'onCreate', result: any): void
  (e: 'onUpdate', result: any): void
}>()

const { t } = useI18n()
const visible = ref(false)
const editMode = ref(false)
const currentStepIndex = ref(0)
const form = ref<any>(null)
const stepComponent = ref()

const isLastStep = computed(() => currentStepIndex.value === props.steps.length - 1)

const showCreateForm = () => {
  form.value = props.initForm()
  editMode.value = false
  visible.value = true
  currentStepIndex.value = 0
  nextTick(() => stepComponent.value?.reset?.())
}

const showEditForm = (inputForm: any) => {
  form.value = inputForm
  editMode.value = true
  visible.value = true
  currentStepIndex.value = 0
  nextTick(() => stepComponent.value?.reset?.())
}

const close = () => {
  visible.value = false
}

const validateStep = async (): Promise<boolean> => {
  if (stepComponent.value?.validate) {
    return await stepComponent.value.validate()
  }
  return true
}

const nextStep = async () => {
  const isValid = await validateStep()
  if (!isValid) return

  if (props.nextStepAction) {
    const nextStepComponent = props.steps[currentStepIndex.value + 1]
    const currentStepComp = stepComponent.value
    const shouldProceed = await props.nextStepAction(
      currentStepIndex.value,
      form.value,
      nextStepComponent,
      currentStepComp
    )
    if (!shouldProceed) return
  }

  currentStepIndex.value++
  nextTick(() => stepComponent.value?.reset?.())
}

const prevStep = () => {
  currentStepIndex.value--
  nextTick(() => stepComponent.value?.reset?.())
}

const submitForm = async () => {
  const isValid = await validateStep()
  if (!isValid) return

  if (props.validateAction && !props.validateAction(form.value)) {
    return
  }

  const action = editMode.value ? props.updateAction : props.createAction
  if (action) {
    const result = await action(form.value)
    if (result !== false) {
        if (editMode.value) {
            emit('onUpdate', result)
        } else {
            emit('onCreate', result)
        }
        close()
    }
  }
}

const fillForm = (dto: any) => {
  if (props.convertAction) {
    form.value = props.convertAction(form.value, dto)
  }
}

const clearForm = () => {
  form.value = props.initForm()
}

const getStepBtnPreviousTitle = computed(() => {
  const step = props.steps[currentStepIndex.value]
  return step.previous ? t(step.previous) : t('component.common.form-wizard.previous')
})
const getStepBtnNextTitle = computed(() => {
  const step = props.steps[currentStepIndex.value]
  return step.next ? t(step.next) : t('component.common.form-wizard.next')
})
const getStepBtnDoneTitle = computed(() => {
  const step = props.steps[currentStepIndex.value]
  return step.done ? t(step.done) : t('component.common.form-wizard.done')
})

// Expose public methods
defineExpose({
  showCreateForm,
  showEditForm,
  close
})
</script>

<style scoped>
.wizard-footer {
  display: flex;
  justify-content: space-between;
  padding-top: 1rem;
}
</style>
