<template>
  <div class="card">
    <div>
      <n-button>{{ t("downloadTemplate") }}</n-button>
      <CSVInputFile
          v-on:updated="uploaded"
          :returnDataAsArrayOfArrays="true"
          :duplicatableHeaders="existingDuplicatablePropertiesNameList"
          :headersPresent="['name']"
      >
      </CSVInputFile>
      <n-button @click="onResetTableBtnClick">{{ t("resetTable") }}</n-button>
      <n-button @click="onAddRowBtnClick">{{ t("addRow") }}</n-button>
      <n-button @click="onAddColumnBtnClick">{{ t("addColumn") }}</n-button>
      <n-select
          v-if="checkedLines > 0"
          v-model="filter"
          :options="checkBoxOptions"
          @input="onFilterLinesInput()"
      >
      </n-select>
    </div>
    <div>
      <n-button
          @click="onCheckBtnClick()"
          :disabled="disableCheck"
      >{{ t("check") }}
      </n-button>
      <n-button
          @click="onInsertDataBtnClick"
          :disabled="disableInsert"
      >{{ t("insert") }}
      </n-button>
      <FormInputLabelHelper
          :helpMessage="t('upsertHelpMessage')"
          :label="t('upsertHelpMessage')">
      </FormInputLabelHelper>
    </div>

    <div class="divHelpMsg">
      <div class="update-status-legend">
        <div class="icon-and-legend">
          <div class="update-symbol"></div>
          {{ t('update-legend') }}
        </div>
        |
        <div class="icon-and-legend">
          <div class="create-symbol"></div>
          {{ t('create-legend') }}
        </div>
      </div>
      <div class="validation-status-legend">
        <div class="icon-and-legend">
          <div class="valid-legend"></div>
          {{ t('valid-legend') }}
        </div>
        |
        <div class="icon-and-legend">
          <div class="invalid-legend"></div>
          {{ t('invalid-legend') }}
        </div>
      </div>
    </div>

    <div ref="table"></div>
    <n-modal
        v-model:show="showErrorModal"
    >
      <ul>
        <li v-for="error in errorsToShowInModal" :key="error.length">
          {{ error }}
        </li>
      </ul>
    </n-modal>
  </div>
  <GermplasmAddColumnModal
      class="searchFilter"
      ref="colModal"
      :existingRdfAttributesObjects="existingDuplicatableRdfAttributesObjects"
      :existingRdfAttributesStringRule="existingRdfAttributesStringRule"
      @addingExistingColumn="onColModalAddExistingColumn"
      @addingUncontrolledColumn="onColModalAddingUncontrolledColumn"
  ></GermplasmAddColumnModal>

</template>

<script setup lang="ts">

import GermplasmAddColumnModal from "@/components/germplasm/creation/GermplasmAddColumnModal.vue";
import CSVInputFile from "@/components/common/forms/CSVInputFile.vue";
import FormInputLabelHelper from "@/components/common/forms/FormInputLabelHelper.vue";
import {useI18n} from "vue-i18n";
import {computed, inject, onMounted, ref, useTemplateRef} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {GermplasmService} from "opensilex-core/api/germplasm.service";
import {useStore} from "vuex";
import {useRouter} from "vue-router";
import {SelectableItem} from "@/components/variables/form/ModalFormSelector.vue";
import {Tabulator} from "tabulator-tables";
import GermplasmTableDataRow from "@/components/germplasm/creation/GermplasmTableDataRow";
import {OntologyService} from "opensilex-core/api/ontology.service";
import {ObjectNamedResourceDTO} from "opensilex-core/model/objectNamedResourceDTO";
import {GermplasmCreationDTO} from "opensilex-core/model/germplasmCreationDTO";
import {MultipleErrorDTO} from "opensilex-core/model/multipleErrorDTO";
import {URIsListPostDTO} from "opensilex-core/model/uRIsListPostDTO";
import {RDFObjectRelationDTO} from "opensilex-core/model/rDFObjectRelationDTO";
import {NModal, NSelect} from "naive-ui";
import Oeso from "@/ontologies/Oeso";


//#region Public
const props = defineProps<{
  germplasmType?: string
}>()
//#endregion

//#region function
const opensilex = inject<OpenSilexVuePlugin>("$opensilex")
const {t} = useI18n();
const store = useStore();
const router = useRouter();
const service = opensilex.getService<GermplasmService>("opensilex.GermplasmService");

const checkBoxOptions = computed(() => [
  {text: t("checkBoxOptionsAll"), value: "all"},
  {text: t("checkBoxOptionsAllErrors"), value: "NOK"},
]);
const onlyChecking = ref(true);
const suppColumnsNames = ref<string[]>([]);
const newColumns = ref<string[]>([]);
const newColumnsSelected = ref<string[]>([]);
const newColumnModalCheckboxData = ref<NewColumnCheckboxData[]>([]);
const addedDuplicatableRdfColumnsToMaxUsedIdMap = ref(new Map<string, number>());
const existingDuplicatableRdfAttributesObjects = ref<SelectableItem[]>([]);
const existingRdfAttributesStringRule = ref("");
const existingDuplicatablePropertiesNameList = ref<string[]>([]);
const tableData = ref<GermplasmTableDataRow[]>([]);
const csvUploadedData = ref([]);
const tableColumns = ref([]);
const jsonForTemplate = ref([]);
const filter = ref("all");
const checkedLines = ref(0);
const showErrorModal = ref(false);
const disableInsert = ref(false);
const disableCheck = ref(false);
const errorsToShowInModal = ref<string[]>([]);

let tabulator: Tabulator | undefined = undefined;

const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);

const table = useTemplateRef("table")

//#region Event handlers
function onAddColumnBtnClick() {
  // colModal.value.show();
}

function onColModalAddExistingColumn(columnName: string, propertyUri: string) {
  // colModal.value.hide();
  // this.addExistingColumn(columnName, propertyUri, "comment");
}

function onColModalAddingUncontrolledColumn(columnName: string) {
  // colModal.value.hide();
  // this.addNonExistingColumn(columnName, "comment");
}

function onAddRowBtnClick() {
  const newGermplasm = new GermplasmTableDataRow(tableData.value.length);
  tableData.value.push(newGermplasm,);
  tabulator.addRow(newGermplasm.getData());
}

function onCheckBtnClick() {
  onlyChecking.value = true;
  tabulator.showColumn("checkingStatus");
  tabulator.hideColumn("insertionStatus");
  disableInsert.value = false;
  checkedLines.value++;
  upsertOrCheckData();
}

function onResetTableBtnClick() {
  filter.value = "all";
  updateColumns();
}

function onInsertDataBtnClick() {
  insertData();
}

function onFilterLinesInput() {
  if (filter.value != "all") {
    tabulator.setFilter("status", "=", filter.value);
  } else {
    tabulator.clearFilter(true);
  }
}

function onNewColsModalHidden() {
  filter.value = "all";
  tabulator.clearData()
  tableData.value = []
  tableData.value = csvUploadedData.value.map((row, index) => {
    const germplasm = new GermplasmTableDataRow(index, row);
    return germplasm;
  });

  SetUpdateStatusForEachGermplasm();
}

function onSelectAllColumnSwitchChange(checked) {
  newColumnsSelected.value = checked ? newColumnModalCheckboxData.value.slice().map(column => column.value) : []
}

//#endregion


onMounted(async () => {
  //Existing duplicatable rdf property stuff
  let ontologyService: OntologyService = opensilex.getService("opensilex.OntologyService");
  let existingProperties: Array<ObjectNamedResourceDTO> = await ontologyService.getSubPropertiesOf(Oeso.GERMPLASM_TYPE_URI, Oeso.HAS_PARENT_GERMPLASM, false).then(http => {
    return http.response.result;
  }).catch(opensilex.errorHandler);
  existingDuplicatablePropertiesNameList.value = [];
  existingProperties.forEach(property => {
        existingDuplicatableRdfAttributesObjects.value.push({
          id: property.uri,
          label: property.name
        });
        existingDuplicatablePropertiesNameList.value.push(property.name);
      }
  );
  //Add stuff to existing property string rule (to prevent duplicates)
  if (existingRdfAttributesStringRule.value === "") {
    existingRdfAttributesStringRule.value = "existingProperty:" + existingDuplicatablePropertiesNameList.value.toString();
  } else {
    existingRdfAttributesStringRule.value = existingRdfAttributesStringRule.value + "," + existingDuplicatablePropertiesNameList.value.toString();
  }
});

//endregion

//#region function methods
function showSelectNewColumnsPopUp() {
  // newcolsModal.value.show();
}

function addNewColumnsCancel() {
  // this.$opensilex.showLoader();
  // newcolsModal.value.hide();
}


/**
 * Adds a set of new metadata columns from this.newColumnsselected and this.newColumns.
 * Only adds duplicates if the header is recognized as duplicable.
 * Data is loaded in the tabulator only when the modal is closed. This ensures better performance because adding column in a fulfilled tabulator is a heavy process.
 */
function addNewColumns() {
  // this.$opensilex.showLoader()
  // colModal.value.hide();
  // let addedNonDuplicatableCols: string[] = [];
  // for (let col of this.newColumns) {
  //   if (newColumnsselected.value.includes(col)) {
  //     let existingProperty = this.tryToGetExistingPropertyFromColumnName(col);
  //     if (existingProperty) {
  //       this.addExistingColumn(existingProperty.label, existingProperty.id, existingProperty.id)
  //     } else {
  //       if (!addedNonDuplicatableCols.includes(col)) {
  //         this.addNonExistingColumn(col, col);
  //         addedNonDuplicatableCols.push(col);
  //       }
  //     }
  //   }
  // }
  // this.newColumns = [];
  // this.newColumnsselected = [];
  // newcolsModal.value.hide();
}

/**
 * Looks at the type of germplasm (species, variety, etc...) to set this.tableColumns.
 * Then initialises this.tabulator
 */
function updateColumns() {
  suppColumnsNames.value = [];

  let idCol = {
    title: "",
    field: "rowNumber",
    visible: true,
    formatter: idFormatterFunction,
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
    title: t("name") + '<span class="required">*</span>',
    field: "name",
    visible: true,
    editor: true,
    minWidth: 150,
  };
  let synonymCol = {
    title: t("synonyms"),
    field: "synonyms",
    visible: true,
    editor: true,
  };
  let subtaxaCol = {
    title: t("subtaxa"),
    field: "subtaxa",
    visible: true,
    editor: true,
  };
  let speciesCol = {
    title:
        t("fromSpecies") +
        '<span class="requiredOnCondition">*</span>',
    field: "species",
    visible: true,
    editor: true,
  };
  let varietyCol = {
    title:
        t("fromVariety") +
        '<span class="requiredOnCondition">*</span>',
    field: "variety",
    visible: true,
    editor: true,
  };
  let accessionCol = {
    title:
        t("fromAccession") +
        '<span class="requiredOnCondition">*</span>',
    field: "accession",
    visible: true,
    editor: true,
  };
  let instituteCol = {
    title: t("institute"),
    field: "institute",
    visible: true,
    editor: true,
  };
  let websiteCol = {
    title: t("website"),
    field: "website",
    visible: true,
    editor: true,
  };
  let productionYearCol = {
    title: t("year"),
    field: "productionYear",
    visible: true,
    editor: true,
  };
  let commentCol = {
    title: t("comment"),
    field: "comment",
    visible: true,
    editor: true,
  };
  let isPublicCol = {
    title: t("is_public"),
    field: "is_public",
    visible: true,
    editor: "select",
    editorParams: {
      values: {
        true: "true",
        false: "false",
      }
    }
  };
  let checkingStatusCol = {
    title: t("checkingStatus"),
    field: "checkingStatus",
    visible: false,
    editor: false,
    minWidth: 400,
    formatter: errorFormaterFunction,
  };
  let insertionStatusCol = {
    title: t("insertionStatus"),
    field: "insertionStatus",
    visible: false,
    editor: false,
    minWidth: 400,
    formatter: errorFormaterFunction,
  };

  if (Oeso.checkURIs(props.germplasmType, Oeso.SPECIES_TYPE_URI)) {
    tableColumns.value = [
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
      Oeso.checkURIs(props.germplasmType, Oeso.VARIETY_TYPE_URI)
  ) {
    let codeVar = {
      title: t("varietyCode"),
      field: "code",
      visible: true,
      editor: true,
    };
    tableColumns.value = [
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
      Oeso.checkURIs(props.germplasmType, Oeso.ACCESSION_TYPE_URI)
  ) {
    let codeAcc = {
      title: t("accessionNumber"),
      field: "code",
      visible: true,
      editor: true,
    };
    tableColumns.value = [
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
      title: t("lotNumber"),
      field: "code",
      visible: true,
      editor: true,
    };
    tableColumns.value = [
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
  let tableStartingHeaderTitlesFields: string = tableColumns.value.map((col, index) => {
    return col.field + "," + _removeSpanRequiredBlockFromTitle(col.title)
  }).toString();
  existingRdfAttributesStringRule.value = "existingProperty:" + tableStartingHeaderTitlesFields;

  tabulator = new Tabulator(table, {
    data: tableData.value.map(row => row.getData()), //link data to table
    reactiveData: true, //enable data reactivity
    columns: tableColumns.value, //define table columns
    layout: "fitData",
    layoutColumnsOnNewData: true,
    index: "rowNumber",
    height: "70vh",
    rowFormatter: function (row) {
      if (row.getData().status == "OK") {
        row.getElement().style.backgroundColor = "#a5e051";
      } else if (row.getData().status == "NOK") {
        row.getElement().style.backgroundColor = "#ed6661";
      }
    },
  });

  tabulator.on("dataProcessed", () => {
    opensilex.hideLoader();
  });

  tabulator.on("cellEdited", (cell) => {
    // check if this is the uri column, if so, throw an error if the uri is not unique
    if (cell.getField() === "uri") {
      const uri = cell.getValue();
      const germplasm: GermplasmTableDataRow = tableData[cell.value.getRow().getData().index];
      if (uri == null || uri === "") {
        germplasm.setIsUpdate(false);
        return;
      } else {
        SetUpdateStatusForEachGermplasm();
      }
    }
  });

  // tabulator initialisation with empty lines when the table is built
  tabulator.on("tableBuilt", () => {
    tableData.value = [];
    addInitialXRows(5);
  });

  jsonForTemplate.value = [];
  //let jsonHeader = {};
  for (let i = 1; i < tableColumns.value.length; i++) {
    if (tableColumns.value[i].visible == true) {
      jsonForTemplate.value.push(tableColumns.value[i].field);
    }
  }
  checkedLines.value = 0;
  disableCheck.value = false;
  disableInsert.value = false;
}

function _removeSpanRequiredBlockFromTitle(title: string): string {
  let result = title;
  if (title.includes("<span")) {
    result = result.substring(0, title.indexOf("<span"));
  }
  return result;
}

function addInitialXRows(X) {
  for (let i = 0; i < X; i++) {
    tableData.value.push(new GermplasmTableDataRow(i));
  }
}

/**
 * Generates a new name in function of how many times this rdf column has been added, then adds it.
 */
function addExistingColumn(columnName: string, propertyUri: string, positionTarget: string) {
  let currentMaxExcludedIndex: number = addedDuplicatableRdfColumnsToMaxUsedIdMap.value.get(propertyUri);
  if (currentMaxExcludedIndex == null) {
    addedDuplicatableRdfColumnsToMaxUsedIdMap.value.set(propertyUri, 1)
    currentMaxExcludedIndex = 0;
  } else {
    addedDuplicatableRdfColumnsToMaxUsedIdMap.value.set(propertyUri, currentMaxExcludedIndex + 1);
  }
  addColumnToTabulator(columnName, propertyUri + currentMaxExcludedIndex, positionTarget);
}

/**
 * Adds a new column, will be stored in Mongo.
 * Does a check to make sure there aren't any duplications
 */
function addNonExistingColumn(columnName: string, positionTarget: string) {
  existingRdfAttributesStringRule.value = existingRdfAttributesStringRule.value + "," + columnName;
  addColumnToTabulator(columnName, columnName, positionTarget);
  suppColumnsNames.value.push(columnName);
}

function addColumnToTabulator(columnLabel: string, columnID: string, positionTarget: string) {
  tabulator.addColumn(
      {title: columnLabel, field: columnID, editor: true},
      false,
      "comment"
  );
  //this.jsonForTemplate[0][columnID] = null;
  jsonForTemplate.value.push(columnLabel);
}

function insertData() {
  onlyChecking.value = false;
  tabulator.hideColumn("checkingStatus");
  tabulator.showColumn("insertionStatus");
  disableInsert.value = true;
  disableCheck.value = true;
  upsertOrCheckData();
}

async function upsertOrCheckData() {
  opensilex.enableLoader();
  opensilex.showLoader();

  let DTOs: Array<GermplasmCreationDTO> = await getDtosFromTableData();

  callUpsertService(DTOs)
      .finally(() => {
        opensilex.hideLoader()
        opensilex.disableLoader();
      })
}

async function callUpsertService(creationDtos: Array<GermplasmCreationDTO>) {
  return service.upsertGermplasms(onlyChecking.value, creationDtos)
      .then(() => {
        let successMessage = onlyChecking.value ? t("successCheckMessage").toString() : t("successUpsertMessage").toString();
        opensilex.showSuccessToast(successMessage);

        setGermplasmsValidationStatus(false);
        filter.value = "all";
      })
      .catch(error => {
        if (error.response.status < 400 || error.response.status >= 500) {
          opensilex.showErrorToast(t("errorServerMessage").toString());
          return;
        }

        let errorMessage = onlyChecking.value ? t("errorCheckMessage").toString() : t("errorUpsertMessage").toString();
        opensilex.showErrorToast(errorMessage);
        let errors: Array<MultipleErrorDTO> = error.response.result.errors;

        if (errors == null) { //if it's not a MultipleErrorResponse (should not happen)
          opensilex.showErrorToast(t("errorServerMessage").toString());
          return;
        }

        setGermplasmsValidationStatus(true);
        errors.forEach(errorDto => {

          let errorMessage = errorDto.errors.length > 1 ?
              t("multipleErrorMessage", {count: errorDto.errors.length}).toString() :
              errorDto.errors[0];

          let rowIndex = errorDto.index;
          if (rowIndex != null) {
            const row = tableData.value[rowIndex]
            row.setErrors(errorDto.errors);
            row.setCheckingStatus(errorMessage);
            row.setInsertionStatus(errorMessage);
          }
        });
      })
}

/**
 * set the update status for each germplasm
 * This will trigger the rowFormatter to change the color of the row.
 */
function setUpdateStatusForEachGermplasm(existingUris: string[]) {
  tableData.value.forEach((data) => {
    if (data.getData().uri != null && opensilex.includesUri(existingUris, data.getData().uri)) {
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
function setGermplasmsValidationStatus(upsertWasCancelled: boolean): void {
  tableData.value.forEach((data) => {
    data.setIsValidated();
    data.setInsertionStatus(t(
        upsertWasCancelled ? "cancelledUpsertRowMessage" : "successUpsertRowMessage"
    ).toString());
    data.setCheckingStatus(t("successCheckRowMessage").toString());
  });
}

async function getExistingGermplasmsUri(): Promise<Array<string>> {
  let uris: URIsListPostDTO = {
    uris: tableData.value.map((row) => {
      return row.getData().uri as string;
    })
  }
  return (await service.checkGermplasmsExist(uris)).response.result
}

/**
 * After an API call, this function sets the update status for each germplasm (true or false).
 */
function SetUpdateStatusForEachGermplasm() {
  getExistingGermplasmsUri()
      .then(uris => {
        setUpdateStatusForEachGermplasm(uris);
      });
}

async function getDtosFromTableData(): Promise<Array<GermplasmCreationDTO>> {
  let creationDtos: Array<GermplasmCreationDTO> = [];

  let dataToInsert = tabulator.getData();

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

    form.rdf_type = props.germplasmType;

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

    addedDuplicatableRdfColumnsToMaxUsedIdMap.value.forEach(
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

    if (suppColumnsNames.value.length > 0) {
      let attributes = {};
      for (let y = 0; y < suppColumnsNames.value.length; y++) {
        let key = suppColumnsNames.value[y];
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
function tryToGetExistingPropertyFromColumnName(columnName: string): SelectableItem {
  let filtered: Array<SelectableItem> = existingDuplicatableRdfAttributesObjects.value.filter(
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
function uploaded(data) {
  if (data.length > 1000) {
    alert(t("alertFileSize"));
  } else {
    newColumns.value = [];
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
            t("missingName") + " " + rowIndex
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
              t("alertDuplicateURI") +
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

      for (let colNumber in tableColumns.value) {
        tableColNames.push(tableColumns.value[colNumber].field);
      }
      let duplicatableProperyOccurencesInCsv: Map<string, number> = new Map<string, number>();
      //Add new column to metadata if it is not already present.
      //If the column is one that can be duplicated, then calculate number of times it comes up
      for (let csvCol of csvColumns) {
        let existingDuplicatableProperty: SelectableItem = tryToGetExistingPropertyFromColumnName(csvCol);
        if (existingDuplicatableProperty) {
          if (duplicatableProperyOccurencesInCsv.has(existingDuplicatableProperty.label)) {
            duplicatableProperyOccurencesInCsv.set(existingDuplicatableProperty.label, duplicatableProperyOccurencesInCsv.get(existingDuplicatableProperty.label) + 1);
          } else {
            duplicatableProperyOccurencesInCsv.set(existingDuplicatableProperty.label, 1);
          }
        } else {
          if (!tableColNames.includes(csvCol)) {
            newColumns.value.push(csvCol);
            if (newColumnModalCheckboxData.value.filter((e) => e.value === csvCol).length === 0) {
              newColumnModalCheckboxData.value.push({"value": csvCol, "name": csvCol});
            }
          }
        }
      }
      //Calculate the amount of missing columns for the duplicatable ones
      for (let duplicatablePropertyNameInCsv of duplicatableProperyOccurencesInCsv.keys()) {
        let duplicatableColumn: SelectableItem = tryToGetExistingPropertyFromColumnName(duplicatablePropertyNameInCsv);
        let tableColumnQuantity: number = addedDuplicatableRdfColumnsToMaxUsedIdMap.value.get(duplicatableColumn.id);
        if (!tableColumnQuantity) {
          tableColumnQuantity = 0;
        }
        let missingColumnQuantity: number = duplicatableProperyOccurencesInCsv.get(duplicatablePropertyNameInCsv) - tableColumnQuantity;
        if (missingColumnQuantity > 0) {
          newColumnModalCheckboxData.value.push(
              {
                "value": duplicatableColumn.label,
                "name": duplicatableColumn.label + " (x" + missingColumnQuantity + ")"
              });
          for (let index = 0; index < missingColumnQuantity; index++) {
            newColumns.value.push(duplicatableColumn.label);
          }
        }
      }

      //Reconstruct data as a json before sending it to tabulator
      let dataInJsonFormat = [];
      for (let rowIndex = 1; rowIndex < data.length - 1; rowIndex++) {
        let nextJsonRow = {};
        nextJsonRow["rowNumber"] = rowIndex;
        //Save indexes of duplicatable columns
        let currentDuplicatableColumnsIndexes = {};
        duplicatableProperyOccurencesInCsv.forEach((value, key) => {
          currentDuplicatableColumnsIndexes[tryToGetExistingPropertyFromColumnName(key).id] = 0;
        });
        for (let colIndex in csvColumns) {
          let colName = csvColumns[colIndex];
          let existingDuplcatableProperty = tryToGetExistingPropertyFromColumnName(colName);
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
      csvUploadedData.value = dataInJsonFormat;
      if (newColumns.value.length > 0) {
        tableData.value = [];
        showSelectNewColumnsPopUp();
      } else {
        tableData.value = dataInJsonFormat.map((row, index) => new GermplasmTableDataRow(index, row));
      }
    }
  }
  SetUpdateStatusForEachGermplasm();
}

/**
 * function triggered by the tabulator cell formatter. Allow to open error modal when clicking on the errors cell
 */
function errorFormaterFunction(cell, formatterParams, onRendered) {
  // Use onRendered to attach the click event listener
  onRendered(() => {
    const rowData = tableData.value[cell.getRow().getData().index];

    if (!rowData.hasError()) {
      return;
    }

    const button = cell.getElement().querySelector(".error-log");
    if (button) {
      button.addEventListener("click", () => {
        errorsToShowInModal.value = rowData.getErrors();
        showErrorModal.value = true;
      });
    }
  });

  // Return the button HTML
  return `<div class="error-log">${cell.getValue() || ""}</div>`;
}

/**
 * function triggered by the tabulator cell formatter. Allow to customize the id column to add the update symbol
 */
function idFormatterFunction(cell, formatterParams, onRendered) {
  const rowData = tableData.value[cell.getRow().getData().index];
  let updateSymbol = ``;

  if (rowData.isUpdate()) {
    updateSymbol = `<div class="update-symbol"></div>`;
  } else {
    updateSymbol = `<div class="create-symbol"></div>`;
  }

  return `<div class="id-update-symbol">${rowData.getRowNumber()} ${updateSymbol}</div>`;
}


//endregion


//#endregion

interface NewColumnCheckboxData {
  value: string,
  name: string
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
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
  successUpsertRowMessage: inserted/updated
  cancelledUpsertRowMessage: insertion/update cancelled
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
  upsertHelpMessage: creates or updates each germplasm in the list in a single operation (a blue square indicates that the germplasm already exists and will therefore be updated).
  update-legend: this line will be updated
  create-legend: this line will be created
  valid-legend: this line has been validated or inserted/updated successfully
  invalid-legend: this line has errors

fr:
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
  is_public: Publique
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
  upsertHelpMessage: crée ou modifie chaque ressource rénétique de la liste en une seule opération (un carré bleu indique que la ressource existe déjà et sera donc mise à jour).
  update-legend: la ligne sera mise à jour
  create-legend: la ligne sera créée
  valid-legend: cette ligne a été validée ou insérée/mise à jour avec succès
  invalid-legend: cette ligne comporte des erreurs
</i18n>
