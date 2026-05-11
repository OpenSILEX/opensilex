<template>
  <n-form
    ref="formRef"
    :model="props.form"
    :rules="rules"
    label-placement="top"
    :show-require-mark="true"
  >
    <div class="row">
      <div class="col" v-if="!linkedToAreaForm">
        <opensilex-UriForm
          v-model:uri="props.form.uri"
          :generated="uriGenerated"
          @update:generated="val => (uriGenerated = val)"
          label="component.common.uri"
          :editMode="editMode"
          :helpMessage="$t('component.common.uri-help-message')"
          :required="true"
        />
      </div>
    </div>

    <div class="row">
      <div class="col">
        <n-form-item path="rdf_type" :show-label="false">
          <opensilex-TypeForm
            ref="typeForm"
            v-model:type="props.form.rdf_type"
            :baseType="baseType"
            :ignoreRoot="false"
            :required="false"
            :disabled="editMode"
            :placeholder="t('EventForm.type-placeholder')"
            @select="typeSwitch($event.id, false)"
            @open="customOptionsTypes"
          />
        </n-form-item>
      </div>
    </div>

    <div class="row">
      <div class="col" v-if="!linkedToAreaForm">
        <n-form-item path="targets" :show-label="false">
          <opensilex-TagInputForm
            v-model:value="props.form.targets"
            :baseType="opensilex.Oeev.CONCERNS"
            :label="t('EventForm.targets')"
            type="text"
            :required="true"
            :helpMessage="t('EventForm.target-help')"
          />
        </n-form-item>
      </div>
    </div>

    <div class="row">
      <div class="col" v-if="!linkedToAreaForm">
        <n-form-item :label="$t('component.common.description')" path="description" :show-require-mark="false">
          <opensilex-TextAreaForm
            v-model:value="props.form.description"
            :helpMessage="t('EventForm.description')"
            :placeholder="t('EventForm.description')"
            @keydown.enter.stop
          />
        </n-form-item>
      </div>
    </div>

    <n-form-item path="end" :show-label="false">
      <opensilex-DateTimeRangeForm
        v-model:startDate="props.form.start"
        v-model:endDate="props.form.end"
        v-model:isInstant="props.form.is_instant"
        v-model:start_required="currentStartDateRequired"
        v-model:end_required="currentEndDateRequired"
        :canBeInstant="true"
        @change="onUpdateIsInstantFilter"
      />
    </n-form-item>

    <br>

    <slot :form="props.form"></slot>

    <opensilex-OntologyRelationsForm
      ref="ontologyRelationsForm"
      :rdfType="props.form.rdf_type"
      :relations="props.form.relations"
      :excludedProperties="excludedProperties"
      :baseType="baseType"
      :editMode="editMode"
      :context="context"
    />

    <opensilex-MoveForm
      v-if="isMove()"
      ref="moveForm"
      v-model:form="props.form"
    />
  </n-form>
</template>

<script setup lang="ts">
import { computed, inject, ref, withDefaults } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInst, FormRules } from 'naive-ui'
import { NForm, NFormItem } from 'naive-ui'

import type { OntologyService } from 'opensilex-core/api/ontology.service'
import type { VueJsOntologyExtensionService } from '@/lib'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { EventCreationDTO, MoveCreationDTO } from 'opensilex-core/index'
import { requiredTrimmed } from './../../../models/FormFieldsFormatter'

const props = withDefaults(defineProps<{
  editMode?: boolean
  form: MoveCreationDTO
  linkedToAreaForm?: boolean
}>(), {
  editMode: false,
  form: () =>
    ({
      uri: undefined,
      rdf_type: undefined,
      relations: [],
      start: undefined,
      end: undefined,
      targets: [],
      description: undefined,
      is_instant: true
    } as MoveCreationDTO),
  linkedToAreaForm: false
})

const emit = defineEmits<{
  (e: 'change'): void
}>()

const { t } = useI18n()
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const ontologyService = opensilex.getService<OntologyService>('opensilex.OntologyService')
const vueOntologyService = opensilex.getService<VueJsOntologyExtensionService>('opensilex.VueJsOntologyExtensionService')

const formRef = ref<FormInst | null>(null)
const moveForm = ref<any>(null)
const ontologyRelationsForm = ref<any>(null)
const typeForm = ref<any>(null)

const uriGenerated = ref(true)
const currentStartDateRequired = ref(false)
const currentEndDateRequired = ref(true)
const errorMsg = ref('')
const context = ref('')
const baseType = opensilex.Oeev.EVENT_TYPE_URI

const excludedProperties = new Set<string>([
  opensilex.Oeev.CONCERNS,
  opensilex.Oeev.IS_INSTANT,
  opensilex.Time.HAS_BEGINNING,
  opensilex.Time.HAS_END,
  opensilex.Rdfs.COMMENT,
  opensilex.Oeev.FROM,
  opensilex.Oeev.TO
])

let propertyFilter = (property: any) => property
let initHandler = () => {}

const rules = computed<FormRules>(() => ({
  targets: !props.linkedToAreaForm
    ? {
        required: true,
        type: 'array',
        min: 1,
        message: t('EventForm.targets-error'),
        trigger: ['blur', 'change']
      }
    : undefined,

  end: {
    required: currentEndDateRequired.value,
    validator: (_rule: any, value: any) => {
      if (!currentEndDateRequired.value) {
        return true
      }

      if (value === undefined || value === null || value === '') {
        return new Error(t('EventForm.end-error'))
      }

      return true
    },
    trigger: ['blur', 'change']
  }
}))

function getDefaultForm(): EventCreationDTO {
  return {
    uri: undefined,
    rdf_type: undefined,
    relations: [],
    start: undefined,
    end: undefined,
    targets: [],
    description: undefined,
    is_instant: true
  }
}

function getEmptyForm() {
  return {
    uri: undefined,
    rdf_type: undefined,
    relations: [],
    start: undefined,
    end: undefined,
    targets: [],
    description: undefined,
    is_instant: true,
    from: undefined,
    to: undefined
  } as MoveCreationDTO
}

function setContext(newContext: string) {
  context.value = newContext
}

function onUpdateIsInstantFilter() {
  emit('change')
}

function reset() {
  uriGenerated.value = true
}

async function validate() {
  try {
    await formRef.value?.validate()
    return true
  } catch {
    return false
  }
}

function handleErrorMessage(message: string) {
  errorMsg.value = message
}

function setInitObjHandler(handler: () => void) {
  initHandler = handler
}

function setTypePropertyFilterHandler(handler: (property: any) => any) {
  propertyFilter = handler
}

function typeSwitch(type: string, initialLoad: boolean) {
  ontologyRelationsForm.value?.typeSwitch?.(type, initialLoad)
}

function isMove(): boolean {
  if (!props.form || !props.form.rdf_type) {
    return false
  }

  return opensilex.Oeev.checkURIs(props.form.rdf_type, opensilex.Oeev.MOVE_TYPE_URI)
}

function customOptionsTypes() {
  if (props.linkedToAreaForm && typeForm.value?.typesOptions) {
    const listOptions = typeForm.value.typesOptions
    const move = listOptions?.[0]?.children?.find((option: any) => option.label === 'Move')
    if (move) {
      move.isDisabled = true
    }
  }
}

defineExpose({
  getEmptyForm,
  reset,
  validate,
  setContext,
  setTypePropertyFilterHandler,
  setInitObjHandler,
  typeSwitch,
  handleErrorMessage
})
</script>

<i18n>
en:
  EventForm:
    description: Description of the event
    targets-error: URI of one or more targets is not valid
    targets: Targets
    target-help: Object targeted by the event (Must exist)
    end-error: End date is required
    type-placeholder: Select a type


fr:
  EventForm:
    description: "Description de l'événement"
    targets-error: L'URI d'un ou plusieurs objets concernés n'est pas valide
    targets: Concerne
    target-help: URI de l'objet concerné par l'évènement (Doit exister).
    end-error: La date de fin est obligatoire
    type-placeholder: Selectionner un type
</i18n>