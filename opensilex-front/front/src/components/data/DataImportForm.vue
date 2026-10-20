<template>
  <b-form>
    <b-row>
      <b-col cols="5">
        <FormField
          :required="true"
          label="DataImportForm.use-default-provenance"
          helpMessage="DataImportForm.use-default-provenance-help"
        >
          <template v-slot:field="field">
            <b-form-checkbox v-model="selectDefaultProvenance" switch @change="changeCheckBox($event)">
              {{$t('DataImportForm.use-default-provenance-title')}}
            </b-form-checkbox>
          </template>
        </FormField>
      </b-col>
      <b-col>
        <ProvenanceSelector
          v-if="!selectDefaultProvenance"
          ref="provenanceSelector"
          :provenances.sync="form.provenance.uri"
          :filterLabel="filterProvenanceLabel"
          label="component.data.form.selector.placeholder"
          @select="loadProvenanceAndCheckUploadedData"
          @clear="reset()"
          :multiple="false"
          :actionHandler="user.hasCredential(credentials.CREDENTIAL_PROVENANCE_MODIFICATION_ID)
            ? showProvenanceCreateForm
            : undefined"
          :viewHandler="showProvenanceDetails"
          :viewHandlerDetailsVisible="visibleDetails"
          :required="true"
        ></ProvenanceSelector>
        <b-collapse id="collapse-4" v-model="visibleDetails" class="mt-2">
          <ProvenanceDetails
            :provenance="this.form.provenance"
          ></ProvenanceDetails>
        </b-collapse>
      </b-col>
    </b-row>

    <ProvenanceForm
      ref="provenanceForm"
      :createTitle="t('ProvenanceView.add')"
      :editTitle="t('ProvenanceView.update')"
      @onCreate="afterCreateProvenance"
    ></ProvenanceForm>
    
      <!-- Upload file  -->
      <GenerateDataTemplateFrom
        ref="templateForm"
        :experiment="form.experiment"
        :hasDeviceAgent="hasDeviceAgent"
      ></GenerateDataTemplateFrom>
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
            <Button
              @click="templateForm.show()"
              class="mr-2 greenThemeColor"
              :small="false"
              icon:
              label="DataView.buttons.generate-template"
            ></Button>
          </b-col>
          <b-col cols="5"></b-col>
        </b-row>
        <br />
      </div>
      <div>
        <DataHelpTableView
          :experiment="form.experiment">
        </DataHelpTableView>
      </div>
      <!-- validation report  -->
      <DataValidationReport
        v-if="form.dataFile != null"
        ref="validationReport"
      >
      </DataValidationReport>
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


<script setup lang="ts">
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {DataService} from "core/api/data.service";
import {OntologyService} from "opensilex-core/api/ontology.service";
import FormField from "@/components/common/forms/FormField.vue";
import ProvenanceSelector from "@/components/data/ProvenanceSelector.vue";
import ProvenanceDetails from "@/components/data/ProvenanceDetails.vue";
import Button from "@/components/common/buttons/Button.vue";
import {computed, inject, ref, useTemplateRef} from "vue";
import {useI18n} from "vue-i18n";
import {useStore} from "vuex";
import {AgentModel, ProvenanceGetDTO} from "opensilex-core/lib";
import Oeso from "@/ontologies/Oeso";
import DataValidationReport from "@/components/data/DataValidationReport.vue";
import ProvenanceForm from "@/components/data/form/ProvenanceForm.vue";
import DataHelpTableView from "@/components/data/form/DataHelpTableView.vue";
import GenerateDataTemplateFrom from "@/components/data/form/GenerateDataTemplateFrom.vue";

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const { t } = useI18n()
const store = useStore()

const file = ref<any>(null);
const users = ref<any[]>([])
const visibleDetails = ref<boolean>(false)
const insertionError = ref<boolean>(false)
const insertionDataError = ref<any>(null)
const isImported = ref<boolean>(false)
const isValid = ref<boolean>(false)
const duplicateData = ref<boolean>(false)
const duplicatedData = ref<any[]>([])
const tooLargeDataset = ref<boolean>(false)
const importedLines = ref<number>(0)
const withSOcolumn = ref<boolean>(true)
const showTable = ref<boolean>()
const provenance = ref<any>(null)
const experiments = ref([])
const filterProvenanceLabel = ref<any>(null)
const selectDefaultProvenance = ref<boolean>(true)
const hasDeviceAgent = ref<boolean>(false)
const standardProvURI = ref()
const validationKey = ref<string>(null)

const provenanceForm = useTemplateRef<InstanceType<typeof ProvenanceForm>>('provenanceForm')
const provenanceSelector = useTemplateRef<InstanceType<typeof ProvenanceSelector>>('provenanceSelector')
const validationReport = useTemplateRef<InstanceType<typeof DataValidationReport>>('validationReport')
const templateForm = useTemplateRef<InstanceType<typeof Button>>('templateForm')

const props = defineProps({
  form: {
    type: Object,
    default: () => ({
      provenance: {
        uri: null,
        name: null,
        comment: null,
        provActivity: [],
        provAgent: [],
      },
      dataFile: null,
    }),
  },
});

const credentials = computed(() => {
  return store.state.credentials;
})

const user = computed(() => {
  return store.state.user;
})

function created() {
    opensilex.getService<DataService>("opensilex.DataService")
    .searchProvenance("standard_provenance")
    .then((http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>) => {
      if(http.response.result[0] === undefined) {
        standardProvURI.value = undefined;
      } else {
          this.standardProvURI = http.response.result[0].uri;
      }
    });
  }

function getEmptyForm() {
    return {
      provenance: {
        uri: null,
        name: null,
        comment: null,
        prov_activity: [],
        prov_agent: [],
      },
      dataFile: null,
      experiment: null
    };
  }

function showProvenanceDetails() {
    if (props.form.provenance != null) {
      visibleDetails.value = !this.visibleDetails;
    }
  }

function showProvenanceCreateForm() {
    provenanceForm.value.showCreateForm(initForm());
  }

function initForm() {
    let formattedDate = opensilex.dateTimeFormatter.formatISODateTime(new Date());
    return {
      uri: null,
      name: "PROV_" + formattedDate,
      description: null,
      experiments: [],
      activity_type: null,
      activity_start_date: null,
      activity_end_date: null,
      activity_uri: null,
      agentTypes: [],
      agents: [
        {
          uris: [],
          rdf_type: null
        }
      ]
    };
  }

function selectedOperators(valueToSelect) {
    let agent: AgentModel = {};
    agent.uri = valueToSelect.id;
    agent.rdf_type = Oeso.OPERATOR_TYPE_URI;
    agent.settings = {};
    props.form.provenance.prov_agent.push(agent);
  }

function deselectedOperators(valueToDeselect) {
    props.form.provenance.prov_agent = props.form.provenance.prov_agent.filter(
      function (agent) {
        return agent.uri != valueToDeselect.id;
      }
    );
  }

function resetProvenanceForm() {
    users.value = [];
    provenance.value = null;
    filterProvenanceLabel.value = null;
    props.form.provenance = {
      uri: null,
      name: null,
      comment: null,
      experiments: props.form.experiments,
      prov_activity: [],
      prov_agent: [],
    };
  }

function afterCreateProvenance(http: HttpResponse<OpenSilexResponse<string>>) {
    let uri = http.response.result;
    provenance.value = uri;
    getProvenance(uri).then((prov) => {
      provenanceSelector.value.select({ id: uri, label: prov.name });
    });
  }

function getProvenance(uri) {
    if (uri != undefined && uri != null) {
      return opensilex
        .getService<DataService>("opensilex.DataService")
        .getProvenance(uri)
        .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
          return http.response.result;
        });
    }
  }

function loadProvenanceAndCheckUploadedData(selectedValue) {
    hasDeviceAgent.value = false;
    if (selectedValue != undefined && selectedValue != null) {
      this.getProvenance(selectedValue.id).then((prov) => {
        this.form.provenance = prov;
        this.hasDevice(prov);
      });

      this.checkUploadedData();
    }
  }

function create() {
    opensilex.enableLoader();
    return new Promise((resolve, reject) => {
      if (isImported.value) {
        resolve(true);
      } else {
        let promise;

        promise = opensilex.uploadFileToService(
          "/core/data/import",
          {
            file: props.form.dataFile,
          },
          {
            provenance: props.form.provenance.uri,
            experiment: props.form.experiment ? props.form.experiment : null,
            validationKey: validationKey.value
          }
        );

        return promise
          .then((data) => {
            //First test if there was a mongo insertion error
            if(data.metadata.status === 409){
              insertionDataError.value = data.result;
              isImported.value = false;
              insertionError.value = true;
              opensilex.disableLoader();
              reject(new Error("Conflict status 409: insertion failed"));

            }else{
              checkCSVValidation(data);
              if (isValid.value) {
                let results = data.result;

                if ("message" in results) {
                  insertionDataError.value = results;
                  isImported.value = false;
                  insertionError.value = true;
                  opensilex.disableLoader();
                  resolve(false);
                } else {
                  if (results.dataErrors.tooLargeDataset) {
                    tooLargeDataset.value = true;
                    isImported.value = false;
                    insertionError.value = true;
                    opensilex.disableLoader();
                  } else if (results.dataErrors.duplicateData) {
                    importedLines.value = results.dataErrors.nbLinesImported;
                    duplicateData.value = true;
                    duplicatedData.value = results.dataErrors.duplicatedData;
                    isImported.value = false;
                    insertionError.value = true;
                    opensilex.disableLoader();
                    resolve(false);
                  } else {
                    importedLines.value = results.dataErrors.nbLinesImported;
                    isImported.value = true;
                    insertionError.value = false;
                    opensilex.disableLoader();
                    resolve({ validation: results, form: this.form });
                  }
                }
              }
            }
          }).catch((e) => {
            if(standardProvURI.value === undefined) {
              let message =
                t("DataImportForm.errorStandardProvenance") +
                " : '" +
                standardProvURI.value +
                "' . " +
                t("DataImportForm.errorStandardProvenanceTwo")
                opensilex.showErrorToast(message);
            }
            console.error(e);
            opensilex.errorHandler(e);
        });
      }
    });
  }

function reset() {
  resetProvenanceForm();
  resetValidationPart();
  file.value = null;
    props.form.dataFile = null;
  }

 function resetValidationPart() {
    visibleDetails.value = false;
    insertionError.value = false;
    insertionDataError.value = null;
    isImported.value = false;
    isValid.value = false;
    duplicateData.value = false;
    duplicatedData.value = [];
    tooLargeDataset.value = false;
    importedLines.value = 0;
  }

 function uploadCSV(data) {
    opensilex.enableLoader();
    props.form.dataFile = data;
    checkUploadedData();
  }

 function  checkUploadedData() {
    if (selectDefaultProvenance.value) {
      props.form.provenance.uri = standardProvURI.value;
      props.form.provenance.name = "standard provenance";
    }
    if (props.form.dataFile !== null && props.form.provenance.uri !== null) {
      insertionError.value = false;
      insertionDataError.value = null;
      isImported.value = false;
      isValid.value = false;
      duplicateData.value = false;
      duplicatedData.value = [];
      tooLargeDataset.value = false;

     return opensilex
        .uploadFileToService(
          "/core/data/import_validation",
          {
            file: props.form.dataFile,
          },
          {
            provenance: props.form.provenance.uri,
            experiment: props.form.experiment ? props.form.experiment : null,
          }
        )
        .then((response) => {
          checkCSVValidation(response);
          opensilex.disableLoader();
        })
        .catch(this.$opensilex.errorHandler);
      
    } else {
      if (this.validationReport != undefined) {
        this.validationReport.reset();
      }
      this.resetValidationPart();
      this.$opensilex.disableLoader();
    }
  }

 function checkCSVValidation(response) {
    let errors = response.result.dataErrors;
    validationReport.value.sizeMax = response.result.sizeMax;
    validationReport.value.checkValidation(errors, false);
    isValid.value = validationReport.value.isValid;
    validationKey.value = errors.validationKey ? errors.validationKey : null;
  }

 function provenanceGetDTOToSelectNode(dto) {
    if (dto) {
      return {
        id: dto.uri,
        label: dto.name
      };
    }
    return null;
  }

 function hasDevice(provenance) {
    let uris = [];
    for (let i in provenance.prov_agent) {
      uris.push(provenance.prov_agent[i].uri);
    }

    if (uris.length>0) {
      let body = {
        uris: uris
      }
      opensilex.getService<OntologyService>("opensilex.OntologyService")
      .checkURIsTypes(new Array(Oeso.DEVICE_TYPE_URI), body)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => { 
        let results = http.response.result;
        for (let i in results) {
          if (results[i].rdf_types.includes(Oeso.DEVICE_TYPE_URI)) {
            this.hasDeviceAgent= true;
            break;
          }
        }          
      })
      .catch(opensilex.errorHandler);
      }
    
  }

function changeCheckBox(value) {
    if (value) {
      hasDeviceAgent.value = false;
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
    errorStandardProvenance: The selected provenance is
    errorStandardProvenanceTwo: To use the standard provenance, define it beforehand.
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
    errorStandardProvenance: La provenance selectionnée est
    errorStandardProvenanceTwo: Pour utiliser la provenance standard, définissez-la au préalable.
    message: Message
    reset-file: Reinitialiser fichier
    import-file : Importez des données
    
</i18n>
