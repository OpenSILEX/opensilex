<template>
  
    <div>
      <ValidationObserver ref="validatorRef">
      
      <b-form-group label="What kind of data do you want to import ?">
        <b-form-radio-group
          v-model="selected"
          :options="options"
          class="mb-3"
          value-field="item"
          text-field="name"
          :required="true"
        ></b-form-radio-group>
      </b-form-group>

      <div v-if="selected" ref="successMessage">
        <opensilex-ProvenanceSelector      
          ref="provSelector"
          :provenances.sync="provenance.uri"
          :filterLabel="filterProvenanceLabel"
          label="Select a provenance"
          @select="loadProvenance"
          @clear="reset()"
          :multiple="false"
          :viewHandler="showProvenanceDetails"
          :viewHandlerDetailsVisible="visibleDetails"
          :showURI="false"
          :required="true"
        ></opensilex-ProvenanceSelector>
        <b-alert variant="success" :show="createOK">The provenance has been successfully created</b-alert>        
      </div>  
      </ValidationObserver>  
        
      <b-card bg-variant="light" v-if="selected">
        <opensilex-ProvenanceForm>
        </opensilex-ProvenanceForm>

      </b-card>

    </div>
  
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import Oeso from "../../../ontologies/Oeso";
//TODO Add document at creation , etc ...</p> -->
// import vueJsonEditor from "vue-json-editor";
// Vue.component("vue-json-editor", vueJsonEditor);
import {
  DataCreationDTO,
  AgentModel,
  ProvenanceGetDTO,
} from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class DataForm1 extends Vue {
  $opensilex: any;
  $store: any;
  $i18n: any;
  $t: any;

  @Ref("provSelector") readonly provSelector!: any;
  @Ref("validatorRef") readonly validatorRef!: any;
  @Ref("successMessage") readonly successMessage!: any;
  
  selected: any = null;

  options = [
          { item: 'observations', name: 'Observations' },
          { item: 'sensor', name: 'Sensor data' },
          { item: 'computed', name: 'Computed data' },
        ]

  createOK: boolean = false;

  styleObject: {
    color: 'transparent',
    background: 'red'
  }

  @PropSync("form")
  dataForm;

  provenance = {
    uri: null,
    name: null,
    description: null,
    activity: {
      rdf_type: null,
      start_date: null,
      end_date: null,
      uri: null,
    },
    sensors: [],
    vectors: [],
    softwares: [],
    operators: []
  }

  filterLabel = null;

  getProvenance(uri) {
    if (uri != null) {
      return this.$opensilex
        .getService("opensilex.DataService")
        .getProvenance(uri)
        .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
          return http.response.result;
        });
    }
  }

  loadProvenance(selectedValue) {
    this.createOK = false;
    if (selectedValue != undefined && selectedValue != null) {
      this.getProvenance(selectedValue.id).then((prov) => {
        this.resetProv();
        this.provenance.name = prov.name;
        this.provenance.uri = prov.uri;
        this.dataForm.provenanceURI = prov.uri;

        if (prov.description != null) {
          this.provenance.description = prov.description;
        } else {
          this.provenance.description = ""
        }

        if (prov.prov_activity != null && prov.prov_activity.length>0) {
          this.provenance.activity.rdf_type = prov.prov_activity[0].rdf_type;
          this.provenance.activity.start_date = prov.prov_activity[0].start_date;
          this.provenance.activity.end_date = prov.prov_activity[0].end_date;
          this.provenance.activity.uri = prov.prov_activity[0].uri;
        }

        if (prov.prov_agent != null) {
          let sensors = [];
          let vectors = [];
          let softwares = [];
          let operators = [];
          for (let i in prov.prov_agent) {
            let agent = prov.prov_agent[i];
            if (Oeso.checkURIs(agent.rdf_type, Oeso.SENSOR_TYPE_URI)) {
              sensors.push(agent.uri);              
            } else if (Oeso.checkURIs(agent.rdf_type, Oeso.VECTOR_TYPE_URI)) {
              vectors.push(agent.uri);
            } else if (Oeso.checkURIs(agent.rdf_type, Oeso.SOFTWARE_TYPE_URI)) {
              softwares.push(agent.uri);
            } else if (Oeso.checkURIs(agent.rdf_type, Oeso.OPERATOR_TYPE_URI)) {
              operators.push(agent.uri);
            }
          }
          this.provenance.sensors = sensors;
          this.provenance.vectors = vectors;
          this.provenance.softwares = softwares;
          this.provenance.operators = operators;          
        }
      });
    }
  }

  copyProvenance() {
    this.provenance.uri = undefined;
  }

  createProvenance() {
    let agents = []
    for (let i in this.provenance.sensors) {
      agents.push({
        uri: i,
        rdf_type: Oeso.SENSOR_TYPE_URI
      })
    }

    for (let i in this.provenance.vectors) {
      agents.push({
        uri: i,
        rdf_type: Oeso.VECTOR_TYPE_URI
      })
    }

    for (let i in this.provenance.softwares) {
      agents.push({
        uri: i,
        rdf_type: Oeso.SOFTWARE_TYPE_URI
      })
    }

    for (let i in this.provenance.softwares) {
      agents.push({
        uri: i,
        rdf_type: Oeso.OPERATOR_TYPE_URI
      })
    }

    let form = {
      name: this.provenance.name,
      description: this.provenance.description,
      prov_activity: [this.provenance.activity],
      prov_agent: agents
    }

    return this.$opensilex
      .getService("opensilex.DataService")
      .createProvenance(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("provenance created", uri);
        this.provenance.uri = uri;
        this.provSelector.select({ id: uri, label: form.name });
        this.createOK = true;
        this.$nextTick(() => {
          this.successMessage.scrollIntoView({behavior: 'smooth'});
        }) 
      });
  }

  validate() {
    return this.validatorRef.validate();
  }

  reset() {
    this.createOK = false;
    this.filterLabel = null;
    this.resetProv();
  }

  resetProv() {
    this.provenance = {
      uri: null,
      name: null,
      description: null,
      activity: {
        rdf_type: null,
        start_date: null,
        end_date: null,
        uri: null,
      },
      sensors: [],
      vectors: [],
      softwares: [],
      operators: []
    }
  }
  
}
</script>
<style scoped lang="scss">

::v-deep .form-control:disabled::placeholder {
    color: transparent;
}

::v-deep .vue-treeselect__placeholder {
    color: transparent;

}

::v-deep .vue-treeselect__disabled {
  background-color: grey;
}

</style>
