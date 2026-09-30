<template>
  <Modal ref="modalRef" modalSize="xl">
    <template #header>
      <i>
        <h4>
          <Icon icon="bi#bi-bar-chart-fill" />
          {{ t("component.data.dataImportForm.create") }}
        </h4>
      </i>
    </template>

    <div class="row">
      <div class="col-5">
        <FormField
          :required="true"
          label="component.data.dataImportForm.use-default-provenance"
          helpMessage="component.data.dataImportForm.use-default-provenance-help"
        >
          <template #field>
            <div class="switch-field">
              <n-switch v-model:value="selectDefaultProvenance" :theme-overrides="switchThemeOverrides" />
              <span class="switch-field__label" @click="selectDefaultProvenance = !selectDefaultProvenance">{{ t("component.data.dataImportForm.use-default-provenance-title") }}</span>
            </div>
          </template>
        </FormField>
      </div>
      <div class="col">
        <ProvenanceSelector
          v-if="!selectDefaultProvenance"
          ref="provenanceSelector"
          v-model:provenances="form.provenance.uri"
          label="component.data.dataImportForm.provenance"
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
        <div v-show="visibleDetails" class="mt-2">
          <ProvenanceDetails
            :provenance="form.provenance"
          ></ProvenanceDetails>
        </div>
      </div>
    </div>

    <ProvenanceForm
      v-if="user.hasCredential(credentials.CREDENTIAL_PROVENANCE_MODIFICATION_ID)"
      ref="provenanceForm"
      :createTitle="t('component.data.dataImportForm.add-provenance')"
      :editTitle="t('component.data.dataImportForm.add-provenance')"
      @onCreate="afterCreateProvenance"
    ></ProvenanceForm>

    <!-- Upload file  -->
    <GenerateDataTemplateFrom
      ref="templateForm"
      :experiment="form.experiment"
      :hasDeviceAgent="hasDeviceAgent"
    ></GenerateDataTemplateFrom>
    <div>
      <div class="row align-items-end">
        <div class="col-6">
          <label class="form-label">
            {{ t("component.data.dataImportForm.import-file") }} <span class="text-danger">*</span>
          </label>
          <n-upload
            v-model:file-list="fileList"
            accept="text/csv,.csv"
            :max="1"
            :default-upload="false"
            @update:file-list="onFileListChange"
          >
            <n-upload-dragger>
              <div>{{ t("component.data.dataImportForm.csv-file-placeholder") }}</div>
            </n-upload-dragger>
          </n-upload>
        </div>
        <div class="col-3 mb-3">
          <button
            type="button"
            class="btn greenThemeColor"
            @click="templateForm.show()"
          >{{ t("component.data.dataImportForm.generate-template") }}</button>
        </div>
      </div>
      <br />
    </div>
    <div>
      <DataHelpTableView
        :experiment="form.experiment">
      </DataHelpTableView>
    </div>
    <!-- validation report  -->
    <DataValidationReport
      v-show="form.dataFile != null"
      ref="validationReport"
    >
    </DataValidationReport>
    <p v-if="!isImported && isValid && insertionError" class="alert-warning">
      {{ t("component.data.dataImportForm.data-not-imported") }}
    </p>
    <p
      v-if="!isImported && tooLargeDataset && insertionError"
      class="alert alert-warning"
    >
      {{ t("component.data.dataImportForm.data-too-much-data") }}
    </p>
    <p
      v-if="insertionError && form.dataFile != null && insertionDataError != null"
      class="alert alert-warning"
    >
      {{ t("component.data.dataImportForm.error") }} : {{ insertionDataError.title }}
      <br />
      {{ t("component.data.dataImportForm.message") }} : {{ insertionDataError.message }}
    </p>

    <template #footer>
      <FormFooter @cancel="hide" @submit="submit" />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import {computed, inject, onMounted, ref, useTemplateRef, watch} from "vue";
import {useI18n} from "vue-i18n";
import {useStore} from "vuex";
import Oeso from "@/ontologies/Oeso";
import {ProvenanceGetDTO} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {DataService} from "opensilex-core/api/data.service";
import {OntologyService} from "opensilex-core/api/ontology.service";
import Modal from "@/components/common/views/Modal.vue";
import Icon from "@/components/common/views/Icon.vue";
import FormField from "@/components/common/forms/FormField.vue";
import {NSwitch, NUpload, NUploadDragger} from "naive-ui";
import type {UploadFileInfo} from "naive-ui";
import ProvenanceSelector from "@/components/data/ProvenanceSelector.vue";
import ProvenanceDetails from "@/components/data/ProvenanceDetails.vue";
import ProvenanceForm from "@/components/data/form/ProvenanceForm.vue";
import GenerateDataTemplateFrom from "@/components/data/form/GenerateDataTemplateFrom.vue";
import DataHelpTableView from "@/components/data/form/DataHelpTableView.vue";
import DataValidationReport from "@/components/data/form/DataValidationReport.vue";
import FormFooter from "@/components/common/forms/FormFooter.vue";

const switchThemeOverrides = {
  railColorActive: "#00a38d",
  boxShadowFocus: "0 0 0 2px rgba(0, 163, 141, 0.2)"
};

const props = defineProps<{
  experiment?: string
}>();

const emit = defineEmits<{
  onCreate: [payload: { validation: any, form: any }]
}>();

const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const {t} = useI18n();
const store = useStore();
const dataService = opensilex.getService<DataService>("opensilex.DataService");

const modalRef = useTemplateRef<InstanceType<typeof Modal>>("modalRef");
const provenanceForm = useTemplateRef<InstanceType<typeof ProvenanceForm>>("provenanceForm");
const provenanceSelector = useTemplateRef<InstanceType<typeof ProvenanceSelector>>("provenanceSelector");
const validationReport = useTemplateRef<InstanceType<typeof DataValidationReport>>("validationReport");
const templateForm = useTemplateRef<InstanceType<typeof GenerateDataTemplateFrom>>("templateForm");

const fileList = ref<UploadFileInfo[]>([]);
const visibleDetails = ref<boolean>(false);

const insertionError = ref<boolean>(false);
const insertionDataError = ref<any>(null);
const isImported = ref<boolean>(false);
const isValid = ref<boolean>(false);
const duplicateData = ref<boolean>(false);
const duplicatedData = ref<any[]>([]);
const tooLargeDataset = ref<boolean>(false);
const importedLines = ref<number>(0);

const selectDefaultProvenance = ref<boolean>(true);
const hasDeviceAgent = ref<boolean>(false);
const standardProvURI = ref<string>(undefined);
const validationKey = ref<string>(null);

const form = ref<any>(getEmptyForm());

const credentials = computed(() => {
  return store.state.credentials;
});

const user = computed(() => {
  return store.state.user;
});

onMounted(() => {
  dataService
    .searchProvenance("standard_provenance")
    .then((http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>) => {
      if (http.response.result[0] === undefined) {
        standardProvURI.value = undefined;
      } else {
        standardProvURI.value = http.response.result[0].uri;
      }
    });
});

watch(selectDefaultProvenance, (value) => {
  if (value) {
    hasDeviceAgent.value = false;
  }
});

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
    experiment: props.experiment ?? null
  };
}

function show() {
  form.value = getEmptyForm();
  selectDefaultProvenance.value = true;
  hasDeviceAgent.value = false;
  fileList.value = [];
  validationReport.value?.reset();
  resetValidationPart();
  modalRef.value.show();
}

function hide() {
  modalRef.value.hide();
}

function showProvenanceDetails() {
  if (form.value.provenance != null) {
    visibleDetails.value = !visibleDetails.value;
  }
}

function showProvenanceCreateForm() {
  provenanceForm.value.showCreateForm();
}

function resetProvenanceForm() {
  form.value.provenance = {
    uri: null,
    name: null,
    comment: null,
    experiments: form.value.experiments,
    prov_activity: [],
    prov_agent: [],
  };
}

function afterCreateProvenance(http: HttpResponse<OpenSilexResponse>) {
  let uri = http?.response?.result;
  if (uri) {
    form.value.provenance.uri = uri;
    loadProvenanceAndCheckUploadedData({id: uri});
    provenanceSelector.value?.refresh();
  }
}

function getProvenance(uri) {
  return dataService
    .getProvenance(uri)
    .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
      return http.response.result;
    });
}

function loadProvenanceAndCheckUploadedData(selectedValue) {
  hasDeviceAgent.value = false;
  if (selectedValue != undefined && selectedValue != null) {
    getProvenance(selectedValue.id).then((prov) => {
      form.value.provenance = prov;
      hasDevice(prov);
    });

    checkUploadedData();
  }
}

function create(): Promise<any> {
  opensilex.enableLoader();
  return new Promise((resolve, reject) => {
    if (isImported.value) {
      resolve(true);
    } else {
      opensilex.uploadFileToService(
        "/core/data/import",
        {
          file: form.value.dataFile,
        } as any,
        {
          provenance: form.value.provenance.uri,
          experiment: form.value.experiment ? form.value.experiment : null,
          validationKey: validationKey.value
        }
      ).then((data: any) => {
        //First test if there was a mongo insertion error
        if (data.metadata.status === 409) {
          insertionDataError.value = data.result;
          isImported.value = false;
          insertionError.value = true;
          opensilex.disableLoader();
          reject(new Error("Conflict status 409: insertion failed"));
        } else {
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
                resolve(false);
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
                resolve({validation: results, form: form.value});
              }
            }
          } else {
            opensilex.disableLoader();
            resolve(false);
          }
        }
      }).catch((e) => {
        opensilex.disableLoader();
        if (standardProvURI.value === undefined) {
          let message =
            t("component.data.dataImportForm.errorStandardProvenance") +
            " : '" +
            standardProvURI.value +
            "' . " +
            t("component.data.dataImportForm.errorStandardProvenanceTwo");
          opensilex.showErrorToast(message);
        }
        console.error(e);
        opensilex.errorHandler(e);
        resolve(false);
      });
    }
  });
}

function submit() {
  create().then((result) => {
    if (result && result !== true) {
      hide();
      emit("onCreate", result);
    }
  }).catch(() => {
    // insertion error is displayed in the form
  });
}

function reset() {
  resetProvenanceForm();
  resetValidationPart();
  fileList.value = [];
  form.value.dataFile = null;
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

function onFileListChange(list: UploadFileInfo[]) {
  uploadCSV(list[0]?.file ?? null);
}

function uploadCSV(data: File) {
  form.value.dataFile = data ?? null;
  checkUploadedData();
}

function checkUploadedData() {
  if (selectDefaultProvenance.value) {
    form.value.provenance.uri = standardProvURI.value;
    form.value.provenance.name = "standard provenance";
  }
  if (form.value.dataFile != null && form.value.provenance.uri != null) {
    insertionError.value = false;
    insertionDataError.value = null;
    isImported.value = false;
    isValid.value = false;
    duplicateData.value = false;
    duplicatedData.value = [];
    tooLargeDataset.value = false;

    opensilex.enableLoader();
    return opensilex
      .uploadFileToService(
        "/core/data/import_validation",
        {
          file: form.value.dataFile,
        } as any,
        {
          provenance: form.value.provenance.uri,
          experiment: form.value.experiment ? form.value.experiment : null,
        }
      )
      .then((response) => {
        checkCSVValidation(response);
        opensilex.disableLoader();
      })
      .catch((e) => {
        opensilex.disableLoader();
        opensilex.errorHandler(e);
      });
  } else {
    validationReport.value?.reset();
    resetValidationPart();
  }
}

function checkCSVValidation(response) {
  let errors = response.result.dataErrors;
  validationReport.value.setSizeMax(response.result.sizeMax);
  validationReport.value.checkValidation(errors);
  isValid.value = validationReport.value.isValid;
  validationKey.value = errors.validationKey ? errors.validationKey : null;
}

function hasDevice(provenance) {
  let uris = [];
  for (let i in provenance.prov_agent) {
    uris.push(provenance.prov_agent[i].uri);
  }

  if (uris.length > 0) {
    let body = {
      uris: uris
    };
    opensilex.getService<OntologyService>("opensilex.OntologyService")
      .checkURIsTypes(new Array(Oeso.DEVICE_TYPE_URI), body)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let results = http.response.result;
        for (let i in results) {
          if (results[i].rdf_types.includes(Oeso.DEVICE_TYPE_URI)) {
            hasDeviceAgent.value = true;
            break;
          }
        }
      })
      .catch(opensilex.errorHandler);
  }
}

defineExpose({
  show,
  hide
});
</script>

<style scoped lang="scss">
.switch-field {
  display: flex;
  align-items: center;
  gap: 8px;
  color: black;
}

.switch-field__label {
  cursor: pointer;
}
</style>
