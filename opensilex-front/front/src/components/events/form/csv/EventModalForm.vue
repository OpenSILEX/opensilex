<template>
  <opensilex-ModalForm
    v-if="user.hasCredential(credentials.CREDENTIAL_EVENT_MODIFICATION_ID) && renderModalForm"
    ref="modalForm"
    modalSize="lg"
    :tutorial="false"
    component="opensilex-EventForm"
    :createTitle="t('Event.add')"
    :editTitle="t('Event.edit')"
    icon="bi#bi-activity"
    :initForm="initForm"
    :successMessage="successMessage"
    :createAction="create"
    :updateAction="update"
  />
</template>

<script setup lang="ts">
import { computed, inject, nextTick, ref } from 'vue';
import { useStore } from 'vuex';
import { useI18n } from 'vue-i18n';

import type { OpenSilexVuePlugin } from '@/models/OpenSilexVuePlugin';
import type { EventsService } from 'opensilex-core/api/events.service';
import type { VueJsOntologyExtensionService } from '@/lib';
import HttpResponse, { OpenSilexResponse } from 'opensilex-core/HttpResponse';
import DTOConverter from './../../../../models/DTOConverter';

import type { EventCreationDTO } from 'opensilex-core/index';
import type { EventUpdateDTO } from 'opensilex-core/model/eventUpdateDTO';
import type { EventDetailsDTO } from 'opensilex-core/model/eventDetailsDTO';
import type { MoveDetailsDTO } from 'opensilex-core/model/moveDetailsDTO';
import type { MoveUpdateDTO } from 'opensilex-core/model/moveUpdateDTO';

// Props
const props = defineProps<{
  target?: string;
  defaultEventType?: string;
  editMode?: any;
  eventCreatedTime?: any;
  context?: any;
}>();

// Emits
const emit = defineEmits<{
  (e: 'onCreate', event: EventCreationDTO): void;
  (e: 'onUpdate', event: EventUpdateDTO): void;
}>();

// Services / env
const store = useStore();
const { t } = useI18n();
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const service = opensilex.getService<EventsService>('opensilex.EventsService');
const vueOntologyService = opensilex.getService<VueJsOntologyExtensionService>('opensilex.VueJsOntologyExtensionService');

// State
const renderModalForm = ref(false);
const modalForm = ref<any>(null);

// Store bindings
const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);

function getFormRef() {
  return modalForm.value?.getFormRef?.();
}

function showCreateForm() {
  renderModalForm.value = true;

  nextTick(() => {
    const form = getFormRef();
    form?.setContext?.(props.context);
    modalForm.value?.showCreateForm?.();
  });
}

function showEditForm(uri: string, type: string, sourceItem?: any) {
  renderModalForm.value = true;

  nextTick(() => {
    const detailsPromise = isMove(type)
      ? service.getMoveEvent(uri)
      : service.getEventDetails(uri);

    detailsPromise
      .then((http: HttpResponse<OpenSilexResponse<EventDetailsDTO | MoveDetailsDTO>>) => {
        const dto = http.response.result;
        const form = getFormRef();

        form?.typeSwitch?.(dto.rdf_type, true);
        form?.setContext?.(props.context);

        const publisher = dto.publisher;
        const editDto = DTOConverter.extractURIFromResourceProperties<any, any>(dto);
        editDto.publisher = publisher;

        if ((dto as any).location) {
          // Injecter les données de location provenant de la ligne de PositionList
          editDto.location = JSON.parse(JSON.stringify((dto as any).location));
        } else if (sourceItem?.location) {
          editDto.location = JSON.parse(JSON.stringify(sourceItem.location));
        }

        modalForm.value?.showEditForm?.(editDto);
      })
      .catch(opensilex.errorHandler);
  });
}

function create(event: EventCreationDTO) {
  const isMoveEvent = isMove(event.rdf_type);

  const events = [event];
  const createPromise = isMoveEvent
    ? service.createMoves(events)
    : service.createEvents(events);

  return createPromise.then((http: HttpResponse<OpenSilexResponse<string>>) => {
    const message = `${t('Event.name')} ${http.response.result} ${t('component.common.success.creation-success-message')}`;
    opensilex.showSuccessToast(message);
    event.uri = http.response.result.toString();
    emit('onCreate', event);
  }).catch((error: any) => {
    if (error.status === 409) {
      opensilex.errorHandler(error, t('component.account.errors.user-already-exists'));
    } else {
      const message = t('Event.uri-error', { error: error.response?.result?.message });
      opensilex.showErrorToast(message);
    }
  });
}

function update(event: EventUpdateDTO | MoveUpdateDTO) {
  if (isMove(event.rdf_type)) {
    const moveEvent = JSON.parse(JSON.stringify(event)) as MoveUpdateDTO;

    return service.updateMoveEvent(moveEvent)
      .then(() => {
        const message = `${t('Event.name')} ${moveEvent.uri} ${t('component.common.success.update-success-message')}`;
        opensilex.showSuccessToast(message);
        emit('onUpdate', moveEvent as any);
      })
      .catch((error: any) => {
        opensilex.errorHandler(error, error.response?.result?.message);
        throw error;
      });
  }

  return service.updateEvent(event as EventUpdateDTO)
    .then(() => {
      const message = `${t('Event.name')} ${event.uri} ${t('component.common.success.update-success-message')}`;
      opensilex.showSuccessToast(message);
      emit('onUpdate', event as any);
    })
    .catch((error: any) => {
      opensilex.errorHandler(error, error.response?.result?.message);
      throw error;
    });
}

function initForm(event: EventCreationDTO) {
  if (!event) {
    return;
  }

  const eventCopy = JSON.parse(JSON.stringify(event));

  if (!eventCopy.targets) {
    eventCopy.targets = [];
  }

  if (props.target) {
    eventCopy.targets.push(props.target);
  }

  if (props.eventCreatedTime) {
    eventCopy.start = props.eventCreatedTime.time;
    eventCopy.end = props.eventCreatedTime.time;
    eventCopy.is_instant = true;
  }

  if (props.defaultEventType) {
    eventCopy.rdf_type = props.defaultEventType;
  }

  return eventCopy;
}

function successMessage(_event: EventCreationDTO) {
  return t('Event.name');
}

function isMove(type: string | undefined): boolean {
  if (!type) {
    return false;
  }

  return opensilex.Oeev.checkURIs(type, opensilex.Oeev.MOVE_TYPE_URI);
}

defineExpose({
  showCreateForm,
  showEditForm
});
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  Event:
    name: The event
    add: Add an event
    add-multiple: Add events
    edit: Edit an event
    uri-help: Event URI (autogenerated if empty)
    uri-example: "os-event:trouble1"
    type-placeholder: Select an event type
    type-help: Event type URI
    type-example: "oeev:Trouble"
    description: Description of the event
    start: Begin
    start-help: Beginning of event, only if the event is not instantaneous
    targets: Targets
    targets-help: Object(s) concerned by this function are “Device” and “Scientific Objects”
    target: Target
    target-help: Object targeted by the event (Must exist)
    targets-example: "os-so:plant1"
    uri-error: Creation canceled, URI not found - {error}
    end: End
    end-help: End of event, required if the event is instantaneous
    list-title: Events
    is-instant: Instantaneous event
    is-instant-help: Indicate if the event is instantaneous or not
    is-instant-example: "true or false"
    event: Event
    creator: Creator
    publisher: Publisher
    datePublication: Issued
    lastUpdateDate: Modified
    specific-properties: Specific properties
    multiple-insert: event(s) registered.
    move-multiple-insert: move(s) registered.
fr:
  Event:
    name: "L'événement"
    add: Ajouter un événement
    add-multiple: Ajouter des événements
    edit: Éditer un événement
    uri-help: URI de l'évenement (auto-générée si vide)
    uri-example: "os-event:trouble1"
    type-placeholder: Selectionner un type d'événement
    type-help: URI du type d'événement
    type-example: "oeev:Trouble"
    description: "Description de l'événement"
    targets: Concerne
    targets-help: Objet(s) concerné(s) sont "Appareils" et "Objets scientifiques"
    targets-example: "os-so:plant1"
    target-help: URI de l'objet concerné par l'évènement (Doit exister).
    uri-error: Création annulée, URI non trouvée - {error}
    start: Début
    start-help: Début de l'événement, uniquement si celui-ci n'est pas instantané
    end: Fin
    end-help: Fin de l'événement, requis si celui-ci est instantané
    list-title: "Événements"
    is-instant: "Événement instantané"
    is-instant-help: Indique si l'évenement est instantané ou non
    is-instant-example: true or false
    event: Évenement
    creator: Créateur
    publisher: Publieur
    datePublication: Publié
    lastUpdateDate: Modifié
    specific-properties: Propriétés spécifiques
    multiple-insert: événement(s) enregistré(s)
    move-multiple-insert: déplacement(s) enregistré(s)
</i18n>