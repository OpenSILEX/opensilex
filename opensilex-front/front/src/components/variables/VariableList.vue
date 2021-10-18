<template>
    <div>
        <opensilex-StringFilter
            :filter.sync="nameFilter"
            @update="updateFilters()"
            placeholder="VariableList.label-filter-placeholder"
        ></opensilex-StringFilter>

        <opensilex-PageContent>
            <template>
              <opensilex-TableAsyncView
                ref="tableRef"
                  :searchMethod="searchVariables"
                  :fields="fields"
                  defaultSortBy="name"
                  :isSelectable="isSelectable"
                  :maximumSelectedRows="maximumSelectedRows"
                  labelNumberOfSelectedRow="VariableList.selected"
                  :iconNumberOfSelectedRow="iconNumberOfSelectedRow">

                    <template v-slot:selectableTableButtons="{ numberOfSelectedRows }">
                      <b-dropdown
                        v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                        dropright
                        class="mb-2 mr-2"
                        :small="true"
                        :disabled="numberOfSelectedRows == 0"
                        text="actions">

                        <b-dropdown-item-button @click="addVariablesToGroups()">{{$t("VariableList.add-groupVariable")}}</b-dropdown-item-button>
                        <b-dropdown-item-button @click="showCreateForm()">{{$t("VariableList.add-newGroupVariable")}}</b-dropdown-item-button>
                        <!-- <b-dropdown-item-button >{{$t("VariableList.export-variables")}}</b-dropdown-item-button> -->

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
                    <template v-slot:cell(_entity_name)="{data}">{{ data.item.entity.name }}</template>
                    <template v-slot:cell(_characteristic_name)="{data}">{{ data.item.characteristic.name }}</template>
                    <template v-slot:cell(_method_name)="{data}">{{ data.item.method ? data.item.method.name : ""  }}</template>
                    <template v-slot:cell(_unit_name)="{data}">{{data.item.unit.name }}</template>

                    <template v-slot:cell(actions)="{data}">
                        <b-button-group size="sm">
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
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";

import { VariablesService, VariablesGroupGetDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import VariablesView from "./VariablesView.vue"
import GroupVariablesForm from "../groupVariable/GroupVariablesForm.vue"

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
  private nameFilter: any = "";

  @Ref("groupVariableSelection") readonly groupVariableSelection!: any;
  @Ref("tableRef") readonly tableRef!: any;

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
  showCreateForm(){
    this.groupVariablesForm.showCreateForm();
  }

  updateFilters() {
    this.$opensilex.updateURLParameter("name", this.nameFilter, "");
    this.refresh();
  }

  resetSearch() {
    this.nameFilter = "";
    this.$opensilex.updateURLParameter("name", undefined, undefined);
    this.refresh();
  }

  refresh() {
    this.tableRef.refresh();
  }
  getSelected() {
    return this.tableRef.getSelected();
  }
  onItemUnselected(row) {
    this.tableRef.onItemUnselected(row);
  }

  searchVariables(options) {
    return this.$service.searchVariables(
      this.nameFilter,
      options.orderBy,
      options.currentPage,
      options.pageSize
    );
  }

  addVariablesToGroups() {
    this.groupVariableSelection.show();
  }

  editGroupVariable(variableGroup) {
    let selected = this.getSelected();
    var form;
    for(let vg = 0; vg < variableGroup.length; vg++){      
      this.$service.getVariablesGroup(variableGroup[vg].uri)
      .then((http: HttpResponse<OpenSilexResponse<VariablesGroupGetDTO>>) => {
        let variablesGroup = http.response.result;
        form = JSON.parse(JSON.stringify(variablesGroup));
        let listUri = [];
        for(let v = 0; v < variableGroup[vg].variables.length; v++){
          listUri.push(variableGroup[vg].variables[v].uri);
        }        
        for (let i = 0; i < selected.length; i++){
          if(!listUri.includes(selected[i].uri)){
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
    let query: any = this.$route.query;
    if (query.name) {
      this.nameFilter = decodeURIComponent(query.name);
    }
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
        label-filter: Search variables
        label-filter-placeholder: "Search variables, plant height, plant, humidity, image processing, percentage, air.*humidity, etc.
            This filter apply on URI, name, alternative name, or on entity/characteristic/method/unit name."
        selected: Selected Variables
        add-groupVariable: Add to an existing group of variables
        add-newGroupVariable: Add to a new group of variables
        export-variables: Export variable list
fr:
    VariableList:
        label-filter: Chercher une variable
        label-filter-placeholder: "Rechercher des variables : Hauteur de plante, plante, humidité, analyse d'image, pourcentage, air.*humidité, etc.
            Ce filtre s'applique à l'URI, au nom, au nom alternatif et au nom de l'entité/caractéristique/méthode/unité."
        selected: Variables Sélectionnées
        add-groupVariable: Ajouter à un groupe de variables existant
        add-newGroupVariable: Ajouter à un nouveau groupe de variables
        export-variables: Exporter la liste de variables

</i18n>
