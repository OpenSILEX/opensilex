<template>
  <div>
    <slot name="customErrors"></slot>

    <Button
      v-if="validationErrors.length"
      class="float-right"
      @click="csvExportInvalidData"
      variant="outline-info"
      >{{ $t("component.data.dataValidationReport.exportResult") }}</Button
    >

    <div v-if="isValid" class="validation-confirm-container">
      {{ $t("component.data.dataValidationReport.CSVIsValid") }}
    </div>

    <div class="error-container" v-if="tooLargeDataset">
      <div class="static-field">
        <span class="field-view-title">{{
          $t("component.data.dataValidationReport.tooLargeDataset", {
            sizeMax: sizeMax,
            nbLinesToImport: nbLinesToImport,
          })
        }}</span>
      </div>
    </div>
    <div class="error-container" v-if="validationErrors.length">
      <div class="static-field">
        <span class="field-view-title"
          >{{ $t("component.data.dataValidationReport.csvErrors") }}:</span
        >
      </div>
      <b-table-simple hover small responsive sticky-header>
        <b-thead head-variant="light">
          <b-tr>
            <b-th>{{ $t("component.data.dataValidationReport.errorLine") }}</b-th>
            <b-th>{{ $t("component.data.dataValidationReport.errorType") }}</b-th>
            <b-th>{{ $t("component.data.dataValidationReport.errorDetail") }}</b-th>
          </b-tr>
        </b-thead>
        <b-tbody>
          <slot v-for="(row, index) in validationErrors">
            <b-tr>
              <b-th :rowspan="row.listSize">{{
                typeof row.index == "number" ? row.index + 4 : row.index
              }}</b-th>
              <b-td>{{
                $t("component.data.dataValidationReport." + row.firstErrorType.type)
              }}</b-td>
              <b-td>
                <ul>
                  <li
                    v-for="validationErr in row.firstErrorType.validationErrors"
                    v-bind:key="
                      getErrKey(validationErr, row.firstErrorType.type)
                    "
                  >
                    {{
                      getValidationErrorDetail(
                        validationErr,
                        row.firstErrorType.type
                      )
                    }}
                  </li>
                </ul>
              </b-td>
            </b-tr>
            <b-tr
              v-for="(validationError, errorType) in row.list"
              v-bind:key="index + errorType"
            >
              <b-td>{{ $t("component.data.dataValidationReport." + errorType) }}</b-td>
              <b-td>
                <ul>
                  <li
                    v-for="validationErr in validationError"
                    v-bind:key="getErrKey(validationErr, errorType)"
                  >
                    {{ getValidationErrorDetail(validationErr, errorType) }}
                  </li>
                </ul>
              </b-td>
            </b-tr>
          </slot>
        </b-tbody>
      </b-table-simple>
    </div>
    <br />
  </div>
</template>

<script setup lang="ts">
import Button from "@/components/common/buttons/Button.vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {computed, inject, ref} from "vue";
import {useI18n} from "vue-i18n";

withDefaults(
    defineProps<{
      validMessage?: string;
    }>(),
    {
      validMessage: "component.data.dataValidationReport.",
    }
);

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const { t } = useI18n()

const checkErrors = ref<boolean>(false)
const validationErrors = ref<any[]>([])
const tooLargeDataset = ref<boolean>(false)
const nbLinesToImport = ref<number>(0)
const sizeMaxNumber = ref<number>(0);

// Getter isValid
const isValid = computed<boolean>(() => {
  return (
      checkErrors.value &&
      validationErrors.value.length === 0 &&
      !tooLargeDataset.value
  );
});

// Getter et setter sizeMax
const sizeMax = computed<number>({
  get: () => sizeMaxNumber.value,
  set: (sizeMax: number) => {
    sizeMaxNumber.value = sizeMax;
  },
});

function reset() {
    checkErrors.value = false;
    validationErrors.value = [];
  }


  /**
   * The only true button.
   * @dataErrors dataErrors validation model
   */
 function checkValidation(dataErrors) {
    tooLargeDataset.value = dataErrors.tooLargeDataset;
    nbLinesToImport.value = dataErrors.nbLinesToImport;
    let errors = dataErrors;
    checkErrors.value = true;
    let globalErrors = {};

    loadErrorType("datatypeErrors", errors, globalErrors);
    loadErrorType("uriNotFoundErrors", errors, globalErrors);
    loadErrorType("invalidURIErrors", errors, globalErrors);

    loadErrorType("missingRequiredValueErrors", errors, globalErrors);
    loadErrorType("invalidValueErrors", errors, globalErrors);
    loadErrorType("invalidObjectErrors", errors, globalErrors);
    loadErrorType("invalidTargetErrors", errors, globalErrors);
    loadErrorType("invalidDateErrors", errors, globalErrors);
    loadErrorType("invalidExperimentErrors", errors, globalErrors);
    loadErrorType("invalidDeviceErrors", errors, globalErrors);
    loadErrorType("deviceChoiceAmbiguityErrors", errors, globalErrors);
    loadErrorType("invalidDataTypeErrors", errors, globalErrors);
    loadErrorType("duplicatedDataErrors", errors, globalErrors);
    loadErrorType("duplicatedExperimentErrors", errors, globalErrors);
    loadErrorType("duplicatedObjectErrors", errors, globalErrors);
    loadErrorType("duplicatedTargetErrors", errors, globalErrors);
    loadErrorType("duplicatedDeviceErrors", errors, globalErrors);
    loadErrorType("invalidAnnotationErrors", errors, globalErrors);

    loadErrorType("alreadyExistingURIErrors", errors, globalErrors);
    loadErrorType("duplicateURIErrors", errors, globalErrors);

    let generalErrors: any = {
      index: t("component.data.dataValidationReport.generalErrors").toString(),
      list: {},
      listSize: 1,
      firstErrorType: null,
    };
    if (errors.missingHeaders.length > 0) {
      generalErrors.list.missingHeaders = errors.missingHeaders;
      generalErrors.listSize++;
      if (!generalErrors.firstErrorType) {
        generalErrors.firstErrorType = "missingHeaders";
      }
    }
    if (errors.invalidAnnotationErrors.length > 0) {
      generalErrors.list.invalidAnnotationErrors = errors.invalidAnnotationErrors;
      generalErrors.listSize++;
      if (!generalErrors.firstErrorType) {
        generalErrors.firstErrorType = "invalidAnnotationErrors";
      }
    }
    if (errors.emptyHeaders.length > 0) {
      generalErrors.list.emptyHeaders = errors.emptyHeaders;
      generalErrors.listSize++;
      if (!generalErrors.firstErrorType) {
        generalErrors.firstErrorType = "emptyHeaders";
      }
    }
    if (Object.keys(errors.invalidHeaderURIs).length > 0) {
      let invalidHeaderURIsList = Object.values(errors.invalidHeaderURIs);
      generalErrors.list.invalidHeaderURIs = invalidHeaderURIsList;
      generalErrors.listSize = generalErrors.listSize + 1;
      if (!generalErrors.firstErrorType) {
        generalErrors.firstErrorType = "invalidHeaderURIs";
      }
    }
    validationErrors.value = [];

    if (generalErrors.firstErrorType) {
      generalErrors.listSize--;
      let firstErrorType = generalErrors.firstErrorType;
      generalErrors.firstErrorType = {
        type: firstErrorType,
        validationErrors: generalErrors.list[firstErrorType],
      };
      delete generalErrors.list[firstErrorType];
      validationErrors.value.push(generalErrors);
    }
    for (let i in globalErrors) {
      if (globalErrors[i].firstErrorType) {
        globalErrors[i].listSize = globalErrors[i].listSize - 1;
        let firstErrorType = globalErrors[i].firstErrorType;
        globalErrors[i].firstErrorType = {
          type: firstErrorType,
          validationErrors: globalErrors[i].list[firstErrorType],
        };
        delete globalErrors[i].list[firstErrorType];
        validationErrors.value.push(globalErrors[i]);
      }
    }
  }

 function getValidationErrorDetail(validationError, errorType) {
    switch (errorType) {
      case "missingHeaders":
        return t(
          "component.data.dataValidationReport.validationErrorMissingHeaderMessage",
          {
            header: validationError,
          }
        );
      case "invalidHeaderURIs":
        return t(
          "component.data.dataValidationReport.validationErrorMissingHeaderMessage",
          {
            header: validationError,
          }
        );
      case "missingRequiredValueErrors":
        return t(
          "component.data.dataValidationReport.validationErrorMissingRequiredMessage",
          validationError
        );
      case "invalidObjectErrors":
        return t(
          "component.data.dataValidationReport.invalidObjectErrorMessage",
          validationError
        );
      case "invalidTargetErrors":
        return t(
        "component.data.dataValidationReport.invalidTargetErrorMessage",
        validationError
      );
      case "invalidDateErrors":
        return t(
          "component.data.dataValidationReport.invalidDateErrorMessage",
          validationError
        );
      case "invalidExperimentErrors":
        return t(
          "component.data.dataValidationReport.invalidExperimentErrorMessage",
          validationError
        );
      case "invalidDeviceErrors":
        return t(
          "component.data.dataValidationReport.invalidDeviceErrorMessage",
          validationError
        );
      case "deviceChoiceAmbiguityErrors":
        return t(
          "component.data.dataValidationReport.deviceChoiceAmbiguityErrors",
          validationError
        );
      case "duplicateURIErrors":
        return t(
          "component.data.dataValidationReport.validationErrorDuplicateURIMessage",
          validationError
        );
      case "datatypeErrors":
        return t(
          "component.data.dataValidationReport.validationErrorDatatypeMessage",
          validationError
        );
      case "duplicatedData":
        return t(
          "component.data.dataValidationReport.validationErrorDuplicatedDataMessage",
          validationError
        );
      case "duplicatedExperimentErrors":
        return t(
          "component.data.dataValidationReport.invalidExperimentErrorMessage",
          validationError
        );
      case "duplicatedObjectErrors":
        return t(
          "component.data.dataValidationReport.invalidObjectErrorMessage",
          validationError
        );
      case "duplicatedTargetErrors":
        return t(
          "component.data.dataValidationReport.invalidTargetErrorMessage",
          validationError
        );
      case "duplicatedDeviceErrors":
        return t(
          "component.data.dataValidationReport.invalidDeviceErrorMessage",
          validationError
        );
      case "invalidValueErrors":
        return t(
          "component.data.dataValidationReport.invalidValueErrorMessage",
          validationError
        );
      case "emptyHeaders":
        return t(
          "component.data.dataValidationReport.validationErrorMissingRequiredMessage",
          {
            header: "#" + validationError,
          }
        );

      default:
        return t(
          "component.data.dataValidationReport.validationErrorMessage",
          validationError
        );
    }
  }

  function loadErrorType(errorType, errors, globalErrors) {
    for (let i in errors[errorType]) {
      let errorList = errors[errorType][i];
      if (!Array.isArray(errorList)) {
        errorList = [errors[errorType][i]];
      }
      for (let j in errorList) {
        let errorItem = errorList[j];
        let rowIndex = errorItem.rowIndex;
        if (!globalErrors[rowIndex]) {
          globalErrors[rowIndex] = {
            index: rowIndex,
            list: {},
            listSize: 1,
          };
        }

        if (!globalErrors[rowIndex].list) {
          globalErrors[rowIndex].list = {};
        }

        if (!globalErrors[rowIndex].firstErrorType) {
          globalErrors[rowIndex].firstErrorType = errorType;
        }

        if (!globalErrors[rowIndex].list[errorType]) {
          globalErrors[rowIndex].list[errorType] = [];
          globalErrors[rowIndex].listSize = globalErrors[rowIndex].listSize + 1;
        }

        globalErrors[rowIndex].list[errorType].push(errorItem);
      }
    }
  }

 function getErrKey(validationError, errorType) {
    return (
      "validationError-" +
      errorType +
      "-" +
      validationError.rowIndex +
      "-" +
      validationError.colIndex
    );
  }

 function csvExportInvalidData() {
    let arrData = [
      [
        t("component.data.dataValidationReport.errorLine").toString(),
        t("component.data.dataValidationReport.errorType").toString(),
        t("component.data.dataValidationReport.errorDetail").toString(),
      ],
    ];
    validationErrors.value.forEach((row, index) => {
      row.index == "number" ? row.index + 3 : row.index;
      let line = [];
      line.push[t("component.data.dataValidationReport." + row.firstErrorType.type)];
      for (let validationErr in row.firstErrorType.validationErrors) {
        line.push(
          row.index,
          t("component.data.dataValidationReport." + row.firstErrorType.type),
          getValidationErrorDetail(
            row.firstErrorType.validationErrors[validationErr],
            row.firstErrorType.type
          )
        );
      }
      arrData.push(line);
    });
    console.debug("CSV validationReport data :", arrData);
    papa.download(
      papa.unparse(arrData, { delimiter: "," }),
      "validationReport"
    );
  }
</script>
<style scoped lang="scss">
.csv-import-helper {
  font-style: italic;
}
.row-header::first-letter {
  text-transform: none;
}

.csv-format {
  margin-top: 15px;
}

.error-container .field-view-title {
  color: rgb(245, 54, 92);
}

.validation-confirm-container {
  color: rgb(40, 167, 69);
  font-weight: bold;
}

.validation-warning-container {
  color: #a58524;
  font-weight: bold;
}

.multi-line-cell {
  white-space: pre;
}

.uri-cell {
  white-space: pre-warp;
}
</style>
