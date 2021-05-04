<template>
  <b-form>
    <div v-if="dataForm.provenanceURI">
      <opensilex-ExperimentSelector
        label="DataForm.experiments"
        :experiments.sync="dataForm.experiments"
        :multiple="true"
      ></opensilex-ExperimentSelector>

      <b-form-group label="How do you want to import your data ?">
        <b-form-radio-group
          v-model="selected"
          :options="options"
          class="mb-3"
          value-field="item"
          text-field="name"
          :required="true"
        ></b-form-radio-group>
      </b-form-group>

      <!-- Upload file  -->
      <div v-if="selected=='importCSV'">
        <opensilex-GenerateDataTemplateFrom
          ref="templateForm"
        ></opensilex-GenerateDataTemplateFrom>
        <div>
          <label
            >{{ $t("DatasetForm.dataFile") }}
            <span class="required">*</span></label
          >

          <b-row>
            <b-col cols="4">
              <b-form-file
                size="sm"
                ref="inputFile"
                accept="text/csv, .csv"
                @input="uploadCSV"
                :placeholder="$t('DatasetForm.csv-file-placeholder')"
                :drop-placeholder="$t('DatasetForm.csv-file-drop-placeholder')"
                :browse-text="$t('component.common.import-files.select-button')"
                v-model="file"
                :state="Boolean(file)"
              ></b-form-file>
            </b-col>
            <b-col cols="2">
              <opensilex-Button
                variant="secondary"
                @click="templateForm.show()"
                class="mr-2"
                :small="false"
                icon
                label="DataView.buttons.generate-template"
              ></opensilex-Button>
            </b-col>
            <b-col cols="5"></b-col>
          </b-row>
          <br />
        </div>
        <div>
          <opensilex-DataHelpTableView></opensilex-DataHelpTableView>
        </div>
        <!-- validation report  -->
        <opensilex-DataValidationReport
          v-if="form.dataFile != null"
          ref="validationReport"
        >
        </opensilex-DataValidationReport>
        <p v-if="!isImported && isValid && insertionError" class="alert-warning">
          {{ $t("DatasetForm.data-not-imported") }}
        </p>
        <p
          v-if="!isImported && tooLargeDataset && insertionError"
          class="alert alert-warning"
        >
          {{ $t("DatasetForm.data-too-much-data") }}
        </p>
        <p
          v-if="
            insertionError && form.dataFile != null && insertionDataError != null
          "
          class="alert alert-warning"
        >
          {{ $t("DatasetForm.error") }} : {{ insertionDataError.title }}
          <br />
          {{ $t("DatasetForm.message") }} : {{ insertionDataError.message }}
        </p>
        <br />
      </div>
      <div v-if="selected=='WS'">

      <opensilex-UriView
        :uri="dataForm.provenanceURI"
        title="provenance URI"
      ></opensilex-UriView>
      <br />
      </div>
    </div>
    <div v-else>no prov</div>
  </b-form>
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
export default class DataForm2 extends Vue {
  $opensilex: any;
  $store: any;
  $i18n: any;
  $t: any;

  @PropSync("form")
  dataForm;

  selected: any = null;
  options = [
          { item: 'importCSV', name: 'import CSV' },
          { item: 'WS', name: 'Web Services' }
        ]  

}
</script>
<style scoped lang="scss">
::v-deep .jsoneditor-menu {
  background-color: #a7a7a7;
  border-bottom: 1px solid #a7a7a7;
}
</style>
