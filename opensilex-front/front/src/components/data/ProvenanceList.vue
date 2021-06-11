<template>
  <div>
    <opensilex-SearchFilterField
      @search="refresh()"
      @clear="reset()"
      label="DataView.filter.label"
      :showTitle="false"
    >
      <template v-slot:filters>

        <!-- Name -->
        <opensilex-FilterField>
          <label>{{$t('ProvenanceView.name')}}</label>
          <opensilex-StringFilter
            :filter.sync="filter.name"
            placeholder="ProvenanceView.name-placeholder"
          ></opensilex-StringFilter>
        </opensilex-FilterField>

        <!-- activity type-->
        <opensilex-FilterField>
          <opensilex-TypeForm
            :type.sync="filter.activity_type"
            :baseType="PROV.ACTIVITY_TYPE_URI"
            label="ProvenanceView.activity_type"
            placeholder="ProvenanceView.activity_type-placeholder"
          ></opensilex-TypeForm>
        </opensilex-FilterField>

        <!-- agent type-->
        <opensilex-FilterField>
          <opensilex-SelectForm
            label="ProvenanceView.agent_type"
            :multiple="true"
            :selected.sync="filter.agent_type"
            :options="agentTypes"
            placeholder="ProvenanceView.agent_type-placeholder"
          ></opensilex-SelectForm>
        </opensilex-FilterField>

        <!-- agent -->
        <opensilex-FilterField>
          <opensilex-DeviceSelector
            label="ProvenanceView.agent"
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
      :isSelectable="true"
      labelNumberOfSelectedRow="ProvenanceList.selected"
      iconNumberOfSelectedRow="ik#ik-target"
    >
      <template v-slot:cell(name)="{data}">
        <opensilex-UriLink
          :value="data.item.name"
          :to="{path: '/provenances/details/'+ encodeURIComponent(data.item.uri)}"
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

  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
// @ts-ignore
import { ProvenanceGetDTO, RDFTypeDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import PROV from "../../ontologies/PROV";
import Oeso from "../../ontologies/Oeso";

@Component
export default class ScientificObjectList extends Vue {
  $opensilex: any;
  $store: any;
  service: any;
  disabled = false;
  PROV = PROV;

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


  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("tableRef") readonly tableRef!: any;

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

  // loadAgentTypes() {
  //   let agentURIs = [
  //     Oeso.SENSOR_TYPE_URI, 
  //     Oeso.OPERATOR_TYPE_URI, 
  //     Oeso.SOFTWARE_TYPE_URI
  //   ];
  //   this.$opensilex.getService("opensilex.OntologyService")
  //   .getClasses(agentURIs, undefined)
  //   .then((http: HttpResponse<OpenSilexResponse<Array<RDFTypeDTO>>>) => {
  //     for (let i = 0; i < http.response.result.length; i++) {   
  //       this.agentTypes.push({
  //           id: http.response.result[i].uri,
  //           label: http.response.result[i].name,
  //         });
  //     }
  //   })
  //   .catch(this.$opensilex.errorHandler);
  // }

  loadAgentTypes() {
    this.$opensilex.getService("opensilex.OntologyService")
    .getSubClassesOf(Oeso.DEVICE_TYPE_URI, true)
    .then((http: HttpResponse<OpenSilexResponse<Array<RDFTypeDTO>>>) => {
      for (let i = 0; i < http.response.result.length; i++) {   
        this.agentTypes.push({
            id: http.response.result[i].uri,
            label: http.response.result[i].name,
          });
      }
    })

    this.$opensilex.getService("opensilex.OntologyService")
    .getSubClassesOf("http://xmlns.com/foaf/0.1/Agent", true)
    .then((http: HttpResponse<OpenSilexResponse<Array<RDFTypeDTO>>>) => {
      for (let i = 0; i < http.response.result.length; i++) {   
        this.agentTypes.push({
            id: http.response.result[i].uri,
            label: http.response.result[i].name,
          });
      }
    })
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

  getSelected() {
    return this.tableRef.getSelected();
  }
  
}
</script>

<style scoped lang="scss">
</style>

<i18n>

en:
  ScientificObjectList:
    name-placeholder: Enter name
    selected: Selected Scientific Objects
    description: Manage and configure scientific objects
    advancedSearch: Advanced search
    propetiesConfiguration: Properties to display
    not-used-in-experiments: Scientific object not used in any experiments
    experiments: Scientific object used in experiment(s)
    column:
      alias: Nom
      experiments: Experiment(s)
      type: Type
      uri: URI
      parents: Parent(s)
      actions: Actions
    placeholder:
      experiments: All Experiments
      germplasm: All Germplasm
      types: All Types
      uri: All URI
      isPartOf: All Alias / URI
      position: All Spacial Positions
      factors: All Factors
      dates: All Dates
    filter:
      label: Search for Scientific Objects
      experiments: Filter by Experiments
      germplasm: Filter by Germplasm
      types: Filter by Type(s)
      uri: Filter by  URI
      isPartOf: isPartOf (Alias ou URI)
      position: Filter by Spatial Position
      factors: Filter by Factors
      dates: Filter by Dates
    creationDate:  Creation date
    destructionDate: Destruction date
    existenceDate: Object exists
    visualize: Visualize
fr:
  ScientificObjectList:
    name-placeholder: Saisir un nom
    selected: Objets Scientifiques Sélectionnés
    description: Gérer et configurer les objets scientifiques
    advancedSearch: Recherche avancée
    propetiesConfiguration: Propriétés à afficher
    not-used-in-experiments: L'objet scientifique n'est utilisé dans aucune expérimentation
    experiments: L'objet scientifique est utilisé dans le/les expérimentation(s)
    column:
      alias: Name
      experiments: Expérimentation(s)
      type: Type
      uri: URI
      parents: Parent(s)
      actions: Actions
    placeholder:
      experiments: Toutes les Expérimentations
      germplasm: Tous les Matériels Génétiques
      types: Tous les Types
      uris: Toutes les URI
      isPartOf: Tous les Alias / URI
      position: Toutes les Positions Spaciales
      factors: Tous les Facteurs
      dates: Toutes les Dates
    filter:
      label: Rechercher des Objets Scientifiques
      experiments: Filtrer par Expérimentation(s)
      germplasm: Filtrer par Matériel Génétiques
      types: Filtrer par Type(s)
      uri: Filtrer par URI
      isPartOf: isPartOf (Alias ou URI)
      position: Filtrer par Position Spaciale
      factors: Filtrer par Facteurs
      dates: Filtrer par Dates
    creationDate:  Date de création
    destructionDate: Date de destruction
    existenceDate: Date d'existence
    visualize: Visualiser
</i18n>
