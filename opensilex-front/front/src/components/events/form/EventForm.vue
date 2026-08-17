<template>
  <Modal ref="modalRef">
    <template #header>
      <FormHeader :title="modalFormLogic.formTitle.value" icon="bi#bi-activity" />
    </template>

    <n-form
      ref="formRef"
      :model="modalFormLogic.form.value"
      :rules="rules"
      label-placement="top"
      :show-require-mark="true"
      size="large"
    >
      <div class="row">
        <div class="col" v-if="!linkedToAreaForm">
          <n-form-item>
            <UriForm
              v-model:uri="modalFormLogic.form.value.uri"
              :generated="uriGenerated"
              @update:generated="val => uriGenerated = val"
              label="component.common.uri"
              :editMode="modalFormLogic.isEditMode.value"
              :helpMessage="t('component.common.uri-help-message')"
              :required="true"
            />
          </n-form-item>
        </div>
      </div>

      <div class="row">
        <div class="col">
          <n-form-item path="rdf_type" :show-label="false">
            <TypeForm
              ref="typeForm"
              v-model:type="modalFormLogic.form.value.rdf_type"
              :baseType="baseType"
              :ignoreRoot="false"
              :required="false"
              :disabled="modalFormLogic.isEditMode.value"
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
            <TagInputForm
              v-model:value="modalFormLogic.form.value.targets"
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
          <n-form-item path="description">
            <TextAreaForm
              v-model:value="modalFormLogic.form.value.description"
              :label="t('component.common.description')"
              :helpMessage="t('EventForm.description')"
              :placeholder="t('EventForm.description')"
              @keydown.enter.stop
            />
          </n-form-item>
        </div>
      </div>

      <n-form-item path="end" :show-label="false">
        <DateTimeRangeForm
          v-model:startDate="modalFormLogic.form.value.start"
          v-model:endDate="modalFormLogic.form.value.end"
          v-model:isInstant="modalFormLogic.form.value.is_instant"
          v-model:start_required="currentStartDateRequired"
          v-model:end_required="currentEndDateRequired"
          :canBeInstant="true"
        />
      </n-form-item>

      <br>

      <slot :form="modalFormLogic.form.value"></slot>

      <OntologyRelationsForm
        ref="ontologyRelationsForm"
        :rdfType="modalFormLogic.form.value.rdf_type"
        :relations="modalFormLogic.form.value.relations"
        :excludedProperties="excludedProperties"
        :baseType="baseType"
        :editMode="modalFormLogic.isEditMode.value"
        :context="context"
      />

      <MoveForm
        v-if="isMove(modalFormLogic.form.value?.rdf_type)"
        ref="moveForm"
        v-model:form="modalFormLogic.form.value"
      />
    </n-form>

    <template #footer>
      <FormFooter @cancel="modalFormLogic.hide" @submit="modalFormLogic.submit" />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import {computed, inject, ref, useTemplateRef} from 'vue'
import {useI18n} from 'vue-i18n'
import type {FormRules} from 'naive-ui'
import {NForm, NFormItem} from 'naive-ui'

import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type {EventsService} from 'opensilex-core/api/events.service'
import type {EventCreationDTO, MoveCreationDTO} from 'opensilex-core/index'
import type {EventUpdateDTO} from 'opensilex-core/model/eventUpdateDTO'
import type {EventDetailsDTO} from 'opensilex-core/model/eventDetailsDTO'
import type {MoveDetailsDTO} from 'opensilex-core/model/moveDetailsDTO'
import type {MoveUpdateDTO} from 'opensilex-core/model/moveUpdateDTO'
import HttpResponse, {OpenSilexResponse} from 'opensilex-core/HttpResponse'
import DTOConverter from '@/models/DTOConverter'

import Modal from '@/components/common/views/Modal.vue'
import FormHeader from '@/components/common/forms/FormHeader.vue'
import FormFooter from '@/components/common/forms/FormFooter.vue'
import UriForm from '@/components/common/forms/UriForm.vue'
import TextAreaForm from '@/components/common/forms/TextAreaForm.vue'
import TypeForm from '@/components/common/forms/TypeForm.vue'
import TagInputForm from '@/components/common/forms/TagInputForm.vue'
import DateTimeRangeForm from '@/components/common/forms/DateTimeRangeForm.vue'
import OntologyRelationsForm from '@/components/ontology/OntologyRelationsForm.vue'
import MoveForm from '@/components/events/form/MoveForm.vue'
import useModalFormLogic, {ModalFormEmits, ModalFormProps} from '@/composables/useModalFormLogic'

//#region Public
const emit = defineEmits<ModalFormEmits>();
const props = defineProps<ModalFormProps & {
  linkedToAreaForm?: boolean
  context?: string
  target?: string;
  defaultEventType?: string;
  eventCreatedTime?: any;
}>()
//#endregion

//#region Private

//#region Plugin and services
const { t } = useI18n()
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = opensilex.getService<EventsService>('opensilex.EventsService')
//#endregion

const modalRef = useTemplateRef<InstanceType<typeof Modal>>('modalRef')
const formRef = useTemplateRef<InstanceType<typeof NForm>>('formRef')
const moveForm = ref<any>(null)
const ontologyRelationsForm = ref<any>(null)
const typeForm = ref<any>(null)

//#region Datas
const uriGenerated = ref(true)
const currentStartDateRequired = ref(false)
const currentEndDateRequired = ref(true)
const baseType = opensilex.Oeev.EVENT_TYPE_URI
//#endregion

//#region Computed
const excludedProperties = new Set<string>([
  opensilex.Oeev.CONCERNS,
  opensilex.Oeev.IS_INSTANT,
  opensilex.Time.HAS_BEGINNING,
  opensilex.Time.HAS_END,
  opensilex.Rdfs.COMMENT,
  opensilex.Oeev.FROM,
  opensilex.Oeev.TO
])

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
//#endregion

//#region modalFormLogic composable
const modalFormLogic = useModalFormLogic<MoveCreationDTO>({
  modalRef,
  nFormRef: formRef,
  getEmptyForm,
  create,
  update,
  reset,
  props,
  emit
})
//#endregion

//#region Methods

function getEmptyForm(): MoveCreationDTO {
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

function initForm(event: EventCreationDTO) {
  if (!event) return

  const eventCopy = JSON.parse(JSON.stringify(event))

  if (!eventCopy.targets) {
    eventCopy.targets = []
  }

  if (props.target) {
    eventCopy.targets.push(props.target)
  }

  if (props.eventCreatedTime) {
    eventCopy.start = props.eventCreatedTime.time
    eventCopy.end = props.eventCreatedTime.time
    eventCopy.is_instant = true
  }

  if (props.defaultEventType) {
    eventCopy.rdf_type = props.defaultEventType
  }

  return eventCopy
}

async function reset(): Promise<void> {
  uriGenerated.value = true
}

async function create(formData: MoveCreationDTO) {
  const isMoveEvent = isMove(formData.rdf_type)
  const events = [formData]
  return isMoveEvent
    ? service.createMoves(events)
    : service.createEvents(events)
}

async function update(formData: any) {
  if (isMove(formData.rdf_type)) {
    const moveEvent = JSON.parse(JSON.stringify(formData)) as MoveUpdateDTO
    return service.updateMoveEvent(moveEvent)
  }
  return service.updateEvent(formData as EventUpdateDTO)
}

function showCreateForm() {
  modalFormLogic.showCreateForm(initForm(getEmptyForm()))
}

function showEditForm(uri: string, type: string, sourceItem?: any) {
  const detailsPromise = isMove(type)
    ? service.getMoveEvent(uri)
    : service.getEventDetails(uri)

  detailsPromise
    .then((http: HttpResponse<OpenSilexResponse<EventDetailsDTO | MoveDetailsDTO>>) => {
      const dto = http.response.result
      typeSwitch(dto.rdf_type, true)

      const publisher = dto.publisher
      const editDto = DTOConverter.extractURIFromResourceProperties<any, any>(dto)
      editDto.publisher = publisher

      if ((dto as any).location) {
        editDto.location = JSON.parse(JSON.stringify((dto as any).location))
      } else if (sourceItem?.location) {
        editDto.location = JSON.parse(JSON.stringify(sourceItem.location))
      }

      modalFormLogic.showEditForm(editDto)
    })
    .catch(opensilex.errorHandler)
}

function typeSwitch(type: string, initialLoad: boolean) {
  ontologyRelationsForm.value?.typeSwitch?.(type, initialLoad)
}

function isMove(type: string | undefined): boolean {
  if (!type) return false
  return opensilex.Oeev.checkURIs(type, opensilex.Oeev.MOVE_TYPE_URI)
}

function customOptionsTypes() {
  if (typeForm.value?.typesOptions) {
    const listOptions = typeForm.value.typesOptions
    const move = listOptions?.[0]?.children?.find((option: any) => option.label === 'Move')
    if (move) {
      move.isDisabled = true
    }
  }
}
//#endregion

//#endregion

defineExpose({
  showCreateForm,
  showEditForm
})
</script>

<style scoped lang="scss">
</style>

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
