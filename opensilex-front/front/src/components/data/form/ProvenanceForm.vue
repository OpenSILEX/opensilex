<template>
  <div>
    <!-- Label -->
    <opensilex-InputForm
      :value.sync="provenance.name"
      :disabled="provenance.uri != undefined && provenance.uri != null"
      label="DataForm.provenance.name"
      helpMessage="DataForm.provenance.name-help"
      type="text"
      placeholder="DataForm.provenance.name-placeholder"
      :required="true"
    ></opensilex-InputForm>

    <!-- description -->
    <opensilex-TextAreaForm            
      :value.sync="provenance.description"
      helpMessage="DataForm.provenance.description-help"
      label="DataForm.provenance.description"
      placeholder="DataForm.provenance.description-placeholder"
      :disabled="provenance.uri != undefined && provenance.uri != null"
    ></opensilex-TextAreaForm>

    <!--activity -->
    <b-card title="Activity" bg-variant="light">
      <!-- type -->
      <opensilex-TypeForm
        v-bind:style="styleObject"
        :type.sync="provenance.activity.rdf_type"
        :baseType="$opensilex.Oeso.PROV_ACTIVITY_TYPE_URI"
        :required="false"
        helpMessage="DataForm.type-help"
        :disabled="provenance.uri != undefined && provenance.uri != null"
      ></opensilex-TypeForm>

      <!-- start_date  & end_date-->
      <div class="row">
        <div class="col">
          <opensilex-DateTimeForm
            :value.sync="provenance.activity.start_date"
            label="DataForm.start"
            :maxDate="form.end"
            :required="true"
            @change="updateRequiredProps"
            helpMessage="DataForm.start-help"
            :disabled="provenance.uri != undefined && provenance.uri != null"
          ></opensilex-DateTimeForm>
        </div>

        <div class="col">
          <opensilex-DateTimeForm
            :value.sync="provenance.activity.end_date"
            :minDate="form.start"
            label="DataForm.end"
            @change="updateRequiredProps"
            helpMessage="DataForm.end-help"
            :disabled="provenance.uri != undefined && provenance.uri != null"
          ></opensilex-DateTimeForm>
        </div>
      </div>

    <!-- uri -->
    <opensilex-InputForm
      :value.sync="provenance.activity.uri"
      label=""
      type="url"
      rules="url"
      helpMessage=""
      :disabled="provenance.uri != undefined && provenance.uri != null"
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
        :disabled="provenance.uri != undefined && provenance.uri != null"
        :required="selected == 'sensor'"
      ></opensilex-DeviceSelector>

      <!-- vectors -->
      <opensilex-DeviceSelector
        v-if="selected == 'sensor'"
        label="DataForm.vectors"
        :devices.sync="provenance.sensors"
        :multiple="true"
        :disabled="provenance.uri != undefined && provenance.uri != null"
      ></opensilex-DeviceSelector>

      <!-- softwares -->
      <opensilex-DeviceSelector
        v-if="selected == 'softwares'"
        label="DataForm.softwares"
        :devices.sync="provenance.softwares"
        :multiple="true"
        :disabled="provenance.uri != undefined && provenance.uri != null"
      ></opensilex-DeviceSelector>

      <!-- operators -->
      <opensilex-UserSelector
        label="DataForm.provenance.operators"
        helpMessage="DataForm.provenance.operators-help"
        :users.sync="provenance.operators"
        :multiple="true"
        @select="selectedOperators"
        @deselect="deselectedOperators"
        :disabled="provenance.uri != undefined && provenance.uri != null"
      ></opensilex-UserSelector>
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

  @Ref("validatorRef") readonly validatorRef!: any;
  
  provenance = null;
  users = [];
  @Prop({
    default: () => {
      return {
        uri: null,
        name: null,
        description: null,
        experiments: [],
        prov_activity: [],
        prov_agent: [],
      };
    },
  })
  form: any;

  @Prop()
  editMode;

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

  selectedOperators(valueToSelect) {
    let agent: AgentModel = {};
    agent.uri = valueToSelect.id;
    agent.rdf_type = Oeso.OPERATOR_TYPE_URI;
    agent.settings = {};
    this.form.prov_agent.push(agent);
  }
  deselectedOperators(valueToDeselect) {
    this.form.prov_agent = this.form.prov_agent.filter(function (
      agent,
      index,
      arr
    ) {
      return agent.uri != valueToDeselect.id;
    });
  }

  create() {
    // format form
    let formNew = JSON.parse(JSON.stringify(this.form));

    return this.$opensilex
      .getService("opensilex.DataService")
      .createProvenance(formNew)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("provenance created", uri);
        formNew.uri = uri;
        return formNew;
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
