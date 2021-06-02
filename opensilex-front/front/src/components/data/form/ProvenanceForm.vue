<template>
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
    <!-- Label -->
    <opensilex-InputForm
      :value.sync="provenance.name"
      label="DataForm.provenance.name"
      helpMessage="DataForm.provenance.name-help"
      type="text"
      placeholder="DataForm.provenance.name-placeholder"
      :required="true"
      :validationDisabled="validationDisabled"
    ></opensilex-InputForm>

    <!-- description -->
    <opensilex-TextAreaForm            
      :value.sync="provenance.description"
      helpMessage="DataForm.provenance.description-help"
      label="DataForm.provenance.description"
      placeholder="DataForm.provenance.description-placeholder"
    ></opensilex-TextAreaForm>

    <!--activity -->
    <b-card title="Activity" bg-variant="light">
      <!-- type -->
      <opensilex-TypeForm
        :type.sync="provenance.activity.rdf_type"
        :baseType="PROV.ACTIVITY_TYPE_URI"
        :required="true"
        helpMessage="DataForm.type-help"
        :validationDisabled="validationDisabled"
      ></opensilex-TypeForm>

      <!-- start_date  & end_date-->
      <div class="row">
        <div class="col">
          <opensilex-DateTimeForm
            :value.sync="provenance.activity.start_date"
            label="DataForm.start"
            :required="true"
            helpMessage="DataForm.start-help"
            :validationDisabled="validationDisabled"
          ></opensilex-DateTimeForm>
        </div>

        <div class="col">
          <opensilex-DateTimeForm
            :value.sync="provenance.activity.end_date"
            label="DataForm.end"
            helpMessage="DataForm.end-help"
            :validationDisabled="validationDisabled"
          ></opensilex-DateTimeForm>
        </div>
      </div>

    <!-- uri -->
    <opensilex-InputForm
      :value.sync="provenance.activity.uri"
      label="url"
      type="url"
      rules="url"
      helpMessage=""
    ></opensilex-InputForm>
    </b-card>

    <!-- agents -->
    <b-card title="agents" bg-variant="light">

    <!-- sensors -->
      <opensilex-DeviceSelector
        v-if="selected == 'sensor'"
        label="DataForm.sensors"
        :devices.sync="provenance.sensors"
        :multiple="true"
        :required="selected == 'sensor'"
        @select="selectedSensors"
        @deselect="deselectedSensors"
      ></opensilex-DeviceSelector>

      <!-- vectors -->
      <!-- <opensilex-DeviceSelector
        v-if="selected == 'sensor'"
        label="DataForm.vectors"
        :devices.sync="provenance.sensors"
        :multiple="true"
      ></opensilex-DeviceSelector> -->

      <!-- softwares -->
      <opensilex-DeviceSelector
        v-if="selected == 'computed'"
        label="DataForm.softwares"
        :devices.sync="provenance.softwares"
        :multiple="true"
        @select="selectedSoftwares"
        @deselect="deselectedSoftwares"
      ></opensilex-DeviceSelector>

      <!-- operators -->
      <opensilex-UserSelector
        label="DataForm.provenance.operators"
        helpMessage="DataForm.provenance.operators-help"
        :users.sync="provenance.operators"
        :multiple="true"
        @select="selectedOperators"
        @deselect="deselectedOperators"
      ></opensilex-UserSelector>
    </b-card>
  </ValidationObserver>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import Oeso from "../../../ontologies/Oeso";
import PROV from "../../../ontologies/PROV";
//TODO Add document at creation , etc ...</p> -->
// import vueJsonEditor from "vue-json-editor";
// Vue.component("vue-json-editor", vueJsonEditor);
// @ts-ignore
import { AgentModel, ProvenanceGetDTO } from "opensilex-core/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class ProvenanceForm extends Vue {
  $opensilex: any;
  $store: any;
  $i18n: any;
  $t: any;
  PROV = PROV;

  @Ref("validatorRef") readonly validatorRef!: any;
  
  users = [];
  @Prop({
    default: () => {
      return {
        uri: null,
        name: null,
        description: null,
        prov_activity: [],
        prov_agent: [],
      };
    },
  })
  form: any;

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

  selected: any = null;

  options = [
          { item: 'observations', name: 'Observations' },
          { item: 'sensor', name: 'Sensor data' },
          { item: 'computed', name: 'Computed data' },
        ]

  @Prop()
  editMode;

  @Prop({
    default: false
  })
  validationDisabled: boolean;

  getEmptyForm() {
    return {
      uri: null,
      name: null,
      description: null,
      experiments: [],
      prov_activity: [],
      prov_agent: [],
    };
  }

  

  create() {
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

    // format form
    this.form.name = this.provenance.name;
    this.form.description = this.provenance.description;
    this.form.prov_activity.push(this.provenance.activity);
    this.form.prov_agent = agents;

    return this.$opensilex
      .getService("opensilex.DataService")
      .createProvenance(this.form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("provenance created", uri);
        this.form.uri = uri;
        return this.form;
      });
  }
  update(form) {}

  reset() {
    this.users = [];
  }

  loadProvenance(selectedValue) {
    if (selectedValue != null) {
      this.$opensilex
        .getService("opensilex.DataService")
        .getProvenance(selectedValue.id)
        .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
          this.form.provenance = http.response.result;
        });
    }
  }
}
</script>
<style scoped lang="scss">
::v-deep .jsoneditor-menu {
  background-color: #a7a7a7;
  border-bottom: 1px solid #a7a7a7;
}
</style>
