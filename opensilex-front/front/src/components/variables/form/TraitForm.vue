<template>
  <n-form ref="validatorRef" :model="variable" :rules="rules">
    <!-- Trait URI -->
    <n-form-item path="trait">
      <InputForm
        v-model:value="variable.trait"
        label="component.variable.trait-uri"
        type="text"
        helpMessage="component.variable.trait-uri-help"
        :placeholder="t('component.variable.trait-uri-placeholder')"
        :required="traitRequired"
      />
    </n-form-item>

    <!-- Trait name -->
    <n-form-item path="trait_name">
      <InputForm
        v-model:value="variable.trait_name"
        label="component.variable.trait-name"
        type="text"
        helpMessage="component.variable.trait-name-help"
        :placeholder="t('component.variable.trait-name-placeholder')"
        :required="traitRequired"
      />
    </n-form-item>
  </n-form>
</template>

<script setup lang="ts">
import { computed, useTemplateRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { NForm, NFormItem, type FormInst, type FormRules } from 'naive-ui'
import InputForm from '@/components/common/forms/InputForm.vue'

/** Part of the variable edited by this wizard step. */
interface VariableTrait {
  trait?: string
  trait_name?: string
}

//#region Public
const variable = defineModel<VariableTrait>('form', { required: true })
//#endregion

//#region Private

//#region Plugins & services
const { t } = useI18n()
//#endregion

//#region Template refs
const validatorRef = useTemplateRef<FormInst>('validatorRef')
//#endregion

//#region Data & Computed
/** The URI and the name of the trait go together: both are required as soon as one is filled. */
const traitRequired = computed<boolean>(() => !!variable.value.trait || !!variable.value.trait_name)

const rules = computed<FormRules>(() => ({
  trait: {
    required: traitRequired.value,
    message: t('component.common.required'),
    trigger: ['blur', 'input'],
  },
  trait_name: {
    required: traitRequired.value,
    message: t('component.common.required'),
    trigger: ['blur', 'input'],
  },
}))
//#endregion

//#region Methods
function reset() {
  validatorRef.value?.restoreValidation()
}

function validate() {
  return validatorRef.value
    .validate()
    .then(() => true)
    .catch(() => false)
}
//#endregion

//#endregion

defineExpose({
  reset,
  validate,
})
</script>
