<template>
   <div class="container-fluid">
      <opensilex-PageContent
      class="pagecontent"
    >

           <!-- Toggle Sidebar--> 
      <div class="searchMenuContainer"
      v-on:click="SearchFiltersToggle = !SearchFiltersToggle"
      :title="searchFiltersPannel()">
        <div class="searchMenuIcon">
          <i class="icon ik ik-search"></i>
        </div>
      </div>
      <!-- FILTERS -->
      <Transition>
        <div v-show="SearchFiltersToggle">

    <opensilex-SearchFilterField
      @search="refresh()"
      @clear="reset()"
      label="DataView.filter.label"
      :showTitle="false"
      class="searchFilterField"
    >
      <template v-slot:filters>

        <!-- Name -->
        <div>
          <opensilex-FilterField>
            <label>{{$t('ProvenanceView.name')}}</label>
            <opensilex-StringFilter
              :filter.sync="filter.name"
              placeholder="ProvenanceView.name-placeholder"
              class="searchFilter"
              @handlingEnterKey="refresh()"
            ></opensilex-StringFilter>
          </opensilex-FilterField> <br>
        </div>

        <!-- activity type-->
        <div>
          <opensilex-FilterField>
            <opensilex-TypeForm
              :type.sync="filter.activity_type"
              :baseType="Prov.ACTIVITY_TYPE_URI"
              label="ProvenanceView.activity_type"
              placeholder="ProvenanceView.activity_type-placeholder"
              class="searchFilter"
              @handlingEnterKey="refresh()"
            ></opensilex-TypeForm>
          </opensilex-FilterField>
        </div>

        <!-- Agent type-->
        <div>
          <opensilex-FilterField>
            <opensilex-AgentTypeSelector
              :multiple="false"
              :selected.sync="filter.agent_type"
              @clear="filter.agent = undefined"
              @select="filter.agent = undefined"
              :key="lang"
              class="searchFilter"
              @handlingEnterKey="refresh()"
            >
            </opensilex-AgentTypeSelector>
          </opensilex-FilterField>
        </div>

        <div>
          <opensilex-FilterField v-if="filter.agent_type === 'vocabulary:Operator'">
            <opensilex-AccountSelector
              :users.sync="filter.agent"
              label="ProvenanceForm.agent"
              class="searchFilter"
              @handlingEnterKey="refresh()"
            ></opensilex-AccountSelector>
          </opensilex-FilterField>

          <opensilex-FilterField v-else-if="filter.agent_type">
            <opensilex-DeviceSelector
              ref="deviceSelector"
              label="ProvenanceForm.agent"
              :value.sync="filter.agent"
              :multiple="false"
              :type="filter.agent_type"
              class="searchFilter"
              @handlingEnterKey="refresh()"
            ></opensilex-DeviceSelector>
          </opensilex-FilterField>
        </div>

      </template>
    </opensilex-SearchFilterField>
        </div>
      </Transition>

    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="searchProvenance"
      :fields="fields"
      :isSelectable="true"
      @refreshed="onRefreshed"
      defaultSortBy="name"
      labelNumberOfSelectedRow="ProvenanceList.selected"
      iconNumberOfSelectedRow="ik#ik-target"
    >
      <template v-slot:selectableTableButtons="{ numberOfSelectedRows }">

        <b-dropdown
          dropright
          class="mb-2 mr-2"
          :small="true"
          :text="$t('VariableList.display')">

          <b-dropdown-item-button @click="clickOnlySelected()">{{ onlySelected ? $t('ProvenanceList.selected-all') : $t("component.common.selected-only")}}</b-dropdown-item-button>
          <b-dropdown-item-button @click="resetSelected()">{{$t("component.common.resetSelected")}}</b-dropdown-item-button>
        </b-dropdown>

        <b-dropdown
          v-if="!noActions" 
          dropright
          class="mb-2 mr-2"
          :small="true"
          :disabled="numberOfSelectedRows == 0"
          text="actions"
        >
          <b-dropdown-item-button @click="createDocument()">
            {{ $t("component.common.addDocument") }}
          </b-dropdown-item-button>
        </b-dropdown>
      </template>
      <template v-slot:cell(name)="{data}">
        <opensilex-UriLink
          :uri="data.item.uri"
          :value="data.item.name"
          :to="{path: '/provenances/details/'+ encodeURIComponent(data.item.uri)}"
        ></opensilex-UriLink>
      </template>

      <template v-slot:cell(activity_type)="{ data }">
        {{ (data.item.prov_activity != null && data.item.prov_activity.length>0) ? activityTypes[data.item.prov_activity[0].rdf_type] : null }}        
      </template>

      <template v-slot:cell(activity_start_date)="{ data }">
        {{ (data.item.prov_activity != null && data.item.prov_activity.length>0) ? data.item.prov_activity[0].start_date : null }}
      </template>

      <template v-slot:cell(activity_end_date)="{ data }">
        {{ (data.item.prov_activity != null && data.item.prov_activity.length>0) ? data.item.prov_activity[0].end_date : null }}
      </template>


      <template v-slot:cell(actions)="{data}">
        <b-button-group size="sm">
          <opensilex-EditButton
            v-if="user.hasCredential(credentials.CREDENTIAL_PROVENANCE_MODIFICATION_ID)"
            @click="$emit('onEdit', data.item.uri)"
            label="ProvenanceList.update"
            :small="true"
            
          ></opensilex-EditButton>
          <opensilex-DeleteButton
            v-if="user.hasCredential(credentials.CREDENTIAL_PROVENANCE_DELETE_ID)"
            @click="$emit('onDelete', data.item.uri)"
            label="ProvenanceList.delete"
            :small="true"
          ></opensilex-DeleteButton>
        </b-button-group>
      </template>
    </opensilex-TableAsyncView>

    <opensilex-ModalForm
      ref="documentForm"
      component="opensilex-DocumentForm"
      createTitle="component.common.addDocument"
      modalSize="lg"
      :initForm="initForm"
      icon="ik#ik-file-text"
    ></opensilex-ModalForm>
      </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
// @ts-ignore
import { ProvenanceGetDTO, RDFTypeDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import Oeso from "../../ontologies/Oeso";
import Prov from "../../ontologies/Prov";

@Component
export default class ProvenanceList extends Vue {
  $opensilex: any;
  $store: any;
  service: any;
  disabled = false;
  Prov = Prov;

  get lang() {
    return this.$store.getters.language;
  }

  @Prop({
    default: false
  })
  isSelectable;

  @Prop({
    default: false
  })
  noActions;

  visibleDetails: boolean = false;
  usedVariables: any[] = [];
  selectedProvenance: any = null;
  agentTypes: any[] = [];
  SearchFiltersToggle: boolean = false;

  get user() {
    return this.$store.state.user;
  }

  get onlySelected() {
    return this.tableRef.onlySelected;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("tableRef") readonly tableRef!: any;
  @Ref("deviceSelector") readonly deviceSelector!: any;
  @Ref("documentForm") readonly documentForm!: any;

  filter: any = {
    name: undefined,
    activity_type: undefined,
    agent_type: undefined,
    agent: undefined,
    operator: undefined
  };


  resetFilter() {
    this.filter = {
      name: undefined,
      activity_type: undefined,
      agent_type: undefined,
      agent: undefined,
      operator: undefined
    };
  }

  clearAgents() {
    this.filter.agent = undefined;
    this.filter.operator = undefined;
  }

  get fields() {
    let tableFields: any = [
      {
        key: "name",
        label: "component.common.name",
        sortable: true
      },
      {
        key: "activity_type",
        label: "ProvenanceView.activity_type",
        sortable: true,
      },
      {
        key: "activity_start_date",
        label: "ProvenanceView.activity_start_date",
        sortable: true,
      },
      {
        key: "activity_end_date",
        label: "ProvenanceView.activity_end_date",
        sortable: true,
      },
      {
        key: "actions",
        label: "component.common.actions",
      }    
    ];
    return tableFields;
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

  reset() {
    this.resetFilter();
    this.refresh();
  }

  clickOnlySelected() {
    this.tableRef.clickOnlySelected();
  }

  resetSelected() {
    this.tableRef.resetSelected();
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.DataService");
    this.loadActivityTypes();
    this.$opensilex.updateFiltersFromURL(this.$route.query,this.filter);
  }

  get getSelectedProv() {
    return this.selectedProvenance;
  }

  getProvenance(uri) {
    if (uri != undefined && uri != null) {
      return this.$opensilex
        .getService("opensilex.DataService")
        .getProvenance(uri)
        .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
          return http.response.result;
        });
    }
  }

  loadProvenance(selectedValue) {
    if (selectedValue != undefined && selectedValue != null) {
      this.getProvenance(selectedValue.id).then((prov) => {
        this.selectedProvenance = prov;
      });
    }
  }


  searchProvenance(options) {
    return this.service.searchProvenance(
      this.filter.name,  //name
      undefined, //description
      undefined, //activity
      this.filter.activity_type, //activity_type
      this.filter.agent, //agent
      this.filter.agent_type, //agent_type
      options.orderBy, // order_by
      options.currentPage,
      options.pageSize
    )
  }

  getSelected() {
    return this.tableRef.getSelected();
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
        keywords: undefined,
      },
      file: undefined,
    };
  }


  activityTypes = {};
  loadActivityTypes() {
    this.$opensilex.getService("opensilex.OntologyService")
      .getSubClassesOf(Prov.ACTIVITY_TYPE_URI, true)
      .then((http: HttpResponse<OpenSilexResponse<Array<RDFTypeDTO>>>) => {
        for (let i = 0; i < http.response.result.length; i++) { 
          this.activityTypes[http.response.result[i].uri] = http.response.result[i].name
        }
      })
      .catch(this.$opensilex.errorHandler);
  }
  searchFiltersPannel() {
    return  this.$t("searchfilter.label")
  }

  onRefreshed() {
    let that = this;
    setTimeout(function() {
      if(that.tableRef.selectAll === true && that.tableRef.selectedItems.length !== that.tableRef.totalRow) {                    
        that.tableRef.selectAll = false;
      } 
    }, 1);
  }
}
</script>

<style scoped lang="scss">
.pagecontent {
  width: 102%;
  margin-left : -12px;
}
</style>

<i18n>
en:
  ProvenanceList: 
    selected: Selected provenances
    update: Update provenance
    delete: Delete provenance
    selected-all: All provenances

fr:
  ProvenanceList: 
    selected: Provenances sélectionnées
    update: Modifier la provenance
    delete: Supprimer la provenance
    selected-all: Toutes les provenances

</i18n>
