<template>
  <v-tour
    v-if="!editMode"
    ref="tutorial"
    :steps="steps"
    :options="options"
    :callbacks="callbacks"
  />
</template>

<script setup lang="ts">
import { ref, defineProps, defineEmits, computed } from 'vue'
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

// Méthodes exposées au parent si besoin
defineExpose({
  start: () => tutorial.value?.start(),
  skip: () => tutorial.value?.skip(),
  stop: () => tutorial.value?.stop(),
  finish: () => tutorial.value?.finish()
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
