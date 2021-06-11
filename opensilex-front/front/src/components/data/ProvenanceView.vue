<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-bar-chart-line"
      title="component.menu.data.provenance"
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

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-ProvenanceList> </opensilex-ProvenanceList>
      </template>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import { Prop, Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import { ProvenanceGetDTO, RDFTypeDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import PROV from "../../ontologies/PROV";
import Oeso from "../../ontologies/Oeso";

@Component
export default class ProvenanceView extends Vue {
  $opensilex: any;
  $store: any;
  service: any;
  disabled = false;
  PROV = PROV;

  visibleDetails: boolean = false;
  usedVariables: any[] = [];
  selectedProvenance: any = null;
  agentTypes: any[] = [];


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

  mounted() {
    this.loadAgentTypes();
  }

  loadAgentTypes() {
    let agentURIs = [
      Oeso.SENSOR_TYPE_URI, 
      Oeso.OPERATOR_TYPE_URI, 
      Oeso.SOFTWARE_TYPE_URI
    ];
    this.$opensilex.getService("opensilex.OntologyService")
    .getClasses(agentURIs, undefined)
    .then((http: HttpResponse<OpenSilexResponse<Array<RDFTypeDTO>>>) => {
      for (let i = 0; i < http.response.result.length; i++) {   
        this.agentTypes.push({
            id: http.response.result[i].uri,
            label: http.response.result[i].name,
          });
      }
    })
    .catch(this.$opensilex.errorHandler);
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
      options.orderBy, // order_by
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

    if (item.prov_activity != null && item.prov_activity.length > 0) {
      activity = {
        rdf_type: item.prov_activity[0].rdf_type,
        start_date: item.prov_activity[0].start_date,
        end_date: item.prov_activity[0].end_date
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
    description: description
    name: Name
    activity_type: Activity type
    agent_type: Agent type
    agent: Agent
    activity_start_date: Start date
    activity_end_date: End date
    name-placeholder: Enter provenance name
    activity_type-placeholder: Select a type of activity
    agent_type-placeholder: Select a type of agent

fr:
  ProvenanceView:
    add: Ajouter une provenance
    description: description
    name: Nom
    activity_type: Type d'activité
    agent_type: Type d'agent
    agent: Agent
    activity_start_date: Date de début
    activity_end_date: Date de fin
    name-placeholder: Entrer un nom de provenance
    activity_type-placeholder: Selectionner un type d'activité
    agent_type-placeholder: Selectionner un type d'agent
  
</i18n>
