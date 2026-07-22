<template>
  <n-form
      ref="formRef"
      v-if="form.name_translations"
      :rules="rules"
      :model="form"
  >
    <n-form-item path="uri">
      <InputForm
          v-model:value="form.uri"
          label="component.common.uri"
          type="text"
          rules="url"
          :disabled="editMode"
          :required="true"
      ></InputForm>
    </n-form-item>

    <n-form-item path="parent">
      <FormSelector
          v-model:selected="form.parent"
          path="parent"
          :options="parentOptions"
          checkStrategy="parent"
          label="component.common.parent"
          :filterable="true"
          :required="true"
      ></FormSelector>
    </n-form-item>

    <n-form-item path="name_translations.en">
      <InputForm
          v-model:value="form.name_translations.en"
          :label="t('OntologyClassForm.labelEN')"
          :required="true"
          type="text"
      ></InputForm>
    </n-form-item>

    <n-form-item path="comment_translations.en">
      <TextAreaForm
          v-model:value="form.comment_translations.en"
          :label="t('OntologyClassForm.commentEN')"
          @keydown.native.enter.stop
      ></TextAreaForm>
    </n-form-item>

    <n-form-item path="name_translations.fr">
      <InputForm
          v-model:value="form.name_translations.fr"
          :label="t('OntologyClassForm.labelFR')"
          :required="true"
          type="text"
      ></InputForm>
    </n-form-item>

    <n-form-item path="comment_translations.fr">
      <TextAreaForm
          v-model:value="form.comment_translations.fr"
          :label="t('OntologyClassForm.commentFR')"
          @keydown.native.enter.stop
      ></TextAreaForm>
    </n-form-item>

    <n-form-item path="icon">
      <IconForm
          v-model:value="form.icon"
          :label="t('OntologyClassForm.icon')"
      ></IconForm>
    </n-form-item>
  </n-form>
</template>

<script setup lang="ts">
import {computed, inject, onMounted, ref, useTemplateRef, watch, watchEffect} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useI18n} from "vue-i18n";
import {VueJsOntologyExtensionService} from "@/lib";
import HttpResponse, {OpenSilexResponse} from "@/lib/HttpResponse";
import {NForm, NFormItem} from "naive-ui";
import {OntologyService} from "opensilex-core/api/ontology.service";
import InputForm from "@/components/common/forms/InputForm.vue";
import FormSelector from "@/components/common/forms/FormSelector.vue";
import TextAreaForm from "@/components/common/forms/TextAreaForm.vue";
import IconForm from "@/components/common/forms/IconForm.vue";
import {required, requiredTrimmed} from "@/models/FormFieldsFormatter";

//#region Public
const props = defineProps<{
  editMode: boolean,
  data: {
    parentUri: string
  }
}>();

const form = defineModel("form", {
  default: {
    uri: null,
    parent: null,
    name: null,
    name_translations: {en: null, fr: null},
    comment: null,
    comment_translations: {en: "", fr: ""},
    icon: null,
    is_abstract: false
  }
});

defineExpose({
  getEmptyForm,
  create,
  update,
  validate
})

const emit = defineEmits<{
  validationChanged: [boolean]
}>()
//#endregion

//#region Private
const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const ontologyService = opensilex.getService<OntologyService>("opensilex-core.OntologyService");
const service = opensilex.getService<VueJsOntologyExtensionService>("opensilex.VueJsOntologyExtensionService");
const {t} = useI18n();

const availableParents = ref<Array<any>>([]);

const parentOptions = computed(() => {
  if (props.editMode) {
    return opensilex.buildTreeListOptions(availableParents.value, {
      disableSubTree: form.value.uri
    });
  } else {
    return opensilex.buildTreeListOptions(availableParents.value);
  }
})

onMounted(() => {
  emit('validationChanged', false)
})

const rules = ref({
  uri: required('component.common.uri'),
  parent: required('component.common.parent'),
  name_translations: {
    en: requiredTrimmed('OntologyClassForm.labelEN'),
    fr: requiredTrimmed('OntologyClassForm.labelFR')
  }
});

const formRef = useTemplateRef<InstanceType<typeof NForm>>("formRef");

watchEffect(() => {
  if (props.data.parentUri) {
    ontologyService.searchSubClassesOf(props.data.parentUri, undefined, false).then(http => {
      if (http.response.result.length > 0) {
        availableParents.value = http.response.result;
      }
    })
  }
})

watch(form, async () => {
  const isValid = await validate();
  emit('validationChanged', isValid);
}, {deep: true})

function getEmptyForm() {
  return {
    uri: null,
    parent: null,
    name: null,
    name_translations: {en: "", fr: ""},
    comment: null,
    comment_translations: {en: "", fr: ""},
    icon: null,
    is_abstract: false
  };
}

function create(form) {
  return service
      .createRDFType(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        let message = t("OntologyClassView.the-type") + " " + uri + t("component.common.success.creation-success-message");
        opensilex.showSuccessToast(message);
      })
      .catch(error => {
        if (error.status == 409) {
          console.error("Object type already exists", error);
          opensilex.errorHandler(
              error,
              t("OntologyClassForm.object-type-already-exists")
          );
        } else {
          opensilex.errorHandler(error);
        }
      });
}

function update(form) {
  return service
      .updateRDFType(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let message = t("OntologyClassView.the-type") + " " + http.response.result + t("component.common.success.update-success-message");
        opensilex.showSuccessToast(message);
      })
      .catch(opensilex.errorHandler);
}

async function validate(): Promise<boolean> {
  try {
    const errors = await formRef.value.validate();
    return !errors.warnings;
  } catch (error) {
    return false;
  }
}

//#endregion
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
  OntologyClassForm:
    abstract-type: Abstract type
    labelEN: English name
    labelFR: French name
    commentEN: English description
    commentFR: French description
    object-type-already-exists: Object type with same URI already exists
    icon: Icon

fr:
  OntologyClassForm:
    abstract-type: Type abstrait
    labelEN: Nom anglais
    labelFR: Nom français
    commentEN: Description anglaise
    commentFR: Description française
    object-type-already-exists: Un type d'objet existe déjà avec la même URI
    icon: Icône
</i18n>