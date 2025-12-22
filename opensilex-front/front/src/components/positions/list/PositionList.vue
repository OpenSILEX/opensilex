<template>
    <div>
        <opensilex-PageActions class="pageActions">
            <opensilex-CreateButton
                v-if="user.hasCredential(modificationCredentialId)"
                label="Move.add"
                @click="showForm"
                class="createButton"
            ></opensilex-CreateButton>

            <opensilex-CreateButton
                v-if="user.hasCredential(modificationCredentialId)"
                label="OntologyCsvImporter.import"
                @click="showCsvForm"
                class="createButton"
            ></opensilex-CreateButton>
        </opensilex-PageActions>
    
    <div class="card">
        <opensilex-PageContent v-if="renderComponent">
            <template v-slot>
                <div class="card-body">
                    <opensilex-TableAsyncView
                        ref="tableRef"
                        :searchMethod="search"
                        :fields="fields"
                        defaultSortBy="end"
                        :defaultSortDesc="true"
                        :isSelectable="isSelectable">

                        <template v-slot:cell(end)="{data}">
                          <opensilex-DateView :value="data.item.end"></opensilex-DateView>
                        </template>

                        <template v-slot:cell(actions)="{data}">
                            <b-button-group size="sm">
                                <opensilex-DetailButton
                                    :detailVisible="data.detailsShowing"
                                    :small="true"
                                    label="Position.details"
                                    @click="showDetails(data)"
                                ></opensilex-DetailButton>
                                <opensilex-EditButton
                                    v-if="! modificationCredentialId || user.hasCredential(modificationCredentialId)"
                                    @click="editEvent(data.item.uri)"
                                    :small="true"
                                ></opensilex-EditButton>
                                <opensilex-DeleteButton
                                    v-if="! deleteCredentialId || user.hasCredential(deleteCredentialId)"
                                    @click="deleteEvent(data.item.uri)"
                                    label="EventForm.delete"
                                    :small="true"
                                ></opensilex-DeleteButton>
                            </b-button-group>
                        </template>

                        <template v-slot:row-details="{ data }">
                          <ul>
                            <li v-if="data.item.location.to">
                              <opensilex-UriLink
                                  :uri="data.item.location.to"
                                  :value="data.item.location.to"
                                  :to="{ path:'/facility/details/'+ encodeURIComponent(data.item.location.to),}"
                                  target="_blank"
                              ></opensilex-UriLink>
                            </li>
                            <li v-if="data.item.location && (data.item.location.x || data.item.location.y || data.item.location.z)">{{customCoordinatesText(data.item.location)}}</li>
                            <li v-if="data.item.location && data.item.location.text">{{data.item.location.text}}</li>
                            <li v-if="data.item.location && data.item.location.geojson">
                              <opensilex-GeometryCopy label="" :value="data.item.location.geojson">
                              </opensilex-GeometryCopy>
                            </li>
                          </ul>
                        </template>

                    </opensilex-TableAsyncView>
                </div>
            </template>
        </opensilex-PageContent>

        <opensilex-EventModalView
            modalSize="lg"
            ref="eventModalView"
        ></opensilex-EventModalView>

        <opensilex-EventModalForm
            v-if="user.hasCredential(credentials.CREDENTIAL_EVENT_MODIFICATION_ID)"
            ref="modalForm"
            :target="target"
            defaultEventType="oeev:Move"
            @onCreate="refresh"
            @onUpdate="refresh"
        ></opensilex-EventModalForm>

        <opensilex-EventCsvForm
            v-if="renderCsvForm"
            ref="csvForm"
            @csvImported="onImport"
            :targets="[this.target]"
            :isMove="true"
        ></opensilex-EventCsvForm>

    </div>
    </div>

</template>

<script lang="ts">
import {Component, Prop, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import {stringify} from "wkt";

import {PositionsService} from "opensilex-core/api/positions.service";

import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";
import {EventsService} from "opensilex-core/api/events.service";
import EventModalView from "../../events/view/EventModalView.vue";
import EventModalForm from "../../events/form/EventModalForm.vue";
import EventCsvForm from "../../events/form/csv/EventCsvForm.vue";
import { MoveDetailsDTO } from 'opensilex-core/index';
import {EventDetailsDTO} from "opensilex-core/model/eventDetailsDTO";
import {LocationObservationDTO} from "opensilex-core/model/locationObservationDTO";

@Component
export default class PositionList extends Vue {

    @Ref("tableRef") readonly tableRef!: any;
    @Ref("eventModalView") readonly eventModalView!: EventModalView;
    @Ref("modalForm") readonly modalForm!: EventModalForm;
    @Ref("csvForm") readonly csvForm!: EventCsvForm;

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

    @Prop({default: PositionList.getDefaultColumns()})
    columnsToDisplay: Set<string>;

    static getDefaultColumns(): Set<string> {
        return new Set(["end"]);
    }


    @Prop({default: false})
    displayFilters: boolean;

    @Prop({default: false})
    displayTitle: boolean;

    @Prop()
    target;

    baseType: string;

    minPageSize = 5;

    renderComponent = true;

    @Watch("target")
    onTargetChange() {
        this.renderComponent = false;

        this.$nextTick(() => {
            // Add the component back in
            this.renderComponent = true;
        });
    }

    renderCsvForm = false;

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

    showForm() {
        this.$nextTick(() => {
            this.modalForm.showCreateForm();
        });
    }

    showCsvForm() {
        this.renderCsvForm = true;
        this.$nextTick(() => {
            this.csvForm.show();
        });

    }

    successMessage(form) {
        return this.$t("ResultModalView.data-imported");
    }

    get fields() {

        let tableFields = [];

        if (this.columnsToDisplay.has("end")) {
            tableFields.push({key: "end", label: "Position.end", sortable: true});
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

    showDetails(data) {
      data.toggleDetails();
    }
    editEvent(uri) {
        this.modalForm.showEditForm(uri, this.$opensilex.Oeev.MOVE_TYPE_URI);
    }

    customCoordinatesText(location: LocationObservationDTO): string {

        if (!location) {
            return undefined;
        }

        let customCoordinates = "";

        if (location.x) {
            customCoordinates += "X : " + location.x;
        }
        if (location.y) {
            if (customCoordinates.length > 0) {
                customCoordinates += ", ";
            }
            customCoordinates += "Y : " + location.y;
        }
        if (location.z) {
            if (customCoordinates.length > 0) {
                customCoordinates += ", ";
            }
            customCoordinates += "Z : " + location.z;
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
.createButton{
    margin-left: 10px;
    margin-top: 10px
}
.pageActions {
    margin-bottom: 10px;
    margin-left: -10px; 
}
</style>

<i18n>

en:
    Position:
        list-title: Positions
        end: Arrival date
        details: Show or hide position details
fr:
    Position:
        list-title: Positions
        end: "Date d'arrivée"
        details: Afficher ou masquer les détails de la position




</i18n>