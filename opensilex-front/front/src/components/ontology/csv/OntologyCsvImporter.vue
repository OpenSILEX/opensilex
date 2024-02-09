<template>
    <b-modal
        ref="OntologyCsvImporter"
        size="xl"
        :static="true"
    >
        <template v-slot:modal-header>
            <b-row class="mt-1" style="width: 100%">
                <b-col cols="11">
                    <i>
                        <h4>
                            <slot name="icon">
                                <opensilex-Icon icon="fa#eye" class="icon-title"/>
                            </slot>
                            <span>{{ $t(title) }}</span>
                        </h4>
                    </i>
                </b-col>
                <b-col cols="1">
                    <!-- Emulate built in modal header close button action -->
                    <button
                        type="button"
                        class="close"
                        @click="hide"
                        data-dismiss="modal"
                        aria-label="Close"
                    >
                        <span aria-hidden="true">&times;</span>
                    </button>
                </b-col>
            </b-row>
        </template>
        <div class="container-fluid">
            <ValidationObserver ref="validatorRef">
                <label>{{ $t(title) }}
                    <span class="required">*</span>
                </label>
                <div class="row">
                    <div class="col-md-4">
                        <b-form-file
                            size="sm"
                            ref="inputFile"
                            accept="text/csv, .csv"
                            @input="csvUploaded"
                            v-model="csvFile"
                            :placeholder="$t('OntologyCsvImporter.csv-file-placeholder')"
                            :drop-placeholder="
                $t('OntologyCsvImporter.csv-file-drop-placeholder')
              "
                            :browse-text="$t('OntologyCsvImporter.csv-file-select-button')"
                        ></b-form-file>
                    </div>
                    <slot name="generator"></slot>
                </div>
                <div class="row" style="padding-top: 15px">
                    <div class="col-md-12">
                        <slot name="help"></slot>
                    </div>
                </div>
                <div class="error-container" v-if="validationErrors && validationErrors.length > 0">
                    <div class="static-field">
            <span class="field-view-title"
            >{{ $t("OntologyCsvImporter.csvErrors") }}:</span
            >
                    </div>

                    <b-table-simple borderless striped hover small responsive sticky-header>
                        <b-thead head-variant="light">
                            <b-tr>
                                <b-th>{{ $t("OntologyCsvImporter.line") }}</b-th>
                                <b-th>{{ $t("OntologyCsvImporter.errorType") }}</b-th>
                                <b-th>{{ $t("OntologyCsvImporter.detail") }}</b-th>
                            </b-tr>
                        </b-thead>
                        <b-tbody>
                            <slot v-for="row in validationErrors">
                                <b-tr>
                                    <b-th variant="danger" :rowspan="row.listSize">{{ row.index }}</b-th>
                                    <b-td>
                                        <b>{{$t("OntologyCsvImporter." + row.firstErrorType.type) }}</b>
                                    </b-td>
                                    <b-td>
                                        <ul>
                                            <li
                                                v-for="validationErr in row.firstErrorType.validationErrors"
                                                v-bind:key="getErrKey(validationErr, row.firstErrorType.type)"
                                            >
                                              {{getValidationErrorDetail(validationErr, row.firstErrorType.type,validation) }}
                                            </li>
                                        </ul>
                                    </b-td>
                                </b-tr>
                                <b-tr
                                    v-for="(validationError, errorType) in row.list"
                                    v-bind:key="row.index + errorType"
                                >
                                    <b-td><b>{{ $t("OntologyCsvImporter." + errorType) }}</b></b-td>
                                    <b-td>
                                        <ul>
                                            <li
                                                v-for="validationErr in validationError"
                                                v-bind:key="getErrKey(validationErr, errorType)"
                                            >
                                                {{ getValidationErrorDetail(validationErr, errorType,validation) }}
                                            </li>
                                        </ul>
                                    </b-td>
                                </b-tr>
                            </slot>
                        </b-tbody>
                    </b-table-simple>
                </div>
                <div class="validation-confirm-container" v-else-if="validationToken && nbLines">
                    {{ this.$t("OntologyCsvImporter.CSVIsValid", {nb_lines: nbLines}) }}
                </div>
            </ValidationObserver>
        </div>
        <template v-slot:modal-footer>
            <button
                type="button"
                class="btn btn-secondary"
                v-on:click="hide()"
            >{{ $t('component.common.close') }}
            </button>

            <button
                type="button"
                class="btn greenThemeColor"
                v-on:click="importCSV()"
                :disabled="!csvFile || validationErrors && validationErrors.length > 0"
            >{{ $t('component.common.ok') }}
            </button>
        </template>
    </b-modal>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import {CSVValidationModel, OntologyService} from "opensilex-core/index";
import {CSVValidationDTO} from "opensilex-core/model/cSVValidationDTO";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {CsvError} from "./CsvError";

@Component
export default class OntologyCsvImporter extends Vue {
    $opensilex: OpenSilexVuePlugin;
    ontologyService: OntologyService;
    $t: any;

    @Ref("OntologyCsvImporter")
    readonly OntologyCsvImporter!: any;

    @Ref("validatorRef") readonly validatorRef!: any;
    @Ref("inputFile") readonly inputFile!: any;

    @Prop()
    baseType;

    @Prop({default: "OntologyCsvImporter.import"})
    title;

    @Prop({
        default: () => Promise.reject("validateCSV property is mandatory"),
    })
    validateCSV;

    @Prop({
        default: () => Promise.reject("uploadCSV property is mandatory"),
    })
    uploadCSV;

    @Prop()
    successImportMsg: string;

    @Prop({
        default: () => [],
    })
    customColumns;

    show() {
        this.validatorRef.reset();
        this.csvFile = null;
        this.validationToken = null;
        this.validationErrors = [];
        this.OntologyCsvImporter.show();
    }

    hide() {
        this.OntologyCsvImporter.hide();
    }

    mounted() {
        this.ontologyService = this.$opensilex.getService(
            "opensilex-core.OntologyService"
        );
    }

    getCellClass(index, cell) {
        let classStr = "multi-line-cell";
        if (index == "URI") {
            classStr = "uri-cell";
        }

        return classStr;
    }

    rows = [];

    csvFile = null;
    validationToken: string = null;
    nbLines: number = 0;
    validation: CSVValidationDTO = null;

    csvUploaded() {
        this.validationToken = null;
        this.validationErrors = [];
        this.nbLines = 0;
        if (this.csvFile != null) {
            this.validateCSV(this.csvFile).then((response) => {
                this.checkCSVValidation(response);
                if (this.validationToken) {
                    this.$emit("csvValidated", response);
                }
            })

        }
    }

    importCSV() {
        this.validationErrors = [];
        this.nbLines = 0;
        this.uploadCSV(this.validationToken, this.csvFile)
            .then((response) => {
                this.checkCSVValidation(response);
                if (this.validationToken) {
                    this.$opensilex.showSuccessToast(response.result.nb_lines_imported + " " + this.$i18n.t(this.successImportMsg));
                    this.$emit("csvImported", response);
                    this.hide();
                }
            })
            .catch(this.$opensilex.errorHandler);
    }

    checkCSVValidation(response) {
        let validation: CSVValidationDTO = response.result;
        this.validation = validation;
        this.validationToken = validation.validation_token;
        this.nbLines = validation.nb_lines_imported;

        if (!this.validationToken) {
            let errors = response.result.errors;

            let errorsByRowIndex:  Map<number, CsvError> = new Map();

            OntologyCsvImporter.loadErrorType("datatypeErrors", errors, errorsByRowIndex);
            OntologyCsvImporter.loadErrorType("invalidDateErrors", errors, errorsByRowIndex);
            OntologyCsvImporter.loadErrorType("uriNotFoundErrors", errors, errorsByRowIndex);
            OntologyCsvImporter.loadErrorType("invalidURIErrors", errors, errorsByRowIndex);
            OntologyCsvImporter.loadErrorType("missingRequiredValueErrors", errors, errorsByRowIndex);
            OntologyCsvImporter.loadErrorType("invalidValueErrors", errors, errorsByRowIndex);
            OntologyCsvImporter.loadErrorType("alreadyExistingURIErrors", errors, errorsByRowIndex);
            OntologyCsvImporter.loadErrorType("duplicateURIErrors", errors, errorsByRowIndex);
            OntologyCsvImporter.loadErrorType("invalidRowSizeErrors", errors, errorsByRowIndex);

            let generalErrors: CsvError = {
                index: "Erreurs générales",
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
            if (errors.emptyHeaders.length > 0) {
                generalErrors.list.emptyHeaders = errors.emptyHeaders;
                generalErrors.listSize++;
                if (!generalErrors.firstErrorType) {
                    generalErrors.firstErrorType = "emptyHeaders";
                }
            }

            if (errors.invalidHeaderURIs.length > 0) {
                generalErrors.list.invalidHeaderURIs = errors.invalidHeaderURIs;
                generalErrors.listSize++;
                if (!generalErrors.firstErrorType) {
                    generalErrors.firstErrorType = "invalidHeaderURIs";
                }
            }

            if (errors.missingRequiredValueErrors.length > 0) {
                generalErrors.list.missingRequiredValueErrors = errors.missingRequiredValueErrors;
                generalErrors.listSize++;
                if (!generalErrors.firstErrorType) {
                    generalErrors.firstErrorType = "missingRequiredValueErrors";
                }
            }

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

            errorsByRowIndex.forEach(error => {
                if (error.firstErrorType) {
                    error.listSize--;
                    let firstErrorType = error.firstErrorType;
                    error.firstErrorType = {
                        type: firstErrorType,
                        validationErrors: error.list[firstErrorType],
                    };
                    delete error.list[firstErrorType];
                }
            });

            // push all element form globalErrors to validationError in one call, in order to perform Vue render only once
            this.validationErrors.push(...errorsByRowIndex.values());
        }
    }

    validationErrors: Array<CsvError> = null;

    getValidationErrorDetail(validationError, errorType, validation: CSVValidationDTO) {
        switch (errorType) {
            case "missingHeaders":
                return this.$t(
                    "OntologyCsvImporter.validationErrorMissingHeaderMessage",
                    {header: validationError}
                );
            case "emptyHeaders":
                return this.$t(
                    "OntologyCsvImporter.validationErrorEmptyHeaderMessage",
                    {header: validationError}
                );
            case "missingRequiredValueErrors":
                return this.$t(
                    "OntologyCsvImporter.validationErrorMissingRequiredMessage",
                    validationError
                );
            case "duplicateURIErrors":
                return this.$t(
                    "OntologyCsvImporter.validationErrorDuplicateURIMessage",
                    validationError
                );
            case "datatypeErrors":
                return this.$t(
                    "OntologyCsvImporter.validationErrorDatatypeMessage",
                    validationError
                );
            case "invalidDateErrors":
                return this.$t( validationError.message);
            case "invalidRowSizeErrors":
                return this.$t(
                    "OntologyCsvImporter.validationErrorInvalidRowSizeErrorsMessage",
                    {row_size: validationError.colIndex, header_size: validation.errors.csvHeader.realCsvHeaderLength} // append offset since (uri,type) column are not counted
                );
            case "missingToValue":
                return this.$t(
                    "OntologyCsvImporter.missingToValue",
                    validationError
                );
            default:
                return this.$t(
                    "OntologyCsvImporter.validationErrorMessage",
                    validationError
                );
        }
    }


    private static loadErrorType(errorType: string, errors: CSVValidationModel, globalErrors: Map<number,CsvError>) {

        for (let i in errors[errorType]) {
            let errorList = errors[errorType][i];
            if (!Array.isArray(errorList)) {
                errorList = [errors[errorType][i]];
            }
            for (let j in errorList) {
                let errorItem = errorList[j];
                let rowIndex: number = errorItem.rowIndex;

                let globalError: CsvError = globalErrors.get(rowIndex);
                if (!globalError) {
                    globalError = {
                        index: ""+rowIndex,
                        list: {},
                        listSize: 1,
                        firstErrorType: undefined
                    };
                    globalErrors.set(rowIndex,globalError);
                }


                if (!globalError.list) {
                    globalError.list = {};
                }

                if (!globalError.firstErrorType) {
                    globalError.firstErrorType = errorType;
                }

                if (!globalError.list[errorType]) {
                    globalError.list[errorType] = [];
                    globalError.listSize++;
                }

                globalError.list[errorType].push(errorItem);
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

.multi-line-cell {
    white-space: pre;
}

.uri-cell {
    white-space: pre-warp;
}
</style>


<i18n>
en:
    OntologyCsvImporter:
        import: CSV Import
        object-type: object type
        object-type-placeholder: Type here to search in object types
        expectedFormat: Expected CSV format
        objectURI: Object URI
        objectURIComment: You can set a custom URI or leave it empty to generate one.
        dataPlaceholder: Your can insert your data from this row.
        ignoredFirstRows: First three rows of CSV content will be ignored.
        columnModification: You can change columns' order and add new ones as long as you don't change the ID of the first row.
        csvErrors: Error(s) detected in CSV file
        missingHeaders: Missing column headers
        emptyHeaders: Header with empty column
        invalidHeaderURIs: Invalid header URI
        datatypeErrors: Data type error
        uriNotFoundErrors: URI not found
        invalidURIErrors: Invalid URI
        missingRequiredValueErrors: Missing required value
        invalidValueErrors: Invalid value
        alreadyExistingURIErrors: URI already existing
        duplicateURIErrors: Duplicate URI
        invalidRowSizeErrors: Invalid row size
        validationErrorMessage: "Column: '{header}' - Value: '{value}' - Details: '{message}'"
        validationErrorMissingRequiredMessage: "Column: '{header}' - Details : '{message}'"
        validationErrorMissingHeaderMessage: "Column: '{header}'"
        validationErrorEmptyHeaderMessage: "Header: column '{header}' - Empty column"
        validationErrorDuplicateURIMessage: "Header: column '{header}' - Value: '{value}' - Identical with row: '{previousRow}'"
        validationErrorDatatypeMessage: "Column: '{header}' - Value: '{value}' ({datatype})"
        validationErrorInvalidRowSizeErrorsMessage: "Invalid row size : {row_size}. Row size for this line must be equals to the CSV header size : {header_size}"
        CSVIsValid: Your CSV file has been successfully validated ({nb_lines} row(s) found), click OK to import it
        csv-file-placeholder: Drop or select CSV file here...
        csv-file-drop-placeholder: Drop CSV file here...
        csv-file-select-button: Select
        downloadTemplate: Download CSV template
        separator: separator
        csv-import-success-message: CSV file imported successfully
        line: Line
        errorType: "Error type"
        detail: Detail
        invalidDateErrors: Invalid date
        missingToValue: "Cannot declare a move with a 'From' value but without a 'To' value."
fr:
    OntologyCsvImporter:
        import: Import CSV
        object-type: type d'objet
        object-type-placeholder: Utiliser cette zone pour rechercher un type d'objet
        expectedFormat: Format de fichier CSV attendu
        objectURI: URI de l'objet
        objectURIComment: Vous pouvez définir une URI personnalisé ou laisser vide pour en générer une.
        dataPlaceholder: Vous pouvez insérer vos données à partir de cette ligne.
        ignoredFirstRows: Les trois premières lignes de contenu du CSV seront ignorées.
        columnModification: Vous pouvez changer l'ordre des colonnes et en ajouter tant que vous ne modifiez pas l'identifiant de la première ligne.
        csvErrors: Erreur(s) détectée(s) lors de la validation du fichier CSV
        missingHeaders: En-tête de colonne manquant
        emptyHeaders: En-tête avec une ou plusieurs colonne(s) vide(s)
        invalidHeaderURIs: URI d'en-tête invalide
        datatypeErrors: Type de donnée invalide
        uriNotFoundErrors: URI non trouvée
        invalidURIErrors: URI invalide
        missingRequiredValueErrors: Valeur obligatoire manquante
        invalidValueErrors: Valeur invalide
        alreadyExistingURIErrors: URI déjà existante
        duplicateURIErrors: URI dupliquée
        invalidRowSizeErrors: Taile de ligne invalide
        validationErrorMessage: "Colonne: '{header}' - Valeur: '{value}' - Détail: '{message}'"
        validationErrorMissingRequiredMessage: "Column: '{header}' - Details : '{message}'"
        validationErrorMissingHeaderMessage: "En-tête: '{header}'"
        validationErrorEmptyHeaderMessage: "En-tête: '{header}' - Colonne vide"
        validationErrorDuplicateURIMessage: "Colonne: '{header}' - Valeur: '{value}' - Identique à la ligne: '{previousRow}'"
        validationErrorDatatypeMessage: "Colonne: '{header}' - Valeur: '{value}' ({datatype})"
        validationErrorInvalidRowSizeErrorsMessage: "Taille de ligne invalide : {row_size}. Le nombre de colonne pour cette ligne doit être égal à la taille de l'entête CSV : {header_size}"
        CSVIsValid: Votre fichier CSV est valide ({nb_lines} ligne lue(s)), cliquer sur OK pour l'importer
        csv-file-placeholder: Déposer ou choisir un fichier CSV ici...
        csv-file-drop-placeholder: Déposer le fichier CSV ici...
        csv-file-select-button: Parcourir
        downloadTemplate: Télécharger le modèle de fichier CSV
        separator: séparateur
        csv-import-success-message: Fichier CSV importé
        line: Ligne
        errorType: "Type d'erreur"
        detail: Détail
        invalidDateErrors: Date invalide
        missingToValue: "Impossible de déclarer un déplacement avec une valeur 'De' mais sans valeur 'Vers'."
</i18n>
