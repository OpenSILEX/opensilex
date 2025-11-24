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

    <b-input-group id="tableOptions" class="mt-2 mb-2" size="sm">
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
      >{{ $t("GermplasmTable.resetTable") }}
      </b-button
      >
      <b-button class="mb-2 mr-2" @click="onAddRowBtnClick" variant="outline-dark">{{
          $t("GermplasmTable.addRow")
        }}
      </b-button>
      <b-button
          class="mb-2 mr-2"
          @click="onAddColumnBtnClick"
          variant="outline-dark"
      >{{ $t("GermplasmTable.addColumn") }}
      </b-button>
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

    <b-input-group id="tableActions" size="sm">
      <b-button
          class="mb-2 mr-2 greenThemeColor"
          @click="onCheckBtnClick()"
          v-bind:disabled="disableCheck"
      >{{ $t("GermplasmTable.check") }}
      </b-button
      >
      <b-button
          class="mb-2 mr-2"
          @click="onInsertDataBtnClick"
          variant="success"
          v-bind:disabled="disableInsert"
      >{{ $t("GermplasmTable.insert") }}
      </b-button>
      <opensilex-FormInputLabelHelper
          :helpMessage="$t('GermplasmTable.upsertHelpMessage')"
      ></opensilex-FormInputLabelHelper>
    </b-input-group>

    <div class="legend">
      <p>{{$t('GermplasmTable.legend')}} </p>
    </div>

    <div ref="table"></div>

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

    <b-modal
        ref="errorDetailsModal"
        centered
        hide-footer
        :title="$t('GermplasmTable.errorModalTitle')"
    >
      <ul>
        <li v-for="error in errorsToShowInModal" :key="error.length">
          {{ error }}
        </li>
      </ul>
    </b-modal>
  </div>
</template>

<script lang="ts">
import {Component, Vue, Watch, Ref} from "vue-property-decorator";
import {GermplasmCreationDTO, GermplasmService} from "opensilex-core/index";

//@ts-ignore
import JsonCSV from "vue-json-csv";
Vue.component("downloadCsv", JsonCSV);

import {TabulatorFull as Tabulator} from 'tabulator-tables';
import Oeso from "../../ontologies/Oeso";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {OntologyService} from "opensilex-core/api/ontology.service";
import { SelectableItem } from '../common/forms/FormSelector.vue';
import {RDFObjectRelationDTO} from "opensilex-core/model/rDFObjectRelationDTO";
import VueRouter from "vue-router";
import {OpenSilexStore} from "../../models/Store";
import VueI18n from "vue-i18n";
import {User} from "../../models/User";
import {ObjectNamedResourceDTO} from "opensilex-core/model/objectNamedResourceDTO";
import {MultipleErrorDTO} from "opensilex-core/model/multipleErrorDTO";
import {URIsListPostDTO} from "opensilex-core/model/uRIsListPostDTO";
import GermplasmTableDataRow from "./GermplasmTableDataRow";
import FormInputLabelHelper from "../common/forms/FormInputLabelHelper.vue";
import { helpers } from "@turf/turf";

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
  @Ref("colModal") readonly colModal!: any;
  @Ref("table") readonly table!: any;
  @Ref("newcolsModal") readonly newcolsModal!: any;
  @Ref("helpModal") readonly helpModal!: any;
  @Ref("errorDetailsModal") readonly errorDetailsModal!: any;
  //#endregion

  //#region Data
  onlyChecking: boolean = true;
  suppColumnsNames: Array<string>;

  private newColumns: string[] = [];
  private newColumnsselected: string[] = []; // Must be an array reference!
  private newColumnModalCheckboxData: NewColumnCheckboxData[] = [];

  /**
   * Map where keys are non obligatary duplicatable columns that already exist.
   * A key is added the first time this column is added (duplicates aloud). The keys are equal to the JsonParams in GermplasmCreationDTO, example : hasParentGermplasmA
   * The values represent the current id for the tabulator, a digit is added each time.
   * Example hasParentGermplasmM0, hasParentGermplasmF1, etc...
   */
  private addedDuplicatableRdfColumnsToMaxUsedIdMap: Map<string, number> = new Map<string, number>();

  private existingDuplicatableRdfAttributesObjects: Array<SelectableItem> = [];

  private existingRdfAttributesStringRule: string = "";

  private existingDuplicatablePropertiesNameList: string[] = [];

  private tabulator: Tabulator = null;

  private tableData :Array<GermplasmTableDataRow> = [];

  @Watch("tableData", {deep: true})
  newData(value: Array<GermplasmTableDataRow>) {
    this.tabulator.replaceData(value.map(row => {
      return row.getData();
    }));
  }

  private csvUploadedData = [];

  private tableColumns: any[] = [];

  private insertionStatus = [];

  private jsonForTemplate = [];

  private readonly checkBoxOptions = [
    { text: this.$t("GermplasmTable.checkBoxOptionsAll"), value: "all" },
    { text: this.$t("GermplasmTable.checkBoxOptionsAllErrors"), value: "NOK" },
  ];

  /**
   * Filter which lines are shown in the table.
   * "all" shows all lines
   * "NOK" shows only lines with errors.
   */
  private filter = "all";

  private disableInsert: boolean = false;
  private disableCheck: boolean = false;
  private checkedLines: number = 0;

  private langUnwatcher;

  private errorsToShowInModal: Array<string> = [];
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
    const newGermplasm = new GermplasmTableDataRow(this.tableData.length);
    this.tableData.push(newGermplasm,);
    this.tabulator.addRow(newGermplasm.getData());
  }

  private onCheckBtnClick() {
    this.onlyChecking = true;
    this.tabulator.showColumn("checkingStatus");
    this.tabulator.hideColumn("insertionStatus");
    this.disableInsert = false;
    this.checkedLines++;
    this.upsertOrCheckData();
  }

  private onResetTableBtnClick() {
    this.filter = "all";
    this.updateColumns();
  }

  private onInsertDataBtnClick() {
    this.insertData();
  }

  private onFilterLinesInput() {
    if (this.filter != "all") {
      this.tabulator.setFilter("status", "=", this.filter);
    } else {
      this.tabulator.clearFilter(true);
    }
  }

  private onNewColsModalHidden() {
    this.filter = "all";
    this.tabulator.clearData()
    this.tableData = []
    this.tableData = this.csvUploadedData.map((row, index) => {
      const germplasm = new GermplasmTableDataRow(index, row);
      return germplasm;
    });

    this.SetUpdateStatusForEachGermplasm();
  }

  private onSelectAllColumnSwitchChange(checked) {
    this.newColumnsselected = checked ? this.newColumnModalCheckboxData.slice().map(column => column.value) : []
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

  private async created() {
    this.service = this.$opensilex.getService("opensilex.GermplasmService");
    //Existing duplicatable rdf property stuff
    let ontologyService: OntologyService = this.$opensilex.getService("opensilex.OntologyService");
    let existingProperties: Array<ObjectNamedResourceDTO> = await ontologyService.getSubPropertiesOf(Oeso.GERMPLASM_TYPE_URI, Oeso.HAS_PARENT_GERMPLASM, false).then(http => {
      return http.response.result;
    }).catch(this.$opensilex.errorHandler);
    this.existingDuplicatablePropertiesNameList = [];
    existingProperties.forEach(property => {
          this.existingDuplicatableRdfAttributesObjects.push({
            id: property.uri,
            label: property.name
          });
          this.existingDuplicatablePropertiesNameList.push(property.name);
        }
    );
    //Add stuff to existing property string rule (to prevent duplicates)
    if (this.existingRdfAttributesStringRule === "") {
      this.existingRdfAttributesStringRule = "existingProperty:" + this.existingDuplicatablePropertiesNameList.toString();
    } else {
      this.existingRdfAttributesStringRule = this.existingRdfAttributesStringRule + "," + this.existingDuplicatablePropertiesNameList.toString();
    }
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
    let addedNonDuplicatableCols: string[] = [];
    for (let col of this.newColumns) {
      if (this.newColumnsselected.includes(col)) {
        let existingProperty = this.tryToGetExistingPropertyFromColumnName(col);
        if (existingProperty) {
          this.addExistingColumn(existingProperty.label, existingProperty.id, existingProperty.id)
        } else {
          if (!addedNonDuplicatableCols.includes(col)) {
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
    this.suppColumnsNames = [];

    let idCol = {
      title: "",
      field: "rowNumber",
      visible: true,
      formatter: "rownum",
    };

    let statusCol = {title: "status", field: "status", visible: false};

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
    let isPublicCol = {
      title:this.$t("GermplasmTable.is_public"),
      field: "is_public",
      visible: true,
      editor: "select",
      editorParams:{
        values: {
          true: "true",
          false: "false",
        }
      }
    };
    let checkingStatusCol = {
      title: this.$t("GermplasmTable.checkingStatus"),
      field: "checkingStatus",
      visible: false,
      editor: false,
      minWidth: 400,
      formatter: this.errorFormaterFunction,
    };
    let insertionStatusCol = {
      title: this.$t("GermplasmTable.insertionStatus"),
      field: "insertionStatus",
      visible: false,
      editor: false,
      minWidth: 400,
      formatter: this.errorFormaterFunction,
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
        isPublicCol,
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
        checkingStatusCol,
        insertionStatusCol,
        subtaxaCol,
        codeVar,
        speciesCol,
        instituteCol,
        websiteCol,
        commentCol,
        isPublicCol,
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
        checkingStatusCol,
        insertionStatusCol,
        subtaxaCol,
        codeAcc,
        speciesCol,
        varietyCol,
        instituteCol,
        websiteCol,
        commentCol,
        isPublicCol,
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
        checkingStatusCol,
        insertionStatusCol,
        synonymCol,
        codeLot,
        speciesCol,
        varietyCol,
        accessionCol,
        instituteCol,
        websiteCol,
        productionYearCol,
        commentCol,
        isPublicCol,
      ];
    }

    //reset existingRdfAttributesStringRule to only contain the basic columns
    let tableStartingHeaderTitlesFields: string = this.tableColumns.map((col, index) => {
      return col.field + "," + this._removeSpanRequiredBlockFromTitle(col.title)
    }).toString();
    this.existingRdfAttributesStringRule = "existingProperty:" + tableStartingHeaderTitlesFields;

    this.tabulator = new Tabulator(this.table, {
      data: this.tableData.map(row => row.getData()), //link data to table
      reactiveData: true, //enable data reactivity
      columns: this.tableColumns, //define table columns
      layout: "fitData",
      layoutColumnsOnNewData: true,
      index: "rowNumber",
      height: "70vh",
      rowFormatter: function (row) {
        if (row.getData().status == "OK") {
          row.getElement().style.backgroundColor = "#a5e051";
        } else if (row.getData().status == "NOK") {
          row.getElement().style.backgroundColor = "#ed6661";
        } else if (row.getData().status == "UPDATE"){
          row.getElement().style.backgroundColor = "#4ba7cf";
        } else {
          row.getElement().style.backgroundColor = "#ffffff";
        }
      },
    });

    this.tabulator.on("dataProcessed", () => {
      this.$opensilex.hideLoader();
    });

    this.tabulator.on("cellEdited", (cell) => {
      // check if this is the uri column, if so, throw an error if the uri is not unique
      if (cell.getField() === "uri") {
        const uri = cell.getValue();
        const germplasm: GermplasmTableDataRow = this.tableData[cell.getRow().getData().index];
        if (uri == null || uri === "") {
          germplasm.setIsUpdate(false);
          return;
        } else {
          this.SetUpdateStatusForEachGermplasm();
        }
      }
    });

    // tabulator initialisation with empty lines when the table is built
    this.tabulator.on("tableBuilt", () => {
      this.tableData = [];
      this.addInitialXRows(5);
    });

    this.jsonForTemplate = [];
    //let jsonHeader = {};
    for (let i = 1; i < this.tableColumns.length; i++) {
      if (this.tableColumns[i].visible == true) {
        this.jsonForTemplate.push(this.tableColumns[i].field);
      }
    }
    this.checkedLines = 0;
    this.disableCheck = false;
    this.disableInsert = false;
  }

  private _removeSpanRequiredBlockFromTitle(title: string): string {
    let result = title;
    if (title.includes("<span")) {
      result = result.substring(0, title.indexOf("<span"));
    }
    return result;
  }

  private addInitialXRows(X) {
    for (let i = 0; i < X; i++) {
      this.tableData.push(new GermplasmTableDataRow(i));
    }
  }

  /**
   * Generates a new name in function of how many times this rdf column has been added, then adds it.
   */
  private addExistingColumn(columnName: string, propertyUri: string, positionTarget: string) {
    let currentMaxExcludedIndex: number = this.addedDuplicatableRdfColumnsToMaxUsedIdMap.get(propertyUri);
    if (currentMaxExcludedIndex == null) {
      this.addedDuplicatableRdfColumnsToMaxUsedIdMap.set(propertyUri, 1)
      currentMaxExcludedIndex = 0;
    } else {
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

  private addColumnToTabulator(columnLabel: string, columnID: string, positionTarget: string) {
    this.tabulator.addColumn(
        {title: columnLabel, field: columnID, editor: true},
        false,
        "comment"
    );
    //this.jsonForTemplate[0][columnID] = null;
    this.jsonForTemplate.push(columnLabel);
  }

  private insertData() {
    this.onlyChecking = false;
    this.tabulator.hideColumn("checkingStatus");
    this.tabulator.showColumn("insertionStatus");
    this.disableInsert = true;
    this.disableCheck = true;
    this.upsertOrCheckData();
  }

  private async upsertOrCheckData() {
    this.$opensilex.enableLoader();
    this.$opensilex.showLoader();

    let DTOs: Array<GermplasmCreationDTO> = await this.getDtosFromTableData();

    this.callUpsertService(DTOs)
        .finally(() => {
          this.$opensilex.hideLoader()
          this.$opensilex.disableLoader();
        })
  }

  private async callUpsertService(creationDtos: Array<GermplasmCreationDTO>) {
    return this.service.upsertGermplasms(this.onlyChecking, creationDtos)
        .then( () => {
          let successMessage = this.onlyChecking ? this.$t("GermplasmTable.successCheckMessage").toString() : this.$t("GermplasmTable.successUpsertMessage").toString();
          this.$opensilex.showSuccessToast(successMessage);

          this.setGermplasmsValidationStatus(false);
          this.filter = "all";
        })
        .catch( error => {
          if (error.response.status < 400 || error.response.status >= 500) {
            this.$opensilex.showErrorToast(this.$t("GermplasmTable.errorServerMessage").toString());
            return;
          }

          let errorMessage = this.onlyChecking ? this.$t("GermplasmTable.errorCheckMessage").toString() : this.$t("GermplasmTable.errorUpsertMessage").toString();
          this.$opensilex.showErrorToast(errorMessage);
          let errors: Array<MultipleErrorDTO> = error.response.result.errors;

          if (errors == null) { //if it's not a MultipleErrorResponse (should not happen)
            this.$opensilex.showErrorToast(this.$t("GermplasmTable.errorServerMessage").toString());
            return;
          }

          this.setGermplasmsValidationStatus(true);
          errors.forEach(errorDto => {

            let errorMessage = errorDto.errors.length > 1 ?
                this.$t("GermplasmTable.multipleErrorMessage", {count: errorDto.errors.length}).toString() :
                errorDto.errors[0];

            let rowIndex = errorDto.index;
            if (rowIndex != null) {
              const row = this.tableData[rowIndex]
              row.setErrors(errorDto.errors);
              row.setCheckingStatus(errorMessage);
              row.setInsertionStatus(errorMessage);
            }
          });

          this.filter = "NOK";
        })
  }

  /**
   * set the update status for each germplasm
   * This will trigger the rowFormatter to change the color of the row.
   */
  private setUpdateStatusForEachGermplasm(existingUris: string[]){
    this.tableData.forEach((data) => {
      if (data.getData().uri != null && this.$opensilex.includesUri(existingUris, data.getData().uri)) {
         data.setIsUpdate(true);
        } else {
          data.setIsUpdate(false);
        }
    });
  }

  /**
   * reset the status of each row to status value. Default is empty string.
   * This will trigger the rowFormatter to change the color of the row.
   * @param upsertWasCancelled if true, the insertion status will not be set to success.
   */
  private setGermplasmsValidationStatus(upsertWasCancelled: boolean): void {
    this.tableData.forEach((data) => {
      data.setIsValidated();
      data.setInsertionStatus(this.$t(
          upsertWasCancelled ? "GermplasmTable.cancelledUpsertRowMessage" : "GermplasmTable.successUpsertRowMessage"
      ).toString());
      data.setCheckingStatus(this.$t("GermplasmTable.successCheckRowMessage").toString());
    });
  }

  private async getExistingGermplasmsUri(): Promise<Array<string>>{
    let uris: URIsListPostDTO = {
      uris: this.tableData.map((row) => {
        return row.getData().uri as string;
      })
    }
    return ( await this.service.checkGermplasmsExist(uris) ).response.result
  }

  /**
   * After an API call, this function sets the update status for each germplasm (true or false).
   */
  private SetUpdateStatusForEachGermplasm(){
    this.getExistingGermplasmsUri()
        .then(uris => {
          this.setUpdateStatusForEachGermplasm(uris);
        });
  }

  private async getDtosFromTableData(): Promise<Array<GermplasmCreationDTO>> {
    let creationDtos: Array<GermplasmCreationDTO> = [];

    let dataToInsert = this.tabulator.getData();

    for (let idx = 0; idx < dataToInsert.length; idx++) {
      let form = {
        rdf_type: null,
        name: null,
        uri: null,
        species: null,
        variety: null,
        accession: null,
        institute: null,
        production_year: null,
        is_public: null,
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

      const isPublicRaw = dataToInsert[idx].is_public;

      if (isPublicRaw === undefined || isPublicRaw === null || isPublicRaw === "") {
        form.is_public = true;
      } else {
        form.is_public = String(isPublicRaw).trim().toLowerCase() === "true";
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
          (currentMaxIndexExcluded, extraRdfAttribute) => {
            for (let duplicationIndex: number = 0; duplicationIndex < currentMaxIndexExcluded; duplicationIndex++) {
              let currentTabulatorIdentifier = extraRdfAttribute + duplicationIndex;
              let currentValue = dataToInsert[idx][currentTabulatorIdentifier];
              if (currentValue != null && currentValue != "") {
                let nextRelationDTO: RDFObjectRelationDTO = {};
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
        continue;
      } else {
        creationDtos.push(form);
      }
    }
    return creationDtos;
  }

  /**
   *
   * @param columnName
   * white spaces will be ignored (example parentA will be registered as parent A)
   */
  private tryToGetExistingPropertyFromColumnName(columnName: string): SelectableItem {
    let filtered: Array<SelectableItem> = this.existingDuplicatableRdfAttributesObjects.filter(
        e => e.label.toLowerCase().replaceAll(" ", "") == columnName.toLowerCase().replaceAll(" ", "")
    );
    if (filtered.length > 0) {
      return filtered[0];
    }
    return null;
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
      let uniqueURIs = [];
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

        let csvColumns: string[] = data[0];
        let tableColNames = [];

        for (let colNumber in this.tableColumns) {
          tableColNames.push(this.tableColumns[colNumber].field);
        }
        let duplicatableProperyOccurencesInCsv: Map<string, number> = new Map<string, number>();
        //Add new column to metadata if it is not already present.
        //If the column is one that can be duplicated, then calculate number of times it comes up
        for (let csvCol of csvColumns) {
          let existingDuplicatableProperty: SelectableItem = this.tryToGetExistingPropertyFromColumnName(csvCol);
          if (existingDuplicatableProperty) {
            if (duplicatableProperyOccurencesInCsv.has(existingDuplicatableProperty.label)) {
              duplicatableProperyOccurencesInCsv.set(existingDuplicatableProperty.label, duplicatableProperyOccurencesInCsv.get(existingDuplicatableProperty.label) + 1);
            } else {
              duplicatableProperyOccurencesInCsv.set(existingDuplicatableProperty.label, 1);
            }
          } else {
            if (!tableColNames.includes(csvCol)) {
              this.newColumns.push(csvCol);
              if (this.newColumnModalCheckboxData.filter((e) => e.value === csvCol).length === 0) {
                this.newColumnModalCheckboxData.push({"value": csvCol, "name": csvCol});
              }
            }
          }
        }
        //Calculate the amount of missing columns for the duplicatable ones
        for (let duplicatablePropertyNameInCsv of duplicatableProperyOccurencesInCsv.keys()) {
          let duplicatableColumn: SelectableItem = this.tryToGetExistingPropertyFromColumnName(duplicatablePropertyNameInCsv);
          let tableColumnQuantity: number = this.addedDuplicatableRdfColumnsToMaxUsedIdMap.get(duplicatableColumn.id);
          if (!tableColumnQuantity) {
            tableColumnQuantity = 0;
          }
          let missingColumnQuantity: number = duplicatableProperyOccurencesInCsv.get(duplicatablePropertyNameInCsv) - tableColumnQuantity;
          if (missingColumnQuantity > 0) {
            this.newColumnModalCheckboxData.push(
                {
                  "value": duplicatableColumn.label,
                  "name": duplicatableColumn.label + " (x" + missingColumnQuantity + ")"
                });
            for (let index = 0; index < missingColumnQuantity; index++) {
              this.newColumns.push(duplicatableColumn.label);
            }
          }
        }

        //Reconstruct data as a json before sending it to tabulator
        let dataInJsonFormat = [];
        for (let rowIndex = 1; rowIndex < data.length-1; rowIndex++) {
          let nextJsonRow = {};
          nextJsonRow["rowNumber"] = rowIndex;
          //Save indexes of duplicatable columns
          let currentDuplicatableColumnsIndexes = {};
          duplicatableProperyOccurencesInCsv.forEach((value, key) => {
            currentDuplicatableColumnsIndexes[this.tryToGetExistingPropertyFromColumnName(key).id] = 0;
          });
          for (let colIndex in csvColumns) {
            let colName = csvColumns[colIndex];
            let existingDuplcatableProperty = this.tryToGetExistingPropertyFromColumnName(colName);
            if (existingDuplcatableProperty) {
              nextJsonRow[existingDuplcatableProperty.id + currentDuplicatableColumnsIndexes[existingDuplcatableProperty.id]] = data[rowIndex][colIndex]
              currentDuplicatableColumnsIndexes[existingDuplcatableProperty.id]++;
            } else {
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
          this.tableData = dataInJsonFormat.map( (row, index) =>  new GermplasmTableDataRow(index, row));
        }
      }
    }
    this.SetUpdateStatusForEachGermplasm();
  }

  /**
   * function triggered by the tabulator cell formatter. Allow to open error modal when clicking on the errors cell
   */
  private errorFormaterFunction(cell, formatterParams, onRendered) {
    // Use onRendered to attach the click event listener
    onRendered(() => {
      const rowData = this.tableData[cell.getRow().getData().index];

      if ( ! rowData.hasError()){
        return;
      }

      const button = cell.getElement().querySelector(".error-log");
      if (button) {
        button.addEventListener("click", () => {
          this.errorsToShowInModal = rowData.getErrors();
          this.errorDetailsModal.show();
        });
      }
    });

    // Return the button HTML
    return `<div class="error-log">${cell.getValue()||""}</div>`;
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

.loadCsvButton {
  float: right;
  margin-right: 10px;
}

.legend{
  background-color: rgba(0, 162, 140, 0.1);
  border: 1px solid rgba(0, 162, 140, 0.5);
  border-radius: 5px;
  padding: 0.4rem 0.8rem;
  margin: 0.5rem 0.5rem;

  p {
    margin: 0;
    padding: 0;
  }
}

</style>

<style lang="scss">
.error-log{
  color: blue;
}
</style>

<i18n>

en:
  GermplasmTable:
    name: Name
    fromSpecies: Species URI
    fromVariety: Variety URI
    fromAccession: Accession URI
    institute: Institute Code
    comment: Comment
    year: ProductionYear
    checkingStatus: Checking status
    insertionStatus: Insertion Status
    downloadTemplate: Dowload template
    resetTable: Reset table
    check: Check
    checkBoxOptionsAll: See all lines
    checkBoxOptionsAllErrors: See lines with errors
    errorCheckMessage: checking shows some errors, see the table for more details
    successCheckMessage: germplasms are ready to be inserted and/or updated
    successCheckRowMessage: ready to be inserted/updated
    insert: Create / update
    errorUpsertMessage: insertion/update shows some errors, nothing was inserted nor updated. See the table for more details
    successUpsertMessage: germplasms inserted and/or updated
    successUpsertRowMessage : inserted/updated
    cancelledUpsertRowMessage : insertion/update cancelled
    errorModalTitle: Germplasm contain following errors
    multipleErrorMessage: click here to see {count} errors for this germplasm
    addRow: Add Row
    accessionNumber: AccessionNumber
    varietyCode: Variety Code
    lotNumber: LotNumber
    synonyms: Synonyms
    subtaxa: Subtaxa
    website: Website
    is_public: Public
    addColumn: Add column
    checkingStatusMessage: ready
    insertionStatusMessage: created
    alertDuplicateURI: The file contains a duplicate uri at line
    alertFileSize: The file has too many lines, 1000 lines maximum
    missingName: The name is missing at line
    newColumns: Supplementary columns
    newColumnsHelp: Select the columns to add
    toggleAll: Select all / Unselect all
    errorServerMessage: A server error occurred, please contact your administrator for more information
    upsertHelpMessage: creates and updates the list of germplasms in a single operation (germplasms that will be updated are highlighted in blue).
    legend: Before Checking/Insertion<\br> if rows are white then the germplasm has no URI or a new URI. <br/> If rows are blue then the germplasm URI already exists and will be updated and not created. <br/> If rows are green then the germplasm URI already exists and no changes were made.

fr:
  GermplasmTable:
    name: Nom
    fromSpecies: URI de l'espèce
    fromVariety: URI de variété
    fromAccession: URI d'accession
    institute: Code institut
    comment: Commentaire
    year: Année
    checkingStatus: Statut
    insertionStatus: Statut
    downloadTemplate: Télécharger un gabarit
    resetTable: Vider tableau
    check: Valider
    checkBoxOptionsAll: Visualiser tout
    checkBoxOptionsAllErrors: Visualiser éléments avec erreurs
    errorCheckMessage: Des erreurs sont apparues lors de la validation, voir le tableau pour plus de détails
    successCheckMessage: Les ressources génétiques sont prêts à être insérées
    successCheckRowMessage: prête à être insérée/modifiée
    insert: Créer / modifier
    errorUpsertMessage: Des erreurs sont apparues lors de l'insertion/mise à jour, rien n'a été ni inséré ni mis à jour. Voir le tableau pour plus de détails
    successUpsertMessage: Les ressources génétiques ont été insérées et/ou mises à jour
    successUpsertRowMessage: insertion / modification réussie
    cancelledUpsertRowMessage: insertion / modification annulée
    errorModalTitle: Erreurs concernant les ressources génétiques
    multipleErrorMessage: Cliquez ici pour voir {count} erreurs pour cette ressource génétique
    addRow: Ajouter ligne
    accessionNumber: Code Accession
    varietyCode: Code Variété
    lotNumber: Code Lot
    synonyms: Synonymes
    subtaxa: Subtaxa
    website: Site web
    is_public : Publique
    addColumn: Ajouter colonne
    checkingStatusMessage: validé
    insertionStatusMessage: créé
    alertDuplicateURI: Le fichier comporte un doublon d'uri à la ligne
    alertFileSize: Le fichier contient trop de ligne, 1000 lignes maximum
    missingName: Le nom n'est pas renseigné à la ligne
    newColumns: Colonnes additionnelles
    newColumnsHelp: Cochez les colonnes à ajouter
    toggleAll: Tout sélectionner / Tout désélectionner
    errorServerMessage: Une erreur serveur est survenue, veuillez contacter votre administrateur pour plus d'informations
    upsertHelpMessage: crée et modifie la liste de ressources génétiques en une seule opération (les ressources génétiques qui seront mises à jour sont colorées en bleu).
    legend: Avant validation/insertion<\br> si les lignes sont blanches alors la ressource génétique n'a pas d'URI ou une nouvelle URI. <br/> Si les lignes sont bleues alors l'URI de la ressource génétique existe déjà et sera mise à jour et non créée. <br/> Si les lignes sont vertes alors l'URI de la ressource génétique existe déjà et aucune modification n'a été apportée.
</i18n>
