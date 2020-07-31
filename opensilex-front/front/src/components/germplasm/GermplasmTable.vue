<template>
  <div class="card"> 
    <b-modal ref="colModal" title="Add columns" size="sm" hide-footer>
      <b-form-input v-model="colName" placeholder="Enter column name"></b-form-input>
      <b-button class="mt-3" variant="outline-primary" block @click="addColumn">add column</b-button>
    </b-modal>

    <div>
      <b-button pill v-b-toggle.collapse-1 variant="outline-secondary">{{$t('GermplasmTable.help')}}</b-button>
      <b-collapse id="collapse-1" class="mb-2mt-2">
        <b-alert show>
          <div>{{$t('GermplasmTable.infoSynonyms')}}</div>
          <div>{{$t('GermplasmTable.infoAttributes')}}</div>
        </b-alert>
      </b-collapse>
    </div>

    <b-input-group class="mt-3 mb-3" size="sm">
      <downloadCsv
        ref="downloadCsv"
        class="btn btn-outline-primary mb-2 mr-2"
        :data="jsonForTemplate"
        name="template.csv">
        {{$t('GermplasmTable.downloadTemplate')}}
      </downloadCsv>
      <opensilex-CSVInputFile v-on:updated="uploaded">         
      </opensilex-CSVInputFile>
      <b-button class="mb-2 mr-2" @click="updateColumns" variant="outline-secondary">{{$t('GermplasmTable.resetTable')}}</b-button>
      <b-button class="mb-2 mr-2" @click="addRow" variant="outline-info">{{$t('GermplasmTable.addRow')}}</b-button>
      <b-button class="mb-2 mr-2" @click="showColumnModal" variant="outline-info">{{$t('GermplasmTable.addColumn')}}</b-button>
    </b-input-group>

    <div ref="table"></div>
    <b-input-group class="mt-3 mb-3" size="sm">
      <b-button class="mb-2 mr-2" @click="checkData()" variant="primary">{{$t("GermplasmTable.check")}}</b-button>
      <b-button class="mb-2 mr-2 " @click="insertData()" variant="success">{{$t('GermplasmTable.insert')}}</b-button>
    </b-input-group>

    <b-modal id="progressModal" size="lg" :no-close-on-backdrop="true" :no-close-on-esc="true" hide-header @shown="insertOrCheckData()">
      <b-alert ref="progressAlert" variant="light" show> {{this.max}} {{$t('GermplasmTable.progressTitle')}} 
        <b-progress :max="max" show-progress animated>
          <b-progress-bar :value="progressValue" :max="max" variant="info">
            <strong> Progress: {{ progressValue }} / {{ max }}</strong>        
          </b-progress-bar>
        </b-progress>
      </b-alert>
      <b-alert variant="primary" :show="infoMessage"> {{this.summary}} </b-alert>
      <b-alert variant="danger" :show="alertEmptyTable"> {{$t('GermplasmTable.emptyMessage')}} </b-alert>
      <template v-slot:modal-footer>
        <b-button v-bind:disabled="disableCloseButton" class="mb-2 mr-2" @click="$bvModal.hide('progressModal')" variant="primary">{{$t('GermplasmTable.close')}}</b-button>
      </template>
    </b-modal>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch, Ref } from 'vue-property-decorator'
import { GermplasmCreationDTO, GermplasmService } from "opensilex-core/index"; 
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import JsonCSV from 'vue-json-csv';
Vue.component('downloadCsv', JsonCSV);
var Tabulator = require("tabulator-tables");

@Component
export default class GermplasmTable extends Vue {
  $opensilex: any;
  $store: any;
  $router: any;
  $t: any;
  $i18n: any;
  service: GermplasmService;
  onlyChecking: boolean;
  colName: string;
  suppColumnsNames: Array<string>;

  // Progress Modal
  errorNumber: number = 0;
  okNumber: number = 0;
  emptyLines: number = 0;
  summary: string = "";
  progressValue = 0
  max = 0
  modalTitle: string = "Scanning lines";
  infoMessage: boolean = false;
  alertEmptyTable: boolean = false;
  disableCloseButton: boolean = true;

  @Ref("progressModal") readonly progressModal!: any;
  @Ref("progressBar") readonly progressBar!: any;
  @Ref("colModal") readonly colModal!: any;

  props = [{
    germplasmType: {
      type: String,
      default: 'vocabulary:Species'
    }
  }]

  tabulator = null
  
  tableData = [];

  tableColumns = [];

  insertionStatus = [];

  jsonForTemplate = [];

  mounted() {
    this.$store.watch(
      () => this.$store.getters.language,
      lang => {       
        this.updateColumns();
      }
    );
  }

  @Watch('tableData',{deep: true })
  newData(value: string, oldValue: string) {
    this.tabulator.replaceData(value)
  }

  updateColumns() {
    this.suppColumnsNames = [];
    this.colName = null;

    let idCol = {title:"", field:"rowNumber",visible:true,formatter:"rownum"};

    let statusCol ={title:"status", field:"status",visible:false};

    let uriCol = {title:"URI", field:"uri", visible:true, editor:true, minWidth:150};

    let labelCol = 
      {title:this.$t('GermplasmTable.label'), field:"name", visible:true, editor:true, minWidth:150, 
        validator: "unique"
        // [{
        //   type:function(cell, value, parameters){
        //     if(value === "" || value === null || typeof value === "undefined"){
        //       return true;
        //     }
        //     var unique = true;
        //     var cellData = cell.getData();
        //     var column = cell.getColumn()._getSelf();

        //     this.table.rowManager.rows.forEach(function(row){
        //       var data = row.getData();
        //       if(data !== cellData){
        //         if  (column.getFieldValue(data) !== undefined) {
        //           if(value.toLowerCase() == column.getFieldValue(data).toLowerCase()){
        //             unique = false;
        //           }
        //         } 
        //       }
        //     });
        //     return unique;
        //   }
        
        // }] 
      };
    let synonymCol = {title:this.$t('GermplasmTable.synonyms'), field:"synonyms", visible:true, editor:true};
    let speciesCol = {title:this.$t('GermplasmTable.fromSpecies'), field:"fromSpecies", visible:true, editor:true};
    let varietyCol = {title:this.$t('GermplasmTable.fromVariety'), field:"fromVariety", visible:true, editor:true};
    let accessionCol = {title:this.$t('GermplasmTable.fromAccession'), field:"fromAccession", visible:true, editor:true}
    let instituteCol =  {title:this.$t('GermplasmTable.institute'), field:"institute", visible:true, editor:true};
    let productionYearCol =  {title:this.$t('GermplasmTable.year'), field:"productionYear", visible:true, editor:true};
    let commentCol = {title:this.$t('GermplasmTable.comment'), field:"comment", visible:true, editor:true};
    let checkingStatusCol = {title:this.$t('GermplasmTable.checkingStatus'), field:"checkingStatus", visible:false, editor:false};
    let insertionStatusCol ={title:this.$t('GermplasmTable.insertionStatus'), field:"insertionStatus", visible:false, editor:false};

    if (this.$attrs.germplasmType.endsWith('Species'))  {
      this.tableColumns = [idCol, statusCol, uriCol, labelCol, synonymCol, commentCol, checkingStatusCol, insertionStatusCol]
    } else if (this.$attrs.germplasmType.endsWith('Variety'))  {
      let codeVar = {title:this.$t('GermplasmTable.varietyCode'), field:"id", visible:true, editor:true};
      this.tableColumns = [idCol, statusCol, uriCol, labelCol, synonymCol, codeVar, instituteCol, speciesCol, commentCol, checkingStatusCol, insertionStatusCol]
    } else if (this.$attrs.germplasmType.endsWith('Accession')) {
      let codeAcc = {title:this.$t('GermplasmTable.accessionNumber'), field:"id", visible:true, editor:true};
      this.tableColumns = [idCol, statusCol, uriCol, labelCol, synonymCol, codeAcc, instituteCol, speciesCol, varietyCol,  commentCol, checkingStatusCol, insertionStatusCol]        
    } else {
      let codeLot = {title:this.$t('GermplasmTable.lotNumber'), field:"id", visible:true, editor:true};
      this.tableColumns = [idCol, statusCol, uriCol, labelCol, synonymCol, codeLot, instituteCol, speciesCol, varietyCol, accessionCol, productionYearCol, commentCol, checkingStatusCol, insertionStatusCol]  
    }
    
    this.tableData = [];
    this.addInitialXRows(5);

    this.tabulator = new Tabulator(this.$refs.table, {
      data: this.tableData, //link data to table
      reactiveData:true, //enable data reactivity
      columns: this.tableColumns, //define table columns
      layout:"fitData",
      layoutColumnsOnNewData: true,
      index:"rowNumber",
      rowFormatter:function(row){
        let r = row.getData().status;
        if (row.getData().status == "OK") {
          row.getElement().style.backgroundColor = "#a5e051";
        } else if (row.getData().status == "NOK"){
          row.getElement().style.backgroundColor = "#ed6661"; 
        }   
      },

    });

    this.jsonForTemplate = [];
    let jsonHeader = {};
    for (var i=1;i<this.tableColumns.length;i++) {
      if (this.tableColumns[i].visible == true) {
        jsonHeader[this.tableColumns[i].field] = null;
      }      
    }
    this.jsonForTemplate.push(jsonHeader)
    this.$attrs.downloadCsv;
  }

  addInitialXRows(X) {
    for (let i = 1; i < X+1; i++) {
         this.tableData.push({rowNumber:i});
    }      
  }

  showColumnModal() {
    this.colModal.show(); 
  }

  addColumn() {
    this.colModal.hide();
    this.tabulator.addColumn({title:this.colName, field:this.colName, editor:true}, false, "comment");
    this.suppColumnsNames.push(this.colName);
    this.jsonForTemplate[0][this.colName] = null;
    this.$attrs.downloadCsv;
    this.colName = null;
  }



  addRow() {
    let size = this.tabulator.getData().length;   
    this.tabulator.addRow({rowNumber:size+1});
    console.log(this.tabulator.getData().length)
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
    this.modalTitle = this.max + " lines to scan"
    this.$bvModal.show('progressModal') 
  }

  checkData() {
    this.onlyChecking = true;
    this.showModal();  
    this.tabulator.showColumn("checkingStatus");
    this.tabulator.hideColumn("insertionStatus");    
  }

  insertData() {
    this.onlyChecking = false; 
    this.showModal();
    this.tabulator.hideColumn("checkingStatus");
    this.tabulator.showColumn("insertionStatus");   

  }

  insertOrCheckData() {     
    this.disableCloseButton = true;
    let dataToInsert = this.tabulator.getData();  

    let promises = [];    
    this.$opensilex.disableLoader();

    let colDefs = this.tabulator.getColumnDefinitions();

    for (let idx = 0; idx < dataToInsert.length; idx++) {

      let form: GermplasmCreationDTO = {
        rdfType: null,
        label: null,
        uri: null,
        fromSpecies:null,
        fromVariety:null,
        fromAccession:null,
        institute:null,
        productionYear:null,
        comment:null,
        code:null,
        synonyms: [],
        attributes: null
        
      } 

      form.rdfType = this.$attrs.germplasmType;
      
      if (dataToInsert[idx].uri != null && dataToInsert[idx].uri != "") {
        form.uri = dataToInsert[idx].uri;
      }
      if (dataToInsert[idx].name != null && dataToInsert[idx].name != "") {
        form.label = dataToInsert[idx].name;
      }
      if (dataToInsert[idx].fromSpecies != null && dataToInsert[idx].fromSpecies != "") {
        form.fromSpecies = dataToInsert[idx].fromSpecies;
      }
      if (dataToInsert[idx].fromVariety != null && dataToInsert[idx].fromVariety != "") {
        form.fromVariety = dataToInsert[idx].fromVariety;
      }
      if (dataToInsert[idx].fromAccession != null && dataToInsert[idx].fromAccession != "") {
        form.fromAccession = dataToInsert[idx].fromAccession;
      }
      if (dataToInsert[idx].institute != null && dataToInsert[idx].institute != "") {
        form.institute = dataToInsert[idx].institute;
      }
      if (dataToInsert[idx].productionYear != null && dataToInsert[idx].productionYear != "") {
        form.productionYear = dataToInsert[idx].productionYear;
      }
      if (dataToInsert[idx].comment != null && dataToInsert[idx].comment != "") {
        form.comment = dataToInsert[idx].comment;
      }
      if (dataToInsert[idx].code != null && dataToInsert[idx].code != "") {
        form.code = dataToInsert[idx].code;
      }

      if (dataToInsert[idx].synonyms != null && dataToInsert[idx].synonyms != "") {
        let stringSynonyms = dataToInsert[idx].synonyms;
        form.synonyms = stringSynonyms.split("|");
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
          form.attributes = attributes;
        }
        
      }
            
      if (((form.label == null) 
          && (form.uri == null) 
          && (form.fromSpecies == null) 
          && (form.fromVariety == null) 
          && (form.fromAccession == null)
          && (form.institute == null)
          && (form.productionYear == null)
          && (form.comment == null)
          && (form.code == null)
          && (form.synonyms.length == 0)
          && (form.attributes == null))) {
        this.emptyLines = this.emptyLines + 1;
        this.progressValue = this.progressValue + 1;
        
      } else {
        promises.push(this.callCreateGermplasmService(form, idx+1, this.onlyChecking));
      } 
    }

    Promise.all(promises).then((result) => {
      if (this.onlyChecking) {
        this.summary = this.okNumber + " germplasm ready to be inserted, " + this.errorNumber + " errors, " + this.emptyLines + " empty lines"
      } else {
        this.summary = this.okNumber + " germplasm inserted, " + this.errorNumber + " errors, " + this.emptyLines + " empty lines"
      }
      this.infoMessage = true; 
      this.disableCloseButton = false;
      this.$opensilex.enableLoader();     
    }) 

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
    console.debug("Loading form view...");
    this.service = this.$opensilex.getService("opensilex.GermplasmService");
  }

  callCreateGermplasmService(form: GermplasmCreationDTO, index: number, onlyChecking: boolean) {
    
    return new Promise((resolve, reject) => {
    this.service
    .createGermplasm(onlyChecking, form)
    .then((http: HttpResponse<OpenSilexResponse<any>>) => {
      if(onlyChecking) {
        this.tabulator.updateData([{rowNumber:index, checkingStatus:"ready to be inserted", status:"OK"}])
      } else {
        this.tabulator.updateData([{rowNumber:index, insertionStatus:"germplasm created", status:"OK", uri:http.response.result}])
      }
      
      let row = this.tabulator.getRow(index);
      row.reformat();
      this.okNumber = this.okNumber + 1;
      this.progressValue = this.progressValue + 1;
      
      resolve();
      
    }).catch(error => {
        let errorMessage: string;
        let errorMessage2: string;
        let failure = true;
        try {
          errorMessage = error.response.result.message;
          failure = false;
        } catch(e1) {
          failure = true;
        }

        if (failure) {
          try {
            errorMessage = error.response.metadata.status[0].exception.details;
          } catch (e2) {
            errorMessage = "uncatched error";
          }
        }

        if (onlyChecking) {
          this.tabulator.updateData([{rowNumber:index, checkingStatus:errorMessage, status:"NOK"}])
        } else {
          this.tabulator.updateData([{rowNumber:index, insertionStatus:errorMessage, status:"NOK"}])
        } 

        
        let row = this.tabulator.getRow(index);
        row.reformat();
        this.errorNumber = this.errorNumber + 1;
        this.progressValue = this.progressValue + 1;
        resolve();
      })
    })
  }
  
  uploaded(data) {
    for (let idx = 0; idx < data.length; idx++) {
      data[idx]["rowNumber"] = idx+1;
    }
    this.tabulator.setData(data);  
  }
}

</script>

<style lang="scss">

.tabulator .tabulator-header .tabulator-col[tabulator-field="label"] {
  color:#f00;
}

.tabulator .tabulator-header .tabulator-col {
  border-right:1px solid #dee2e6;
}

.tabulator .tabulator-table .tabulator-row .tabulator-cell {
  border-right:1px solid #dee2e6;
}
  
</style>

<i18n>

en:
  GermplasmTable:
    label: Name
    uri: URI
    rdfType: Type
    fromSpecies : Species URI
    fromVariety : Variety URI
    fromAccession: Accession URI
    institute: Institute Code
    comment: Comment
    year: ProductionYear
    checkingStatus: Checking status
    insertionStatus: Insertion Status
    downloadTemplate : Dowload template
    resetTable : Reset table
    check : Check before insertion
    insert : Insert
    progressTitle: lines to scan
    emptyMessage: The table is empty
    close: Close
    addRow: Add Row
    accessionNumber: AccessionNumber
    varietyCode: Variety Code
    lotNumber: LotNumber
    synonyms: Synonyms
    addColumn: Add column
    infoSynonyms: To add several synonyms, use | as separator
    infoAttributes: To add additional information, you can add columns
    infoLot: You have to fill species, variety or accession
    infoAccession: You have to fill at least species or variety
    help: Help

fr:
  GermplasmTable:
    label: Nom
    uri: URI
    rdfType: Type
    fromSpecies : URI de l'espèce
    fromVariety : URI de variété
    fromAccession: URI d'accession
    institute: Code institut
    comment: Commentaire
    year: Année
    checkingStatus: Statut
    insertionStatus: Statut
    downloadTemplate : Télécharger le template
    resetTable : Vider tableau
    check : Valider avant d'insérer
    insert : Insérer
    progressTitle: lignes à parcourir
    emptyMessage: Le tableau est vide
    close : Fermer
    addRow: Ajouter ligne
    accessionNumber: Code Accession
    varietyCode: Code Variété
    lotNumber: Code Lot
    synonyms: Synonymes
    addColumn: Ajouter colonne
    infoSynonyms: Pour ajouter plusieurs synonyms, utilisez | comme séparateur
    infoAttributes: Pour ajouter des informations supplémentaires, vous pouvez ajouter des colonnes
    infoLot: Vous devez renseigner au moins l'espèce, la variété ou l'accession
    infoAccession: Vous devez renseigner l'espèce ou la variété
    help: Aide
</i18n>
