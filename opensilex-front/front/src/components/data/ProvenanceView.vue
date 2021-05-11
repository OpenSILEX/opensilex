<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-bar-chart-line"
      title="component.menu.provenance.label"
      description="ProvenanceView.description"
    ></opensilex-PageHeader>

    <opensilex-PageActions>
      <template v-slot>
        <opensilex-CreateButton
          @click="createProvenance()"
          label="ProvenanceView.add"
        ></opensilex-CreateButton>
      </template>
    </opensilex-PageActions>

    <opensilex-SearchFilterField
      @search="refresh()"
      @clear="reset()"
      label="DataView.filter.label"
      :showTitle="false"
    >
      <template v-slot:filters>

        <!-- Name -->
        <opensilex-FilterField>
          <label>{{$t('ProvenanceView.filter.name')}}</label>
          <opensilex-StringFilter
            :filter.sync="filter.name"
            placeholder="ProvenanceView.filter.name-placeholder"
          ></opensilex-StringFilter>
        </opensilex-FilterField>

        <!-- activity type-->
        <opensilex-FilterField>
          <opensilex-TypeForm
            :type.sync="filter.activity_type"
            :baseType="$opensilex.Oeso.PROV_ACTIVITY_TYPE_URI"
            label="ProvenanceView.filter.activity_type"
          ></opensilex-TypeForm>
        </opensilex-FilterField>

        <!-- agent type-->
        <opensilex-FilterField>
          <opensilex-TypeForm
            :type.sync="filter.agent_type"
            :baseType="$opensilex.Oeso.DEVICE_TYPE_URI"
            label="ProvenanceView.filter.agent_type"
          ></opensilex-TypeForm>
        </opensilex-FilterField>

        <!-- agent -->
        <opensilex-FilterField>
          <opensilex-DeviceSelector
            label="ProvenanceView.filter.agent"
            :devices.sync="filter.agent"
            :multiple="false"
          ></opensilex-DeviceSelector>
        </opensilex-FilterField>

      </template>
    </opensilex-SearchFilterField>

    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="searchProvenance"
      :fields="fields"
      defaultSortBy="name"
    >
      <template v-slot:cell(name)="{data}">
        <opensilex-UriLink
          :uri="data.item.uri"
          :value="data.item.name"
          :noExternalLink="true"
        ></opensilex-UriLink>
      </template>

      <template v-slot:cell(activity_type)="{ data }">
        {{ getActivity(data.item).rdf_type }}
      </template>

      <template v-slot:cell(activity_start_date)="{ data }">
        {{ getActivity(data.item).start_date }}
      </template>

      <template v-slot:cell(activity_end_date)="{ data }">
        {{ getActivity(data.item).end_date }}
      </template>


      <template v-slot:cell(actions)="{data}">
        <b-button-group size="sm">
          <opensilex-EditButton
            v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID) && !data.item.rdf_type.endsWith('Species')"
            @click="$emit('onEdit', data.item.uri)"
            label="GermplasmList.update"
            :small="true"
            
          ></opensilex-EditButton>
          <opensilex-DeleteButton
            v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_DELETE_ID)"
            @click="$emit('onDelete', data.item.uri)"
            label="GermplasmList.delete"
            :small="true"
          ></opensilex-DeleteButton>
        </b-button-group>
      </template>
    </opensilex-TableAsyncView>


    <opensilex-ModalForm
      ref="provenanceForm"
      component="opensilex-ProvenanceForm"
      createTitle="ProvenanceView.add"
      editTitle="ProvenanceView.update"
      icon="fa#seedling"
      modalSize="lg"
      @onCreate="tableRef.refresh()"
      @onUpdate="tableRef.refresh()"
      :initForm="initForm"
      :successMessage="successMessage"
    ></opensilex-ModalForm>
  </div>
</template>

<script lang="ts">
import { Prop, Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueConstructor from "vue";
import VueI18n from "vue-i18n";
import moment from "moment";
import { ProvenanceGetDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import { UserGetDTO } from "opensilex-security/index";

@Component
export default class ProvenanceView extends Vue {
  $opensilex: any;
  $store: any;
  service: any;
  disabled = false;

  visibleDetails: boolean = false;
  usedVariables: any[] = [];
  selectedProvenance: any = null;


  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("tableRef") readonly tableRef!: any;
  @Ref("provenanceForm") readonly provenanceForm!: any;

  filter = {
    name: undefined,
    activity_type: undefined,
    agent_type: undefined,
    agent: undefined
  };

  resetFilter() {
    this.filter = {
      name: undefined,
      activity_type: undefined,
      agent_type: undefined,
      agent: undefined
    };
  }

  createProvenance() {
    this.provenanceForm.showCreateForm()
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
      }
    ];
    return tableFields;
  }

  refresh() {
    this.tableRef.refresh();
  }

  reset() {
    this.resetFilter();
    this.refresh();
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.DataService");
    let query: any = this.$route.query;

    this.reset();
    for (let [key, value] of Object.entries(this.filter)) {
      if (query[key]) {
        this.filter[key] = decodeURIComponent(query[key]);
      }
    }
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
      undefined, // order_by
      options.currentPage,
      options.pageSize
    )
  }

  getActivity(item) {
    let activity = {
      rdf_type:null,
      start_date: null,
      end_date: null
    }

    if (item.activity != null || item.activity.length() > 0) {
      activity = {
        rdf_type: item.activity[0].rdf_type,
        start_date: item.activity[0].start_date,
        end_date: item.activity[0].end_date
      }
    }

    return activity;
  }

}
</script>


<style scoped lang="scss">
</style>

<i18n>

en:
  ProvenanceView:
    add: Add provenance

fr:
  ProvenanceView:
    add: Ajouter une provenance      
  
</i18n>
