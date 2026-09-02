<template>
  <Modal ref="modal">
    <template #header>
      <FormHeader :title="formTitle"/>
    </template>
    <div>
      <Tutorial
          v-if="!isEditMode"
          ref="factorTutorial"
          :steps="steps"
          @onSkip="continueFormEditing()"
          @onFinish="continueFormEditing()"
          :editMode="isEditMode"
      ></Tutorial>
      <p v-if="isEditMode" class="alert-info">
        {{ $t("component.factor.alert-help") }}
      </p>
      <n-form
          ref="nForm"
      >
        <!-- URI -->
        <n-form-item>
        <UriForm
            v-model:uri="form.uri"
            :label="t('component.experiment.search.column.uri')"
            helpMessage="component.common.uri-help-message"
            :editMode="isEditMode"
            :generated.sync="uriGenerated"
        ></UriForm>
        </n-form-item>

        <!-- Name -->
        <div id="v-step-0">
          <n-form-item>
          <InputForm
              rules="nameFiltered"
              v-model:value="form.name"
              :label="t('component.experiment.label')"
              :helpMessage="t('component.experiment.name-factor-help')"
              type="text"
              :required="true"
              :placeholder="t('component.experiment.name-factor-placeholder')"
          ></InputForm>
          </n-form-item>
        </div>

        <p class="alert-info">
          {{ $t("component.experiment.category-help-more") }} : PECO (
          <a
              target="_blank"
              href="http://agroportal.lirmm.fr/ontologies/PECO/?p=classes&conceptid=http%3A%2F%2Fpurl.obolibrary.org%2Fobo%2FPECO_0001001"
          >
            Agroportal
          </a>
          ;
          <a
              target="_blank"
              href="http://agroportal.lirmm.fr/ontologies/PECO/?p=classes&conceptid=http%3A%2F%2Fpurl.obolibrary.org%2Fobo%2FPECO_0007359"
          >
            Ontobee
          </a>
          ) - AGROVOC (
          <a
              target="_blank"
              href="http://agroportal.lirmm.fr/ontologies/AGROVOC/?p=classes&conceptid=http%3A%2F%2Faims.fao.org%2Faos%2Fagrovoc%2Fc_331093"
          >
            Agroportal
          </a>
          ;
          <a
              target="_blank"
              href="http://agroportal.lirmm.fr/ontologies/AGROVOC/?p=classes&conceptid=http%3A%2F%2Faims.fao.org%2Faos%2Fagrovoc%2Fc_331093"
          >
            Agrovoc
          </a>
          )

        </p>
        <!-- Category-->
        <div id="v-step-1">
          <n-form-item>
          <FactorCategorySelector
              ref="factorCategorySelector"
              label="component.experiment.category"
              placeholder="component.experiment.category-factor-placeholder"
              helpMessage="component.experiment.category-factor-help"
              v-model:category="form.category"
          ></FactorCategorySelector>
          </n-form-item>
        </div>
        <!-- description -->
        <div id="v-step-2">
          <n-form-item>
          <TextAreaForm
              :value.sync="form.description"
              label="component.experiment.description"
              :placeholder="t('component.experiment.description-factor-placeholder')"
              helpMessage="component.experiment.description-factor-help"
          ></TextAreaForm>
          </n-form-item>
        </div>
        <div id="v-step-3">
          <FactorLevelTable
              ref="factorLevelTable"
              :editMode.sync="isEditMode"
              v-model:factorLevels="form.levels"
          ></FactorLevelTable>
        </div>
      </n-form>
    </div>

    <template #footer>
      <FormFooter @cancel="hide" @submit="submit"/>
    </template>
  </Modal>
</template>


<script setup lang="ts">
import UriForm from "@/components/common/forms/UriForm.vue";
import Tutorial from "@/components/common/views/Tutorial.vue";
import InputForm from "@/components/common/forms/InputForm.vue";
import TextAreaForm from "@/components/common/forms/TextAreaForm.vue";
import {computed, inject, ref, useTemplateRef} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useStore} from "vuex";
import {useI18n} from "vue-i18n";
import HttpResponse, {OpenSilexResponse} from "@/lib/HttpResponse";
import FactorCategorySelector from "@/components/experiments/factors/FactorCategorySelector.vue";
import {FactorsService} from "opensilex-core/api/factors.service";
import {FactorCreationDTO} from "opensilex-core/model/factorCreationDTO";
import FactorLevelTable from "@/components/experiments/factors/FactorLevelTable.vue";
import useModalFormLogic, {ModalFormEmits, ModalFormProps} from "@/composables/useModalFormLogic";
import {NForm} from "naive-ui";
import {NFormItem} from "naive-ui";
import Modal from "@/components/common/views/Modal.vue";
import FormHeader from "@/components/common/forms/FormHeader.vue";
import FormFooter from "@/components/common/forms/FormFooter.vue";


const opensilex = inject<OpenSilexVuePlugin>('opensilex')
const store = useStore()
const {t} = useI18n()

const user = computed(() => {
  return store.state.user;
})

const factorTutorial = useTemplateRef<InstanceType<typeof Tutorial>>('factorTutorial')
const factorCategorySelector = useTemplateRef<InstanceType<typeof FactorCategorySelector>>('factorCategorySelector')
const factorLevelTable = useTemplateRef<InstanceType<typeof FactorLevelTable>>('factorLevelTable')

const uriGenerated = ref(true);

const props = defineProps<ModalFormProps & {
  experiment: string
}>();

const emit = defineEmits<ModalFormEmits>()

const {form, formTitle, isEditMode, exposed, submit, hide} = useModalFormLogic<FactorCreationDTO>({
  modalRef: useTemplateRef<InstanceType<typeof Modal>>('modal'),
  nFormRef: useTemplateRef<InstanceType<typeof NForm>>('nForm'),
  getEmptyForm,
  create,
  update,
  props,
  emit
})

let savedForm: Partial<FactorCreationDTO> = {};

function reset(uriGenerated) {
  uriGenerated = true;
  if (!isEditMode) {
    factorTutorial.value.stop();
  }
}

function addEmptyRow(form) {
  console.debug("add row");
  form.value.levels.unshift({
    uri: null,
    name: null,
    description: null,
  });
}

function saveForm() {
  savedForm = structuredClone(form.value);
}

function getEmptyForm(): FactorCreationDTO {
  return {
    uri: null,
    name: null,
    category: null,
    description: null,
    experiment: props.experiment,
    levels: [
      {
        uri: null,
        name: null,
        description: null,
      },
    ],
  };
}

const steps = computed(() => {
  return [
    {
      target: "#v-step-0", // We're using document.querySelector() under the hood
      header: {
        title: t("component.factor.name").toString(),
      },
      content: t("component.factor.name-help").toString(),
      params: {
        placement: "left", // Any valid Popper.js placement. See https://popper.js.org/popper-documentation.html#Popper.placements
      },
    },
    {
      target: "#v-step-1",
      header: {
        title: t("component.factor.category").toString(),
      },
      content: t("component.factor.category-help").toString(),
      params: {
        placement: "left", // Any valid Popper.js placement. See https://popper.js.org/popper-documentation.html#Popper.placements
      },
    },
    {
      target: "#v-step-2",
      header: {
        title: t("component.factor.description").toString(),
      },
      content: t("component.factor.description-help").toString(),
      params: {
        placement: "left", // Any valid Popper.js placement. See https://popper.js.org/popper-documentation.html#Popper.placements
      },
    },
    {
      target: "#v-step-3",
      header: {
        title: t("component.factorLevel.associated").toString(),
      },
      content:
          t("component.factorLevel.associated-help")
              .toString(),
      params: {
        placement: "left", // Any valid Popper.js placement. See https://popper.js.org/popper-documentation.html#Popper.placements
      },
    },
  ];
})


function continueFormEditing() {
  console.debug('Reinitialise form')

  form.value.uri = savedForm.uri
  form.value.name = savedForm.name
  form.value.category = savedForm.category
  form.value.description = savedForm.description
  form.value.levels = savedForm.levels
}

function setUri(uri: string) {
  form.value.uri = uri;
}

function afterCreate(uri: string) {
  setUri(uri);
}

function create(form) {
  console.debug("factor", form);
  return opensilex
      .getService<FactorsService>("opensilex.FactorsService")
      .createFactor(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Factor created", uri);
        form.uri = uri;
        return uri;
      })
      .catch((error) => {
        if (error.status == 409) {
          console.error("Factor already exists", error);
          opensilex.errorHandler(
              error,
              t("component.account.errors.user-already-exists")
          );
        } else {
          opensilex.errorHandler(error);
        }
      });
}

function update(form) {
  return opensilex
      .getService<FactorsService>("opensilex.FactorsService")
      .updateFactor(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Factor updated", uri);
        return http
      })
      .catch(opensilex.errorHandler);
}

const languageCode = computed(() => {
  return opensilex.getLocalLangCode();
})


function tutorial() {
  form.value.name = t("component.factor.example.name");
  form.value.category = t("component.factor.example.category");

  form.value.description = t(
      "component.factor.example.description"
  );
  let levels = [];
  for (const [key, value] of Object.entries(
      t("component.factor.example.factorLevels")
  )) {
    levels.push(value);
  }
  form.value.levels = levels;
  factorTutorial.value.start();
}

defineExpose(exposed)
</script>

<style scoped lang="scss">
a {
  color: #007bff;
}
</style>
<i18n>

en:
  component:
    factor:
      alert-help: This factor can be linked to existing experiments be careful when update
      example:
        name: Nitrogen
        category: "http://aims.fao.org/aos/agrovoc/c_5b384c25"
        description: Chemical compound added in the soil
        factorLevels:
          - name: "N-10/N+"
            description: "Dose 10 mmolar"
          - name: "N-5/N-"
            description: "Dose 5 mmolar"
      category-help-more: "More information"
      or: or

fr:
  component:
    factor:
      alert-help: Ce facteur peut être lié à des expérimentations faite attention lors de son édition
      example:
        name: Azote
        category: "http://aims.fao.org/aos/agrovoc/c_5b384c25"
        description: Composant chimique ajouté dans le sol
        factorLevels:
          - name: "N-10/N+"
            description: "Dosage 10 mmol"
          - name: "N-5/N-"
            description: "Dosage 5 mmol"
      category-help-more: "Plus d'informations"
      or: ou
      category-help: "Classifie le facteur utilisé pour des recherches"

</i18n>