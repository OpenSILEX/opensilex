<template>
  <div>
    <b-button @click="onImportButtonClick" class="mb-2 mr-2" variant="outline-success">{{
        $t(buttonLabel)
      }}
    </b-button>
    <slot name="error">
    </slot>
    <n-button
        ref="inputFile"
        accept="text/csv, .csv"
        @input="onInput"
        v-model="file"
        hidden
        style="display: none"
    ></n-button>
  </div>
</template>

<script setup lang="ts">
import Papa from 'papaparse'
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {inject, nextTick, ref, useTemplateRef} from "vue";
import {useStore} from "vuex";
import {NButton} from "naive-ui";
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
//#region Refs
const inputFile = useTemplateRef<InstanceType<typeof NButton>>('inputFile')
//#endregion

//#region Data
const errors = ref<string[]>([]);
const file = ref<File | null>(null);
const data = ref<any>(null);
//#endregion

//#region Event handlers

function onInput(file)
{
  this.fileUpdated(file);
}


function onImportButtonClick()
{
  this.importCsv();
}
//#endregion

//#region  methods

function importCsv()
{
  this.inputFile.$el.childNodes[0].click();
}


function fileUpdated(file)
{
  if (!file) {
    return;
  }
  opensilex.showLoader();
  nextTick(() => {
    this.readUploadedFileAsText(file).then((text) => {
      this.file = null;
      let delimiter = CSV.detect(text.toString());

      //Parsing with header set to false with make result.data be an Array of Arrays instead of an Array of jsons
      //The first array contains the headers, this allows us to keep track of duplicated columns.
      let result = Papa.parse(text, {
        header: !this.returnDataAsArrayOfArrays,
        delimiter: delimiter,
      });

      this.errors = [];
      if (result.data == null || result.data.length == 0) {
        this.errors.push(
            "Unable to parse csv, delimiter used : '" + delimiter + "'"
        );
      } else {
        let objectToCheck: Array<string> = (this.returnDataAsArrayOfArrays ? result.data[0] : Object.keys(result.data[0]));

        //Check non duplicatable headers if we are returning array of arrays
        if (this.returnDataAsArrayOfArrays) {
          let visitedHeaders = [];
          let noCapsNoSpacesDuplicatableHeaders: Array<string> = this.duplicatableHeaders.map(e => e.toLowerCase().replaceAll(" ", ""));
          for (let header of objectToCheck) {
            let noCapsNoSpacesHeader = header.toLowerCase().replaceAll(" ", "");
            if (visitedHeaders.includes(noCapsNoSpacesHeader)) {
              if (!noCapsNoSpacesDuplicatableHeaders.includes(noCapsNoSpacesHeader)) {
                this.errors.push(
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
        if (Array.isArray(this.headersPresent) && !CSVInputFile.containsAll(objectToCheck, this.headersPresent)) {
          this.errors.push(
              t('CSVInputFile.headersMissingMessage') + ": [" +
              this.headersPresent + "]"
          );
        }
        //Check the headers have exactly the same quantity and content as headersExactMatch
        if (Array.isArray(this.headersExactMatch)) {
          if (
              !CSVInputFile.equalArrays(
                  objectToCheck,
                  this.headersExactMatch
              )
          ) {
            let finalErrorMessage = t('CSVInputFile.wrongColumns') + ": [" +
                this.headersExactMatch.toString() +
                "] " + t('CSVInputFile.isExpected');
            //Only push found columns if size > 1, because a user can potentialy save some other file,
            //like a json as a .csv, in this case then a fake csv with a single cell containing whole json will be registered
            //making the error message unreadable
            if (objectToCheck.length > 1) {
              finalErrorMessage += ". [" +
                  objectToCheck +
                  "] " + t('CSVInputFile.found') + "."
            }

            this.errors.push(finalErrorMessage);
          }
        }
      }

      //Emit if no errors
      if (this.errors.length == 0) {
        this.data = result.data;
        emit("updated", result.data);
      } else {
        this.errors.forEach(error => opensilex.showErrorToast(error));
      }
      opensilex.hideLoader();
    });
  });
}


function containsAll(container, headersToContain) {
  return headersToContain.every((header) => container.includes(header));
}


function equalArrays(arr1, arr2) {
  return CSVInputFile.containsAll(arr1, arr2) && CSVInputFile.containsAll(arr2, arr1);
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