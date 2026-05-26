<template>
  <n-form
    v-if="props.form"
    ref="formRef"
    :model="form"
    :rules="rules"
    label-placement="top"
    :show-require-mark="true"
  >
    <!-- URI -->
    <opensilex-UriForm
      :key="(form?.uri ?? '') + (editMode ? '-edit' : '-create')"
      v-model:uri="form.uri"
      :generated="uriGenerated"
      @update:generated="val => (uriGenerated = val)"
      :editMode="editMode"
      :helpMessage="$t('component.common.uri-help-message')"
      label="component.group.group-uri"
    />

    <!-- Name -->
    <n-form-item :label="$t('component.common.name')" path="name">
      <opensilex-InputForm
        v-model:value="form.name"
        type="text"
        :required="true"
        :placeholder="$t('component.group.form-name-placeholder')"
      />
    </n-form-item>

    <!-- Description -->
    <opensilex-TextAreaForm
      v-model:value="form.description"
      label="component.common.description"
      :placeholder="$t('component.group.form-description-placeholder')"
      @keydown.enter.stop
    />

    <br/>
    <!-- Variables -->
    <opensilex-VariableSelectorWithFilter
      ref="variablesSelectorRef"
      v-model:variables-with-labels="variablesWithLabels"
      v-model:variables="form.variables"
      :editMode="editMode"
      :label="$t('component.variable.title')"
      :placeholder="$t('component.variable.placeholder-multiple')"
      @hideSelector="$emit('hideSelector')"
      @shownSelector="$emit('shownSelector')"
    />
  </n-form>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInst } from 'naive-ui'
import { NForm, NFormItem } from 'naive-ui'

import { requiredTrimmed } from  "../../models/FormFieldsFormatter"

// ---- Props / Emits ----
const props = withDefaults(defineProps<{
    editMode?: boolean
    uriGenerated?: boolean,
    form: {
      uri?: string | null
      name?: string | null
      description?: string | null
      variables: string[]
      __variablesWithLabels?: Array<{ id: string; label: string }>
    }
  }>(),
  {
    uriGenerated: true
  }
);

const emit = defineEmits<{
  (e: 'hideSelector'): void
  (e: 'shownSelector'): void
  (e: 'hide'): void
}>()

const { t } = useI18n()

// ---- Form / Refs ----
const formRef = ref<FormInst | null>(null)
const variablesSelectorRef = ref<any>()
const uriGenerated = ref(props.uriGenerated ?? true)

// Liste de labels passée au sélecteur
const variablesWithLabels = ref<Array<{ id: string; label: string }>>([])

// ---- Règles Naive UI ----
const rules = computed(() => ({
  name: requiredTrimmed('component.common.name')
  // autres règles si nécessaire, ex :
  // variables: { type: 'array', required: true, message: t('...'), trigger: 'change' }
}))

// ---- API attendue par ModalForm.vue ----
// - getEmptyForm() : utilisé par showCreateForm()
// - reset() : si on veut remettre à zéro l'état du form
function getEmptyForm () {
  return {
    uri: null,
    name: null,
    description: null,
    variables: [] as string[],
    __variablesWithLabels: [] as Array<{ id: string; label: string }>
  }
}

function reset () {
  // Remettre à zéro le sélecteur si nécessaire
  variablesWithLabels.value = []
  // Si le composant enfant expose une API de reset :
  variablesSelectorRef.value?.setVariableSelectorToFirstTimeOpen?.()
}

// Compat utilitaire : positionner “à la première ouverture” + labels sélectionnés
function setSelectorsToFirstTimeOpenAndSetLabels (list: Array<{ id: string; label: string }>) {
  variablesSelectorRef.value?.setVariableSelectorToFirstTimeOpen?.()
  variablesWithLabels.value = list ?? []
}

// Quand le parent injecte le form en édition, pré-remplir les champs
watch(
  () => props.form,
  async (form) => {
    const list = form?.__variablesWithLabels
    if (Array.isArray(list) && list.length) {
      await nextTick()
      setSelectorsToFirstTimeOpenAndSetLabels(list)
    }
  },
  { immediate: true }
)

// Optionnel : méthode de validation utilisée en amont si besoin
async function validate () {
  console.log("validate ??")
  try {
    await formRef.value?.validate()
    return true
  } catch {
    return false
  }
}


// Expose pour le parent (ModalForm l’appelle)
defineExpose({
  getEmptyForm,
  reset,
  setSelectorsToFirstTimeOpenAndSetLabels,
  validate
})
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  GroupVariablesForm:
    add: Add variable group
    edit: Edit variable group
fr:
  GroupVariablesForm:
    add: Ajouter un groupe de variables
    edit: Éditer un groupe de variables
</i18n>