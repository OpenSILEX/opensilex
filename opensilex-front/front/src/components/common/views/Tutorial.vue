<template>
  <v-tour
    v-if="!editMode"
    ref="tutorial"
    name="form-tour"
    :steps="steps"
    :options="options"
    :callbacks="callbacks"
  />
</template>

<script setup lang="ts">
import { ref, defineProps, defineEmits, computed, getCurrentInstance } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  editMode: {
    type: Boolean,
    default: false
  },
  steps: {
    type: Array,
    required: true
  }
})

const emit = defineEmits([
  'onPreviousStep',
  'onNextStep',
  'onFinish',
  'onSkip',
  'onStart'
])

const tutorial = ref()

const { t } = useI18n()

const options = computed(() => ({
  useKeyboardNavigation: false,
  labels: {
    buttonSkip: t('component.tutorial.skip-tour'),
    buttonPrevious: t('component.tutorial.previous'),
    buttonNext: t('component.tutorial.next'),
    buttonStop: t('component.tutorial.finish')
  }
}))

const callbacks = {
  onPreviousStep: () => emit('onPreviousStep'),
  onNextStep: () => emit('onNextStep'),
  onFinish: () => emit('onFinish'),
  onSkip: () => emit('onSkip'),
  onStart: () => emit('onStart')
}

//  accès à this.$tours via l’instance courante
const instance = getCurrentInstance()
const proxy = instance?.proxy as any

const tours = proxy?.$tours as any
const TOUR_NAME = 'form-tour'

// Méthodes exposées au parent si besoin
defineExpose({
  start: () => tours?.[TOUR_NAME]?.start?.(),
  skip: () => tours?.[TOUR_NAME]?.skip?.(),
  stop: () => tours?.[TOUR_NAME]?.stop?.(),
  finish: () => tours?.[TOUR_NAME]?.finish?.()
})
</script>

<style lang="scss">
$v-tour-base-color: #00a38d !important;
$v-tour-base-text-color: black !important;
$v-tour-background-color: #fff !important;

.v-step__header {
  background-color: $v-tour-base-color;
  font-weight: bold;
}
.v-step {
  background-color: $v-tour-background-color;
}
.v-step__content {
  color: $v-tour-base-text-color;
}
.v-step__button {
  color: $v-tour-base-text-color;
  border: 0.05rem solid $v-tour-base-text-color;
}
</style>
