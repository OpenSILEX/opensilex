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
            <b-form-checkbox v-model="selectDefaultProvenance" switch>
              {{$t('DataImportForm.use-default-provenance-title')}}
            </b-form-checkbox>
          </template>
        </opensilex-FormField>
      </b-col>
      <b-col>
        <opensilex-ProvenanceSelector     
          v-if="!selectDefaultProvenance" 
          ref="provenanceSelector"
          :provenances.sync="provenance"
          :filterLabel="filterProvenanceLabel"
          label="Select a provenance"
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
    <opensilex-ExperimentSelector
      label="DataImportForm.experiments"
      :experiments.sync="experiments"
      :multiple="true"
    ></opensilex-ExperimentSelector>

    <!-- <opensilex-SelectForm
      label="DataView.filter.provenance"
      placeholder="DataView.filter.provenance-placeholder"
      :selected.sync="provenance"
      :conversionMethod="provenanceGetDTOToSelectNode"
      modalComponent="opensilex-ProvenanceModalList"
      :isModalSearch="true"
      :clearable="false"
      
    ></opensilex-SelectForm> -->

    
    <b-row>
      <b-col cols="5">
        <opensilex-FormField
          :required="true"
          label="DataImportForm.add-device-column"
          helpMessage="DataImportForm.add-device-column-help"
        >
          <template v-slot:field="field">
            <b-form-checkbox v-model="withDeviceColumn" switch>
              {{$t('DataImportForm.add-device-column-title')}}
            </b-form-checkbox>
          </template>
        </opensilex-FormField>
      </b-col>
      <b-col>

        <opensilex-SelectForm
          v-if="withDeviceColumn"
          label="ProvenanceView.agent_type"
          :selected.sync="selectedAgentTypes"
          :options="agentTypes"
          :multiple="true"
          :required="required"
          placeholder="ProvenanceView.agent_type-placeholder"
          @clear="$emit('clear')"
          @select="$emit('select')"
          @deselect="$emit('deselect')"
        ></opensilex-SelectForm>
      </b-col>
    </b-row>


    <opensilex-ModalForm
      ref="provenanceForm"
      component="opensilex-ProvenanceForm"
      createTitle="ProvenanceView.add"
      editTitle="ProvenanceView.update"
      icon="fa#seedling"
      modalSize="lg"
      @hide="validateProvenanceForm='true'"
      :initForm="initForm"
      :successMessage="successMessage"
      :validationDisabled="validateProvenanceForm"
    ></opensilex-ModalForm>
    
      <!-- Upload file  -->
      <opensilex-GenerateDataTemplateFrom
        :selectExperiment="false"
        :acceptSONames="false"
        :deviceColumns="getAgentsNames(selectedAgentTypes)"
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
        <opensilex-DataHelpTableView
          ref="helpTable"
          :acceptSONames="false"
          :visibleAtFirst="false"
          :deviceColumns="getAgentsNames(selectedAgentTypes)"
        >
        </opensilex-DataHelpTableView>
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
export default class DataForm extends Vue {
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
  withDeviceColumn: boolean = false;
  selectedAgentTypes = [];
  agentTypes: any[] = [];

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
        dataFile: null
      };
    },
  })
  form;

  mounted() {
    this.loadAgentTypes();
  }

  agentTypesMap: Map<string, string> = new Map();
  loadAgentTypes() {
    this.$opensilex.getService("opensilex.OntologyService")
    .getSubClassesOf(Oeso.DEVICE_TYPE_URI, true)
    .then((http: HttpResponse<OpenSilexResponse<Array<RDFTypeDTO>>>) => {
      for (let i = 0; i < http.response.result.length; i++) { 
        this.agentTypes.push({
          id: http.response.result[i].uri,
          label: http.response.result[i].name,
        });
        this.agentTypesMap[http.response.result[i].uri] = http.response.result[i].name;
      }      
    })
    .catch(this.$opensilex.errorHandler);   

  }

  getAgentsNames(urisList) {
    let namesList = [];
    for (let i in urisList) {
      namesList.push(this.agentTypesMap[urisList[i]]);
    }
    return namesList;
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
      dataFile: null
    };
  }

  showProvenanceDetails() {
    if (this.provenance != null) {
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
    return this.$t("DatasetForm.provenance.success-message");
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
    data.then((data) => {
      this.provenance = data.uri;
      this.filterProvenanceLabel = data.name;
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
            "/core/data/import",
            {
              file: this.form.dataFile
            },
            {
              experiments: this.experiments,
              provenance: this.provenance
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
    if (this.selectDefaultProvenance) {
      this.provenance="dev:id/provenance/standard_provenance"
    }
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
          "/core/data/import_validation",
          {
            file: this.form.dataFile
          },
          {
            experiments: this.experiments,
            provenance: this.provenance
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
        label: dto.name
      };
    }
    return null;
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
    
fr:
  DataForm:
    use-default-provenance: Utiliser la provenance standard
    use-default-provenance-title: Décocher pour choisir une autre provenance
    use-default-provenance-help: Les données seront liées à la provenance standard. Décocher pour sélectionner une autre provenance.
    experiments: Selectionner les expérimentations associées aux données (facultatif)
    add-device-column: Ajouter des colonnes equipement
    add-device-column-title: Cocher pour spécifier un équipement sur chaque donnée
    add-device-column-help: Cocher pour ajouter des colonnes équipements et sélectionner les types d'équipements à ajouter. Si l'équipement en question est déjà spécifié dans la provenance, il n'est pas nécessaire de le repréciser dans le fichier d'import.
    
</i18n>