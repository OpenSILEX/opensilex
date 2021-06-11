    <template>
        <opensilex-ModalForm
            ref="modalForm"
            modalSize="lg"
            :tutorial="false"
            component="opensilex-EventForm"
            createTitle="Event.add"
            editTitle="Event.edit"
            icon="ik#ik-activity"
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
    import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";
    import {EventsService} from "opensilex-core/api/events.service";
    // @ts-ignore
    import {EventCreationDTO} from "opensilex-core/model/eventCreationDTO";
    // @ts-ignore
    import {ObjectUriResponse} from "opensilex-core/model/objectUriResponse";
    import EventForm from "./EventForm.vue";
    import PositionsView from "../../positions/view/PositionsView.vue";
    // @ts-ignore
    import {MoveCreationDTO} from "opensilex-core/model/moveCreationDTO";
    // @ts-ignore
    import {PositionCreationDTO} from "opensilex-core/model/positionCreationDTO";
    import MoveForm from "./MoveForm.vue";
    import {VueJsOntologyExtensionService, VueRDFTypeDTO} from "../../../lib";
    import Oeev from "../../../ontologies/Oeev";
    import {EventDetailsDTO} from "opensilex-core/model/eventDetailsDTO";

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

        get user() {
            return this.$store.state.user;
        }

        get credentials() {
            return this.$store.state.credentials;
        }

        @Ref("modalForm") readonly modalForm!: ModalForm;

        created() {
            this.service = this.$opensilex.getService("opensilex.EventsService");
            this.vueOntologyService = this.$opensilex.getService("opensilex.VueJsOntologyExtensionService");
        }

        showCreateForm() {
            this.modalForm.getFormRef().resetTypeModel();
            this.modalForm.showCreateForm();
        }

        getEventPromise(isMove: boolean, uri: string, typeModel: VueRDFTypeDTO, reject): Promise<void | Promise<HttpResponse<OpenSilexResponse>>>{

            let getEventPromise = isMove ?
                this.service.getMoveEvent(uri) :
                this.service.getEventDetails(uri);

            return getEventPromise.then(http => {
                let event = http.response.result;

                if (isMove) {
                    EventModalForm.convertMoveDtoToMoveForm(event);
                }
                EventModalForm.convertToMultiValuedRelations(event, typeModel);

                this.modalForm.showEditForm(event);

            }).catch(reject);
        }

        showEditForm(uri, type) {

            this.modalForm.getFormRef().setBaseType(this.$opensilex.Oeev.EVENT_TYPE_URI);

            let isMove = this.isMove(type, this.$opensilex.Oeev);

            // first Promise : get type Model associated to event type
            new Promise((resolve, reject) => {
                this.vueOntologyService
                    .getRDFTypeProperties(type, this.$opensilex.Oeev.EVENT_TYPE_URI)
                    .then(http => {

                        // second Promise : get event/move
                        let getEventPromise =  this.getEventPromise(isMove, uri, http.response.result, reject);

                        Promise.resolve(getEventPromise).then(() => {
                            resolve(http);
                        });

                    }).catch(reject => this.$opensilex.errorHandler(reject));
            });
        }

        create(event: EventCreationDTO) {

            let isMove = this.isMove(event.rdf_type,this.$opensilex.Oeev);
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

            let isMove = this.isMove(event.rdf_type,this.$opensilex.Oeev);

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

            if (this.defaultEventType) {
                eventCopy.rdf_type = this.defaultEventType;
            }
            return eventCopy;
        }

        successMessage(event: EventCreationDTO) {
            return this.$i18n.t("EventView.name");
        }

        static getValuesByProperty(event: EventCreationDTO, typeModel: VueRDFTypeDTO) {
            let valueByProperties = {};

            for (let i in event.relations) {
                let relation = event.relations[i];
                let property = relation.property;

                let propertyModel = typeModel.object_properties.find(propertyModel => propertyModel.property == property)
                if(! propertyModel){
                    propertyModel = typeModel.data_properties.find(propertyModel => propertyModel.property == property)
                }

                if(propertyModel.is_list){

                    // create a new array if the relation is not already defined into map
                    if(! valueByProperties[relation.property]){
                        valueByProperties[relation.property] = [];
                    }
                    // append value into array
                    valueByProperties[relation.property].push(relation.value);

                }else{
                    valueByProperties[relation.property] = relation.value
                }
            }
            return valueByProperties;
        }

        static convertToMultiValuedRelations(event,typeModel){

            // compute relations values (by grouping multivalued relation into an array)
            let valueByProperties = EventModalForm.getValuesByProperty(event,typeModel);

            event.relations = [];

            for (const [property, value] of Object.entries(valueByProperties)) {
                event.relations.push({
                    property: property,
                    value: value
                })
            }
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

            EventModalForm.convertToMonoValuedRelations(event);
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

        static convertToMonoValuedRelations(event: EventCreationDTO){

            if (event.relations) {
                let newRelations = [];

                for (let i in event.relations) {
                    let relation = event.relations[i];

                    if (relation.value != null) {

                        // if the relation is multi-valued then decompose it into multiple mono-valued relation
                        // since the service is waiting for an array of mono-valued relation

                        if (Array.isArray(relation.value)) {
                            for (let j in relation.value) {
                                newRelations.push({
                                    property: relation.property,
                                    value: relation.value[j],
                                });
                            }
                        }else{
                            if(relation.value.length > 0){
                                newRelations.push({
                                    property: relation.property,
                                    value: relation.value,
                                });
                            }

                        }
                    }
                }

                event.relations = newRelations;
            }
        }

        isMove(type, oeev): boolean {
            if (!type) {
                return false;
            }
            return (type == oeev.MOVE_TYPE_URI || type == oeev.MOVE_TYPE_PREFIXED_URI);
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
            description: Description
            start: Begin
            start-help: Begin of event, only if the event is not instantaneous
            start-example: "2019-09-08T13:00:00+01:00"
            targets: Targets
            targets-help: Object(s) targeted by the event (Must exist)
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
            description: Description
            targets: Concerne
            targets-help: Objet(s) concerné(s) par l'événement
            targets-example: "os-so:plant1"
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