<template>
    <div>
        <b-button @click="onImportButtonClick" class="mb-2 mr-2" variant="outline-success">{{
                $t(buttonLabel)
            }}
        </b-button>
        <slot name="error">
      <span v-if="errors" class="error-message alert alert-danger mb-2 mr-2">{{
              this.errors[0]
          }}</span>
        </slot>
        <b-form-file
                ref="inputFile"
                accept="text/csv, .csv"
                @input="onInput"
                v-model="file"
                hidden
                style="display: none"
        ></b-form-file>
    </div>
</template>

<script lang="ts">
import {Component, Prop, Ref, PropSync} from "vue-property-decorator";
import Vue from "vue";
import * as CSV from "csv-string";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {OpenSilexStore} from "../../../models/Store"

@Component
export default class CSVInputFile extends Vue {
    //#region Plugins and services
    $opensilex: OpenSilexVuePlugin;
    $store: OpenSilexStore;
    $papa;
    //#endregion

    //#region Props

    @PropSync("csvData")
    private data: any[];

    @Prop()
    private readonly headersExactMatch: string[];

    @Prop()
    private readonly headersPresent: string[];

    /**
     * To check some headers don't show up more that once.
     * Only gets checked if we are returning an array of arrays in data.
     *
     * Note that this list will be transformed into no caps no spaces.
     */
    @Prop()
    private readonly duplicatableHeaders: string[];

    /**
     * Data's first array will be headers, allows for duplicated headers but this changes the output format.
     */
    @Prop({default: false})
    private readonly returnDataAsArrayOfArrays: boolean;

    @Prop()
    private readonly config: any;

    @Prop({
        default: "component.common.import-files.csv-file",
    })
    private readonly buttonLabel: string;
    //#endregion

    //#region Refs
    @Ref("inputFile") readonly inputFile!: any;
    //#endregion

    //#region Data
    private errors: any = [];
    private file: any = null;
    //#endregion

    //#region Event handlers
    private onInput(file) {
        this.fileUpdated(file);
    }

    private onImportButtonClick() {
        this.importCsv();
    }
    //#endregion

    //#region Private methods
    private importCsv() {
        this.inputFile.$el.childNodes[0].click();
    }


    private fileUpdated(file): any {
        if (!file) {
            return;
        }
        this.$opensilex.showLoader();
        this.$nextTick(() => {
            this.readUploadedFileAsText(file).then((text) => {
                this.file = null;
                console.debug("Input file text", text);
                let delimiter = CSV.detect(text.toString());

                //Parsing with header set to false with make result.data be an Array of Arrays instead of an Array of jsons
                //The first array contains the headers, this allows us to keep track of duplicated columns.
                let result = this.$papa.parse(text, {
                    header: !this.returnDataAsArrayOfArrays,
                    delimiter: delimiter,
                });
                console.debug("result.data", result.data);
                console.debug("result.errors", result.errors);
                console.debug("result.meta", result.meta);

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
                                            this.$i18n.t('CSVInputFile.headerCantBeDuplicatedMessage') + ": " +
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
                                this.$i18n.t('CSVInputFile.headersMissingMessage') + ": [" +
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
                            this.errors.push(
                                    this.$i18n.t('CSVInputFile.wrongColumns') + ": [" +
                                    this.headersExactMatch.toString() +
                                    "] " + this.$i18n.t('CSVInputFile.isExpected') + ". [" +
                                    objectToCheck +
                                    "] " + this.$i18n.t('CSVInputFile.found') + "."
                            );
                        }
                    }
                }

                //Emit if no errors
                if (this.errors.length == 0) {
                    this.data = result.data;
                    this.$emit("updated", result.data);
                }
                this.$opensilex.hideLoader();
            });
        });
    }

    private static containsAll(container, headersToContain) {
        return headersToContain.every((header) => container.includes(header));
    }

    private static equalArrays(arr1, arr2): boolean {
        return CSVInputFile.containsAll(arr1, arr2) && CSVInputFile.containsAll(arr2, arr1);
    }

    private async readUploadedFileAsText(inputFile) {
        console.debug("inputFile", inputFile);
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
}
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