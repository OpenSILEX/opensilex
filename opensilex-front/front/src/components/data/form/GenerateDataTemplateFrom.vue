<template>
  <Modal ref="modalRef" modalSize="lg">
    <template #header>
      <h5 class="modal-title">{{ t("component.data.dataHelp.title") }}</h5>
    </template>
    <div class="row">
      <div class="col">
        <label class="form-label">{{ t("component.data.dataTemplateForm.select-columns") }}</label>
        <n-form-item>
          <div class="columns-checkboxes">
            <CheckboxForm
              v-for="option in (experiment == null ? options : expeOptions)"
              :key="option.value"
              :title="option.text"
              :value="selectedColumns.includes(option.value)"
              @update:value="(checked) => toggleColumn(option.value, checked)"
            ></CheckboxForm>
          </div>
        </n-form-item>
      </div>
      <div class="col-7">
        <div
          v-if="experiment == null && !validSelection"
          class="alert alert-danger"
        >{{ t("component.data.dataTemplateForm.target-device-required") }}</div>
      </div>
    </div>
    <div class="row">
      <div class="col-9">
        <n-form-item>
        <VariableSelectorWithFilter
            :label="t('component.data.dataHelp.variables')"
          placeholder="component.variable.placeholder-multiple"
          v-model:variables="variables"
        ></VariableSelectorWithFilter>
        </n-form-item>

        <n-form-item>
        <CheckboxForm
          v-model:value="withRawData"
          title="component.data.dataTemplateForm.with-raw-data"
        ></CheckboxForm>
        </n-form-item>

        <n-form-item>
        <CheckboxForm
          v-model:value="withProvUsed"
          title="component.data.dataTemplateForm.with-prov-used"
        ></CheckboxForm>
        </n-form-item>

      </div>
      <div class="col">
        <CSVSelectorInputForm v-model:separator="separator">
        </CSVSelectorInputForm>
      </div>
    </div>
    <template #footer>
      <FormFooter @cancel="hide" @submit="submit" />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import {inject, ref, useTemplateRef} from "vue";
import {useI18n} from "vue-i18n";
import Papa from "papaparse";
import {VariablesService} from "opensilex-core/index";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import Xsd from "@/ontologies/Xsd";
import Modal from "@/components/common/views/Modal.vue";
import CheckboxForm from "@/components/common/forms/CheckboxForm.vue";
import CSVSelectorInputForm from "@/components/common/forms/CSVSelectorInputForm.vue";
import VariableSelectorWithFilter from "@/components/variables/views/VariableSelectorWithFilter.vue";
import FormFooter from "@/components/common/forms/FormFooter.vue";
import {NFormItem} from "naive-ui";

const props = withDefaults(defineProps<{
  experiment?: string
  hasDeviceAgent?: boolean
}>(), {
  experiment: null,
  hasDeviceAgent: false
});

const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const {t} = useI18n();
const service = opensilex.getService<VariablesService>("opensilex.VariablesService");

const modalRef = useTemplateRef<InstanceType<typeof Modal>>("modalRef");

const expColumn = "experiment";
const targetColumn = "target";
const deviceColumn = "device";
const soColumn = "scientific_object";
const annotationColumn = "object_annotation";

const options = [
  {text: expColumn, value: expColumn},
  {text: targetColumn, value: targetColumn},
  {text: deviceColumn, value: deviceColumn},
  {text: annotationColumn, value: annotationColumn}
];

const expeOptions = [
  {text: deviceColumn, value: deviceColumn},
  {text: annotationColumn, value: annotationColumn}
];

const separator = ref<string>(",");
const selectedColumns = ref<string[]>([]);
const withRawData = ref<boolean>(false);
const withProvUsed = ref<boolean>(false);
const validSelection = ref<boolean>(true);
const variables = ref<string[]>([]);

function show() {
  validSelection.value = props.hasDeviceAgent;
  selectedColumns.value = [];
  modalRef.value.show();
}

function hide() {
  modalRef.value.hide();
}

function downloadCsv(content: string, filename: string) {
  const blob = new Blob([content], {type: "text/csv;charset=utf-8;"});
  const url = URL.createObjectURL(blob);

  const link = document.createElement("a");
  link.href = url;
  link.setAttribute("download", `${filename}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  URL.revokeObjectURL(url);
}

function csvExportDataExample() {
  let line1 = [];
  let line2 = [];
  let line3 = [];
  let line4 = [];

  //column experiment
  if (selectedColumns.value.includes(expColumn)) {
    line1.push(expColumn);
    line2.push(t("component.data.dataHelp.experiment-help"));
    line3.push(t("component.data.dataHelp.column-type-help") + getDataTypeLabel("xsd:string"));
    line4.push("exp-test");
  }

  //column object
  if (props.experiment != null) {
    line1.push(soColumn);
    line2.push(t("component.data.dataHelp.objectId-help"));
    line3.push(t("component.data.dataHelp.column-type-help") + getDataTypeLabel("xsd:string"));
    line4.push("test");
  } else {
    if (selectedColumns.value.includes(targetColumn)) {
      line1.push(targetColumn);
      line2.push(t("component.data.dataHelp.targetId-help"));
      line3.push(t("component.data.dataHelp.column-type-help") + getDataTypeLabel("xsd:string"));
      line4.push("test");
    }
  }

  //column device
  if (selectedColumns.value.includes(deviceColumn)) {
    line1.push(deviceColumn);
    line2.push(t("component.data.dataHelp.device-help"));
    line3.push(t("component.data.dataHelp.column-type-help") + getDataTypeLabel("xsd:string"));
    line4.push("device-test");
  }

  //columns date & first variable
  line1.push("Date", "demo:variable#variable.air_temperature");
  line2.push(t("component.data.dataHelp.date-help"), "Air_Temperature");
  line3.push(
    t("component.data.dataHelp.column-type-help") + getDataTypeLabel("xsd:date") + "\n" + t("component.data.dataHelp.required"),
    t("component.data.dataHelp.column-type-help") + getDataTypeLabel("xsd:integer")
  );
  line4.push("2020-09-21T00:00:00+0100", "30");

  //column rawData for first variable
  if (withRawData.value) {
    line1.push("raw_data");
    line2.push(t("component.data.dataTemplateForm.raw-data"));
    line3.push(t("component.data.dataHelp.column-type-help") + t("component.data.dataTemplateForm.type-list") + getDataTypeLabel("xsd:integer"));
    line4.push("30,31,29");
  }

  //column 2nd variable
  line1.push("demo:variable#variable.fruit_color/2");
  line2.push("Fruit_Color");
  line3.push(t("component.data.dataHelp.column-type-help") + getDataTypeLabel("xsd:string"));
  line4.push("Red");

  //column rawData for 2nd variable
  if (withRawData.value) {
    line1.push("raw_data");
    line2.push(t("component.data.dataTemplateForm.raw-data"));
    line3.push(t("component.data.dataHelp.column-type-help") + t("component.data.dataTemplateForm.type-list") + getDataTypeLabel("xsd:string"));
    line4.push("Red,Red,Red");
  }

  //column 3rd variable
  line1.push("demo:variable#variable.veraison_date");
  line2.push("Veraison_Date");
  line3.push(t("component.data.dataHelp.column-type-help") + getDataTypeLabel("xsd:date"));
  line4.push("2020-09-21");

  //column rawData for 3rd variable
  if (withRawData.value) {
    line1.push("raw_data");
    line2.push(t("component.data.dataTemplateForm.raw-data"));
    line3.push(t("component.data.dataHelp.column-type-help") + t("component.data.dataTemplateForm.type-list") + getDataTypeLabel("xsd:date"));
    line4.push("2020-09-21,2020-09-21,2020-09-21");
  }

  //column annotation on object
  if (selectedColumns.value.includes(annotationColumn)) {
    line1.push(annotationColumn);
    line2.push(t("component.data.dataTemplate.annotationHelp"));
    line3.push(t("component.data.dataHelp.column-type-help") + "String");
    line4.push("annotation-test");
  }

  //column provused
  if (withProvUsed.value) {
    line1.push("prov_used");
    line2.push(t("component.data.dataTemplateForm.prov_used"));
    line3.push(t("component.data.dataHelp.column-type-help") + t("component.data.dataTemplateForm.type-list") + getDataTypeLabel("xsd:string"));
    line4.push(t("component.data.dataTemplateForm.prov-used-exemple"));
  }

  let arrData = [line1, line2, line3, line4];
  downloadCsv(Papa.unparse(arrData, {delimiter: separator.value}), "dataTemplateExample");
}

function csvExport() {
  let line1 = [];
  let line2 = [];
  let line3 = [];

  //column experiment
  if (selectedColumns.value.includes(expColumn)) {
    line1.push(expColumn);
    line2.push(t("component.data.dataHelp.experiment-help"));
    line3.push(t("component.data.dataHelp.column-type-help") + getDataTypeLabel("xsd:string"));
  }

  //column object
  if (props.experiment != null) {
    line1.push(soColumn);
    line2.push(t("component.data.dataHelp.objectId-help"));
    line3.push(t("component.data.dataHelp.column-type-help") + getDataTypeLabel("xsd:string"));
  } else {
    if (selectedColumns.value.includes(targetColumn)) {
      line1.push(targetColumn);
      line2.push(t("component.data.dataHelp.targetId-help"));
      line3.push(t("component.data.dataHelp.column-type-help") + getDataTypeLabel("xsd:string"));
    }
  }

  //column devices
  if (selectedColumns.value.includes(deviceColumn)) {
    line1.push(deviceColumn);
    line2.push(t("component.data.dataHelp.device-help"));
    line3.push(t("component.data.dataHelp.column-type-help") + getDataTypeLabel("xsd:string"));
  }

  //columns date
  line1.push("Date");
  line2.push(t("component.data.dataHelp.date-help"));
  line3.push(getDataTypeLabel("xsd:date") + "\n" + t("component.data.dataHelp.required"));

  service.getVariablesByURIs(variables.value)
    .then((http) => {
      for (let element of http.response.result) {

        //column variable
        line1.push(element.uri);
        line2.push(element.name);
        if (element.datatype === undefined || element.datatype === null) {
          element.datatype = Xsd.STRING;
        }
        let variableHelp = t("component.data.dataHelp.column-type-help").toString() +
          opensilex.getVariableDatatypeLabel(element.datatype);
        if (opensilex.compareUris(element.datatype, Xsd.DATE)) {
          variableHelp += " " + t("component.data.dataTemplateForm.format-help.date");
        } else if (opensilex.compareUris(element.datatype, Xsd.DATETIME)) {
          variableHelp += " " + t("component.data.dataTemplateForm.format-help.datetime");
        } else if (opensilex.compareUris(element.datatype, Xsd.BOOLEAN)) {
          variableHelp += " " + t("component.data.dataTemplateForm.format-help.boolean");
        }
        line3.push(variableHelp);

        //column raw_data
        if (withRawData.value) {
          line1.push("raw_data");
          line2.push(t("component.data.dataTemplateForm.raw-data"));
          line3.push(
            t("component.data.dataHelp.column-type-help").toString() +
            t("component.data.dataTemplateForm.type-list") +
            opensilex.getVariableDatatypeLabel(element.datatype));
        }
      }

      //column annotation on object
      if (selectedColumns.value.includes(annotationColumn)) {
        line1.push(annotationColumn);
        line2.push(t("component.data.dataTemplate.annotationHelp"));
        line3.push(t("component.data.dataHelp.column-type-help") + "String");
      }

      //column prov_used
      if (withProvUsed.value) {
        line1.push("prov_used");
        line2.push(t("component.data.dataTemplateForm.prov_used"));
        line3.push(t("component.data.dataHelp.column-type-help") + "String");
      }

      let arrData = [line1, line2, line3];
      downloadCsv(Papa.unparse(arrData, {delimiter: separator.value}), "datasetTemplate");
    }).catch((error) => {
      opensilex.showErrorToast(t("component.data.dataTemplateForm.variable-not-found-error"));
      console.error(error);
    });
}

function getDataTypeLabel(dataTypeUri: string): string {
  if (!dataTypeUri) {
    return undefined;
  }
  let datatype = opensilex.getDatatype(dataTypeUri);
  if (!datatype) {
    return dataTypeUri;
  }
  let label = t(datatype.label_key);
  return label.charAt(0).toUpperCase() + label.slice(1);
}

function toggleColumn(column: string, checked: boolean) {
  selectedColumns.value = checked
    ? [...selectedColumns.value, column]
    : selectedColumns.value.filter(c => c !== column);
  change();
}

function change() {
  validSelection.value = selectedColumns.value.includes(targetColumn) || selectedColumns.value.includes(deviceColumn);
}
function submit() {
  if ((props.experiment == null && !validSelection.value) || variables.value.length === 0) {
    return;
  }
  csvExport();
}
defineExpose({
  show,
  hide
});
</script>

<style scoped>
.columns-checkboxes {
  display: flex;
  justify-content: flex-start;
  align-items: center;
}

.columns-checkboxes {
  display: flex;
  gap: 10px;
}
</style>
