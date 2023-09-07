<template>
    <div>
        <opensilex-PageContent
        class="pagecontent">
            <template>
            <!-- Toggle Sidebar-->
                <div class="searchMenuContainer"

                v-on:click="toggleFilter()"
                :title="searchFiltersPannel()">
                    <div class="searchMenuIcon">
                    <i class="ik ik-search"></i>
                    </div>
                </div>
                <!-- FILTERS -->
                <Transition>

                  <!-- Conditional display of filter and conditional rendering of advanced filters -->
                  <div v-show="toggleSearchFilters">
                    <opensilex-SearchFilterField
                        @clear="reset()"
                        @search="refresh()"
                        @toggleAdvancedSearch="loadAdvancedFilter"
                        :showAdvancedSearch="true"
                        class="searchFilterField">
                      <template v-slot:filters>
                        <div>
                          <opensilex-FilterField>
                            <opensilex-SharedResourceInstanceSelector
                                label="component.sharedResourceInstances.label"
                                :sharedResourceInstance.sync="filter.sharedResourceInstance"
                                placeholder="component.sharedResourceInstances.selector-placeholder"
                                @select="refresh"
                                :multiple="false"
                                class="searchFilter"
                            ></opensilex-SharedResourceInstanceSelector>
                          </opensilex-FilterField>
                        </div>
                        <!-- Name -->
                        <div>
                            <opensilex-FilterField>
                                <label>{{ $t("ExperimentList.filter-label") }}</label>
                                <opensilex-StringFilter
                                    :filter.sync="filter.name"
                                    placeholder="VariableList.name-placeholder"
                                    class="searchFilter"
                                    @handlingEnterKey="refresh()"
                                ></opensilex-StringFilter>
                            </opensilex-FilterField>
                        </div>

                        <!-- Entity -->
                        <div>
                        <opensilex-FilterField>
                            <opensilex-EntitySelector v-if="loadSearchFilters"
                                label="VariableView.entity"
                                :entity.sync="filter.entity"
                                :sharedResourceInstance="filter.sharedResourceInstance"
                                @handlingEnterKey="refresh()"
                                class="searchFilter"
                            ></opensilex-EntitySelector>
                        </opensilex-FilterField>
                        </div>

                        <!-- Characteristic -->
                        <div>
                        <opensilex-FilterField>
                            <opensilex-CharacteristicSelector v-if="loadSearchFilters"
                                label="VariableView.characteristic"
                                :characteristic.sync="filter.characteristic"
                                :sharedResourceInstance="filter.sharedResourceInstance"
                                @handlingEnterKey="refresh()"
                                class="searchFilter"
                            ></opensilex-CharacteristicSelector>
                        </opensilex-FilterField>
                        </div>

                        <!-- Group of variables -->
                        <div>
                        <opensilex-FilterField>
                            <opensilex-GroupVariablesSelector v-if="loadSearchFilters && withoutGroup !== true"
                                label="VariableView.groupVariable"
                                :variableGroup.sync="filter.includedGroup"
                                :sharedResourceInstance="filter.sharedResourceInstance"
                                class="searchFilter"
                                @handlingEnterKey="refresh()"
                            ></opensilex-GroupVariablesSelector>

                            <opensilex-GroupVariablesSelector v-if="loadSearchFilters && withoutGroup === true"
                                label="VariableView.groupVariable"
                                :variableGroup.sync="filter.notIncludedGroup"
                                :sharedResourceInstance="filter.sharedResourceInstance"
                                class="searchFilter"
                                @handlingEnterKey="refresh()"
                            ></opensilex-GroupVariablesSelector>

                            <opensilex-CheckboxForm
                                title="VariableList.withoutGroup"
                                helpMessage="VariableList.withoutGroup-info"
                                :value.sync="withoutGroup"
                                class="searchFilter"
                            ></opensilex-CheckboxForm>
                        </opensilex-FilterField>
                        </div>
                    </template>
                    <template v-slot:advancedSearch>
                      <!-- Entity of interest -->
                      <div>
                        <opensilex-FilterField>
                            <opensilex-InterestEntitySelector v-if="loadAdvancedSearchFilters"
                                label="VariableForm.interestEntity-label"
                                :interestEntity.sync="filter.entityOfInterest"
                                :sharedResourceInstance="filter.sharedResourceInstance"
                                @handlingEnterKey="refresh()"
                                class="searchFilter"
                            ></opensilex-InterestEntitySelector>
                        </opensilex-FilterField>
                      </div>

                      <!-- Method -->
                      <div>
                        <opensilex-FilterField>
                            <opensilex-MethodSelector v-if="loadAdvancedSearchFilters"
                                label="VariableView.method"
                                :method.sync="filter.method"
                                :sharedResourceInstance="filter.sharedResourceInstance"
                                class="searchFilter"
                                @handlingEnterKey="refresh()"
                            ></opensilex-MethodSelector>
                        </opensilex-FilterField>
                      </div>

                      <!-- Unit/Scale -->
                      <div>
                        <opensilex-FilterField>
                            <opensilex-UnitSelector v-if="loadAdvancedSearchFilters"
                                label="VariableView.unit"
                                :unit.sync="filter.unit"
                                :sharedResourceInstance="filter.sharedResourceInstance"
                                @handlingEnterKey="refresh()"
                                class="searchFilter"
                            ></opensilex-UnitSelector>
                        </opensilex-FilterField>
                      </div>

                      <!-- Data type-->
                      <div>
                        <opensilex-FilterField>
                            <opensilex-VariableDataTypeSelector v-if="loadAdvancedSearchFilters"
                                label="OntologyPropertyForm.data-type"
                                :datatype.sync="filter.dataType"
                                @handlingEnterKey="refresh()"
                                class="searchFilter"
                            ></opensilex-VariableDataTypeSelector>
                        </opensilex-FilterField>
                      </div>

                      <!-- Time interval -->
                      <div>
                        <opensilex-FilterField>
                            <opensilex-VariableTimeIntervalSelector v-if="loadAdvancedSearchFilters"
                                label="VariableForm.time-interval"
                                :timeinterval.sync="filter.timeInterval"
                                @handlingEnterKey="refresh()"
                                class="searchFilter"
                            ></opensilex-VariableTimeIntervalSelector>
                        </opensilex-FilterField>
                      </div>

                        <!-- Species -->
                        <div>
                          <opensilex-FilterField>
                            <opensilex-SpeciesSelector
                                label="SpeciesSelector.select-multiple"
                                placeholder="SpeciesSelector.select-multiple-placeholder"
                                :multiple="true"
                                :species.sync="filter.species"
                                :sharedResourceInstance="filter.sharedResourceInstance"
                                class="searchFilter"
                            ></opensilex-SpeciesSelector>
                          </opensilex-FilterField>
                        </div>
                        </template>
                        </opensilex-SearchFilterField>
                    </div>
                </Transition>

                <opensilex-TableAsyncView
                    ref="tableRef"
                    :searchMethod="searchVariablesWithAttribute"
                    :fields="fields"
                    defaultSortBy="name"
                    :defaultPageSize="pageSize"
                    :isSelectable="isSelectable"
                    :maximumSelectedRows="maximumSelectedRows"
                    labelNumberOfSelectedRow="VariableList.selected"
                    iconNumberOfSelectedRow="fa#vials"
                    @refreshed="onRefreshed"
                    @select="$emit('select', $event)"
                    @unselect="$emit('unselect', $event)"
                    @selectall="$emit('selectall', $event)"
                    class="modalVariablesList">

                    <template v-slot:selectableTableButtons="{ numberOfSelectedRows }">
                        <b-dropdown
                            dropright
                            class="mb-2 mr-2"
                            :small="true"
                            :text="$t('VariableList.display')">

                            <b-dropdown-item-button @click="clickOnlySelected()">{{ onlySelected ? $t('VariableList.selected-all') : $t("component.common.selected-only")}}</b-dropdown-item-button>
                            <b-dropdown-item-button @click="resetSelected()">{{$t("component.common.resetSelected")}}</b-dropdown-item-button>
                        </b-dropdown>

                        <b-dropdown
                            v-if="!noActions"
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
                        <b-dropdown-item-button
                            v-if="filter.sharedResourceInstance && user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                            @click="importVariablesOnLocal()"
                        >{{$t('VariableList.import-variables-from-shared-resources')}}</b-dropdown-item-button>

                        </b-dropdown>

                    </template>

                    <template v-slot:cell(name)="{data}">
                        <span class="lowSize" v-bind:class="{ variablesCheckboxMarginHighSize: toggleSearchFilters }">
                             <opensilex-UriLink
                                 :uri="data.item.uri"
                                 :value="data.item.name"
                                 :to="{path: getDetailsPageUrl(data.item)}"
                             ></opensilex-UriLink>
                        </span >
                        <br>
                        <span class="lowSize" v-bind:class="{ variablesCheckboxMarginHighSize: toggleSearchFilters }">{{data.item.alternative_name}}</span>
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
                    <template v-slot:cell(db_logo)="{data}">
                      <div v-if="data.item.onLocal">
                        <opensilex-Icon
                            icon="fa#database"
                            :title="$t('component.sharedResourceInstances.on-local')"
                        />
                      </div>
                    </template>
                    <template v-slot:cell(share_logo)="{data}">
                      <div v-if="data.item.sharedResourceInstance">
                          <opensilex-Icon
                              icon="fa#share-alt"
                              :title="data.item.sharedResourceInstance.label"
                          />
                      </div>
                    </template>
                    <template v-slot:cell(_entity_name)="{data}">{{ data.item.entity.name }}</template>
                    <template v-slot:cell(_interest_entity_name)="{data}">
                        {{ data.item.entity_of_interest ? data.item.entity_of_interest.name : "" }}
                    </template>
                    <template v-slot:cell(_characteristic_name)="{data}">{{ data.item.characteristic.name }}</template>
                    <template v-slot:cell(_method_name)="{data}">{{ data.item.method.name }}</template>
                    <template v-slot:cell(_unit_name)="{data}">{{data.item.unit.name }}</template>

                    <template v-if="!noActions" v-slot:cell(actions)="{data}">
                        <b-button-group size="sm">
                            <opensilex-DetailButton
                                @click="loadVariablesGroupFromVariable(data)"
                                label="component.common.details-label"
                                :detailVisible="data.detailsShowing"
                                :small="true"
                            ></opensilex-DetailButton>
                            <opensilex-EditButton
                                v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID) && displayActions"
                                @click="$emit('onEdit', data.item.uri)"
                                label="component.common.list.buttons.update"
                                :small="true"
                            ></opensilex-EditButton>
                            <opensilex-InteroperabilityButton
                                v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID) && displayActions"
                                :small="true"
                                label="component.common.list.buttons.interoperability"
                                @click="$emit('onInteroperability', data.item.uri)"
                            ></opensilex-InteroperabilityButton>
                            <opensilex-DeleteButton
                                v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_DELETE_ID) && displayActions"
                                @click="$emit('onDelete', data.item.uri)"
                                label="component.common.list.buttons.delete"
                                :small="true"
                            ></opensilex-DeleteButton>
                        </b-button-group>
                    </template>
                </opensilex-TableAsyncView>

                <opensilex-ModalForm
                    v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID) && !noActions && loadGroupVariablesForm"
                    ref="groupVariablesForm"
                    modalSize="lg"
                    @onCreate="refresh()"
                    @onUpdate="refresh()"
                    component="opensilex-GroupVariablesForm"
                    createTitle="GroupVariablesForm.add"
                    editTitle="GroupVariablesForm.edit"
                    :initForm="initForm"
                ></opensilex-ModalForm>

                <opensilex-GroupVariablesModalList
                    v-if="!noActions"
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
import {Component, Prop, Ref, Watch} from "vue-property-decorator";
import Vue, {VNode} from "vue";
import {VariablesGroupGetDTO, VariablesService} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import ModalForm from "../common/forms/ModalForm.vue";
import GroupVariablesModalList from '../groupVariable/GroupVariablesModalList.vue';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import GroupVariablesForm from "../groupVariable/GroupVariablesForm.vue";
import {VariablesGroupCreationDTO} from "opensilex-core/model/variablesGroupCreationDTO";
import {VariablesGroupUpdateDTO} from "opensilex-core/model/variablesGroupUpdateDTO";
import TableAsyncView from '../common/views/TableAsyncView.vue';
import {CopyResourceDTO} from "opensilex-core/model/copyResourceDTO";
import {VariableGetDTO} from "opensilex-core/model/variableGetDTO";
import {OpenSilexStore} from "../../models/Store";

@Component
export default class VariableList extends Vue {
    $opensilex: OpenSilexVuePlugin;
    $service: VariablesService;
    $store: OpenSilexStore;
    $route: any;
    $i18n: any;
    SearchFiltersToggle: boolean = false;

    displayActions:boolean = true;

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

    @Prop({
        default: false
    })
    withAssociatedData: boolean; // affiche les variables associées aux datas uniquement

    @Prop()
    experiment;

    @Prop()
    objects;

    @Prop()
    devices;

    filter = {
        sharedResourceInstance: undefined,
        name: undefined,
        entity: undefined,
        entityOfInterest: undefined,
        characteristic: undefined,
        method: undefined,
        unit: undefined,
        includedGroup: undefined,
        notIncludedGroup: undefined,
        dataType: undefined,
        timeInterval: undefined,
        experiment: undefined,
        objects: undefined,
        devices: undefined,
        species: []
    };

    withoutGroup: boolean = false; //checkbox for the group filter

    @Watch("withoutGroup")
    onWithoutGroupChange() {
        if(this.withoutGroup === true) {
            this.filter.notIncludedGroup = this.filter.includedGroup;
            this.filter.includedGroup = undefined;
        } else{
            this.filter.includedGroup = this.filter.notIncludedGroup;
            this.filter.notIncludedGroup = undefined;
        }
    }

    @Ref("groupVariableSelection") readonly groupVariableSelection!: GroupVariablesModalList;
    @Ref("tableRef") readonly tableRef!: TableAsyncView<VariableGetDTO>;

    get onlySelected() {
        return this.tableRef.onlySelected;
    }

    get lang() {
        return this.$store.state.lang;
    }

    created() {
        this.$opensilex.updateFiltersFromURL(this.$route.query, this.filter);
        this.$opensilex.disableLoader();
        this.$service = this.$opensilex.getService("opensilex.VariablesService");
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

    loadGroupVariablesForm: boolean = false;
    @Ref("groupVariablesForm") readonly groupVariablesForm!: ModalForm<GroupVariablesForm, VariablesGroupCreationDTO, VariablesGroupUpdateDTO>;

    showCreateForm() {
        // lazy loading of form
        this.loadGroupVariablesForm = true;
        this.$nextTick(() => {
            this.groupVariablesForm.showCreateForm();
        });
    }

    clickOnlySelected() {
        this.tableRef.clickOnlySelected();
    }

    resetSelected() {
        this.tableRef.resetSelected();
    }

    reset() {
        this.filter = {
            sharedResourceInstance: this.filter.sharedResourceInstance,
            name: undefined,
            entity: undefined,
            entityOfInterest: undefined,
            characteristic: undefined,
            method: undefined,
            unit: undefined,
            includedGroup: undefined,
            notIncludedGroup: undefined,
            dataType: undefined,
            timeInterval: undefined,
            experiment: undefined,
            objects: undefined,
            devices: undefined,
            species: []
        };
        this.withoutGroup = false;
        this.refresh();
        this.$emit("onReset");
    }

    refresh() {
        this.$opensilex.updateURLParameters(this.filter);
        if(this.tableRef.onlySelected) {
            this.tableRef.onlySelected = false;
            this.tableRef.changeCurrentPage(1);
        } else {
            this.tableRef.changeCurrentPage(1);
        }
    }

    // fix the state of the button selectAll
    onRefreshed() {
        let that = this;
        setTimeout(function() {
            if(that.tableRef.selectAll === true && that.tableRef.selectedItems.length !== that.tableRef.totalRow) {
                that.tableRef.selectAll = false;
            }
        }, 1);
    }

    getDetailsPageUrl(variable: VariableGetDTO) {
      if (this.filter.sharedResourceInstance) {
        return '/variable/details/'+ encodeURIComponent(variable.uri) + '?sharedResourceInstance=' + this.filter.sharedResourceInstance;
      }
      return '/variable/details/' + encodeURIComponent(variable.uri);
    }

    getSelected() {
        return this.tableRef.getSelected();
    }

    onItemUnselected(row) {
        this.tableRef.onItemUnselected(row);
    }
    onItemSelected(row) {
      this.tableRef.onItemSelected(row);
    }

    searchVariablesWithAttribute(options) {
        return this.$service.searchVariables(
            this.filter.name,
            this.filter.entity,
            this.filter.entityOfInterest,
            this.filter.characteristic,
            this.filter.method,
            this.filter.unit,
            this.filter.includedGroup,
            this.filter.notIncludedGroup,
            this.filter.dataType,
            this.filter.timeInterval,
            this.filter.species,
            this.withAssociatedData,
            this.experiment ? this.experiment : this.filter.experiment,
            this.objects ? this.objects : this.filter.objects,
            this.devices ? this.devices : this.filter.devices,
            options.orderBy,
            options.currentPage,
            options.pageSize,
            this.filter.sharedResourceInstance
        ).then(response => {
          this.displayActions = this.filter.sharedResourceInstance === undefined;
          return response;
        });
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

    async importVariablesOnLocal() {
      let confirmMessage: Array<VNode> = [
        this.$createElement("p", this.$t("component.sharedResourceInstances.variable-import-confirmation").toString())
      ];
      if (this.tableRef.getSelected().some(variable => variable.onLocal)) {
        confirmMessage.push(this.$createElement("p", this.$t("component.sharedResourceInstances.variable-warning-already-imported").toString()));
      }
      const confirm = await this.$bvModal.msgBoxConfirm(
          confirmMessage,
          {
            cancelTitle: this.$t("component.common.cancel").toString(),
            okTitle: this.$t("component.sharedResourceInstances.import").toString(),
            okVariant: "primary",
            centered: true
          }
      );

      if (!confirm) {
        return;
      }

      let variablesURIs = this.tableRef.getSelected()
          .map(variable => variable.uri);
      let form: CopyResourceDTO = {
        uris: variablesURIs,
        sharedResourceInstance: this.filter.sharedResourceInstance
      };
      const http = await this.$service.copyFromSharedResourceInstance(form);

      if (http.status === 200) {
        this.tableRef.refresh();
      }
      const result = http.response.result;
      let message = this.$i18n.t("component.common.success.import-success-message", {
        variablesCount: result.variableUris.length,
        entitiesCount: result.entityUris.length,
        characteristicsCount: result.characteristicUris.length,
        methodsCount: result.methodUris.length,
        unitsCount: result.unitUris.length,
        interestEntityCount: result.interestEntityUris.length
      });
      this.$opensilex.showSuccessToast(message);
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
            this.$service.searchVariablesGroups(undefined, data.item.uri, ["name=asc"],undefined, undefined, undefined)
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

    get fields() {
        let tableFields = [
            {
                key: "name",
                label: "component.common.name",
                sortable: true
            },
            {
                key: "db_logo",
                label: "",
                sortable: false
            },
            {
              key: "share_logo",
              label: "",
              sortable: false
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

    searchFiltersPannel() {
        return  this.$t("searchfilter.label")
    }

    toggleSearchFilters: boolean = false;
    loadSearchFilters: boolean = false;

    /**
     * Show or hide the search filter (v-show) on the filter div
     * Trigger render of search filters selector (v-if).
     * This ensures that API methods corresponding with the selector are not executed
     * at the render of this component but only at the first toggle of the filter
     *
     */
    toggleFilter(){
        this.toggleSearchFilters = ! this.toggleSearchFilters;
        if(! this.loadSearchFilters){
            this.loadSearchFilters = true;
        }
    }

    loadAdvancedSearchFilters: boolean = false;

    /**
     * Show or hide the advanced search filter (v-show) on the filter div
     * Trigger render of advanced search filters selector (v-if)
     * This ensures that API methods corresponding with the selector are not executed
     * at the render of this component but only at the first toggle of the advanced filter
     */
    loadAdvancedFilter(){
        if(! this.loadAdvancedSearchFilters){
            this.loadAdvancedSearchFilters = true;
        }
    }
}
</script>


<style scoped lang="scss">

.modalVariablesList {
    overflow: hidden;
}

.variablesCheckboxMarginHighSize {
    margin-left: 15px
}
@media (min-width: 200px) and (max-width: 1199px) {
    .lowSize {
        margin-left:15px
    }
}
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
        import-variables-from-shared-resources: Import from the shared source
        variablesGroup: Variable used in one or many groups of variables
        not-used-in-variablesGroup: Variable not used in any group of variables
        selected-all: All variables
        display: Display
        withoutGroup: Not in group
        withoutGroup-info: Select the checkbox to filter the variables that are not included in the selected group
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
        import-variables-from-shared-resources: Importer depuis la source partagée
        variablesGroup: Variable utilisé dans un ou plusieurs groupe de variables
        not-used-in-variablesGroup: Variable n'est utilisé dans aucun groupe de variables
        selected-all: Toutes les variables
        display: Affichage
        withoutGroup: Pas dans ce groupe
        withoutGroup-info: Cocher la case pour filtrer les variables qui n'appartiennent pas au groupe sélectionné

</i18n>