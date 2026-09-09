<template>
  <div>
    <slot name="error">
    </slot>
    <n-upload
        ref="inputFile"
        accept="text/csv, .csv"
        :show-file-list="false"
        :default-upload="false"
        @change="onChange"
    >
      <n-button class="load-csv-button">{{t(buttonLabel)}}</n-button>
    </n-upload>
  </div>
</template>

<script setup lang="ts">
import Papa from 'papaparse'
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {inject, nextTick, ref, useTemplateRef} from "vue";
import {useStore} from "vuex";
import {NUpload} from "naive-ui";
import type {UploadFileInfo} from "naive-ui";
import {useI18n} from "vue-i18n";

const emit = defineEmits(['updated'])

//#region Plugins and services
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const store = useStore()
const { t } = useI18n()
//#endregion

//#region Props

const props = withDefaults(defineProps<{
      headersExactMatch?: string[]
      headersPresent?: string[]
      /** To check some headers don't show up more that once.
       * Only gets checked if we are returning an array of arrays in data.
       *
       * Note that this list will be transformed into no caps no spaces.  */
      duplicatableHeaders?: string[]
      // Data's first array will be headers, allows for duplicated headers but this changes the output format.
      returnDataAsArrayOfArrays?: boolean
      config?: any
      buttonLabel: string
    }>(), {
      returnDataAsArrayOfArrays: false,
      config: "component.common.import-files.csv-file",
    }
)

//#region Data
const errors = ref<string[]>([]);
const data = ref<any>(null);
//#endregion

//#region Event handlers

function onChange(options: { file: UploadFileInfo })
{
  fileUpdated(options.file.file ?? null);
}
//#endregion

//#region  methods

function fileUpdated(uploadedFile: File | null){
  if (!uploadedFile) {
    return;
  }
  opensilex.showLoader();
  nextTick(() => {
    readUploadedFileAsText(uploadedFile).then((text) => {
      //Parsing with header set to false with make result.data be an Array of Arrays instead of an Array of jsons
      //The first array contains the headers, this allows us to keep track of duplicated columns.
      Papa.parse(text, {
        header: !props.returnDataAsArrayOfArrays,
        complete: processParseFile
      },
      );
    });
  });
}

function processParseFile(result, file){
  errors.value = [];
  if (result.data == null || result.data.length == 0) {
    errors.value.push(
        "Unable to parse csv, delimiter used : '" + result.meta.delimiter + "'"
    );
  } else {
    let objectToCheck: Array<string> = (props.returnDataAsArrayOfArrays ? result.data[0] : Object.keys(result.data[0]));

    //Check non duplicatable headers if we are returning array of arrays
    if (props.returnDataAsArrayOfArrays) {
      let visitedHeaders = [];
      let noCapsNoSpacesDuplicatableHeaders: Array<string> = props.duplicatableHeaders.map(e => e.toLowerCase().replaceAll(" ", ""));
      for (let header of objectToCheck) {
        let noCapsNoSpacesHeader = header.toLowerCase().replaceAll(" ", "");
        if (visitedHeaders.includes(noCapsNoSpacesHeader)) {
          if (!noCapsNoSpacesDuplicatableHeaders.includes(noCapsNoSpacesHeader)) {
            errors.value.push(
                t('CSVInputFile.headerCantBeDuplicatedMessage') + ": " +
                header
            );
            break;
          }
        } else {
          visitedHeaders.push(header);
        }
      }
    }
    //Check that some headers are present
    if (Array.isArray(props.headersPresent) && !containsAll(objectToCheck, props.headersPresent)) {
      errors.value.push(
          t('CSVInputFile.headersMissingMessage') + ": [" +
          props.headersPresent + "]"
      );
    }
    //Check the headers have exactly the same quantity and content as headersExactMatch
    if (Array.isArray(props.headersExactMatch)) {
      if (
          !equalArrays(
              objectToCheck,
              props.headersExactMatch
          )
      ) {
        let finalErrorMessage = t('CSVInputFile.wrongColumns') + ": [" +
            props.headersExactMatch.toString() +
            "] " + t('CSVInputFile.isExpected');
        //Only push found columns if size > 1, because a user can potentialy save some other file,
        //like a json as a .csv, in this case then a fake csv with a single cell containing whole json will be registered
        //making the error message unreadable
        if (objectToCheck.length > 1) {
          finalErrorMessage += ". [" +
              objectToCheck +
              "] " + t('CSVInputFile.found') + "."
        }

        errors.value.push(finalErrorMessage);
      }
    }
  }

  //Emit if no errors
  if (errors.value.length == 0) {
    data.value = result.data;
    emit("updated", result.data);
  } else {
    errors.value.forEach(error => opensilex.showErrorToast(error));
  }
  opensilex.hideLoader();
}


function containsAll(container, headersToContain) {
  return headersToContain.every((header) => container.includes(header));
}


function equalArrays(arr1, arr2) {
  return containsAll(arr1, arr2) && containsAll(arr2, arr1);
}

async function readUploadedFileAsText(inputFile) {
  const temporaryFileReader = new FileReader();

  return new Promise((resolve, reject) => {
    temporaryFileReader.onerror = () => {
      temporaryFileReader.abort();
      reject(new DOMException("Problem parsing input file."));
    };

    temporaryFileReader.onload = () => {
      resolve(temporaryFileReader.result);
    };
    temporaryFileReader.readAsText(inputFile);
  });
}
//#endregion
</script>

<style scoped lang="scss">
.load-csv-button{
  color: #28A745;
  border-color: #28A745;
  --n-border: 1px solid #28A745 !important;
}

.load-csv-button:hover{
  color: #ffffff;
  background-color: #28A745;
  border-color: #28A745;
}
</style>

<i18n>

en:
  CSVInputFile:
    headerCantBeDuplicatedMessage: This header can't be duplicated
    headersMissingMessage: Some of these headers are missing
    wrongColumns: Wrong columns
    isExpected: is expected
    found: has been found

fr:
  CSVInputFile:
    headerCantBeDuplicatedMessage: Cette colonne n'est pas duplicable
    headersMissingMessage: Des colonnes parmi les suivantes sont manquantes
    wrongColumns: Mauvaises colonnes
    isExpected: est attendue
    found: a été trouvé

</i18n>
