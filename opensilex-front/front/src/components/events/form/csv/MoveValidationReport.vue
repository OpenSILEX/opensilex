<template>
    <div>
        <slot name="customErrors"></slot>

        <b-button
                v-if="validationErrors.length"
                class="float-right"
                @click="csvExportInvalidData"
                variant="outline-info"
        >{{ $t("DataValidationReport.exportResult") }}</b-button
        >

        <div v-if="isValid" class="validation-confirm-container">
            {{ $t("DataValidationReport.CSVIsValid") }}
        </div>

        <div class="error-container" v-if="tooLargeDataset">
            <div class="static-field">
        <span class="field-view-title">{{
          $t("DataValidationReport.tooLargeDataset", {
            sizeMax: sizeMax,
            nbLinesToImport: nbLinesToImport,
          })
        }}</span>
            </div>
        </div>
        <div class="error-container" v-if="validationErrors.length">
            <div class="static-field">
        <span class="field-view-title"
        >{{ $t("DataValidationReport.csvErrors") }}:</span
        >
            </div>
            <b-table-simple hover small responsive sticky-header>
                <b-thead head-variant="light">
                    <b-tr>
                        <b-th>{{ $t("DataValidationReport.errorLine") }}</b-th>
                        <b-th>{{ $t("DataValidationReport.errorType") }}</b-th>
                        <b-th>{{ $t("DataValidationReport.errorDetail") }}</b-th>
                    </b-tr>
                </b-thead>
                <b-tbody>
                    <slot v-for="(row, index) in validationErrors">
                        <b-tr>
                            <b-th :rowspan="row.listSize">{{
                                typeof row.index == "number" ? row.index + 4 : row.index
                                }}</b-th>
                            <b-td>{{
                                $t("DataValidationReport." + row.firstErrorType.type)
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
                            <b-td>{{ $t("DataValidationReport." + errorType) }}</b-td>
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

<script lang="ts">
    import { Component, Prop, Ref } from "vue-property-decorator";
    import Vue from "vue";
    @Component
    export default class MoveValidationReport extends Vue {
        $opensilex: any;
        $papa: any;
        $t: any;

        checkErrors: boolean = false;

        validationErrors: any[] = [];
        tooLargeDataset: boolean = false;
        nbLinesToImport: number = 0;

        sizeMaxNumber: number;

        get isValid(): boolean {
            return (
                this.checkErrors &&
                this.validationErrors.length == 0 &&
                !this.tooLargeDataset
            );
        }

        set sizeMax(sizeMax: number) {
            this.sizeMaxNumber = sizeMax;
        }

        get sizeMax(): number {
            return this.sizeMaxNumber;
        }

        reset() {
            this.checkErrors = false;
            this.validationErrors = [];
        }

        @Prop({ default: "DataValidationReport." })
        validMessage;

        /**
         * The only true button.
         * @dataErrors dataErrors validation model
         */
        checkValidation(dataErrors) {
            console.debug("Verification data", dataErrors);
            this.tooLargeDataset = dataErrors.tooLargeDataset;
            this.nbLinesToImport = dataErrors.nbLinesToImport;
            let errors = dataErrors;
            this.checkErrors = true;
            let globalErrors = {};

            this.loadErrorType("datatypeErrors", errors, globalErrors);
            this.loadErrorType("uriNotFoundErrors", errors, globalErrors);
            this.loadErrorType("invalidURIErrors", errors, globalErrors);

            this.loadErrorType("missingRequiredValueErrors", errors, globalErrors);
            this.loadErrorType("invalidValueErrors", errors, globalErrors);
            this.loadErrorType("invalidObjectErrors", errors, globalErrors);
            this.loadErrorType("invalidDateErrors", errors, globalErrors);
            this.loadErrorType("invalidDataTypeErrors", errors, globalErrors);
            this.loadErrorType("duplicatedDataErrors", errors, globalErrors);

            this.loadErrorType("alreadyExistingURIErrors", errors, globalErrors);
            this.loadErrorType("duplicateURIErrors", errors, globalErrors);

            let generalErrors: any = {
                index: this.$t("DataValidationReport.generalErrors").toString(),
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
            if (Object.keys(errors.invalidHeaderURIs).length > 0) {
                let invalidHeaderURIsList = Object.values(errors.invalidHeaderURIs);
                generalErrors.list.invalidHeaderURIs = invalidHeaderURIsList;
                generalErrors.listSize = generalErrors.listSize + 1;
                if (!generalErrors.firstErrorType) {
                    generalErrors.firstErrorType = "invalidHeaderURIs";
                }
            }
            this.validationErrors = [];

            if (generalErrors.firstErrorType) {
                generalErrors.listSize--;
                let firstErrorType = generalErrors.firstErrorType;
                generalErrors.firstErrorType = {
                    type: firstErrorType,
                    validationErrors: generalErrors.list[firstErrorType],
                };
                delete generalErrors.list[firstErrorType];
                this.validationErrors.push(generalErrors);
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
                    this.validationErrors.push(globalErrors[i]);
                }
            }
        }

        getValidationErrorDetail(validationError, errorType) {
            switch (errorType) {
                case "missingHeaders":
                    return this.$t(
                        "DataValidationReport.validationErrorMissingHeaderMessage",
                        {
                            header: validationError,
                        }
                    );
                case "invalidHeaderURIs":
                    return this.$t(
                        "DataValidationReport.validationErrorMissingHeaderMessage",
                        {
                            header: validationError,
                        }
                    );
                case "missingRequiredValueErrors":
                    return this.$t(
                        "DataValidationReport.validationErrorMissingRequiredMessage",
                        validationError
                    );
                case "invalidObjectErrors":
                    return this.$t(
                        "DataValidationReport.invalidObjectErrorMessage",
                        validationError
                    );
                case "invalidDateErrors":
                    return this.$t(
                        "DataValidationReport.invalidDateErrorMessage",
                        validationError
                    );
                case "duplicateURIErrors":
                    return this.$t(
                        "DataValidationReport.validationErrorDuplicateURIMessage",
                        validationError
                    );
                case "datatypeErrors":
                    return this.$t(
                        "DataValidationReport.validationErrorDatatypeMessage",
                        validationError
                    );
                case "duplicatedData":
                    return this.$t(
                        "DataValidationReport.validationErrorDuplicatedDataMessage",
                        validationError
                    );
                case "invalidValueErrors":
                    return this.$t(
                        "DataValidationReport.invalidValueErrorMessage",
                        validationError
                    );

                default:
                    return this.$t(
                        "DataValidationReport.validationErrorMessage",
                        validationError
                    );
            }
        }

        private loadErrorType(errorType, errors, globalErrors) {
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

        getErrKey(validationError, errorType) {
            return (
                "validationError-" +
                errorType +
                "-" +
                validationError.rowIndex +
                "-" +
                validationError.colIndex
            );
        }

        csvExportInvalidData() {
            let arrData = [
                [
                    this.$t("DataValidationReport.errorLine").toString(),
                    this.$t("DataValidationReport.errorType").toString(),
                    this.$t("DataValidationReport.errorDetail").toString(),
                ],
            ];
            this.validationErrors.forEach((row, index) => {
                row.index == "number" ? row.index + 3 : row.index;
                let line = [];
                line.push[this.$t("DataValidationReport." + row.firstErrorType.type)];
                for (let validationErr in row.firstErrorType.validationErrors) {
                    line.push(
                        row.index,
                        this.$t("DataValidationReport." + row.firstErrorType.type),
                        this.getValidationErrorDetail(
                            row.firstErrorType.validationErrors[validationErr],
                            row.firstErrorType.type
                        )
                    );
                }
                arrData.push(line);
            });
            console.debug("CSV validationReport data :", arrData);
            this.$papa.download(
                this.$papa.unparse(arrData, { delimiter: "," }),
                "validationReport"
            );
        }
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
