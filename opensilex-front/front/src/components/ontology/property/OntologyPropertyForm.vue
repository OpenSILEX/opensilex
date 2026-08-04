<template>
  <Modal ref="modal">
    <template #header>
      <FormHeader :title="formTitle" icon="bi#bi-geo-alt"/>
    </template>

    <n-form
        ref="nForm"
        :model="form"
        :rules="rules"
        v-if="form.name_translations"
    >
      <n-form-item path="uri">
        <InputForm
            v-model:value="form.uri"
            label="component.common.uri"
            type="text"
            rules="url"
            :disabled="isEditMode"
            :required="true"
        ></InputForm>
      </n-form-item>

      <hr/>

      <div class="row">
        <div class="col-lg-6">
          <n-form-item path="rdf_type">
            <n-radio-group v-model:value="form.rdf_type" id="datatypeRadio">
              <div>
                <n-radio
                    :value="OWL.DATATYPE_PROPERTY_URI"
                    :label="t('component.ontology.property.form.dataProperty')"
                >
                </n-radio>
              </div>
              <div>
                <n-radio
                    :value="OWL.OBJECT_PROPERTY_URI"
                    :label="t('component.ontology.property.form.objectProperty')"
                >
                </n-radio>
              </div>
              <div>
                <n-radio
                    :value="null"
                    :label="t('component.ontology.property.form.inheritedType')"
                >
                </n-radio>
              </div>
            </n-radio-group>
          </n-form-item>
        </div>
        <div class="col-lg-6">
          <FormSelector
              v-if="form.rdf_type == OWL.DATATYPE_PROPERTY_URI"
              path="range"
              :label="t('component.ontology.property.form.data-type')"
              :required="true"
              v-model:selected="form.range"
              :options="dataTypes"
              :filterable="true"
              :helpMessage="t('component.ontology.property.form.dataProperty-help')"
          ></FormSelector>

          <FormSelector
              v-if="form.rdf_type == OWL.OBJECT_PROPERTY_URI"
              path="range"
              :label="t('component.ontology.property.form.object-type')"
              :required="true"
              v-model:selected="form.range"
              :options="objectTypes"
              :filterable="true"
              :helpMessage="t('component.ontology.property.form.objectProperty-help')"
          ></FormSelector>

          <FormSelector
              v-if="form.rdf_type == null"
              path="range"
              :label="t('component.common.parent')"
              :required="true"
              v-model:selected="form.parent"
              :options="parentOptions"
              :filterable="true"
              :helpMessage="t('component.ontology.property.form.parent-help')"
          ></FormSelector>

          <n-form-item path="domain">
            <TypeForm
                v-model:type="form.domain"
                :required="true"
                :baseType="domain"
                :ignoreRoot="false"
                :label="t('component.ontology.property.form.domain')"
                :helpMessage="t('component.ontology.property.form.domain-help')"
            ></TypeForm>
          </n-form-item>
        </div>
      </div>

      <hr/>

      <n-form-item path="name_translations.en">
        <InputForm
            v-model:value="form.name_translations.en"
            :label="t('component.ontology.property.form.label.en')"
            type="text"
            :required="enLangRequired"
        ></InputForm>
      </n-form-item>

      <n-form-item path="comment_translations.en">
        <TextAreaForm
            v-model:value="form.comment_translations.en"
            :label="t('component.ontology.property.form.comment.en')"
            :required="false"
            @keydown.native.enter.stop
        ></TextAreaForm>
      </n-form-item>

      <n-form-item path="name_translations.fr">
        <InputForm
            v-model:value="form.name_translations.fr"
            :label="t('component.ontology.property.form.label.fr')"
            type="text"
            :required="otherLangRequired"
        ></InputForm>
      </n-form-item>

      <n-form-item path="comment_translations.fr">
        <TextAreaForm
            v-model:value="form.comment_translations.fr"
            :label="t('component.ontology.property.form.comment.fr')"
            :required="false"
            @keydown.native.enter.stop
        ></TextAreaForm>
      </n-form-item>
    </n-form>
    <template #footer>
      <FormFooter @cancel="hide" @submit="submit"/>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import {computed, inject, ref, useTemplateRef, watchEffect} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {OntologyService} from "opensilex-core/api/ontology.service";
import {useI18n} from "vue-i18n";
import {useStore} from "vuex";
import OWL from "@/ontologies/OWL";
import {FormRules, NForm, NFormItem, NRadio, NRadioGroup} from "naive-ui";
import InputForm from "@/components/common/forms/InputForm.vue";
import FormSelector from "@/components/common/forms/FormSelector.vue";
import TypeForm from "@/components/common/forms/TypeForm.vue";
import TextAreaForm from "@/components/common/forms/TextAreaForm.vue";
import Modal from "@/components/common/views/Modal.vue";
import FormHeader from "@/components/common/forms/FormHeader.vue";
import FormFooter from "@/components/common/forms/FormFooter.vue";
import useModalFormLogic, {ModalFormEmits, ModalFormProps} from "@/composables/useModalFormLogic";
import {RDFPropertyDTO} from "opensilex-core/model/rDFPropertyDTO";
import { required } from "@/models/FormFieldsFormatter";

//#region Public
const props = defineProps<ModalFormProps & {
  domain: string
}>();

const emit = defineEmits<ModalFormEmits>();
//#endregion

//#region Private
const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const store = useStore();
const ontologyService = opensilex.getService<OntologyService>("opensilex-core.OntologyService");
const {t} = useI18n();

const lang = computed(() => store.state.lang);
const enLangRequired = computed(() => lang.value === "en");
const otherLangRequired = computed(() => lang.value !== "en");

const rules: FormRules = {
  uri: required("component.common.uri"),
  domain: required("component.ontology.property.form.domain"),
  range: required("component.ontology.property.form.range"),
  name_translations: {
    en: enLangRequired.value ? required("component.ontology.property.form.label.en") : undefined,
    fr: otherLangRequired.value ? required("component.ontology.property.form.label.fr") : undefined,
  }
};

const availableParents = ref([]);
const rdfTypeByParentURI = ref({});

const parentOptions = computed(() => {
  if (isEditMode) {
    return opensilex.buildTreeListOptions(availableParents.value, {
      disableSubTree: form.value.uri
    });
  } else {
    return opensilex.buildTreeListOptions(availableParents.value);
  }
})

const dataTypes = computed(() => {
  let types: Array<{ id: string, label: string }> = [];

  opensilex.datatypes.forEach(type => {
    let label: any = t(type.label_key);
    types.push({
      id: type.uri,
      label: label.charAt(0).toUpperCase() + label.slice(1)
    });
  });

  sortTypesByLabel(types);
  return types;
});

const objectTypes = computed(() => {
  let types: Array<{ id: string, label: string }> = [];
  opensilex.objectTypes
      .filter(type => type.name)
      .forEach(type => {
        // try to get translated name
        let translatedLabel: string = type.rdf_type.name_translations[store.getters.language];

        // if no translation found, then use default name
        if (!translatedLabel || translatedLabel.length == 0) {
          translatedLabel = type.rdf_type.name;
        }
        types.push({
          id: type.uri,
          label: translatedLabel
        });
      });

  sortTypesByLabel(types);
  return types;
})

watchEffect(() => {
  if (props.domain) {
    ontologyService.getProperties(props.domain, undefined, true).then(http => {
      if (http.response.result.length > 0) {
        const dtoList = http.response.result;
        availableParents.value = dtoList;
        dtoList.forEach((dto) => {
          rdfTypeByParentURI.value[dto.uri] = dto.rdf_type;
        })
      }
    })
  }
});


const {form, formTitle, exposed, isEditMode, submit, hide} = useModalFormLogic<RDFPropertyDTO>({
  modalRef: useTemplateRef<InstanceType<typeof Modal>>("modal"),
  nFormRef: useTemplateRef("nForm"),
  getEmptyForm,
  create: ontologyService.createProperty.bind(ontologyService),
  update: ontologyService.updateProperty.bind(ontologyService),
  props,
  emit
});

function getEmptyForm(): RDFPropertyDTO {
  return {
    uri: undefined,
    rdf_type: OWL.DATATYPE_PROPERTY_URI,
    parent: undefined,
    name_translations: {en: "", fr: ""},
    comment_translations: {en: "", fr: ""},
    domain: undefined,
    range: undefined
  };
}

function sortTypesByLabel(types: Array<{ id: string, label: string }>): void {
  types.sort((a, b) => {
    if (a.label > b.label) {
      return 1;
    } else if (a.label < b.label) {
      return -1;
    }
    return 0;
  });
}

function showCreateForm(options: {
  parentUri?: string,
  domainUri: string
}): void {
  const createForm = getEmptyForm();
  createForm.parent = options.parentUri;
  createForm.domain = options.domainUri;
  exposed.showCreateForm(createForm);
}

//#endregion

defineExpose({
  ...exposed,
  showCreateForm
});
</script>

<style scoped lang="scss">
</style>