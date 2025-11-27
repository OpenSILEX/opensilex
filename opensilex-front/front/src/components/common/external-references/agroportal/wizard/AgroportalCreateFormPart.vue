<template>
  <div class="v-step-agroportal-create">
    <opensilex-Tutorial
      ref="tutorialRef"
      :steps="tutorialSteps"
      @onFinish="onTutorialFinishOrSkip"
      @onSkip="onTutorialFinishOrSkip"
    />

    <div class="container-fluid">
      <div class="row align-items-center">
        <div class="agroportalCreateForm col-12 col-lg-8 mx-auto">
          <!-- URI -->
          <opensilex-UriForm
            class="v-step-uri mb-3"
            v-model:uri="formDto.uri"
            v-model:generated="uriGenerated"
            label="component.common.uri"
            :helpMessage="t('AgroportalCreateFormPart.uri-help')"
            :required="true"
            :editMode="editMode"
          />

          <div class="v-step-name mb-3" :class="{ 'has-error': !!nameError }">
            <!-- Name -->
            <opensilex-InputForm
                v-model:value="formDto.name"
                label="component.common.name"
                type="text"
                :required="true"
                :placeholder="props.namePlaceholder"
            />
            <div v-if="nameError" class="field-error">
                {{ nameError }}
            </div>
          </div>

          <!-- Comment -->
          <opensilex-TextAreaForm
            class="v-step-description mb-3"
            v-model:value="formDto.description"
            label="component.common.description"
          />

          <!-- Champs additionnels -->
          <slot name="createAdditionalFields" :form="formDto" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, defineProps, defineExpose, watch, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { BaseExternalReferencesDTO } from '../../ExternalReferencesTypes'

/** Props */
const props = defineProps<{
  editMode?: boolean
  namePlaceholder: string
  descriptionPlaceholder: string
  form: BaseExternalReferencesDTO
}>()


const { t } = useI18n({ useScope: 'local' })

/** Refs & Stats */
const formDto = computed<BaseExternalReferencesDTO>({
  get: () => props.form,
  set: (v) => Object.assign(props.form, v)
})
const uriGenerated = ref(true)
const tutorialRef = ref<any>(null)
const nameError = ref<string>('')

watch(
  () => formDto.value.name,
  (newVal) => {
    if (nameError.value && (newVal ?? '').toString().trim()) {
      nameError.value = ''
    }
  }
)

/** Tutorial Datas */
const savedFormBeforeTutorial = ref<BaseExternalReferencesDTO | null>(null)

const tutorialSteps = [
  {
    target: '.v-step-agroportal-create .v-step-uri',
    header: { title: t('AgroportalCreateFormPart.tutorial.step-uri.title') },
    content: t('AgroportalCreateFormPart.tutorial.step-uri.content'),
    params: { placement: 'right' }
  },
  {
    target: '.v-step-agroportal-create .v-step-name',
    header: { title: t('AgroportalCreateFormPart.tutorial.step-name.title') },
    content: t('AgroportalCreateFormPart.tutorial.step-name.content'),
    params: { placement: 'right' }
  },
  {
    target: '.v-step-agroportal-create .v-step-description',
    header: { title: t('AgroportalCreateFormPart.tutorial.step-description.title') },
    content: t('AgroportalCreateFormPart.tutorial.step-description.content'),
    params: { placement: 'right' }
  },
  {
    target: '#v-step-wizard-buttons',
    header: { title: t('AgroportalCreateFormPart.tutorial.step-validation.title') },
    content: t('AgroportalCreateFormPart.tutorial.step-validation.content'),
    params: { placement: 'top' }
  }
]

/** Public Methods */
function reset() {
  uriGenerated.value = true
}
function validate() {
  nameError.value = ''

  const name = (formDto.value.name ?? '').toString().trim()
  if (!name) {
    nameError.value = t('validations.required_if', {
      _field_: t('component.common.name') as string
    }) as string

    return false
  }
  return true
}


/** Tutorial Methods */
function startTutorial() {
  savedFormBeforeTutorial.value = JSON.parse(JSON.stringify(formDto.value))

  // On utilise directement les placeholders, sans passer par `t()`
  formDto.value.name = props.namePlaceholder ?? ''
  formDto.value.description = props.descriptionPlaceholder ?? ''

  tutorialRef.value?.start()
}

function onTutorialFinishOrSkip() {
  if (savedFormBeforeTutorial.value) {
    formDto.value = JSON.parse(JSON.stringify(savedFormBeforeTutorial.value))
  }
}

/** Exposed to WizardForm */
defineExpose({
  reset,
  validate,
  startTutorial
})
</script>

<style scoped lang="scss">
.agroportalCreateForm {
  position: relative;
  width: 100%;
  padding-right: 15px;
  padding-left: 15px;
}

/* Bordure rouge autour du champ Nom quand erreur */
.has-error :deep(.n-input)
 {
  border: 1px solid !important;
  border-color: red !important;
}

/* Message d’erreur sous le champ */
.field-error {
  margin-top: 4px;
  font-size: 0.875rem;
  color: #dc3545;
}
</style>

<i18n>
en:
  AgroportalCreateFormPart:
    uri-help: >
      Uncheck this checkbox if you want to insert a concept from an existing ontology or if you want to set a
      particular URI. Leave it checked if you want to create a new entity with an auto-generated URI.
    tutorial:
      step-uri:
        title: URI
        content: >
          By default, the URI of the concept will be generated by OpenSilex. You can also choose to define a custom
          URI to identify your concept.
      step-name:
        title: Name
        content: >
          Enter the name of your concept.
      step-description:
        title: Description
        content: >
          Enter the description of your concept.
      step-validation:
        title: Validation
        content: >
          Click on the 'Next' button to proceed to the next step, where you will add
          external references to your concept. Click on the 'Save' button to save your concept.
fr:
  AgroportalCreateFormPart:
    uri-help: >
      Décocher si vous souhaitez ajouter une entité à partir d'une ontologie existante ou si vous souhaitez
      spécifier une URI particulière. Laisser coché si vous souhaitez ajouter une entité avec une URI auto-générée.
    tutorial:
      step-uri:
        title: URI
        content: >
          Par défaut, l'URI de votre concept sera générée automatiquement par OpenSilex. Vous pouvez également
          choisir de définir l'URI que vous souhaitez pour identifier votre concept.
      step-name:
        title: Nom
        content: >
          Entrez le nom de votre concept. Cette étape est obligatoire.
      step-description:
        title: Description
        content: >
          Entrez la description de votre concept. Essayez de fournir une description précise et compréhensible.
      step-validation:
        title: Validation
        content: >
          Cliquez sur le bouton 'Suivant' pour passer à l'étape d'ajout de références
          externes. Si vous pensez ne pas avoir besoin d'en ajouter, vous pouvez cliquer sur le bouton
          'Enregistrer' pour sauvegarder votre concept tel quel.
</i18n>
