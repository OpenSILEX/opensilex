<template>
  <b-modal
    ref="modalRef"
    size="lg"
    ok-only
    :static="true"
    @hide="requiredField = false"
    @show="shown()"
  >
    <template v-slot:modal-title>{{ $t("DataHelp.title") }}</template>
    <div>
      <ValidationObserver ref="validatorRefDataTemplate">
        <b-row>
          <b-col>
            <b-form-group v-if="experiment==null" :label="$t('DataTemplateForm.select-columns')" v-slot="{ ariaDescribedby }">
              <b-form-checkbox-group
                v-model="selectedColumns"
                :options="options"
                :aria-describedby="ariaDescribedby"
                @change="change"
              ></b-form-checkbox-group>
            </b-form-group>

            <b-form-group v-else :label="$t('DataTemplateForm.select-columns')" v-slot="{ ariaDescribedby }">
              <b-form-checkbox-group
                v-model="selectedColumns"
                :options="expeOptions"
                :aria-describedby="ariaDescribedby"
              ></b-form-checkbox-group>
            </b-form-group>

          </b-col>
          <b-col cols="7">
            <b-alert v-if="experiment==null"
              variant="danger" 
              :show="!validSelection"
            >{{$t("DataTemplateForm.target-device-required")}}</b-alert>
          </b-col>
        </b-row>
        <b-row>
          <b-col cols="9">
            <opensilex-VariableSelectorWithFilter
              placeholder="VariableSelectorWithFilter.placeholder-multiple"
              :variables.sync="variables"
            ></opensilex-VariableSelectorWithFilter>

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
      </ValidationObserver>
    </div>

    <template v-slot:modal-footer>
      <b-button
        v-if="variables.length == 0"
        class="float-right greenThemeColor"
        @click="csvExportDataExample"
        :disabled="experiment==null && !validSelection"
      >{{ $t("DataHelp.download-template-example") }}
      </b-button>
      <b-button 
        @click="csvExport" 
        class="greenThemeColor"
        :disabled="experiment==null && !validSelection || variables.length == 0">{{$t("OntologyCsvImporter.downloadTemplate")}}
      </b-button>
      &nbsp;
      <font-awesome-icon icon="question-circle" v-b-tooltip.hover.right=" $t('DataTemplateForm.help') "/>
    </template>

  </b-modal>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import {VariableDatatypeDTO, VariableDetailsDTO, VariablesService} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import Xsd from "../../../ontologies/Xsd";

@Component
export default class GenerateDataTemplateFrom extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $store: any;
  $t: any;
  $i18n: any;
  $papa: any;
  service: VariablesService;

  requiredField: boolean = false;

  separator = ",";

  selectedColumns = [];

  withRawData = false;

  validSelection = true;

  readonly expColumn = "experiment";
  readonly targetColumn = "target";
  readonly deviceColumn = "device";
  readonly soColumn = "scientific_object";
    readonly annotationColumn = "object_annotation";

  @Prop()
  editMode;

  @Prop({ default: false })
  hasDeviceAgent;  
  
  options = [
    { text: this.expColumn, value: this.expColumn },
    { text: this.targetColumn, value: this.targetColumn },
    { text: this.deviceColumn, value: this.deviceColumn },
    { text: this.annotationColumn, value: this.annotationColumn }
  ];


  expeOptions = [
    { text: this.deviceColumn, value: this.deviceColumn },
    { text: this.annotationColumn, value: this.annotationColumn }
  ];

  @Ref("validatorRefDataTemplate") readonly validatorRefDataTemplate!: any;

  @Ref("modalRef") readonly modalRef!: any;

  get user() {
    return this.$store.state.user;
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.VariablesService");
  }

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
    return this.validatorRefDataTemplate.validate({ silent: true });
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

    let line1 = [];
    let line2 = [];
    let line3 = [];
    let line4 = [];

    //column experiment
    if (this.selectedColumns.includes(this.expColumn)) {
      line1.push(this.expColumn);
      line2.push("DataHelp.experiment-help");
      line3.push(this.$t("DataHelp.column-type-help")+
        this.getDataTypeLabel("xsd:string"));
      line4.push("exp-test");
    }

    //column object
    if (this.experiment != null) {
      line1.push(this.soColumn);
      line2.push(this.$t("DataHelp.objectId-help")); 
      line3.push(
        this.$t("DataHelp.column-type-help")+
        this.getDataTypeLabel("xsd:string")
      );
      line4.push("test");
    } else {
      if (this.selectedColumns.includes(this.targetColumn)) {
        line1.push(this.targetColumn);
        line2.push(this.$t("DataHelp.targetId-help")); 
        line3.push(
          this.$t("DataHelp.column-type-help")+
          this.getDataTypeLabel("xsd:string")
        );
        line4.push("test");
      }
    }    

    //column device
    if (this.selectedColumns.includes(this.deviceColumn)) {      
      line1.push(this.deviceColumn);
      line2.push(this.$t("DataHelp.device-help"));
      line3.push(this.$t("DataHelp.column-type-help")+
        this.getDataTypeLabel("xsd:string"));
      line4.push("device-test");
    }

    //columns date & first variable
    line1.push("Date", "demo:variable#variable.air_temperature");
    line2.push(this.$t("DataHelp.date-help"),
      "Air_Temperature");
    line3.push(this.$t("DataHelp.column-type-help")+
      this.getDataTypeLabel("xsd:date") +
      "\n" + this.$t("DataHelp.required"),
      this.$t("DataHelp.column-type-help") +
      this.getDataTypeLabel("xsd:integer"));
    line4.push("2020-09-21T00:00:00+0100", "30");

    //column rawData for first variable
    if (this.withRawData) {
      line1.push("raw_data");
      line2.push(this.$t("DataTemplateForm.raw-data"));
      line3.push(this.$t("DataHelp.column-type-help") +
        this.$t("DataTemplateForm.type-list") +
        this.getDataTypeLabel("xsd:integer"));
      line4.push("30,31,29");
    }

    //column 2nd variable
    line1.push("demo:variable#variable.fruit_color/2");
    line2.push("Fruit_Color");
    line3.push(this.$t("DataHelp.column-type-help") +
          this.getDataTypeLabel("xsd:string"),);
    line4.push("Red");

    //column rawData for 2nd variable
    if (this.withRawData) {
      line1.push("raw_data");
      line2.push(this.$t("DataTemplateForm.raw-data"));
      line3.push(this.$t("DataHelp.column-type-help") +
        this.$t("DataTemplateForm.type-list") +
        this.getDataTypeLabel("xsd:string"));
      line4.push('Red,Red,Red');
    }

    //column 3rd variable
    line1.push("demo:variable#variable.veraison_date");
    line2.push("Veraison_Date");
    line3.push(this.$t("DataHelp.column-type-help") +
          this.getDataTypeLabel("xsd:date"));
    line4.push("2020-09-21");

    //column rawData for 3rd variable
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

    //column annotation on object
    if (this.selectedColumns.includes(this.annotationColumn)) {
        line1.push(this.annotationColumn);
        line2.push(this.$t("DataTemplate.annotationHelp"));
        line3.push(this.$t("DataHelp.column-type-help")+
            this.getDataTypeLabel("String"));
        line4.push("annotation-test");
    }
  }

  csvExport() {
    let arrData;
    this.validateTemplate().then((isValid) => {
      // fill in large
      if (isValid) {
        let line1 = [];
        let line2 = [];
        let line3 = [];

        //column experiment
        if (this.selectedColumns.includes(this.expColumn)) {
          line1.push(this.expColumn);
          line2.push(this.$t("DataHelp.experiment-help"));
          line3.push(this.$t("DataHelp.column-type-help") +
              this.getDataTypeLabel("xsd:string")
          );
        }
        
        //column object
        if (this.experiment != null) {
          line1.push(this.soColumn);
          line2.push(this.$t("DataHelp.objectId-help")); 
          line3.push(
            this.$t("DataHelp.column-type-help")+
            this.getDataTypeLabel("xsd:string")
          );
        } else {
          if (this.selectedColumns.includes(this.targetColumn)) {
            line1.push(this.targetColumn);
            line2.push(this.$t("DataHelp.targetId-help")); 
            line3.push(
              this.$t("DataHelp.column-type-help")+
              this.getDataTypeLabel("xsd:string")
            );
          }
        }

        //column devices (1 column per deviceType)
        if (this.selectedColumns.includes(this.deviceColumn)) {
            line1.push(this.deviceColumn);
            line2.push(this.$t("DataHelp.device-help"));
            line3.push(this.$t("DataHelp.column-type-help")+
              this.getDataTypeLabel("xsd:string"));
        }

        //columns date
        line1.push("Date");
        line2.push(this.$t("DataHelp.date-help"));
        line3.push( this.getDataTypeLabel("xsd:date") +
          "\n" + this.$t("DataHelp.required"));

        this.service.getVariablesByURIs(this.variables)
          .then((http) => {
            for (let element of http.response.result) {

              //column variable
              line1.push(element.uri);
              line2.push(element.name);
              if (element.datatype === undefined || element.datatype === null) {
                element.datatype = Xsd.STRING;
              }
              let variableHelp = this.$t("DataHelp.column-type-help").toString() +
                  this.$opensilex.getVariableDatatypeLabel(element.datatype);
              if (this.$opensilex.checkURIs(element.datatype, Xsd.DATE)) {
                variableHelp += " " + this.$t("DataTemplateForm.format-help.date");
              } else if (this.$opensilex.checkURIs(element.datatype, Xsd.DATETIME)) {
                variableHelp += " " + this.$t("DataTemplateForm.format-help.datetime");
              } else if (this.$opensilex.checkURIs(element.datatype, Xsd.BOOLEAN)) {
                variableHelp += " " + this.$t("DataTemplateForm.format-help.boolean");
              }
              line3.push(variableHelp);

              //column raw_data
              if (this.withRawData) {
                line1.push("raw_data");
                line2.push(this.$t("DataTemplateForm.raw-data"));
                line3.push(
                  this.$t("DataHelp.column-type-help").toString() +
                  this.$t("DataTemplateForm.type-list") +
                  this.$opensilex.getVariableDatatypeLabel(element.datatype));
              }
            }

            //column annotation on object
            if (this.selectedColumns.includes(this.annotationColumn)) {
                line1.push(this.annotationColumn);
                line2.push(this.$t("DataTemplate.annotationHelp"));
                line3.push(this.$t("DataHelp.column-type-help")+"String");
            }

            arrData = [line1, line2, line3];
            this.$papa.download(
              this.$papa.unparse(arrData, { delimiter: this.separator }),
              "datasetTemplate"
            );
          }).catch((error) => {
            this.$opensilex.showErrorToast(this.$i18n.t("DataTemplateForm.variable-not-found-error"));
            throw error;
          });
      }
    });
  }

  getDataTypeLabel(dataTypeUri: string): string {
    if (!dataTypeUri) {
      return undefined;
    }
    let label = this.$t(this.$opensilex.getDatatype(dataTypeUri).label_key);
    return label.charAt(0).toUpperCase() + label.slice(1);
  }

  change() {
    if (this.selectedColumns.includes(this.targetColumn) || this.selectedColumns.includes(this.deviceColumn)) {
      this.validSelection = true;
    } else {
      this.validSelection = false;
    }
  }

  shown() {
    this.validSelection = this.hasDeviceAgent;
     this.requiredField = true; // due to the impact on the required field on the Form who encapsulte this form component
    this.selectedColumns = [];
  }

}
</script>
<i18n>
en :
  DataTemplateForm:
    help: The button is disabled if no variables are selected
    with-raw-data: "With raw data columns"
    raw-data: "Raw data"
    type-list: "Array of "
    raw-data-example: "Raw data (e.g. 20.3,20.4,20.5)"
    select-columns: Select the optional columns you need
    select-variables: Select the variables you need
    target-device-required: The provenance you selected doesn't contain any device agent, so you must add the target or the device column
    example :
      column-data-type : "Column data type: "
    format-help:
      datetime: "(format: YYYY-MM-DDThh:mm:ssZ)"
      date: "(format: YYYY-MM-DD)"
      boolean: "(format: true/false)"
    variable-not-found-error: Variable not found. Please make sure you have imported it.
  DataTemplate:
    annotationHelp: "Annotation (On the target object)"
fr :
  DataTemplateForm:
    help: Le bouton est désactivé si aucune variable n'est sélectionnée
    with-raw-data: "Avec colonnes 'raw data' (données brutes)"
    raw-data: "Données brutes"
    type-list: "Liste de "
    raw-data-example: "Données brutes (ex : 20.3,20.4,20.5)"
    select-columns: Sélectionnez les colonnes optionnelles dont vous avez besoin
    select-variables: Sélectionnez les variables dont vous avez besoin
    target-device-required: "La provenance sélectionnée ne contient pas de device, vous devez donc ajouter la colonne \"target\" ou \"device\""
    example :
      column-data-type : "Type de données colonne : "
    format-help:
      datetime: "(format: AAAA-MM-JJThh:mm:ssZ)"
      date: "(format: AAAA-MM-JJ)"
      boolean: "(format: true/false)"
    variable-not-found-error: Variable non trouvée. Veuillez verifier que la variable est bien importée.
  DataTemplate:
    annotationHelp: "Annotation (Sur l'object cible)"
 </i18n>