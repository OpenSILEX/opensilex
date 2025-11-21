<template>
  <n-modal
    v-model:show="visible"
    preset="card"
    :mask-closable="false"
    :style="{ width: modalWidth }"
    :title="editMode ? t(editTitle) : t(createTitle)"
    to="body"
    display-directive="if"
  >
  <div class="wizard-steps">
    <n-steps 
      :current="currentStepIndex + 1" 
      size="small" 
      class="mb-4"
    >
      <n-step v-for="(step, i) in steps" :key="i" :title="t(step.title)" />
    </n-steps>
  </div>

    <component
      :is="steps[currentStepIndex].component"
      :key="currentStepIndex"
      v-bind="steps[currentStepIndex].props"
      :form="form"
      :editMode="editMode"
      ref="stepComponent"
      @fill="fillForm"
      @clear="clearForm"
      @agroportalTermSelected="payload => emit('agroportalTermSelected', payload)"
      @agroportalTermUnselected="() => emit('agroportalTermUnselected')"
    />

    <template #footer>
      <n-space justify="space-between">
        <n-button @click="close">{{ t('component.common.form-wizard.cancel') }}</n-button>

        <div>
          <n-space justify="end" :size="10"> 
            <n-button v-if="currentStepIndex > 0" @click="prevStep">
              {{ prevText }}
            </n-button>

            <n-button
              v-if="finishText"
              type="primary"
              class="greenThemeColor"
              @click="submitForm"
            >
              {{ finishText }}
            </n-button>

            <n-button v-if="!isLastStep" type="primary" class="greenThemeColor" @click="nextStep">
              {{ nextText }}
            </n-button>

            <n-button v-if="isLastStep" type="primary" class="greenThemeColor" @click="submitForm">
              {{ doneText }}
            </n-button>
          </n-space>
        </div>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { NModal, NSteps, NStep, NButton, NSpace } from 'naive-ui'

interface WizardStep {
  component: any
  title: string          // clé i18n
  finish?: string | (() => string)
  next?: string | (() => string)
  done?: string | (() => string)
  previous?: string | (() => string)
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
  (e: 'agroportalTermSelected', payload: any): void
  (e: 'agroportalTermUnselected'): void
}>()

const { t } = useI18n()

const visible = ref(false)
const editMode = ref(false)
const currentStepIndex = ref(0)
const form = ref<any>(null)
const stepComponent = ref<any>()

const modalWidth = computed(() => props.modalWidth ?? '900px')
const isLastStep = computed(() => currentStepIndex.value === props.steps.length - 1)

function showCreateForm () {
  form.value = props.initForm()
  editMode.value = false
  currentStepIndex.value = 0
  visible.value = true
  nextTick(() => stepComponent.value?.reset?.())
}

function showEditForm (inputForm: any) {
  form.value = inputForm
  editMode.value = true
  currentStepIndex.value = 0
  visible.value = true
  nextTick(() => stepComponent.value?.reset?.())
}

function close () { visible.value = false }

async function validateStep () {
  if (stepComponent.value?.validate) return await stepComponent.value.validate()
  return true
}

async function nextStep () {
  if (!(await validateStep())) return

  if (props.nextStepAction) {
    const ok = await props.nextStepAction(
      currentStepIndex.value,
      form.value,
      props.steps[currentStepIndex.value + 1],
      stepComponent.value
    )
    if (!ok) return
  }

  currentStepIndex.value++
  nextTick(() => stepComponent.value?.reset?.())
}

function prevStep () {
  currentStepIndex.value--
  nextTick(() => stepComponent.value?.reset?.())
}

const finishText = computed(() => {
  const raw = props.steps[currentStepIndex.value].finish

  if (typeof raw === 'function') {
    return raw()
  }

  // si pas de finish défini > chaîne vide
  if (!raw) return ''

  return t(raw) as string
})


async function submitForm () {
  if (!(await validateStep())) return
  if (props.validateAction && !props.validateAction(form.value)) return

  const action = editMode.value ? props.updateAction : props.createAction
  if (!action) return

  const result = await action(form.value)
  if (result !== false) {
    // les événements sont émis par le parent (AgroportalCreateForm) si besoin
    close()
  }
}

function fillForm (dto: any) {
  if (props.convertAction) form.value = props.convertAction(form.value, dto)
}
function clearForm () { form.value = props.initForm() }

const prevText = computed(() => {
  const raw = props.steps[currentStepIndex.value].previous
  if (typeof raw === 'function') {
    return raw()
  }
  return t(raw ?? 'component.common.form-wizard.previous')
})

const nextText = computed(() => {
  const raw = props.steps[currentStepIndex.value].next
  if (typeof raw === 'function') {
    return raw()
  }
  return t(raw ?? 'component.common.form-wizard.next')
})

const doneText = computed(() => {
  const raw = props.steps[currentStepIndex.value].done
  if (typeof raw === 'function') {
    return raw()
  }
  return t(raw ?? 'component.common.form-wizard.done')
})



defineExpose({ showCreateForm, showEditForm, close })
</script>


<style lang="scss">
/* Pastille de chaque étape */
.wizard-steps .n-step-indicator {
  background-color: #00A38D !important;
}

/* Icone des étapes validées */
.wizard-steps .n-base-icon {
  color: #fff !important;
}
</style>