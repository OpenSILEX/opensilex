<template>
  <div>
    <b-form>
      <br />
      <!-- Label -->
      <opensilex-InputForm
        :value.sync="form.name"
        :disabled="false"
        label="DatasetForm.provenance.name"
        helpMessage="DatasetForm.provenance.name-help"
        type="text"
        placeholder="DatasetForm.provenance.name-placeholder"
      ></opensilex-InputForm>

      <opensilex-UserSelector
        label="DatasetForm.provenance.operators"
        helpMessage="DatasetForm.provenance.operators-help"
        :users.sync="users"
        :multiple="true"
        @select="selectedOperators"
        @deselect="deselectedOperators"
      ></opensilex-UserSelector>

      <!-- description -->
      <opensilex-TextAreaForm
        :value.sync="form.description"
        helpMessage="DatasetForm.provenance.description-help"
        label="DatasetForm.provenance.description"
        placeholder="DatasetForm.provenance.description-placeholder"
      ></opensilex-TextAreaForm>
      <!-- <p class="alert-warning">//TODO Add Settings , etc ...</p>
      <vue-json-editor
        v-model="json"
        :show-btns="false"
        :expandedOnStart="false"
        mode="tree"
        @json-change="onJsonChange"
      ></vue-json-editor>
      <br />
      <p class="alert-warning">//TODO Add document at creation , etc ...</p> -->
    </b-form>
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
export default class ExpProvenanceForm extends Vue {
  $opensilex: any;
  $store: any;
  $i18n: any;
  $t: any;

  @Ref("validatorRef") readonly validatorRef!: any;
  
  //TODO Add document at creation , etc ...</p> -->
  // onJsonChange(value) {
  //   console.log("value:", value);
  // }
  // json = {
  //   startedAtTime: "",
  //   endedAtTime: "",
  //   settings: {},
  //   rdf_type: "",
  // };
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
