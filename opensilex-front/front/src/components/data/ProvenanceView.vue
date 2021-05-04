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
          @click="dataForm.showCreateForm()"
          label="OntologyCsvImporter.import"
        ></opensilex-CreateButton>
      </template>
    </opensilex-PageActions>

    <opensilex-SearchFilterField
      @search="refresh()"
      @clear="reset()"
      label="ProvenanceView.filter.label"
      :showTitle="true"
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
            :type.sync="filter.actvity_type"
            :baseType="$opensilex.Oeso.PROV_ACTIVITY_TYPE_URI"
            helpMessage="ProvenanceView.agent-type-help"
          ></opensilex-TypeForm>
        </opensilex-FilterField>

        <!-- agent type-->
        <opensilex-FilterField>
          <opensilex-TypeForm
            :type.sync="filter.agent_type"
            :baseType="$opensilex.Oeso.PROV_AGENT_TYPE_URI"
            helpMessage="ProvenanceView.agent-type-help"
            :disabled="provenance.uri != undefined && provenance.uri != null"
          ></opensilex-TypeForm>
        </opensilex-FilterField>

        <!-- agent -->
        <opensilex-FilterField>
          <opensilex-SelectForm
            label="DataView.filter.sensor"
            :selected.sync="filter.agent"
          ></opensilex-SelectForm>
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
          @click="showProvenanceDetailsModal"
        ></opensilex-UriLink>
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
      v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID)"
      ref="provForm"
      component="opensilex-ProvenanceForm"
      createTitle="ProvenanceView.add"
      editTitle="ProvenanceView.update"
      icon="fa#seedling"
      modalSize="lg"
      @onCreate="tableRef.refresh()"
      @onUpdate="tableRef.refresh()"
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
export default class DataView extends Vue {
  $opensilex: any;
  $store: any;
  service: any;
  disabled = false;

  visibleDetails: boolean = false;
  usedVariables: any[] = [];
  selectedProvenance: any = null;

  @Ref("templateForm") readonly templateForm!: any;

  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("tableRef") readonly tableRef!: any;

  @Ref("dataForm") readonly dataForm!: any;

  @Ref("searchField") readonly searchField!: any;

  @Ref("provSelector") readonly provSelector!: any;

  @Ref("resultModal") readonly resultModal!: any;

  @Ref("dataProvenanceModalView") readonly dataProvenanceModalView!: any;

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


  get user() {
    return this.$store.state.user;
  }

  get fields() {
    let tableFields: any = [
      {
        key: "name",
        label: "component.common.name",
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

  showProvenanceDetailsModal(item) {
    this.$opensilex.enableLoader();
    this.getProvenance(item.provenance.uri)
    .then(result => {
      let value = {
        provenance: result,
        data: item
      }
      this.dataProvenanceModalView.setProvenance(value);
      this.dataProvenanceModalView.show();
    });    
  }

  searchProvenance(options) {
    return this.service.searchProvenance(
      this.filter.name,  //name
      null, //description
      null, //activity
      this.filter.activity_type, //activity_type
      this.filter.agent, //agent
      this.filter.agent_type, //agent_type
      undefined, // order_by
      options.currentPage,
      options.pageSize
    )
  }

}
</script>


<style scoped lang="scss">
</style>

<i18n>

en:
  DataView:
    buttons:
      create-data : Add data
      generate-template : Generate template
    description: View and export data
    list:
      date: Date
      variable: Variable
      value: Value
      object: Object
      provenance: provenance
    filter:
      label: Search data
      experiments:  Experiment(s)
      traits: Filter par Trait(s)
      methods:  Method(s)
      units:  Unit(s)
      scientificObjects: scientific object(s)
      sensor: sensor
    placeholder:
      traits: All Traits
      methods: All Methods
      units: All Units

fr:
  DataView:
    buttons:
      create-data : Ajouter un jeu de données
      generate-template : Générer un gabarit
    description: Visualiser et exporter des données
    list:
      date: Date
      variable: Variable
      value: Valeur
      object: objet
      provenance: provenance
    filter:
      label: Rechercher des données
      experiments:  Expérimentation(s)
      traits:  Trait(s)
      methods: Méthode(s)
      units:  Unité(s)
      scientificObject: Objet(s) scientifique(s)
      sensor: capteur
    placeholder:
      traits: Tous les Traits
      methods: Toutes les Méthodes
      units: Toutes les Unités

      
  
</i18n>
