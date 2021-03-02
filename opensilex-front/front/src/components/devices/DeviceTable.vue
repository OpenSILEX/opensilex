<template>
  <div class="card"> 
    <b-modal ref="colModal" :title="$t('DeviceTable.addColumn')" size="sm" hide-footer>
      <b-form-checkbox id="removalColSw" v-model="displayRemovalCol" switch>{{$t('DeviceTable.removal')}}</b-form-checkbox>
      <b-form-checkbox id="personColSw" v-model="displayPersonCol" switch>{{$t('DeviceTable.person_in_charge')}}</b-form-checkbox>
      <b-form-input v-model="colName" placeholder="Enter column name"></b-form-input>
      <b-button class="mt-3" variant="primary" block @click="addColumn">{{$t('DeviceTable.addColumn')}}</b-button>
    </b-modal>

    <div>
      <b-button pill v-b-toggle.collapse-1 variant="outline-secondary">{{$t('DeviceTable.help')}}</b-button>
      <b-collapse id="collapse-1" class="mb-2mt-2">
        <b-alert show>
          <div>{{$t('DeviceTable.infoVariable')}}</div>
          <div>{{$t('DeviceTable.infoAttributes')}}</div>
          <div>{{$t('DeviceTable.infoDateFormat')}}</div>
          <div>{{$t('DeviceTable.infoMandatoryFields')}}</div>
        </b-alert>
      </b-collapse>
    </div>

    <b-input-group class="mt-3 mb-3" size="sm">
      <downloadCsv
        ref="downloadCsv"
        class="btn btn-outline-primary mb-2 mr-2"
        :data="jsonForTemplate"
        name="template.csv">
        {{$t('DeviceTable.downloadTemplate')}}
      </downloadCsv>
      <opensilex-CSVInputFile v-on:updated="uploaded" delimiterOption=",">         
      </opensilex-CSVInputFile>
      <b-button class="mb-2 mr-2" @click="updateColumns" variant="outline-secondary">{{$t('DeviceTable.resetTable')}}</b-button>
      <b-button class="mb-2 mr-2" @click="addRow" variant="outline-dark">{{$t('DeviceTable.addRow')}}</b-button>
      <b-button class="mb-2 mr-2" @click="showColumnModal" variant="outline-dark">{{$t('DeviceTable.addColumn')}}</b-button>
      <b-button class="mb-2 mr-2" @click="addVariableCol" variant="outline-dark" v-bind:disabled="!measure">{{$t('DeviceTable.addVarColumn')}} </b-button>
      <b-form-select v-if="this.checkedLines>0"
        id="filter"
        v-model="filter"
        :options="checkBoxOptions"
        name="filter"
        size="sm"
        @input="filterLines()"
        
      ></b-form-select> 
    </b-input-group>    

    <b-input-group class="mt-3 mb-3" size="sm">
      <b-button class="mb-2 mr-2" @click="checkData()" variant="primary" v-bind:disabled="disableCheck">{{$t("DeviceTable.check")}}</b-button>
      <b-button class="mb-2 mr-2 " @click="insertData()" variant="success" v-bind:disabled="disableInsert">{{$t('DeviceTable.insert')}}</b-button>
    </b-input-group>

    <div ref="table"></div>

    <b-modal
      id="progressModal"
      size="lg"
      :no-close-on-backdrop="true"
      :no-close-on-esc="true"
      hide-header
      @shown="insertOrCheckData()"
    >
      <b-alert ref="progressAlert" variant="light" show>
        {{this.max}} {{$t('DeviceTable.progressTitle')}}
        <b-progress :max="max" show-progress animated>
          <b-progress-bar :value="progressValue" :max="max" variant="info">
            <strong>Progress: {{ progressValue }} / {{ max }}</strong>
          </b-progress-bar>
        </b-progress>
      </b-alert>
      <b-alert variant="primary" :show="infoMessage">{{this.summary}}</b-alert>
      <b-alert variant="danger" :show="alertEmptyTable">{{$t('DeviceTable.emptyMessage')}}</b-alert>
      <b-button
          v-if="onlyChecking && !alertEmptyTable"
          class="mb-2 mr-2"
          @click="insertDataFromModal()"
          variant="success"
        >{{$t('DeviceTable.insert')}}</b-button>
      <template v-slot:modal-footer>
        <b-button
          v-bind:disabled="disableCloseButton"
          class="mb-2 mr-2"
          @click="$bvModal.hide('progressModal')"
          variant="primary"
        >{{$t('DeviceTable.close')}}</b-button>
      </template>
    </b-modal>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch, Ref } from "vue-property-decorator";
import { DeviceCreationDTO, DevicesService, OntologyService, ResourceTreeDTO, RDFClassDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import JsonCSV from "vue-json-csv";
Vue.component("downloadCsv", JsonCSV);
import Tabulator from 'tabulator-tables';
import moment from 'moment';
window.moment = moment;

@Component
export default class DeviceTable extends Vue {
  $opensilex: any;
  $store: any;
  $router: any;
  $t: any;
  $i18n: any;
  service: DevicesService;
  onlyChecking: boolean;

  //add Metadata
  colName: string;
  suppColumnsNames: Array<string>;

  // Optionnal column display
  displayRemovalCol: boolean = false;
  displayPersonCol: boolean = false;

  // Variable
  nbVariableCol: number = 0;
  measure: boolean = false;
  

  // Progress Modal
  errorNumber: number = 0;
  okNumber: number = 0;
  emptyLines: number = 0;
  summary: string = "";
  progressValue = 0;
  max = 0;
  modalTitle: string = "Scanning lines";
  infoMessage: boolean = false;
  alertEmptyTable: boolean = false;
  disableCloseButton: boolean = true;

  deviceTypes: any = [];
  typeProperty: any = [];
  service_onto: OntologyService;

  @Ref("progressModal") readonly progressModal!: any;
  @Ref("progressBar") readonly progressBar!: any;
  @Ref("colModal") readonly colModal!: any;
  @Ref("table") readonly table!: any;

  props = [
    {
      deviceType: {
        type: String,
        default: "vocabulary:SensingDevice"
      }
    }
  ];

  tabulator = null;
  
  tableData = [];
   @Ref("helpModal") readonly helpModal!: any;

  tableColumns = [];

  insertionStatus = [];

  jsonForTemplate = [];

  checkBoxOptions = [
          { text: 'See all lines', value: 'all' },
          { text: 'See lines with errors', value: 'NOK' }
        ];

  filter = "all";

  disableInsert: boolean = false;
  disableCheck: boolean = false;

  checkedLines: number = 0;

  private langUnwatcher;
  mounted() {
      this.langUnwatcher = this.$store.watch(
        () => this.$store.getters.language,
        lang => {       
          this.updateColumns();
        }
      );
      this.updateColumns();
  }
  
  beforeDestroy() {
    this.langUnwatcher();
  }

  @Watch("tableData", { deep: true })
  newData(value: string, oldValue: string) {
    this.tabulator.replaceData(value);
  }

  isSubClassOf(parent){
    let ontoService: OntologyService = this.$opensilex.getService(
      "opensilex.OntologyService"
    );

    return new Promise((resolve,reject) => {ontoService
      .getClass(this.$attrs.deviceType,parent)
      .then((http: HttpResponse<OpenSilexResponse<RDFClassDTO>>) => {
        this.measure = true;
        resolve(this.measure);
      })
      .catch((error) => {
        this.measure = false;
        resolve(this.measure);
      })});
  }
  

  buildFinalTypeList(){
    this.deviceTypes = [];
    let ontoService: OntologyService = this.$opensilex.getService(
      "opensilex.OntologyService"
    );

    ontoService
      .getSubClassesOf(this.$attrs.deviceType, true)
      .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
        for (let i = 0; i < http.response.result.length; i++) {
            let resourceDTO = http.response.result[i];
            this.deviceTypes.push({
                value: resourceDTO.uri,
                label: resourceDTO.name
            });
        }
      })
      .catch(this.$opensilex.errorHandler);
  }

  async updateColumns() {
    this.measure = false;
    let measureType = ['SensingDevice','Actuator','SoftSensor'];
    let idx = 0;
    let ontoService: OntologyService = this.$opensilex.getService(
        "opensilex.OntologyService"
      );
    while (!this.measure && idx < measureType.length){
      this.measure = this.$attrs.deviceType.endsWith(measureType[idx]);
      if(!this.measure){
        await this.isSubClassOf('vocabulary:'+measureType[idx]);
      }
      
      idx++;
    }

    this.suppColumnsNames = [];
    this.colName = null;
    this.displayRemovalCol = true;
    this.displayPersonCol = true;
    this.nbVariableCol = 0;

    
    this.buildFinalTypeList();

    let idCol = {title:"", field:"rowNumber",visible:true,formatter:"rownum"};

    let statusCol ={title:"status", field:"status",visible:false};

    let uriCol = {title:"URI", field:"uri", visible:true, editor:true, minWidth:150, validator: "unique"};
    let typeCol = {title:"Type", field:"rdf_type", visible:true, editor:"autocomplete", editorParams:{values: this.deviceTypes},minWidth:150};
    let labelCol = 
      {title:this.$t('DeviceTable.name') + '<span class="required">*</span>', field:"name", visible:true, editor:true, minWidth:150, 
        validator: "unique"
      };
    let brandCol = {title:this.$t('DeviceTable.brand'), field:"brand", visible:true, editor:true};
    let constructor_modelCol = {title:this.$t('DeviceTable.constructor_model'), field:"constructor_model", visible:true, editor:true};
    let serial_numberCol = {title:this.$t('DeviceTable.serial_number'), field:"serial_number", visible:true, editor:true}
    let person_in_chargeCol =  {title:this.$t('DeviceTable.person_in_charge'), field:"person_in_charge", visible:this.displayPersonCol, editor:true};
    let start_upCol =  {title:this.$t('DeviceTable.start_up'), field:"start_up", visible:true, editor:true};
    let removalCol =  {title:this.$t('DeviceTable.removal'), field:"removal", visible:this.displayRemovalCol, editor:true};
    let commentCol =  {title:this.$t('DeviceTable.comment'), field:"comment", visible:true, editor:true};
    let checkingStatusCol = {title:this.$t('DeviceTable.checkingStatus'), field:"checkingStatus", visible:false, editor:false};
    let insertionStatusCol ={title:this.$t('DeviceTable.insertionStatus'), field:"insertionStatus", visible:false, editor:false};

    if(this.measure){
      this.nbVariableCol = 1;
      let variableCol = {title:this.$t('DeviceTable.variable')+'_1', field:"variable_1", visible:true, editor:true};
      this.tableColumns = [idCol, statusCol, uriCol, typeCol, labelCol, brandCol, constructor_modelCol,  serial_numberCol, person_in_chargeCol, start_upCol,removalCol, commentCol, variableCol, checkingStatusCol, insertionStatusCol]
    }else{
      this.tableColumns = [idCol, statusCol, uriCol, typeCol, labelCol, brandCol, constructor_modelCol,  serial_numberCol, person_in_chargeCol, start_upCol,removalCol, commentCol, checkingStatusCol, insertionStatusCol]
    }

    this.tableData = [];
    this.addInitialXRows(5);

    this.tabulator = new Tabulator(this.table, {
      data: this.tableData, //link data to table
      reactiveData: true, //enable data reactivity
      columns: this.tableColumns, //define table columns
      layout: "fitData",
      layoutColumnsOnNewData: true,
      index: "rowNumber",
      rowFormatter: function(row) {
        let r = row.getData().status;
        if (row.getData().status == "OK") {
          row.getElement().style.backgroundColor = "#a5e051";
        } else if (row.getData().status == "NOK") {
          row.getElement().style.backgroundColor = "#ed6661";
        }
      }
    });

    this.jsonForTemplate = [];
    let jsonHeader = {};
    for (var i = 1; i < this.tableColumns.length; i++) {
      if (this.tableColumns[i].visible == true) {
        jsonHeader[this.tableColumns[i].field] = null;
      }
    }
    this.jsonForTemplate.push(jsonHeader);
    this.$attrs.downloadCsv;
    this.checkedLines = 0;
    this.disableCheck = false;
    this.disableInsert = false;

    this.displayRemovalCol = false;
    this.tabulator.hideColumn("removal");
    this.displayPersonCol = false;
    this.tabulator.hideColumn("person_in_charge");
    
  }
  
  enableCheckInsert(){
    this.disableCheck = false;
    this.disableInsert = false;
  }

  addInitialXRows(X) {
    for (let i = 1; i < X + 1; i++) {
      this.tableData.push({ rowNumber: i });
    }
  }

  showColumnModal() {
    this.colModal.show();
  }

  addColumn() {
    this.colModal.hide();
    if(this.colName != null && this.colName !=  ""){
      this.tabulator.addColumn(
        { title: this.colName, field: this.colName, editor: true },
        false,
        "comment"
      );
      this.suppColumnsNames.push(this.colName);
      this.jsonForTemplate[0][this.colName] = null;
      this.$attrs.downloadCsv;
    }

    if(this.displayRemovalCol){
      this.tabulator.showColumn("removal");
      this.displayRemovalCol = true;
    }else{
      this.tabulator.hideColumn("removal");
      this.displayRemovalCol = false;
    }
    if(this.displayPersonCol){
      this.tabulator.showColumn("person_in_charge");
      this.displayPersonCol = true;
    }else{
      this.tabulator.hideColumn("person_in_charge");
      this.displayPersonCol = false;
    }
    this.colName = null;
  }

  addVariableCol(){
    this.nbVariableCol = this.nbVariableCol + 1;
    let varColName = "variable_" + this.nbVariableCol
    this.tabulator.addColumn(
              { title:this.$t('DeviceTable.variable')+'_'+this.nbVariableCol, 
                field: varColName, 
                visible:true, 
                editor:true
              },
              false,"variable_" + (this.nbVariableCol-1)
              );
    this.jsonForTemplate[0][varColName] = null;
    this.$attrs.downloadCsv;
  }

  addRow() {
    let size = this.tabulator.getData().length;
    this.tabulator.addRow({ rowNumber: size + 1});
  }

  resetModal() {
    this.max = 0;
    this.progressValue = 0;
    this.errorNumber = 0;
    this.okNumber = 0;
    this.emptyLines = 0;
    this.infoMessage = false;
    this.alertEmptyTable = false;
  }

  showModal() {
    this.resetModal();
    this.max = this.tabulator.getData().length;
    this.modalTitle = this.max + " lines to scan";
    this.$bvModal.show("progressModal");
  }

   insertDataFromModal() {
    this.$bvModal.hide('progressModal');
    this.insertData();
  }

  checkData() {
    this.onlyChecking = true;
    this.showModal();
    this.tabulator.showColumn("checkingStatus");
    this.tabulator.hideColumn("insertionStatus");    
    this.disableInsert = false;
    this.checkedLines++;
    this.disableInsert = false;
  }

  insertData() {
    this.onlyChecking = false;
    this.showModal();
    this.tabulator.hideColumn("checkingStatus");
    this.tabulator.showColumn("insertionStatus");   
    this.disableInsert = false;
    this.disableCheck = false;
  }

  insertOrCheckData() {
    this.disableCloseButton = true;
    let dataToInsert = this.tabulator.getData();

    let promises = [];
    this.$opensilex.disableLoader();

    for (let idx = 0; idx < dataToInsert.length; idx++) {
      let form: DeviceCreationDTO = {
        rdf_type: null,
        name: null,
        uri: null,
        brand: null,
        constructor_model: null,
        serial_number: null,
        person_in_charge: null,
        start_up: null,
        removal: null,
        description:null,
        relations: [],
        metadata: {}
      };

      if (dataToInsert[idx].rdf_type != null && dataToInsert[idx].rdf_type != ""){
        form.rdf_type = dataToInsert[idx].rdf_type;
      }else{
        form.rdf_type = this.$attrs.deviceType;
      }
      if (dataToInsert[idx].uri != null && dataToInsert[idx].uri != "") {
        form.uri = dataToInsert[idx].uri;
      }
      if (dataToInsert[idx].name != null && dataToInsert[idx].name != "") {
        form.name = dataToInsert[idx].name;
      }
      if (
        dataToInsert[idx].brand != null &&
        dataToInsert[idx].brand != ""
      ) {
        form.brand = dataToInsert[idx].brand;
      }
      if (
        dataToInsert[idx].constructor_model != null &&
        dataToInsert[idx].constructor_model != ""
      ) {
        form.constructor_model = dataToInsert[idx].constructor_model;
      }
      if (
        dataToInsert[idx].serial_number != null &&
        dataToInsert[idx].serial_number != ""
      ) {
        form.serial_number = dataToInsert[idx].serial_number;
      }
      if (
        this.displayPersonCol &&
        dataToInsert[idx].person_in_charge != null &&
        dataToInsert[idx].person_in_charge != ""
      ) {
        form.person_in_charge = dataToInsert[idx].person_in_charge;
      }
      if (
        dataToInsert[idx].start_up != null &&
        dataToInsert[idx].start_up != ""
      ) {
        let startDate = moment(dataToInsert[idx].start_up, ["YYYY-MM-DD","DD-MM-YYYY","DD/MM/YYYY"]);
        form.start_up = startDate.format("YYYY-MM-DD");
      }

      if (
        this.displayRemovalCol &&
        dataToInsert[idx].removal != null &&
        dataToInsert[idx].removal != ""
      ) {
        let endDate = moment(dataToInsert[idx].removal, ["YYYY-MM-DD","DD-MM-YYYY","DD/MM/YYYY"]);
        form.removal = endDate.format("YYYY-MM-DD");
      }

      if (
        dataToInsert[idx].comment != null &&
        dataToInsert[idx].comment != ""
      ) {
        form.description = dataToInsert[idx].comment;
      }

      if (this.suppColumnsNames.length > 0) {
        let attributes = {};
        for (let y = 0; y < this.suppColumnsNames.length; y++) {
          let key = this.suppColumnsNames[y];
          if (dataToInsert[idx][key] != null && dataToInsert[idx][key] != "") {
            attributes[key] = dataToInsert[idx][key];
          }
        }

        if (Object.keys(attributes).length !== 0) {
          form.metadata = attributes;
        }
      }

      if (
        this.measure &&
        this.nbVariableCol > 0
      ){
        for(let i = 1; i <= this.nbVariableCol; i++ ){
          if(
            dataToInsert[idx]['variable_'+i] != null &&
            dataToInsert[idx]['variable_'+i] != ""
          ) {
              form.relations.push({"property":"vocabulary:measures","value":dataToInsert[idx]['variable_'+i]})
          }
        }
      }

      if (
        form.name == null &&
        form.uri == null &&
        form.brand == null &&
        form.constructor_model == null &&
        form.serial_number == null &&
        form.person_in_charge == null &&
        form.start_up == null &&
        form.removal == null &&
        form.description == null
      ) {
        this.emptyLines = this.emptyLines + 1;
        this.progressValue = this.progressValue + 1;
      } else {
        promises.push(
          this.callCreateDeviceService(form, idx + 1, this.onlyChecking)
        );
      }
    }

    Promise.all(promises).then(result => {
      if (this.onlyChecking) {
        this.summary = this.okNumber + " " + this.$t('DeviceTable.infoMessageDevReady') + ", " + this.errorNumber + " " + this.$t('DeviceTable.infoMessageErrors') + ", " + this.emptyLines + " " + this.$t('DeviceTable.infoMessageEmptyLines');
      } else {
        this.summary = this.okNumber + " " + this.$t('DeviceTable.infoMessageDevInserted') +", " + this.errorNumber + " " + this.$t('DeviceTable.infoMessageErrors') + ", " + this.emptyLines + " " + this.$t('DeviceTable.infoMessageEmptyLines');
      }
      this.infoMessage = true;
      this.disableCloseButton = false;
      this.$opensilex.enableLoader();
    });

    if (this.emptyLines == this.max) {
      this.alertEmptyTable = true;
      this.disableCloseButton = false;
      this.$opensilex.enableLoader();
    }
  }

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.DevicesService");
  }

  callCreateDeviceService(
    form: DeviceCreationDTO,
    index: number,
    onlyChecking: boolean
  ) {
    return new Promise((resolve, reject) => {
    this.service
    .createDevice(onlyChecking,form)
    .then((http: HttpResponse<OpenSilexResponse<any>>) => {
      if(onlyChecking) {
        this.tabulator.updateData([{rowNumber:index, checkingStatus:this.$t('DeviceTable.checkingStatusMessage'), status:"OK"}])
      } else {
        this.tabulator.updateData([{rowNumber:index, insertionStatus:this.$t('DeviceTable.insertionStatusMessage'), status:"OK", uri:http.response.result}])
      }
      
      let row = this.tabulator.getRow(index);
      row.reformat();
      this.okNumber = this.okNumber + 1;
      this.progressValue = this.progressValue + 1;
      
      resolve();
      
    }).catch(error => {
        let errorMessage: string;
        let failure = true;
        try {
          errorMessage = error.response.result.message;
          failure = false;
        } catch(e1) {
          failure = true;
        }

        if (failure) {
            try {
              errorMessage =
                error.response.metadata.status[0].exception.details;
            } catch (e2) {
              errorMessage = "uncatched error";
            }
          }
          
        if (onlyChecking) {
          this.tabulator.updateData([
            { rowNumber: index, checkingStatus: errorMessage, status: "NOK" }
          ]);
        } else {
          this.tabulator.updateData([
            { rowNumber: index, insertionStatus: errorMessage, status: "NOK" }
          ]);
        }

        let row = this.tabulator.getRow(index);
        row.reformat();
        this.errorNumber = this.errorNumber + 1;
        this.progressValue = this.progressValue + 1;
        this.disableInsert = false;
        this.disableCheck = false;
        resolve();
      });
    });
  }

  uploaded(data) {
    if (data.length > 1000) {
      alert(this.$t('DeviceTable.alertFileSize'));
    } else {
      var uniqueNames = [];
      var uniqueURIs = [];
      let insertionOK = true;
      for (let idx = 0; idx < data.length; idx++) {
        data[idx]["rowNumber"] = idx + 1;
        if(data[idx].name !== "" && uniqueNames.indexOf(data[idx].name) === -1){
          uniqueNames.push(data[idx].name);        
        } else {
          insertionOK = false
          alert(this.$t('DeviceTable.alertDuplicateName') + " " + data[idx]["rowNumber"] + ", name= " + data[idx].name);
          break
        }
        if(data[idx].uri !== "") {
            if (uniqueURIs.indexOf(data[idx].uri) === -1){
              uniqueURIs.push(data[idx].uri);        
            } else {
              insertionOK = false
              alert(this.$t('DeviceTable.alertDuplicateURI') + " " + data[idx]["rowNumber"] + ", uri= " + data[idx].uri);
              break
            }
        } 
      }
      let header = data[0];
      for(let idy = 0; idy < header.length; idy++){
        if(header[idy].startsWith('variable_')){
          this.nbVariableCol++;
        }
      }
      if (insertionOK) {
        this.tabulator.setData(data);
        this.displayRemovalCol = true;
        this.displayPersonCol = true;
      }
    }
        
  }

  filterLines() {
    if (this.filter != "all") {
      this.tabulator.setFilter("status", "=", this.filter);
    } else {
      this.tabulator.clearFilter();
    }
    
  }

}
</script>

<style scoped lang="scss">

.requiredOnCondition {
  color:blue
}

.tabulator {
  font-size: 13px;
}

.tabulator-row {
  min-height: 0rem;
  height: 30px;
}

.tabulator .tabulator-header .tabulator-col .tabulator-col-content {
  height: 30px;
}

.tabulator .tabulator-header .tabulator-col[tabulator-field="name"] {
  color:#f00;
}

.tabulator .tabulator-header .tabulator-col {
  border-right: 1px solid #dee2e6;
}

.tabulator .tabulator-table .tabulator-row .tabulator-cell {
  border-right:1px solid #dee2e6;
  height: 30px;
}
  
</style>

<i18n>

en:
  DeviceTable:
    name: Name
    uri: URI
    rdfType: Type
    brand: Brand
    constructor_model: Constructor model
    serial_number: Serial number
    person_in_charge: Person in charge
    start_up: Start-up date
    removal: Removal date
    comment: Description
    variable: Variable
    checkingStatus: Checking status
    insertionStatus: Insertion Status
    downloadTemplate : Download template
    resetTable : Reset table
    check : Check
    insert : Insert
    progressTitle: lines to scan
    emptyMessage: The table is empty
    close: Close
    addRow: Add Row
    addColumn: Add column
    addVarColumn: Add variable column
    help: Help
    infoMessageDevReady: device ready to be inserted
    infoMessageErrors: errors
    infoMessageEmptyLines: empty lines
    infoMessageDevInserted: device inserted
    infoVariable: Only SensingDevice, SoftSensors and Actuator can have variable. To add variable use <Add column variable> button
    infoAttributes: To add informations, you can add personnal column
    checkingStatusMessage: ready
    insertionStatusMessage: created
    filterLines: Filter the lines
    infoMandatoryFields: It is mandatory to fill the name
    infoDateFormat:  date format YYYY-MM-DD,DD-MM-YYYY,DD/MM/YYYY
    alertDuplicate: The file contains a duplicate name at line
    alertDuplicateURI: The file contains a duplicate uri at line
    alertFileSize: The file has too many lines, 1000 lines maximum

fr:
  DeviceTable:
    name: Nom
    uri: URI
    rdfType: Type
    brand: Marque
    constructor_model: Modèle du dispositif
    serial_number: Numéro de série
    person_in_charge: Personne en charge
    start_up: Date de mise en service
    removal: Date de mise hors service
    comment: Description
    variable: Variable
    checkingStatus: Statut
    insertionStatus: Statut
    downloadTemplate : Télécharger un gabarit
    resetTable : Vider tableau
    check : Valider
    insert : Insérer
    progressTitle: lignes à parcourir
    emptyMessage: Le tableau est vide
    close : Fermer
    addColumn: Ajouter colonne
    addVarColumn: Ajouter colonne variable
    infoVariable: Seul les types Capteurs, Capteurs logiciel et les Actionneurs peuvent être associé a une ou plusieurs variable. Pour ajouter une variable supplémentaire utilisez le bouton <Ajouter colonne variable>
    infoAttributes: Pour ajouter des informations supplémentaires, vous pouvez ajouter des colonnes
    help: Aide
    infoMessageDevReady: device prêts à être insérer
    infoMessageErrors: erreursoeso
    infoMessageEmptyLines: lignes vides
    infoMessageDevInserted: device insérés
    checkingStatusMessage: validé
    insertionStatusMessage: créé
    seeErrorLines: See lines
    seeAll : see all 
    infoMandatoryFields: Il est obligatoire de renseigner au moins un nom
    infoDateFormat: format date YYYY-MM-DD,DD-MM-YYYY,DD/MM/YYYY 
    alertDuplicateName: Le fichier comporte un doublon de nom à la ligne
    alertDuplicateURI: Le fichier comporte un doublon d'uri à la ligne 
    alertFileSize: Le fichier contient trop de ligne, 1000 lignes maximum
</i18n>
