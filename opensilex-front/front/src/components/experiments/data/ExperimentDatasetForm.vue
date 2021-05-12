<template>
  <b-form>
    <opensilex-ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_DATA_MODIFICATION_ID) && user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
      ref="provenanceForm"
      modalSize="lg"
      :initForm="initForm"
      component="opensilex-ProvenanceForm"
      createTitle="DatasetForm.provenance.label"
      editTitle="component.factor.update"
      icon="fa#sun"
      @onCreate="afterCreateProvenance"
      :successMessage="successMessage"
    ></opensilex-ModalForm>
    <br />

    <!-- Provenance selector if Experiment not choosen-->
    <div v-if="form.experiment != null">
      <h4>{{ $t("DatasetForm.describe") }}</h4>
      <opensilex-ProvenanceSelector
        ref="provenanceSelector"
        :provenances.sync="provenance"
        :filterLabel="filterLabel"
        label="DatasetForm.provenance.name-search"
        @select="loadProvenanceAndCheckUploadedData"
        @clear="resetProvenanceForm"
        :multiple="false"
        :actionHandler="showCreateForm"
        :viewHandler="showProvenanceDetails"
        :viewHandlerDetailsVisible="visibleDetails"
        :required="true"
      ></opensilex-ProvenanceSelector>
      <b-collapse id="collapse-4" v-model="visibleDetails" class="mt-2">
        <opensilex-ProvenanceDetails
          :provenance="this.form.provenance"
        ></opensilex-ProvenanceDetails>
      </b-collapse>
      <!-- Upload file  -->
      <opensilex-GenerateDataTemplateFrom
        :experiment="this.form.experiment"
        :selectExperiment="false"
        ref="templateForm"
      ></opensilex-GenerateDataTemplateFrom>
      <div>
        <label
          >{{ $t("DatasetForm.dataFile") }}
          <span class="required">*</span></label
        >

        <b-row>
          <b-col cols="4">
            <b-form-file
              size="sm"
              ref="inputFile"
              accept="text/csv, .csv"
              @input="uploadCSV"
              :placeholder="$t('DatasetForm.csv-file-placeholder')"
              :drop-placeholder="$t('DatasetForm.csv-file-drop-placeholder')"
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
        <opensilex-DataHelpTableView></opensilex-DataHelpTableView>
      </div>
      <!-- validation report  -->
      <opensilex-DataValidationReport
        v-if="form.dataFile != null"
        ref="validationReport"
      >
      </opensilex-DataValidationReport>
      <p v-if="!isImported && isValid && insertionError" class="alert-warning">
        {{ $t("DatasetForm.data-not-imported") }}
      </p>
      <p
        v-if="!isImported && tooLargeDataset && insertionError"
        class="alert alert-warning"
      >
        {{ $t("DatasetForm.data-too-much-data") }}
      </p>
      <p
        v-if="
          insertionError && form.dataFile != null && insertionDataError != null
        "
        class="alert alert-warning"
      >
        {{ $t("DatasetForm.error") }} : {{ insertionDataError.title }}
        <br />
        {{ $t("DatasetForm.message") }} : {{ insertionDataError.message }}
      </p>
      <br />
    </div>
    <div v-else>{{ $t("DatasetForm.choose-experiment") }}</div>
  </b-form>
</template>


<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import Oeso from "../../../ontologies/Oeso";
import moment from "moment";

// @ts-ignore
import { AgentModel, ProvenanceGetDTO } from "opensilex-core/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
// @ts-ignore
import { ExperimentGetDTO } from "opensilex-core/index";

@Component
export default class DatasetForm extends Vue {
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

  provenance = null;
  filterLabel = null;

  @Prop({
    default: () => {
      return {
        provenance: {
          uri: null,
          name: null,
          comment: null,
          experiments: [],
          provActivity: [],
          provAgent: [],
        },
        experiment: null,
        dataFile: null,
        selectExperiment: null,
      };
    },
  })
  form;

  getEmptyForm() {
    return {
      provenance: {
        uri: null,
        name: null,
        comment: null,
        experiments: [],
        prov_activity: [],
        prov_agent: [],
      },
      experiment: null,
      dataFile: null,
      selectExperiment: null,
    };
  }

  showProvenanceDetails() {
    if (this.provenance != null) {
      this.visibleDetails = !this.visibleDetails;
    }
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get user() {
    return this.$store.state.user;
  }
  showCreateForm() {
    this.provenanceForm.showCreateForm();
  }

  successMessage(form) {
    return this.$t("DatasetForm.provenance.success-message");
  }

  initForm(form) {
    let date = new Date();
    let formattedDate = moment(date).format("YYYY-MM-DDTHH:MM:SS");
    let service = this.$opensilex.getService("opensilex.ExperimentsService");
    service
      .getExperiment(this.form.experiment)
      .then((http: HttpResponse<OpenSilexResponse<ExperimentGetDTO>>) => {
        let experimentName = http.response.result.name;
        let name = "PROV_" + experimentName.toUpperCase() + "_" + formattedDate;
        form.name = name;
        form.experiments = [this.form.experiment];
      })
      .catch((error) => {
        this.$opensilex.errorHandler(error);
      });
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
    this.filterLabel = null;
    let experiment = this.form.experiment;
    this.form.provenance = {
      uri: null,
      name: null,
      comment: null,
      experiments: [{ uri: experiment }],
      prov_activity: [],
      prov_agent: [],
    };
  }

  afterCreateProvenance(data) {
    data.then((data) => {
      this.provenance = data.uri;
      this.filterLabel = data.name;
      this.provenanceSelector.select({ id: data.uri, label: data.name });
    });
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
    if (selectedValue != undefined && selectedValue != null) {
      this.getProvenance(selectedValue.id).then((prov) => {
        this.form.provenance = prov;
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
        return this.$opensilex
          .uploadFileToService(
            "/core/experiments/" +
              encodeURIComponent(this.form.experiment) +
              "/data/import",
            {
              file: this.form.dataFile,
            },
            {
              provenance: this.form.provenance.uri,
            }
          )
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
                  this.importedLines = results.dataErrors.nb_lines_imported;
                  this.duplicateData = true;
                  this.duplicatedData = results.dataErrors.duplicatedData;
                  this.isImported = false;
                  this.insertionError = true;
                  this.$opensilex.disableLoader();
                  resolve(false);
                } else {
                  this.importedLines = results.dataErrors.nb_lines_imported;
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
    if (this.form.dataFile !== null && this.provenance !== null) {
      this.insertionError = false;
      this.insertionDataError = null;
      this.isImported = false;
      this.isValid = false;
      this.duplicateData = false;
      this.duplicatedData = [];
      this.tooLargeDataset = false;

      return this.$opensilex
        .uploadFileToService(
          "/core/experiments/" +
            encodeURIComponent(this.form.experiment) +
            "/data/import_validation",
          {
            file: this.form.dataFile,
          },
          {
            provenance: this.form.provenance.uri,
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
}
</script>
<style scoped lang="scss">
</style>
<i18n>
en:
  DatasetForm:
    create: Add Data
    update: Update
    experiment: Choose experiment
    describe: Describe information involved in the production of data
    provenance: 
      label: Provenance
      operators: Operators
      operators-help: Who have generated these data ?
      name: Name 
      name-search: Data provenance name in this experiment
      name-help: Give a name
      description : Description
      description-help : Other comment(s) about your data environment parameters
      description-placeholder : Add description
      title: Create a new provenance
      title-create : Create a new provenance
      title-use : Use an already existing provenance
      already-exists: Provenance already existing
      success-message : Provenance has been successfully created
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
    dataFile : Import data CSV
    
fr:
  DatasetForm:
    create: Ajouter des données
    update: Modifier
    experiment: Choisir une experimentation
    describe: Décrivez les informations impliquées dans la production des données
    provenance:
      label: Provenance
      operators: Operateurs
      agent-help: Qui a produit ces données ?
      name: Nom 
      name-search: Nom de la provenance des données de l'expérimentation
      name-help: Nommez votre provenance
      description: Description
      description-help: Autres commentaires à propos de vos données ?
      description-placeholder: Ajouter une description
      title-create: Créer la provenance des données
      title-use: Utilisation de la provenance des données
      already-exists: La provenance existe déjà
      success-message : La provenance a été créé avec succès
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
    dataFile : Importez des données
    
</i18n>
