<template>
  <b-form>
    <b-row>
      <b-col cols="5">
        <opensilex-FormField
          :required="true"
          label="DataImportForm.use-default-provenance"
          helpMessage="DataImportForm.use-default-provenance-help"
        >
          <template v-slot:field="field">
            <b-form-checkbox
              v-model="selectDefaultProvenance"
              switch
              @change="changeCheckBox($event)"
            >
              {{ $t("DataImportForm.use-default-provenance-title") }}
            </b-form-checkbox>
          </template>
        </opensilex-FormField>
      </b-col>
      <b-col>
        <opensilex-ProvenanceSelector
          v-if="!selectDefaultProvenance"
          ref="provenanceSelector"
          :provenances.sync="form.provenance.uri"
          :filterLabel="filterProvenanceLabel"
          label="component.data.form.selector.placeholder"
          @select="loadProvenanceAndCheckUploadedData"
          @clear="reset()"
          :multiple="false"
          :actionHandler="showProvenanceCreateForm"
          :viewHandler="showProvenanceDetails"
          :viewHandlerDetailsVisible="visibleDetails"
          :showURI="false"
          :required="true"
        ></opensilex-ProvenanceSelector>
        <b-collapse id="collapse-4" v-model="visibleDetails" class="mt-2">
          <opensilex-ProvenanceDetails
            :provenance="this.form.provenance"
          ></opensilex-ProvenanceDetails>
        </b-collapse>
      </b-col>
    </b-row>

    <opensilex-ModalForm
      ref="provenanceForm"
      component="opensilex-ProvenanceForm"
      createTitle="ProvenanceView.add"
      editTitle="ProvenanceView.update"
      icon="fa#seedling"
      modalSize="lg"
      @hide="validateProvenanceForm = 'true'"
      :initForm="initForm"
      :successMessage="successMessage"
      :validationDisabled="validateProvenanceForm"
      @onCreate="afterCreateProvenance"
    ></opensilex-ModalForm>

    <!-- Upload file  -->
    <opensilex-GenerateDataTemplateFrom
      ref="templateForm"
      :experiment="form.experiment"
      :hasDeviceAgent="hasDeviceAgent"
    ></opensilex-GenerateDataTemplateFrom>
    <div>
      <label
        >{{ $t("DataImportForm.import-file") }}
        <span class="required">*</span></label
      >

      <b-row>
        <b-col cols="4">
          <b-form-file
            size="sm"
            ref="inputFile"
            accept="text/csv, .csv"
            @input="uploadCSV"
            :placeholder="$t('DataImportForm.csv-file-placeholder')"
            :drop-placeholder="$t('DataImportForm.csv-file-drop-placeholder')"
            :browse-text="$t('component.common.import-files.select-button')"
            v-model="file"
            :state="Boolean(file)"
          ></b-form-file>
        </b-col>
        <b-col cols="2">
          <opensilex-Button
            variant="secondary"
            @click="templateForm.show()"
            class="mr-2"
            :small="false"
            icon
            label="DataView.buttons.generate-template"
          ></opensilex-Button>
        </b-col>
        <b-col cols="5"></b-col>
      </b-row>
      <br />
    </div>
    <div>
      <opensilex-DataHelpTableView :experiment="form.experiment">
      </opensilex-DataHelpTableView>
    </div>
    <!-- validation report  -->
    <opensilex-DataValidationReport
      v-if="form.dataFile != null"
      ref="validationReport"
    >
    </opensilex-DataValidationReport>
    <p v-if="!isImported && isValid && insertionError" class="alert-warning">
      {{ $t("DataImportForm.data-not-imported") }}
    </p>
    <p
      v-if="!isImported && tooLargeDataset && insertionError"
      class="alert alert-warning"
    >
      {{ $t("DataImportForm.data-too-much-data") }}
    </p>
    <p
      v-if="
        insertionError && form.dataFile != null && insertionDataError != null
      "
      class="alert alert-warning"
    >
      {{ $t("DataImportForm.error") }} : {{ insertionDataError.title }}
      <br />
      {{ $t("DataImportForm.message") }} : {{ insertionDataError.message }}
    </p>
    <br />
  </b-form>
</template>


<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import Oeso from "../../../ontologies/Oeso";
import moment from "moment";

// @ts-ignore
import { AgentModel, ProvenanceGetDTO, RDFTypeDTO } from "opensilex-core/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
// @ts-ignore
import { ExperimentGetDTO } from "opensilex-core/index";

@Component
export default class DataImportForm extends Vue {
  $opensilex: any;
  $i18n: any;
  $store: any;
  $papa: any;
  $t: any;
  file = null;

  @Ref("warningModal") readonly warningModal!: any;
  @Ref("provenanceForm") readonly provenanceForm!: any;
  @Ref("provenanceSelector") readonly provenanceSelector!: any;
  @Ref("validationReport") readonly validationReport!: any;
  @Ref("templateForm") readonly templateForm!: any;

  users: any[] = [];
  visibleDetails: boolean = false;

  insertionError: boolean = false;
  insertionDataError: any = null;
  isImported: boolean = false;
  isValid: boolean = false;
  duplicateData: boolean = false;
  duplicatedData: any[] = [];
  tooLargeDataset: boolean = false;
  importedLines: number = 0;
  withSOcolumn: boolean = true;
  validateProvenanceForm: boolean = true;
  showTable: boolean;

  provenance = null;
  experiments = [];
  filterProvenanceLabel = null;
  selectDefaultProvenance: boolean = true;
  hasDeviceAgent: boolean = false;
  standardProvURI;

  @Prop({
    default: () => {
      return {
        provenance: {
          uri: null,
          name: null,
          comment: null,
          provActivity: [],
          provAgent: [],
        },
        dataFile: null,
      };
    },
  })
  form;

  created() {
    this.$opensilex
      .getService("opensilex.DataService")
      .searchProvenance("standard_provenance")
      .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
        this.standardProvURI = http.response.result[0].uri;
      });
  }

  getEmptyForm() {
    return {
      provenance: {
        uri: null,
        name: null,
        comment: null,
        prov_activity: [],
        prov_agent: [],
      },
      dataFile: null,
      experiment: null,
    };
  }

  showProvenanceDetails() {
    if (this.form.provenance != null) {
      this.visibleDetails = !this.visibleDetails;
    }
  }

  showProvenanceCreateForm() {
    this.validateProvenanceForm = false;
    this.provenanceForm.showCreateForm();
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get user() {
    return this.$store.state.user;
  }

  successMessage(form) {
    return this.$t("DataImportForm.provenance.success-message");
  }

  initForm(form) {
    let date = new Date();
    let formattedDate = moment(date).format("YYYY-MM-DDTHH:MM:SS");
    let name = "PROV_" + formattedDate;
    form.name = name;
    form.experiments = [];
  }

  selectedOperators(valueToSelect) {
    let agent: AgentModel = {};
    agent.uri = valueToSelect.id;
    agent.rdf_type = Oeso.OPERATOR_TYPE_URI;
    agent.settings = {};
    this.form.provenance.prov_agent.push(agent);
  }

  deselectedOperators(valueToDeselect) {
    this.form.provenance.prov_agent = this.form.provenance.prov_agent.filter(
      function (agent, index, arr) {
        return agent.uri != valueToDeselect.id;
      }
    );
  }

  resetProvenanceForm() {
    this.users = [];
    this.provenance = null;
    this.filterProvenanceLabel = null;
    this.form.provenance = {
      uri: null,
      name: null,
      comment: null,
      experiments: this.form.experiments,
      prov_activity: [],
      prov_agent: [],
    };
  }

  afterCreateProvenance(data) {
    this.provenance = data.uri;
    this.provenanceSelector.select({ id: data.uri, label: data.name });
  }

  getProvenance(uri) {
    if (uri != undefined && uri != null) {
      return this.$opensilex
        .getService("opensilex.DataService")
        .getProvenance(uri)
        .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
          return http.response.result;
        });
    }
  }

  loadProvenanceAndCheckUploadedData(selectedValue) {
    this.hasDeviceAgent = false;
    if (selectedValue != undefined && selectedValue != null) {
      this.getProvenance(selectedValue.id).then((prov) => {
        this.form.provenance = prov;
        this.hasDevice(prov);
      });

      this.checkUploadedData();
    }
  }

  create() {
    this.$opensilex.enableLoader();
    return new Promise((resolve, reject) => {
      if (this.isImported) {
        resolve(true);
      } else {
        let promise;
        promise = this.$opensilex.uploadFileToService(
          "/core/data/import",
          {
            file: this.form.dataFile,
          },
          {
            provenance: this.form.provenance.uri,
            experiment: this.form.experiment ? this.form.experiment : null,
          }
        );

        return promise
          .then((data) => {
            this.checkCSVValidation(data);
            if (this.isValid) {
              let results = data.result;

              if ("message" in results) {
                this.insertionDataError = results;
                this.isImported = false;
                this.insertionError = true;
                this.$opensilex.disableLoader();
                resolve(false);
              } else {
                if (results.dataErrors.tooLargeDataset) {
                  this.tooLargeDataset = true;
                  this.isImported = false;
                  this.insertionError = true;
                  this.$opensilex.disableLoader();
                } else if (results.dataErrors.duplicateData) {
                  this.importedLines = results.dataErrors.nbLinesImported;
                  this.duplicateData = true;
                  this.duplicatedData = results.dataErrors.duplicatedData;
                  this.isImported = false;
                  this.insertionError = true;
                  this.$opensilex.disableLoader();
                  resolve(false);
                } else {
                  this.importedLines = results.dataErrors.nbLinesImported;
                  this.isImported = true;
                  this.insertionError = false;
                  this.$opensilex.disableLoader();
                  resolve({ validation: results, form: this.form });
                }
              }
            }
          })
          .catch((e) => {
            console.error(e);
          });
      }
    });
  }

  reset() {
    this.resetProvenanceForm();
    this.resetValidationPart();
    this.file = null;
    this.form.dataFile = null;
  }

  resetValidationPart() {
    this.visibleDetails = false;

    this.insertionError = false;
    this.insertionDataError = null;
    this.isImported = false;
    this.isValid = false;
    this.duplicateData = false;
    this.duplicatedData = [];
    this.tooLargeDataset = false;
    this.importedLines = 0;
  }

  uploadCSV(data) {
    this.$opensilex.enableLoader();
    this.form.dataFile = data;
    this.checkUploadedData();
  }

  checkUploadedData() {
    if (this.selectDefaultProvenance) {
      this.form.provenance.uri = this.standardProvURI;
      this.form.provenance.name = "standard provenance";
    }
    if (this.form.dataFile !== null && this.form.provenance.uri !== null) {
      this.insertionError = false;
      this.insertionDataError = null;
      this.isImported = false;
      this.isValid = false;
      this.duplicateData = false;
      this.duplicatedData = [];
      this.tooLargeDataset = false;
      return this.$opensilex
        .uploadFileToService(
          "/core/data/import_validation",
          {
            file: this.form.dataFile,
          },
          {
            provenance: this.form.provenance.uri,
            experiment: this.form.experiment ? this.form.experiment : null,
          }
        )
        .then((response) => {
          this.checkCSVValidation(response);
          this.$opensilex.disableLoader();
        })
        .catch((e) => {
          console.error(e);
        });
    } else {
      if (this.validationReport != undefined) {
        this.validationReport.reset();
      }
      this.resetValidationPart();
      this.$opensilex.disableLoader();
    }
  }

  checkCSVValidation(response) {
    let errors = response.result.dataErrors;
    this.validationReport.sizeMax = response.result.sizeMax;
    this.validationReport.checkValidation(errors, false);
    this.isValid = this.validationReport.isValid;
  }

  provenanceGetDTOToSelectNode(dto) {
    if (dto) {
      return {
        id: dto.uri,
        label: dto.name,
      };
    }
    return null;
  }

  hasDevice(provenance) {
    let uris = [];
    for (let i in provenance.prov_agent) {
      uris.push(provenance.prov_agent[i].uri);
    }

    if (uris.length > 0) {
      let body = {
        uris: uris,
      };
      this.$opensilex
        .getService("opensilex.OntologyService")
        .checkURIsTypes(new Array(Oeso.DEVICE_TYPE_URI), body)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let results = http.response.result;
          for (let i in results) {
            if (results[i].rdf_types.includes(Oeso.DEVICE_TYPE_URI)) {
              this.hasDeviceAgent = true;
              break;
            }
          }
        })
        .catch(this.$opensilex.errorHandler);
    }
  }

  changeCheckBox(value) {
    if (value) {
      this.hasDeviceAgent = false;
    }
  }
}
</script>
<style scoped lang="scss">
</style>
<i18n>
en:
  DataImportForm:
    use-default-provenance: Use standard provenance 
    use-default-provenance-title: Uncheck to select another provenance
    use-default-provenance-help: The data will be linked to a default provenance. Uncheck to select another provenance.
    experiments: Select the experiments associated to the data (optionnal)
    add-device-column: Add device column 
    add-device-column-title: Check if you need to specify a device on each piece of data
    add-device-column-help: Check if you need to add a device column, then select the device types. If the device is already defined in the provenance, then it is not necessary to specify device.
    create: Add Data
    update: Update
    experiment: Choose experiment
    describe: Describe information involved in the production of data
    provenance-success-message: Provenance has been successfully created      
    choose-experiment: First choose an experiment
    choosen-experiment: Choosen experiment
    choosen-provenance: Choosen provenance
    selected-file: Selected file
    no-selected-file: No selected file
    csv-file-placeholder: Drop CSV Data file or select a file...
    csv-file-drop-placeholder: Drop CSV Data file here...
    data-duplicated: Duplicated data
    data-not-imported: Data has not been imported. An error has occured during the importation process
    error: Erreur 
    message: Message
    reset-file: Reset file
    import-file : Import data CSV
    
fr:
  DataImportForm:
    use-default-provenance: Utiliser la provenance standard
    use-default-provenance-title: Décocher pour choisir une autre provenance
    use-default-provenance-help: Les données seront liées à la provenance standard. Décocher pour sélectionner une autre provenance.
    experiments: Selectionner les expérimentations associées aux données (facultatif)
    add-device-column: Ajouter des colonnes equipement
    add-device-column-title: Cocher pour spécifier un équipement sur chaque donnée
    add-device-column-help: Cocher pour ajouter des colonnes équipements et sélectionner les types d'équipements à ajouter. Si l'équipement en question est déjà spécifié dans la provenance, il n'est pas nécessaire de le repréciser dans le fichier d'import.
    create: Ajouter des données
    update: Modifier
    experiment: Choisir une experimentation
    describe: Décrivez les informations impliquées dans la production des données
    provenance-success-message: La provenance a été créé avec succès
    choose-experiment: Choisissez une expérimentation
    choosen-experiment: Expérimentation choisie
    choosen-provenance: Provenance choisie
    selected-file: Fichier sélectionné
    no-selected-file: Aucun fichier sélectionné
    csv-file-placeholder: Déposez le CSV de données ici ou sélectionner un fichier..
    csv-file-drop-placeholder: Déposez le CSV de données ici ...    
    data-duplicated: Données dupliquées
    data-not-imported: Données non importées. Une erreur s'est produite durant le processus d'importation
    error: Error
    message: Message
    reset-file: Reinitialiser fichier
    import-file : Importez des données
    
</i18n>
