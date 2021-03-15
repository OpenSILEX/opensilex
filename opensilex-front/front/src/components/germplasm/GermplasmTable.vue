<template>
  <div class="card"> 
    <b-modal ref="colModal" :title="$t('GermplasmTable.addColumn')" size="sm" hide-footer>
      <b-form-input v-model="colName" placeholder="Enter column name"></b-form-input>
      <b-button class="mt-3" variant="primary" block @click="addColumn">{{$t('GermplasmTable.addColumn')}}</b-button>
    </b-modal>

    <!-- <div>
      <b-button pill v-b-toggle.collapse-1 variant="outline-secondary">{{$t('GermplasmTable.help')}}</b-button>
      <b-collapse id="collapse-1" class="mb-2mt-2">
        <b-alert show>
          <div>{{$t('GermplasmTable.infoSynonyms')}}</div>
          <div>{{$t('GermplasmTable.infoAttributes')}}</div>
          <div>{{$t('GermplasmTable.infoMandatoryFields')}}</div>
        </b-alert>
      </b-collapse>
    </div> -->

    <b-input-group class="mt-2 mb-2" size="sm">
      <downloadCsv
        ref="downloadCsv"
        class="btn btn-outline-primary mb-2 mr-2"
        :data="jsonForTemplate"
        name="template.csv">
        {{$t('GermplasmTable.downloadTemplate')}}
      </downloadCsv>
      <opensilex-CSVInputFile v-on:updated="uploaded" delimiterOption=",">         
      </opensilex-CSVInputFile>
      <b-button class="mb-2 mr-2" @click="updateColumns" variant="outline-secondary">{{$t('GermplasmTable.resetTable')}}</b-button>
      <b-button class="mb-2 mr-2" @click="addRow" variant="outline-dark">{{$t('GermplasmTable.addRow')}}</b-button>
      <b-button class="mb-2 mr-2" @click="showColumnModal" variant="outline-dark">{{$t('GermplasmTable.addColumn')}}</b-button>  
      <b-form-select v-if="this.checkedLines>0"
        id="filter"
        v-model="filter"
        :options="checkBoxOptions"
        name="filter"
        size="sm"
        @input="filterLines()"
        
      ></b-form-select> 
    </b-input-group>    

    <b-input-group  size="sm">
      <b-button class="mb-2 mr-2" @click="checkData()" variant="primary" v-bind:disabled="disableCheck">{{$t("GermplasmTable.check")}}</b-button>
      <b-button class="mb-2 mr-2 " @click="insertData()" variant="success" v-bind:disabled="disableInsert">{{$t('GermplasmTable.insert')}}</b-button>
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
        {{this.max}} {{$t('GermplasmTable.progressTitle')}}
        <b-progress :max="max" show-progress animated>
          <b-progress-bar :value="progressValue" :max="max" variant="info">
            <strong>Progress: {{ progressValue }} / {{ max }}</strong>
          </b-progress-bar>
        </b-progress>
      </b-alert>
      <b-alert variant="primary" :show="infoMessage">{{this.summary}}</b-alert>
      <b-alert variant="danger" :show="alertEmptyTable">{{$t('GermplasmTable.emptyMessage')}}</b-alert>
      <b-alert v-if="onlyChecking" variant="warning" :show="onlyChecking && !alertEmptyTable">{{$t('GermplasmTable.infoProposeInsertion')}}</b-alert>
      <template v-slot:modal-footer>
        <b-button
          v-if="onlyChecking && !alertEmptyTable"
          class="mb-2 mr-2"
          @click="insertDataFromModal()"
          variant="success"
        >{{$t('GermplasmTable.insert')}}</b-button>
        <b-button
          v-bind:disabled="disableCloseButton"
          class="mb-2 mr-2"
          @click="$bvModal.hide('progressModal')"
          variant="primary"
        >{{$t('GermplasmTable.close')}}</b-button>
      </template>
    </b-modal>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch, Ref } from "vue-property-decorator";
import { GermplasmCreationDTO, GermplasmService } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import JsonCSV from "vue-json-csv";
Vue.component("downloadCsv", JsonCSV);
import Tabulator from 'tabulator-tables';
import Oeso from "../../ontologies/Oeso";

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
  progressValue = 0;
  max = 0;
  modalTitle: string = "Scanning lines";
  infoMessage: boolean = false;
  alertEmptyTable: boolean = false;
  disableCloseButton: boolean = true;

  @Ref("progressModal") readonly progressModal!: any;
  @Ref("progressBar") readonly progressBar!: any;
  @Ref("colModal") readonly colModal!: any;
  @Ref("table") readonly table!: any;

  props = [
    {
      germplasmType: {
        type: String,
        default: "vocabulary:Species"
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
    //})
  }
  
  beforeDestroy() {
    this.langUnwatcher();
  }

  @Watch("tableData", { deep: true })
  newData(value: string, oldValue: string) {
    this.tabulator.replaceData(value);
  }

  updateColumns() {
    console.log(this.$attrs.germplasmType);
    this.suppColumnsNames = [];
    this.colName = null;

    let idCol = {title:"", field:"rowNumber",visible:true,formatter:"rownum"};

    let statusCol ={title:"status", field:"status",visible:false};

    let uriCol = {title:"URI", field:"uri", visible:true, editor:true, minWidth:150, validator: "unique"};

    let labelCol = 
      {title:this.$t('GermplasmTable.name') + '<span class="required">*</span>', field:"name", visible:true, editor:true, minWidth:150, 
        //validator: "unique"
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
    let subtaxaCol = {title:this.$t('GermplasmTable.subtaxa'), field:"subtaxa", visible:true, editor:true};
    let speciesCol = {title:this.$t('GermplasmTable.fromSpecies') + '<span class="requiredOnCondition">*</span>', field:"species", visible:true, editor:true};
    let varietyCol = {title:this.$t('GermplasmTable.fromVariety') + '<span class="requiredOnCondition">*</span>', field:"variety", visible:true, editor:true};
    let accessionCol = {title:this.$t('GermplasmTable.fromAccession') + '<span class="requiredOnCondition">*</span>', field:"accession", visible:true, editor:true}
    let instituteCol =  {title:this.$t('GermplasmTable.institute'), field:"institute", visible:true, editor:true};
    let websiteCol = {title:this.$t('GermplasmTable.website'), field:"website", visible:true, editor:true};
    let productionYearCol =  {title:this.$t('GermplasmTable.year'), field:"productionYear", visible:true, editor:true};
    let commentCol = {title:this.$t('GermplasmTable.comment'), field:"comment", visible:true, editor:true};
    let checkingStatusCol = {title:this.$t('GermplasmTable.checkingStatus'), field:"checkingStatus", visible:false, editor:false};
    let insertionStatusCol ={title:this.$t('GermplasmTable.insertionStatus'), field:"insertionStatus", visible:false, editor:false, minWidth:400};

    if (Oeso.checkURIs(this.$attrs.germplasmType, Oeso.SPECIES_TYPE_URI))  {
      this.tableColumns = [idCol, statusCol, uriCol, labelCol, synonymCol, commentCol, checkingStatusCol, insertionStatusCol]
    } else if (Oeso.checkURIs(this.$attrs.germplasmType, Oeso.VARIETY_TYPE_URI)) {
      let codeVar = {title:this.$t('GermplasmTable.varietyCode'), field:"code", visible:true, editor:true};
      this.tableColumns = [idCol, statusCol, uriCol, labelCol, subtaxaCol, codeVar,  speciesCol, instituteCol, websiteCol, commentCol, checkingStatusCol, insertionStatusCol]
    } else if (Oeso.checkURIs(this.$attrs.germplasmType, Oeso.ACCESSION_TYPE_URI)) {
      let codeAcc = {title:this.$t('GermplasmTable.accessionNumber'), field:"code", visible:true, editor:true};
      this.tableColumns = [idCol, statusCol, uriCol, labelCol, subtaxaCol, codeAcc, speciesCol, varietyCol, instituteCol, websiteCol, commentCol, checkingStatusCol, insertionStatusCol]        
    } else {
      let codeLot = {title:this.$t('GermplasmTable.lotNumber'), field:"code", visible:true, editor:true};
      this.tableColumns = [idCol, statusCol, uriCol, labelCol, synonymCol, codeLot,  speciesCol, varietyCol, accessionCol, instituteCol, websiteCol, productionYearCol, commentCol, checkingStatusCol, insertionStatusCol]  
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
    this.tabulator.addColumn(
      { title: this.colName, field: this.colName, editor: true },
      false,
      "comment"
    );
    this.suppColumnsNames.push(this.colName);
    this.jsonForTemplate[0][this.colName] = null;
    this.$attrs.downloadCsv;
    this.colName = null;
  }

  addRow() {
    let size = this.tabulator.getData().length;
    this.tabulator.addRow({ rowNumber: size + 1 });
    console.log(this.tabulator.getData().length);
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

  checkData() {
    this.onlyChecking = true;
    this.showModal();
    this.tabulator.showColumn("checkingStatus");
    this.tabulator.hideColumn("insertionStatus");     
    this.disableInsert = false;
    this.checkedLines++;
  }

  insertData() {
    this.onlyChecking = false;
    this.showModal();
    this.tabulator.hideColumn("checkingStatus");
    this.tabulator.showColumn("insertionStatus");  
    this.disableInsert = true;
    this.disableCheck = true;
  }

  insertOrCheckData() {
    this.disableCloseButton = true;
    let dataToInsert = this.tabulator.getData();

    let promises = [];
    this.$opensilex.disableLoader();

    let colDefs = this.tabulator.getColumnDefinitions();

    for (let idx = 0; idx < dataToInsert.length; idx++) {
      let form: GermplasmCreationDTO = {
        rdf_type: null,
        name: null,
        uri: null,
        species: null,
        variety: null,
        accession: null,
        institute: null,
        production_year: null,
        description: null,
        code: null,
        synonyms: [],
        metadata: null,
        website: null
      };

      form.rdf_type = this.$attrs.germplasmType;

      if (dataToInsert[idx].uri != null && dataToInsert[idx].uri != "") {
        form.uri = dataToInsert[idx].uri;
      }
      if (dataToInsert[idx].name != null && dataToInsert[idx].name != "") {
        form.name = dataToInsert[idx].name;
      }
      if (
        dataToInsert[idx].species != null &&
        dataToInsert[idx].species != ""
      ) {
        form.species = dataToInsert[idx].species;
      }
      if (
        dataToInsert[idx].variety != null &&
        dataToInsert[idx].variety != ""
      ) {
        form.variety = dataToInsert[idx].variety;
      }
      if (
        dataToInsert[idx].accession != null &&
        dataToInsert[idx].accession != ""
      ) {
        form.accession = dataToInsert[idx].accession;
      }
      if (
        dataToInsert[idx].institute != null &&
        dataToInsert[idx].institute != ""
      ) {
        form.institute = dataToInsert[idx].institute;
      }
      if (
        dataToInsert[idx].productionYear != null &&
        dataToInsert[idx].productionYear != ""
      ) {
        form.production_year = dataToInsert[idx].productionYear;
      }
      if (
        dataToInsert[idx].comment != null &&
        dataToInsert[idx].comment != ""
      ) {
        form.description = dataToInsert[idx].comment;
      }
      if (dataToInsert[idx].code != null && dataToInsert[idx].code != "") {
        form.code = dataToInsert[idx].code;
      }

      if (
        dataToInsert[idx].synonyms != null &&
        dataToInsert[idx].synonyms != ""
      ) {
        let stringSynonyms = dataToInsert[idx].synonyms;
        form.synonyms = stringSynonyms.split("|");
      }

      if (dataToInsert[idx].website != null && dataToInsert[idx].website != "") {
        form.website = dataToInsert[idx].website;
      }


      if (
        dataToInsert[idx].subtaxa != null &&
        dataToInsert[idx].subtaxa != ""
      ) {
        let stringSynonyms = dataToInsert[idx].subtaxa;
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
          form.metadata = attributes;
        }
      }

      if (
        form.name == null &&
        form.uri == null &&
        form.species == null &&
        form.variety == null &&
        form.accession == null &&
        form.institute == null &&
        form.production_year == null &&
        form.description == null &&
        form.code == null &&
        form.synonyms.length == 0 &&
        form.metadata == null &&
        form.website == null
      ) {
        this.emptyLines = this.emptyLines + 1;
        this.progressValue = this.progressValue + 1;
      } else {
        promises.push(
          this.callCreateGermplasmService(form, idx + 1, this.onlyChecking)
        );
      }
    }

    Promise.all(promises).then(result => {
      if (this.onlyChecking) {
        this.summary = this.okNumber + " " + this.$t('GermplasmTable.infoMessageGermplReady') + ", " + this.errorNumber + " " + this.$t('GermplasmTable.infoMessageErrors') + ", " + this.emptyLines + " " + this.$t('GermplasmTable.infoMessageEmptyLines');
      } else {
        this.summary = this.okNumber + " " + this.$t('GermplasmTable.infoMessageGermplInserted') +", " + this.errorNumber + " " + this.$t('GermplasmTable.infoMessageErrors') + ", " + this.emptyLines + " " + this.$t('GermplasmTable.infoMessageEmptyLines');
      }
      this.tabulator.redraw(true); 
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
    console.debug("Loading form view...");
    this.service = this.$opensilex.getService("opensilex.GermplasmService");
  }

  callCreateGermplasmService(
    form: GermplasmCreationDTO,
    index: number,
    onlyChecking: boolean
  ) {
    return new Promise((resolve, reject) => {
    this.service
    .createGermplasm(onlyChecking, form)
    .then((http: HttpResponse<OpenSilexResponse<any>>) => {
      if(onlyChecking) {
        this.tabulator.updateData([{rowNumber:index, checkingStatus:this.$t('GermplasmTable.checkingStatusMessage'), status:"OK"}])
      } else {
        this.tabulator.updateData([{rowNumber:index, insertionStatus:this.$t('GermplasmTable.insertionStatusMessage'), status:"OK", uri:http.response.result}])
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
        resolve();
      });
    });
  }

  uploaded(data) {
    if (data.length > 1000) {
      alert(this.$t('GermplasmTable.alertFileSize'));
    } else {
      var uniqueNames = [];
      var uniqueURIs = [];
      let insertionOK = true;
      for (let idx = 0; idx < data.length; idx++) {
        data[idx]["rowNumber"] = idx + 1;
        if (data[idx].name === "") {
          alert(this.$t('GermplasmTable.missingName') + " " + data[idx]["rowNumber"]);
          insertionOK = false
          break
        } else {
          if(data[idx].name !== "" && uniqueNames.indexOf(data[idx].name) === -1){
            uniqueNames.push(data[idx].name);        
          } else {
            insertionOK = false
            alert(this.$t('GermplasmTable.alertDuplicateName') + " " + data[idx]["rowNumber"] + ", name= " + data[idx].name);
            break
          }
        }
        if(data[idx].uri !== "") {
            if (uniqueURIs.indexOf(data[idx].uri) === -1){
              uniqueURIs.push(data[idx].uri);        
            } else {
              insertionOK = false
              alert(this.$t('GermplasmTable.alertDuplicateURI') + " " + data[idx]["rowNumber"] + ", uri= " + data[idx].uri);
              break
            }
        }
         
      }
      if (insertionOK) {
        this.tabulator.setData(data);
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

  insertDataFromModal() {
    this.$bvModal.hide('progressModal');
    this.insertData();
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

// .tabulator .tabulator-header .tabulator-row  {
//   height: 30px;
// }
  
</style>

<i18n>

en:
  GermplasmTable:
    name: Name
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
    check : Check
    insert : Insert
    progressTitle: lines to scan
    emptyMessage: The table is empty
    close: Close
    addRow: Add Row
    accessionNumber: AccessionNumber
    varietyCode: Variety Code
    lotNumber: LotNumber
    synonyms: Synonyms
    subtaxa: Subtaxa
    website: Website
    addColumn: Add column
    infoSynonyms: To add several synonyms or subtaxa, use | as separator
    infoAttributes: To add additional information, you can add columns
    infoLot: You have to fill species, variety or accession
    infoAccession: You have to fill at least species or variety
    help: Help
    infoMessageGermplReady: germplasm ready to be inserted
    infoMessageErrors: errors
    infoMessageEmptyLines: empty lines
    infoMessageGermplInserted: germplasm inserted
    infoProposeInsertion: Don't forget to click on Insert button in order to finalize germplasm insertion (button below or above the table)
    checkingStatusMessage: ready
    insertionStatusMessage: created
    filterLines: Filter the lines
    infoMandatoryFields: It is mandatory to fill the species URI column if you create varieties. If you create Accession or Lot, you have to fill at least one column between Accession URI, Variety URI and Species URI.
    alertDuplicate: The file contains a duplicate name at line
    alertDuplicateURI: The file contains a duplicate uri at line
    alertFileSize: The file has too many lines, 1000 lines maximum
    missingName: The name is missing at line

fr:
  GermplasmTable:
    name: Nom
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
    downloadTemplate : Télécharger un gabarit
    resetTable : Vider tableau
    check : Valider
    insert : Insérer
    progressTitle: lignes à parcourir
    emptyMessage: Le tableau est vide
    close : Fermer
    addRow: Ajouter ligne
    accessionNumber: Code Accession
    varietyCode: Code Variété
    lotNumber: Code Lot
    synonyms: Synonymes
    subtaxa : Subtaxa
    website: Site web
    addColumn: Ajouter colonne
    infoSynonyms: Pour ajouter plusieurs synonymes ou subtaxa, utilisez | comme séparateur
    infoAttributes: Pour ajouter des informations supplémentaires, vous pouvez ajouter des colonnes
    infoLot: Vous devez renseigner au moins l'espèce, la variété ou l'accession
    infoAccession: Vous devez renseigner l'espèce ou la variété
    help: Aide
    infoMessageGermplReady: germplasm prêts à être insérer
    infoMessageErrors: erreurs
    infoMessageEmptyLines: lignes vides
    infoMessageGermplInserted: germplasm insérés
    infoProposeInsertion: N'oubliez pas de cliquer sur le bouton Insérer afin de finaliser l'insertion des ressources (bouton situé ci-dessous ou au-dessus du tableau)
    checkingStatusMessage: validé
    insertionStatusMessage: créé
    seeErrorLines: See lines
    seeAll : see all 
    infoMandatoryFields: Il est obligatoire de renseigner au moins une des 3 colonnes URI de l'espèce, URI de la varieté ou URI de l'Accession.
    alertDuplicateName: Le fichier comporte un doublon de nom à la ligne
    alertDuplicateURI: Le fichier comporte un doublon d'uri à la ligne 
    alertFileSize: Le fichier contient trop de ligne, 1000 lignes maximum
    missingName: Le nom n'est pas renseigné à la ligne 
</i18n>
