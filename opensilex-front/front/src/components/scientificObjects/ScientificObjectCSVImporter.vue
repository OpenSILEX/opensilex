<template>
  <div>
    <OntologyCsvImporter
      ref="importForm"
      :baseType="$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI"
      successImportMsg="component.scientificObjects.import.success-message"
      :validateCSV="validateCSV"
      :uploadCSV="uploadCSV"
      @csvImported="onCsvImported($event)"
    >
      <template #icon>
        <Icon icon="bi#bi-bullseye" class="icon-title"/>
      </template>
      <template #help>
        <ScientificObjectImportHelp></ScientificObjectImportHelp>
      </template>
      <template #generator>
        <div class="col-12 col-md-2">
          <Button
            class="mr-2 greenThemeColor"
            :small="false"
            @click="templateGenerator.show()"
            label="component.common.import-files.generate-template"
          ></Button>
          <OntologyCsvTemplateGenerator
            ref="templateGenerator"
            :baseType="$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI"
            templatePrefix="scientific_object"
            typePlaceholder="OntologyObjectForm.form-type-placeholder"
            uriHelp="component.scientificObjects.help-messages.uri-help"
            :uriExample="SCIENTIFIC_OBJECT_URI_EXAMPLE"
            typeHelp="component.scientificObjects.help-messages.type-help"
            :typeExample="SCIENTIFIC_OBJECT_TYPE_EXAMPLE"
            :extraHeadersAndDescriptions="moveDescriptionGeneratorsPerMoveHeader"
          ></OntologyCsvTemplateGenerator>
        </div>
      </template>
    </OntologyCsvImporter>
  </div>
</template>

<script setup lang="ts">
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {useStore} from "vuex";
import OntologyCsvImporter from '../ontology/csv/OntologyCsvImporter.vue';
import OntologyCsvTemplateGenerator from "../ontology/csv/OntologyCsvTemplateGenerator.vue";
import {DescriptionGeneratorInformation} from "../../components/ontology/csv/OntologyCsvTemplateGenerator.vue";
import GenerateEventTemplate from "../../components/events/form/csv/GenerateEventTemplate.vue";
import {computed, inject, ref} from "vue";
import Icon from "@/components/common/views/Icon.vue";
import Button from "@/components/common/buttons/Button.vue";

//#region Constant values
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const $store = useStore();

const moveDescriptionGeneratorsPerMoveHeader: Map<string, DescriptionGeneratorInformation> = new Map<string, DescriptionGeneratorInformation>([
  ["start_date_of_Location", {propertyTranslationKey: "component.scientificObjects.geometry.startHelp", required: false, example: "component.events.start-example"}],
  ["end_date_of_Location", {propertyTranslationKey: "component.scientificObjects.geometry.endHelp", required: false, example: "component.events.start-example"}],
  ...GenerateEventTemplate.MOVE_DESCRIPTION_GENERATOR_BY_HEADER
]);

const SCIENTIFIC_OBJECT_URI_EXAMPLE: string = "http://opensilex.org/id/scientific-object/so-name1";
const SCIENTIFIC_OBJECT_TYPE_EXAMPLE: string = "vocabulary:Plant";
//#endregion

//#region Template refs
const importForm = ref<InstanceType<typeof OntologyCsvImporter>>(null);
const templateGenerator = ref<InstanceType<typeof OntologyCsvTemplateGenerator>>(null);
const resultModal = ref(null);
//#endregion

//#region Props
interface Props{
  experimentURI?: string
}
const props = defineProps<Props>();
//#endregion

//#region Emits
const emit = defineEmits<{
  csvImported: () => void;
}>();
//#endregion

//#region Computed
const user = computed(() => {
  return $store.state.user;
});

const lang = computed(() => {
  return $store.state.lang;
});
//#endregion

//#region Reactive data
const nbLinesImported = ref(0);
//#endregion

//#region Functions
function validateCSV(csvFile: File): Promise<any> {
  return $opensilex.uploadFileToService(
    "/core/scientific_objects/import_validation",
    {
      description: {
        experiment: props.experimentURI,
      },
      file: csvFile,
    },
    null,
    null
  );
}

function uploadCSV(validationToken: string, csvFile: File): Promise<any> {
  return $opensilex.uploadFileToService(
    "/core/scientific_objects/import",
    {
      description: {
        experiment: props.experimentURI,
        validationToken: validationToken
      },
      file: csvFile,
    },
    null,
    null
  );
}
//#endregion

//#region Function calls to children
function show() {
  importForm.value.show();
}
//#endregion

//#region Event handlers
function onCsvImported(response) {
  emit("csvImported", response);
}
//#endregion

//#region Exposed to the virus
defineExpose({show});
//#endregion
</script>

<style scoped lang="scss">
</style>
