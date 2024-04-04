<template>
  <div class="card">
    <div>
      <opensilex-GermplasmAddColumnModal
          class="searchFilter"
          ref="colModal"
          :existingRdfAttributesObjects="existingDuplicatableRdfAttributesObjects"
          :existingRdfAttributesStringRule="existingRdfAttributesStringRule"
          @addingExistingColumn="onColModalAddExistingColumn"
          @addingUncontrolledColumn="onColModalAddingUncontrolledColumn"
      ></opensilex-GermplasmAddColumnModal>
    </div>

    <b-input-group class="mt-2 mb-2" size="sm">
      <downloadCsv
        ref="downloadCsv"
        class="btn downloadTemplateBtn mb-2 mr-2"
        :data="[jsonForTemplate]"
        :advancedOptions="{'header': false}"
        name="template.csv"
      >
        {{ $t("GermplasmTable.downloadTemplate") }}
      </downloadCsv>
      <opensilex-CSVInputFile
        v-on:updated="uploaded"
        :returnDataAsArrayOfArrays="true"
        :duplicatableHeaders="existingDuplicatablePropertiesNameList"
        :headersPresent="['name']"
      >
      </opensilex-CSVInputFile>
      <b-button
        class="mb-2 mr-2"
        @click="onResetTableBtnClick"
        variant="outline-secondary"
        >{{ $t("GermplasmTable.resetTable") }}</b-button
      >
      <b-button class="mb-2 mr-2" @click="onAddRowBtnClick" variant="outline-dark">{{
        $t("GermplasmTable.addRow")
      }}</b-button>
      <b-button
        class="mb-2 mr-2"
        @click="onAddColumnBtnClick"
        variant="outline-dark"
        >{{ $t("GermplasmTable.addColumn") }}</b-button
      >
      <b-form-select
        v-if="this.checkedLines > 0"
        id="filter"
        v-model="filter"
        :options="checkBoxOptions"
        name="filter"
        size="sm"
        @input="onFilterLinesInput()"
      ></b-form-select>
    </b-input-group>

    <b-input-group size="sm">
      <b-button
        class="mb-2 mr-2 greenThemeColor"
        @click="onCheckBtnClick()"
        v-bind:disabled="disableCheck"
        >{{ $t("GermplasmTable.check") }}</b-button
      >
      <b-button
        class="mb-2 mr-2"
        @click="onInsertDataBtnClick"
        variant="success"
        v-bind:disabled="disableInsert"
        >{{ $t("GermplasmTable.insert") }}</b-button
      >
    </b-input-group>

    <div ref="table"></div>

    <b-modal
      id="progressModal"
      size="lg"
      :no-close-on-backdrop="true"
      :no-close-on-esc="true"
      hide-header
      @shown="onProgressmodalShown"
    >
      <b-alert ref="progressAlert" variant="light" show>
        {{ this.max }} {{ $t("GermplasmTable.progressTitle") }}
        <b-progress :max="max" show-progress animated>
          <b-progress-bar :value="progressValue" :max="max" variant="info">
            <strong> {{ $t("GermplasmTable.progressValue") }}  {{ progressValue }} / {{ max }}</strong>
          </b-progress-bar>
        </b-progress>
      </b-alert>
      <b-alert variant="primary" :show="infoMessage">{{
        this.summary
      }}</b-alert>
      <b-alert variant="danger" :show="alertEmptyTable">{{
        $t("GermplasmTable.emptyMessage")
      }}</b-alert>
      <b-alert
        v-if="onlyChecking"
        variant="warning"
        :show="onlyChecking && !alertEmptyTable"
        >{{ $t("GermplasmTable.infoProposeInsertion") }}</b-alert
      >
      <template v-slot:modal-footer>
        <b-button
          v-if="onlyChecking && !alertEmptyTable"
          class="mb-2 mr-2"
          @click="onPorgressModalInsertBtnClick()"
          variant="success"
          >{{ $t("GermplasmTable.insert") }}</b-button
        >
        <b-button
          v-bind:disabled="disableCloseButton"
          class="mb-2 mr-2 greenThemeColor"
          @click="$bvModal.hide('progressModal')"
          >{{ $t("GermplasmTable.close") }}</b-button
        >
      </template>
    </b-modal>

    <b-modal
      :no-close-on-backdrop="true"
      :no-close-on-esc="true"
      @hidden="onNewColsModalHidden"
      ref="newcolsModal"
      centered
      hide-footer
   
      :title="$t('GermplasmTable.newColumns')"
    >
      <b-form-group
        :label="$t('GermplasmTable.newColumnsHelp')"
        v-slot="{ ariaDescribedby }"
      >
        <b-form-checkbox
          v-for="columnCheckboxData in newColumnModalCheckboxData"
          v-model="newColumnsselected"
          :key="columnCheckboxData.value"
          :value="columnCheckboxData.value"
          :aria-describedby="ariaDescribedby"
          :name="columnCheckboxData.name"
        >
          {{ columnCheckboxData.name }}
        </b-form-checkbox>
      </b-form-group>

      <!--        checkbox to select/unselect all columns-->
      <b-form-checkbox
          @change="onSelectAllColumnSwitchChange"
          switch
      >
        {{ $t('GermplasmTable.toggleAll') }}
      </b-form-checkbox>

      <b-button
        type="button"
        class="btn greenThemeColor loadCsvButton"
        v-on:click="addNewColumns()"
      >
        {{ $t('component.common.ok') }}
      </b-button>
      <b-button
        type="button"
        class="btn loadCsvButton"
        v-on:click="addNewColumnsCancel()"
      >
        {{ $t('component.common.cancel') }}
      </b-button>

    </b-modal>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch, Ref } from "vue-property-decorator";
// @ts-ignore
import { GermplasmCreationDTO, GermplasmService } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import JsonCSV from "vue-json-csv";
Vue.component("downloadCsv", JsonCSV);
// @ts-ignore
import {TabulatorFull as Tabulator} from 'tabulator-tables';
import Oeso from "../../ontologies/Oeso";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {OntologyService} from "opensilex-core/api/ontology.service";
import {ResourceTreeDTO} from "opensilex-core/model/resourceTreeDTO";
import { SelectableItem } from '../common/forms/SelectForm.vue';
import {RDFObjectRelationDTO} from "opensilex-core/model/rDFObjectRelationDTO";
import VueRouter from "vue-router";
import {OpenSilexStore} from "../../models/Store";
import VueI18n from "vue-i18n";
import {User} from "../../models/User";

export interface NewColumnCheckboxData {
  value: string,
  name: string
}

@Component
/**
 * The table that's shown during the creation of germplasms.
 */
export default class GermplasmTable extends Vue {
  //#region Plugins and services
  $opensilex: OpenSilexVuePlugin;
  $store: OpenSilexStore;
  $router: VueRouter;
  $t: typeof VueI18n.prototype.t;
  $i18n: VueI18n;
  service: GermplasmService;
  //#endregion

  //#region Props
  props = [
    {
      germplasmType: {
        type: String,
        default: "vocabulary:Species",
      },
    },
  ];
  //#endregion

  //#region Refs
  @Ref("progressModal") readonly progressModal!: any;
  @Ref("progressBar") readonly progressBar!: any;
  @Ref("colModal") readonly colModal!: any;
  @Ref("table") readonly table!: any;
  @Ref("newcolsModal") readonly newcolsModal!: any;
  @Ref("helpModal") readonly helpModal!: any;
  //#endregion

  //#region Data
  onlyChecking: boolean = true;
  suppColumnsNames: Array<string>;
  $bvModal: any;

  // Progress Modal
  private errorNumber: number = 0;
  private okNumber: number = 0;
  private emptyLines: number = 0;
  private summary: string = "";
  private progressValue = 0;
  private max = 0;
  private infoMessage: boolean = false;
  private alertEmptyTable: boolean = false;
  private disableCloseButton: boolean = true;

  private newColumns: string[] = [];
  private newColumnsselected: string[] = []; // Must be an array reference!
  private newColumnModalCheckboxData: NewColumnCheckboxData[] = [];

  /**
   * Map where keys are non obligatary duplicatable columns that already exist.
   * A key is added the first time this column is added (duplicates aloud). The keys are equal to the JsonParams in GermplasmCreationDTO, example : hasParentGermplasmA
   * The values represent the current id for the tabulator, a digit is added each time.
   * Example hasParentGermplasmM0, hasParentGermplasmF1, etc...
   */
  private addedDuplicatableRdfColumnsToMaxUsedIdMap : Map<string, number> = new Map<string, number>();

  private existingDuplicatableRdfAttributesObjects: Array<SelectableItem> = [];

  private existingRdfAttributesStringRule:string = "";

  private existingDuplicatablePropertiesNameList: string[] = [];

  private tabulator:Tabulator = null;

  private tableData = [];
  @Watch("tableData", { deep: true })
  newData(value: string, oldValue: string) {
    this.tabulator.replaceData(value);
  }

  private csvUploadedData = [];

  private tableColumns: any[] = [];

  private insertionStatus = [];

  private jsonForTemplate = [];

  private readonly checkBoxOptions = [
    { text: "See all lines", value: "all" },
    { text: "See lines with errors", value: "NOK" },
  ];

  private filter = "all";

  private disableInsert: boolean = false;
  private disableCheck: boolean = false;
  private checkedLines: number = 0;

  private langUnwatcher;
  //endregion

  //#region Computed
  get user(): User {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }
  //endregion

  //#region Event handlers
  private onAddColumnBtnClick() {
    this.colModal.show();
  }
  private onColModalAddExistingColumn(columnName: string, propertyUri: string) {
    this.colModal.hide();
    this.addExistingColumn(columnName, propertyUri, "comment");
  }
  private onColModalAddingUncontrolledColumn(columnName: string) {
    this.colModal.hide();
    this.addNonExistingColumn(columnName, "comment");
  }

  private onAddRowBtnClick() {
    let size = this.tabulator.getData().length;
    this.tabulator.addRow({ rowNumber: size + 1 });
    console.log(this.tabulator.getData().length);
  }

  private onCheckBtnClick() {
    this.onlyChecking = true;
    this.showModal();
    this.tabulator.showColumn("checkingStatus");
    this.tabulator.hideColumn("insertionStatus");
    this.disableInsert = false;
    this.checkedLines++;
  }

  private onResetTableBtnClick() {
    this. updateColumns();
  }

  private onInsertDataBtnClick(){
    this.insertData();
  }

  private onFilterLinesInput() {
    if (this.filter != "all") {
      this.tabulator.setFilter("status", "=", this.filter);
    } else {
      this.tabulator.clearFilter(true);
    }
  }

  private onProgressmodalShown(){
    this.insertOrCheckData();
  }

  private onPorgressModalInsertBtnClick() {
    this.$bvModal.hide("progressModal");
    this.insertData();
  }

  private onNewColsModalHidden(){
    this.tableData = this.csvUploadedData;
  }

  private onSelectAllColumnSwitchChange(checked) {
    this.newColumnsselected = checked ? this.newColumnModalCheckboxData.slice().map( column => column.value) : []
  }

  //#endregion

  //#region Hooks
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      (lang) => {
        this.updateColumns();
      }
    );
    this.updateColumns();
  }

  beforeDestroy() {
    this.langUnwatcher();
  }
  //endregion

  //#region Private methods
  private showSelectNewColumnsPopUp() {
    this.newcolsModal.show();
  }

  private addNewColumnsCancel() {
    this.$opensilex.showLoader();
    this.newcolsModal.hide();
  }


  /**
   * Adds a set of new metadata columns from this.newColumnsselected and this.newColumns.
   * Only adds duplicates if the header is recognized as duplicable.
   * Data is loaded in the tabulator only when the modal is closed. This ensures better performance because adding column in a fulfilled tabulator is a heavy process.
   */
  private addNewColumns() {
    this.$opensilex.showLoader()
    this.colModal.hide();
    let addedNonDuplicatableCols : string[] = [];
    for (let col of this.newColumns) {
      if(this.newColumnsselected.includes(col)){
        let existingProperty = this.tryToGetExistingPropertyFromColumnName(col);
        if(existingProperty){
          this.addExistingColumn(existingProperty.label, existingProperty.id, existingProperty.id)
        }else{
          if(!addedNonDuplicatableCols.includes(col)){
            this.addNonExistingColumn(col, col);
            addedNonDuplicatableCols.push(col);
          }
        }
      }
    }
    this.newColumns = [];
    this.newColumnsselected = [];
    this.newcolsModal.hide();
  }

  /**
   * Looks at the type of germplasm (species, variety, etc...) to set this.tableColumns.
   * Then initialises this.tabulator
   */
  private updateColumns() {
    console.log(this.$attrs.germplasmType);
    this.suppColumnsNames = [];

    let idCol = {
      title: "",
      field: "rowNumber",
      visible: true,
      formatter: "rownum",
    };

    let statusCol = { title: "status", field: "status", visible: false };

    let uriCol = {
      title: "URI",
      field: "uri",
      visible: true,
      editor: true,
      minWidth: 150,
      validator: "unique",
    };

    let labelCol = {
      title: this.$t("GermplasmTable.name") + '<span class="required">*</span>',
      field: "name",
      visible: true,
      editor: true,
      minWidth: 150,
    };
    let synonymCol = {
      title: this.$t("GermplasmTable.synonyms"),
      field: "synonyms",
      visible: true,
      editor: true,
    };
    let subtaxaCol = {
      title: this.$t("GermplasmTable.subtaxa"),
      field: "subtaxa",
      visible: true,
      editor: true,
    };
    let speciesCol = {
      title:
        this.$t("GermplasmTable.fromSpecies") +
        '<span class="requiredOnCondition">*</span>',
      field: "species",
      visible: true,
      editor: true,
    };
    let varietyCol = {
      title:
        this.$t("GermplasmTable.fromVariety") +
        '<span class="requiredOnCondition">*</span>',
      field: "variety",
      visible: true,
      editor: true,
    };
    let accessionCol = {
      title:
        this.$t("GermplasmTable.fromAccession") +
        '<span class="requiredOnCondition">*</span>',
      field: "accession",
      visible: true,
      editor: true,
    };
    let instituteCol = {
      title: this.$t("GermplasmTable.institute"),
      field: "institute",
      visible: true,
      editor: true,
    };
    let websiteCol = {
      title: this.$t("GermplasmTable.website"),
      field: "website",
      visible: true,
      editor: true,
    };
    let productionYearCol = {
      title: this.$t("GermplasmTable.year"),
      field: "productionYear",
      visible: true,
      editor: true,
    };
    let commentCol = {
      title: this.$t("GermplasmTable.comment"),
      field: "comment",
      visible: true,
      editor: true,
    };
    let checkingStatusCol = {
      title: this.$t("GermplasmTable.checkingStatus"),
      field: "checkingStatus",
      visible: false,
      editor: false,
    };
    let insertionStatusCol = {
      title: this.$t("GermplasmTable.insertionStatus"),
      field: "insertionStatus",
      visible: false,
      editor: false,
      minWidth: 400,
    };

    if (Oeso.checkURIs(this.$attrs.germplasmType, Oeso.SPECIES_TYPE_URI)) {
      this.tableColumns = [
        idCol,
        statusCol,
        uriCol,
        labelCol,
        synonymCol,
        commentCol,
        checkingStatusCol,
        insertionStatusCol,
      ];
    } else if (
      Oeso.checkURIs(this.$attrs.germplasmType, Oeso.VARIETY_TYPE_URI)
    ) {
      let codeVar = {
        title: this.$t("GermplasmTable.varietyCode"),
        field: "code",
        visible: true,
        editor: true,
      };
      this.tableColumns = [
        idCol,
        statusCol,
        uriCol,
        labelCol,
        subtaxaCol,
        codeVar,
        speciesCol,
        instituteCol,
        websiteCol,
        commentCol,
        checkingStatusCol,
        insertionStatusCol,
      ];
    } else if (
      Oeso.checkURIs(this.$attrs.germplasmType, Oeso.ACCESSION_TYPE_URI)
    ) {
      let codeAcc = {
        title: this.$t("GermplasmTable.accessionNumber"),
        field: "code",
        visible: true,
        editor: true,
      };
      this.tableColumns = [
        idCol,
        statusCol,
        uriCol,
        labelCol,
        subtaxaCol,
        codeAcc,
        speciesCol,
        varietyCol,
        instituteCol,
        websiteCol,
        commentCol,
        checkingStatusCol,
        insertionStatusCol,
      ];
    } else {
      let codeLot = {
        title: this.$t("GermplasmTable.lotNumber"),
        field: "code",
        visible: true,
        editor: true,
      };
      this.tableColumns = [
        idCol,
        statusCol,
        uriCol,
        labelCol,
        synonymCol,
        codeLot,
        speciesCol,
        varietyCol,
        accessionCol,
        instituteCol,
        websiteCol,
        productionYearCol,
        commentCol,
        checkingStatusCol,
        insertionStatusCol,
      ];
    }

    //Add stuff to existing property string rule (to prevent duplicates)
    let tableStartingHeaderTitlesFields : string = this.tableColumns.map((col, index)=>{return col.field + ","+ this._removeSpanRequiredBlockFromTitle(col.title)}).toString();
    if(this.existingRdfAttributesStringRule===""){
      this.existingRdfAttributesStringRule = "existingProperty:" + tableStartingHeaderTitlesFields;
    }else{
      this.existingRdfAttributesStringRule = this.existingRdfAttributesStringRule + "," + tableStartingHeaderTitlesFields;
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
      height: "70vh",
      rowFormatter: function (row) {
        let r = row.getData().status;
        if (row.getData().status == "OK") {
          row.getElement().style.backgroundColor = "#a5e051";
        } else if (row.getData().status == "NOK") {
          row.getElement().style.backgroundColor = "#ed6661";
        }
      },
    });

    this.tabulator.on("dataProcessed", () => {
      this.$opensilex.hideLoader();
    });

    this.jsonForTemplate = [];
    //let jsonHeader = {};
    for (var i = 1; i < this.tableColumns.length; i++) {
      if (this.tableColumns[i].visible == true) {
        this.jsonForTemplate.push(this.tableColumns[i].field);
      }
    }
    this.checkedLines = 0;
    this.disableCheck = false;
    this.disableInsert = false;
  }

  private _removeSpanRequiredBlockFromTitle(title: string): string{
    let result = title;
    if(title.includes("<span")){
      result = result.substring(0, title.indexOf("<span"));
    }
    return result;
  }

  private addInitialXRows(X) {
    for (let i = 1; i < X + 1; i++) {
      this.tableData.push({ rowNumber: i });
    }
  }

  /**
   * Generates a new name in function of how many times this rdf column has been added, then adds it.
   */
  private addExistingColumn(columnName: string, propertyUri: string, positionTarget: string) {
    let currentMaxExcludedIndex : number = this.addedDuplicatableRdfColumnsToMaxUsedIdMap.get(propertyUri);
    if(currentMaxExcludedIndex == null){
      this.addedDuplicatableRdfColumnsToMaxUsedIdMap.set(propertyUri, 1)
      currentMaxExcludedIndex = 0;
    }else{
      this.addedDuplicatableRdfColumnsToMaxUsedIdMap.set(propertyUri, currentMaxExcludedIndex + 1);
    }
    this.addColumnToTabulator(columnName, propertyUri + currentMaxExcludedIndex, positionTarget);
  }

  /**
   * Adds a new column, will be stored in Mongo.
   * Does a check to make sure there aren't any duplications
   */
  private addNonExistingColumn(columnName: string, positionTarget: string) {
    this.existingRdfAttributesStringRule = this.existingRdfAttributesStringRule + "," + columnName;
    this.addColumnToTabulator(columnName, columnName, positionTarget);
    this.suppColumnsNames.push(columnName);
  }

  private addColumnToTabulator(columnLabel: string, columnID: string, positionTarget: string){
    this.tabulator.addColumn(
        { title: columnLabel, field: columnID, editor: true },
        false,
        "comment"
    );
    //this.jsonForTemplate[0][columnID] = null;
    this.jsonForTemplate.push(columnLabel);
    this.$attrs.downloadCsv;
  }

  private resetModal() {
    this.max = 0;
    this.progressValue = 0;
    this.errorNumber = 0;
    this.okNumber = 0;
    this.emptyLines = 0;
    this.infoMessage = false;
    this.alertEmptyTable = false;
  }

  private showModal() {
    this.resetModal();
    this.max = this.tabulator.getData().length;
    this.$bvModal.show("progressModal");
  }

  private insertData() {
    this.onlyChecking = false;
    this.showModal();
    this.tabulator.hideColumn("checkingStatus");
    this.tabulator.showColumn("insertionStatus");
    this.disableInsert = true;
    this.disableCheck = true;
  }

  private insertOrCheckData() {
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
        website: null,
        relations: []
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

      if (
        dataToInsert[idx].website != null &&
        dataToInsert[idx].website != ""
      ) {
        form.website = dataToInsert[idx].website;
      }

      this.addedDuplicatableRdfColumnsToMaxUsedIdMap.forEach(
          (currentMaxIndexExcluded, extraRdfAttribute)=>{
            for(let duplicationIndex: number = 0 ; duplicationIndex<currentMaxIndexExcluded ; duplicationIndex++){
              let currentTabulatorIdentifier = extraRdfAttribute + duplicationIndex;
              let currentValue = dataToInsert[idx][currentTabulatorIdentifier];
              if (currentValue != null && currentValue != "") {
                let nextRelationDTO : RDFObjectRelationDTO = {};
                nextRelationDTO.inverse = false;
                nextRelationDTO.value = currentValue;
                nextRelationDTO.property = extraRdfAttribute;
                form.relations.push(nextRelationDTO);
              }
            }
          });

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
        (form.relations == null || form.relations.length === 0) &&
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

    doAllSequentually(promises).then(() => {
      if (this.onlyChecking) {
        this.summary =
          this.okNumber +
          " " +
          this.$t("GermplasmTable.infoMessageGermplReady") +
          ", " +
          this.errorNumber +
          " " +
          this.$t("GermplasmTable.infoMessageErrors") +
          ", " +
          this.emptyLines +
          " " +
          this.$t("GermplasmTable.infoMessageEmptyLines");
      } else {
        this.summary =
          this.okNumber +
          " " +
          this.$t("GermplasmTable.infoMessageGermplInserted") +
          ", " +
          this.errorNumber +
          " " +
          this.$t("GermplasmTable.infoMessageErrors") +
          ", " +
          this.emptyLines +
          " " +
          this.$t("GermplasmTable.infoMessageEmptyLines");
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

    async function doAllSequentually(fnPromiseArr) {
      for (let i = 0; i < fnPromiseArr.length; i++) {
        const val = await fnPromiseArr[i]();
      }
    }
  }

  private async created() {
    this.service = this.$opensilex.getService("opensilex.GermplasmService");
    //Existing duplicatable rdf property stuff
    let ontologyService: OntologyService = this.$opensilex.getService("opensilex.OntologyService");
    let existingPropertiesRessourceTree: Array<ResourceTreeDTO> = await ontologyService.getSubPropertiesOf(Oeso.GERMPLASM_TYPE_URI, Oeso.HAS_PARENT_GERMPLASM, false).then(http => {
      return http.response.result;
    }).catch(this.$opensilex.errorHandler);
    this.existingDuplicatablePropertiesNameList = [];
    existingPropertiesRessourceTree.forEach(resourceTree => {
          this.existingDuplicatableRdfAttributesObjects.push({
            id: resourceTree.uri,
            label: resourceTree.name});
            this.existingDuplicatablePropertiesNameList.push(resourceTree.name);
        }
    );
    //Add stuff to existing property string rule (to prevent duplicates)
    if(this.existingRdfAttributesStringRule===""){
      this.existingRdfAttributesStringRule = "existingProperty:" + this.existingDuplicatablePropertiesNameList.toString();
    }else{
      this.existingRdfAttributesStringRule = this.existingRdfAttributesStringRule + "," + this.existingDuplicatablePropertiesNameList.toString();
    }
  }

  /**
   *
   * @param columnName
   * white spaces will be ignored (example parentA will be registered as parent A)
   */
  private tryToGetExistingPropertyFromColumnName(columnName: string): SelectableItem{
    let filtered:Array<SelectableItem> = this.existingDuplicatableRdfAttributesObjects.filter(
        e => e.label.toLowerCase().replaceAll(" ", "") == columnName.toLowerCase().replaceAll(" ", "")
    );
    if(filtered.length>0){
      return filtered[0];
    }
    return null;
  }


  private callCreateGermplasmService(
    form: GermplasmCreationDTO,
    index: number,
    onlyChecking: boolean
  ) {
    return () =>
      new Promise((resolve, reject) => {
        this.service
          .createGermplasm(onlyChecking, form)
          .then((http: HttpResponse<OpenSilexResponse<any>>) => {
            if (onlyChecking) {
              this.tabulator.updateData([
                {
                  rowNumber: index,
                  checkingStatus: this.$t(
                    "GermplasmTable.checkingStatusMessage"
                  ),
                  status: "OK",
                },
              ]);
            } else {
              this.tabulator.updateData([
                {
                  rowNumber: index,
                  insertionStatus: this.$t(
                    "GermplasmTable.insertionStatusMessage"
                  ),
                  status: "OK",
                  uri: http.response.result,
                },
              ]);
            }

            let row = this.tabulator.getRow(index);
            row.reformat();
            this.okNumber = this.okNumber + 1;
            this.progressValue = this.progressValue + 1;

            resolve(http);
          })
          .catch((error) => {
            let errorMessage: string;
            let failure = true;
            try {
              if (error.response.result.translationKey) {
                errorMessage = this.$t(error.response.result.translationKey, error.response.result.translationValues).toString();
              } else {
                errorMessage = error.response.result.message;
              }
              failure = false;
            } catch (e1) {
              failure = true;
            }

            if (failure) {
              try {
                errorMessage =
                  error.response.metadata.status[0].exception.details;
              } catch (e2) {
                if(error.response[0].message.includes("is not a valid URI")){
                  errorMessage= this.$t("component.common.errors.not-a-valid-uri").toString();
                } else {
                  errorMessage = this.$t("component.common.errors.unexpected-error").toString();
                }

              }
            }

            if (onlyChecking) {
              this.tabulator.updateData([
                {
                  rowNumber: index,
                  checkingStatus: errorMessage,
                  status: "NOK",
                },
              ]);
            } else {
              this.tabulator.updateData([
                {
                  rowNumber: index,
                  insertionStatus: errorMessage,
                  status: "NOK",
                },
              ]);
            }

            let row = this.tabulator.getRow(index);
            row.reformat();
            this.errorNumber = this.errorNumber + 1;
            this.progressValue = this.progressValue + 1;
            resolve(error);
          });
      });
  }

  /**
   * Called when we upload a csv. Verifies that each germplasm has a unique uri.
   * Initialises this.newColumns to all from the csv that were not already present in this.tableColumns.
   */
  private uploaded(data) {
    if (data.length > 1000) {
      alert(this.$t("GermplasmTable.alertFileSize"));
    } else {
      this.newColumns = [];
      var uniqueURIs = [];
      let insertionOK = true;

      let uriColIndex = (data[0] as Array<string>).indexOf("uri");
      let nameColIndex = (data[0] as Array<string>).indexOf("name");

      //Perform checks
      for (let rowIndex = 1; rowIndex < data.length; rowIndex++) {
        let currentUri = data[rowIndex][uriColIndex];
        let currentName = data[rowIndex][nameColIndex];
        if (currentName === "") {
          alert(
              this.$t("GermplasmTable.missingName") + " " + rowIndex
          );
          insertionOK = false;
          break;
        }
        if (currentUri !== "") {
          if (uniqueURIs.indexOf(currentUri) === -1) {
            uniqueURIs.push(currentUri);
          } else {
            insertionOK = false;
            alert(
                this.$t("GermplasmTable.alertDuplicateURI") +
                " " +
                rowIndex +
                ", uri= " +
                currentUri
            );
            break;
          }
        }
      }

      if (insertionOK) {
        //Prepare pop-up to add any required columns

        let csvColumns : string[] = data[0];
        let tableColNames = [];

        for (let colNumber in this.tableColumns) {
          tableColNames.push(this.tableColumns[colNumber].field);
        }
        let duplicatableProperyOccurencesInCsv: Map<string, number> = new Map<string, number>();
        //Add new column to metadata if it is not already present.
        //If the column is one that can be duplicated, then calculate number of times it comes up
        for(let csvCol of csvColumns){
          let existingDuplicatableProperty: SelectableItem = this.tryToGetExistingPropertyFromColumnName(csvCol);
          if(existingDuplicatableProperty){
            if(duplicatableProperyOccurencesInCsv.has(existingDuplicatableProperty.label)){
              duplicatableProperyOccurencesInCsv.set(existingDuplicatableProperty.label, duplicatableProperyOccurencesInCsv.get(existingDuplicatableProperty.label) + 1);
            }else{
              duplicatableProperyOccurencesInCsv.set(existingDuplicatableProperty.label, 1);
            }
          }else{
            if(!tableColNames.includes(csvCol)){
              this.newColumns.push(csvCol);
              if(this.newColumnModalCheckboxData.filter((e) => e.value===csvCol).length === 0){
                this.newColumnModalCheckboxData.push({"value": csvCol, "name": csvCol});
              }
            }
          }
        }
        //Calculate the amount of missing columns for the duplicatable ones
        for(let duplicatablePropertyNameInCsv of duplicatableProperyOccurencesInCsv.keys()){
          let duplicatableColumn : SelectableItem = this.tryToGetExistingPropertyFromColumnName(duplicatablePropertyNameInCsv);
          let tableColumnQuantity: number = this.addedDuplicatableRdfColumnsToMaxUsedIdMap.get(duplicatableColumn.id);
          if(!tableColumnQuantity){
            tableColumnQuantity = 0;
          }
          let missingColumnQuantity: number = duplicatableProperyOccurencesInCsv.get(duplicatablePropertyNameInCsv) - tableColumnQuantity;
          if(missingColumnQuantity > 0){
            this.newColumnModalCheckboxData.push(
                {"value": duplicatableColumn.label,
                  "name": duplicatableColumn.label + " (x" + missingColumnQuantity + ")"
                });
            for(let index = 0; index < missingColumnQuantity; index++){
              this.newColumns.push(duplicatableColumn.label);
            }
          }
        }

        //Reconstruct data as a json before sending it to tabulator
        let dataInJsonFormat = [];
        for(let rowIndex = 1 ; rowIndex<data.length; rowIndex++){
          let nextJsonRow = {};
          nextJsonRow["rowNumber"] = rowIndex;
          //Save indexes of duplicatable columns
          let currentDuplicatableColumnsIndexes = {};
          duplicatableProperyOccurencesInCsv.forEach((value, key)=>{
            currentDuplicatableColumnsIndexes[this.tryToGetExistingPropertyFromColumnName(key).id] = 0;
          });
          for(let colIndex in csvColumns){
            let colName = csvColumns[colIndex];
            let existingDuplcatableProperty = this.tryToGetExistingPropertyFromColumnName(colName);
            if(existingDuplcatableProperty){
              nextJsonRow[existingDuplcatableProperty.id + currentDuplicatableColumnsIndexes[existingDuplcatableProperty.id]] = data[rowIndex][colIndex]
              currentDuplicatableColumnsIndexes[existingDuplcatableProperty.id]++;
            }else{
              //Note : for now this means that any column that isn't listed as duplicatable will simply be overwritten if there are multiple columns
              nextJsonRow[colName] = data[rowIndex][colIndex]
            }
          }
          dataInJsonFormat.push(nextJsonRow);
        }
        this.csvUploadedData = dataInJsonFormat;
        if (this.newColumns.length > 0) {
          this.tableData = [];
          this.showSelectNewColumnsPopUp();
        } else {
          this.tableData = dataInJsonFormat;
        }
      }
    }
  }
  //endregion

}
</script>

<style scoped lang="scss">
.requiredOnCondition {
  color: blue;
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
  color: #f00;
}

.tabulator .tabulator-header .tabulator-col {
  border-right: 1px solid #dee2e6;
}

.tabulator .tabulator-table .tabulator-row .tabulator-cell {
  border-right: 1px solid #dee2e6;
  height: 30px;
}

// .tabulator .tabulator-header .tabulator-row  {
//   height: 30px;
// }

.loadCsvButton {
  float: right;
  margin-right: 10px;
}
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
    progressValue: Progress
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
    newColumns: Supplementary columns
    newColumnsHelp: Select the columns to add
    missingHeader: Uri or name columns missing
    toggleAll: Select all / Unselect all

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
    progressValue: Progression
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
    infoMessageGermplReady: ressources génétiques prêtes à être insérées
    infoMessageErrors: erreurs
    infoMessageEmptyLines: lignes vides
    infoMessageGermplInserted: ressources génétiques insérées
    infoProposeInsertion: N'oubliez pas de cliquer sur le bouton Insérer afin de finaliser l'insertion des ressources (bouton situé ci-dessous ou au-dessus du tableau)
    checkingStatusMessage: validé
    insertionStatusMessage: créé
    seeErrorLines: See lines
    seeAll : see all 
    infoMandatoryFields: Il est obligatoire de renseigner au moins une des 3 colonnes URI de l'espèce, URI de la varieté ou URI de l'Accession.
    alertDuplicateURI: Le fichier comporte un doublon d'uri à la ligne 
    alertFileSize: Le fichier contient trop de ligne, 1000 lignes maximum
    missingName: Le nom n'est pas renseigné à la ligne
    newColumns: Colonnes additionnelles
    newColumnsHelp: Cochez les colonnes à ajouter
    missingHeader: Colonne uri ou nom manquant
    toggleAll: Tout sélectionner / Tout désélectionner
</i18n>
