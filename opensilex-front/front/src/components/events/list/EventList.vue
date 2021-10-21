<template>

    <div class="card">

        <opensilex-PageActions>

            <opensilex-CreateButton
                v-if="user.hasCredential(modificationCredentialId)"
                label="Event.add"
                @click="showForm"
            ></opensilex-CreateButton>

            <opensilex-CreateButton
                v-if="user.hasCredential(modificationCredentialId)"
                label="OntologyCsvImporter.import"
                @click="showCsvForm"
            ></opensilex-CreateButton>

        </opensilex-PageActions>

        <opensilex-SearchFilterField
            @search="refresh()"
            @clear="reset()"
            label="component.experiment.search.label"
            :showAdvancedSearch="true"
        >
            <template v-slot:filters>

                <!-- type -->
                <opensilex-FilterField :halfWidth="maximizeFilterSize">
                    <opensilex-TypeForm
                        :type.sync="filter.type"
                        :baseType="baseType"
                        :ignoreRoot="false"
                        placeholder="Event.type-placeholder"
                    ></opensilex-TypeForm>

                </opensilex-FilterField>

                <!-- target -->
                <opensilex-FilterField v-if="displayTargetFilter">
                    <label>{{ $t("Event.targets") }}</label>
                    <opensilex-StringFilter
                        id="target"
                        :filter.sync="filter.target"
                        placeholder="EventList.target-filter-placeholder"
                    ></opensilex-StringFilter>
                </opensilex-FilterField>

                <!-- description -->
                <opensilex-FilterField :halfWidth="maximizeFilterSize">
                    <label>{{ $t("component.common.description") }}</label>
                    <opensilex-StringFilter
                        id="description"
                        :filter.sync="filter.description"
                        placeholder="ExperimentList.filter-label-placeholder"
                    ></opensilex-StringFilter>
                </opensilex-FilterField>

            </template>

            <template v-slot:advancedSearch>
                <opensilex-FilterField :halfWidth="maximizeFilterSize">
                    <opensilex-DateTimeForm
                        :value.sync="filter.start"
                        label="Event.start"
                        :required="false"
                    ></opensilex-DateTimeForm>
                </opensilex-FilterField>

                <opensilex-FilterField :halfWidth="maximizeFilterSize">
                    <opensilex-DateTimeForm
                        :value.sync="filter.end"
                        label="Event.end"
                        :required="false"
                    ></opensilex-DateTimeForm>
                </opensilex-FilterField>

            </template>

        </opensilex-SearchFilterField>
        <opensilex-TableAsyncView
                        ref="tableRef"
                        :searchMethod="search"
                        :fields="fields"
                        defaultSortBy=""
                        labelNumberOfSelectedRow="EventList.selected"
                        iconNumberOfSelectedRow="ik#ik-layers"
                    >

                        <template v-slot:selectableTableButtons="{ numberOfSelectedRows }">
                            <b-dropdown
                            dropright
                            class="mb-2 mr-2"
                            :small="true"
                            :disabled="numberOfSelectedRows == 0"
                            text=actions>
                                <b-dropdown-item-button
                                @click="createDocument()"
                                >{{$t('component.common.addDocument')}}</b-dropdown-item-button>
                            </b-dropdown>
                        </template>

                        <template v-slot:cell(rdf_type_name)="{data}">
                            <opensilex-UriLink
                                v-if="data.item.rdf_type_name"
                                :uri="data.item.uri"
                                :value="data.item.rdf_type_name"
                                @click="showEventView(data.item)"
                            ></opensilex-UriLink>

                        </template>

                        <template v-slot:cell(start)="{data}">
                            <opensilex-TextView v-if="data.item.start && data.item.start.length > 0"
                                                :value="new Date(data.item.start).toLocaleString()">
                            </opensilex-TextView>
                        </template>
                        <template v-slot:cell(end)="{data}">
                            <opensilex-TextView v-if="data.item.end"
                                                :value="new Date(data.item.end).toLocaleString()">
                            </opensilex-TextView>
                        </template>

                        <template v-slot:cell(targets)="{data}">
                            <span :key="index" v-for="(uri, index) in data.item.targets">
                                 <opensilex-TextView :value="uri"></opensilex-TextView>
                                <span v-if="data.item.targets.length > 1 && index < 2"> </span>
                                <span v-if="index >= 2"> ... </span>
                            </span>
                        </template>

                        <template v-slot:cell(description)="{data}">
                            <opensilex-TextView :value="data.item.description"></opensilex-TextView>
                        </template>

                        <template v-slot:cell(actions)="{data}">
                            <b-button-group size="sm">

                                <opensilex-EditButton
                                    v-if="! modificationCredentialId || user.hasCredential(modificationCredentialId)"
                                    @click="editEvent(data.item.uri,data.item.rdf_type)"
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
                    </opensilex-TableAsyncView>

        <opensilex-EventModalView
            modalSize="lg"
            ref="eventModalView"
            :dto.sync="selectedEvent"
            :type.sync="selectedEvent.rdf_type"
        ></opensilex-EventModalView>

        <opensilex-EventModalForm
            v-if="renderModalForm"
            ref="modalForm"
            :target="target"
            @onCreate="displayAfterCreation"
            @onUpdate="refresh"
            :context="context"
        ></opensilex-EventModalForm>

        <opensilex-EventCsvForm
            v-if="renderCsvForm"
            ref="csvForm"
            @csvImported="onImport"
            :targets="[this.target]"
        ></opensilex-EventCsvForm>

        <opensilex-ModalForm
            v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
            ref="documentForm"
            component="opensilex-DocumentForm"
            createTitle="component.common.addDocument"
            modalSize="lg"
            :initForm="initForm"
            icon="ik#ik-file-text"
        ></opensilex-ModalForm>

    </div>
</template>

<script lang="ts">
import {Component, Prop, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import {EventsService} from "opensilex-core/api/events.service";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import EventModalView from "../view/EventModalView.vue";
import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";
import EventModalForm from "../form/EventModalForm.vue";
import EventCsvForm from "../form/csv/EventCsvForm.vue";

@Component
export default class EventList extends Vue {

    @Ref("tableRef") readonly tableRef!: any;
    @Ref("eventModalView") readonly eventModalView!: EventModalView;
    @Ref("modalForm") readonly modalForm!: EventModalForm;
    @Ref("csvForm") readonly csvForm!: EventCsvForm;
    @Ref("documentForm") readonly documentForm!: any;

    $opensilex: OpenSilexVuePlugin;
    $service: EventsService
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

    @Prop({default: EventList.getDefaultColumns})
    columnsToDisplay: Set<string>;

    static getDefaultColumns(): Set<string> {
      return new Set(["type", "start", "end", "targets","description"]);
    }

    @Prop({default: 10})
    maxPageSize: number;

    @Prop({default: true})
    displayTargetFilter: boolean;

    @Prop({default: false})
    displayTitle: boolean;

    @Prop({default: false})
    maximizeFilterSize;

    selectedEvent = {};

    @Prop({default: EventList.newFilter})
    filter;

    @Prop()
    target;

    @Prop()
    context;

    baseType: string;

    renderComponent = true;

    @Watch("target")
    onTargetChange() {
        this.renderComponent = false;

        this.$nextTick(() => {
            // Add the component back in
            this.renderComponent = true;
        });
    }

    renderModalForm = false;
    renderCsvForm = false;

    showForm(){
        this.renderModalForm = true;
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

    created() {
        this.$service = this.$opensilex.getService("opensilex.EventsService");
        this.baseType = this.$opensilex.Oeev.EVENT_TYPE_URI;
        this.$opensilex.updateFiltersFromURL(this.$route.query, EventList.newFilter());
    }

    get user() {
        return this.$store.state.user;
    }

    get credentials() {
        return this.$store.state.credentials;
    }

    refresh() {
        this.tableRef.refresh();
        this.$opensilex.updateURLParameters(this.filter);
    }

    displayAfterCreation(event) {
        this.refresh();
        this.showEventView(event);
    }

    static newFilter() {
        return {
            target: undefined,
            type: undefined,
            description: undefined,
            start: undefined,
            end: undefined,
        };
    }

    reset() {
        this.filter = EventList.newFilter();
        this.refresh();
    }

    cleanFilter() {
        if (this.target) {
            this.filter.target = this.target;
        }
        if (this.filter.target == "") {
            this.filter.target = undefined;
        }
        if (this.filter.start == "") {
            this.filter.start = undefined;
        }
        if (this.filter.end == "") {
            this.filter.end = undefined;
        }
        if (this.filter.description == "") {
            this.filter.description = undefined;
        }
    }

    search(options) {

        this.cleanFilter();

        return this.$service
            .searchEvents(
                this.filter.type,
                this.filter.start,
                this.filter.end,
                this.filter.target,
                this.filter.description,
                options.orderBy,
                options.currentPage,
                options.pageSize
            );
    }

    get fields() {

        let tableFields = [];

        // tableFields.push({key: "uri", label: "component.common.uri", sortable: true});

        if (this.columnsToDisplay.has("type")) {
            tableFields.push({key: "rdf_type_name", label: "component.common.type", sortable: true});
        }
        if (this.columnsToDisplay.has("start")) {
            tableFields.push({key: "start", label: "Event.start", sortable: true});
        }
        if (this.columnsToDisplay.has("end")) {
            tableFields.push({key: "end", label: "Event.end", sortable: true});
        }

        if (this.columnsToDisplay.has("targets")) {
            tableFields.push({key: "targets", label: "Event.targets", sortable: true});
        }
        if (this.columnsToDisplay.has("description")) {
            tableFields.push({key: "description", label: "Event.description", sortable: true});
        }
        if (this.enableActions) {
            tableFields.push({key: "actions", label: "component.common.actions", sortable: false});
        }

        return tableFields;
    }

    deleteEvent(uri: string) {
        this.$service.deleteEvent(uri).then(() => {
            this.refresh();

            let message = this.$i18n.t("Event.name") + " " + uri + " " + this.$i18n.t("component.common.success.delete-success-message");
            this.$opensilex.showSuccessToast(message);

            this.$emit('onDelete', uri);

        }).catch(this.$opensilex.errorHandler);
    }


    private getEventPromise(event): Promise<HttpResponse<OpenSilexResponse>> {
        if (this.isMove(event)) {
            return this.$service.getMoveEvent(event.uri);
        } else {
            return this.$service.getEventDetails(event.uri);
        }
    }

    isMove(event) {
        return event.rdf_type == this.$opensilex.Oeev.MOVE_TYPE_URI
            || event.rdf_type == this.$opensilex.Oeev.MOVE_TYPE_PREFIXED_URI;
    }

    showEventView(event) {
        this.getEventPromise(event).then((http: HttpResponse<OpenSilexResponse>) => {
            this.selectedEvent = http.response.result;
            this.eventModalView.show();
        }).catch(this.$opensilex.errorHandler);
    }

    editEvent(uri, type) {
        this.renderModalForm = true;
        this.$nextTick(() => {
             this.modalForm.showEditForm(uri, type);
        });
      
    }

    onImport(response) {
        this.refresh();
    }

    createDocument() {
    this.documentForm.showCreateForm();
    }

    initForm() {
        let targetURI = [];
        for (let select of this.tableRef.getSelected()) {
            targetURI.push(select.uri);
        }

        return {
            description: {
                uri: undefined,
                identifier: undefined,
                rdf_type: undefined,
                title: undefined,
                date: undefined,
                description: undefined,
                targets: targetURI,
                authors: undefined,
                language: undefined,
                deprecated: undefined,
                keywords: undefined
            },
            file: undefined
        }
    }
}
</script>


<style scoped lang="scss">
</style>

<i18n>
en:
    EventList:
        selected: Selected events
        target-filter-placeholder: Enter part or all of an URI
fr:
    EventList:
        selected: Évènements selectionnés
        target-filter-placeholder: Saisir une partie ou la totalité d'un URI

</i18n>

