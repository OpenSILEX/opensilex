<template>
    <opensilex-ModalForm
            ref="modalForm"
            modalSize="lg"
            :tutorial="false"
            component="opensilex-EventForm"
            createTitle="Event.add"
            editTitle="Event.edit"
            icon="fa#vials"
            :createAction="create"
            :updateAction="update"
            :initForm="initForm"
            :successMessage="successMessage"
    ></opensilex-ModalForm>

</template>


<script lang="ts">
    import {Component, Prop, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import ModalForm from "../../common/forms/ModalForm.vue";
    import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
    import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";
    import {EventsService} from "opensilex-core/api/events.service";
    import {EventCreationDTO} from "opensilex-core/model/eventCreationDTO";
    import {ObjectUriResponse} from "opensilex-core/model/objectUriResponse";
    import EventForm from "./EventForm.vue";
    import PositionsView from "../../positions/view/PositionsView.vue";
    import {MoveCreationDTO} from "opensilex-core/model/moveCreationDTO";
    import {PositionCreationDTO} from "opensilex-core/model/positionCreationDTO";
    import {ConcernedItemPositionCreationDTO} from "opensilex-core/model/concernedItemPositionCreationDTO";

    @Component
    export default class EventModalForm extends Vue {

        $opensilex: OpenSilexVuePlugin;
        $store: any;
        service: EventsService;
        $i18n: any;

        @Prop()
        target: string;

        @Prop()
        defaultEventType: string;

        @Prop()
        editMode;

        get user() {
            return this.$store.state.user;
        }

        get credentials() {
            return this.$store.state.credentials;
        }

        @Ref("modalForm") readonly modalForm!: ModalForm;

        created() {
            this.service = this.$opensilex.getService("opensilex.EventsService");
        }
        showCreateForm() {
            this.modalForm.showCreateForm();
        }

        showEditForm(event) {

            if(this.isMove(event)){
                let move = JSON.parse(JSON.stringify(event));

                if(! event.concerned_item_positions || event.concerned_item_positions.length == 0){
                    move.concerned_item_positions = PositionsView.getEmptyForm();
                }
                if (move.from && move.from.uri) {
                    move.from = move.from.uri;
                }
                if (move.to && move.to.uri) {
                    move.to = move.to.uri;
                }
                this.modalForm.showEditForm(move)
            }else {
                this.modalForm.showEditForm(event);
            }
        }

        isMove(event: EventCreationDTO) : boolean {
            if(! event.rdf_type) {
                return false;
            }
            return ( event.rdf_type == this.$opensilex.Oeev.MOVE_TYPE_URI || event.rdf_type == this.$opensilex.Oeev.MOVE_TYPE_PREFIXED_URI);
        }


        private static isPositionValid(position : PositionCreationDTO) : boolean{
            if(!position){
                return false;
            }

            // position is valid if all property of position are not undefined or empty
            let allPropertiesUndefined =  ( !position.point && (!position.text || position.text.length == 0) && !position.x && !position.y && !position.z );
            return ! allPropertiesUndefined;
        }

        getCheckedEvent(event: MoveCreationDTO) {
            let eventUpdate;

            if (this.isMove(event)) {
                eventUpdate = JSON.parse(JSON.stringify(event))

                if (eventUpdate.from && eventUpdate.from.uri) {
                    eventUpdate.from = eventUpdate.from.uri;
                }
                if (eventUpdate.to && eventUpdate.to.uri) {
                    eventUpdate.to = eventUpdate.to.uri;
                }


                // to the moment the form only handle on position for the first or for all targets
                if (eventUpdate.targets_positions && eventUpdate.targets_positions.length == 1) {

                    let position = eventUpdate.targets_positions[0].position;

                    if (EventModalForm.isPositionValid(position)) {

                        // one position on one target
                        if(eventUpdate.targets.length == 1) {
                            eventUpdate.targets_positions[0].target = eventUpdate.targets[0];
                        }else {
                            // one position unique for each target
                            eventUpdate.targets_positions = eventUpdate.targets.map(target => ({target: target, position: position}));
                        }
                    }else{
                        eventUpdate.targets_positions = undefined;
                    }
                }

            } else {
                eventUpdate = Object.assign(EventForm.getEmptyForm(), event);
                eventUpdate.targets_positions = undefined;
            }

            if (eventUpdate.is_instant) {
                eventUpdate.start = undefined;
            }

            if (event.relations) {
                eventUpdate.relations = event.relations.filter(relation => {
                    return relation.value != null;
                });

            }
            return eventUpdate;
        }



        initForm(event: EventCreationDTO){
            if(! event){
                return;
            }

            let eventCopy = JSON.parse(JSON.stringify(event));

            if(! eventCopy.targets){
                eventCopy.targets = [];
            }
            if(this.target){
                eventCopy.targets.push(this.target);
            }

            if(this.defaultEventType){
                eventCopy.rdf_type = this.defaultEventType;
            }
            return eventCopy;
        }

        create(event : EventCreationDTO) {

            let checkedEvents = [this.getCheckedEvent(event)];
            let createPromise = this.isMove(event) ? this.service.createMoves(checkedEvents) : this.service.createEvents(checkedEvents);

            return createPromise.then((http: HttpResponse<OpenSilexResponse<ObjectUriResponse>>) => {

                let message = this.$i18n.t("Event.name") + " " + http.response.result + " " + this.$i18n.t("component.common.success.creation-success-message");
                this.$opensilex.showSuccessToast(message);

                event.uri = http.response.result.toString();
                this.$emit("onCreate", event);
            }).catch(this.$opensilex.errorHandler);
        }

        successMessage(event : EventCreationDTO){
            return this.$i18n.t("EventView.name");
        }

        update(event) {

            let checkedEvent = this.getCheckedEvent(event);
            let updatePromise = this.isMove(event) ? this.service.updateMoveEvent(checkedEvent) : this.service.updateEvent(checkedEvent);

            return updatePromise.then(() => {

                let message = this.$i18n.t("Event.name") + " " + event.uri + " " + this.$i18n.t("component.common.success.update-success-message");
                this.$opensilex.showSuccessToast(message);

                this.$emit("onUpdate", event);
            }).catch(this.$opensilex.errorHandler);
        }
    }

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
        type-placeholder: Select an event type
        type-help: Event type URI
        description: Description
        start: Begin
        start-help: Begin of event, only if the event is not instantaneous
        targets: Targets
        targets-help: Object(s) targeted by the event (Must exist)
        target: Target
        target-help: Object targeted by the event (Must exist)
        end: End
        end-help: End of event, required if the event is instantaneous
        list-title: Events
        is-instant: Instantaneous event
        is-instant-help: Indicate if the event is instantaneous or not
        event: Event
        creator: Creator
        specific-properties: specific properties
        multiple-insert: event(s) registered.
        move-multiple-insert: move(s) registered.
fr:
    Event:
        name: "L'événement"
        add: Ajouter un événement
        add-multiple: Ajouter des événements
        edit: Éditer un événement
        uri-help: URI de l'évenement (auto-générée si vide)
        type-placeholder: Selectionner un type d'événement
        type-help: URI du type d'événement
        description: Description
        targets: Concerne
        targets-help: Objet(s) concerné(s) par l'événement
        start: Début
        start-help: Début de l'événement, uniquement si celui-ci n'est pas instantané
        end: Fin
        end-help: Fin de l'événement, requis si celui-ci est instantané
        list-title: "Événements"
        is-instant: "Événement instantané"
        is-instant-help: Indique si l'évenement est instantané ou non
        event: Évenement
        creator: Créateur
        specific-properties: propriétés spécifiques
        multiple-insert: événement(s) enregistré(s)
        move-multiple-insert: déplacement(s) enregistré(s)

</i18n>