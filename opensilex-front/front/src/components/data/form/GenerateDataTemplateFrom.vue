<template>
  <b-modal
    ref="modalRef"
    size="lg"
    ok-only
    :static="true"
    @hide="requiredField = false"
    @show="requiredField = true"
  >
    <template v-slot:modal-ok>{{ $t("component.common.ok") }}</template>
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
          </b-col>
          <b-col> 
            <opensilex-SelectForm
              label="component.common.csv-delimiters.label"
              helpMessage="component.common.csv-delimiters.placeholder"
              :selected.sync="separator"
              :options="delimiterOptions" 
              :required="true"
              ></opensilex-SelectForm>
            </b-col>
        </b-row>
        <b-button @click="csvExport" variant="outline-primary">{{
          $t("OntologyCsvImporter.downloadTemplate")
        }}</b-button>
        <b-button
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
import {
  VariablesService,
  VariableDatatypeDTO
} from "opensilex-core/index"; 
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
  delimiterOptions=[{
    id :",",
    label: this.$t('component.common.csv-delimiters.comma')
  },{
    id :";",
    label: this.$t('component.common.csv-delimiters.semicolon')
  }]

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
    if(this.$store.getters.language == "fr"){
      this.separator = ';'
    }
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      () => this.loadDatatypes() ,
      () => {
        if(this.$store.getters.language == "fr"){
          this.separator = ';'
        }
      }
    );
  }
  
  beforeDestroy() {
    this.langUnwatcher();
  }

  csvExportDataExample() {
    let arrData = [
      [
        this.$t("DataHelp.objectId").toString(),
        "Date",
        "demo:variable#variable.air_temperature",
        "demo:variable#variable.fruit_color/2",
        "demo:variable#variable.veraison_date",
      ],
      [
        this.$t("DataHelp.objectId-help").toString(),
        this.$t("DataHelp.date-help").toString(),
        "Air_Temperature",
        "Fruit_Color",
        "Veraison_Date",
      ],
      [
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
        this.$t("DataHelp.column-type-help").toString() +
          this.getDataTypeLabel("xsd:string"),
        this.$t("DataHelp.column-type-help").toString() +
          this.getDataTypeLabel("xsd:date"),
      ],
      ["test", "2020-09-21T00:00:00+0100", "30", "Red", "2020-09-21"],
    ];
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
          let varsList = [];
          for (let element of http.response.result) {
            variableUriInfo.push(element.uri);
            otherHeaders.push(element.name);
            if (element.datatype != undefined && element.datatype != null) {
              otherExample.push(
                this.$t("DataHelp.column-type-help").toString() +
                  this.getDataTypeLabel(element.datatype)
              );
            } else {
              otherExample.push(this.getDataTypeLabel("xsd:string"));
            }
          }
          arrData = [variableUriInfo, otherHeaders, otherExample];
          this.$papa.download(this.$papa.unparse(arrData , { delimiter: this.separator }), "datasetTemplate");
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
    example :
      column-data-type : "Column data type: "
fr :
  DataTemplateForm:
    example :
      column-data-type : "Type de données colonne : " 
 </i18n>