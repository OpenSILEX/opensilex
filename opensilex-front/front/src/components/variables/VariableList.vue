<template>
    <div>
        <opensilex-PageContent>
            <template>

                <opensilex-SearchFilterField @clear="reset()" @search="refresh()" :showAdvancedSearch="true">
                    <template v-slot:filters>

                        <opensilex-FilterField>
                            <label>{{ $t("ExperimentList.filter-label") }}</label>
                            <opensilex-StringFilter
                                :filter.sync="filter.name"
                                placeholder="VariableList.name-placeholder"
                            ></opensilex-StringFilter>
                        </opensilex-FilterField>

                        <opensilex-FilterField>
                            <opensilex-EntitySelector
                                label="VariableView.entity"
                                :entity.sync="filter.entity"
                            ></opensilex-EntitySelector>
                        </opensilex-FilterField>

                        <opensilex-FilterField>
                            <opensilex-CharacteristicSelector
                                label="VariableView.characteristic"
                                :characteristic.sync="filter.characteristic"
                            ></opensilex-CharacteristicSelector>
                        </opensilex-FilterField>

                        <opensilex-FilterField>
                            <opensilex-GroupVariablesSelector
                                label="VariableView.groupVariable"
                                :variableGroup.sync="filter.group"
                            ></opensilex-GroupVariablesSelector>
                        </opensilex-FilterField>

                    </template>
                    <template v-slot:advancedSearch>

                        <opensilex-FilterField>
                            <opensilex-InterestEntitySelector
                                label="VariableForm.interestEntity-label"
                                :interestEntity.sync="filter.entityOfInterest"
                            ></opensilex-InterestEntitySelector>
                        </opensilex-FilterField>

                        <opensilex-FilterField>
                            <opensilex-MethodSelector
                                label="VariableView.method"
                                :method.sync="filter.method"
                            ></opensilex-MethodSelector>
                        </opensilex-FilterField>

                        <opensilex-FilterField>
                            <opensilex-UnitSelector
                                label="VariableView.unit"
                                :unit.sync="filter.unit"
                            ></opensilex-UnitSelector>
                        </opensilex-FilterField>

                    </template>
                </opensilex-SearchFilterField>

                <opensilex-TableAsyncView
                    ref="tableRef"
                    :searchMethod="searchVariablesWithAttribute"
                    :fields="fields"
                    defaultSortBy="name"
                    :defaultPageSize="pageSize"
                    :isSelectable="isSelectable"
                    :maximumSelectedRows="maximumSelectedRows"
                    labelNumberOfSelectedRow="VariableList.selected"
                    :iconNumberOfSelectedRow="iconNumberOfSelectedRow">

                    <template v-slot:selectableTableButtons="{ numberOfSelectedRows }">
                      <b-dropdown
                        dropright
                        class="mb-2 mr-2"
                        :small="true"
                        :disabled="numberOfSelectedRows == 0"
                        text="actions">

                        <b-dropdown-item-button
                            v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                            @click="addVariablesToGroups()">{{$t("VariableList.add-groupVariable")}}</b-dropdown-item-button>
                        <b-dropdown-item-button
                            v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                            @click="showCreateForm()">{{$t("VariableList.add-newGroupVariable")}}</b-dropdown-item-button>
                        <b-dropdown-item-button @click="classicExportVariables()">{{$t('VariableList.export-variables')}}</b-dropdown-item-button>
                        <b-dropdown-item-button @click="detailsExportVariables()">{{$t('VariableList.export-variables-details')}}</b-dropdown-item-button>

                        </b-dropdown>
                    </template>

                    <template v-slot:cell(name)="{data}">
                        <opensilex-UriLink
                            v-if="!noActions"
                            :uri="data.item.uri"
                            :value="data.item.name"
                            :to="{path: '/variable/details/'+ encodeURIComponent(data.item.uri)}"
                        ></opensilex-UriLink>
                    </template>
                    <template v-slot:row-details="{data}">
                        <div v-if="variableGroupsList[data.item.uri] && variableGroupsList[data.item.uri].length > 0">
                            <div>{{ $t("VariableList.variablesGroup") }}:</div>
                            <ul>
                                <li v-for="(vg, index) in variableGroupsList[data.item.uri]" :key="index">{{vg.name}}
                                </li>
                            </ul>
                        </div>
                        <div v-else> {{ $t("VariableList.not-used-in-variablesGroup") }}</div>
                    </template>
                    <template v-slot:cell(_entity_name)="{data}">{{ data.item.entity.name }}</template>
                    <template v-slot:cell(_interest_entity_name)="{data}">
                        {{ data.item.entity_of_interest ? data.item.entity_of_interest.name : "" }}
                    </template>
                    <template v-slot:cell(_characteristic_name)="{data}">{{ data.item.characteristic.name }}</template>
                    <template v-slot:cell(_method_name)="{data}">{{ data.item.method.name }}</template>
                    <template v-slot:cell(_unit_name)="{data}">{{data.item.unit.name }}</template>

                    <template v-slot:cell(actions)="{data}">
                        <b-button-group size="sm">
                            <opensilex-DetailButton
                                @click="loadVariablesGroupFromVariable(data)"
                                label="component.common.details-label"
                                :detailVisible="data.detailsShowing"
                                :small="true"
                            ></opensilex-DetailButton>
                            <opensilex-EditButton
                                v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                                @click="$emit('onEdit', data.item.uri)"
                                label="component.common.list.buttons.update"
                                :small="true"
                            ></opensilex-EditButton>
                            <opensilex-InteroperabilityButton
                                v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                                :small="true"
                                label="component.common.list.buttons.interoperability"
                                @click="$emit('onInteroperability', data.item.uri)"
                            ></opensilex-InteroperabilityButton>
                            <opensilex-DeleteButton
                                v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_DELETE_ID)"
                                @click="$emit('onDelete', data.item.uri)"
                                label="component.common.list.buttons.delete"
                                :small="true"
                            ></opensilex-DeleteButton>
                        </b-button-group>
                    </template>
                </opensilex-TableAsyncView>

                <opensilex-ModalForm
                    v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                    ref="groupVariablesForm"
                    modalSize="lg"
                    @onCreate="refresh($event)"
                    @onUpdate="refresh($event)"
                    component="opensilex-GroupVariablesForm"
                    createTitle="GroupVariablesForm.add"
                    editTitle="GroupVariablesForm.edit"
                    :initForm="initForm"
                ></opensilex-ModalForm>

                <opensilex-GroupVariablesModalList
                    label="label"
                    ref="groupVariableSelection"
                    :isModalSearch="true"
                    :required="true"
                    :multiple="true"
                    @onValidate="editGroupVariable"
                ></opensilex-GroupVariablesModalList>

            </template>
        </opensilex-PageContent>

    </div>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import {
  CharacteristicGetDTO,
  EntityGetDTO,
  InterestEntityGetDTO,
  MethodGetDTO,
  UnitGetDTO,
  VariablesGroupGetDTO,
  VariablesService
} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";

@Component
export default class VariableList extends Vue {
    $opensilex: any;
    $service: VariablesService;
    $store: any;
    $route: any;
    $i18n: any;

    get user() {
        return this.$store.state.user;
    }

    get credentials() {
        return this.$store.state.credentials;
    }

    @Prop({
        default: true
    })
    isSelectable;

    @Prop({
        default: false
    })
    noActions;

    @Prop()
    maximumSelectedRows;

    @Prop()
    iconNumberOfSelectedRow;

    @Prop({
    default: 20
    })
    pageSize: number;

    @Ref("groupVariableSelection") readonly groupVariableSelection!: any;
    @Ref("tableRef") readonly tableRef!: any;

    get lang() {
        return this.$store.state.lang;
    }

    initForm() {
        let variableURIs = [];
        for (let select of this.tableRef.getSelected()) {
            variableURIs.push(select.uri);
        }

        return {
            uri: undefined,
            name: undefined,
            description: undefined,
            variables: variableURIs
        };
    }

    @Ref("groupVariablesForm") readonly groupVariablesForm!: any;

    showCreateForm() {
        this.groupVariablesForm.showCreateForm();
    }

    filter = {
        name: undefined,
        entity: undefined,
        entityOfInterest: undefined,
        characteristic: undefined,
        method: undefined,
        unit: undefined,
        group: undefined
    };

    reset() {
        this.filter = {
            name: undefined,
            entity: undefined,
            entityOfInterest: undefined,
            characteristic: undefined,
            method: undefined,
            unit: undefined,
            group: undefined
        };
        this.refresh();
    }

    refresh() {
        this.tableRef.selectAll = false;
        this.tableRef.onSelectAll();
        this.$opensilex.updateURLParameters(this.filter);

        this.tableRef.refresh();
    }

    getSelected() {
        return this.tableRef.getSelected();
    }

    onItemUnselected(row) {
        this.tableRef.onItemUnselected(row);
    }

    searchVariablesWithAttribute(options) {
        return this.$service.searchVariables(
            this.filter.name,
            this.filter.entity,
            this.filter.entityOfInterest,
            this.filter.characteristic,
            this.filter.method,
            this.filter.unit,
            this.filter.group,
            options.orderBy,
            options.currentPage,
            options.pageSize
        );
    }

    classicExportVariables() {
        let path = "/core/variables/export_classic_by_uris";
        let today = new Date();
        let filename = "export_classic_variables_" + today.getFullYear() + String(today.getMonth() + 1).padStart(2, "0") + String(today.getDate()).padStart(2, "0");
        let variablesURIs = [];

        for (let select of this.tableRef.getSelected()) {
            variablesURIs.push(select.uri);
        }

        this.$opensilex.downloadFilefromPostService(path, filename, "csv", {uris: variablesURIs}, this.lang);
    }

    detailsExportVariables() {
        let path = "/core/variables/export_details_by_uris";
        let today = new Date();
        let filename = "export_detailed_variables_" + today.getFullYear() + String(today.getMonth() + 1).padStart(2, "0") + String(today.getDate()).padStart(2, "0");
        let variablesURIs = [];

        for (let select of this.tableRef.getSelected()) {
            variablesURIs.push(select.uri);
        }

        this.$opensilex.downloadFilefromPostService(path, filename, "csv", {uris: variablesURIs}, this.lang);
    }

    addVariablesToGroups() {
        this.groupVariableSelection.show();
    }

    private variableGroupsList = {};

    loadVariablesGroupFromVariable(data) {
        if (!data.detailsShowing) {
            this.$opensilex.disableLoader();
            this.$service.searchVariablesGroups(undefined, data.item.uri, ["name=asc"], undefined, undefined)
                .then((http: any) => {
                    let listVariableGroups = [];
                    for (let variableGroup of http.response.result) {
                        listVariableGroups.push({
                            uri: variableGroup.uri,
                            name: variableGroup.name,
                        });
                    }
                    this.variableGroupsList[data.item.uri] = listVariableGroups;
                    data.toggleDetails();
                    this.$opensilex.enableLoader();
                })
                .catch(this.$opensilex.errorHandler);
        } else {
            data.toggleDetails();
        }
    }

    editGroupVariable(variableGroup) {
        let selected = this.getSelected();
        var form;
        for (let vg = 0; vg < variableGroup.length; vg++) {
            this.$service.getVariablesGroup(variableGroup[vg].uri)
                .then((http: HttpResponse<OpenSilexResponse<VariablesGroupGetDTO>>) => {
                    let variablesGroup = http.response.result;
                    form = JSON.parse(JSON.stringify(variablesGroup));
                    let listUri = [];
                    for (let v = 0; v < variableGroup[vg].variables.length; v++) {
                        listUri.push(variableGroup[vg].variables[v].uri);
                    }
                    for (let i = 0; i < selected.length; i++) {
                        if (!listUri.includes(selected[i].uri)) {
                            listUri.push(selected[i].uri);
                        }
                    }
                    form.variables = listUri;
                    this.updateVariableGroup(form);
                }).catch(this.$opensilex.errorHandler);
        }
    }

    updateVariableGroup(form) {
        this.$service
            .updateVariablesGroup(form)
            .then((http: HttpResponse<OpenSilexResponse<any>>) => {
                let message = this.$i18n.t(form.name) + this.$i18n.t("component.common.success.update-success-message");
                this.$opensilex.showSuccessToast(message);
                let uri = http.response.result;
                console.debug("variable group updated", uri);
            })
            .catch(this.$opensilex.errorHandler);
    }

    created() {
        // let query: any = this.$route.query;
        // if (query.name) {
        //   this.nameFilter = decodeURIComponent(query.name);
        // }
        this.$opensilex.updateFiltersFromURL(this.$route.query, this.filter);
        this.$opensilex.disableLoader();
        this.$service = this.$opensilex.getService("opensilex.VariablesService");
    }

    get fields() {
        let tableFields = [
            {
                key: "name",
                label: "component.common.name",
                sortable: true
            },
            {
                key: "_entity_name",
                label: "VariableView.entity",
                sortable: true
            },
            {
                key: "_interest_entity_name",
                label: "VariableView.entityOfInterest",
                sortable: false
            },
            {
                key: "_characteristic_name",
                label: "VariableView.characteristic",
                sortable: true
            },
            {
                key: "_method_name",
                label: "VariableView.method",
                sortable: true
            },
            {
                key: "_unit_name",
                label: "VariableView.unit",
                sortable: true
            }
        ];
        if (!this.noActions) {
            tableFields.push({
                key: "actions",
                label: "component.common.actions",
                sortable: false
            });
        }

        return tableFields;
    }
}
</script>


<style scoped lang="scss">
</style>


<i18n>

en:
    VariableList:
        name-placeholder: Enter variable name
        label-filter: Search variables
        label-filter-placeholder: "Search variables, plant height, plant, humidity, image processing, percentage, air.*humidity, etc.
            This filter apply on variable name."
        selected: Selected Variables
        add-groupVariable: Add to an existing group of variables
        add-newGroupVariable: Add to a new group of variables
        export-variables: Export variable list
        export-variables-details: Export detailed variable list
        variablesGroup: Variable used in one or many groups of variables
        not-used-in-variablesGroup: Variable not used in any group of variables
fr:
    VariableList:
        name-placeholder: Entrer un nom de variable
        label-filter: Chercher une variable
        label-filter-placeholder: "Rechercher des variables : Hauteur de plante, plante, humidité, analyse d'image, pourcentage, air.*humidité, etc.
            Ce filtre s'applique au nom d'une variable."
        selected: Variables Sélectionnées
        add-groupVariable: Ajouter à un groupe de variables existant
        add-newGroupVariable: Ajouter à un nouveau groupe de variables
        export-variables: Exporter la liste de variables
        export-variables-details: Exporter la liste détaillée de variables
        variablesGroup: Variable utilisé dans un ou plusieurs groupe de variables
        not-used-in-variablesGroup: Variable n'est utilisé dans aucun groupe de variables


</i18n>
