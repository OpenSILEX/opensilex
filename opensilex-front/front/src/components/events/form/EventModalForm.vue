    <template>
        <opensilex-ModalForm
            v-if="user.hasCredential(credentials.CREDENTIAL_EVENT_MODIFICATION_ID) && renderModalForm"
            ref="modalForm"
            modalSize="lg"
            :tutorial="false"
            component="opensilex-EventForm"
            createTitle="Event.add"
            editTitle="Event.edit"
            icon="ik#ik-activity"
            :initForm="initForm"
            :successMessage="successMessage"
            :createAction="create"
            :updateAction="update"
            @onCreate="$emit('onCreate', $event)"
            @onUpdate="$emit('onUpdate', $event)"
        ></opensilex-ModalForm>

    </template>


    <script lang="ts">
    import {Component, Prop, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import ModalForm from "../../common/forms/ModalForm.vue";
    import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
    import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";
    import {EventsService} from "opensilex-core/api/events.service";

    import PositionsView from "../../positions/view/PositionsView.vue";
    import MoveForm from "./MoveForm.vue";
    import {VueJsOntologyExtensionService} from "../../../lib";
    import EventForm from "./EventForm.vue";
    import { EventCreationDTO, MoveCreationDTO, ObjectUriResponse, PositionCreationDTO } from 'opensilex-core/index';

    @Component
    export default class EventModalForm extends Vue {

        $opensilex: OpenSilexVuePlugin;
        $store: any;
        service: EventsService;
        vueOntologyService: VueJsOntologyExtensionService;
        $i18n: any;

        @Prop()
        target: string;

        @Prop()
        defaultEventType: string;

        @Prop()
        editMode;

        @Prop()
        eventCreatedTime: any;

        @Prop()
        context: any;

        get user() {
            return this.$store.state.user;
        }

        get credentials() {
            return this.$store.state.credentials;
        }

        renderModalForm: boolean = false;
        @Ref("modalForm") readonly modalForm!: ModalForm;

        created() {
            this.service = this.$opensilex.getService("opensilex.EventsService");
            this.vueOntologyService = this.$opensilex.getService("opensilex.VueJsOntologyExtensionService");
        }

        showCreateForm() {
            this.renderModalForm = true;
            this.$nextTick(() => {
                let form: EventForm = this.modalForm.getFormRef();
                form.setContext(this.context);

                this.modalForm.showCreateForm();
            });
        }

        showEditForm(uri: string, type: string) {

            this.renderModalForm = true;
            this.$nextTick(() => {

                // determine if the event is a move or not since the called service differ
                let isMove = this.isMove(type);

                let eventPromise = isMove ?
                    this.service.getMoveEvent(uri) :
                    this.service.getEventDetails(uri);

                eventPromise.then(http => {
                    let dto = http.response.result;

                    if (isMove) {
                        EventModalForm.convertMoveDtoToMoveForm(dto);
                    }

                    let form: EventForm = this.modalForm.getFormRef();
                    form.typeSwitch(dto.rdf_type,true);
                    form.setContext(this.context);

                    this.modalForm.showEditForm(dto);
                })
            });

        }

        create(event: EventCreationDTO) {
            let isMove = this.isMove(event.rdf_type);
            EventModalForm.convertFormToDto(event,isMove);

            let events = [event];
            let createPromise = isMove ?
                this.service.createMoves(events) :
                this.service.createEvents(events);

            return createPromise.then((http: HttpResponse<OpenSilexResponse<ObjectUriResponse>>) => {

                let message = this.$i18n.t("Event.name") + " " + http.response.result + " " + this.$i18n.t("component.common.success.creation-success-message");
                this.$opensilex.showSuccessToast(message);

                event.uri = http.response.result.toString();
                this.$emit("onCreate", event);

            }).catch((error) => {
                if (error.status == 409) {
                    this.$opensilex.errorHandler(error, this.$i18n.t("component.user.errors.user-already-exists"));
                } else {
                    this.$opensilex.errorHandler(error,error.response.result.message);
                }
            });
        }

        update(event) {

            let isMove = this.isMove(event.rdf_type);

            EventModalForm.convertFormToDto(event,isMove);

            let updatePromise = isMove?
                this.service.updateMoveEvent(event) :
                this.service.updateEvent(event);

            return updatePromise.then(() => {

                let message = this.$i18n.t("Event.name") + " " + event.uri + " " + this.$i18n.t("component.common.success.update-success-message");
                this.$opensilex.showSuccessToast(message);

                this.$emit("onUpdate", event);
            }).catch((error) => {
                this.$opensilex.errorHandler(error,error.response.result.message);
            });
        }

        initForm(event: EventCreationDTO) {
            if (!event) {
                return;
            }

            let eventCopy = JSON.parse(JSON.stringify(event));

            if (!eventCopy.targets) {
                eventCopy.targets = [];
            }
            if (this.target) {
                eventCopy.targets.push(this.target);
            }

            if(this.eventCreatedTime){
                eventCopy.start = this.eventCreatedTime.time;
                eventCopy.end = this.eventCreatedTime.time;
                eventCopy.is_instant = true;
            }

            if (this.defaultEventType) {
                eventCopy.rdf_type = this.defaultEventType;
            }
            return eventCopy;
        }

        successMessage(event: EventCreationDTO) {
            return this.$i18n.t("EventView.name");
        }

        static convertMoveDtoToMoveForm(move){

            if (!move.targets_positions || move.targets_positions.length == 0) {
                move.targets_positions = PositionsView.getEmptyForm();
            }
            if (!move.targets_positions) {
                move.targets_positions = MoveForm.getEmptyTargetsPositions();
            }
            if (move.from && move.from.uri) {
                move.from = move.from.uri;
            }
            if (move.to && move.to.uri) {
                move.to = move.to.uri;
            }
        }

        static convertFormToDto(event: MoveCreationDTO, isMove: boolean) {
            if (isMove) {
                EventModalForm.convertMoveFormToMoveDto(event);
            } else {
                event.targets_positions = undefined;
            }

            if (event.is_instant) {
                event.start = undefined;
            }

        }

        static convertMoveFormToMoveDto(move){

            if (move.from && move.from.uri) {
                move.from = move.from.uri;
            }
            if (move.to && move.to.uri) {
                move.to = move.to.uri;
            }

            // to the moment the form only handle on position for the first or for all targets
            if (move.targets_positions && move.targets_positions.length == 1) {

                let position = move.targets_positions[0].position;

                if (EventModalForm.isPositionValid(position)) {

                    // one position on one target
                    if (move.targets.length == 1) {
                        move.targets_positions[0].target = move.targets[0];
                    } else {
                        // one position unique for each target
                        move.targets_positions = move.targets.map(target => ({
                            target: target,
                            position: position
                        }));
                    }
                } else {
                    move.targets_positions = undefined;
                }
            }
        }

        isMove(type): boolean {
            if (!type) {
                return false;
            }
            return this.$opensilex.Oeev.checkURIs(type, this.$opensilex.Oeev.MOVE_TYPE_URI);
        }

        static isPositionValid(position: PositionCreationDTO): boolean {
            if (!position) {
                return false;
            }

            // position is valid if all property of position are not undefined or empty
            let allPropertiesUndefined = (!position.point && (!position.text || position.text.length == 0) && !position.x && !position.y && !position.z);
            return !allPropertiesUndefined;
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
            uri-example: "os-event:trouble1"
            type-placeholder: Select an event type
            type-help: Event type URI
            type-example: "oeev:Trouble"
            description: Description of the event
            start: Begin
            start-help: Begin of event, only if the event is not instantaneous
            start-example: "2019-09-08T13:00:00+01:00"
            targets: Targets
            targets-help: Object(s) concerned by this function are “Device” and “Scientific Objects” 
            target: Target
            target-help: Object targeted by the event (Must exist)
            targets-example: "os-so:plant1"
            end: End
            end-help: End of event, required if the event is instantaneous
            list-title: Events
            is-instant: Instantaneous event
            is-instant-help: Indicate if the event is instantaneous or not
            is-instant-example: "true or false"
            event: Event
            creator: Creator
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
            targets-help: Objet(s) concerné(s) sont "Dispositifs" et "Objets scientifiques"
            targets-example: "os-so:plant1"
            target-help: URI de l'objet concerné par l'évènement (Doit exister).
            start: Début
            start-help: Début de l'événement, uniquement si celui-ci n'est pas instantané
            start-example: 2019-09-08T13:00:00+01:00"
            end: Fin
            end-help: Fin de l'événement, requis si celui-ci est instantané
            list-title: "Événements"
            is-instant: "Événement instantané"
            is-instant-help: Indique si l'évenement est instantané ou non
            is-instant-example: true or false
            event: Évenement
            creator: Créateur
            specific-properties: Propriétés spécifiques
            multiple-insert: événement(s) enregistré(s)
            move-multiple-insert: déplacement(s) enregistré(s)
    </i18n>