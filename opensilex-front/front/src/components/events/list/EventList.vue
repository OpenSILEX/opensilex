<template>
        <div class="row">
            <div class="col col-xl-12">

                <div class="card">
                    <div class="card-header" v-if="displayTitle">
                        <h3>
                            <i class="ik ik-clipboard"></i>
                            {{ $t('Event.list-title') }}
                        </h3>
                    </div>

                    <div class="card-body">

                        <opensilex-PageActions>

                            <opensilex-CreateButton
                                    v-if="user.hasCredential(modificationCredentialId)"
                                    label="Event.add"
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

                                <opensilex-SearchFilterField
                                        v-if="displayFilters"
                                        @search="refresh()"
                                        @clear="reset()"
                                        label="component.experiment.search.label"
                                        :showAdvancedSearch="true"
                                >
                                    <template v-slot:filters>
                                        <opensilex-FilterField>

                                            <opensilex-TypeForm
                                                    :type.sync="filter.type"
                                                    :baseType="baseType"
                                                    placeholder="Event.type-placeholder"
                                            ></opensilex-TypeForm>

                                        </opensilex-FilterField>

                                        <opensilex-FilterField>
                                            <opensilex-InputForm
                                                    :value.sync="filter.target"
                                                    label="Event.targets"
                                                    type="text"
                                                    placeholder="ExperimentList.filter-label-placeholder"
                                            ></opensilex-InputForm>
                                        </opensilex-FilterField>

                                        <!-- description -->
                                        <opensilex-FilterField>
                                            <opensilex-InputForm
                                                    :value.sync="filter.description"
                                                    label="component.common.description"
                                                    type="text"
                                                    placeholder="ExperimentList.filter-label-placeholder"
                                            ></opensilex-InputForm>
                                        </opensilex-FilterField>

                                    </template>

                                    <template v-slot:advancedSearch>
                                        <opensilex-FilterField>
                                            <opensilex-DateTimeForm
                                                    :value.sync="filter.start"
                                                    label="Event.start"
                                                    :required="false"
                                            ></opensilex-DateTimeForm>
                                        </opensilex-FilterField>

                                        <opensilex-FilterField>
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
                                        :isSelectable="isSelectable"
                                >
                                    <template v-slot:cell(uri)="{data}">
                                        <opensilex-UriLink
                                                :uri="data.item.uri"
                                                :value="data.item.uri"
                                                @click="showEventView(data.item)"
                                        ></opensilex-UriLink>
                                    </template>

                                    <template v-slot:cell(type_label)="{data}">
                                      <opensilex-TextView :value="data.item.rdf_type_name"></opensilex-TextView>
                                    </template>

                                    <template v-slot:cell(start)="{data}">
                                        <opensilex-TextView v-if="data.item.start && data.item.start.length > 0"
                                                :value="new Date(data.item.start).toLocaleString()"></opensilex-TextView>
                                    </template>
                                    <template v-slot:cell(end)="{data}">
                                        <opensilex-TextView v-if="data.item.end"
                                                              :value="new Date(data.item.end).toLocaleString()"></opensilex-TextView>
                                    </template>

                                    <template v-slot:cell(targets)="{data}">
                                        <span :key="index" v-for="(uri, index) in getItemsToDisplay(data.item.targets)">
                                            <span :title="uri">{{uri}}</span>
                                            <span v-if="index < 2"> , </span>
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
                                                    @click="editEvent(data.item)"
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
                            </template>
                        </opensilex-PageContent>

                        <opensilex-EventModalView
                                modalSize="lg"
                                ref="eventModalView"
                                :dto.sync="selectedEvent"
                                :type.sync="selectedEvent.rdf_type"
                        ></opensilex-EventModalView>

                        <opensilex-EventModalForm
                                ref="eventModalForm"
                                :target="target"
                                @onCreate="refresh"
                                @onUpdate="refresh"
                        ></opensilex-EventModalForm>

                        <opensilex-EventCsvForm
                            ref="eventCsvForm"
                            @csvImported="onImport"
                            :targets="[this.target]"
                        ></opensilex-EventCsvForm>

                    </div>
                </div>
            </div>
        </div>

</template>

<script lang="ts">
    import {Component, Prop, Ref} from "vue-property-decorator";
    import Vue from "vue";
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
        @Ref("eventModalForm") readonly eventModalForm!: EventModalForm;
        @Ref("eventCsvForm") readonly eventCsvForm!: EventCsvForm;


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

        @Prop({default: new Set(["uri","type","start","end","description"])})
        columnsToDisplay: Set<string>;

        @Prop({default: 10})
        maxPageSize: number;

        @Prop({default : false})
        displayFilters : boolean;

        @Prop({default : false})
        displayTitle: boolean;

        selectedEvent = {};

        @Prop({default: EventList.newFilter})
        filter;

        @Prop()
        target;

        baseType: string;

        showCsvForm() {
            this.eventCsvForm.show();
        }

        created() {
            this.$service = this.$opensilex.getService("opensilex.EventsService");
            this.baseType = this.$opensilex.Oeev.EVENT_TYPE_URI;
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

        cleanFilter(){
            if(this.target){
                this.filter.target = this.target;
            }
            if(this.filter.target == ""){
                this.filter.target = undefined;
            }
            if(this.filter.start == ""){
                this.filter.start = undefined;
            }
            if(this.filter.end == ""){
                this.filter.end = undefined;
            }
            if(this.filter.description == ""){
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

            tableFields.push({key: "uri", label: "component.common.uri", sortable: true});

            if(this.columnsToDisplay.has("type")){
                tableFields.push({key: "type_label", label: "component.common.type", sortable: true});
            }
            if(this.columnsToDisplay.has("start")){
                tableFields.push({key: "start", label: "Event.start", sortable: true});
            }
            if(this.columnsToDisplay.has("end")){
                tableFields.push({key: "end", label: "Event.end", sortable: true});
            }

            if(this.columnsToDisplay.has("targets")){
                tableFields.push({key: "targets", label: "Event.concerned-items", sortable: true});
            }
            if(this.columnsToDisplay.has("description")){
                tableFields.push({key: "description", label: "Event.description", sortable: true});
            }
            if (this.enableActions) {
                tableFields.push({key: "actions", label: "component.common.actions", sortable: false});
            }

            return tableFields;
        }

        deleteEvent(uri : string){
            this.$service.deleteEvent(uri).then( () => {
                this.refresh();

                let message = this.$i18n.t("Event.name") + " " + uri + " " + this.$i18n.t("component.common.success.delete-success-message");
                this.$opensilex.showSuccessToast(message);

                this.$emit('onDelete', uri);

            }).catch(this.$opensilex.errorHandler);
        }


        private getEventPromise(event) : Promise<HttpResponse<OpenSilexResponse>>{
            if (this.isMove(event)){
                return this.$service.getMoveEvent(event.uri);
            }else{
                return  this.$service.getEventDetails(event.uri);
            }
        }

        isMove(event){
            return event.rdf_type == this.$opensilex.Oeev.MOVE_TYPE_URI
                || event.rdf_type == this.$opensilex.Oeev.MOVE_TYPE_PREFIXED_URI;
        }

        showEventView(event){
            this.getEventPromise(event).then((http: HttpResponse<OpenSilexResponse>) => {
                this.selectedEvent = http.response.result;
                this.eventModalView.show();
            }).catch(this.$opensilex.errorHandler);
        }

        editEvent(event){
            this.getEventPromise(event).then((http: HttpResponse<OpenSilexResponse>) => {
                this.eventModalForm.showEditForm(http.response.result);
            }).catch(this.$opensilex.errorHandler);
        }

        getItemsToDisplay(targets){
            return targets.slice(0,3);
        }

        onImport(response) {
            this.refresh();
        }

    }
</script>


<style scoped lang="scss">
</style>

<i18n>

</i18n>