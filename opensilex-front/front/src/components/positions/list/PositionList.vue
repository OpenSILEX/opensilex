<template>
    <div class="row">
        <div class="col col-xl-12">

            <div class="card">
                <div class="card-header" v-if="displayTitle">
                    <h3>
                        <i class="ik ik-clipboard"></i>
                        {{ $t('Position.list-title') }}
                    </h3>
                </div>

                <div class="card-body">
                    <opensilex-PageActions>

                        <opensilex-CreateButton
                            v-if="user.hasCredential(modificationCredentialId)"
                            label="Move.add"
                            @click="eventModalForm.showCreateForm()"
                        ></opensilex-CreateButton>

                        <opensilex-CreateButton
                            v-if="user.hasCredential(modificationCredentialId)"
                            label="OntologyCsvImporter.import"
                            @click="showCsvForm"
                        ></opensilex-CreateButton>
                    </opensilex-PageActions>

                    <opensilex-PageContent>
                        <template v-slot>

                            <opensilex-TableAsyncView
                                ref="tableRef"
                                :searchMethod="search"
                                :fields="fields"
                                :isSelectable="isSelectable">

                                <template v-slot:cell(uri)="{data}">
                                    <opensilex-UriLink
                                        :uri="data.item.event"
                                        :value="data.item.event"
                                        @click="showEventView(data.item)"
                                    ></opensilex-UriLink>
                                </template>

                                <template v-slot:cell(to)="{data}">
                                    <opensilex-StringView
                                        v-if="data.item.to"
                                        :value="data.item.to.name">
                                    </opensilex-StringView>
                                </template>

                                <template v-slot:cell(end)="{data}">
                                    <opensilex-StringView
                                        v-if="data.item.move_time"
                                        :value="new Date(data.item.move_time).toLocaleString()">
                                    </opensilex-StringView>
                                </template>

                                <template v-slot:cell(coordinates)="{data}">
                                    <opensilex-GeometryView
                                        v-if="data.item.position && data.item.position.point"
                                        label="Position.coordinates"
                                        :value="data.item.position.point"
                                        :displayLabel="false"
                                    ></opensilex-GeometryView>
                                </template>

                                <template v-slot:cell(custom_coordinates)="{data}">
                                    <opensilex-StringView :value="customCoordinatesText(data.item.position)">
                                    </opensilex-StringView>
                                </template>

                                <template v-slot:cell(textual_position)="{data}">
                                    <opensilex-StringView
                                        v-if="data.item.position && data.item.position.text"
                                        :value="data.item.position.text">
                                    </opensilex-StringView>
                                </template>

                                <template v-slot:cell(actions)="{data}">
                                    <b-button-group size="sm">
                                        <opensilex-EditButton
                                            v-if="! modificationCredentialId || user.hasCredential(modificationCredentialId)"
                                            @click="editEvent(data.item)"
                                            :small="true"
                                        ></opensilex-EditButton>
                                        <opensilex-DeleteButton
                                            v-if="! deleteCredentialId || user.hasCredential(deleteCredentialId)"
                                            @click="deleteEvent(data.item.event)"
                                            label="EventForm.delete"
                                            :small="true"
                                        ></opensilex-DeleteButton>
                                    </b-button-group>
                                </template>
                            </opensilex-TableAsyncView>
                        </template>
                    </opensilex-PageContent>

                    <opensilex-EventModalView
                        modalSize="lg"
                        ref="eventModalView"
                        :dto.sync="selectedEvent"
                        :type.sync="selectedEvent.type"
                    ></opensilex-EventModalView>

                    <opensilex-EventModalForm
                        ref="eventModalForm"
                        :target="target"
                        defaultEventType="oeev:Move"
                        @onCreate="refresh"
                        @onUpdate="refresh"
                    ></opensilex-EventModalForm>

                    <opensilex-EventCsvForm
                        ref="moveCsvForm"
                        @csvImported="onImport"
                        :targets="[this.target]"
                        :isMove="true"
                    ></opensilex-EventCsvForm>

                </div>
            </div>
        </div>
    </div>

</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";

import {PositionsService} from "opensilex-core/api/positions.service";

import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";
import {EventsService} from "opensilex-core/api/events.service";
import {MoveDetailsDTO} from "opensilex-core/model/moveDetailsDTO";
import MoveForm from "../../events/form/MoveForm.vue";
import EventModalView from "../../events/view/EventModalView.vue";
import EventModalForm from "../../events/form/EventModalForm.vue";
import EventCsvForm from "../../events/form/csv/EventCsvForm.vue";

@Component
export default class PositionList extends Vue {

    @Ref("tableRef") readonly tableRef!: any;
    @Ref("eventModalView") readonly eventModalView!: EventModalView;
    @Ref("eventModalForm") readonly eventModalForm!: EventModalForm;
    @Ref("moveCsvForm") readonly moveCsvForm!: EventCsvForm;

    showCsvForm() {
        this.moveCsvForm.show();
    }

    $opensilex: OpenSilexVuePlugin;
    $positionService: PositionsService;
    $eventService: EventsService;
    $i18n: any;
    $store: any;

    @Prop({
        default: false
    })
    isSelectable;

    @Prop()
    modificationCredentialId;

    @Prop()
    deleteCredentialId;

    @Prop({default: true})
    enableActions;

    @Prop({default: () => new Set(["from", "to", "end", "coordinates", "custom_coordinates", "textual_position"])})
    columnsToDisplay: Set<string>;

    @Prop({default: false})
    displayFilters: boolean;

    @Prop({default: false})
    displayTitle: boolean;

    selectedEvent = {};

    @Prop()
    target;

    baseType: string;

    minPageSize = 5;

    created() {
        this.$positionService = this.$opensilex.getService("opensilex.PositionsService");
        this.$eventService = this.$opensilex.getService("opensilex.EventsService");
    }

    get user() {
        return this.$store.state.user;
    }

    get credentials() {
        return this.$store.state.credentials;
    }

    refresh() {
        this.tableRef.refresh();
    }

    search(options) {
        return this.$positionService.searchPositionHistory(
            this.target,
            undefined,
            undefined,
            options.orderBy,
            options.currentPage,
            options.pageSize < this.minPageSize ? this.minPageSize : options.pageSize
        );
    }

    successMessage(form) {
        return this.$t("ResultModalView.data-imported");
    }


    get fields() {

        let tableFields = [];

        tableFields.push({key: "uri", label: "component.common.uri", sortable: true});

        if (this.columnsToDisplay.has("to")) {
            tableFields.push({key: "to", label: "Move.location", sortable: true});
        }

        if (this.columnsToDisplay.has("end")) {
            tableFields.push({key: "end", label: "Position.end", sortable: true});
        }

        if (this.columnsToDisplay.has("coordinates")) {
            tableFields.push({key: "coordinates", label: "Position.coordinates", sortable: false});
        }
        if (this.columnsToDisplay.has("custom_coordinates")) {
            tableFields.push({key: "custom_coordinates", label: "Position.custom-coordinates", sortable: false});
        }
        if (this.columnsToDisplay.has("textual_position")) {
            tableFields.push({key: "textual_position", label: "Position.textual-position", sortable: false});
        }


        if (this.enableActions) {
            tableFields.push({key: "actions", label: "component.common.actions", sortable: false});
        }

        return tableFields;
    }

    deleteEvent(uri: string) {
        this.$eventService.deleteEvent(uri).then(() => {
            this.refresh();

            let message = this.$i18n.t("Event.name") + " " + uri + " " + this.$i18n.t("component.common.success.delete-success-message");
            this.$opensilex.showSuccessToast(message);

            this.$emit('onDelete', uri);

        }).catch(this.$opensilex.errorHandler);
    }


    private getEventPromise(position): Promise<HttpResponse<OpenSilexResponse<MoveDetailsDTO>>> {
        return this.$eventService.getMoveEvent(position.event);
    }

    showEventView(position) {
        this.getEventPromise(position).then((http: HttpResponse<OpenSilexResponse>) => {
            this.selectedEvent = http.response.result;
            this.eventModalView.show();
        }).catch(this.$opensilex.errorHandler);
    }

    editEvent(position) {
        this.getEventPromise(position).then((http: HttpResponse<OpenSilexResponse<MoveDetailsDTO>>) => {
            let move: MoveDetailsDTO = http.response.result;
            if (!move.targets_positions) {
                move.targets_positions = MoveForm.getEmptyTargetsPositions();
            }
            this.eventModalForm.showEditForm(move);
        }).catch(this.$opensilex.errorHandler);
    }

    getItemsToDisplay(targets) {
        return targets.slice(0, 3);
    }

    customCoordinatesText(position: any): string {

        if (!position) {
            return undefined;
        }

        let customCoordinates = "";

        if (position.x) {
            customCoordinates += "X : " + position.x;
        }
        if (position.y) {
            if (customCoordinates.length > 0) {
                customCoordinates += ", ";
            }
            customCoordinates += "Y : " + position.y;
        }
        if (position.z) {
            if (customCoordinates.length > 0) {
                customCoordinates += ", ";
            }
            customCoordinates += "Z : " + position.z;
        }

        if (customCoordinates.length == 0) {
            return undefined;
        }
        return "(" + customCoordinates + ")";
    }

    onImport(response) {
        this.refresh();
    }

}
</script>


<style scoped lang="scss">
</style>

<i18n>

en:
    Position:
        list-title: Positions
        end: Arrival date
fr:
    Position:
        list-title: Positions
        end: "Date d'arrivée"


</i18n>