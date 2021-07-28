<template>
  <b-modal
    ref="modalRef"
    size="lg"
    ok-only
    :static="true"
    @hide="requiredField = false"
    @show="requiredField = true"
  >
    <template v-slot:modal-ok>{{ $t("component.common.close") }}</template>
    <template v-slot:modal-title>{{ $t("DataHelp.title") }}</template>

    <div>
      <ValidationObserver ref="validatorRefDataTemplate">
        <br />
        <!-- Experiments -->
        <opensilex-ExperimentSelector
          v-if="selectExperiment"
          label="DataForm.experiment"
          :multiple="false"
          :experiments.sync="experiment"
          :required="true"
        ></opensilex-ExperimentSelector>
        <b-row>
          <b-col cols="9">
            <opensilex-VariableSelector
              label="VariableView.title"
              placeholder="VariableList.label-filter-placeholder"
              :multiple="true"
              :variables.sync="variables"
              :required="requiredField"
            >
            </opensilex-VariableSelector>
            <opensilex-CheckboxForm
              :value.sync="withRawData"
              title="DataTemplateForm.with-raw-data"
            ></opensilex-CheckboxForm>            
          </b-col>
          <b-col>
            <opensilex-CSVSelectorInputForm :separator.sync="separator">
            </opensilex-CSVSelectorInputForm>
          </b-col>
        </b-row>
        <b-button @click="csvExport" variant="outline-primary">{{
          $t("OntologyCsvImporter.downloadTemplate")
        }}</b-button>
        <b-button
          v-if="variables.length == 0"
          class="float-right"
          @click="csvExportDataExample"
          variant="outline-info"
          >{{ $t("DataHelp.download-template-example") }}</b-button
        >
        <hr />
      </ValidationObserver>
    </div>
  </b-modal>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { VariablesService, VariableDatatypeDTO } from "opensilex-core/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class GenerateDataTemplateFrom extends Vue {
  $opensilex: any;
  $store: any;
  $t: any;
  $i18n: any;
  $papa: any;
  service: VariablesService;

  requiredField: boolean = false;

  separator = ",";

  withRawData = false;

  @Prop()
  editMode;

  @Prop({ default: true })
  uriGenerated;

  @Ref("validatorRefDataTemplate") readonly validatorRefDataTemplate!: any;

  @Ref("modalRef") readonly modalRef!: any;

  get user() {
    return this.$store.state.user;
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.VariablesService");
  }

  @Prop({ default: true })
  selectExperiment: boolean;

  @Prop({ default: null })
  experiment: string;

  variables: any[] = [];

  variableFilter: string;

  datatypes: Array<VariableDatatypeDTO> = [];

  reset() {
    this.validatorRefDataTemplate.reset();
    this.experiment = null;
    this.variables = [];
  }

  show() {
    this.modalRef.show();
  }

  validateTemplate() {
    return this.validatorRefDataTemplate.validate();
  }

  loadDatatypes() {
    if (this.datatypes.length == 0) {
      this.service
        .getDatatypes()
        .then(
          (
            http: HttpResponse<OpenSilexResponse<Array<VariableDatatypeDTO>>>
          ) => {
            this.datatypes = http.response.result;
          }
        );
    }
  }

  loadDataType(dataTypeUri: string) {
    if (!dataTypeUri) {
      return undefined;
    }
    let dataType = this.datatypes.find(
      (datatypeNode) => datatypeNode.uri == dataTypeUri
    );

    return [dataType.name];
  }

  private langUnwatcher;
  mounted() {
    this.loadDatatypes();

    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      () => {
        this.loadDatatypes();
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  csvExportDataExample() {

    let line1 = [
      this.$t("DataHelp.objectId").toString(),
      "Date",
      "demo:variable#variable.air_temperature"
    ];
    
    let line2 = [
      this.$t("DataHelp.objectId-help").toString(),
      this.$t("DataHelp.date-help").toString(),
      "Air_Temperature"
    ];

    let line3 = [
      this.$t("DataHelp.column-type-help").toString() +
        this.getDataTypeLabel("xsd:string") +
        "\n" +
        this.$t("DataHelp.required").toString(),
      this.$t("DataHelp.column-type-help").toString() +
        this.getDataTypeLabel("xsd:date") +
        "\n" +
        this.$t("DataHelp.required").toString(),
      this.$t("DataHelp.column-type-help").toString() +
        this.getDataTypeLabel("xsd:integer"),
    ];

    let line4 = ["test", "2020-09-21T00:00:00+0100", "30"];

    if (this.withRawData) {
      line1.push("raw_data");
      line2.push(this.$t("DataTemplateForm.raw-data"));
      line3.push(this.$t("DataHelp.column-type-help") +
        this.$t("DataTemplateForm.type-list") +
        this.getDataTypeLabel("xsd:integer"));
      line4.push("30,31,29");
    }

    line1.push("demo:variable#variable.fruit_color/2");
    line2.push("Fruit_Color");
    line3.push(this.$t("DataHelp.column-type-help") +
          this.getDataTypeLabel("xsd:string"),);
    line4.push("Red");

    if (this.withRawData) {
      line1.push("raw_data");
      line2.push(this.$t("DataTemplateForm.raw-data"));
      line3.push(this.$t("DataHelp.column-type-help") +
        this.$t("DataTemplateForm.type-list") +
        this.getDataTypeLabel("xsd:string"));
      line4.push('Red,Red,Red');
    }

    line1.push("demo:variable#variable.veraison_date");
    line2.push("Veraison_Date");
    line3.push(this.$t("DataHelp.column-type-help") +
          this.getDataTypeLabel("xsd:date"));
    line4.push("2020-09-21");

    if (this.withRawData) {
      line1.push("raw_data");
      line2.push(this.$t("DataTemplateForm.raw-data"));
      line3.push(this.$t("DataHelp.column-type-help") +
        this.$t("DataTemplateForm.type-list") +
        this.getDataTypeLabel("xsd:date"));
      line4.push("2020-09-21,2020-09-21,2020-09-21");
    }

    let arrData = [line1, line2, line3, line4];
    this.$papa.download(
      this.$papa.unparse(arrData, { delimiter: this.separator }),
      "dataTemplateExample"
    );
  }

  csvExport() {
    let arrData;
    this.validateTemplate().then((isValid) => {
      // fill in large
      if (isValid) {
        let variableUriInfo = [this.$t("DataHelp.objectId").toString(), "Date"];
        let otherHeaders = [
          this.$t("DataHelp.objectId-help").toString(),
          this.$t("DataHelp.date-help").toString(),
        ];
        let otherExample = [
          this.$t("DataHelp.column-type-help").toString() +
            this.getDataTypeLabel("xsd:string") +
            "\n" +
            this.$t("DataHelp.required").toString(),
          this.$t("DataHelp.column-type-help").toString() +
            this.getDataTypeLabel("xsd:date") +
            "\n" +
            this.$t("DataHelp.required").toString(),
        ];

        this.service.getVariablesByURIs(this.variables).then((http) => {
          for (let element of http.response.result) {
            variableUriInfo.push(element.uri);
            otherHeaders.push(element.name);
            if (element.datatype === undefined || element.datatype === null) {
              element.datatype = "xsd:string";
            } 
            otherExample.push(
              this.$t("DataHelp.column-type-help").toString() +
              this.getDataTypeLabel(element.datatype)
            );

            if (this.withRawData) {
              variableUriInfo.push("raw_data");
              otherHeaders.push(this.$t("DataTemplateForm.raw-data-example"));
              otherExample.push(
                this.$t("DataHelp.column-type-help").toString() +
                this.$t("DataTemplateForm.type-list") +
                this.getDataTypeLabel(element.datatype));
            }
          }
          arrData = [variableUriInfo, otherHeaders, otherExample];
          this.$papa.download(
            this.$papa.unparse(arrData, { delimiter: this.separator }),
            "datasetTemplate"
          );
        });
      }
    });
  }

  loadMethods() {
    let service = this.$opensilex.getService("opensilex.VariablesService");
    return service
      .searchMethods(undefined, undefined, 100, 0)
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<any>>>) =>
          http.response.result
      );
  }

  dtoToSelectNode(dto) {
    return {
      id: dto.uri,
      label: dto.name,
    };
  }
  getDataTypeLabel(dataTypeUri: string): string {
    if (!dataTypeUri) {
      return undefined;
    }
    let label = this.$t(this.$opensilex.getDatatype(dataTypeUri).label_key);
    return label.charAt(0).toUpperCase() + label.slice(1);
  }
}
</script>
<i18n>
en :
  DataTemplateForm:
    with-raw-data: "With raw data columns"
    raw-data: "Raw data"
    type-list: "Array of "
    raw-data-example: "Raw data (e.g. 20.3,20.4,20.5)"
    example :
      column-data-type : "Column data type: "
fr :
  DataTemplateForm:
    with-raw-data: "Avec colonnes 'raw data' (données brutes)"
    raw-data: "Données brutes"
    type-list: "Liste de "
    raw-data-example: "Données brutes (ex : 20.3,20.4,20.5)"
    example :
      column-data-type : "Type de données colonne : " 
 </i18n>